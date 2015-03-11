/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2010-2015 Carnegie Mellon University
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
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import edu.cmu.cylab.starslinger.ExchangeException;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgException;
import edu.cmu.cylab.starslinger.model.CryptoMsgPrivateData;
import edu.cmu.cylab.starslinger.util.SSUtil;

/**
 * Utilities for device registration. Will keep track of the registration token
 * in a private preference.
 */
public class C2DMessaging {
	public static final String TAG = "C2DMessaging";

	public static final String EXTRA_SENDER = "sender";
	public static final String EXTRA_APPLICATION_PENDING_INTENT = "app";
	public static final String REQUEST_UNREGISTRATION_INTENT = "com.google.android.c2dm.intent.UNREGISTER";
	public static final String REQUEST_REGISTRATION_INTENT = "com.google.android.c2dm.intent.REGISTER";

	public static final String SENDER_ID = "995290364307";
	public static final String EXTRA_UNREGISTERED = "unregistered";
	public static final String EXTRA_ERROR = "error";
	public static final String EXTRA_REGISTRATION_ID = "registration_id";
	public static final String ERRREG_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE";
	public static final String C2DM_RETRY = "com.google.android.c2dm.intent.RETRY";
	public static final String PUSH_REGISTERED = "PUSH_REGISTERED";
	
	public static final String ERRMSG_ERROR_PREFIX = "Error=";
    public static final String ERRREG_TOO_MANY_REGISTRATIONS = "TOO_MANY_REGISTRATIONS";

    /**
     * The sender account is not recognized.
     */
    public static final String ERRREG_INVALID_SENDER = "INVALID_SENDER";
  /* Incorrect phone registration with Google. This phone doesn't currently
  * support C2DM.
  */
    public static final String ERRREG_PHONE_REGISTRATION_ERROR = "PHONE_REGISTRATION_ERROR";

	 /***
	  * Too many messages sent by the sender. Retry after a while.
	  */
	 public static final String ERRMSG_QUOTA_EXCEEDED = "QuotaExceeded";
	
	 /***
	  * Too many messages sent by the sender to a specific device. Retry after a
	  * while.
	  */
	 public static final String ERRMSG_DEVICE_QUOTA_EXCEEDED = "DeviceQuotaExceeded";
	
	 /***
	  * Missing or bad registration_id. Sender should stop sending messages to
	  * this device.
	  */
	 public static final String ERRMSG_INVALID_REGISTRATION = "InvalidRegistration";
	
	 /***
	  * The registration_id is no longer valid, for example user has uninstalled
	  * the application or turned off notifications. Sender should stop sending
	  * messages to this device.
	  */
	 public static final String ERRMSG_NOT_REGISTERED = "NotRegistered";
	
	 /***
	  * The payload of the message is too big, see the limitations. Reduce the
	  * size of the message.
	  */
	 public static final String ERRMSG_MESSAGE_TOO_BIG = "MessageTooBig";
	
	 /***
	  * Collapse key is required. Include collapse key in the request.
	  */
	 public static final String ERRMSG_MISSING_COLLAPSE_KEY = "MissingCollapseKey";
	
	 /***
	  * HTTP Error from push service provider. See server logs for more details.
	  */
	 public static final String ERRMSG_NOTIFCATION_FAIL = "PushNotificationFail";
	
	 /***
	  * Internal server error from push service provider.
	  */
	 public static final String ERRMSG_SERVICE_FAIL = "PushServiceFail";
	
	 /***
	  * Message id not found on server. Likely it has been cleaned up already
	  * (expired).
	  */
	 public static final String ERRMSG_MESSAGE_NOT_FOUND = "MessageNotFound";

 // wakelock
 private static final String WAKELOCK_KEY = "C2DM_LIB";
	
	/**
	 * Initiate c2d messaging registration for the current application
	 */
//	public static void register(Context context, String senderId) {
//		Intent registrationIntent = new Intent(REQUEST_REGISTRATION_INTENT);
//		registrationIntent.putExtra(EXTRA_APPLICATION_PENDING_INTENT,
//				PendingIntent.getBroadcast(context, 0, new Intent(), 0));
//		registrationIntent.putExtra(EXTRA_SENDER, senderId);
//		context.startService(SSUtil.updateIntentExplicitness(context,
//				registrationIntent));
//	}

	/**
	 * Unregister the application. New messages will be blocked by server.
	 */
	public static void unregister(Context context) {
		Intent regIntent = new Intent(REQUEST_UNREGISTRATION_INTENT);
		regIntent.putExtra(EXTRA_APPLICATION_PENDING_INTENT,
				PendingIntent.getBroadcast(context, 0, new Intent(), 0));
		context.startService(SSUtil
				.updateIntentExplicitness(context, regIntent));
	}

	public static String getRegistrationId(Context context) {
		String regId = SafeSlingerPrefs.getPushRegistrationId();
		if (TextUtils.isEmpty(regId))
			return "";

		return regId;
	}

	public static void registerInBackground(final Context context) {

		final GoogleCloudMessaging gcm = GoogleCloudMessaging
				.getInstance(context);

		new AsyncTask<Void, Void, String>() {
			protected String doInBackground(Void params[]) {

				Intent intent = new Intent();
				try {

					String regId = gcm.register(SENDER_ID);
					intent.putExtra(EXTRA_REGISTRATION_ID, regId);

				} catch (IOException ex) {

					// If there is an error, don't just keep trying to register.
					// Require the user to click a button again, or perform
					// exponential back-off.
					intent.putExtra(EXTRA_ERROR, ex.getLocalizedMessage());
				}

				String regId = handleRegistration(context, intent);

				return regId;

			}
		
			protected void onPostExecute(String registrationId) 
			{
				if(!TextUtils.isEmpty(registrationId))
				{
					Intent i = new Intent(PUSH_REGISTERED).putExtra(extra.PUSH_REGISTRATION_ID, registrationId);
			        context.sendBroadcast(i);
				}
			};

		}.execute();
	}

	public static String handleRegistration(final Context context, Intent intent) {
		String registrationId = intent.getStringExtra(EXTRA_REGISTRATION_ID);
		String unregistered = intent.getStringExtra(EXTRA_UNREGISTERED);
		String error = intent.getStringExtra(EXTRA_ERROR);

		// MyLog.d(TAG, "dmControl: registrationId = " + registrationId
		// + ", error = " + error
		// + ", removed = " + unregistered);

		if (unregistered != null) {
			// Remember we are unregistered
			SafeSlingerPrefs.setPushRegistrationId(null); // clear
			SafeSlingerPrefs.setPushRegistrationIdPosted(false); // reset
			SafeSlingerPrefs
					.setPusgRegBackoff(SafeSlingerPrefs.DEFAULT_PUSHREG_BACKOFF);
			// onUnregistered(context);
			return "";
		} else if (error != null) {
			// we are not registered, can try again
			SafeSlingerPrefs.setPushRegistrationId(null); // clear
			SafeSlingerPrefs.setPushRegistrationIdPosted(false); // reset
			// Registration failed
			MyLog.e(TAG, "push reg error: " + error);

			// retry registration according to recommendations...
			if (error.equals(ERRREG_SERVICE_NOT_AVAILABLE)) {
				long backoff = SafeSlingerPrefs.getPusgRegBackoff();

				Intent retryIntent = new Intent(C2DM_RETRY);
				PendingIntent retryPIntent = PendingIntent
						.getBroadcast(context, 0 /* requestCode */, retryIntent,
								0 /* flags */);

				Date futureDate = new Date(new Date().getTime() + backoff);
				AlarmManager am = (AlarmManager) context
						.getSystemService(Context.ALARM_SERVICE);
				am.set(AlarmManager.RTC_WAKEUP, futureDate.getTime(),
						retryPIntent);

				// Next retry should wait longer.
				backoff *= 2;
				SafeSlingerPrefs.setPusgRegBackoff(backoff);
			}
		} else {
			// save incoming registration locally
			SafeSlingerPrefs.setPushRegistrationId(registrationId);
			SafeSlingerPrefs.setPushRegistrationIdPosted(false); // reset
			SafeSlingerPrefs
					.setPusgRegBackoff(SafeSlingerPrefs.DEFAULT_PUSHREG_BACKOFF);

			// save incoming registration on server
			try {
				WebEngine web = new WebEngine(context,
						SafeSlingerConfig.HTTPURL_MESSENGER_HOST);
				String pass = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs
						.getKeyIdString());
				// only update online if we are logged in
				if (!TextUtils.isEmpty(pass)) {
					CryptoMsgPrivateData mine = CryptTools.getSecretKey(pass);
					String keyId = mine.getKeyId();
					String submisisonToken = Base64.encodeToString(CryptTools
							.computeSha3Hash(mine.getSignPriKey().getBytes()),
							Base64.NO_WRAP);
					// only upload valid registration ids
					if (!TextUtils.isEmpty(registrationId)) {
						// post local active reg
						byte[] result = web.postRegistration(keyId,
								submisisonToken, registrationId,
								SafeSlingerConfig.NOTIFY_ANDROIDGCM);

						// update local active regisLinked
						if (result != null) {
							SafeSlingerPrefs.setPushRegistrationIdPosted(true);
						} else {
							SafeSlingerPrefs.setPushRegistrationId(null); // clear
							SafeSlingerPrefs.setPushRegistrationIdPosted(false); // reset
						}
					}
				}
			} catch (IOException e) {
				showNote(e, context);
			} catch (ClassNotFoundException e) {
				showNote(e, context);
			} catch (CryptoMsgException e) {
				showNote(e, context);
			} catch (ExchangeException e) {
				SafeSlingerPrefs.setPushRegistrationId(null); // clear
				SafeSlingerPrefs.setPushRegistrationIdPosted(false); // reset
				showNote(e, context);
			} catch (MessageNotFoundException e) {
				SafeSlingerPrefs.setPushRegistrationId(null); // clear
				SafeSlingerPrefs.setPushRegistrationIdPosted(false); // reset
				showNote(e, context);
			}

			// notify UI that registration is complete for now...
//			onRegistered(context, registrationId);
			return registrationId;
		}
		
		return "";
	}

	protected static void showNote(String msg, Context context) {
		MyLog.i(TAG, msg);
		Toast toast = Toast.makeText(context, msg.trim(), Toast.LENGTH_LONG);
		toast.show();
	}

	protected static void showNote(int resId, Context context) {
		showNote(context.getString(resId), context);
	}

	protected static void showNote(Exception e, Context context) {
		showNote(e.getLocalizedMessage(), context);
	}

}
