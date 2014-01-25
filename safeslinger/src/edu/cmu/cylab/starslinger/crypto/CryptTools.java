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

package edu.cmu.cylab.starslinger.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.model.CryptoMsgPrivateData;
import edu.cmu.cylab.starslinger.util.SSUtil;
import edu.cmu.cylab.starslinger.view.BaseActivity;
import fr.cryptohash.Digest;
import fr.cryptohash.Keccak256;

public class CryptTools {

    protected static final int REST_ENC_KEYLEN = 256;

    /***
     * Number of PBKDF2 hardening rounds to use.
     */
    public static final int HARDNESS_ROUNDS = 1000;

    public static byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] buf = new byte[4096];
        int numRead = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((numRead = fis.read(buf)) > 0) {
            baos.write(buf, 0, numRead);
        }
        fis.close();
        byte[] returnVal = baos.toByteArray();
        baos.close();
        return returnVal;
    }

    public static void putSecretKey(Context ctx, CryptoMsgPrivateData secret, String passPhrase)
            throws FileNotFoundException, IOException, CryptoMsgException {

        if (TextUtils.isEmpty(passPhrase)) {
            throw new CryptoMsgException(ctx.getString(R.string.error_noPassPhrase));
        }

        // 1 - serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(baos);
        out.writeObject(secret);
        out.close();

        // 2 - encrypt
        byte[] encRaw;
        try {
            // update hardness rounds and salt on each passphrase change
            byte[] newSalt = CryptTools.generateSalt();
            int newHardness = HARDNESS_ROUNDS;
            ConfigData.savePrefKeySalt(ctx, newSalt);
            ConfigData.savePrefHardnessIterations(ctx, newHardness);
            encRaw = encryptBlobWithSaltPass(passPhrase, baos.toByteArray(), newSalt, newHardness);
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
        } catch (InvalidKeySpecException e) {
            throw new CryptoMsgException(e.getLocalizedMessage());
        }

        // 3 - write
        synchronized (SafeSlinger.sDataLock) {
            FileOutputStream encKeyFile = ctx.openFileOutput(CryptTools.getCurrentKeyFile(ctx),
                    Context.MODE_PRIVATE);
            encKeyFile.write(encRaw);
            encKeyFile.close();
        }
        ConfigData.queueBackup(ctx);
    }

    public static boolean changeSecretKeyPassphrase(Context ctx, String newPassPhrase,
            String oldPassPhrase) throws FileNotFoundException, IOException,
            ClassNotFoundException, CryptoMsgException {

        CryptoMsgPrivateData secret = getSecretKey(ctx, oldPassPhrase);
        putSecretKey(ctx, secret, newPassPhrase);
        return true;
    }

    public static CryptoMsgPrivateData getSecretKey(Context ctx, String passPhrase)
            throws IOException, ClassNotFoundException, CryptoMsgException {

        if (TextUtils.isEmpty(passPhrase)) {
            throw new CryptoMsgException(ctx.getString(R.string.error_noPassPhrase));
        }

        // 1 - read
        byte[] encRaw = null;
        synchronized (SafeSlinger.sDataLock) {
            FileInputStream encKeyFile = ctx.openFileInput(CryptTools.getCurrentKeyFile(ctx));
            encRaw = new byte[encKeyFile.available()];
            encKeyFile.read(encRaw);
            encKeyFile.close();
        }

        // 2 - decrypt
        byte[] plain;
        byte[] salt = ConfigData.loadPrefKeySalt(ctx);
        int iterations = ConfigData.loadPrefHardnessIterations(ctx);
        try {
            plain = decryptBlobWithSaltPass(passPhrase, encRaw, salt, iterations);
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
        } catch (InvalidKeySpecException e) {
            throw new CryptoMsgException(e.getLocalizedMessage());
        }

        // 3 - deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(plain);
        ObjectInputStream in = new ObjectInputStream(bis);
        CryptoMsgPrivateData secret = (CryptoMsgPrivateData) in.readObject();
        in.close();

        return secret;
    }

    public static String getCurrentKeyFile(Context ctx) {
        int currUser = ConfigData.loadPrefUser(ctx);
        if (currUser == 0) {
            return ConfigData.FILENAME_SECKEY_CRYPTOMSG;
        } else {
            return (SSUtil.getFileNameOnly(ConfigData.FILENAME_SECKEY_CRYPTOMSG) + currUser + "." + SSUtil
                    .getFileExtensionOnly(ConfigData.FILENAME_SECKEY_CRYPTOMSG));
        }
    }

    public static String getKeyFile(int userNumber) {
        if (userNumber == 0) {
            return ConfigData.FILENAME_SECKEY_CRYPTOMSG;
        } else {
            return (SSUtil.getFileNameOnly(ConfigData.FILENAME_SECKEY_CRYPTOMSG) + userNumber + "." + SSUtil
                    .getFileExtensionOnly(ConfigData.FILENAME_SECKEY_CRYPTOMSG));
        }
    }

    public static boolean existsSecretKey(Context ctx) {
        try {
            synchronized (SafeSlinger.sDataLock) {

                FileInputStream f = ctx.openFileInput(CryptTools.getCurrentKeyFile(ctx));
                f.close();
            }
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public static byte[] encryptMessage(Context ctx, byte[] plainData, String pass, byte[] pubKey)
            throws IOException, ClassNotFoundException, CryptoMsgException, GeneralException {

        byte[] cipher = null;

        if (isPubKeys(new String(pubKey))) {
            cipher = Encrypt(ctx, plainData, pass, pubKey);
        } else {
            throw new GeneralException(
                    ctx.getString(R.string.error_AllMembersMustUpgradeBadKeyFormat));
        }
        return cipher;
    }

    private static byte[] Encrypt(Context ctx, byte[] plainData, String pass, byte[] pubKey)
            throws IOException, ClassNotFoundException, CryptoMsgException {
        byte[] encFile = null;
        try {
            CryptoMsgProvider tool = CryptoMsgProvider.createInstance(SafeSlinger.getApplication()
                    .isLoggable());
            CryptoMsgPrivateData mine = CryptTools.getSecretKey(ctx, pass);

            encFile = tool.PackCipher(plainData, new String(pubKey), mine.getSignPriKey(), mine
                    .getKeyId().getBytes());
        } catch (CryptoMsgPeerKeyFormatException e) {
            throw new CryptoMsgException(ctx.getString(R.string.error_MessageInvalidPeerKeyFormat));
        }
        return encFile;
    }

    public static byte[] decryptMessage(BaseActivity act, byte[] encMsg, String pass,
            StringBuilder keyidout) throws IOException, ClassNotFoundException, CryptoMsgException,
            GeneralException {

        byte[] plainMsg = null;

        if (isEncMsg(encMsg)) {
            plainMsg = Decrypt(act, encMsg, pass, keyidout);
        } else {
            throw new GeneralException(
                    act.getString(R.string.error_AllMembersMustUpgradeBadKeyFormat));
        }
        return plainMsg;
    }

    private static byte[] Decrypt(final BaseActivity act, byte[] encData, String pass,
            StringBuilder keyidout) throws IOException, ClassNotFoundException, CryptoMsgException {
        try {
            CryptoMsgProvider tool = CryptoMsgProvider.createInstance(SafeSlinger.getApplication()
                    .isLoggable());
            CryptoMsgPrivateData mine = CryptTools.getSecretKey(act.getApplicationContext(), pass);
            keyidout.append(tool.ExtractKeyIDfromPacket(encData));
            String theirs = act.getSignersContact(keyidout.toString());

            if (TextUtils.isEmpty(theirs)) {
                throw new CryptoMsgException(act.getString(R.string.error_UnableFindPubKey));
            }

            byte[] plain = tool.UnPackCipher(encData, theirs, mine.getEncPubKey(),
                    mine.getEncPriKey());
            return plain;

        } catch (CryptoMsgPacketSizeException e) {
            throw new CryptoMsgException(act.getString(R.string.error_InvalidIncomingMessage));
        } catch (CryptoMsgPeerKeyFormatException e) {
            throw new CryptoMsgException(act.getString(R.string.error_MessageInvalidPeerKeyFormat));
        } catch (CryptoMsgSignatureVerificationException e) {
            throw new CryptoMsgException(
                    act.getString(R.string.error_MessageSignatureVerificationFails));
        }
    }

    public static boolean isNullKeyId(String keyId) {
        if (keyId == null) {
            return true;
        }
        byte[] rawkeyid;
        try {
            rawkeyid = Base64.decode(keyId.getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            return true;
        } catch (IllegalArgumentException e) {
            return true;
        }
        if (rawkeyid == null) {
            return true;
        }
        if (rawkeyid.length == 0) {
            return true;
        }
        if (rawkeyid.length == (Long.SIZE / 8)) {
            if (ByteBuffer.wrap(rawkeyid).getLong() == 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean is64BitKeyId(String keyId) {
        if (keyId == null) {
            return false;
        }
        byte[] rawkeyid;
        try {
            rawkeyid = Base64.decode(keyId.getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (rawkeyid == null) {
            return false;
        }
        if (rawkeyid.length == (Long.SIZE / 8)) {
            return true;
        }

        return false;
    }

    public static boolean isPubKeys(String pubKeys) {
        if (TextUtils.isEmpty(pubKeys)) {
            return false;
        }
        CryptoMsgProvider tool = CryptoMsgProvider.createInstance(SafeSlinger.getApplication()
                .isLoggable());
        try {
            // test key format to ensure proper parsing...
            String keyId = tool.ExtractKeyIDfromSafeSlingerString(pubKeys);
            if (!isArrayByteBase64(keyId.getBytes())) {
                return false;
            }
            return true;
        } catch (NullPointerException e) {
            return false;
        } catch (CryptoMsgPeerKeyFormatException e) {
            return false;
        }
    }

    private static boolean isEncMsg(byte[] cipher) {

        if (cipher == null || cipher.length == 0) {
            return false;
        }
        CryptoMsgProvider tool = CryptoMsgProvider.createInstance(SafeSlinger.getApplication()
                .isLoggable());
        try {
            // test key format to ensure proper parsing...
            String keyId = tool.ExtractKeyIDfromPacket(cipher);
            if (!isArrayByteBase64(keyId.getBytes())) {
                return false;
            }
            return !is64BitKeyId(keyId);
        } catch (NullPointerException e) {
            return false;
        } catch (CryptoMsgPacketSizeException e) {
            return false;
        }
    }

    public static byte[] encryptBlobWithSaltPass(String pass, byte[] cleartext, byte[] salt,
            int iterations) throws NoSuchAlgorithmException, InvalidKeyException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException,
            NoSuchProviderException, InvalidKeySpecException, UnsupportedEncodingException {

        byte[] rawKey = generatePBKDF2Key(pass, salt, iterations);
        byte[] result = encryptWithRawKey(rawKey, cleartext);
        return result;
    }

    public static byte[] decryptBlobWithSaltPass(String pass, byte[] encrypted, byte[] salt,
            int iterations) throws NoSuchAlgorithmException, InvalidKeyException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException,
            NoSuchProviderException, InvalidKeySpecException, UnsupportedEncodingException {

        byte[] rawKey = generatePBKDF2Key(pass, salt, iterations);
        byte[] result = decryptWithRawKey(rawKey, encrypted);
        return result;
    }

    public static byte[] generateSalt() throws NoSuchAlgorithmException {
        // try to keep entropy size to match PBKDF2WithHmacSHA1
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[20];
        sr.nextBytes(salt);
        return salt;
    }

    public static byte[] generatePBKDF2Key(String pass, byte[] salt, int iterations)
            throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        // Traditional key factory. Will use lower 8-bits of passphrase
        // chars on older Android versions (API level 18 and lower) and all
        // available bits on KitKat and newer (API level 19 and higher).
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

        // To avoid read/write errors we manually convert the passphrase
        // to use all unicode characters before writing the format.
        char[] passWithAllUnicodeBytes = new String(pass.getBytes(), "UTF-8").toCharArray();

        KeySpec keySpec = new PBEKeySpec(passWithAllUnicodeBytes, salt, iterations, REST_ENC_KEYLEN);
        SecretKey secretKey = factory.generateSecret(keySpec);
        return secretKey.getEncoded();
    }

    protected static byte[] encryptWithRawKey(byte[] raw, byte[] clear)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }

    protected static byte[] decryptWithRawKey(byte[] raw, byte[] encrypted)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

    public static byte[] computeSha3Hash(byte[] buffer) {
        Digest sha3 = new Keccak256();
        return sha3.digest(buffer);
    }

    public static byte[] computeSha3Hash2(byte[] buffer1, byte[] buffer2) {
        ByteBuffer join = ByteBuffer.allocate(buffer1.length + buffer2.length);
        join.put(buffer1);
        join.put(buffer2);
        return computeSha3Hash(join.array());
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
