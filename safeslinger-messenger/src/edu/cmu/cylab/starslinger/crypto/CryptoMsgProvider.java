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

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class offers several cryptographic operations leveraged in SafeSlinger
 * Android Software. Partial operations use CryptoMsg C library.</br>
 * <p>
 * How to use CryptoMsgProvider class:</br> First, we have to allocate this
 * object by using calling its constructor. </br></br> CryptoMsgProvider p = new
 * CryptoMsgProvider(); </br></br> Then call GenKeyPairs() method to generate
 * two RSA key pairs for encryption</br> and signing operations. You can also
 * use InGenerated() to check whether the</br> CryptoMsgProvider object has been
 * generated keys yet or not.Once the key pairs</br> are ready, we are ready to
 * call getPrivateKeyForEnc(),getPrivateKeyForSign(),</br> getPublicKeyForEnc(),
 * and getPublicKeyForSign() to access each key.</br> For SafeSlinger key
 * exchange, we call getSafeSlingerString() to get a IMPP</br> string like:
 * </br></br> {Base64 encoded key string}. </br></br> For other properties, such
 * as key generation date-time and unique key ID, call</br> GetSelfKeyGenDate()
 * and GetSelfKeyid() to get related information (return String).</br>
 * </p>
 * <p>
 * When Android program receives SafeSlinger key string encapsulated in
 * exchanged</br> vCards, two methods ExtractKeyIDfromKeyString and
 * ExtractDateTimefromKeyString</br> are used to< extract its key id string and
 * generated date-time string.</br>
 * </p>
 * <p>
 * For raw data security (here we use byte[] as input), for sender side, we
 * call</br> PackCipher to pack a secure packet for transmission. While the
 * receipt obtained</br> this packet from Internet, he call
 * ExtractKeyIDfromPacket to get the KeyID inside</br> this packet. And then it
 * could find related SafeSlinger key string stored in the</br> database.
 * Finally, we can run UnPackCipher to verify and decrypt the original byte</br>
 * array data.</br>
 * </p>
 * 
 * @version 0.1
 * @since June 24, 2012
 * @author Yue-Hsun Lin
 */
public abstract class CryptoMsgProvider {

    // key store
    private String mSignPubKey = null;
    private String mEncPubKey = null;
    private String mSignPriKey = null;
    private String mEncPriKey = null;
    private String mKeyId = null;
    private String mFormatDate = null;

    // parameter, lengths
    final private int ENCKEYSIZE = 2048;
    final private int SIGNKEYSIZE = 1024;

    private boolean mHasKeyGen = false;
    private boolean mLog = false; // is loggable
    private static final String DATE_FORMAT_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String TAG = CryptoMsgProvider.class.getSimpleName();

    /***
     * Create default instance of crypto provider = JavaXCryptoWrapper.
     */
    public static CryptoMsgProvider createInstance(boolean loggable) {
        return createInstance(JavaXCryptoWrapper.class.getName(), loggable);
    }

    /***
     * Create named instance of crypto provider.
     */
    public static CryptoMsgProvider createInstance(String fullClass, boolean loggable) {
        CryptoMsgProvider sInstance = null;
        try {
            Class<? extends CryptoMsgProvider> clazz;
            clazz = Class.forName(fullClass).asSubclass(CryptoMsgProvider.class);
            sInstance = clazz.newInstance();
            sInstance.mLog = loggable;
            sInstance.DebugFlag(loggable);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        }

        return sInstance;
    }

    /**
     * Check the existing key pairs is generated or not.
     * <p>
     * This function should be executed before extracting key pairs, e.g.,
     * getPublicKeyForEnc, getPrivateKeyForEnc, or related information
     * (GetSelfKeyGenDate).
     * </p>
     * 
     * @see edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider#GenKeyPairs
     * @return true if key pairs are already generated; otherwise, return false.
     */
    public boolean isGenerated() {
        return mHasKeyGen;
    }

    /**
     * Generate two sets of key pairs for public key encrypt/decrypt and
     * sign/verify cryptographic operations.
     * <p>
     * This function generates two set of RSA keys for further use.<br>
     * It might take a while to finish it since key generation is quite slow,
     * around few minutes.<br>
     * </p>
     * 
     * @return true indicates success in keygen process, and false indicates
     *         failure in keygen process.
     */
    public boolean GenKeyPairs() {
        CryptLog.i(mLog, TAG, "CryptoMsgProvider Key Generator Function..");
        if (!this.GenEncKeyPair()) {
            CryptLog.e(mLog, TAG, "Key Generation Fail at RSA ENC/DEC");
            return false;
        }
        if (!this.GenSignKeyPair()) {
            CryptLog.e(mLog, TAG, "Key Generation Fail at RSA SIGN/VERF");
            return false;
        }
        // gen key success
        mSignPubKey = this.GetSignPKey();
        mEncPubKey = this.GetEncPKey();
        mSignPriKey = this.GetSignRKey();
        mEncPriKey = this.GetEncRKey();

        // gen keyid
        String tmpkey = new String(mEncPubKey);
        // String concatenate
        tmpkey = tmpkey + " " + new String(mSignPubKey);
        CryptLog.i(mLog, TAG, "key input = " + tmpkey);
        byte[] keyid = this.GetKeyID(tmpkey.getBytes());
        mKeyId = new String(keyid);
        CryptLog.i(mLog, TAG, "keyid = " + mKeyId);

        // TimeStamp
        mFormatDate = gmtMillis2Iso8601Zulu(new Date().getTime());
        CryptLog.i(mLog, TAG, "KeyGen Date : " + mFormatDate);

        mHasKeyGen = true;
        return (mHasKeyGen);
    }

    /**
     * Get formatted string of key generation time.
     * <p>
     * Return the date time formatted string while generating key pairs.
     * </p>
     * 
     * @return A formatted date-time string, format is {yyyy-MM-ddTHH:mm:ssZ}.
     */
    public long GetSelfKeyGenDate() {
        if (mHasKeyGen) {
            return iso8601Zulu2GmtMillis(mFormatDate);
        } else {
            CryptLog.e(mLog, TAG, "Return empty key, GenKey First!");
            return 0;
        }
    }

    /**
     * Get Key ID string of generated key pairs.
     * <p>
     * First we concatenate all key data and compute SHA4 hash for it.<br>
     * Then the hash would be encode as Base64 encoding and return as its key ID
     * string.<br>
     * </p>
     * 
     * @return Unique Key ID Base64 Encoding String (length: 88 bytes).
     * @throws Throw an exception while the key generation does not finish.
     */
    public String GetSelfKeyid() throws CryptoMsgNonExistingKeyException {
        if (mHasKeyGen) {
            return mKeyId;
        } else {
            CryptLog.e(mLog, TAG, "Return empty key, GenKey First!");
            throw new CryptoMsgNonExistingKeyException();
        }
    }

    /**
     * Get the Private Key String (base64 encoding) for Signing/Verifying
     * operation.
     * 
     * @return Encoded Private Key String.
     * @throws Throw an exception while the key generation does not finish.
     */
    public String getPrivateKeyForSign() throws CryptoMsgNonExistingKeyException {
        if (mHasKeyGen) {
            return mSignPriKey;
        } else {
            CryptLog.e(mLog, TAG, "Return empty key, GenKey First!");
            throw new CryptoMsgNonExistingKeyException();
        }
    }

    /**
     * Get the Private Key String (base64 encoding) for Encryption/Decryption
     * operation.
     * 
     * @return Encoded Private Key String.
     * @throws Throw an exception while the key generation does not finish.
     */
    public String getPrivateKeyForEnc() throws CryptoMsgNonExistingKeyException {
        if (mHasKeyGen) {
            return mEncPriKey;
        } else {
            CryptLog.e(mLog, TAG, "Return empty key, GenKey First!");
            throw new CryptoMsgNonExistingKeyException();
        }
    }

    /**
     * Get the Public Key String (base64 encoding) for Signing/Verifying
     * operation.
     * 
     * @return Encoded Public Key String.
     * @throws Throw an exception while the key generation does not finish.
     */
    public String getPublicKeyForSign() throws CryptoMsgNonExistingKeyException {
        if (mHasKeyGen) {
            return mSignPubKey;
        } else {
            CryptLog.e(mLog, TAG, "Return empty key, GenKey First!");
            throw new CryptoMsgNonExistingKeyException();
        }
    }

    /**
     * Get the Public Key String (base64 encoding) for Encryption/Decryption
     * operation.
     * 
     * @return Encoded Public Key String.
     * @throws Throw an exception while the key generation does not finish.
     */
    public String getPublicKeyForEnc() throws CryptoMsgNonExistingKeyException {
        if (mHasKeyGen) {
            return mEncPubKey;
        } else {
            CryptLog.e(mLog, TAG, "Return empty key, GenKey First!");
            throw new CryptoMsgNonExistingKeyException();
        }
    }

    /**
     * Get SafeSlinger Key String for SafeSlinger Key Exchange. The string is
     * encode as Base64Encde {KeyID "\n" DateTime "\n" PublicKeyForEnc " "
     * PublicKeyForSign }
     * 
     * @return Encoded SafeSlinger String.
     * @throws Throw an exception while the key generation does not finish.
     */
    public String getSafeSlingerString() throws CryptoMsgNonExistingKeyException {
        StringBuffer sb = new StringBuffer();

        if (mHasKeyGen) {
            // field 1: keyid, SHA-512 hash
            CryptLog.i(mLog, TAG, "KeyID size = " + mKeyId.length());
            sb.append(mKeyId);
            sb.append('\n');

            // field 2: get date string
            sb.append(mFormatDate);
            sb.append('\n');

            // field 3: public keys
            CryptLog.i(mLog, TAG, "encpubkey size = " + mEncPubKey.length());
            sb.append(mEncPubKey);
            sb.append(' ');
            CryptLog.i(mLog, TAG, "signpubkey size = " + mSignPubKey.length());
            sb.append(mSignPubKey);

            // encode as base64 encoding
            byte[] encode = this.encodeKey(sb.toString().getBytes());
            sb.setLength(0);
            // sb.append("IMPP;SafeSlinger-PubKey:");
            sb.append(new String(encode));

            return sb.toString();
        } else {
            CryptLog.e(mLog, TAG, "Return empty key, GenKey First!");
            throw new CryptoMsgNonExistingKeyException();
        }
    }

    /**
     * Extract the Key ID (encoded as Base64 string) from an incoming packet.
     * 
     * @param packet The incoming packet byte array
     * @return Key ID String (length: 88 bytes)
     * @throws Throw an exception while the packet does not have the correct
     *             length.
     */
    public String ExtractKeyIDfromPacket(byte[] packet) throws CryptoMsgPacketSizeException {
        byte[] keyid = new byte[88];
        if (packet.length < 552) {
            throw new CryptoMsgPacketSizeException();
        }
        System.arraycopy(packet, 0, keyid, 0, 88);
        return new String(keyid);
    }

    /**
     * Extract the Key ID (encoded as Base64 string) from the exchanged
     * SafeSlinger String.
     * 
     * @param keystr The peer's SafeSlinger String
     * @return Key ID String (length: 88 bytes)
     * @throws Throw an exception while the string does not have the correct
     *             format.
     */
    public String ExtractKeyIDfromSafeSlingerString(String keystr)
            throws CryptoMsgPeerKeyFormatException {
        // decode for the remind string
        byte[] decode = this.decodeKey(keystr.getBytes());
        String[] elems = new String(decode).split("\n");

        if (elems.length != 3) {
            CryptLog.e(mLog, TAG, "Invalid Peer Key Format!");
            throw new CryptoMsgPeerKeyFormatException();
        }
        CryptLog.i(mLog, TAG, "Keyid: " + elems[0]);
        return elems[0];
    }

    /**
     * Extract the Date-Time formatted string (encoded as Base64 string) from
     * the exchanged SafeSlinger String.
     * 
     * @param keystr The peer's SafeSlinger String
     * @return Date-Time Formatted String {yyyy-MM-ddTHH:mm:ssZ}
     * @throws Throw an exception while the string does not have the correct
     *             format.
     */
    public long ExtractDateTimefromSafeSlingerString(String keystr)
            throws CryptoMsgPeerKeyFormatException {
        // decode for the remind string
        byte[] decode = this.decodeKey(keystr.getBytes());
        String[] elems = new String(decode).split("\n");

        if (elems.length != 3) {
            CryptLog.e(mLog, TAG, "Invalid Peer Key Format!");
            throw new CryptoMsgPeerKeyFormatException();
        }
        CryptLog.i(mLog, TAG, "DateTime: " + elems[1]);
        return iso8601Zulu2GmtMillis(elems[1]);
    }

    /**
     * Pack an secure packet from a given byte array input.
     * <p>
     * It takes the receipt's SafeSlinger key string (for data encryption) , the
     * sender's private key string (for signing data) and his key id to pack a
     * secure message
     * </p>
     * 
     * @param input The input data array
     * @param peerKey The receipt's SafeSlinger String
     * @param SignRKey The sender's private key string for signing (call
     *            getPrivateKeyForSign)
     * @param keyid The sender's key id (call GetSelfKeyid)
     * @return A byte array as cipher data
     * @throws Exceptions while the peer key is not well-formatted or
     *             cryptographic operations failed.
     * @see edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider#getSafeSlingerString
     * @see edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider#getPrivateKeyForSign
     * @see edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider#GetSelfKeyid
     */
    public byte[] PackCipher(byte[] input, String peerKey, String SignRKey, byte[] keyid)
            throws CryptoMsgPeerKeyFormatException {
        String EncPKey = null;

        // decode for the remind string
        EncPKey = peerKey;
        byte[] decode = this.decodeKey(EncPKey.getBytes());
        String[] elems = new String(decode).split("\n");

        if (elems.length != 3) {
            CryptLog.e(mLog, TAG, "Invalid Peer Key Format!");
            throw new CryptoMsgPeerKeyFormatException();
        }

        elems = elems[2].split(" ");
        if (elems.length != 2) {
            CryptLog.e(mLog, TAG, "Invalid Peer Key Format!");
            throw new CryptoMsgPeerKeyFormatException();
        }
        CryptLog.i(mLog, TAG, "EncPKey: " + elems[0]);
        EncPKey = elems[0];

        // prepare symmetric cipher first
        byte[] skey = this.GetRandomKey();
        byte[] mcipher = this.AESEncrypt(input, skey);
        int plen = input.length;
        CryptLog.i(mLog, TAG, "plen: " + plen);

        // perform Sign/Encrypt/Sign model on symmetric key
        // getSelfPrivateKeyPath; YES for Encrypt, No for Sign

        // to solve IV problem, it should sign skey||plen
        ByteBuffer s = ByteBuffer.allocate(skey.length + 4);
        s.put(skey);
        s.putInt(plen);

        byte[] sig = this.Sign(SignRKey, s.array());

        ByteBuffer b = ByteBuffer.allocate(skey.length + sig.length + 4);
        b.clear();
        b.put(skey);
        b.putInt(plen);
        b.put(sig);
        CryptLog.i(mLog, TAG, "skey+plen+sig = " + b.array() + ", size = " + b.array().length);
        byte[] cipher = this.Encrypt(EncPKey, b.array());
        if (cipher == null) {
            throw new CryptoMsgPeerKeyFormatException();
        }

        CryptLog.i(mLog, TAG, "S/E len = " + cipher.length);

        // re-allocate bytebuffer
        b = ByteBuffer.allocate(cipher.length + 20);
        // set to 0
        b.clear();
        b.put(cipher);

        // compute public key hash
        byte[] hmac = this.KeyHMAC(EncPKey.getBytes());
        b.put(hmac);

        // sign again
        sig = this.Sign(SignRKey, b.array());

        // construct the packet
        b = ByteBuffer.allocate(keyid.length + cipher.length + sig.length + mcipher.length);
        // set to 0
        b.clear();

        // pack keyid, SHA-512 hash
        b.put(keyid);
        b.put(cipher);
        b.put(sig);
        b.put(mcipher);

        CryptLog.i(mLog, TAG, "Final Packet Size = " + b.position());
        return b.array();
    }

    /**
     * Unpack plain data array from a secure packet.
     * <p>
     * It takes the sender's SafeSlinger key string (for data verifying) , the
     * sender's public/private key string (for verifying and decryption) to
     * unpack the received secure packet.
     * </p>
     * 
     * @param pkt The received secure packet data
     * @param peerKey The sender's SafeSlinger String
     * @param PubPKey The receiver's (self) private key string for signing (call
     *            getPrivateKeyForSign)
     * @param PubRKey The receiver's (self) private key string for signing (call
     *            getPrivateKeyForSign)
     * @return A byte array as plaintext data
     * @throws Exceptions while the peer key is not well-formatted or
     *             cryptographic operations failed.
     * @see edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider#getSafeSlingerString
     * @see edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider#getPublicKeyForEnc
     * @see edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider#getPrivateKeyForEnc
     * @see edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider#ExtractKeyIDfromPacket
     */
    public byte[] UnPackCipher(byte[] pkt, String peerKey, String PubPKey, String PubRKey)
            throws CryptoMsgPeerKeyFormatException, CryptoMsgPacketSizeException,
            CryptoMsgSignatureVerificationException {
        int offset = 0;
        // for verify
        String VerifyPKey = null;

        // decode for the remind string
        VerifyPKey = peerKey;
        byte[] decode = this.decodeKey(VerifyPKey.getBytes());
        String[] elems = new String(decode).split("\n");

        if (elems.length != 3) {
            CryptLog.e(mLog, TAG, "Invalid Peer Key Format!");
            throw new CryptoMsgPeerKeyFormatException();
        }

        elems = elems[2].split(" ");
        if (elems.length != 2) {
            CryptLog.e(mLog, TAG, "Invalid Peer Key Format!");
            throw new CryptoMsgPeerKeyFormatException();
        }
        CryptLog.i(mLog, TAG, "SignPKey: " + elems[1]);
        VerifyPKey = elems[1];

        // empty byte array for store pkt segements
        byte[] skey_and_Plen = new byte[68];
        byte[] skey = new byte[64];
        byte[] cipher = new byte[ENCKEYSIZE / 8];
        byte[] sig = new byte[SIGNKEYSIZE / 8];
        byte[] hash = new byte[20];
        int mclen;

        if (pkt.length < 552)
            throw new CryptoMsgPacketSizeException();

        // public key cipher length
        mclen = pkt.length - (ENCKEYSIZE / 8) - (SIGNKEYSIZE / 8) - 88;
        CryptLog.i(mLog, TAG, "Symmetric Cipher length = " + mclen);

        // first we have read the packet each by each
        // 1. public key cipher, 256 bytes
        offset = 88;
        System.arraycopy(pkt, offset, cipher, 0, ENCKEYSIZE / 8);
        offset = offset + ENCKEYSIZE / 8;

        // 2. signature, 128 bytes
        System.arraycopy(pkt, offset, sig, 0, SIGNKEYSIZE / 8);
        offset = offset + SIGNKEYSIZE / 8;

        // verify signature first
        ByteBuffer b = ByteBuffer.allocate(ENCKEYSIZE / 8 + 20);
        // compute public key hash
        hash = this.KeyHMAC(PubPKey.getBytes());
        b.clear();
        b.put(cipher);
        b.put(hash);

        if (!this.Verify(VerifyPKey, b.array(), sig)) {
            CryptLog.e(mLog, TAG, "Invalid Signature.");
            throw new CryptoMsgSignatureVerificationException();
        }

        byte[] decipher = this.Decrypt(PubRKey, cipher);
        if (decipher == null) {
            throw new CryptoMsgPeerKeyFormatException();
        }

        CryptLog.i(mLog, TAG, "deCipher length = " + decipher.length);
        System.arraycopy(decipher, 0, skey_and_Plen, 0, 68);
        System.arraycopy(decipher, 0, skey, 0, 64);
        System.arraycopy(decipher, 68, sig, 0, SIGNKEYSIZE / 8);

        if (!this.Verify(VerifyPKey, skey_and_Plen, sig)) {
            CryptLog.e(mLog, TAG, "Invalid Signature.");
            throw new CryptoMsgSignatureVerificationException();
        }

        int plenInt = ByteBuffer.wrap(skey_and_Plen, 64, 4).getInt();
        CryptLog.i(mLog, TAG, "plen: " + plenInt);

        // 3. remaindering, block cipher
        byte[] mcipher = new byte[mclen];
        System.arraycopy(pkt, offset, mcipher, 0, mclen);
        // use the first 64 as AES key
        byte[] message = this.AESDecrypt(mcipher, skey, plenInt);

        return message;
    }

    private static String gmtMillis2Iso8601Zulu(long gmtMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_ISO8601, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date(gmtMillis));
    }

    private static long iso8601Zulu2GmtMillis(String iso8601Date) {
        DateFormat format = new SimpleDateFormat(DATE_FORMAT_ISO8601, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            return format.parse(iso8601Date).getTime();
        } catch (ParseException e) {
            return Date.parse(iso8601Date);
        }
    }

    public void CryptoUnitTest(CryptoMsgProvider SecInstance) {

        String text = "This is a short message.";
        this.GenKeyPairs();
        SecInstance.GenKeyPairs();

        String EncPK1 = this.GetEncPKey();
        String EncRK1 = this.GetEncRKey();
        String SignPK1 = this.GetSignPKey();
        String SignRK1 = this.GetSignRKey();

        String EncPK2 = SecInstance.GetEncPKey();
        String EncRK2 = SecInstance.GetEncRKey();
        String SignPK2 = SecInstance.GetSignPKey();
        String SignRK2 = SecInstance.GetSignRKey();

        CryptLog.i(mLog, TAG, "EncPK1=\n" + EncPK1);
        CryptLog.i(mLog, TAG, "EncRK1=\n" + EncRK1);
        CryptLog.i(mLog, TAG, "SignPK1=\n" + SignPK1);
        CryptLog.i(mLog, TAG, "SignRK1=\n" + SignRK1);

        CryptLog.i(mLog, TAG, "EncPK2=\n" + EncPK2);
        CryptLog.i(mLog, TAG, "EncRK2=\n" + EncRK2);
        CryptLog.i(mLog, TAG, "SignPK2=\n" + SignPK2);
        CryptLog.i(mLog, TAG, "SignRK2=\n" + SignRK2);

        CryptLog.i(mLog, TAG, "Sign Test..");
        byte[] sig = this.Sign(SignRK1, text.getBytes());
        this.Verify(SignPK1, text.getBytes(), sig);

        sig = this.Sign(SignRK2, text.getBytes());
        this.Verify(SignPK2, text.getBytes(), sig);

        sig = SecInstance.Sign(SignRK2, text.getBytes());
        SecInstance.Verify(SignPK2, text.getBytes(), sig);

        sig = SecInstance.Sign(SignRK1, text.getBytes());
        SecInstance.Verify(SignPK1, text.getBytes(), sig);

        sig = SecInstance.Sign(SignRK1, text.getBytes());
        this.Verify(SignPK1, text.getBytes(), sig);

        sig = this.Sign(SignRK1, text.getBytes());
        SecInstance.Verify(SignPK1, text.getBytes(), sig);

        sig = SecInstance.Sign(SignRK2, text.getBytes());
        this.Verify(SignPK2, text.getBytes(), sig);

        sig = this.Sign(SignRK2, text.getBytes());
        SecInstance.Verify(SignPK2, text.getBytes(), sig);

        CryptLog.i(mLog, TAG, "Encrypt Test ..");

        byte[] cipher = this.Encrypt(EncPK1, text.getBytes());
        byte[] decipher = this.Decrypt(EncRK1, cipher);
        CryptLog.i(mLog, TAG, "Decipher Result = " + new String(decipher));

        cipher = SecInstance.Encrypt(EncPK2, text.getBytes());
        decipher = SecInstance.Decrypt(EncRK2, cipher);
        CryptLog.i(mLog, TAG, "Decipher Result = " + new String(decipher));

        cipher = SecInstance.Encrypt(EncPK1, text.getBytes());
        decipher = SecInstance.Decrypt(EncRK1, cipher);
        CryptLog.i(mLog, TAG, "Decipher Result = " + new String(decipher));

        cipher = this.Encrypt(EncPK2, text.getBytes());
        decipher = this.Decrypt(EncRK2, cipher);
        CryptLog.i(mLog, TAG, "Decipher Result = " + new String(decipher));

        cipher = SecInstance.Encrypt(EncPK2, text.getBytes());
        decipher = this.Decrypt(EncRK2, cipher);
        CryptLog.i(mLog, TAG, "Decipher Result = " + new String(decipher));

        cipher = this.Encrypt(EncPK1, text.getBytes());
        decipher = SecInstance.Decrypt(EncRK1, cipher);
        CryptLog.i(mLog, TAG, "Decipher Result = " + new String(decipher));

        byte[] aeskey = this.GetRandomKey();
        text = "This is a long message. This is a long message. This is a long message. This is a long message. This is a long message.";
        cipher = this.AESEncrypt(text.getBytes(), aeskey);
        decipher = SecInstance.AESDecrypt(cipher, aeskey, text.length());
        CryptLog.i(mLog, TAG, "AES Decipher Result = " + new String(decipher) + ", legnth = "
                + decipher.length);

        cipher = SecInstance.AESEncrypt(text.getBytes(), aeskey);
        decipher = this.AESDecrypt(cipher, aeskey, text.length());
        CryptLog.i(mLog, TAG, "AES Decipher Result = " + new String(decipher) + ", legnth = "
                + decipher.length);

        byte[] fp1 = this.KeyHMAC(EncPK2.getBytes());
        byte[] fp2 = SecInstance.KeyHMAC(EncPK2.getBytes());
        if (Arrays.equals(fp1, fp2))
            CryptLog.i(mLog, TAG, "KeyHMAC equals.");
        else
            CryptLog.e(mLog, TAG, "KeyHMAC errors.");

        String pubkey = EncPK2 + " " + SignPK2;
        fp1 = this.GetKeyID(pubkey.getBytes());
        fp2 = SecInstance.GetKeyID(pubkey.getBytes());
        if (Arrays.equals(fp1, fp2))
            CryptLog.i(mLog, TAG, "GetKeyID equals.");
        else
            CryptLog.e(mLog, TAG, "GetKeyID errors.");
    }

    protected abstract boolean GenSignKeyPair();

    protected abstract boolean GenEncKeyPair();

    protected abstract byte[] GetKeyID(byte[] combined_key);

    protected abstract byte[] encodeKey(byte[] plain);

    protected abstract byte[] decodeKey(byte[] encoded);

    protected abstract String GetSignPKey();

    protected abstract String GetSignRKey();

    protected abstract String GetEncPKey();

    protected abstract String GetEncRKey();

    public abstract byte[] Sign(String prikey, byte[] text);

    protected abstract boolean Verify(String pubkey, byte[] text, byte[] signature);

    protected abstract byte[] Encrypt(String pubkey, byte[] text);

    protected abstract byte[] Decrypt(String prikey, byte[] cipher);

    protected abstract byte[] GetRandomKey();

    protected abstract byte[] AESEncrypt(byte[] text, byte[] key);

    protected abstract byte[] AESDecrypt(byte[] cipher, byte[] key, int plen);

    protected abstract byte[] KeyHMAC(byte[] input);

    protected abstract void DebugFlag(boolean OnOff);
}
