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

import java.util.Arrays;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Base64;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.ConfigData.extra;
import edu.cmu.cylab.starslinger.ExchangeException;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPacketSizeException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider;
import edu.cmu.cylab.starslinger.model.MessageDbAdapter;
import edu.cmu.cylab.starslinger.view.HomeActivity;

public class C2DMReceiver extends C2DMBaseReceiver {
    private static final String TAG = ConfigData.LOG_TAG;
    public static final String PUSH_REGISTERED = "PUSH_REGISTERED";
    public static final String PUSH_MESSAGE = "PUSH_MESSAGE";
    private static WebEngine mWeb = null;

    public C2DMReceiver() {
        super(ConfigData.PUSH_SENDERID_EMAIL);
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

        // Extract the payload from the message
        Bundle extras = intent.getExtras();
        if (extras != null) {
            MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this);

            long rowId = 0;
            byte[] msgHashBytes = null;
            String msgHash = extras.getString("msgid");
            MyLog.d(TAG, "msgid:" + msgHash);

            if (TextUtils.isEmpty(msgHash)) {
                // ignore messages we are not expecting
                return;
            }

            // parse retrieval id
            try {
                msgHashBytes = Base64.decode(msgHash.getBytes(), Base64.NO_WRAP);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return;
            }

            // save retrieval id
            rowId = dbMessage.createRecvEncMessage(msgHash,
                    MessageDbAdapter.MESSAGE_STATUS_GOTPUSH, MessageDbAdapter.MESSAGE_IS_NOT_SEEN);
            if (rowId == -1) {
                return; // ignore on error, perhaps duplicate
            }

            int msgCount = dbMessage.fetchUnseenMessageCount();
            doUnseenMessagesNotification(this, msgCount);

            // attempt to update messages if in view...
            Intent recvIntent = new Intent(ConfigData.Intent.ACTION_MESSAGEUPDATE);
            recvIntent.putExtra(extra.MESSAGE_ROW_ID, rowId);
            sendBroadcast(recvIntent);

            // download initial message, from push, or else from web...
            if (mWeb == null) {
                mWeb = new WebEngine(this);
            }
            byte[] resp = null;
            String msgEnc = extras.getString("msgenc");
            if (!TextUtils.isEmpty(msgEnc)) {
                // try automatic parse of cipher
                try {
                    resp = Base64.decode(msgEnc.getBytes(), Base64.NO_WRAP);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    // try downloading...
                }
            }
            if (resp == null) {
                // try manual download of cipher
                try {
                    resp = mWeb.getMessage(msgHashBytes);
                } catch (ExchangeException e) {
                    showNote(e.getLocalizedMessage());
                    return;
                } catch (MessageNotFoundException e) {
                    showNote(e.getLocalizedMessage());
                    return;
                }
            }
            if (resp == null || resp.length == 0) {
                showNote(R.string.error_InvalidIncomingMessage);
                return;
            }
            byte[] encMsg = WebEngine.parseMessageResponse(resp);
            if (encMsg == null) {
                showNote(R.string.error_InvalidIncomingMessage);
                return;
            }

            // if hash does not match do not store download
            if (Arrays.equals(msgHashBytes, CryptTools.computeSha3Hash(encMsg))) {
                CryptoMsgProvider tool = CryptoMsgProvider.createInstance(SafeSlinger
                        .getApplication().isLoggable());
                String keyid = null;
                try {
                    keyid = tool.ExtractKeyIDfromPacket(encMsg);
                } catch (CryptoMsgPacketSizeException e) {
                    e.printStackTrace();
                }
                // save downloaded initial message
                if (!dbMessage.updateMessageDownloaded(rowId, encMsg,
                        MessageDbAdapter.MESSAGE_IS_NOT_SEEN, keyid)) {
                    showNote(R.string.error_UnableToUpdateMessageInDB);
                }
            }

            // attempt to update messages if in view...
            Intent downldIntent = new Intent(ConfigData.Intent.ACTION_MESSAGEUPDATE);
            downldIntent.putExtra(extra.MESSAGE_ROW_ID, rowId);
            sendBroadcast(downldIntent);
        }
    }

    public static void doUnseenMessagesNotification(Context ctx, int msgCount)
            throws OutOfMemoryError {
        long when = System.currentTimeMillis(); // notification time

        // To create a status bar notification:
        // Get a reference to the NotificationManager:
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(ns);

        String tickerText = ctx.getString(R.string.title_NotifyFileAvailable);

        String contentTitle = String.format(Locale.getDefault(), "%s (%d)",
                ctx.getString(R.string.app_name), msgCount);
        String contentText = String.format(ctx.getString(R.string.label_ClickForNMsgs), msgCount);

        Intent intent = makeMessagesNotificationIntent(ctx);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // the next two lines initialize the Notification, using the
        // configurations above
        int visibleMsgCount = msgCount != 1 ? msgCount : 0;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)//
                .setContentIntent(contentIntent)//
                .setSmallIcon(R.drawable.ic_stat_notify_msg)//
                .setTicker(tickerText)//
                .setWhen(when)//
                .setAutoCancel(true)//
                .setContentTitle(contentTitle)//
                .setContentText(contentText)//
                .setDefaults(Notification.DEFAULT_ALL)//
                .setNumber(visibleMsgCount); // API 11+

        try {
            builder.setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(),
                    R.drawable.ic_launcher));
        } catch (OutOfMemoryError e) {
            // ignore icon when out of memory
        }

        Notification n = builder.getNotification();

        // total messages seen...
        n.number = visibleMsgCount; // API <11
        if (msgCount != 1) {
            // cancel old one since we want to avoid the "1" when updating
            // number
            nm.cancel(HomeActivity.NOTIFY_NEW_MSG_ID);
        }

        // Pass the Notification to the NotificationManager:
        nm.notify(HomeActivity.NOTIFY_NEW_MSG_ID, n);
    }

    public static Intent makeMessagesNotificationIntent(Context ctx) {
        Intent getFileIntent = new Intent(ctx, HomeActivity.class);
        getFileIntent.setAction(ConfigData.Intent.ACTION_MESSAGENOTIFY);
        return getFileIntent;
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
