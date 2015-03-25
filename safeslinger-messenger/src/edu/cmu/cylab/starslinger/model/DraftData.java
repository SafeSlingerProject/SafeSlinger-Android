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

    public boolean existsRecip() {
        return mRecip != null;
    }

    public boolean existsRecip1() {
        return mRecip1 != null;
    }

    public boolean existsRecip2() {
        return mRecip2 != null;
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

    public void setRecip(RecipientRow recip) {
        mRecip = recip;
    }

    public void setRecip1(RecipientRow recip) {
        mRecip1 = recip;
    }

    public void setRecip2(RecipientRow recip) {
        mRecip2 = recip;
    }

    public RecipientRow getRecip1() {
        return mRecip1;
    }

    public RecipientRow getRecip2() {
        return mRecip2;
    }

}
