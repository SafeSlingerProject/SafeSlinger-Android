
package edu.cmu.cylab.keyslinger.lib;

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

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Base64;

/***
 * This static class is meant to hold user preferences and static data and
 * settings. The string resource should contain all localizable strings. The
 * strings stored here are programmatic and are not user facing.
 */
public class KsConfig {

    // for KeySlinger Release
    public static final String HTTPURL_PREFIX = "https://";
    public static final String HTTPURL_HOST = "keyslinger-server.appspot.com";
    public static final int VER_SHA3 = 0x01060000;

    public static final String HTTPURL_SUFFIX = "";
    public static final int HTTPPORT = 80;
    public static final int MIN_USERS = 2;
    public static final int MIN_USERS_AUTOCOUNT = 11;
    public static final int MAX_USERS = 63;
    public static final int AES_KEY_LEN = 256 / 8;
    public static final int AES_IV_LEN = 128 / 8;
    public static final int HALFKEY_LEN = 512 / 8;
    public static final int HASH_LEN = 256 / 8;
    public static final int MSSVR_TIMEOUT_AUTO = 1000 * 15; // 15s
    public static final int MSSVR_TIMEOUT = 1000 * 60 * 2; // 2m
    public static final int MSSVR_POLL = 1000; // exp. 2s->4s->8s...
    public static final long MSSVR_EXCH_PROT_MAX = 1000 * 60 * 10;// 10m

    public static final String LOG_TAG = "AppLog";

    // internal interprocess communication...
    public static final class extra {
        public static final String CONTACT_LOOKUP_KEY = "ContactLookupKey";
        public static final String CONTACT_KEYNAME_PREFIX = "Key";
        public static final String CONTACT_VALUE_PREFIX = "Value";
        public static final String MEMBER_DATA = "MemberData";
        public static final String GROUP_SIZE = "GroupSize";
        public static final String GROUP_ID = "GroupId";
        public static final String SERVER_SIZE = "ServerSize";
        public static final String FLAG_HASH = "FlagHash";
        public static final String DECOY1_HASH = "DecoyHash1";
        public static final String DECOY2_HASH = "DecoyHash2";
        public static final String MESSAGE = "Message";
        public static final String USER_ID = "UserId";
        public static final String USER_DATA = "UserData";
        public static final String NAME = "Name";
        public static final String PHONE = "Phone";
        public static final String EMAIL = "Email";
        public static final String PHONE_TYPE = "PhoneType";
        public static final String EMAIL_TYPE = "EmailType";
        public static final String EXCHANGED_TOTAL = "ExchangedTotal";
        public static final String SELECTED_TOTAL = "SelectedTotal";
        public static final String RESID_TITLE = "ResIdTitle";
        public static final String RESID_MSG = "ResIdMsg";
        public static final String REQUEST_CODE = "RequestCode";
        public static final String RANDOM_POS = "RandomPosition";
        public static final String PHOTO = "Photo";
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

    /***
     * Ensure arbitrary data is only Base 64 encoded once.
     */
    public static byte[] finalEncode(byte[] src) {
        byte[] dest = null;
        if (isArrayByteBase64(src)) {
            dest = src;
        } else {
            dest = Base64.encode(src, Base64.NO_WRAP);
        }
        return dest;
    }

    /***
     * Ensure arbitrary data can only be Base 64 decoded once.
     */
    public static byte[] finalDecode(byte[] src) {
        byte[] dest = null;
        try {
            dest = Base64.decode(src, Base64.NO_WRAP);
        } catch (IllegalArgumentException e) {
            return src;
        }

        if (!isArrayByteBase64(dest)) {
            dest = src;
        }
        return dest;
    }

    private static boolean isArrayByteBase64(byte[] src) {
        try {
            String s = new String(src, "UTF-8");
            final String base64Regex = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})$";
            return Pattern.matches(base64Regex, s);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }
}
