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

package edu.cmu.cylab.starslinger.crypto;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.model.CryptoMsgPrivateData;
import edu.cmu.cylab.starslinger.model.PolarSslPrivateData;

@Deprecated
public class CryptToolsLegacy extends CryptTools {

    @Deprecated
    public static final String FILENAME_SECKEY_LEGACY_PSSLPD = "SafeSlingerSecretKeyPolarSsl.enc";

    @Deprecated
    public static boolean updateKeyFormatOld(String pass) {
        try {
            PolarSslPrivateData secretOld = getSecretKeyOld(pass);

            String signpubkey = secretOld.getSignPubKey();
            String encpubkey = secretOld.getEncPubKey();
            String signprikey = secretOld.getSignPriKey();
            String encprikey = secretOld.getEncPriKey();
            String keyid = secretOld.getKeyId();
            long gendate = secretOld.getGenDate();
            String safeslingerstring = secretOld.getSafeSlingerString();
            CryptoMsgPrivateData secret = new CryptoMsgPrivateData(signpubkey, encpubkey,
                    signprikey, encprikey, keyid, gendate, safeslingerstring);

            putSecretKey(secret, pass);
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CryptoMsgException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Deprecated
    public static PolarSslPrivateData getSecretKeyOld(String passPhrase) throws IOException,
            ClassNotFoundException, CryptoMsgException {
        Context ctx = SafeSlinger.getApplication();

        if (TextUtils.isEmpty(passPhrase)) {
            throw new CryptoMsgException(ctx.getString(R.string.error_noPassPhrase));
        }

        // 1 - read
        byte[] encRaw = null;
        synchronized (SafeSlinger.sDataLock) {
            FileInputStream encKeyFile = ctx.openFileInput(getKeyFileOld());
            encRaw = new byte[encKeyFile.available()];
            encKeyFile.read(encRaw);
            encKeyFile.close();
        }

        // 2 - decrypt
        byte[] plain;
        try {
            plain = decryptBlobWithPass(passPhrase, encRaw);
        } catch (InvalidKeyException e) {
            throw new CryptoMsgException(e.getLocalizedMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoMsgException(e.getLocalizedMessage());
        } catch (NoSuchProviderException e) {
            throw new CryptoMsgException(e.getLocalizedMessage());
        } catch (NoSuchPaddingException e) {
            throw new CryptoMsgException(e.getLocalizedMessage());
        } catch (IllegalBlockSizeException e) {
            throw new CryptoMsgException(e.getLocalizedMessage());
        } catch (BadPaddingException e) {
            throw new CryptoMsgException(e.getLocalizedMessage());
        }

        // 3 - deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(plain);
        ObjectInputStream in = new ObjectInputStream(bis);
        PolarSslPrivateData secret = (PolarSslPrivateData) in.readObject();
        in.close();

        return secret;
    }

    @Deprecated
    private static byte[] getRawKey(byte[] pass, boolean usePreApi17Prng)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom sr;

        if (usePreApi17Prng) {
            sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
        } else {
            sr = SecureRandom.getInstance("SHA1PRNG");
        }
        sr.setSeed(pass);
        kgen.init(REST_ENC_KEYLEN, sr);
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        return raw;
    }

    @Deprecated
    public static String getKeyFileOld() {
        // multiple keys not allowed in legacy crypto releases,
        // so only check main key file
        return FILENAME_SECKEY_LEGACY_PSSLPD;
    }

    @Deprecated
    public static boolean existsSecretKeyOld() {
        Context ctx = SafeSlinger.getApplication();
        try {
            synchronized (SafeSlinger.sDataLock) {

                FileInputStream f = ctx.openFileInput(getKeyFileOld());
                f.close();
            }
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    @Deprecated
    public static byte[] encryptBlobWithPass(String seed, byte[] cleartext)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {

        boolean usePreApi17Prng = (Build.VERSION.SDK_INT >= 17);

        byte[] rawKey = getRawKey(seed.getBytes(), usePreApi17Prng);
        byte[] result = encryptWithRawKey(rawKey, cleartext);
        return result;
    }

    @Deprecated
    public static byte[] decryptBlobWithPass(String seed, byte[] encrypted)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
        byte[] rawKey;
        byte[] result;

        try {
            // first, try with PRNG from API 17+
            rawKey = getRawKey(seed.getBytes(), false);
            result = decryptWithRawKey(rawKey, encrypted);
        } catch (BadPaddingException e) {
            // now, try with PRNG from API <17
            rawKey = getRawKey(seed.getBytes(), true);
            result = decryptWithRawKey(rawKey, encrypted);
        }
        return result;
    }
}
