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
import java.util.ArrayList;
import java.util.Arrays;
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
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPacketSizeException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider;
import edu.cmu.cylab.starslinger.crypto.PRNGFixes;
import edu.cmu.cylab.starslinger.model.CachedPassPhrase;
import edu.cmu.cylab.starslinger.model.InboxDbAdapter;
import edu.cmu.cylab.starslinger.model.MessageData;
import edu.cmu.cylab.starslinger.model.MessageDbAdapter;
import edu.cmu.cylab.starslinger.model.MessagePacket;
import edu.cmu.cylab.starslinger.model.MessageRow;
import edu.cmu.cylab.starslinger.transaction.C2DMReceiver;
import edu.cmu.cylab.starslinger.transaction.MessageNotFoundException;
import edu.cmu.cylab.starslinger.transaction.WebEngine;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class SafeSlinger extends Application {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private static HashMap<String, CachedPassPhrase> mPassPhraseCache = new HashMap<String, CachedPassPhrase>();
    private static SafeSlinger sSafeSlinger = null;
    private ConnectivityManager mConnectivityManager;
    private static boolean sPassEntryOpen = false;
    private static boolean sAppVisible;
    private static Handler sHandler;
    private Locale locale = null;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (locale != null) {
            Locale.setDefault(locale);
            Configuration config = new Configuration(newConfig);
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }
    }

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

        updateLanguage(SafeSlingerPrefs.getLanguage());

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

        startCacheService(this);
    }

    private void checkForPendingMessages() throws SQLException {
        // only fetch encrypted, decrypted will always be locked on startup
        InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(this);
        int inCount = dbInbox.getUnseenInboxCount();
        if (inCount > 0) {
            C2DMReceiver.doUnseenMessagesNotification(this, inCount);
        }

        // attempt to retry pending message downloads
        checkForMissedMessages();
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

    public static void startCacheService(Context ctx) {
        Intent intent = new Intent(ctx, Service.class);
        intent.putExtra(extra.PASSPHRASE_CACHE_TTL, SafeSlingerPrefs.getPassPhraseCacheTtl());
        ctx.startService(intent);
    }

    public static void setCachedPassPhrase(String keyId, String passPhrase) {
        mPassPhraseCache.put(keyId, new CachedPassPhrase(new Date().getTime(), passPhrase));
    }

    public static void removeCachedPassPhrase(String keyId) {
        mPassPhraseCache.remove(keyId);
    }

    public static long getPassPhraseCacheTimeRemaining(String keyId) {
        String realId = keyId;
        CachedPassPhrase cpp = mPassPhraseCache.get(realId);
        if (cpp != null) {
            long now = new Date().getTime();
            long timeout = SafeSlingerPrefs.getPassPhraseCacheTtl() * 1000;
            long lived = now - cpp.timestamp;
            long remain = timeout - lived;

            // add 2 seconds to allow service to run cache cleanup
            return remain > 0 ? remain + 2000 : 0;
        } else {
            return 0;
        }
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

    synchronized public static boolean isAppVisible() {
        return sAppVisible;
    }

    synchronized public static void activityResumed() {
        sAppVisible = true;
    }

    synchronized public static void activityPaused() {
        sAppVisible = false;
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

    public static int getTotalUsers() {
        String userName;
        long keyDate;
        int userNumber = 0;
        do {
            userName = null;
            keyDate = 0;
            userName = SafeSlingerPrefs.getContactName(userNumber);
            keyDate = SafeSlingerPrefs.getKeyDate(userNumber);
            if (!TextUtils.isEmpty(userName)) {
                userNumber++;
            }
        } while (keyDate > 0);

        // always at least 1
        return userNumber > 1 ? userNumber : 1;
    }

    public void checkForMissedMessages() {
        SafeSlingerPrefs
                .setPendingGetMessageBackoff(SafeSlingerPrefs.DEFAULT_PENDING_GETMSG_BACKOFF);
        if (sHandler == null) {
            sHandler = new Handler();
        }
        sHandler.removeCallbacks(getPendingMessageDownloads);
        sHandler.postDelayed(getPendingMessageDownloads,
                SafeSlingerPrefs.getPendingGetMessageBackoff());
    }

    private Runnable getPendingMessageDownloads = new Runnable() {

        @Override
        public void run() {
            runThreadGetPendingMessages();
        }
    };

    public void runThreadGetPendingMessages() {
        Thread t = new Thread() {

            @Override
            public void run() {
                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
                InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(getApplicationContext());
                WebEngine web = new WebEngine(getApplication(),
                        SafeSlingerConfig.HTTPURL_MESSENGER_HOST);
                int pendingDownloads = 0;
                int successDownloads = 0;

                Cursor c = dbInbox.fetchAllInboxGetMessagePending();
                if (c != null) {
                    pendingDownloads = c.getCount();
                    while (c.moveToNext()) {
                        try {
                            MessageData inRow = new MessageRow(c, true);

                            if (!SafeSlinger.getApplication().isOnline()) {
                                break;
                            }

                            byte[] msgHashBytes;
                            try {
                                msgHashBytes = Base64.decode(inRow.getMsgHash().getBytes(),
                                        Base64.NO_WRAP);
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                                break;
                            }

                            byte[] resp = null;
                            try {
                                resp = web.getMessage(msgHashBytes);
                            } catch (MessageNotFoundException e) {
                                if (!dbInbox.updateInboxExpired(inRow.getRowId())) {
                                    break;
                                }
                            }

                            if (resp == null || resp.length == 0) {
                                break;
                            }

                            byte[] encMsg = WebEngine.parseMessageResponse(resp);
                            if (encMsg == null) {
                                break;
                            }

                            if (Arrays.equals(msgHashBytes, CryptTools.computeSha3Hash(encMsg))) {
                                CryptoMsgProvider tool = CryptoMsgProvider
                                        .createInstance(SafeSlinger.isLoggable());
                                String keyid = null;
                                try {
                                    keyid = tool.ExtractKeyIDfromPacket(encMsg);
                                } catch (CryptoMsgPacketSizeException e) {
                                    e.printStackTrace();
                                }
                                // save downloaded initial message
                                if (!dbInbox.updateInboxDownloaded(inRow.getRowId(), encMsg,
                                        MessageDbAdapter.MESSAGE_IS_SEEN, keyid)) {
                                    break;
                                }

                                // got it! let the user know it's here...
                                successDownloads++;
                                C2DMReceiver.doUnseenMessagesNotification(
                                        SafeSlinger.getApplication(), successDownloads);

                            } else {
                                break;
                            }

                            String pass = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs
                                    .getKeyIdString());

                            // if requested, and logged in, try to decrypt
                            // message
                            if (!TextUtils.isEmpty(pass) && SafeSlingerPrefs.getAutoDecrypt()) {
                                StringBuilder keyidout = new StringBuilder();
                                byte[] plain = CryptTools.decryptMessage(inRow.getEncBody(), pass,
                                        keyidout);
                                MessagePacket push = new MessagePacket(plain);

                                // move encrypted message to decrypted
                                // storage...
                                // add decrypted
                                long rowIdMsg = dbMessage.createMessageDecrypted(inRow, push,
                                        keyidout.toString());
                                if (rowIdMsg == -1) {
                                    return; // unable to save progress
                                } else {
                                    // remove encrypted
                                    dbInbox.deleteInbox(inRow.getRowId());
                                }
                            }
                        } catch (OutOfMemoryError e) {
                            e.printStackTrace();
                        } catch (ExchangeException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (CryptoMsgException e) {
                            e.printStackTrace();
                        } catch (GeneralException e) {
                            e.printStackTrace();
                        }
                    }
                    c.close();
                }

                // if we missed any downloads, set to try again using
                // exponential backoff later as long as message might be
                // available
                if (sHandler == null) {
                    sHandler = new Handler();
                }
                sHandler.removeCallbacks(getPendingMessageDownloads);
                long backoff = SafeSlingerPrefs.getPendingGetMessageBackoff();
                if (pendingDownloads > successDownloads
                        && backoff < SafeSlingerConfig.MESSAGE_EXPIRATION_MS) {
                    // check later
                    long newBackoff = backoff * 2;
                    SafeSlingerPrefs.setPendingGetMessageBackoff(newBackoff);
                    sHandler.postDelayed(getPendingMessageDownloads, newBackoff);
                } else {
                    // remove checking
                    SafeSlingerPrefs
                            .setPendingGetMessageBackoff(SafeSlingerPrefs.DEFAULT_PENDING_GETMSG_BACKOFF);
                }
            }
        };
        t.start();
    }

    public void updateLanguage(String language) {
        Configuration config = getBaseContext().getResources().getConfiguration();

        // TODO: this could be handling region changes better
        if (!TextUtils.isEmpty(language) && !config.locale.getLanguage().equals(language)
                && !language.equals(SafeSlingerPrefs.DEFAULT_LANGUAGE)) {
            // set local app config to alternate language
            SafeSlingerPrefs.setLanguage(language);

            String[] loc = language.split("_");
            if (loc.length > 1) {
                locale = new Locale(loc[0], loc[1]);
            } else {
                locale = new Locale(language);
            }

            Locale.setDefault(locale);
            Configuration conf = new Configuration(config);
            conf.locale = locale;
            getBaseContext().getResources().updateConfiguration(conf,
                    getBaseContext().getResources().getDisplayMetrics());
        } else {
            // set local app config to use default system language
            SafeSlingerPrefs.setLanguage(SafeSlingerPrefs.DEFAULT_LANGUAGE);

            Locale.setDefault(config.locale);
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());

        }
    }

    public ArrayList<String> getListLanguages(boolean returnEntries) {
        // filter available languages in case this version of android does not
        // render them well or at all
        Locale[] instLoc = Locale.getAvailableLocales();
        String[] appCode = getResources().getStringArray(R.array.language_values);
        String[] appLang = getResources().getStringArray(R.array.language_entries);
        ArrayList<String> showCode = new ArrayList<String>();
        ArrayList<String> showLang = new ArrayList<String>();

        // TODO: filter on rendering capability to show more languages
        showCode.add(SafeSlingerPrefs.DEFAULT_LANGUAGE);
        showLang.add(getString(R.string.choice_default));
        for (int i = 0; i < appCode.length; i++) {
            for (Locale inst : instLoc) {
                if (appCode[i].startsWith(inst.getLanguage())) {
                    showCode.add(appCode[i]);
                    showLang.add(appLang[i]);
                    break;
                }
            }
        }
        if (returnEntries) {
            return showCode;
        } else {
            return showLang;
        }
    }
}
