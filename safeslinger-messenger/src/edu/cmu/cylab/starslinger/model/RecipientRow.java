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

import android.database.Cursor;

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

}
