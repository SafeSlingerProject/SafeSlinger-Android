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

package edu.cmu.cylab.starslinger.util;

import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.view.HomeActivity;

public class NotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            int allCount = intent.getExtras().getInt(extra.NOTIFY_COUNT, 0);
            boolean giveNotificationFeedback = intent.getExtras().getBoolean(extra.NOTIFY_STATUS,
                    true);

            doUnseenMessagesNotification(context, allCount, giveNotificationFeedback);
        }
    }

    public static void doUnseenMessagesNotification(Context ctx, int msgCount)
            throws OutOfMemoryError {
        doUnseenMessagesNotification(ctx, msgCount, true);
    }

    @SuppressWarnings("deprecation")
    public static void doUnseenMessagesNotification(Context ctx, int msgCount,
            boolean giveNotificationFeedback) throws OutOfMemoryError {

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
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // the next two lines initialize the Notification, using the
        // configurations above
        int visibleMsgCount = msgCount != 1 ? msgCount : 0;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)//
                .setSmallIcon(R.drawable.ic_stat_notify_msg)//
                .setTicker(tickerText)//
                .setWhen(when)//
                .setAutoCancel(true)//
                .setContentTitle(contentTitle)//
                .setContentText(contentText)//
                .setNumber(visibleMsgCount)//
                .setVisibility(NotificationCompat.VISIBILITY_SECRET);

        // prevent the intent from canceling active key exchange
        if (!SafeSlinger.getApplication().isExchangeActive()) {
            builder.setContentIntent(contentIntent);
        }

        try {
            builder.setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(),
                    R.drawable.ic_launcher));
        } catch (OutOfMemoryError e) {
            // ignore icon when out of memory
        }

        // set notification alerts based on user preferences
        int defaults = 0;
        if (SafeSlingerPrefs.getNotificationVibrate() && giveNotificationFeedback) {
            defaults |= Notification.DEFAULT_VIBRATE;
        }
        String ringtoneStr = SafeSlingerPrefs.getNotificationRingTone();
        builder.setSound(TextUtils.isEmpty(ringtoneStr) || !giveNotificationFeedback ? null : Uri
                .parse(ringtoneStr));
        defaults |= Notification.DEFAULT_LIGHTS;
        builder.setDefaults(defaults);

        Notification n = builder.build();

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
        getFileIntent.setAction(SafeSlingerConfig.Intent.ACTION_MESSAGEINCOMING);
        return getFileIntent;
    }

}
