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

import java.io.Serializable;

import edu.cmu.cylab.starslinger.crypto.CryptoMsgException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgNonExistingKeyException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider;

/***
 * This class is designed to store the data from the Wrapper class in an
 * unencrypted form. All storage of this object should first serialize then
 * encrypt, to write; and deserialize, then decrypt, to read. WARNING: do not
 * change package or class or any variable names of this class, it will break
 * it's serialization.
 */
public class CryptoMsgPrivateData implements Serializable {
    private static final long serialVersionUID = 5295833936452554078L;

    private String signpubkey = null;
    private String encpubkey = null;
    private String signprikey = null;
    private String encprikey = null;
    private String keyid = null;
    private long gendate = 0;
    private String safeslingerstring = null;

    public CryptoMsgPrivateData(CryptoMsgProvider p) throws CryptoMsgException,
            CryptoMsgNonExistingKeyException {
        if (p.isGenerated()) {
            this.signpubkey = p.getPublicKeyForSign();
            this.encpubkey = p.getPublicKeyForEnc();
            this.signprikey = p.getPrivateKeyForSign();
            this.encprikey = p.getPrivateKeyForEnc();
            this.keyid = p.GetSelfKeyid();
            this.gendate = p.GetSelfKeyGenDate();
            this.safeslingerstring = p.getSafeSlingerString();
        } else {
            throw new CryptoMsgException("Can only create key object at generation time.");
        }
    }

    public CryptoMsgPrivateData(String signpubkey, String encpubkey, String signprikey,
            String encprikey, String keyid, long gendate, String safeslingerstring) {
        this.signpubkey = signpubkey;
        this.encpubkey = encpubkey;
        this.signprikey = signprikey;
        this.encprikey = encprikey;
        this.keyid = keyid;
        this.gendate = gendate;
        this.safeslingerstring = safeslingerstring;
    }

    public void setSignPubKey(String signPubKey) {
        this.signpubkey = signPubKey;
    }

    public String getSignPubKey() {
        return signpubkey;
    }

    public void setEncPubKey(String encPubKey) {
        this.encpubkey = encPubKey;
    }

    public String getEncPubKey() {
        return encpubkey;
    }

    public void setSignPriKey(String signPriKey) {
        this.signprikey = signPriKey;
    }

    public String getSignPriKey() {
        return signprikey;
    }

    public void setEncprikey(String encPriKey) {
        this.encprikey = encPriKey;
    }

    public String getEncPriKey() {
        return encprikey;
    }

    public void setKeyId(String keyId) {
        this.keyid = keyId;
    }

    public String getKeyId() {
        return keyid;
    }

    public void setFormatDate(long genDate) {
        this.gendate = genDate;
    }

    public long getGenDate() {
        return gendate;
    }

    public void setSafeSlingerString(String safeSlingerString) {
        this.safeslingerstring = safeSlingerString;
    }

    public String getSafeSlingerString() {
        return safeslingerstring;
    }

}
