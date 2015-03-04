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

import android.os.Parcel;
import android.os.Parcelable;

public class ThreadData  implements Parcelable{

    private MessageRow mMsgRow;
    private int mMsgCount;
    private int mNewCount;
    private boolean mIsDetail;
    private RecipientRow mRecipient;
    private boolean mHasDraft;
    private String mLastPerson;
    private boolean mNewerExists;
    private String mProgress;

    private static final Parcelable.Creator<ThreadData> CREATOR = new creatorImplementation();
    
    private static final class creatorImplementation implements Parcelable.Creator<ThreadData>
    {

        @Override
        public ThreadData createFromParcel(Parcel source) {
            return new ThreadData(source);
        }

        @Override
        public ThreadData[] newArray(int size) {
            
            return new ThreadData[size];
        }
        
    }
    
    public ThreadData(Parcel in)
    {
        readFromParcel(in);
    }
    
    /***
     * create raw thread
     */
    public ThreadData(MessageRow msgRow, int msgs, int newMsgs, boolean hasDraft,
            String lastPerson, boolean isDetail, boolean newerExists, RecipientRow recip) {
        mMsgRow = msgRow;
        mMsgCount = msgs;
        mNewCount = newMsgs;
        mIsDetail = isDetail;
        mRecipient = recip;
        mHasDraft = hasDraft;
        mLastPerson = lastPerson;
        mNewerExists = newerExists;
        mProgress = msgRow.getProgress();
    }

    /***
     * create merged thread
     */
    public ThreadData(ThreadData old, ThreadData up) {
        mMsgCount = old.getMsgCount() + up.getMsgCount();
        mNewCount = old.getNewCount() + up.getNewCount();
        mMsgRow = old.getMsgRow().getProbableDate() > up.getMsgRow().getProbableDate() ? old
                .getMsgRow() : up.getMsgRow();
        mIsDetail = old.isDetail();
        mRecipient = old.getRecipient();
        mHasDraft = old.hasDraft() || up.hasDraft();
        mLastPerson = old.getLastPerson();
        mNewerExists = old.isNewerExists() || up.isNewerExists();
        mProgress = old.getProgress() != null ? old.getProgress() : up.getProgress();
    }

    public MessageRow getMsgRow() {
        return mMsgRow;
    }

    public int getMsgCount() {
        return mMsgCount;
    }

    public boolean isDetail() {
        return mIsDetail;
    }

    public int getNewCount() {
        return mNewCount;
    }

    public RecipientRow getRecipient() {
        return mRecipient;
    }

    public boolean hasDraft() {
        return mHasDraft;
    }

    public String getLastPerson() {
        return mLastPerson;
    }

    public boolean isNewerExists() {
        return mNewerExists;
    }

    public String getProgress() {
        return mProgress;
    }

    public void setProgress(String msg) {
        mProgress = msg;
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
       
        dest.writeInt(mMsgCount);
        dest.writeInt(mNewCount);
        dest.writeString(mLastPerson);
        dest.writeString(mProgress);
        dest.writeInt((mIsDetail)? 1 : 0);
        dest.writeInt((mHasDraft)? 1 : 0);
        dest.writeInt((mNewerExists)? 1 : 0);
        dest.writeParcelable(mMsgRow, flags);
        dest.writeParcelable(mRecipient, flags);
        
    }
    
    public void readFromParcel(Parcel in)
    {
        mMsgCount = in.readInt();
        mNewCount = in.readInt();
        
        mLastPerson = in.readString();
        mProgress = in.readString();
        
        mIsDetail = (in.readInt() == 1)? true : false;
        mHasDraft = (in.readInt() == 1)?true : false;
        mNewerExists = (in.readInt() == 1)?  true: false;
        
        mMsgRow = in.readParcelable(MessageRow.class.getClassLoader());
        mRecipient = in.readParcelable(RecipientRow.class.getClassLoader());
    }
}
