
package edu.cmu.cylab.starslinger.exchange;

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

/***
 * This static class is meant to hold user preferences and static data and
 * settings. The string resource should contain all localizable strings. The
 * strings stored here are programmatic and are not user facing.
 */
public class ExchangeConfig {

    public static final String HTTPURL_PREFIX = "https://";
    public static final String HTTPURL_SUFFIX = "";
    public static final int HTTPPORT = 80;
    public static final int VER_SHA3 = 0x01060000;
    public static final int VER_NOTIFY_BACKCOMPAT = 0x01070000;
    public static final int MIN_USERS = 2;
    public static final int MIN_USERS_AUTOCOUNT = 11;
    public static final int MAX_USERS = 63;
    public static final int AES_KEY_LEN = 256 / 8;
    public static final int AES_IV_LEN = 128 / 8;
    public static final int HALFKEY_LEN = 512 / 8;
    public static final int HASH_LEN = 256 / 8;
    public static final long MSSVR_TIMEOUT = 1000 * 60 * 2; // 2m
    public static final long MSSVR_EXCH_PROT_MAX = 1000 * 60 * 10;// 10m
    public static final int LONG_DELAY = 3500; // 3.5 seconds
    public static final int SHORT_DELAY = 2000; // 2 seconds
    public static final int MS_READ_PER_CHAR = 50; // 50 msrpc ~ 20 cps
    public static final String LOG_TAG = "SafeSlinger-Exchange";

    // internal interprocess communication...
    public static final class extra {
        public static final String DECOY1_HASH = "DecoyHash1";
        public static final String DECOY2_HASH = "DecoyHash2";
        public static final String FLAG_HASH = "FlagHash";
        public static final String GROUP_ID = "GroupId";
        public static final String MEMBER_DATA = "MemberData";
        public static final String MESSAGE = "Message";
        public static final String RANDOM_POS = "RandomPosition";
        public static final String REQUEST_CODE = "RequestCode";
        public static final String RESID_MSG = "ResIdMsg";
        public static final String RESID_TITLE = "ResIdTitle";
        public static final String USER_DATA = "UserData";
        public static final String USER_ID = "UserId";
        public static final String HOST_NAME = "HostName";
        public static final String NUM_USERS = "NumUsers";
    }

    public static int getVersionCode() {
        return VER_NOTIFY_BACKCOMPAT;
    }

}
