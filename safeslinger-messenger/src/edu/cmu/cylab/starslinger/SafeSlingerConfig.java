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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;

public class SafeSlingerConfig {

    public static final String HTTPURL_PREFIX = "https://";
    public static final String HTTPURL_MESSENGER_HOST = "starsling-server.appspot.com";
    public static final String HTTPURL_EXCHANGE_HOST = "keyslinger-server.appspot.com";
    public static final String HTTPURL_SUFFIX = "";
    public static final String PUSH_SENDERID_EMAIL = "starslingerapp@gmail.com";
    public static final String KEY_COMMENT = "SafeSlinger";
    public static final String HELP_EMAIL = "safeslingerapp@gmail.com";
    public static final String HELP_URL = "www.cylab.cmu.edu/safeslinger";
    public static final String SOURCE_URL = "github.com/safeslingerproject";
    public static final String INTRODUCTION_VCF = "introduction.vcf";
    public static final String DATETIME_FILENAME = "yyyyMMdd_HHmmss";
    public static final String FEEDBACK_TXT = "feedback.txt";
    public static final String GOOGLE_TRANSLATE = "Google Translate";
    public static final String MIMETYPE_ADD_ATTACH = "*/*";
    public static final String MIMETYPE_OPEN_ATTACH_DEF = "image/*";
    public static final String LOG_TAG = "SafeSlinger-Messenger";
    public static final String PRIVACY_URL = "http://www.cylab.cmu.edu/safeslinger/privacy.html";
    public static final String EULA_URL = "http://www.cylab.cmu.edu/safeslinger/eula.html";
    public static final String LOCALIZE_URL = "transifex.com/projects/p/safeslinger";

    @Deprecated
    public static final String APP_KEY_OLD1 = "StarSlingerKey";
    @Deprecated
    public static final String APP_KEY_OLD2 = "SafeSlingerKey";

    public static final String APP_KEY_PUBKEY = "SafeSlinger-PubKey";
    public static final String APP_KEY_PUSHTOKEN = "SafeSlinger-Push";

    public static final String MIMETYPE_CLASS = "SafeSlinger";
    public static final String MIMETYPE_FUNC_SECINTRO = "SecureIntroduce";

    public static final String FILENAME_SECKEY_CRYPTOMSG = "SafeSlingerSecretKeyCryptoMsg.enc";

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

    // TODO remove RECEIVE_DISABLED and send-only mode when GCM is in use
    public static final String NOTIFY_NOPUSH_TOKENDATA = "RECEIVE_DISABLED";
    /***
     * This has no token type to receive messages. Messages cannot be received
     * by this type by any means. Define new types if there are other receiving
     * mechanisms, do not use this one. The token type will consistently appear
     * as the string "RECEIVE_DISABLED". Value: 0.
     */
    public static final int NOTIFY_NOPUSH = 0;
    /***
     * This token type receives messages via Google's Cloud to Device Messaging
     * Service for Android. Value: 1.
     */
    public static final int NOTIFY_ANDROIDC2DM = 1;
    /***
     * This token type receives messages via Apple's Notification Service,
     * through Urban Airship for iOS. Value: 2.
     */
    public static final int NOTIFY_APPLEUA = 2;
    /***
     * This token type receives messages via Google's Cloud Messaging Service
     * for Android. Value: 3.
     */
    public static final int NOTIFY_ANDROIDGCM = 3;
    /***
     * This token type receives messages via Microsoft Push Notification Service
     * for Windows Phone. Value: 4.
     */
    public static final int NOTIFY_WINPHONEMPNS = 4;
    /***
     * This token type receives messages via RIM Blackberry Push Service for
     * Blackberry. Value: 5.
     */
    public static final int NOTIFY_BLACKBERRYPS = 5;
    /***
     * This token type receives messages via Apple's Notification Service,
     * directly through APNS for iOS. Value: 6.
     */
    public static final int NOTIFY_APPLEAPNS = 6;
    /***
     * This token type receives messages via Amazon Device Messaging for Amazon
     * Fire OS. Value: 7.
     */
    public static final int NOTIFY_AMAZONADM = 7;

    public static class Intent {
        public static final String ACTION_MESSAGEOUTGOING = "edu.cmu.cylab.starslinger.action.MESSAGEOUTGOING";
        public static final String ACTION_MESSAGEINCOMING = "edu.cmu.cylab.starslinger.action.MESSAGEINCOMING";
        public static final String ACTION_BACKUPNOTIFY = "edu.cmu.cylab.starslinger.action.BACKUPREMINDER";
        public static final String ACTION_SLINGKEYSNOTIFY = "edu.cmu.cylab.starslinger.action.SLINGKEYSNOTIFY";
        public static final String ACTION_CHANGESETTINGS = "edu.cmu.cylab.starslinger.action.CHANGESETTINGS";
    }

    /***
     * internal interprocess communication...
     */
    public static final class extra {

        public static final String ALLOW_DELETE = "AllowDelete";
        public static final String ALLOW_EXCH = "AllowExchRecip";
        public static final String ALLOW_INTRO = "AllowIntroRecip";
        public static final String CHANGE_PASS_PHRASE = "ChangePassPhrase";
        public static final String CONTACT_KEYNAME_PREFIX = "Key";
        public static final String CONTACT_LOOKUP_KEY = "ContactLookupKey";
        public static final String CONTACT_VALUE_PREFIX = "Value";
        public static final String CREATE_PASS_PHRASE = "CreatePassPhrase";
        public static final String CREATED = "Created";
        public static final String DATA = "Data";
        public static final String DIRS = "DirectoryStack";
        public static final String ERROR = "Error";
        public static final String EXCH_NAME = "ExchName";
        public static final String EXCHANGED_TOTAL = "ExchangedTotal";
        public static final String FIRSTLVL = "FirstLvl";
        public static final String FNAME = "FileName";
        public static final String FPATH = "FilePath";
        public static final String INTRO_NAME = "IntroName";
        public static final String MAX = "max";
        public static final String MESSAGE_ROW_ID = "MessageRowId";
        public static final String INBOX_ROW_ID = "InboxRowId";
        public static final String NAME = "Name";
        public static final String PASS_PHRASE_NEW = "PassPhraseNew";
        public static final String PASS_PHRASE_OLD = "PassPhraseOld";
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
        public static final String USER_TOTAL = "UserTotal";
        public static final String RECIP_SOURCE = "RecipientSource";
        public static final String KEYID = "KeyId";
        public static final String INTRO_PUBKEY = "IntroPubKey";

        public static final String NOTIFY_COUNT = "notify_count";
        public static final String NOTIFY_STATUS = "notify_status";
    }

    public static boolean isNameValid(String name) {

        if (name == null || TextUtils.isEmpty(name.trim())) {
            return false;
        }
        Context ctx = SafeSlinger.getApplication();
        String[] invalid = ctx.getResources().getStringArray(R.array.invalid_contact_names);
        for (String badName : invalid) {
            if (name.trim().equalsIgnoreCase(badName))
                return false;
        }
        return true;
    }

    public static String getVersionName() {
        Context ctx = SafeSlinger.getApplication();
        PackageManager pm = ctx.getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
            return pi.versionName;
        } catch (NameNotFoundException e) {
            return "0.0";
        }
    }

    public static int getVersionCode() {
        Context ctx = SafeSlinger.getApplication();
        PackageManager pm = ctx.getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
            return pi.versionCode;
        } catch (NameNotFoundException e) {
            return 0x00000000;
        }
    }

    public static String getFullVersion() {
        Context ctx = SafeSlinger.getApplication();
        return ctx.getString(R.string.app_name) + " v" + getVersionName();
    }

    public static boolean isDebug() {
        Context ctx = SafeSlinger.getApplication();
        PackageManager pm = ctx.getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
            return ((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
