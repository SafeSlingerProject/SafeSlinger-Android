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

package edu.cmu.cylab.starslinger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.Application;
import android.app.backup.BackupManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.PRNGFixes;
import edu.cmu.cylab.starslinger.model.CachedPassPhrase;
import edu.cmu.cylab.starslinger.model.MessageDbAdapter;
import edu.cmu.cylab.starslinger.transaction.C2DMReceiver;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class SafeSlinger extends Application {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private static HashMap<String, CachedPassPhrase> mPassPhraseCache = new HashMap<String, CachedPassPhrase>();
    private static SafeSlinger sSafeSlinger = null;
    private ConnectivityManager mConnectivityManager;
    private static boolean sPassEntryOpen = false;

    @Override
    public void onCreate() {
        super.onCreate();

        sSafeSlinger = this;

        // Catching Unhandled Exceptions...
        // We used to use UncaughtExceptionHandler to catch exceptions here,
        // and send via email, however the android system has such a good
        // feedback mechanism, that already takes pains to protect user privacy
        // we prefer now to use the default method and let users decide when
        // to submit anonymous error data that way.

        // when legacy crypto has been deprecated, apply PRNG fixes from
        // http://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html
        if (CryptTools.existsSecretKey(getApplicationContext())) {
            PRNGFixes.apply();
        }

        // alert for pending messages
        checkForPendingMessages();

        // manage preferences...
        SafeSlingerPrefs.removePrefDeprecated();
        SafeSlingerPrefs.setPusgRegBackoff(SafeSlingerPrefs.DEFAULT_PUSHREG_BACKOFF);
    }

    private void checkForPendingMessages() throws SQLException {
        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this);
        int msgCount = dbMessage.fetchUnseenMessageCount();
        if (msgCount > 0) {
            C2DMReceiver.doUnseenMessagesNotification(this, msgCount);
        }
    }

    synchronized public static SafeSlinger getApplication() {
        return sSafeSlinger;
    }

    public boolean isOnline() {
        boolean connected = false;
        if (mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) sSafeSlinger
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        connected = networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
        return connected;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void copyPlainTextToClipboard(String string) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(string);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(
                    getString(R.string.app_name), string);
            clipboard.setPrimaryClip(clip);
        }
    }

    // static methods to manage static variables...

    public static void startCacheService(Activity activity) {
        Intent intent = new Intent(activity, Service.class);
        intent.putExtra(Service.EXTRA_TTL, SafeSlingerPrefs.getPassPhraseCacheTtl());
        activity.startService(intent);
    }

    public static void setCachedPassPhrase(String keyId, String passPhrase) {
        mPassPhraseCache.put(keyId, new CachedPassPhrase(new Date().getTime(), passPhrase));
    }

    public static void removeCachedPassPhrase(String keyId) {
        mPassPhraseCache.remove(keyId);
    }

    public static String getCachedPassPhrase(String keyId) {
        String realId = keyId;
        CachedPassPhrase cpp = mPassPhraseCache.get(realId);
        if (cpp != null) {
            // set it again to reset the cache life cycle
            setCachedPassPhrase(realId, cpp.passPhrase);
            return cpp.passPhrase;
        }
        return null;
    }

    public static void updateCachedPassPhrase(String keyId) {
        String realId = keyId;
        CachedPassPhrase cpp = mPassPhraseCache.get(realId);
        if (cpp != null) {
            // set it again to reset the cache life cycle
            setCachedPassPhrase(realId, cpp.passPhrase);
        }
    }

    public static boolean isCacheEmpty() {
        return mPassPhraseCache.size() == 0;
    }

    public static long cleanUpCache(long mPassPhraseCacheTtl, long delay) {
        long realTtl = mPassPhraseCacheTtl * 1000;
        long now = new Date().getTime();
        Vector<String> oldKeys = new Vector<String>();
        for (Map.Entry<String, CachedPassPhrase> pair : mPassPhraseCache.entrySet()) {
            long lived = now - pair.getValue().timestamp;
            if (lived >= realTtl) {
                oldKeys.add(pair.getKey());
            } else {
                // see whether the remaining time for this cache entry improves
                // our check delay
                long nextCheck = realTtl - lived + 1000;
                if (nextCheck < delay) {
                    delay = nextCheck;
                }
            }
        }

        for (String keyId : oldKeys) {
            mPassPhraseCache.remove(keyId);
        }

        return delay;
    }

    public void showFeedbackEmail(Activity act) {
        StringBuilder output = new StringBuilder();
        SafeSlinger.getDebugData(act, output);

        // Walk up all the way to the root thread group
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parent;
        while ((parent = rootGroup.getParent()) != null) {
            rootGroup = parent;
        }
        SafeSlinger.listThreads(rootGroup, "", output);

        String filePath = SSUtil.makeDebugLoggingDir(this) + File.separator
                + SafeSlingerConfig.FEEDBACK_TXT;

        sendEmail(output.toString(), filePath);
    }

    public void sendEmail(String feedback, String filePath) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {
            SafeSlingerConfig.HELP_EMAIL
        });
        intent.putExtra(Intent.EXTRA_SUBJECT, String.format("%s (%s %s)",
                getString(R.string.title_comments), getString(R.string.label_AndroidOS),
                SafeSlingerConfig.getVersionName()));

        try {
            BufferedWriter bos = new BufferedWriter(new FileWriter(filePath));
            bos.write(feedback);
            bos.flush();
            bos.close();
            Uri uri = Uri.fromFile(new File(filePath));
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // If there is nothing that can send a text/html MIME type
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    public static void getDebugData(Context ctx, StringBuilder output) {
        String deviceId = Settings.Secure.getString(ctx.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        output.append("\n").append("app version: " + SafeSlingerConfig.getVersionName());

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.US);
        df.setTimeZone(TimeZone.getDefault());
        String localTime = df.format(new Date());
        output.append("\n").append("local time: " + localTime);

        df.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        String supportTime = df.format(new Date());

        output.append("\n").append("support time: " + supportTime);
        output.append("\n").append("android api level: " + Build.VERSION.SDK_INT);
        output.append("\n").append("android version: " + Build.VERSION.RELEASE);
        output.append("\n").append("build: " + (SafeSlingerConfig.isDebug() ? "debug" : "release"));
        output.append("\n").append("locale: " + Locale.getDefault().getDisplayName(Locale.US));
        output.append("\n").append("device id: " + deviceId);

        output.append("\n");
        output.append("\n").append("BOARD: " + Build.BOARD);
        output.append("\n").append("BOOTLOADER: " + Build.BOOTLOADER);
        output.append("\n").append("BRAND: " + Build.BRAND);
        output.append("\n").append("CPU_ABI: " + Build.CPU_ABI);
        output.append("\n").append("CPU_ABI2: " + Build.CPU_ABI2);
        output.append("\n").append("DEVICE: " + Build.DEVICE);
        output.append("\n").append("DISPLAY: " + Build.DISPLAY);
        output.append("\n").append("FINGERPRINT: " + Build.FINGERPRINT);
        output.append("\n").append("HARDWARE: " + Build.HARDWARE);
        output.append("\n").append("HOST: " + Build.HOST);
        output.append("\n").append("ID: " + Build.ID);
        output.append("\n").append("MANUFACTURER: " + Build.MANUFACTURER);
        output.append("\n").append("MODEL: " + Build.MODEL);
        output.append("\n").append("PRODUCT: " + Build.PRODUCT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            output.append("\n").append("RADIO: " + Build.getRadioVersion());
        } else {
            output.append("\n").append("RADIO: " + Build.RADIO);
        }
        output.append("\n").append("TAGS: " + Build.TAGS);
        output.append("\n").append("TIME: " + new Date(Build.TIME));
        output.append("\n").append("TYPE: " + Build.TYPE);
        output.append("\n").append("UNKNOWN: " + Build.UNKNOWN);
        output.append("\n").append("USER: " + Build.USER);

        output.append("\n");
        ActivityManager activityManager = (ActivityManager) getApplication().getSystemService(
                ACTIVITY_SERVICE);
        MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        output.append("\n");
        output.append("Memory Available: " + memoryInfo.availMem + "\n");
        output.append("Memory Low: " + memoryInfo.lowMemory + "\n");
        output.append("Memory Threshold: " + memoryInfo.threshold + "\n");

        output.append("\n");
    }

    public static boolean isLoggable() {
        return SafeSlingerConfig.isDebug();
    }

    synchronized public static void setPassphraseOpen(boolean passEntryOpen) {
        sPassEntryOpen = passEntryOpen;
    }

    synchronized public static boolean isPassphraseOpen() {
        return sPassEntryOpen;
    }

    /***
     * List all threads and recursively list all subgroup
     */
    private static void listThreads(ThreadGroup group, String indent, StringBuilder output) {
        output.append("\n");
        output.append(indent + "Group[" + group.getName() + ":" + group.getClass() + "]").append(
                "\n");

        // TODO improve accuracy of nt
        int nt = group.activeCount();
        Thread[] threads = new Thread[nt * 2 + 10];
        nt = group.enumerate(threads, false);

        // List every thread in the group
        for (int i = 0; i < nt; i++) {
            Thread t = threads[i];
            output.append("\n");
            output.append(indent + "  Thread[" + t.getName() + ":" + t.getClass() + "]").append(
                    "\n");
            for (StackTraceElement ste : t.getStackTrace()) {
                output.append(indent + "  " + ste).append("\n");
            }
        }

        // Recursively list all subgroups
        int ng = group.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[ng * 2 + 10];
        ng = group.enumerate(groups, false);

        for (int i = 0; i < ng; i++) {
            listThreads(groups[i], indent + "  ", output);
        }
    }

    public static String writeByteArray(byte[] bytes) {
        StringBuilder raw = new StringBuilder(String.format(Locale.US, "len %d: ", bytes.length));
        for (int i = 0; i < bytes.length; i++)
            raw.append(String.format(Locale.US, "%X ", bytes[i]));
        return raw.toString();
    }

    public static void queueBackup() {
        Context ctx = SafeSlinger.getApplication();
        BackupManager bm = new BackupManager(ctx);
        bm.dataChanged();
        SafeSlingerPrefs.setBackupRequestDate(new Date().getTime());
    }
}
