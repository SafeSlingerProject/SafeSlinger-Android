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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Base64;
import edu.cmu.cylab.starslinger.crypto.CryptTools;

public class SafeSlingerPrefs {

    public static final int DEFAULT_CURRENT_USER = 0;
    public static final long DEFAULT_PUSHREG_BACKOFF = 5000;
    public static final long DEFAULT_PENDING_GETMSG_BACKOFF = 1000;
    public static final long DEFAULT_PASSPHRASE_BACKOFF = 1000;
    private static final int DEFAULT_PPCACHETTL = (5 * 60);
    private static final boolean DEFAULT_SHOW_WALKTHROUGH = true;
    private static final boolean DEFAULT_EULA_ACCEPTED = false;
    private static final boolean DEFAULT_REMINDBACKUPDELAY = true;
    private static final boolean DEFAULT_FIRST_EXCH_COMPLETE = false;
    private static final boolean DEFAULT_SHOW_REMINDSLINGKEYS = true;
    private static final int DEFAULT_FONT_SIZE = 16;
    private static final boolean DEFAULT_NOTIFICATION_VIBRATE = true;
    private static final String DEFAULT_RINGTONE = "content://settings/system/notification_sound";
    private static final String DEFAULT_FILEMANAGER_ROOTDIR = Environment
            .getExternalStorageDirectory().getAbsolutePath();
    private static final String DEFAULT_DOWNLOAD_DIR = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    private static final boolean DEFAULT_AUTO_DECRYPT = true;
    private static final boolean DEFAULT_AUTO_RETRIEVAL = false;
    public static final String DEFAULT_LANGUAGE = "zz";
    private static final boolean DEFAULT_PUSH_REG_ID_POSTED = false;

    public static final int NOTIFICATION_SLEEP_PERIOD = 5 * 1000;
    /**
     * This is the volume at which to play the in-conversation notification
     * sound, expressed as a fraction of the system notification volume.
     */
    public static final float IN_CONVERSATION_NOTIFICATION_VOLUME = 0.25f;

    /***
     * internal long term storage...
     */
    public static final class pref {
        @Deprecated
        public static final String CONTACT_EMAIL = "ContactEmail";
        @Deprecated
        public static final String CONTACT_EMAIL_TYPE = "ContactEmailType";
        @Deprecated
        public static final String CONTACT_ID = "ContactID";
        @Deprecated
        public static final String CONTACT_PHONE = "ContactPhone";
        @Deprecated
        public static final String CONTACT_PHONE_TYPE = "ContactPhoneType";
        @Deprecated
        public static final String PUSH_REG_ID_C2DM_INDIRECT = "PushRegistrationId"; // pre-1.5.2
        @Deprecated
        public static final String PUSH_REG_ID_C2DM_DIRECT = "PushRegistrationIdDirect"; // pre-1.8.2
        @Deprecated
        public static final String PUSH_REG_ID_C2DM_DIRECT_1811 = PUSH_REG_ID_C2DM_DIRECT
                + "1.8.1.1";
        @Deprecated
        public static final String PUSH_REG_ID_C2DM_DIRECT_1812 = PUSH_REG_ID_C2DM_DIRECT
                + "1.8.1.2";

        public static final String ACCOUNT_NAME = "AccountName";
        public static final String ACCOUNT_TYPE = "AccountType";
        public static final String AUTO_DECRYPT = "autoDecrypt";
        public static final String AUTO_RETRIEVAL = "autoRetrieval";
        public static final String BACKUP_COMPLETE_DATE = "backupCompleteDate";
        public static final String BACKUP_REQUEST_DATE = "backupRequestDate";
        public static final String CHANGE_PASSPHRASE = "changePassphrase";
        public static final String CONTACT_DB_LAST_SCAN = "contactDBLastScan";
        public static final String CONTACT_LOOKUP_KEY = "ContactLookupKey";
        public static final String CONTACT_NAME = "ContactName";
        public static final String CURR_MSG_DB_VER = "CurrentMessageDBVer";
        public static final String CURR_RECIP_DB_VER = "CurrentRecipientDBVer";
        public static final String CURRENT_USER = "CurrentUser";
        public static final String DOWNLOAD_DIRECTORY = "downloadDirectory";
        public static final String EULA_ACCEPTED = "eula20140106.accepted";
        public static final String FILEMANAGER_ROOTDIR = "fileManagerRootDir";
        public static final String FIRST_EXCH_COMPLETE = "firstExchangeComplete";
        public static final String FONT_SIZE = "fontSize172";
        public static final String HAS_SEEN_HELP = "seenHelp";
        public static final String KEYDATE = "KeyDate";
        public static final String KEYHARDNESS = "KeyHardness";
        public static final String KEYID_STRING = "KeyIdString";
        public static final String KEYSALT = "KeySalt";
        public static final String LANGUAGE = "language";
        public static final String LAST_MSG_STAMP = "lastMessageStamp";
        public static final String LOGOUT = "logout";
        public static final String MANAGE_PASSPHRASE = "managePassphrase";
        public static final String NEXT_PASS_ATTEMPT_DATE = "nextPassAttemptDate";
        public static final String NOTIFICATION_RINGTONE = "ringtone_notification";
        public static final String NOTIFICATION_VIBRATE = "vibrate_notification";
        public static final String PASS_BACKOFF_TIMEOUT = "passBackoffTimeout";
        public static final String PASSPHRASE_CACHE_TTL = "passPhraseCacheTtl";
        public static final String PENDING_GETMSG_BACKOFF_TIMEOUT = "PendingGetMessageBackoff";
        public static final String PUSH_BACKOFF = "PushBackoff";
        public static final String PUSH_REG_ID_LINKED = "PushRegistrationIdLinked";
        public static final String PUSH_REG_ID_LINKED_DISPLAY = "PushRegistrationIdLinkedDisplay";
        public static final String PUSH_REG_ID_POSTED = "PushRegistrationIdPosted";
        public static final String PUSHREG_BACKOFF_TIMEOUT = "pushRegistrationBackoff";
        public static final String REMIND_BACKUP_DELAY = "RemindBackupDelay";
        public static final String RESTORE_COMPLETE_DATE = "restoreCompleteDate";
        public static final String SHOW_ABOUT = "showAbout";
        public static final String SHOW_LICENSE = "showLicense";
        public static final String SHOW_PRIVACY = "showPrivacy";
        public static final String SHOW_SLING_KEYS_REMIND = "showSlingKeysReminder";
        public static final String SHOW_WALKTHROUGH = "ShowWalkthrough";
        public static final String TEMPKEY_SYNCACCOUNT_LIST = "keyTempListContactSyncAccount";
    }

    public static final String PREFS_RECOVER_YES = "MyPrefsFile";
    public static final String PREFS_RECOVER_NO = "MyPrefsNoRecoveryFile";
    public static final String VERSION_OPEN_SUFFIX = ".version.opened";

    public static void restoreUserDefaultSettings() {
        setShowWalkthrough(DEFAULT_SHOW_WALKTHROUGH);
        setPassPhraseCacheTtl(DEFAULT_PPCACHETTL);
        setRemindBackupDelay(DEFAULT_REMINDBACKUPDELAY);
        setDownloadDir(DEFAULT_DOWNLOAD_DIR);
        setFileManagerRootDir(DEFAULT_FILEMANAGER_ROOTDIR);
        setFontSize(DEFAULT_FONT_SIZE);
        setLanguage(DEFAULT_LANGUAGE);
        setNotificationRingTone(DEFAULT_RINGTONE);
        setNotificationVibrate(DEFAULT_NOTIFICATION_VIBRATE);
        setAutoDecrypt(DEFAULT_AUTO_DECRYPT);
        setAutoRetrieval(DEFAULT_AUTO_RETRIEVAL);
        setLastTimeStamp(0);
    }

    public static void removePrefDeprecated() {
        removePref(pref.CONTACT_EMAIL, true);
        removePref(pref.CONTACT_EMAIL_TYPE, true);
        removePref(pref.CONTACT_PHONE, true);
        removePref(pref.CONTACT_PHONE_TYPE, true);
    }

    public static void deletePrefs(int userNumber) {
        removePref(pref.ACCOUNT_NAME + userNumber, true);
        removePref(pref.ACCOUNT_TYPE + userNumber, true);
        removePref(pref.AUTO_DECRYPT + userNumber, true);
        removePref(pref.AUTO_RETRIEVAL + userNumber, true);
        removePref(pref.CONTACT_LOOKUP_KEY + userNumber, true);
        removePref(pref.CONTACT_NAME + userNumber, true);
        removePref(pref.FONT_SIZE + userNumber, true);
        removePref(pref.LANGUAGE + userNumber, true);
        removePref(pref.KEYDATE + userNumber, true);
        removePref(pref.KEYHARDNESS + userNumber, true);
        removePref(pref.KEYID_STRING + userNumber, true);
        removePref(pref.KEYSALT + userNumber, true);
        removePref(pref.PASSPHRASE_CACHE_TTL + userNumber, true);
        removePref(pref.LAST_MSG_STAMP, false);
    }

    // persist to backup account...

    public static boolean getHashContactField(String field) {
        return getBoolean(new String(CryptTools.computeSha3Hash(getUserKeyName(field).getBytes())),
                true, true);
    }

    public static void setHashContactField(String field, boolean checked) {
        setBoolean(new String(CryptTools.computeSha3Hash(getUserKeyName(field).getBytes())),
                checked, true);
    }

    public static void migrateContactField(String field) {
        boolean recover = true;
        Context ctx = SafeSlinger.getApplication();
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        if (settings.contains(getUserKeyName(field))) {
            boolean checked = getBoolean(getUserKeyName(field), true, recover);
            removePref(getUserKeyName(field), recover); // old
            setHashContactField(getUserKeyName(field), checked); // new
        }
    }

    public static String getContactLookupKey() {
        return getString(getUserKeyName(pref.CONTACT_LOOKUP_KEY), null, true);
    }

    public static void setContactLookupKey(String lookupKey) {
        setString(getUserKeyName(pref.CONTACT_LOOKUP_KEY), lookupKey, true);
    }

    public static String getContactName() {
        return getString(getUserKeyName(pref.CONTACT_NAME), null, true);
    }

    public static String getContactName(int userNumber) {
        return getString(getUserKeyName(pref.CONTACT_NAME, userNumber), null, true);
    }

    public static void setContactName(String ContactName) {
        setString(getUserKeyName(pref.CONTACT_NAME), ContactName, true);
    }

    public static String getAccountName() {
        return getString(getUserKeyName(pref.ACCOUNT_NAME), null, true);
    }

    public static void setAccountName(String accountName) {
        setString(getUserKeyName(pref.ACCOUNT_NAME), accountName, true);
    }

    public static String getAccountType() {
        return getString(getUserKeyName(pref.ACCOUNT_TYPE), null, true);
    }

    public static void setAccountType(String accountType) {
        setString(getUserKeyName(pref.ACCOUNT_TYPE), accountType, true);
    }

    public static int getHardnessIterations() {
        return getInt(getUserKeyName(pref.KEYHARDNESS), CryptTools.HARDNESS_ROUNDS, true);
    }

    public static void setHardnessIterations(int HardnessIterations) {
        setInt(getUserKeyName(pref.KEYHARDNESS), HardnessIterations, true);
    }

    public static byte[] getKeySalt() {
        return getByteArray(getUserKeyName(pref.KEYSALT), null, true);
    }

    public static void setKeySalt(byte[] salt) {
        setByteArray(getUserKeyName(pref.KEYSALT), salt, true);
    }

    public static String getKeyIdString() {
        return getString(getUserKeyName(pref.KEYID_STRING), null, true);
    }

    public static void setKeyIdString(String privKeyId) {
        setString(getUserKeyName(pref.KEYID_STRING), privKeyId, true);
    }

    public static long getKeyDate() {
        return getLong(getUserKeyName(pref.KEYDATE), 0, true);
    }

    public static long getKeyDate(int userNumber) {
        return getLong(getUserKeyName(pref.KEYDATE, userNumber), 0, true);
    }

    public static void setKeyDate(long date) {
        setLong(getUserKeyName(pref.KEYDATE), date, true);
    }

    public static int getPassPhraseCacheTtl() {
        return getInt(getUserKeyName(pref.PASSPHRASE_CACHE_TTL), DEFAULT_PPCACHETTL, true);
    }

    public static void setPassPhraseCacheTtl(int pass_phrase_cache_ttl) {
        setInt(getUserKeyName(pref.PASSPHRASE_CACHE_TTL), pass_phrase_cache_ttl, true);
    }

    public static int getFontSize() {
        return getInt(getUserKeyName(pref.FONT_SIZE), DEFAULT_FONT_SIZE, true);
    }

    public static void setFontSize(int fontSize) {
        setInt(getUserKeyName(pref.FONT_SIZE), fontSize, true);
    }

    public static boolean getAutoDecrypt() {
        return getBoolean(getUserKeyName(pref.AUTO_DECRYPT), DEFAULT_AUTO_DECRYPT, true);
    }

    public static void setAutoDecrypt(boolean autoDecrypt) {
        setBoolean(getUserKeyName(pref.AUTO_DECRYPT), autoDecrypt, true);
    }

    public static boolean getAutoRetrieval() {
        return getBoolean(getUserKeyName(pref.AUTO_RETRIEVAL), DEFAULT_AUTO_RETRIEVAL, true);
    }

    public static void setAutoRetrieval(boolean autoRetrieval) {
        setBoolean(getUserKeyName(pref.AUTO_RETRIEVAL), autoRetrieval, true);
    }

    // backup, however not per each user...

    public static int getCurrentRecipientDBVer() {
        return getInt(pref.CURR_RECIP_DB_VER, 0, true);
    }

    public static void setCurrentRecipientDBVer(int ver) {
        setInt(pref.CURR_RECIP_DB_VER, ver, true);
    }

    public static int getCurrentMessageDBVer() {
        return getInt(pref.CURR_MSG_DB_VER, 0, true);
    }

    public static void setCurrentMessageDBVer(int ver) {
        setInt(pref.CURR_MSG_DB_VER, ver, true);
    }

    public static boolean getEulaAccepted() {
        return getBoolean(pref.EULA_ACCEPTED, DEFAULT_EULA_ACCEPTED, true);
    }

    public static void setEulaAccepted(boolean accepted) {
        setBoolean(pref.EULA_ACCEPTED, accepted, true);
    }

    public static boolean getShowSlingKeysReminder() {
        return getBoolean(pref.SHOW_SLING_KEYS_REMIND, DEFAULT_SHOW_REMINDSLINGKEYS, true);
    }

    public static void setShowSlingKeysReminder(boolean showSlingKeysReminder) {
        setBoolean(pref.SHOW_SLING_KEYS_REMIND, showSlingKeysReminder, true);
    }

    public static boolean getShowWalkthrough() {
        return getBoolean(pref.SHOW_WALKTHROUGH, DEFAULT_SHOW_WALKTHROUGH, true);
    }

    public static void setShowWalkthrough(boolean showWalkthrough) {
        setBoolean(pref.SHOW_WALKTHROUGH, showWalkthrough, true);
    }

    public static boolean getNotificationVibrate() {
        return getBoolean(pref.NOTIFICATION_VIBRATE, DEFAULT_NOTIFICATION_VIBRATE, true);
    }

    public static void setNotificationVibrate(boolean notificationVibrate) {
        setBoolean(pref.NOTIFICATION_VIBRATE, notificationVibrate, true);
    }

    public static String getNotificationRingTone() {
        return getString(pref.NOTIFICATION_RINGTONE, DEFAULT_RINGTONE, true);
    }

    public static void setNotificationRingTone(String notificationRingTone) {
        setString(pref.NOTIFICATION_RINGTONE, notificationRingTone, true);
    }

    public static long getLastTimeStamp() {
        return getLong(getUserKeyName(pref.LAST_MSG_STAMP), 0, false);
    }

    public static void setLastTimeStamp(long timeStamp) {
        setLong(getUserKeyName(pref.LAST_MSG_STAMP), timeStamp, false);
    }

    // do NOT persist to backup account...

    @Deprecated
    public static String getContactId() {
        return getString(pref.CONTACT_ID, null, false);
    }

    @Deprecated
    public static void setContactId(String contactId) {
        setString(pref.CONTACT_ID, contactId, false);
    }

    public static boolean getThisVersionOpened() {
        return getBoolean(SafeSlingerConfig.getVersionName() + VERSION_OPEN_SUFFIX, false, false);
    }

    public static void setThisVersionOpened() {
        setBoolean(SafeSlingerConfig.getVersionName() + VERSION_OPEN_SUFFIX, true, false);
    }

    public static long getContactDBLastScan() {
        return getLong(pref.CONTACT_DB_LAST_SCAN, 0, false);
    }

    public static void setContactDBLastScan(long contactDBLastScan) {
        setLong(pref.CONTACT_DB_LAST_SCAN, contactDBLastScan, false);
    }

    public static boolean getRemindBackupDelay() {
        return getBoolean(pref.REMIND_BACKUP_DELAY, DEFAULT_REMINDBACKUPDELAY, false);
    }

    public static void setRemindBackupDelay(boolean remindBackupDelay) {
        setBoolean(pref.REMIND_BACKUP_DELAY, remindBackupDelay, false);
    }

    public static boolean getPushRegistrationIdPosted() {
        // add version to get updated push registration every new release
        return getBoolean(pref.PUSH_REG_ID_POSTED + SafeSlingerConfig.getVersionName(),
                DEFAULT_PUSH_REG_ID_POSTED, false);
    }

    public static void setPushRegistrationIdPosted(boolean registrationIdPosted) {
        // add version to get updated push registration every new release
        setBoolean(pref.PUSH_REG_ID_POSTED + SafeSlingerConfig.getVersionName(),
                registrationIdPosted, false);
    }

    public static String getPushRegistrationId() {
        // add version to get updated push registration every new release
        final String registrationId = getString(
                pref.PUSH_REG_ID_LINKED + SafeSlingerConfig.getVersionName(), null, false);
        // set a simple display to allow preferences to view an accurate id
        setString(pref.PUSH_REG_ID_LINKED_DISPLAY, registrationId, false);
        return registrationId;
    }

    public static void setPushRegistrationId(String registrationId) {
        // add version to get updated push registration every new release
        setString(pref.PUSH_REG_ID_LINKED + SafeSlingerConfig.getVersionName(), registrationId,
                false);
    }

    public static long getBackupRequestDate() {
        return getLong(pref.BACKUP_REQUEST_DATE, 0, false);
    }

    public static void setBackupRequestDate(long backupRequestDate) {
        setLong(pref.BACKUP_REQUEST_DATE, backupRequestDate, false);
    }

    public static long getBackupCompleteDate() {
        return getLong(pref.BACKUP_COMPLETE_DATE, 0, false);
    }

    public static void setBackupCompleteDate(long backupCompleteDate) {
        setLong(pref.BACKUP_COMPLETE_DATE, backupCompleteDate, false);
    }

    public static long getRestoreCompleteDate() {
        return getLong(pref.RESTORE_COMPLETE_DATE, 0, false);
    }

    public static void setRestoreCompleteDate(long restoreCompleteDate) {
        setLong(pref.RESTORE_COMPLETE_DATE, restoreCompleteDate, false);
    }

    public static long getNextPassAttemptDate() {
        return getLong(pref.NEXT_PASS_ATTEMPT_DATE, 0, false);
    }

    public static void setNextPassAttemptDate(long nextPassAttemptDate) {
        setLong(pref.NEXT_PASS_ATTEMPT_DATE, nextPassAttemptDate, false);
    }

    public static long getPassBackoffTimeout() {
        return getLong(pref.PASS_BACKOFF_TIMEOUT, DEFAULT_PASSPHRASE_BACKOFF, false);
    }

    public static void setPassBackoffTimeout(long passBackoffTimeout) {
        setLong(pref.PASS_BACKOFF_TIMEOUT, passBackoffTimeout, false);
    }

    public static long getPusgRegBackoff() {
        return getLong(pref.PUSHREG_BACKOFF_TIMEOUT, DEFAULT_PUSHREG_BACKOFF, false);
    }

    public static void setPusgRegBackoff(long pushRegBackoff) {
        setLong(pref.PUSHREG_BACKOFF_TIMEOUT, pushRegBackoff, false);
    }

    public static int getUser() {
        return getInt(pref.CURRENT_USER, DEFAULT_CURRENT_USER, false);
    }

    public static void setUser(int user) {
        setInt(pref.CURRENT_USER, user, false);
    }

    public static boolean getFirstExchangeComplete() {
        return getBoolean(pref.FIRST_EXCH_COMPLETE, DEFAULT_FIRST_EXCH_COMPLETE, false);
    }

    public static void setFirstExchangeComplete(boolean firstExchangeComplete) {
        setBoolean(pref.FIRST_EXCH_COMPLETE, firstExchangeComplete, false);
    }

    public static String getDownloadDir() {
        return getString(pref.DOWNLOAD_DIRECTORY, DEFAULT_DOWNLOAD_DIR, false);
    }

    public static void setDownloadDir(String downloadDir) {
        setString(pref.DOWNLOAD_DIRECTORY, downloadDir, false);
    }

    public static String getFileManagerRootDir() {
        return getString(pref.FILEMANAGER_ROOTDIR, DEFAULT_FILEMANAGER_ROOTDIR, false);
    }

    public static void setFileManagerRootDir(String rootDir) {
        setString(pref.FILEMANAGER_ROOTDIR, rootDir, false);
    }

    public static long getPendingGetMessageBackoff() {
        return getLong(pref.PENDING_GETMSG_BACKOFF_TIMEOUT, DEFAULT_PENDING_GETMSG_BACKOFF, false);
    }

    public static void setPendingGetMessageBackoff(long backoff) {
        setLong(pref.PENDING_GETMSG_BACKOFF_TIMEOUT, backoff, false);
    }

    public static String getLanguage() {
        return getString(pref.LANGUAGE, DEFAULT_LANGUAGE, false);
    }

    public static void setLanguage(String language) {
        setString(pref.LANGUAGE, language, false);
    }

    // Generic getters and setters....

    private static byte[] getByteArray(String key, byte[] def, boolean recover) {
        String encodedDefault = (def == null) ? null : new String(
                Base64.encode(def, Base64.NO_WRAP));
        String encodedValue = getString(key, encodedDefault, recover);
        return encodedValue != null ? Base64.decode(encodedValue.getBytes(), Base64.NO_WRAP) : null;
    }

    private static void setByteArray(String key, byte[] value, boolean recover) {
        byte[] encodedValue = (value == null) ? null : Base64.encode(value, Base64.NO_WRAP);
        setString(key, new String(encodedValue), recover);
    }

    public static String getString(String key, String def, boolean recover) {
        Context ctx = SafeSlinger.getApplication();
        if (ctx == null) {
            return def;
        }
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        return settings.getString(key, def);
    }

    private static void setString(String key, String value, boolean recover) {
        String getString = getString(key, null, recover);
        Context ctx = SafeSlinger.getApplication();
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
        // request backup when recoverable setting has changed
        if (recover && getString != value) {
            SafeSlinger.queueBackup();
        }
    }

    private static boolean getBoolean(String key, boolean def, boolean recover) {
        Context ctx = SafeSlinger.getApplication();
        if (ctx == null) {
            return def;
        }
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        return settings.getBoolean(key, def);
    }

    private static void setBoolean(String key, boolean value, boolean recover) {
        boolean getBoolean = getBoolean(key, false, recover);
        Context ctx = SafeSlinger.getApplication();
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.commit();
        // request backup when recoverable setting has changed
        if (recover && getBoolean != value) {
            SafeSlinger.queueBackup();
        }
    }

    private static long getLong(String key, long def, boolean recover) {
        Context ctx = SafeSlinger.getApplication();
        if (ctx == null) {
            return def;
        }
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        return settings.getLong(key, def);
    }

    private static void setLong(String key, long value, boolean recover) {
        long getLong = getLong(key, 0, recover);
        Context ctx = SafeSlinger.getApplication();
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(key, value);
        editor.commit();
        // request backup when recoverable setting has changed
        if (recover && getLong != value) {
            SafeSlinger.queueBackup();
        }
    }

    private static int getInt(String key, int def, boolean recover) {
        Context ctx = SafeSlinger.getApplication();
        if (ctx == null) {
            return def;
        }
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        return settings.getInt(key, def);
    }

    private static void setInt(String key, int value, boolean recover) {
        int getInt = getInt(key, 0, recover);
        Context ctx = SafeSlinger.getApplication();
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
        // request backup when recoverable setting has changed
        if (recover && getInt != value) {
            SafeSlinger.queueBackup();
        }
    }

    public static void removePref(String key, boolean recover) {
        Context ctx = SafeSlinger.getApplication();
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        if (settings.contains(key)) {
            editor.remove(key);
            editor.commit();
            // request backup when recoverable setting has changed
            if (recover) {
                SafeSlinger.queueBackup();
            }
        }
    }

    public static boolean existsPref(String key, boolean recover) {
        Context ctx = SafeSlinger.getApplication();
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        return settings.contains(key);
    }

    private static String getUserKeyName(String key) {
        int user = getUser();
        if (user == 0) {
            // use 0 for for default user
            return key;
        } else {
            // use 1+ for additional users
            return (key + user);
        }
    }

    private static String getUserKeyName(String key, int userNumber) {
        if (userNumber == 0) {
            // use 0 for for default user
            return key;
        } else {
            // use 1+ for additional users
            return (key + userNumber);
        }
    }

    private static String getPrefsFileName(boolean recover) {
        return recover ? PREFS_RECOVER_YES : PREFS_RECOVER_NO;
    }

}
