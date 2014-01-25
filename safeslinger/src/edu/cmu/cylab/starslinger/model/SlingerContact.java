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

import edu.cmu.cylab.starslinger.ConfigData;

public class SlingerContact {
    public int index;
    public String lookup = null;
    public String rawid = null;
    public String name = null;
    public byte[] photoBytes = null;
    public int notify = ConfigData.NOTIFY_NOPUSH;
    public String pushTok = null;
    public String pubKey = null;

    @Deprecated
    public String contactId = null;

    public static SlingerContact createContact(String id, String lookup, String rawId, String name,
            byte[] photo, SlingerIdentity si) {
        SlingerContact ct = new SlingerContact();
        ct.contactId = id;
        ct.lookup = lookup;
        ct.rawid = rawId;
        ct.name = name;
        ct.photoBytes = photo;
        if (si != null) {
            ct.pubKey = si.getPublicKey();
            ct.pushTok = si.getToken();
            ct.notify = si.getNotification();
        }
        return ct;
    }
}
