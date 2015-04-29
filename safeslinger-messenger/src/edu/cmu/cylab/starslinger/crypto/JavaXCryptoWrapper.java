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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;
import android.util.Log;

public class JavaXCryptoWrapper extends CryptoMsgProvider {

    private static final String TAG = JavaXCryptoWrapper.class.getSimpleName();

    private static final int ENCKEYSIZE = 2048;
    private static final int SIGNKEYSIZE = 1024;
    private static final int EncodeKeyLen1 = 392;
    private static final int EncodeKeyLen2 = 609;

    private static final String BEGIN_RSA_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n";
    private static final String END_RSA_PRIVATE_KEY = "-----END RSA PRIVATE KEY-----\n";

    /** Block size of entropy accumulator (SHA-512) */
    private static final int ENTROPY_BLOCK_SIZE = 64;

    private String sign_pub = null;
    private String sign_pri = null;
    private String enc_pub = null;
    private String enc_pri = null;
    boolean DebugFlag = false;

    @Override
    protected void DebugFlag(boolean input) {
        DebugFlag = input;
    }

    @Override
    protected byte[] GetKeyID(byte[] keyStream) {
        try {
            ByteBuffer keystream = ByteBuffer.wrap(keyStream);
            int keylen;
            byte[] keyidarray = new byte[64];
            byte[] tmpbuf = new byte[1024];

            // copy key to key buffer
            keylen = keystream.capacity();

            CryptLog.i(DebugFlag, TAG, String.format("Read KeyStream Size(GetKeyID) = %d", keylen));
            // buffer overflow detection
            if (keylen != EncodeKeyLen2) {
                Log.e(TAG, "Input Key has invalid size.");
                return null;
            }
            // copy from keystream
            keystream.get(tmpbuf, 0, keylen);
            // compute sha512 hash
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(tmpbuf, 0, keylen);
            keyidarray = md.digest();

            // base64 encode
            byte[] encoded = Base64.encode(keyidarray, Base64.NO_WRAP);
            if (encoded == null) {
                return null;
            }
            CryptLog.i(DebugFlag, TAG, String.format("Based64 Encoded KeyID = %s, size = %d",
                    new String(encoded), encoded.length));

            return encoded;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected byte[] KeyHMAC(byte[] input) {
        try {
            int keylen, i;
            byte[] hash = new byte[20];
            byte[] tmpbuf = new byte[1024];

            // copy key to key buffer
            ByteBuffer keystream = ByteBuffer.wrap(input);
            keylen = keystream.capacity();
            keystream.get(tmpbuf, 0, keylen);

            CryptLog.i(DebugFlag, TAG, String.format("Read KeyStream Size(KeyHMAC) = %d", keylen));

            // buffer overflow detection
            if (keylen != EncodeKeyLen1) {
                Log.e(TAG, "Input Key has invalid size.");
                return null;
            }

            // compute sha1 hash
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(tmpbuf, 0, keylen);
            hash = md.digest();

            if (hash == null) {
                return null;
            }
            if (DebugFlag) {
                CryptLog.i(DebugFlag, TAG, "HMAC: ");
                for (i = 0; i < 20; i++) {
                    CryptLog.i(DebugFlag, TAG, String.format("%02X ", hash[i]));
                }
                CryptLog.i(DebugFlag, TAG, "END OF HMAC");
            }
            return hash;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected byte[] encodeKey(byte[] plain) {
        int plen;

        // copy plain to key buffer
        plen = plain.length;
        CryptLog.i(DebugFlag, TAG, String.format("Read KeyStream Size = %d", plen));

        if (plen == 0) {
            Log.e(TAG, "Plain size is 0.");
            return null;
        }

        // base64 encode
        byte[] encodedData = Base64.encode(plain, Base64.NO_WRAP);
        if (encodedData == null) {
            return null;
        }
        CryptLog.i(DebugFlag, TAG, String.format("Based64 Encoded Data = %s, size = %d",
                new String(encodedData), encodedData.length));

        return encodedData;
    }

    @Override
    protected byte[] decodeKey(byte[] encode) {
        int clen;

        // copy plain to key buffer
        clen = encode.length;
        CryptLog.i(DebugFlag, TAG, String.format("Encode Size = %d", clen));
        if (clen == 0) {
            Log.e(TAG, "Encode data is 0.");
            return null;
        }

        // base64 decode
        byte[] decoded;
        try {
            decoded = Base64.decode(encode, Base64.NO_WRAP);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
        if (decoded == null) {
            return null;
        }
        CryptLog.i(DebugFlag, TAG, String.format("Based64 Decoded Data = %s, size = %d",
                new String(decoded), decoded.length));

        return decoded;
    }

    @Override
    protected byte[] GetRandomKey() {
        CryptLog.i(DebugFlag, TAG, "Gen AES Session Key Start.");

        // cipher
        SecureRandom random = new SecureRandom();
        byte[] sessionKey = new byte[ENTROPY_BLOCK_SIZE];
        random.nextBytes(sessionKey);
        if (sessionKey == null || sessionKey.length != ENTROPY_BLOCK_SIZE) {
            Log.e(TAG, "Get Random Failed.");
            return null;
        }

        CryptLog.i(DebugFlag, TAG, "Gen AES Session Key End.");
        return sessionKey;
    }

    @Override
    protected byte[] AESEncrypt(byte[] text, byte[] key) {
        try {
            int i, n, blk;
            int psize, offset, totaloff;
            int keylen;
            byte[] IV = new byte[16];
            byte[] keyarray = new byte[ENTROPY_BLOCK_SIZE];
            byte[] digest = new byte[32];
            ByteBuffer source;
            ByteBuffer output;

            psize = text.length;
            CryptLog.i(DebugFlag, TAG, String.format("Plain Len = %d.", psize));

            blk = (int) Math.ceil(psize / 16.0f);
            CryptLog.i(DebugFlag, TAG, String.format("Plain has %d blocks", blk));
            CryptLog.i(DebugFlag, TAG, String.format("Cipher has %d bytes", blk * 16 + 16 + 32));

            output = ByteBuffer.allocate(blk * 16 + 16 + 32);
            if (output == null) {
                Log.e(TAG, "Out of memory, failure at memory allocation.");
                return null;
            }

            // copy key to key buffer
            keylen = key.length;
            if (keylen != ENTROPY_BLOCK_SIZE) {
                Log.e(TAG, "AES Key Length is invalid.");
                return null;
            }
            keyarray = key;

            source = ByteBuffer.wrap(text);

            /*
             * IV = 0
             */
            IV = new byte[16];

            /*
             * Append the IV at the beginning of the output.
             */
            totaloff = 0;
            output.put(IV);
            totaloff = totaloff + 16;

            /*
             * Hash the IV and the secret key together 8192 times using the
             * result to setup the AES context and HMAC.
             */
            digest = new byte[32];

            for (i = 0; i < 8192; i++) {
                MessageDigest sha2 = MessageDigest.getInstance("SHA-256");
                sha2.update(digest);
                sha2.update(keyarray);
                digest = sha2.digest();
            }

            keyarray = new byte[keyarray.length];
            SecretKeySpec skeySpec = new SecretKeySpec(digest, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            try {
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            } catch (InvalidKeyException e) {
                e.printStackTrace();
                return null;
            }

            final Mac sha2_hmac = Mac.getInstance("HmacSHA256");
            final SecretKeySpec secret_key = new javax.crypto.spec.SecretKeySpec(digest,
                    "HmacSHA256");
            sha2_hmac.init(secret_key);

            /*
             * Encrypt and write the ciphertext.
             */
            for (offset = 0; offset < psize; offset += 16) {
                n = (psize - offset > 16) ? 16 : (int) (psize - offset);
                byte[] buffer = new byte[16];
                source.get(buffer, 0, n);
                for (i = 0; i < n; i++) {
                    buffer[i] = (byte) (buffer[i] ^ IV[i]);
                }

                buffer = cipher.update(buffer);
                sha2_hmac.update(buffer);
                output.put(buffer);
                totaloff = totaloff + 16;

                IV = buffer;
            }

            /*
             * Finally write the HMAC.
             */
            digest = sha2_hmac.doFinal();
            output.put(digest);
            CryptLog.i(DebugFlag, TAG, String.format("Total cipher len = %d", output.limit()));

            return output.array();

        } catch (NoSuchAlgorithmException e) {
            CryptLog.e(DebugFlag, TAG, e.getMessage());
            e.printStackTrace();
            return null;
        } catch (NoSuchPaddingException e) {
            CryptLog.e(DebugFlag, TAG, e.getMessage());
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            CryptLog.e(DebugFlag, TAG, e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected byte[] AESDecrypt(byte[] cipher, byte[] key, int plen) {
        try {
            int i, n, blk;
            int csize, offset, totaloffset;
            int lastn, keylen;
            byte[] IV = new byte[16];
            byte[] keyarray = new byte[ENTROPY_BLOCK_SIZE];
            byte[] digest = new byte[32];
            byte[] buffer = new byte[32];
            byte[] tmp = new byte[16];
            ByteBuffer source;
            ByteBuffer output;

            csize = cipher.length;
            CryptLog.i(DebugFlag, TAG, String.format("Cipher Len = %d.", csize));

            if (csize < 48) {
                Log.e(TAG, "Ciphertext is too short to be encrypted.");
                return null;
            }

            if ((csize & 0x0F) != 0) {
                Log.e(TAG, "File size not a multiple of 16.");
                return null;
            }

            blk = (int) Math.ceil((csize - 16 - 32) / 16.0f);
            CryptLog.i(DebugFlag, TAG, String.format("Plain has %d blocks", blk));

            output = ByteBuffer.allocate(blk * 16);
            if (output == null) {
                Log.e(TAG, "Out of memory. Failure on memory allocation.");
                return null;
            }

            // copy key to key buffer
            keylen = key.length;
            if (keylen != ENTROPY_BLOCK_SIZE) {
                Log.e(TAG, "AES Key Length is invalid.");
                return null;
            }
            keyarray = key;

            // get cipher pointer
            source = ByteBuffer.wrap(cipher);

            /*
             * Substract the IV + HMAC length.
             */
            csize -= (16 + 32);

            /*
             * Read the IV, IV should be 0
             */
            IV = new byte[16];
            source.get(IV);

            lastn = plen & 0x0F;

            /*
             * Hash the IV and the secret key together 8192 times using the
             * result to setup the AES context and HMAC.
             */
            digest = new byte[32];

            for (i = 0; i < 8192; i++) {
                MessageDigest sha2 = MessageDigest.getInstance("SHA-256");
                sha2.update(digest);
                sha2.update(keyarray);
                digest = sha2.digest();
            }

            keyarray = new byte[keyarray.length];
            SecretKeySpec skeySpec = new SecretKeySpec(digest, "AES");
            Cipher ciphr = Cipher.getInstance("AES/ECB/NoPadding");
            try {
                ciphr.init(Cipher.DECRYPT_MODE, skeySpec);
            } catch (InvalidKeyException e) {
                e.printStackTrace();
                return null;
            }

            Mac sha2_hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new javax.crypto.spec.SecretKeySpec(digest, "HmacSHA256");
            sha2_hmac.init(secret_key);

            /*
             * Decrypt and write the plaintext.
             */
            totaloffset = 0;
            for (offset = 0; offset < csize; offset += 16) {

                buffer = new byte[16];
                source.get(buffer, 0, 16);

                tmp = buffer;
                sha2_hmac.update(buffer);
                buffer = ciphr.update(buffer);

                for (i = 0; i < 16; i++) {
                    buffer[i] = (byte) (buffer[i] ^ IV[i]);
                }

                IV = tmp;
                n = (lastn > 0 && offset == csize - 16) ? lastn : 16;
                output.put(buffer, 0, n);
                totaloffset = totaloffset + n;
            }

            CryptLog.i(DebugFlag, TAG, String.format("Plaintext has %d bytes.", totaloffset));

            /*
             * Verify the message authentication code.
             */
            digest = sha2_hmac.doFinal();

            buffer = new byte[32];
            source.get(buffer, 0, 32);

            if (!Arrays.equals(digest, buffer)) {
                Log.e(TAG, "HMAC check failed: wrong key, or ciphertext corrupted.");
                return null;
            }

            // decipher
            byte[] decipher = new byte[totaloffset];
            output.position(0);
            output.get(decipher, 0, totaloffset);
            return decipher;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            CryptLog.e(DebugFlag, TAG, e.getMessage());
            return null;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            CryptLog.e(DebugFlag, TAG, e.getMessage());
            return null;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            CryptLog.e(DebugFlag, TAG, e.getMessage());
            return null;
        }
    }

    @Override
    protected boolean GenSignKeyPair() {
        try {
            // Seeding the random number generator...
            CryptLog.i(DebugFlag, TAG, "Seeding the random number generator...");

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(SIGNKEYSIZE,
                    RSAKeyGenParameterSpec.F4);
            kpg.initialize(spec);

            // Generating the RSA key
            KeyPair keyPair = kpg.generateKeyPair();
            if (keyPair == null) {
                Log.e(TAG, "cannot generate RSA-SIGN key pair object.");
                return false;
            }

            // encode public key using X509 format
            X509EncodedKeySpec x509Object = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
            byte[] encodeKey = Base64.encode(x509Object.getEncoded(), Base64.NO_WRAP);

            if (encodeKey == null) {
                return false;
            }
            CryptLog.i(DebugFlag, TAG, String.format("Public Key has %d bytes.", encodeKey.length));
            sign_pub = new String(encodeKey, "UTF-8");

            // output private key
            RSAPrivateCrtKey rk = (RSAPrivateCrtKey) keyPair.getPrivate();

            // output private key
            ByteBuffer output = ByteBuffer.wrap(new byte[1024]);
            // version 02 01 00
            output.put((byte) 0x02);
            output.put((byte) 0x01);
            output.put((byte) 0x00);

            // BigInteger Bn
            AddDerInt(output, rk.getModulus());
            // BigInteger Be
            AddDerInt(output, rk.getPublicExponent());
            // BigInteger Bd
            AddDerInt(output, rk.getPrivateExponent());
            // BigInteger Bp
            AddDerInt(output, rk.getPrimeP());
            // BigInteger Bq
            AddDerInt(output, rk.getPrimeQ());
            // BigInteger Bep
            AddDerInt(output, rk.getPrimeExponentP());
            // BigInteger Beq
            AddDerInt(output, rk.getPrimeExponentQ());
            // BigInteger Bc
            AddDerInt(output, rk.getCrtCoefficient());
            // total length header
            int curlen = output.position();
            byte[] dump = new byte[curlen];

            output.position(0);
            output.get(dump, 0, curlen);

            ByteBuffer header = ByteBuffer.wrap(new byte[1024]);
            header.put((byte) 0x30);
            if (curlen < 128)
                header.put((byte) curlen);
            else {
                if (curlen > 0xFF) {
                    header.put((byte) 0x82);
                    header.put((byte) ((curlen >> 8) & 0xFF));
                    header.put((byte) (curlen & 0xFF));
                } else {
                    header.put((byte) 0x81);
                    header.put((byte) (curlen & 0xFF));
                }
            }

            header.put(dump);
            curlen = header.position();
            dump = new byte[curlen];
            header.position(0);
            header.get(dump, 0, curlen);

            String key = new String(Base64.encode(dump, Base64.NO_WRAP), "UTF-8");

            StringBuffer pem = new StringBuffer(BEGIN_RSA_PRIVATE_KEY);
            int len = key.length();
            for (int idx = 0; idx < len; idx += 64) {
                pem.append(key.substring(idx, (idx + 64) < len ? (idx + 64) : len) + "\n");
            }
            pem.append(END_RSA_PRIVATE_KEY);
            sign_pri = pem.toString();
            if (sign_pri == null) {
                return false;
            }
            CryptLog.i(DebugFlag, TAG,
                    String.format("Private Key has %d bytes.", sign_pri.length()));
            CryptLog.i(DebugFlag, TAG, "Finish RSA-SIGN key generation.");
            return true;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return false;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected boolean GenEncKeyPair() {
        try {
            // Seeding the random number generator...
            CryptLog.i(DebugFlag, TAG, "Seeding the random number generator...");

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(ENCKEYSIZE,
                    RSAKeyGenParameterSpec.F4);
            kpg.initialize(spec);

            // Generating the RSA key
            KeyPair keyPair = kpg.generateKeyPair();
            if (keyPair == null) {
                Log.e(TAG, "cannot generate RSA-ENC key pair object.");
                return false;
            }

            // encode public key by base64 encoding
            X509EncodedKeySpec x509Object = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
            byte[] encodeKey = Base64.encode(x509Object.getEncoded(), Base64.NO_WRAP);

            if (encodeKey == null) {
                return false;
            }
            enc_pub = new String(encodeKey, "UTF-8");
            CryptLog.i(DebugFlag, TAG, String.format("Public Key has %d bytes.", enc_pub.length()));

            // output private key
            RSAPrivateCrtKey rk = (RSAPrivateCrtKey) keyPair.getPrivate();

            // output private key
            ByteBuffer output = ByteBuffer.wrap(new byte[2048]);
            // version 02 01 00
            output.put((byte) 0x02);
            output.put((byte) 0x01);
            output.put((byte) 0x00);

            // BigInteger Bn
            AddDerInt(output, rk.getModulus());
            // BigInteger Be
            AddDerInt(output, rk.getPublicExponent());
            // BigInteger Bd
            AddDerInt(output, rk.getPrivateExponent());
            // BigInteger Bp
            AddDerInt(output, rk.getPrimeP());
            // BigInteger Bq
            AddDerInt(output, rk.getPrimeQ());
            // BigInteger Bep
            AddDerInt(output, rk.getPrimeExponentP());
            // BigInteger Beq
            AddDerInt(output, rk.getPrimeExponentQ());
            // BigInteger Bc
            AddDerInt(output, rk.getCrtCoefficient());

            // total length header
            int curlen = output.position();
            byte[] dump = new byte[curlen];

            output.position(0);
            output.get(dump, 0, curlen);

            ByteBuffer header = ByteBuffer.wrap(new byte[2048]);
            header.put((byte) 0x30);
            if (curlen < 128)
                header.put((byte) curlen);
            else {
                if (curlen > 0xFF) {
                    header.put((byte) 0x82);
                    header.put((byte) ((curlen >> 8) & 0xFF));
                    header.put((byte) (curlen & 0xFF));
                } else {
                    header.put((byte) 0x81);
                    header.put((byte) (curlen & 0xFF));
                }
            }

            header.put(dump);
            curlen = header.position();
            dump = new byte[curlen];
            header.position(0);
            header.get(dump, 0, curlen);

            String key = new String(Base64.encode(dump, Base64.NO_WRAP), "UTF-8");

            StringBuffer pem = new StringBuffer(BEGIN_RSA_PRIVATE_KEY);
            int len = key.length();
            for (int idx = 0; idx < len; idx += 64) {
                pem.append(key.substring(idx, (idx + 64) < len ? (idx + 64) : len) + "\n");
            }
            pem.append(END_RSA_PRIVATE_KEY);
            enc_pri = pem.toString();
            if (enc_pri == null) {
                return false;
            }
            CryptLog.i(DebugFlag, TAG, String.format("Private Key has %d bytes.", enc_pri.length()));

            CryptLog.i(DebugFlag, TAG, "Finish RSA-ENC key generation.");

            return true;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return false;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected String GetSignPKey() {
        return sign_pub;
    }

    @Override
    protected String GetSignRKey() {
        return sign_pri;
    }

    @Override
    protected String GetEncPKey() {
        return enc_pub;
    }

    @Override
    protected String GetEncRKey() {
        return enc_pri;
    }

    private static void AddDerInt(ByteBuffer output, BigInteger num) {
        byte[] rawbyte = num.toByteArray();
        output.put((byte) 0x02);
        if (rawbyte.length < 128)
            output.put((byte) rawbyte.length);
        else {
            if (rawbyte.length > 0xFF) {
                output.put((byte) 0x82);
                output.put((byte) ((rawbyte.length >> 8) & 0xFF));
                output.put((byte) (rawbyte.length & 0xFF));
            } else {
                output.put((byte) 0x81);
                output.put((byte) (rawbyte.length & 0xFF));
            }
        }
        output.put(rawbyte);
    }

    private static BigInteger derint(ByteBuffer input) {
        byte[] value = new byte[der(input, 0x02)];
        input.get(value);
        return new BigInteger(+1, value);
    }

    private static int der(ByteBuffer input, int exp) {
        int tag = input.get() & 0xFF;
        if (tag != exp)
            throw new IllegalArgumentException("Unexpected tag");
        int n = input.get() & 0xFF;
        if (n < 128)
            return n;
        n &= 0x7F;
        if ((n < 1) || (n > 2))
            throw new IllegalArgumentException("Invalid length");
        int len = 0;
        while (n-- > 0) {
            len <<= 8;
            len |= input.get() & 0xFF;
        }
        return len;
    }

    @Override
    public byte[] Sign(String prikey, byte[] ptext) {
        try {
            CryptLog.i(DebugFlag, TAG, " Start Sign operation ..");

            byte[] signature = new byte[512];
            byte[] RawPrikey = null;
            int plen = 0, klen = 0;

            klen = prikey.length();
            plen = ptext.length;

            CryptLog.i(DebugFlag, TAG, String.format("klen = %d, plen = %d", klen, plen));

            if ((plen == 0) || (klen == 0)) {
                Log.e(TAG, "failed!  text or Key is null.");
                return null;
            }

            CryptLog.i(DebugFlag, TAG, " Loading the private key ..");
            Signature signer = Signature.getInstance("SHA1withRSA");

            // read key
            // copy to prikey structure
            prikey = prikey.replaceAll(BEGIN_RSA_PRIVATE_KEY, "");
            prikey = prikey.replaceAll(END_RSA_PRIVATE_KEY, "");
            try {
                RawPrikey = Base64.decode(prikey.getBytes(), Base64.CRLF);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return null;
            }
            CryptLog.i(DebugFlag, TAG, " sign_pri len = " + RawPrikey.length);

            // manual parsing
            ByteBuffer input = ByteBuffer.wrap(RawPrikey);

            if (der(input, 0x30) != input.remaining())
                throw new IllegalArgumentException("Excess data");
            if (!BigInteger.ZERO.equals(derint(input)))
                throw new IllegalArgumentException("Unsupported version");

            BigInteger Bn = derint(input);
            BigInteger Be = derint(input);
            BigInteger Bd = derint(input);
            BigInteger Bp = derint(input);
            BigInteger Bq = derint(input);
            BigInteger Bep = derint(input);
            BigInteger Beq = derint(input);
            BigInteger Bc = derint(input);

            RSAPrivateCrtKeySpec pvtspec = new RSAPrivateCrtKeySpec(Bn, Be, Bd, Bp, Bq, Bep, Beq,
                    Bc);

            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey pk;
            try {
                pk = kf.generatePrivate(pvtspec);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, " failed!  generating private key spec");
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return null;
            }
            try {
                signer.initSign(pk);
            } catch (InvalidKeyException e) {
                Log.e(TAG, " failed!  initializing signature");
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return null;
            }

            CryptLog.i(DebugFlag, TAG, "loading private key ok.");

            /*
             * Compute the SHA-1 hash of the input file, then calculate the RSA
             * signature of the hash.
             */
            CryptLog.i(DebugFlag, TAG, "Generating the RSA/SHA-1 signature");

            try {
                signer.update(ptext, 0, ptext.length);
                signature = signer.sign();
            } catch (SignatureException e) {
                Log.e(TAG, " failed!  generating signature");
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return null;
            }

            CryptLog.i(DebugFlag, TAG, String.format("Signature size: %d", signature.length));
            return signature;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    protected boolean Verify(String pubkey, byte[] ptext, byte[] signature) {
        try {
            byte[] RawPubkey = null;
            int olen = 0;

            // Load public key first
            CryptLog.i(DebugFlag, TAG, " Loading the public key ..");

            // rsa_init( rsa, RSA_PKCS_V15, 0 );
            Signature signer = Signature.getInstance("SHA1withRSA");

            // format public key first
            olen = pubkey.length();
            CryptLog.i(DebugFlag, TAG, String.format(" KeyLen = %d", olen));
            try {
                RawPubkey = Base64.decode(pubkey.getBytes("UTF-8"), Base64.CRLF);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            }

            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec ks = new X509EncodedKeySpec(RawPubkey);
            PublicKey pk;
            try {
                pk = kf.generatePublic(ks);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, " failed!  generating public key spec");
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return false;
            }

            try {
                signer.initVerify(pk);
            } catch (InvalidKeyException e) {
                Log.e(TAG, " failed!  initializing verification");
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return false;
            }

            // base64 decoding for signature file
            olen = signature.length;
            CryptLog.i(DebugFlag, TAG, String.format(" SigLen = %d", olen));
            CryptLog.i(DebugFlag, TAG, " Verifying the RSA/SHA-1 signature");
            try {
                signer.update(ptext, 0, ptext.length);
                if (!signer.verify(signature)) {
                    Log.e(TAG, " failed! signature verification");
                    return false;
                }
            } catch (SignatureException e) {
                Log.e(TAG, " failed! signature verification: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            CryptLog.i(DebugFlag, TAG, " OK (the decrypted SHA-1 hash matches)");

            return true;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected byte[] Encrypt(String pubkey, byte[] ptext) {
        try {
            int plen;
            int olen;
            byte[] ppoint;
            byte[] buf = new byte[1024];
            byte[] RawPubkey = null;

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            // format public key first
            olen = pubkey.length();
            CryptLog.i(DebugFlag, TAG, String.format(" KeyLen = %d", olen));

            try {
                RawPubkey = Base64.decode(pubkey.getBytes("UTF-8"), Base64.CRLF);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return null;
            }

            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec ks = new X509EncodedKeySpec(RawPubkey);
            PublicKey pk;
            try {
                pk = kf.generatePublic(ks);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, " failed!  generating public key spec");
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return null;
            }
            CryptLog.i(DebugFlag, TAG, String.format(" key len = %d", RawPubkey.length));

            plen = ptext.length;
            CryptLog.i(DebugFlag, TAG, String.format("plen = %d", plen));

            if (plen > 200) {
                Log.e(TAG, " Input data larger than 200 bytes.");
                return null;
            }

            CryptLog.i(DebugFlag, TAG, " Generating the RSA encrypted value");

            ppoint = ptext;
            try {
                cipher.init(Cipher.ENCRYPT_MODE, pk);
            } catch (InvalidKeyException e) {
                Log.e(TAG, " failed!  initializing encryption");
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return null;
            }
            try {
                buf = cipher.doFinal(ppoint);
            } catch (IllegalBlockSizeException e) {
                Log.e(TAG, " failed!  encrypting...");
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return null;
            } catch (BadPaddingException e) {
                Log.e(TAG, " failed!  encrypting...");
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return null;
            }

            CryptLog.i(DebugFlag, TAG, " Public Key Encryption Done.");
            return buf;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected byte[] Decrypt(String prikey, byte[] ctext) {
        try {
            int klen = 0;
            byte[] result = new byte[512];
            byte[] buf;
            byte[] RawPrikey = null;

            klen = prikey.length();
            if (klen == 0) {
                Log.e(TAG, "failed! Key String is null.");
                return null;
            }

            CryptLog.i(DebugFlag, TAG, " Loading the private key ..");
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            // read key
            // copy to prikey structure
            prikey = prikey.replaceAll(BEGIN_RSA_PRIVATE_KEY, "");
            prikey = prikey.replaceAll(END_RSA_PRIVATE_KEY, "");
            try {
                RawPrikey = Base64.decode(prikey.getBytes(), Base64.CRLF);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return null;
            }

            // manual parsing
            ByteBuffer input = ByteBuffer.wrap(RawPrikey);
            if (der(input, 0x30) != input.remaining())
                throw new IllegalArgumentException("Excess data");
            if (!BigInteger.ZERO.equals(derint(input)))
                throw new IllegalArgumentException("Unsupported version");
            BigInteger Bn = derint(input);
            BigInteger Be = derint(input);
            BigInteger Bd = derint(input);
            BigInteger Bp = derint(input);
            BigInteger Bq = derint(input);
            BigInteger Bep = derint(input);
            BigInteger Beq = derint(input);
            BigInteger Bc = derint(input);
            RSAPrivateCrtKeySpec pvtspec = new RSAPrivateCrtKeySpec(Bn, Be, Bd, Bp, Bq, Bep, Beq,
                    Bc);

            KeyFactory kf = KeyFactory.getInstance("RSA");
            // PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(enc_pri);
            PrivateKey pk;
            try {
                pk = kf.generatePrivate(pvtspec);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return null;
            }
            try {
                cipher.init(Cipher.DECRYPT_MODE, pk);
            } catch (InvalidKeyException e) {
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return null;
            }
            CryptLog.i(DebugFlag, TAG, " loading private key ok.");

            /*
             * Decrypt the encrypted RSA data and print the result.
             */
            CryptLog.i(DebugFlag, TAG, " Decrypting the encrypted data");

            // copy from cipher input
            buf = ctext;
            try {
                result = cipher.doFinal(buf);
            } catch (IllegalBlockSizeException e) {
                Log.e(TAG, " failed! decrypting...");
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return null;
            } catch (BadPaddingException e) {
                Log.e(TAG, " failed! decrypting...");
                Log.e(TAG, String.format(" Reason = %s", e.getMessage()));
                e.printStackTrace();
                return null;
            }

            CryptLog.i(DebugFlag, TAG, String.format(" decipher length = %d", result.length));

            // cipher
            return result;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        }
        /*
         * catch (UnsupportedEncodingException e) { e.printStackTrace(); return
         * null; }
         */
    }

}
