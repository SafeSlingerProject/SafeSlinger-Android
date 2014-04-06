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

import java.util.Date;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.widget.Toast;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;

/**
 * Base class for C2D message receiver. Includes constants for the strings used
 * in the protocol.
 */
public abstract class C2DMBaseReceiver extends IntentService {
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    private static final String C2DM_RETRY = "com.google.android.c2dm.intent.RETRY";

    public static final String REGISTRATION_CALLBACK_INTENT = "com.google.android.c2dm.intent.REGISTRATION";
    public static final String C2DM_INTENT = "com.google.android.c2dm.intent.RECEIVE";

    // Extras in the registration callback intents.
    public static final String EXTRA_UNREGISTERED = "unregistered";

    public static final String EXTRA_ERROR = "error";

    public static final String EXTRA_REGISTRATION_ID = "registration_id";

    /**
     * The device can't read the response, or there was a 500/503 from the
     * server that can be retried later. The application should use exponential
     * back off and retry.
     */
    public static final String ERRREG_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE";

    /**
     * There is no Google account on the phone. The application should ask the
     * user to open the account manager and add a Google account. Fix on the
     * device side.
     */
    public static final String ERRREG_ACCOUNT_MISSING = "ACCOUNT_MISSING";

    /**
     * Bad password. The application should ask the user to enter his/her
     * password, and let user retry manually later. Fix on the device side.
     */
    public static final String ERRREG_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";

    /**
     * The user has too many applications registered. The application should
     * tell the user to uninstall some other applications, let user retry
     * manually. Fix on the device side.
     */
    public static final String ERRREG_TOO_MANY_REGISTRATIONS = "TOO_MANY_REGISTRATIONS";

    /**
     * The sender account is not recognized.
     */
    public static final String ERRREG_INVALID_SENDER = "INVALID_SENDER";

    /**
     * Incorrect phone registration with Google. This phone doesn't currently
     * support C2DM.
     */
    public static final String ERRREG_PHONE_REGISTRATION_ERROR = "PHONE_REGISTRATION_ERROR";

    public static final String ERRMSG_ERROR_PREFIX = "Error=";

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

    private static PowerManager.WakeLock mWakeLock;
    private final String mSenderId;

    /**
     * The C2DMReceiver class must create a no-arg constructor and pass the
     * sender id to be used for registration.
     */
    public C2DMBaseReceiver(String senderId) {
        // senderId is used as base name for threads, etc.
        super(senderId);
        mSenderId = senderId;
    }

    /**
     * Called when a cloud message has been received.
     */
    protected abstract void onMessage(Context context, Intent intent);

    /**
     * Called on registration error. Override to provide better error messages.
     * This is called in the context of a Service - no dialog or UI.
     */
    public abstract void onError(Context context, String errorId);

    /**
     * Called when a registration token has been received.
     */
    public void onRegistered(Context context, String registrationId) {
        // nothing special, handleRegistration() is doing the work for now.
    }

    /**
     * Called when the device has been unregistered.
     */
    public void onUnregistered(Context context) {
        // nothing special, handleRegistration() is doing the work for now.
    }

    @Override
    public final void onHandleIntent(Intent intent) {
        try {
            Context context = getApplicationContext();
            if (intent.getAction().equals(REGISTRATION_CALLBACK_INTENT)) {
                handleRegistration(context, intent);
            } else if (intent.getAction().equals(C2DM_INTENT)) {
                onMessage(context, intent);
            } else if (intent.getAction().equals(C2DM_RETRY)) {
                C2DMessaging.register(context, mSenderId);
            }
        } finally {
            // Release the power lock, so phone can get back to sleep.
            // The lock is reference counted by default, so multiple
            // messages are OK.

            // If the onMessage() needs to spawn a thread or do something else,
            // it should use it's own lock.
            if (mWakeLock != null) {
                mWakeLock.release();
            }
        }
    }

    /**
     * Called from the broadcast receiver. Will process the received intent,
     * call handleMessage(), registered(), etc. in background threads, with a
     * wake lock, while keeping the service alive.
     */
    static void runIntentInService(Context context, Intent intent) {
        if (mWakeLock == null) {
            // This is called from BroadcastReceiver, there is no init.
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);
        }
        mWakeLock.acquire();

        // Use a naming convention, similar with how permissions and intents are
        // used. Alternatives are introspection or an ugly use of statics.
        String receiver = C2DMReceiver.class.getName();
        intent.setClassName(context, receiver);

        context.startService(intent);

    }

    private void handleRegistration(final Context context, Intent intent) {
        String registrationId = intent.getStringExtra(EXTRA_REGISTRATION_ID);
        String unregistered = intent.getStringExtra(EXTRA_UNREGISTERED);
        String error = intent.getStringExtra(EXTRA_ERROR);

        MyLog.d(TAG, "dmControl: registrationId = " + registrationId + ", error = " + error
                + ", removed = " + unregistered);

        if (unregistered != null) {
            // Remember we are unregistered
            SafeSlingerPrefs.setPushRegistrationIdWriteOnlyC2dm(null); // clear
            SafeSlingerPrefs.setPusgRegBackoff(SafeSlingerPrefs.DEFAULT_PUSHREG_BACKOFF);
            onUnregistered(context);
            return;
        } else if (error != null) {
            // we are not registered, can try again
            SafeSlingerPrefs.setPushRegistrationIdWriteOnlyC2dm(null); // clear
            // Registration failed
            MyLog.e(TAG, "push reg error: " + error);

            onError(context, error);

            // retry registration according to recommendations...
            if (error.equals(ERRREG_SERVICE_NOT_AVAILABLE)) {
                long backoff = SafeSlingerPrefs.getPusgRegBackoff();

                Intent retryIntent = new Intent(C2DM_RETRY);
                PendingIntent retryPIntent = PendingIntent.getBroadcast(context,
                        0 /* requestCode */, retryIntent, 0 /* flags */);

                Date futureDate = new Date(new Date().getTime() + backoff);
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, futureDate.getTime(), retryPIntent);

                // Next retry should wait longer.
                backoff *= 2;
                SafeSlingerPrefs.setPusgRegBackoff(backoff);
            }
        } else {
            SafeSlingerPrefs.setPushRegistrationIdWriteOnlyC2dm(registrationId);
            SafeSlingerPrefs.setPusgRegBackoff(SafeSlingerPrefs.DEFAULT_PUSHREG_BACKOFF);
            onRegistered(context, registrationId);
        }
    }

    protected void showNote(String msg) {
        MyLog.i(TAG, msg);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }

    protected void showNote(int resId) {
        MyLog.i(TAG, getString(resId));
        Toast toast = Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_LONG);
        toast.show();
    }
}
