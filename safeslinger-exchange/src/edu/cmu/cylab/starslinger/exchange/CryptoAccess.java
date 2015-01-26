
package edu.cmu.cylab.starslinger.exchange;

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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPrivateKeySpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import fr.cryptohash.Digest;
import fr.cryptohash.Keccak256;

public class CryptoAccess {

    private static final String DATA_CIPHERALG = "AES";
    private static final String DATA_XFORM = "AES/CBC/PKCS7Padding";

    // 1.3: old 512-bit prime
    // public static final String DH_PRIME =
    // "B028E9B70DEE5D3C1EACA1E0FACE73ECC89B9B90B106062BDA9D622C58CB47D4FEFD539DE528B68B90B8A93E9142735AFAD0D2D8D67B32528306D0E66AF77BB3";

    // 1.8: new 1536-bit prime
    public static final String DH_PRIME = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF";

    public static final String DH_GEN = "02";

    public static final int HALFKEY_LEN = DH_PRIME.length() / 2;

    private static BigInteger sP = new BigInteger(CryptoAccess.DH_PRIME, 16);
    private static BigInteger sG = new BigInteger(CryptoAccess.DH_GEN, 16);
    private KeyAgreement mKA;

    public byte[] generateDHPublicKey() throws NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException {

        // generate key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        DHParameterSpec param = new DHParameterSpec(sP, sG);
        kpg.initialize(param);
        KeyPair kp = kpg.generateKeyPair();
        DHPrivateKey privateKey = (DHPrivateKey) kp.getPrivate();
        DHPublicKey publicKey = (DHPublicKey) kp.getPublic();

        // initialize key agreement with our private key
        mKA = KeyAgreement.getInstance("DH");
        mKA.init(privateKey);

        // return our public 1/2 key to share
        return getBytes(publicKey.getY());
    }

    public byte[] createNodeKey(byte[] pubKeyNode) throws NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException, IllegalStateException {

        // add this public key node to agreement
        KeyFactory keyFac = KeyFactory.getInstance("DH");
        BigInteger y = new BigInteger(1, pubKeyNode);
        DHPublicKeySpec spec = new DHPublicKeySpec(y, sP, sG);
        PublicKey nodePubKey = keyFac.generatePublic(spec);
        mKA.doPhase(nodePubKey, true);

        // complete this phase of agreement by generating secret
        BigInteger x = new BigInteger(1, mKA.generateSecret());
        BigInteger v = sG.modPow(x, sP);

        DHPrivateKeySpec specX = new DHPrivateKeySpec(x, sP, sG);
        PrivateKey nodePrivKey = keyFac.generatePrivate(specX);
        mKA.doPhase(nodePubKey, true);

        mKA = KeyAgreement.getInstance("DH");
        mKA.init(nodePrivKey);

        return getBytes(v);
    }

    public byte[] createFinalKey(byte[] pubKeyNode) throws InvalidKeySpecException,
            NoSuchAlgorithmException, InvalidKeyException, IllegalStateException {

        // add their public key to agreement
        KeyFactory keyFac = KeyFactory.getInstance("DH");
        BigInteger y = new BigInteger(1, pubKeyNode);
        DHPublicKeySpec spec = new DHPublicKeySpec(y, sP, sG);
        PublicKey nodePubKey = keyFac.generatePublic(spec);
        mKA.doPhase(nodePubKey, true);

        // complete agreement by generating secret
        return mKA.generateSecret();
    }

    public byte[] encryptNonce(byte[] plaintext, byte[] sharedSecretKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {

        byte[] key = hmacSha3("1".getBytes(), sharedSecretKey);
        byte[] iv = hmacSha3Trunc128("2".getBytes(), sharedSecretKey);
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher c = Cipher.getInstance(DATA_XFORM);
        SecretKeySpec keySpec = new SecretKeySpec(key, DATA_CIPHERALG);
        c.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = c.doFinal(plaintext);

        return encrypted;
    }

    public byte[] decryptNonce(byte[] ciphertext, byte[] sharedSecretKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {

        byte[] key = hmacSha3("1".getBytes(), sharedSecretKey);
        byte[] iv = hmacSha3Trunc128("2".getBytes(), sharedSecretKey);
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher c = Cipher.getInstance(DATA_XFORM);
        SecretKeySpec keySpec = new SecretKeySpec(key, DATA_CIPHERALG);
        c.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] raw = c.doFinal(ciphertext);

        return raw;
    }

    /**
     * The authenticity for this encryption is provided from the match nonce.
     * 
     * @param plaintext The plaintext user vCard data to be exchanged.
     * @param matchNonce The 256-bit match nonce derived from
     *            java.security.SecureRandom which serves as the key and the
     *            last item exchanged through the exchange.
     * @return The derived ciphertext.
     */
    public byte[] encryptData(byte[] plaintext, byte[] matchNonce) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, InvalidAlgorithmParameterException {

        // HMAC-SHA3 an enumerated key with the match nonce
        byte[] key = hmacSha3("1".getBytes(), matchNonce);
        byte[] iv = hmacSha3Trunc128("2".getBytes(), matchNonce);
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher c = Cipher.getInstance(DATA_XFORM);
        SecretKeySpec keySpec = new SecretKeySpec(key, DATA_CIPHERALG);
        c.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = c.doFinal(plaintext);

        return encrypted;
    }

    /**
     * The authenticity for this decryption is provided from the match nonce.
     * 
     * @param ciphertext The ciphertext user vCard data to be exchanged.
     * @param matchNonce The 256-bit match nonce derived from
     *            java.security.SecureRandom which serves as the key and the
     *            last item exchanged through the exchange.
     * @return The derived plaintext.
     */
    public byte[] decryptData(byte[] ciphertext, byte[] matchNonce)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        byte[] key = hmacSha3("1".getBytes(), matchNonce);
        byte[] iv = hmacSha3Trunc128("2".getBytes(), matchNonce);
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher c = Cipher.getInstance(DATA_XFORM);
        SecretKeySpec keySpec = new SecretKeySpec(key, DATA_CIPHERALG);
        c.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] raw = c.doFinal(ciphertext);

        return raw;
    }

    /**
     * Derives a unique hash based on an enumeration and the match nonce which
     * can be used for any 128-bit source.
     * 
     * @param key A short number which should be unique between key and IV.
     * @param message The 256-bit match nonce derived from
     *            java.security.SecureRandom which serves as the key and the
     *            last item exchanged through the exchange.
     * @return The HMAC-SHA1 digest truncated to 128-bits.
     */
    private static byte[] hmacSha3Trunc128(byte[] key, byte[] message) {
        // truncate nonce down to 128 bit key since 256 bit is not avail
        byte[] macTrunc = new byte[ExchangeConfig.AES_IV_LEN];
        ByteBuffer.wrap(computeSha3Hash2(key, message)).get(macTrunc, 0, ExchangeConfig.AES_IV_LEN);

        return macTrunc;
    }

    /**
     * Derives a unique hash based on an enumeration and the match nonce which
     * can be used for any 256-bit source.
     * 
     * @param key A short number which should be unique between key and IV.
     * @param message The 256-bit match nonce derived from
     *            java.security.SecureRandom which serves as the key and the
     *            last item exchanged through the exchange.
     * @return The HMAC-SHA3 digest.
     */
    private static byte[] hmacSha3(byte[] key, byte[] message) {

        return computeSha3Hash2(key, message);
    }

    public static byte[] getBytes(BigInteger big) {
        byte[] bigBytes = big.toByteArray();

        // use given array if sign bit does not extend the bit length
        if ((big.bitLength() % 8) != 0) {
            return bigBytes;
        }

        // strip off the sign bit when the sign bit does extend the bit length
        byte[] smallerBytes = new byte[big.bitLength() / 8];
        System.arraycopy(bigBytes, 1, smallerBytes, 0, smallerBytes.length);
        return smallerBytes;
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

    public static byte[] computeSha3Hash3(byte[] buffer1, byte[] buffer2, byte[] buffer3) {
        ByteBuffer join = ByteBuffer.allocate(buffer1.length + buffer2.length + buffer3.length);
        join.put(buffer1);
        join.put(buffer2);
        join.put(buffer3);
        return computeSha3Hash(join.array());
    }
}
