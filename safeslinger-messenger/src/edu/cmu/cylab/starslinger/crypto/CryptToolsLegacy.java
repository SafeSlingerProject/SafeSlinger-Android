
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
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.model.CryptoMsgPrivateData;
import edu.cmu.cylab.starslinger.model.PolarSslPrivateData;
import edu.cmu.cylab.starslinger.util.SSUtil;

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
            FileInputStream encKeyFile = ctx.openFileInput(getCurrentKeyFileOld());
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
    public static String getCurrentKeyFileOld() {
        int currUser = SafeSlingerPrefs.getUser();
        if (currUser == 0) {
            return FILENAME_SECKEY_LEGACY_PSSLPD;
        } else {
            return (SSUtil.getFileNameOnly(FILENAME_SECKEY_LEGACY_PSSLPD) + currUser + "." + SSUtil
                    .getFileExtensionOnly(FILENAME_SECKEY_LEGACY_PSSLPD));
        }
    }

    @Deprecated
    public static boolean existsSecretKeyOld() {
        Context ctx = SafeSlinger.getApplication();
        try {
            synchronized (SafeSlinger.sDataLock) {

                FileInputStream f = ctx.openFileInput(getCurrentKeyFileOld());
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
