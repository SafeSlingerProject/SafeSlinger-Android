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

package edu.cmu.cylab.starslinger;

import java.util.Date;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.view.HomeActivity;

public class Service extends android.app.Service {
    private final IBinder mBinder = new LocalBinder();

    private long mPassPhraseCacheTtl = 15;
    private Handler mCacheHandler = new Handler();
    static private boolean mIsRunning = false;

    private BroadcastReceiver mAirplaneModeReceiver = new BroadcastReceiver() {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean airplaneOn = false;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                airplaneOn = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.AIRPLANE_MODE_ON, 0) != 0;
            } else {
                airplaneOn = Settings.Global.getInt(context.getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
            }

            // logout when in airplane mode
            if (airplaneOn) {
                SafeSlinger.removeCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
                stopForeground(true);
                System.exit(0);
            }
        }
    };

    private Runnable mCacheTask = new Runnable() {

        @Override
        public void run() {
            mPassPhraseCacheTtl = SafeSlingerPrefs.getPassPhraseCacheTtl();

            long delay = mPassPhraseCacheTtl * 1000 / 2;

            // make sure the delay is not longer than one minute
            if (delay > 60000) {
                delay = 60000;
            }

            delay = SafeSlinger.cleanUpCache(mPassPhraseCacheTtl, delay);
            // don't check too often, even if we were close
            if (delay < 5000) {
                delay = 5000;
            }

            // ensure service stops when passphrase is absent
            if (SafeSlinger.isCacheEmpty()) {
                stopForeground(true);
            }

            mCacheHandler.postDelayed(this, delay);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        mIsRunning = true;

        registerReceiver(mAirplaneModeReceiver, new IntentFilter(
                Intent.ACTION_AIRPLANE_MODE_CHANGED));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        mIsRunning = false;

        // attempt to unregister, however we can safely ignore the
        // "IllegalArgumentException: Receiver not registered" called when
        // some hardware experiences a race condition here.
        try {
            unregisterReceiver(mAirplaneModeReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        mPassPhraseCacheTtl = SafeSlingerPrefs.getPassPhraseCacheTtl();

        if (mPassPhraseCacheTtl < 15) {
            mPassPhraseCacheTtl = 15;
        }

        // ensure service runs when passphrase is present
        if (SafeSlinger.isCacheEmpty()) {
            stopForeground(true);
        } else {
            startForeground(HomeActivity.NOTIFY_PASS_CACHED_ID, createCacheNotification());
        }

        // only query exchange status on startup
        queryExchangeStatus();

        // immediate backup delay not critical, it's an optional service
        queryBackupStatus();

        mCacheHandler.removeCallbacks(mCacheTask);
        mCacheHandler.postDelayed(mCacheTask, 1000);
    }

    static public boolean isRunning() {
        return mIsRunning;
    }

    public class LocalBinder extends Binder {
        Service getService() {
            return Service.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void queryBackupStatus() {
        long now = new Date().getTime();
        long request = SafeSlingerPrefs.getBackupRequestDate();
        long delayed = (request) + SafeSlingerConfig.BACKUP_DELAY_WARN_MS;
        long complete = SafeSlingerPrefs.getBackupCompleteDate();
        boolean reminder = SafeSlingerPrefs.getRemindBackupDelay();
        boolean exchanged = false;

        // We don't need to give the first backup delayed reminder before the
        // first time a user has exchanged any keys
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this);
        int trustRecips = dbRecipient.getTrustedRecipientCount();
        if (trustRecips > 0) {
            SafeSlingerPrefs.setFirstExchangeComplete(true);
            exchanged = true;
        }

        if (request <= 0) {
            // if never backed up, begin a request...
            SafeSlinger.queueBackup();
        } else if (reminder && exchanged && request > complete && now > delayed) {
            // if backup delayed, send user notification...
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager nm = (NotificationManager) getSystemService(ns);
            Notification n = createBackupDelayNotification(request);
            nm.notify(HomeActivity.NOTIFY_BACKUP_DELAY_ID, n);

            // make another request for backup...
            SafeSlinger.queueBackup();
        }
    }

    private void queryExchangeStatus() {
        boolean reminder = SafeSlingerPrefs.getShowSlingKeysReminder();
        boolean exchanged = false;
        boolean setupComplete = !TextUtils.isEmpty(SafeSlingerPrefs.getKeyIdString());
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) getSystemService(ns);

        // update exchanged status for older versions
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this);
        int trustRecips = dbRecipient.getTrustedRecipientCount();
        if (trustRecips > 0) {
            SafeSlingerPrefs.setFirstExchangeComplete(true);
            exchanged = true;
        }

        // if keys exist, and recipients = 0, remind user to sling keys
        if (setupComplete && !exchanged && reminder) {
            Notification n = createSlingKeysReminderNotification();
            nm.notify(HomeActivity.NOTIFY_SLINGKEYS_REMIND_ID, n);
        } else {
            nm.cancel(HomeActivity.NOTIFY_SLINGKEYS_REMIND_ID);
        }

        // show once per query only
        SafeSlingerPrefs.setShowSlingKeysReminder(false);
    }

    public Notification createBackupDelayNotification(long request) {
        String tickerText = getString(R.string.label_SafeSlingerBackupDelayed);
        String contentTitle = getString(R.string.label_SafeSlingerBackupDelayed);
        String contentText = getString(R.string.label_TouchToConfigureBackupSettingsAndroid);
        Intent intent = new Intent(Service.this, HomeActivity.class);
        intent.setAction(SafeSlingerConfig.Intent.ACTION_BACKUPNOTIFY);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Context ctx = SafeSlinger.getApplication();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)//
                .setSmallIcon(R.drawable.ic_stat_notify_backup)//
                .setTicker(tickerText)//
                .setWhen(request)//
                .setAutoCancel(true)//
                .setContentTitle(contentTitle)//
                .setContentText(contentText)//
                .setVisibility(NotificationCompat.VISIBILITY_SECRET);

        // prevent the intent from canceling active key exchange
        if (!SafeSlinger.getApplication().isExchangeActive()) {
            builder.setContentIntent(contentIntent);
        }

        return builder.build();
    }

    public Notification createCacheNotification() {
        String timeout = Integer.toString(SafeSlingerPrefs.getPassPhraseCacheTtl());
        String[] entries = getResources().getStringArray(R.array.pass_phrase_cache_ttl_entries);
        String[] values = getResources().getStringArray(R.array.pass_phrase_cache_ttl_values);
        String setting = getString(R.string.label_undefinedTypeLabel);
        for (int i = 0; i < values.length; i++) {
            if (timeout.equals(values[i]) && entries.length == values.length) {
                setting = entries[i];
                break;
            }
        }

        String tickerText = getString(R.string.label_PassPhraseIsCached);
        String contentTitle = String.format("%s: %s", getString(R.string.label_PassPhraseIsCached),
                setting);
        String contentText = getString(R.string.label_TouchToConfigureCacheTimeout);
        Intent intent = new Intent(Service.this, HomeActivity.class);
        intent.setAction(SafeSlingerConfig.Intent.ACTION_CHANGESETTINGS);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Context ctx = SafeSlinger.getApplication();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)//
                .setSmallIcon(R.drawable.ic_stat_notify_cache)//
                .setTicker(tickerText)//
                .setContentTitle(contentTitle)//
                .setContentText(contentText)//
                .setWhen(0)//
                .setVisibility(NotificationCompat.VISIBILITY_SECRET);

        // prevent the intent from canceling active key exchange
        if (!SafeSlinger.getApplication().isExchangeActive()) {
            builder.setContentIntent(contentIntent);
        }

        return builder.build();
    }

    public Notification createSlingKeysReminderNotification() {
        String tickerText = String.format("%s %s", getString(R.string.app_name),
                getString(R.string.menu_TagExchange));
        String contentTitle = String.format("%s %s", getString(R.string.app_name),
                getString(R.string.menu_TagExchange));
        String contentText = getString(R.string.error_ReminderToSlingKeysToUseApp);
        Intent intent = new Intent(Service.this, HomeActivity.class);
        intent.setAction(SafeSlingerConfig.Intent.ACTION_SLINGKEYSNOTIFY);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Context ctx = SafeSlinger.getApplication();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)//
                .setSmallIcon(R.drawable.ic_stat_notify_msg)//
                .setAutoCancel(true)//
                .setTicker(tickerText)//
                .setContentTitle(contentTitle)//
                .setContentText(contentText)//
                .setWhen(0)//
                .setVisibility(NotificationCompat.VISIBILITY_SECRET);

        // prevent the intent from canceling active key exchange
        if (!SafeSlinger.getApplication().isExchangeActive()) {
            builder.setContentIntent(contentIntent);
        }

        return builder.build();
    }

}
