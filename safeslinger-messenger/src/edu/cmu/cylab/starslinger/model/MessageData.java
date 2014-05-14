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

public class MessageData {

    protected String mMsgHash = null;
    protected int mFileSize = 0;
    protected String mFileName = null;
    protected String mFileType = null;
    protected String mText = null;
    protected String mPerson = null;
    protected boolean mInbox;
    protected long mDateRecv;
    protected long mRowId = -1;
    protected boolean mSeen = false;
    protected boolean mRead = false;
    protected long mDateSent = 0;
    protected int mStatus = MessageDbAdapter.MESSAGE_STATUS_NONE;
    protected byte[] mEncBody = null;
    protected String mKeyId = null;
    protected String mFileDir = null;
    protected byte[] mPhoto = null;
    protected byte[] mRawFile = null;
    protected byte[] mFileHash = null;
    protected int mRetNotify;
    protected String mRetPushToken;
    protected String mRetReceipt;
    protected String mProgress = null;
    protected boolean mIsInboxTable = false;

    public MessageData() {
        super();
    }

    // getters...

    public String getFileType() {
        return mFileType;
    }

    public String getFileName() {
        return mFileName;
    }

    public int getFileSize() {
        return mFileSize;
    }

    public String getMsgHash() {
        return mMsgHash;
    }

    public String getText() {
        return mText;
    }

    public boolean isInbox() {
        return mInbox;
    }

    public long getDateRecv() {
        return mDateRecv;
    }

    public long getRowId() {
        return mRowId;
    }

    public boolean isSeen() {
        return mSeen;
    }

    public boolean isRead() {
        return mRead;
    }

    public long getDateSent() {
        return mDateSent;
    }

    public int getStatus() {
        return mStatus;
    }

    public byte[] getEncBody() {
        return mEncBody;
    }

    public String getKeyId() {
        return mKeyId;
    }

    public String getFileDir() {
        return mFileDir;
    }

    public byte[] getFileData() {
        return mRawFile;
    }

    public byte[] getFileHash() {
        return mFileHash;
    }

    public String getPerson() {
        return mPerson;
    }

    public boolean isInboxTable() {
        return mIsInboxTable;
    }

    // setters...

    public void setFileType(String value) {
        mFileType = value;
    }

    public void setFileName(String value) {
        mFileName = value;
    }

    public void setFileSize(int value) {
        mFileSize = value;
    }

    public void setMsgHash(String value) {
        mMsgHash = value;
    }

    public void setText(String value) {
        mText = value;
    }

    public void setInbox(boolean value) {
        mInbox = value;
    }

    public void setDateRecv(long value) {
        mDateRecv = value;
    }

    public void setRowId(long value) {
        mRowId = value;
    }

    public void setSeen(boolean value) {
        mSeen = value;
    }

    public void setRead(boolean value) {
        mRead = value;
    }

    public void setDateSent(long value) {
        mDateSent = value;
    }

    public void setStatus(int value) {
        mStatus = value;
    }

    public void setEncBody(byte[] value) {
        mEncBody = value;
    }

    public void setKeyId(String value) {
        mKeyId = value;
    }

    public void setFileDir(String value) {
        mFileDir = value;
    }

    public void setFileData(byte[] fileData) {
        mRawFile = fileData;
    }

    public void setFileHash(byte[] value) {
        mFileHash = value;
    }

    public void setPerson(String value) {
        mPerson = value;
    }

    public void setInboxTable(boolean value) {
        mIsInboxTable = value;
    }

    // Others...

    public void removeFile() {
        mRawFile = new byte[0];
        mFileSize = 0;
        mFileName = null;
        mFileDir = null;
        mFileType = null;
        mFileHash = null;
    }

    public void removeText() {
        mText = null;
    }

}
