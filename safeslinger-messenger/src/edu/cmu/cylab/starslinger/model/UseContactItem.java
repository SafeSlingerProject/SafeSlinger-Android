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

public class UseContactItem {
    public final String text;
    public final byte[] icon;
    public final boolean contact;
    public final String contactLookupKey;
    public final UCType type;

    public static enum UCType {
        NONE, PROFILE, CONTACT, ANOTHER, NEW, EDIT_CONTACT, EDIT_NAME
    }

    public UseContactItem(String text, byte[] icon, String lookupKey, UCType t) {
        this.text = text;
        this.icon = icon;
        this.contact = true;
        this.contactLookupKey = lookupKey;
        this.type = t;
    }

    public UseContactItem(String text, UCType t) {
        this.text = text;
        this.icon = null;
        this.contact = false;
        this.contactLookupKey = null;
        this.type = t;
    }

    @Override
    public String toString() {
        return text;
    }
}
