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

import android.content.Context;
import android.database.Cursor;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.crypto.CryptTools;

public class RecipientRow {

    private long mRowId = -1;
    private String mMyKeyId;
    private long mExchDate;
    private String mContactLu;
    private String mRawId;
    private String mName;
    private byte[] mPhoto;
    private String mKeyId;
    private long mKeyDate;
    private String mKeyUserId;
    private String mPushToken;
    private int mNotify;
    private byte[] mPubKey;
    private int mSource;
    private int mAppVer;
    private long mHistDate;
    private boolean mActive;
    private String mIntroKeyId;
    private long mNotRegDate;
    private int mMyNotify;
    private String mMyPushToken;

    @Deprecated
    private String mContactId;

    public RecipientRow(Cursor c) {
        if (c == null)
            return;

        mRowId = c.getLong(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_ROWID));
        mMyKeyId = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_MYKEYID));
        mExchDate = c.getLong(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_EXCHDATE));
        mContactId = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_CONTACTID));
        mContactLu = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_CONTACTLKUP));
        mRawId = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_RAWCONTACTID));
        mName = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_NAME));
        mPhoto = c.getBlob(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_PHOTO));
        mKeyId = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_KEYID));
        mKeyDate = c.getLong(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_KEYDATE));
        mKeyUserId = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_KEYUSERID));
        mPushToken = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_PUSHTOKEN));
        mNotify = c.getInt(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_NOTIFY));
        mPubKey = c.getBlob(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_PUBKEY));
        mSource = c.getInt(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_SOURCE));
        mAppVer = c.getInt(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_APPVERSION));
        mHistDate = c.getLong(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_HISTDATE));
        mActive = c.getInt(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_ACTIVE)) == RecipientDbAdapter.RECIP_IS_ACTIVE ? true
                : false;
        mIntroKeyId = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_INTROKEYID));
        mNotRegDate = c.getLong(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_NOTREGDATE));
        mMyNotify = c.getInt(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_MYNOTIFY));
        mMyPushToken = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_MYPUSHTOKEN));
    }

    private RecipientRow(String keyid) {
        // place holder private null object creation
        mKeyId = keyid;
    }

    public static RecipientRow createEmptyRecipient() {
        return new RecipientRow((String) null);
    }

    public static RecipientRow createKeyIdOnlyRecipient(String keyid) {
        return new RecipientRow(keyid);
    }

    /***
     * Test purposes only
     */
    public RecipientRow(String name, byte[] photo, String keyId, long keyDate, int notify) {
        mName = name;
        mPhoto = photo;
        mKeyId = keyId;
        mKeyDate = keyDate;
        mNotify = notify;
    }

    public boolean hasMyKeyChanged(Context ctx) {
        String myKeyId = ConfigData.loadPrefKeyIdString(ctx);
        return (myKeyId == null ? false : !(myKeyId.compareTo(mMyKeyId) == 0));
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
        // - pulled from address book, only older versions good
        return (mSource == RecipientDbAdapter.RECIP_SOURCE_EXCHANGE
                || mSource == RecipientDbAdapter.RECIP_SOURCE_INTRODUCTION || mSource == RecipientDbAdapter.RECIP_SOURCE_CONTACTSDB);
    }

    public boolean isPushable() {
        return mNotify != ConfigData.NOTIFY_NOPUSH;
    }

    public boolean isRegistered() {
        // if we have a date from the service returning a registration failure
        // then we are no longer registered with the push service
        return (mNotRegDate <= 0);
    }

    public boolean isSendable(Context ctx) {
        return (mActive && isRegistered() && isPushable() && !isDeprecated()
                && isFromTrustedSource() && !hasMyKeyChanged(ctx));
    }

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

    public String getRawid() {
        return mRawId;
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

}
