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

import android.database.Cursor;
import android.text.TextUtils;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.crypto.CryptTools;

public class RecipientRow extends RecipientData {

    public RecipientRow(Cursor c) {
        if (c == null)
            return;

        mRowId = c.getLong(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_ROWID));
        mMyKeyId = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_MYKEYID));
        mExchDate = c.getLong(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_EXCHDATE));
        mContactId = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_CONTACTID));
        mContactLu = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_CONTACTLKUP));
        mRawContactId = c.getString(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_RAWCONTACTID));
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

}
