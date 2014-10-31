/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2010-2014 Carnegie Mellon University
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.cmu.cylab.starslinger.transaction;

import java.io.IOException;
import java.util.Arrays;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import edu.cmu.cylab.starslinger.ExchangeException;
import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPacketSizeException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider;
import edu.cmu.cylab.starslinger.model.InboxDbAdapter;
import edu.cmu.cylab.starslinger.model.MessageData;
import edu.cmu.cylab.starslinger.model.MessageDbAdapter;
import edu.cmu.cylab.starslinger.model.MessagePacket;
import edu.cmu.cylab.starslinger.model.MessageRow;

public class C2DMReceiver extends C2DMBaseReceiver {
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    public static final String PUSH_REGISTERED = "PUSH_REGISTERED";
    public static final String PUSH_MESSAGE = "PUSH_MESSAGE";
    private static WebEngine mWeb = null;

    public C2DMReceiver() {
        super(SafeSlingerConfig.PUSH_SENDERID_EMAIL);
    }

    @Override
    public void onRegistered(Context context, String registrationId) {
        MyLog.i(TAG, "Push registration ID arrived.");
        MyLog.i(TAG, registrationId);

        // link to callback in Home...
        Intent i = new Intent(PUSH_REGISTERED).putExtra(extra.PUSH_REGISTRATION_ID, registrationId);
        sendBroadcast(i);
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        MyLog.i(TAG, "C2DM message arrived.");

        InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(this);
        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this);
        long rowIdInbox = 0;
        long rowIdMsg = 0;
        boolean giveNotificationFeedback = true;

        boolean reattempt = true;
        while (reattempt) {
            // we only want to execute once...
            reattempt = false;

            // Extract the payload from the message
            Bundle extras = intent.getExtras();
            if (extras == null) {
                break; // we require message data
            }

            byte[] msgHashBytes = null;
            String msgHash = extras.getString("msgid");
            MyLog.d(TAG, "msgid:" + msgHash);

            if (TextUtils.isEmpty(msgHash)) {
                break; // ignore messages we are not expecting
            }

            // parse retrieval id
            try {
                msgHashBytes = Base64.decode(msgHash.getBytes(), Base64.NO_WRAP);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                break; // ignore messages we are not expecting
            }

            // save retrieval id
            rowIdInbox = dbInbox.createRecvEncInbox(msgHash,
                    MessageDbAdapter.MESSAGE_STATUS_GOTPUSH, MessageDbAdapter.MESSAGE_IS_NOT_SEEN);
            if (rowIdInbox == -1) {
                // TODO: pre rowId, so provide notice that database failed
                break; // ignore on error, perhaps duplicate
            }

            long prevStamp = SafeSlingerPrefs.getLastTimeStamp();
            SafeSlingerPrefs.setLastTimeStamp(System.currentTimeMillis());
            giveNotificationFeedback = ((System.currentTimeMillis() - prevStamp) >= SafeSlingerPrefs.NOTIFICATION_SLEEP_PERIOD) ? true
                    : false;

            // download initial message, from push, or else from web...
            if (mWeb == null) {
                mWeb = new WebEngine(this, SafeSlingerConfig.HTTPURL_MESSENGER_HOST);
            }
            byte[] respGetMsg = null;
            String msgEnc = extras.getString("msgenc");
            if (!TextUtils.isEmpty(msgEnc)) {
                // try automatic parse of cipher
                try {
                    respGetMsg = Base64.decode(msgEnc.getBytes(), Base64.NO_WRAP);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace(); // not fatal, try manual...
                }
            }
            if (respGetMsg == null) {
                // try manual download of cipher
                try {
                    respGetMsg = mWeb.getMessage(msgHashBytes);
                } catch (ExchangeException e) {
                    e.printStackTrace();
                    SafeSlinger.getApplication().checkForMissedMessages();
                    break;
                } catch (MessageNotFoundException e) {
                    if (!dbInbox.updateInboxExpired(rowIdInbox)) {
                        break;
                    }
                    break;
                }
            }
            if (respGetMsg == null || respGetMsg.length == 0) {
                break; // invalid format
            }
            byte[] encMsg = WebEngine.parseMessageResponse(respGetMsg);
            if (encMsg == null) {
                break; // unable to parse
            }

            // if hash does not match do not store download
            if (Arrays.equals(msgHashBytes, CryptTools.computeSha3Hash(encMsg))) {
                CryptoMsgProvider tool = CryptoMsgProvider.createInstance(SafeSlinger.isLoggable());
                String keyid = null;
                try {
                    keyid = tool.ExtractKeyIDfromPacket(encMsg);
                } catch (CryptoMsgPacketSizeException e) {
                    e.printStackTrace();
                    break;
                }
                // save downloaded initial message
                if (!dbInbox.updateInboxDownloaded(rowIdInbox, encMsg,
                        MessageDbAdapter.MESSAGE_IS_NOT_SEEN, keyid)) {
                    break; // unable to save progress
                }
            }

            String pass = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());

            // if requested, and logged in, try to decrypt message
            if (!TextUtils.isEmpty(pass) && SafeSlingerPrefs.getAutoDecrypt()) {
                try {
                    StringBuilder keyidout = new StringBuilder();
                    byte[] plain = CryptTools.decryptMessage(encMsg, pass, keyidout);
                    MessagePacket push = new MessagePacket(plain);

                    // move encrypted message to decrypted storage...
                    MessageData inRow = null;
                    Cursor c = dbInbox.fetchInboxSmall(rowIdInbox);
                    if (c != null) {
                        inRow = new MessageRow(c, true);
                        c.close();
                    }
                    if (inRow != null) {
                        // add decrypted
                        rowIdMsg = dbMessage.createMessageDecrypted(inRow, push,
                                keyidout.toString());
                        if (rowIdMsg == -1) {
                            break; // unable to save progress
                        } else {
                            // remove encrypted
                            dbInbox.deleteInbox(rowIdInbox);
                        }
                    }

                    // if requested, and logged in, try to get attachment
                    // (dependent on auto-decrypt)
                    // if (SafeSlingerPrefs.getAutoRetrieval()) {
                    // byte[] respGetFile = mWeb.getFile(msgHashBytes);
                    // if (respGetFile == null || respGetFile.length == 0) {
                    // break; // invalid format
                    // }
                    //
                    // byte[] encFile =
                    // WebEngine.parseMessageResponse(respGetFile);
                    // if (encFile == null) {
                    // break; // unable to parse
                    // }
                    //
                    // if (Arrays.equals(push.getFileHash(),
                    // CryptTools.computeSha3Hash(encFile))) {
                    // break; // invalid format
                    // }
                    //
                    // StringBuilder keyIdOut = new StringBuilder();
                    // byte[] rawFile = CryptTools.decryptMessage(encFile,
                    // pass, keyIdOut);
                    //
                    // if (rawFile != null) {
                    //
                    // // TODO: save decrypted file
                    // // mRecvMsg.setFileData(rawFile);
                    //
                    // dbMessage =
                    // MessageDbAdapter.openInstance(getApplicationContext());
                    // if (!dbMessage.updateFileDecrypted(rowId)) {
                    // break;
                    // }
                    // }
                    // }
                    // } catch (ExchangeException e) {
                    // e.printStackTrace();
                    // break;
                    // } catch (MessageNotFoundException e) {
                    // e.printStackTrace();
                    // break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } catch (CryptoMsgException e) {
                    e.printStackTrace();
                    break;
                } catch (GeneralException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        // attempt to update messages if in view...
        if (rowIdInbox > -1) {
            int inCount = dbInbox.getUnseenInboxCount();
            int msgCount = dbMessage.getUnseenMessageCount();
            int allCount = inCount + msgCount;
            if (allCount > 0) {
                // doUnseenMessagesNotification(this, allCount,
                // giveNotificationFeedback);

                // attempt to update messages if in view...
                Intent updateIntent = new Intent(SafeSlingerConfig.Intent.ACTION_MESSAGEUPDATE);
                updateIntent.putExtra(extra.MESSAGE_ROW_ID, rowIdMsg);
                updateIntent.putExtra(extra.NOTIFY_COUNT, allCount);
                updateIntent.putExtra(extra.NOTIFY_STATUS, giveNotificationFeedback);
                sendOrderedBroadcast(updateIntent, null);
                // sendBroadcast(updateIntent);
            }
        }
    }

    @Override
    public void onError(Context context, String errorId) {
        MyLog.e(TAG, "push reg error: " + errorId);

        // link to callback in Home...
        String failedId = null;
        Intent i = new Intent(PUSH_REGISTERED).putExtra(extra.PUSH_REGISTRATION_ID, failedId)
                .putExtra(extra.ERROR, errorId);
        sendBroadcast(i);
    }
}
