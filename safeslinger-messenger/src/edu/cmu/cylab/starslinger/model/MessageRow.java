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

import java.io.File;

import android.database.Cursor;
import android.text.TextUtils;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class MessageRow extends MessageData {

    public static enum MsgAction {
        DISPLAY_ONLY, // default
        MSG_EDIT, //
        MSG_PROGRESS, //
        MSG_EXPIRED, //
        MSG_DOWNLOAD, //
        MSG_DECRYPT, //
        FILE_DOWNLOAD_DECRYPT, //
        FILE_OPEN, //
    }

    public MessageRow(Cursor c, boolean isInboxTable) {
        mIsInboxTable = isInboxTable;
        if (c == null)
            return;

        mRowId = c.getLong(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_ROWID));
        mMsgHash = c.getString(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_MSGHASH));
        mFileSize = c.getInt(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_FILELEN));
        mFileType = c.getString(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_FILETYPE));
        mFileName = c.getString(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_FILENAME));
        mText = c.getString(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_TEXT));
        mPerson = c.getString(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_PERSON));
        mInbox = c.getInt(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_TYPE)) == MessageDbAdapter.MESSAGE_TYPE_SENT ? false
                : true;
        mSeen = c.getInt(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_SEEN)) == MessageDbAdapter.MESSAGE_IS_SEEN ? true
                : false;
        mRead = c.getInt(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_READ)) == MessageDbAdapter.MESSAGE_IS_READ ? true
                : false;
        mDateRecv = c.getLong(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_DATE_RECV));
        mDateSent = c.getLong(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_DATE_SENT));
        mStatus = c.getInt(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_STATUS));
        mEncBody = c.getBlob(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_ENCBODY));
        mKeyId = c.getString(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_KEYID));
        mFileDir = c.getString(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_FILEDIR));
        mRetNotify = c.getInt(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_RETNOTIFY));
        mRetPushToken = c.getString(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_RETPUSHTOKEN));
        mRetReceipt = c.getString(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_RETRECEIPT));
    }

    public MsgAction getMessageAction() {
        MsgAction action = MsgAction.DISPLAY_ONLY;
        if (mInbox) {
            if (isFileOpenable()) {
                action = MsgAction.FILE_OPEN;
            } else if (isFileDownloadable()) {
                action = MsgAction.FILE_DOWNLOAD_DECRYPT;
            } else if (TextUtils.isEmpty(mText) && TextUtils.isEmpty(mFileName)) {
                if (mEncBody != null) {
                    action = MsgAction.MSG_DECRYPT;
                } else if (isExpiredOnServer()) {
                    action = MsgAction.MSG_EXPIRED;
                } else {
                    action = MsgAction.MSG_DOWNLOAD;
                }
            }
        } else {
            if (mStatus == MessageDbAdapter.MESSAGE_STATUS_QUEUED) {
                action = MsgAction.MSG_PROGRESS;
            } else if (isEditable()) {
                action = MsgAction.MSG_EDIT;
            } else if (isFileOpenable()) {
                action = MsgAction.FILE_OPEN;
            }
        }
        return action;
    }

    private boolean isExpiredOnServer() {
        return mStatus == MessageDbAdapter.MESSAGE_STATUS_EXPIRED;
    }

    public boolean isFileDownloadable() {
        long diff = System.currentTimeMillis() - Long.valueOf(mDateSent);
        return mRead && mInbox && mFileSize > 0 && diff < SafeSlingerConfig.MESSAGE_EXPIRATION_MS;
    }

    public boolean isFileOpenable() {

        if (!TextUtils.isEmpty(mFileType)
                && !mFileType.startsWith(SafeSlingerConfig.MIMETYPE_CLASS + "/")) {

            if (!TextUtils.isEmpty(mFileDir)) {
                File f = new File(mFileDir);
                return f.exists() && f.length() > 0;
            } else if (!TextUtils.isEmpty(mFileName)) {
                if (!mInbox
                        || (mInbox && mStatus == MessageDbAdapter.MESSAGE_STATUS_FILE_DECRYPTED)) {
                    File f = SSUtil.getOldDefaultDownloadPath(mFileType, mFileName);
                    if (SSUtil.isExternalStorageReadable()) {
                        return f.exists() && f.length() > 0;
                    } else {
                        // make actual access of missing volume to show its
                        // error
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isEditable() {
        return (mStatus == MessageDbAdapter.MESSAGE_STATUS_DRAFT)
                && (TextUtils.isEmpty(mFileType) || (!TextUtils.isEmpty(mFileType) && !mFileType
                        .startsWith(SafeSlingerConfig.MIMETYPE_CLASS)));
    }

    public long getProbableDate() {
        if (mDateSent <= 0) {
            return mDateRecv;
        }

        // if sent stamp is lass than CLOCK_SKEW_MS from recv, trust recv
        return Math.abs(mDateRecv - mDateSent) < SafeSlingerConfig.CLOCK_SKEW_MS ? mDateRecv
                : mDateSent;
    }

    public void setPhoto(byte[] photo) {
        mPhoto = photo;
    }

    public byte[] getPhoto() {
        return mPhoto;
    }

    public String getProgress() {
        return mProgress;
    }

    public void setProgress(String msg) {
        mProgress = msg;
    }

}
