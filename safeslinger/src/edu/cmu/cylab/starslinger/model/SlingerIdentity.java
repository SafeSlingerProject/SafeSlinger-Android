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

package edu.cmu.cylab.starslinger.model;

import java.nio.ByteBuffer;

import android.text.TextUtils;
import android.util.Base64;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPeerKeyFormatException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider;

public class SlingerIdentity {

    private String mPublicKeyPolar = null;
    private String mToken = "";
    private int mNotification;

    public SlingerIdentity() {
        super();
    }

    public SlingerIdentity(String token, int notification, String pubKeyPolar) {
        if (pubKeyPolar != null)
            mPublicKeyPolar = pubKeyPolar;
        if (token != null)
            mToken = token;
        mNotification = notification;
    }

    public String getPublicKey() {
        return mPublicKeyPolar;
    }

    public void setPublicKey(String public_key) {
        mPublicKeyPolar = public_key;
    }

    public int getNotification() {
        return mNotification;
    }

    public void setNotification(int notification) {
        mNotification = notification;
    }

    public String getToken() {
        return mToken;
    }

    public void setToken(String token) {
        if (token != null)
            mToken = token;
    }

    /***
     * Transform address book Base 64 public key to string form
     */
    public static String dbKey2BytesKey(String keyData) throws CryptoMsgException,
            CryptoMsgPeerKeyFormatException {
        if (keyData == null)
            return null;

        CryptoMsgProvider p = CryptoMsgProvider.createInstance(SafeSlinger.getApplication()
                .isLoggable());

        String id = p.ExtractKeyIDfromSafeSlingerString(keyData);
        if (CryptTools.isNullKeyId(id)) {
            throw new CryptoMsgException(" key id is missing");
        }
        return keyData;
    }

    /***
     * Transform address book Base 64 push token data to string form of token
     * 
     * @throws GeneralException
     */
    public static String dbPush2strPush(String tokenData) throws GeneralException {
        if (!TextUtils.isEmpty(tokenData)) {
            try {
                byte[] tok;
                byte[] t = Base64.decode(tokenData.getBytes(), Base64.NO_WRAP);
                ByteBuffer buf = ByteBuffer.wrap(t);

                int pushType = buf.getInt();
                switch (pushType) {
                    case ConfigData.NOTIFY_NOPUSH:
                    case ConfigData.NOTIFY_ANDROIDC2DM:
                    case ConfigData.NOTIFY_APPLEUA:
                        // correct code, check next item...
                        break;
                    default:
                        throw new GeneralException("Unknown push type code.");
                }

                int len = buf.getInt();
                if (len > (buf.capacity() - 8) || len < 0) {
                    return null; // unable to read
                }
                tok = new byte[len];
                buf.get(tok, 0, len);
                return new String(tok);

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return null; // unable to read
            }
        }
        return tokenData; // unable to read
    }

    /***
     * Transform address book Base 64 push token data to integer form of
     * notification type
     * 
     * @throws GeneralException
     */
    public static int dbPush2strNotify(String tokenData) throws GeneralException {
        if (!TextUtils.isEmpty(tokenData)) {
            try {
                byte[] t = Base64.decode(tokenData.getBytes(), Base64.NO_WRAP);
                ByteBuffer buf = ByteBuffer.wrap(t);

                int pushType = buf.getInt();
                switch (pushType) {
                    case ConfigData.NOTIFY_NOPUSH:
                    case ConfigData.NOTIFY_ANDROIDC2DM:
                    case ConfigData.NOTIFY_APPLEUA:
                        return pushType;
                    default:
                        throw new GeneralException("Unknown push type code.");
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return ConfigData.NOTIFY_NOPUSH;// unable to read
            }
        }
        return ConfigData.NOTIFY_NOPUSH;// unable to read
    }

    /***
     * Transform address book Base 64 public key and push token data items to
     * rich form of Slinger identity
     */
    public static SlingerIdentity dbAll2sidAll(String tokenData, String keyData) {

        SlingerIdentity si = new SlingerIdentity();

        try {
            si.setPublicKey(dbKey2BytesKey(keyData));
        } catch (CryptoMsgException e) {
            e.printStackTrace();
        } catch (CryptoMsgPeerKeyFormatException e) {
            e.printStackTrace();
        }

        try {
            si.setNotification(dbPush2strNotify(tokenData));
        } catch (GeneralException e) {
            e.printStackTrace();
        }

        try {
            si.setToken(dbPush2strPush(tokenData));
        } catch (GeneralException e) {
            e.printStackTrace();
        }

        return si;
    }

    /***
     * Transform rich Slinger identity to Base 64 push token data form for
     * address book
     */
    public static String sidPush2DBPush(SlingerIdentity slinger) {
        if (slinger != null) {
            ByteBuffer b = ByteBuffer.allocate(4 + 4 + slinger.getToken().length());
            b.putInt(slinger.getNotification());
            b.putInt(slinger.getToken().length());
            b.put(slinger.getToken().getBytes());
            return new String(Base64.encode(b.array(), Base64.NO_WRAP));
        } else {
            return "";
        }
    }

    /***
     * Transform rich Slinger identity to Base 64 public key data form for
     * address book
     */
    public static String sidKey2DBKey(SlingerIdentity slinger) {
        if (slinger != null) {
            return slinger.getPublicKey();
        } else {
            return "";
        }
    }

}
