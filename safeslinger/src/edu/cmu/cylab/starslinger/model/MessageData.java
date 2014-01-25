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

    // setters...

    public void setRowId(long rowId) {
        mRowId = rowId;
    }

    public void setFileType(String fileType) {
        mFileType = fileType;
    }

    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    public void setFileDir(String fileDir) {
        mFileDir = fileDir;
    }

    public void setMsgHash(String msgHash) {
        mMsgHash = msgHash;
    }

    public void setText(String text) {
        mText = text;
    }

    public void setFileData(byte[] fileData) {
        mRawFile = fileData;
        mFileSize = fileData == null ? 0 : fileData.length;
    }

    public void setKeyId(String keyid) {
        mKeyId = keyid;
    }

    public void setFileHash(byte[] fileHash) {
        mFileHash = fileHash;
    }

    public void setPerson(String person) {
        mPerson = person;
    }

    public String getPerson() {
        return mPerson;
    }

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

    public void setFileSize(int fileSize) {
        mFileSize = fileSize;
    }

}
