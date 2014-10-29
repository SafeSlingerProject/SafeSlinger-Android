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

/***
 * This a simple, fast, and thread-safe singleton implementation for keeping
 * state of a user's draft data when constructing a invitation on the
 * Introduction tab or message on the Compose tab.
 */
public class DraftData {
    public final static DraftData INSTANCE = new DraftData();

    private RecipientRow mRecip = null;
    private RecipientRow mRecip1 = null;
    private RecipientRow mRecip2 = null;
    private MessageData mSendMsg = new MessageData();

    private DraftData() {
    }

    public void clearRecip() {
        mRecip = null;
    }

    public void clearRecip1() {
        mRecip1 = null;
    }

    public void clearRecip2() {
        mRecip2 = null;
    }

    public void clearSendMsg() {
        mSendMsg = new MessageData();
    }

    public boolean existsRecip() {
        return mRecip != null;
    }

    public boolean existsRecip1() {
        return mRecip1 != null;
    }

    public boolean existsRecip2() {
        return mRecip2 != null;
    }

    public byte[] getFileData() {
        return mSendMsg.getFileData();
    }

    public String getFileName() {
        return mSendMsg.getFileName();
    }

    public int getFileSize() {
        return mSendMsg.getFileSize();
    }

    public String getFileType() {
        return mSendMsg.getFileType();
    }

    public int getNotify() {
        return mRecip.getNotify();
    }

    public RecipientRow getRecip() {
        return mRecip;
    }

    public long getRecip1RowId() {
        return mRecip1.getRowId();
    }

    public long getRecip2RowId() {
        return mRecip2.getRowId();
    }

    public long getRecipRowId() {
        return mRecip.getRowId();
    }

    public MessageData getSendMsg() {
        return mSendMsg;
    }

    public long getSendMsgRowId() {
        return mSendMsg.getRowId();
    }

    public String getText() {
        return mSendMsg.getText();
    }

    public void removeFile() {
        mSendMsg.removeFile();
    }

    public void removeText() {
        mSendMsg.removeText();
    }

    public void setFileData(byte[] data) {
        mSendMsg.setFileData(data);
    }

    public void setFileDir(String fileDir) {
        mSendMsg.setFileDir(fileDir);
    }

    public void setFileName(String fileName) {
        mSendMsg.setFileName(fileName);
    }

    public void setFileSize(int fileSize) {
        mSendMsg.setFileSize(fileSize);
    }

    public void setFileType(String fileType) {
        mSendMsg.setFileType(fileType);
    }

    public void setKeyId(String keyId) {
        mSendMsg.setKeyId(keyId);
    }

    public void setMsgHash(String msgHash) {
        mSendMsg.setMsgHash(msgHash);
    }

    public void setRecip(RecipientRow recip2) {
        mRecip = recip2;
    }

    public void setRecip1(RecipientRow recip1) {
        mRecip1 = recip1;
    }

    public void setRecip2(RecipientRow recip) {
        mRecip2 = recip;
    }

    public void setSendMsg(MessageData sendMsg) {
        mSendMsg = sendMsg;
    }

    public void setSendMsgRowId(long rowId) {
        mSendMsg.setRowId(rowId);
    }

    public void setText(String text) {
        mSendMsg.setText(text);
    }

}
