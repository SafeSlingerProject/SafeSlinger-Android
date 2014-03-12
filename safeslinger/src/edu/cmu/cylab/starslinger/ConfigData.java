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

import java.util.Date;
import java.util.Locale;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import edu.cmu.cylab.starslinger.crypto.CryptTools;

public class ConfigData {

    public static final String HTTPURL_PREFIX = "https://";
    public static final String HTTPURL_HOST = "starsling-server.appspot.com";
    public static final String HTTPURL_SUFFIX = "";
    public static final String URL_SS_INSTALL = "http://www.cylab.cmu.edu/safeslinger/m.html";
    public static final String PUSH_SENDERID_EMAIL = "starslingerapp@gmail.com";
    public static final String KEY_COMMENT = "SafeSlinger";
    public static final String HELP_EMAIL = "safeslingerapp@gmail.com";
    public static final String HELP_URL = "www.cylab.cmu.edu/safeslinger";
    public static final String INTRODUCTION_VCF = "introduction.vcf";
    public static final String DATETIME_FILENAME = "yyyyMMdd_HHmmss";
    public static final String FEEDBACK_TXT = "feedback.txt";

    @Deprecated
    public static final String APP_KEY_OLD1 = "StarSlingerKey";
    @Deprecated
    public static final String APP_KEY_OLD2 = "SafeSlingerKey";

    public static final String APP_KEY_PUBKEY = "SafeSlinger-PubKey";
    public static final String APP_KEY_PUSHTOKEN = "SafeSlinger-Push";

    public static final String MIMETYPE_CLASS = "SafeSlinger";
    public static final String MIMETYPE_FUNC_SECINTRO = "SecureIntroduce";

    public static final String FILENAME_SECKEY_CRYPTOMSG = "SafeSlingerSecretKeyCryptoMsg.enc";

    // TODO some of these values could be exported into an xml prefs file
    public static final int MAX_C2DM_PAYLOAD = 1024;
    public static final int MAX_APNS_PAYLOAD = 256;
    public static final int MAX_TEXTMESSAGE = 2000;
    public static final int MAX_FILEBYTES = 9000000;
    public static final int MIN_KEYSIZE = 2048;
    public static final long MESSAGE_EXPIRATION_MS = (24 * 60 * 60 * 1000);
    public static final int VER_FILEMETA = 0x01032700; // 1.3.39
    public static final long CLOCK_SKEW_MS = (5 * 60 * 1000);
    public static final long BACKUP_DELAY_WARN_MS = (1 * 60 * 60 * 1000);
    public static final long MAX_PUSHREG_TIMEOUT_MS = (5 * 60 * 1000);
    public static final int LONG_DELAY = 3500; // 3.5 seconds
    public static final int SHORT_DELAY = 2000; // 2 seconds
    public static final int MS_READ_PER_CHAR = 50; // 50 msrpc ~ 20 cps
    public static final int MIN_PASSLEN = 8;

    public static final String NOTIFY_NOPUSH_TOKENDATA = "RECEIVE_DISABLED";
    /***
     * This has no token type to receive messages. Messages cannot be received
     * by this type by any means. Define new types if there are other receiving
     * mechanisms, do not use this one. The token type will consistently appear
     * as the string "RECEIVE_DISABLED".
     */
    public static final int NOTIFY_NOPUSH = 0;
    /***
     * This token type receives messages via Google's Cloud to Device Messaging
     * Service.
     */
    public static final int NOTIFY_ANDROIDC2DM = 1;
    /***
     * This token type receives messages via Apple's Notification Service,
     * through Urban Airship.
     */
    public static final int NOTIFY_APPLEUA = 2;

    public static final int DEFAULT_CURRENT_USER = 0;
    public static final long DEFAULT_PUSHREG_BACKOFF = 5000;
    public static final long DEFAULT_PASSPHRASE_BACKOFF = 1000;
    private static final int DEFAULT_PPCACHETTL = (5 * 60);
    private static final boolean DEFAULT_SHOW_WALKTHROUGH = true;
    private static final boolean DEFAULT_EULA_ACCEPTED = false;
    private static final boolean DEFAULT_REMINDBACKUPDELAY = true;
    private static final boolean DEFAULT_SHOW_RECENT_RECIPONLY = true;
    private static final boolean DEFAULT_SHOW_RECENT_RECIPONLY_EDITED = false;
    private static final boolean DEFAULT_FIRST_EXCH_COMPLETE = false;
    private static final boolean DEFAULT_SHOW_REMINDSLINGKEYS = true;

    public static class Intent {
        public static final String ACTION_MESSAGENOTIFY = "edu.cmu.cylab.starslinger.action.MESSAGES";
        public static final String ACTION_MESSAGEUPDATE = "edu.cmu.cylab.starslinger.action.MESSAGESUPDATE";
        public static final String ACTION_BACKUPNOTIFY = "edu.cmu.cylab.starslinger.action.BACKUPREMINDER";
        public static final String ACTION_SLINGKEYSNOTIFY = "edu.cmu.cylab.starslinger.action.SLINGKEYSNOTIFY";
    }

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

        public static final String ACCOUNT_NAME = "AccountName";
        public static final String ACCOUNT_TYPE = "AccountType";
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
        public static final String HAS_SEEN_HELP = "seenHelp";
        public static final String KEYDATE = "KeyDate";
        public static final String KEYHARDNESS = "KeyHardness";
        public static final String KEYID_STRING = "KeyIdString";
        public static final String KEYSALT = "KeySalt";
        public static final String NEXT_PASS_ATTEMPT_DATE = "nextPassAttemptDate";
        public static final String PASS_BACKOFF_TIMEOUT = "passBackoffTimeout";
        public static final String PASSPHRASE_CACHE_TTL = "passPhraseCacheTtl";
        public static final String PUSH_BACKOFF = "PushBackoff";
        public static final String PUSH_REG_DEPRECATED_OLD = "PushRegistrationId"; // pre-1.5.2
        public static final String PUSH_REGISTRATION_ID_DIRECT = "PushRegistrationIdDirect";
        public static final String PUSHREG_BACKOFF_TIMEOUT = "pushRegistrationBackoff";
        public static final String REMIND_BACKUP_DELAY = "RemindBackupDelay";
        public static final String RESTORE_COMPLETE_DATE = "restoreCompleteDate";
        public static final String SHOW_ABOUT = "showAbout";
        public static final String SHOW_LICENSE = "showLicense";
        public static final String SHOW_RECENT_RECIPONLY = "showRecentRecipOnly";
        public static final String SHOW_RECENT_RECIPONLY_EDITED = "showRecentRecipOnlyEdited";
        public static final String SHOW_SLING_KEYS_REMIND = "showSlingKeysReminder";
        public static final String SHOW_WALKTHROUGH = "ShowWalkthrough";
        public static final String TEMPKEY_SYNCACCOUNT_LIST = "keyTempListContactSyncAccount";
    }

    /***
     * internal interprocess communication...
     */
    public static final class extra {

        public static final String ALLOW_EXCH = "AllowExchRecip";
        public static final String ALLOW_INTRO = "AllowIntroRecip";
        public static final String CHANGE_PASS_PHRASE = "ChangePassPhrase";
        public static final String CONTACT_KEYNAME_PREFIX = "Key";
        public static final String CONTACT_LOOKUP_KEY = "ContactLookupKey";
        public static final String CONTACT_VALUE_PREFIX = "Value";
        public static final String CREATE_PASS_PHRASE = "CreatePassPhrase";
        public static final String CREATED = "Created";
        public static final String DIRS = "DirectoryStack";
        public static final String ERROR = "Error";
        public static final String EXCH_NAME = "ExchName";
        public static final String EXCHANGED_TOTAL = "ExchangedTotal";
        public static final String FILE_PATH = "FilePath";
        public static final String FIRSTLVL = "FirstLvl";
        public static final String FNAME = "FILE NAME";
        public static final String FPATH = "FILE PATH";
        public static final String INTRO_NAME = "IntroName";
        public static final String MAX = "max";
        public static final String MEMBER_DATA = "MemberData";
        public static final String MESSAGE_ROW_ID = "MessageRowId";
        public static final String NAME = "Name";
        public static final String PASS_PHRASE = "PassPhrase";
        public static final String PCT = "pct";
        public static final String PHOTO = "Photo";
        public static final String POSITION = "Position";
        public static final String PUSH_FILE_NAME = "PushFileName";
        public static final String PUSH_FILE_SIZE = "PushFileSize";
        public static final String PUSH_FILE_TYPE = "PushFileType";
        public static final String PUSH_MSG_HASH = "PushMsgHash";
        public static final String PUSH_REGISTRATION_ID = "PushRegistrationId";
        public static final String RECIPIENT_ROW_ID = "RecipientRowId";
        public static final String RECIPIENT_ROW_ID1 = "RecipientRowId1";
        public static final String RECIPIENT_ROW_ID2 = "RecipientRowId2";
        public static final String RECOVERY_STATE = "RecoveryState";
        public static final String RECOVERY_TAB = "RecoveryTab";
        public static final String REQUEST_CODE = "RequestCode";
        public static final String RESID_MSG = "ResIdMsg";
        public static final String RESID_TITLE = "ResIdTitle";
        public static final String RESULT_CODE = "ResultCode";
        public static final String TEXT_MESSAGE = "TextMessage";
        public static final String TEXT_MESSAGE1 = "TextMessage1";
        public static final String TEXT_MESSAGE2 = "TextMessage2";
        public static final String THUMBNAIL = "Thumbnail";
        public static final String USER_DATA = "UserData";
        public static final String VERIFY_PASS_PHRASE = "VerifyPassPhrase";
    }

    public static final String LOG_TAG = "AppLog";
    public static final String PREFS_RECOVER_YES = "MyPrefsFile";
    public static final String PREFS_RECOVER_NO = "MyPrefsNoRecoveryFile";
    public static final String VERSION_OPEN_SUFFIX = ".version.opened";

    public static boolean isNameValid(String name, Context ctx) {

        if (name == null || TextUtils.isEmpty(name.trim())) {
            return false;
        }
        String[] invalid = ctx.getResources().getStringArray(R.array.invalid_contact_names);
        for (String badName : invalid) {
            if (name.trim().equalsIgnoreCase(badName))
                return false;
        }
        return true;
    }

    public static String getVersionName(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
            return pi.versionName;
        } catch (NameNotFoundException e) {
            return "0.0";
        }
    }

    public static int getVersionCode(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
            return pi.versionCode;
        } catch (NameNotFoundException e) {
            return 0x00000000;
        }
    }

    public static String getFullVersion(Context context) {
        return context.getString(R.string.app_name) + " v" + getVersionName(context);
    }

    public static boolean isDebug(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
            return ((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    // persist to backup account...

    public static void removePrefDeprecated(Context ctx) {
        removePref(ctx, pref.CONTACT_EMAIL, true);
        removePref(ctx, pref.CONTACT_EMAIL_TYPE, true);
        removePref(ctx, pref.CONTACT_PHONE, true);
        removePref(ctx, pref.CONTACT_PHONE_TYPE, true);
    }

    public static boolean loadPrefContactField(Context ctx, String field) {
        return loadPrefBoolean(ctx, getUserKeyName(ctx, field), true, true);
    }

    public static void savePrefContactField(Context ctx, String field, boolean checked) {
        savePrefBoolean(ctx, getUserKeyName(ctx, field), checked, true);
    }

    public static String loadPrefContactLookupKey(Context ctx) {
        return loadPrefString(ctx, getUserKeyName(ctx, pref.CONTACT_LOOKUP_KEY), null, true);
    }

    public static void savePrefContactLookupKey(Context ctx, String lookupKey) {
        savePrefString(ctx, getUserKeyName(ctx, pref.CONTACT_LOOKUP_KEY), lookupKey, true);
    }

    public static boolean loadPrefEulaAccepted(Context ctx) {
        return loadPrefBoolean(ctx, getUserKeyName(ctx, pref.EULA_ACCEPTED), DEFAULT_EULA_ACCEPTED,
                true);
    }

    public static void savePrefEulaAccepted(Context ctx, boolean accepted) {
        savePrefBoolean(ctx, getUserKeyName(ctx, pref.EULA_ACCEPTED), accepted, true);
    }

    public static String loadPrefContactName(Context ctx) {
        return loadPrefString(ctx, getUserKeyName(ctx, pref.CONTACT_NAME), null, true);
    }

    public static String loadPrefContactName(Context ctx, int userNumber) {
        return loadPrefString(ctx, getUserKeyName(ctx, pref.CONTACT_NAME, userNumber), null, true);
    }

    public static void savePrefContactName(Context ctx, String ContactName) {
        savePrefString(ctx, getUserKeyName(ctx, pref.CONTACT_NAME), ContactName, true);
    }

    public static String loadPrefAccountName(Context ctx) {
        return loadPrefString(ctx, getUserKeyName(ctx, pref.ACCOUNT_NAME), null, true);
    }

    public static void savePrefAccountName(Context ctx, String accountName) {
        savePrefString(ctx, getUserKeyName(ctx, pref.ACCOUNT_NAME), accountName, true);
    }

    public static String loadPrefAccountType(Context ctx) {
        return loadPrefString(ctx, getUserKeyName(ctx, pref.ACCOUNT_TYPE), null, true);
    }

    public static void savePrefAccountType(Context ctx, String accountType) {
        savePrefString(ctx, getUserKeyName(ctx, pref.ACCOUNT_TYPE), accountType, true);
    }

    public static int loadPrefHardnessIterations(Context ctx) {
        return loadPrefInt(ctx, getUserKeyName(ctx, pref.KEYHARDNESS), CryptTools.HARDNESS_ROUNDS,
                true);
    }

    public static void savePrefHardnessIterations(Context ctx, int HardnessIterations) {
        savePrefInt(ctx, getUserKeyName(ctx, pref.KEYHARDNESS), HardnessIterations, true);
    }

    public static byte[] loadPrefKeySalt(Context ctx) {
        return loadPrefByteArray(ctx, getUserKeyName(ctx, pref.KEYSALT), null, true);
    }

    public static void savePrefKeySalt(Context ctx, byte[] salt) {
        savePrefByteArray(ctx, getUserKeyName(ctx, pref.KEYSALT), salt, true);
    }

    public static String loadPrefKeyIdString(Context ctx) {
        return loadPrefString(ctx, getUserKeyName(ctx, pref.KEYID_STRING), null, true);
    }

    public static void savePrefKeyIdString(Context ctx, String privKeyId) {
        savePrefString(ctx, getUserKeyName(ctx, pref.KEYID_STRING), privKeyId, true);
    }

    public static long loadPrefKeyDate(Context ctx) {
        return loadPrefLong(ctx, getUserKeyName(ctx, pref.KEYDATE), 0, true);
    }

    public static long loadPrefKeyDate(Context ctx, int userNumber) {
        return loadPrefLong(ctx, getUserKeyName(ctx, pref.KEYDATE, userNumber), 0, true);
    }

    public static void savePrefKeyDate(Context ctx, long date) {
        savePrefLong(ctx, getUserKeyName(ctx, pref.KEYDATE), date, true);
    }

    public static int loadPrefPassPhraseCacheTtl(Context ctx) {
        return loadPrefInt(ctx, getUserKeyName(ctx, pref.PASSPHRASE_CACHE_TTL), DEFAULT_PPCACHETTL,
                true);
    }

    public static void savePrefPassPhraseCacheTtl(Context ctx, int pass_phrase_cache_ttl) {
        savePrefInt(ctx, getUserKeyName(ctx, pref.PASSPHRASE_CACHE_TTL), pass_phrase_cache_ttl,
                true);
    }

    public static boolean loadPrefShowWalkthrough(Context ctx) {
        return loadPrefBoolean(ctx, getUserKeyName(ctx, pref.SHOW_WALKTHROUGH),
                DEFAULT_SHOW_WALKTHROUGH, true);
    }

    public static void savePrefShowWalkthrough(Context ctx, boolean showWalkthrough) {
        savePrefBoolean(ctx, getUserKeyName(ctx, pref.SHOW_WALKTHROUGH), showWalkthrough, true);
    }

    public static boolean loadPrefShowRecentRecipOnly(Context ctx) {
        if (!loadPrefShowRecentRecipOnlyEdited(ctx)) {
            // now unedited, use default
            savePrefBoolean(ctx, getUserKeyName(ctx, pref.SHOW_RECENT_RECIPONLY),
                    DEFAULT_SHOW_RECENT_RECIPONLY, true);
        }
        return loadPrefBoolean(ctx, getUserKeyName(ctx, pref.SHOW_RECENT_RECIPONLY),
                DEFAULT_SHOW_RECENT_RECIPONLY, true);
    }

    public static void savePrefShowRecentRecipOnly(Context ctx, boolean showRecentRecipOnly) {
        // now edited
        savePrefBoolean(ctx, getUserKeyName(ctx, pref.SHOW_RECENT_RECIPONLY_EDITED), true, true);
        savePrefBoolean(ctx, getUserKeyName(ctx, pref.SHOW_RECENT_RECIPONLY), showRecentRecipOnly,
                true);
    }

    public static boolean loadPrefShowRecentRecipOnlyEdited(Context ctx) {
        return loadPrefBoolean(ctx, getUserKeyName(ctx, pref.SHOW_RECENT_RECIPONLY_EDITED),
                DEFAULT_SHOW_RECENT_RECIPONLY_EDITED, true);
    }

    public static void savePrefShowRecentRecipOnlyEdited(Context ctx,
            boolean showRecentRecipOnlyEdited) {
        savePrefBoolean(ctx, getUserKeyName(ctx, pref.SHOW_RECENT_RECIPONLY_EDITED),
                showRecentRecipOnlyEdited, true);
    }

    public static boolean loadPrefShowSlingKeysReminder(Context ctx) {
        return loadPrefBoolean(ctx, getUserKeyName(ctx, pref.SHOW_SLING_KEYS_REMIND),
                DEFAULT_SHOW_REMINDSLINGKEYS, true);
    }

    public static void savePrefShowSlingKeysReminder(Context ctx, boolean showSlingKeysReminder) {
        savePrefBoolean(ctx, getUserKeyName(ctx, pref.SHOW_SLING_KEYS_REMIND),
                showSlingKeysReminder, true);
    }

    // backup, however not per each user...

    public static int loadPrefCurrentRecipientDBVer(Context ctx) {
        return loadPrefInt(ctx, pref.CURR_RECIP_DB_VER, 0, true);
    }

    public static void savePrefCurrentRecipientDBVer(Context ctx, int ver) {
        savePrefInt(ctx, pref.CURR_RECIP_DB_VER, ver, true);
    }

    public static int loadPrefCurrentMessageDBVer(Context ctx) {
        return loadPrefInt(ctx, pref.CURR_MSG_DB_VER, 0, true);
    }

    public static void savePrefCurrentMessageDBVer(Context ctx, int ver) {
        savePrefInt(ctx, pref.CURR_MSG_DB_VER, ver, true);
    }

    // do NOT persist to backup account...

    @Deprecated
    public static String loadPrefContactId(Context ctx) {
        return loadPrefString(ctx, pref.CONTACT_ID, null, false);
    }

    @Deprecated
    public static void savePrefContactId(Context ctx, String contactId) {
        savePrefString(ctx, pref.CONTACT_ID, contactId, false);
    }

    public static boolean loadPrefThisVersionOpened(Context ctx) {
        return loadPrefBoolean(ctx, getVersionName(ctx) + VERSION_OPEN_SUFFIX, false, false);
    }

    public static void savePrefThisVersionOpened(Context ctx) {
        savePrefBoolean(ctx, getVersionName(ctx) + VERSION_OPEN_SUFFIX, true, false);
    }

    public static long loadPrefContactDBLastScan(Context ctx) {
        return loadPrefLong(ctx, pref.CONTACT_DB_LAST_SCAN, 0, false);
    }

    public static void savePrefContactDBLastScan(Context ctx, long contactDBLastScan) {
        savePrefLong(ctx, pref.CONTACT_DB_LAST_SCAN, contactDBLastScan, false);
    }

    public static boolean loadPrefRemindBackupDelay(Context ctx) {
        return loadPrefBoolean(ctx, pref.REMIND_BACKUP_DELAY, DEFAULT_REMINDBACKUPDELAY, false);
    }

    public static void savePrefRemindBackupDelay(Context ctx, boolean remindBackupDelay) {
        savePrefBoolean(ctx, pref.REMIND_BACKUP_DELAY, remindBackupDelay, false);
    }

    public static String loadPrefPushRegistrationId(Context ctx) {
        return loadPrefString(ctx, pref.PUSH_REGISTRATION_ID_DIRECT, null, false);
    }

    public static void savePrefPushRegistrationIdWriteOnlyC2dm(Context ctx, String registrationId) {
        savePrefString(ctx, pref.PUSH_REGISTRATION_ID_DIRECT, registrationId, false);
    }

    public static long loadPrefbackupRequestDate(Context ctx) {
        return loadPrefLong(ctx, pref.BACKUP_REQUEST_DATE, 0, false);
    }

    public static void savePrefbackupRequestDate(Context ctx, long backupRequestDate) {
        savePrefLong(ctx, pref.BACKUP_REQUEST_DATE, backupRequestDate, false);
    }

    public static long loadPrefbackupCompleteDate(Context ctx) {
        return loadPrefLong(ctx, pref.BACKUP_COMPLETE_DATE, 0, false);
    }

    public static void savePrefbackupCompleteDate(Context ctx, long backupCompleteDate) {
        savePrefLong(ctx, pref.BACKUP_COMPLETE_DATE, backupCompleteDate, false);
    }

    public static long loadPrefrestoreCompleteDate(Context ctx) {
        return loadPrefLong(ctx, pref.RESTORE_COMPLETE_DATE, 0, false);
    }

    public static void savePrefrestoreCompleteDate(Context ctx, long restoreCompleteDate) {
        savePrefLong(ctx, pref.RESTORE_COMPLETE_DATE, restoreCompleteDate, false);
    }

    public static long loadPrefnextPassAttemptDate(Context ctx) {
        return loadPrefLong(ctx, pref.NEXT_PASS_ATTEMPT_DATE, 0, false);
    }

    public static void savePrefnextPassAttemptDate(Context ctx, long nextPassAttemptDate) {
        savePrefLong(ctx, pref.NEXT_PASS_ATTEMPT_DATE, nextPassAttemptDate, false);
    }

    public static long loadPrefpassBackoffTimeout(Context ctx) {
        return loadPrefLong(ctx, pref.PASS_BACKOFF_TIMEOUT, DEFAULT_PASSPHRASE_BACKOFF, false);
    }

    public static void savePrefpassBackoffTimeout(Context ctx, long passBackoffTimeout) {
        savePrefLong(ctx, pref.PASS_BACKOFF_TIMEOUT, passBackoffTimeout, false);
    }

    public static long loadPrefPusgRegBackoff(Context ctx) {
        return loadPrefLong(ctx, pref.PUSHREG_BACKOFF_TIMEOUT, DEFAULT_PUSHREG_BACKOFF, false);
    }

    public static void savePrefPusgRegBackoff(Context ctx, long pushRegBackoff) {
        savePrefLong(ctx, pref.PUSHREG_BACKOFF_TIMEOUT, pushRegBackoff, false);
    }

    public static int loadPrefUser(Context ctx) {
        return loadPrefInt(ctx, pref.CURRENT_USER, DEFAULT_CURRENT_USER, false);
    }

    public static void savePrefUser(Context ctx, int user) {
        savePrefInt(ctx, pref.CURRENT_USER, user, false);
    }

    public static boolean loadPrefFirstExchangeComplete(Context ctx) {
        return loadPrefBoolean(ctx, pref.FIRST_EXCH_COMPLETE, DEFAULT_FIRST_EXCH_COMPLETE, false);
    }

    public static void savePrefFirstExchangeComplete(Context ctx, boolean firstExchangeComplete) {
        savePrefBoolean(ctx, pref.FIRST_EXCH_COMPLETE, firstExchangeComplete, false);
    }

    public static String loadPrefDownloadDir(Context ctx) {
        return loadPrefString(ctx, pref.DOWNLOAD_DIRECTORY, Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .getAbsolutePath(), false);
    }

    public static void savePrefDownloadDir(Context ctx, String downloadDir) {
        savePrefString(ctx, pref.DOWNLOAD_DIRECTORY, downloadDir, false);
    }

    public static String loadPrefFileManagerRootDir(Context ctx) {
        return loadPrefString(ctx, pref.FILEMANAGER_ROOTDIR, Environment
                .getExternalStorageDirectory().getAbsolutePath(), false);
    }

    public static void savePrefFileManagerRootDir(Context ctx, String rootDir) {
        savePrefString(ctx, pref.FILEMANAGER_ROOTDIR, rootDir, false);
    }

    // Generic getters and setters....

    private static byte[] loadPrefByteArray(Context ctx, String key, byte[] def, boolean recover) {
        String encodedDefault = (def == null) ? null : new String(
                Base64.encode(def, Base64.NO_WRAP));
        String encodedValue = loadPrefString(ctx, key, encodedDefault, recover);
        return Base64.decode(encodedValue.getBytes(), Base64.NO_WRAP);
    }

    private static void savePrefByteArray(Context ctx, String key, byte[] value, boolean recover) {
        byte[] encodedValue = (value == null) ? null : Base64.encode(value, Base64.NO_WRAP);
        savePrefString(ctx, key, new String(encodedValue), recover);
    }

    private static String loadPrefString(Context ctx, String key, String def, boolean recover) {
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        return settings.getString(key, def);
    }

    private static void savePrefString(Context ctx, String key, String value, boolean recover) {
        String loadPrefString = loadPrefString(ctx, key, null, recover);
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
        // request backup when recoverable setting has changed
        if (recover && loadPrefString != value) {
            queueBackup(ctx);
        }
    }

    private static boolean loadPrefBoolean(Context ctx, String key, boolean def, boolean recover) {
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        return settings.getBoolean(key, def);
    }

    private static void savePrefBoolean(Context ctx, String key, boolean value, boolean recover) {
        boolean loadPrefBoolean = loadPrefBoolean(ctx, key, false, recover);
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.commit();
        // request backup when recoverable setting has changed
        if (recover && loadPrefBoolean != value) {
            queueBackup(ctx);
        }
    }

    private static long loadPrefLong(Context ctx, String key, long def, boolean recover) {
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        return settings.getLong(key, def);
    }

    private static void savePrefLong(Context ctx, String key, long value, boolean recover) {
        long loadPrefLong = loadPrefLong(ctx, key, 0, recover);
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(key, value);
        editor.commit();
        // request backup when recoverable setting has changed
        if (recover && loadPrefLong != value) {
            queueBackup(ctx);
        }
    }

    private static int loadPrefInt(Context ctx, String key, int def, boolean recover) {
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        return settings.getInt(key, def);
    }

    private static void savePrefInt(Context ctx, String key, int value, boolean recover) {
        int loadPrefInt = loadPrefInt(ctx, key, 0, recover);
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
        // request backup when recoverable setting has changed
        if (recover && loadPrefInt != value) {
            queueBackup(ctx);
        }
    }

    private static void removePref(Context ctx, String key, boolean recover) {
        SharedPreferences settings = ctx.getSharedPreferences(getPrefsFileName(recover),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        if (settings.contains(key)) {
            editor.remove(key);
            editor.commit();
            // request backup when recoverable setting has changed
            if (recover) {
                queueBackup(ctx);
            }
        }
    }

    private static String getUserKeyName(Context ctx, String key) {
        if (loadPrefUser(ctx) == 0) {
            // use 0 for for default user
            return key;
        } else {
            // use 1+ for additional users
            return (key + loadPrefUser(ctx));
        }
    }

    private static String getUserKeyName(Context ctx, String key, int userNumber) {
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

    public static String writeByteArray(byte[] bytes) {
        StringBuilder raw = new StringBuilder(String.format(Locale.US, "len %d: ", bytes.length));
        for (int i = 0; i < bytes.length; i++)
            raw.append(String.format(Locale.US, "%X ", bytes[i]));
        return raw.toString();
    }

    public static void queueBackup(Context ctx) {
        BackupManager bm = new BackupManager(ctx);
        bm.dataChanged();
        savePrefbackupRequestDate(ctx, new Date().getTime());
    }

}
