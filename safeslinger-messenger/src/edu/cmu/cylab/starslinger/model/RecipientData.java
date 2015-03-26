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

package edu.cmu.cylab.starslinger.model;

import android.text.TextUtils;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.crypto.CryptTools;

public class RecipientData {

    protected long mRowId = -1;
    protected String mMyKeyId;
    protected long mExchDate;
    protected String mContactLu;
    protected String mName;
    protected byte[] mPhoto;
    protected String mKeyId;
    protected long mKeyDate;
    protected String mKeyUserId;
    protected String mPushToken;
    protected int mNotify;
    protected byte[] mPubKey;
    protected int mSource;
    protected int mAppVer;
    protected long mHistDate;
    protected boolean mActive;
    protected String mIntroKeyId;
    protected long mNotRegDate;
    protected int mMyNotify;
    protected String mMyPushToken;

    @Deprecated
    protected String mContactId;
    @Deprecated
    protected String mRawContactId;

    public RecipientData() {
        super();
    }

    public boolean hasMyKeyChanged() {
        String myKeyId = SafeSlingerPrefs.getKeyIdString();
        if (myKeyId == null) {
            return false;
        } else if (mMyKeyId == null) {
            return myKeyId.equals(mMyKeyId);
        } else {
            return (myKeyId.compareTo(mMyKeyId) != 0);
        }
    }

    public boolean hasMyPushRegChanged() {
        String myPushReg = SafeSlingerPrefs.getPushRegistrationId();
        if (myPushReg == null) {
            return false;
        } else if (mMyPushToken == null) {
            return myPushReg.equals(mMyPushToken);
        } else {
            return (myPushReg.compareTo(mMyPushToken) != 0);
        }
    }

    public boolean isDeprecated() {
        if (mPubKey != null) {
            return CryptTools.is64BitKeyId(new String(mPubKey));
        } else {
            return true;
        }
    }

    public boolean isFromTrustedSource() {
        // - direct from exchange ACL good
        // - one hop trust ok
        // - pulled from address book, only older versions good
        return (mSource == RecipientDbAdapter.RECIP_SOURCE_EXCHANGE
                || mSource == RecipientDbAdapter.RECIP_SOURCE_INTRODUCTION || mSource == RecipientDbAdapter.RECIP_SOURCE_CONTACTSDB);
    }

    public boolean isPushable() {
        return mNotify != SafeSlingerConfig.NOTIFY_NOPUSH;
    }

    public boolean isRegistered() {
        // if we have a date from the service returning a registration failure
        // then we are no longer registered with the push service
        return (mNotRegDate <= 0);
    }

    public boolean isSendable() {
        return (mActive && isRegistered() && isPushable() && !isDeprecated()
                && isFromTrustedSource() && !hasMyKeyChanged());
    }

    public boolean isInvited() {
        return (mSource == RecipientDbAdapter.RECIP_SOURCE_INVITED);
    }

    public boolean isValidContactLink() {
        return (TextUtils.isEmpty(mContactId) && TextUtils.isEmpty(mRawContactId) && !TextUtils
                .isEmpty(mContactLu));
    }

    // getters...

    public long getRowId() {
        return mRowId;
    }

    public String getMykeyid() {
        return mMyKeyId;
    }

    public long getExchdate() {
        return mExchDate;
    }

    @Deprecated
    public String getContactid() {
        return mContactId;
    }

    public String getContactlu() {
        return mContactLu;
    }

    @Deprecated
    public String getRawContactid() {
        return mRawContactId;
    }

    public String getName() {
        return mName;
    }

    public byte[] getPhoto() {
        return mPhoto;
    }

    public String getKeyid() {
        return mKeyId;
    }

    public long getKeydate() {
        return mKeyDate;
    }

    public String getKeyuserid() {
        return mKeyUserId;
    }

    public String getPushtoken() {
        return mPushToken;
    }

    public int getNotify() {
        return mNotify;
    }

    public byte[] getPubkey() {
        return mPubKey;
    }

    public int getSource() {
        return mSource;
    }

    public int getAppver() {
        return mAppVer;
    }

    public long getHistdate() {
        return mHistDate;
    }

    public boolean isActive() {
        return mActive;
    }

    public String getIntroKeyid() {
        return mIntroKeyId;
    }

    public long getNotRegDate() {
        return mNotRegDate;
    }

    public String getMyPushtoken() {
        return mMyPushToken;
    }

    public int getMyNotify() {
        return mMyNotify;
    }

    // setters...

    public void setRowId(long value) {
        mRowId = value;
    }

    public void setMykeyid(String value) {
        mMyKeyId = value;
    }

    public void setExchdate(long value) {
        mExchDate = value;
    }

    @Deprecated
    public void setContactid(String value) {
        mContactId = value;
    }

    public void setContactlu(String value) {
        mContactLu = value;
    }

    @Deprecated
    public void setRawContactid(String value) {
        mRawContactId = value;
    }

    public void setName(String value) {
        mName = value;
    }

    public void setPhoto(byte[] value) {
        mPhoto = value;
    }

    public void setKeyid(String value) {
        mKeyId = value;
    }

    public void setKeydate(long value) {
        mKeyDate = value;
    }

    public void setKeyuserid(String value) {
        mKeyUserId = value;
    }

    public void setPushtoken(String value) {
        mPushToken = value;
    }

    public void setNotify(int value) {
        mNotify = value;
    }

    public void setPubkey(byte[] value) {
        mPubKey = value;
    }

    public void setSource(int value) {
        mSource = value;
    }

    public void setAppver(int value) {
        mAppVer = value;
    }

    public void setHistdate(long value) {
        mHistDate = value;
    }

    public void setActive(boolean value) {
        mActive = value;
    }

    public void setIntroKeyid(String value) {
        mIntroKeyId = value;
    }

    public void setNotRegDate(long value) {
        mNotRegDate = value;
    }

    public void setMyPushtoken(String value) {
        mMyPushToken = value;
    }

    public void setMyNotify(int value) {
        mMyNotify = value;
    }

}
