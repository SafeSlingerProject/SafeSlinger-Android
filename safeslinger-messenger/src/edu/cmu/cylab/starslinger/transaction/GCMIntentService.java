package edu.cmu.cylab.starslinger.transaction;

import java.io.IOException;
import java.util.Arrays;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;

import com.google.android.gms.gcm.GoogleCloudMessaging;

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

public class GCMIntentService extends IntentService
{

    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    
    private static WebEngine mWeb = null;
    
	public GCMIntentService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		Bundle extras = intent.getExtras();
		 GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
	     String messageType = gcm.getMessageType(intent);

	     if(!extras.isEmpty())
	     {
	    	 if(intent.getAction().equals(C2DMessaging.C2DM_RETRY))
	    	 {
	    		 C2DMessaging.registerInBackground(this);
	    	 }
	    	 else if(GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.compareTo(messageType) == 0)
	    	 {
	    		 InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(this);
	    	        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this);
	    	        long rowIdInbox = 0;
	    	        long rowIdMsg = 0;
	    	        boolean giveNotificationFeedback = true;
	    	        StringBuilder keyidout = new StringBuilder();

	    	        boolean reattempt = true;
	    	        while (reattempt) {
	    	            // we only want to execute once...
	    	            reattempt = false;

	    	            // Extract the payload from the message
//	    	            Bundle extras = intent.getExtras();
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
	    	                    byte[] plain = CryptTools.decryptMessage(encMsg, pass, keyidout);
	    	                    MessagePacket push = new MessagePacket(plain);

	    	                    // move encrypted message to decrypted storage...
	    	                    MessageData inRow = null;
	    	                    Cursor c = dbInbox.fetchInboxSmall(rowIdInbox);
	    	                    if (c != null) {
	    	                        try {
	    	                            if (c.moveToFirst()) {
	    	                                inRow = new MessageRow(c, true);
	    	                            }
	    	                        } finally {
	    	                            c.close();
	    	                        }
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
	    	                // attempt to notify if appropriate...
	    	                Intent updateIntent = new Intent(SafeSlingerConfig.Intent.ACTION_MESSAGEINCOMING);
	    	                updateIntent.putExtra(extra.MESSAGE_ROW_ID, rowIdMsg);
	    	                updateIntent.putExtra(extra.KEYID, keyidout.toString());
	    	                updateIntent.putExtra(extra.NOTIFY_COUNT, allCount);
	    	                updateIntent.putExtra(extra.NOTIFY_STATUS, giveNotificationFeedback);
	    	                sendOrderedBroadcast(updateIntent, null);
	    	            }
	    	        }
	    	 }
	    	 else if(GoogleCloudMessaging.ERROR_SERVICE_NOT_AVAILABLE.compareTo(messageType) == 0 || GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.compareTo(messageType) == 0)
	    	 {
	    		 Intent errIntent = new Intent();
	    		 intent.putExtra(C2DMessaging.EXTRA_ERROR, messageType);
	    		 C2DMessaging.handleRegistration(this, errIntent);
	    		 onError(this, messageType);
	    	 }
	    	 else
	    		 onError(this, extras.toString());
	     }
	     
	     GCMBroadcaastReceiver.completeWakefulIntent(intent);
        
	}
	
	private void onError(Context context, String errorId)
	{
		MyLog.e(TAG, "push reg error: " + errorId);

        // link to callback in Home...
        String failedId = null;
        Intent i = new Intent(C2DMessaging.PUSH_REGISTERED).putExtra(extra.PUSH_REGISTRATION_ID, failedId)
                .putExtra(extra.ERROR, errorId);
        sendBroadcast(i);
	}
}
