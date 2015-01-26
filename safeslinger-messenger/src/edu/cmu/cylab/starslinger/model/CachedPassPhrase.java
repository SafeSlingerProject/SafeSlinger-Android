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

public class CachedPassPhrase {
    public final long timestamp;
    public final String passPhrase;

    public CachedPassPhrase(long timestamp, String passPhrase) {
        super();
        this.timestamp = timestamp;
        this.passPhrase = passPhrase;
    }

    @Override
    public int hashCode() {
        int hc1 = (int) (this.timestamp & 0xffffffff);
        int hc2 = (this.passPhrase == null ? 0 : this.passPhrase.hashCode());
        return (hc1 + hc2) * hc2 + hc1;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CachedPassPhrase)) {
            return false;
        }

        CachedPassPhrase o = (CachedPassPhrase) other;
        if (timestamp != o.timestamp) {
            return false;
        }

        if (passPhrase != o.passPhrase) {
            if (passPhrase == null || o.passPhrase == null) {
                return false;
            }

            if (!passPhrase.equals(o.passPhrase)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "(" + timestamp + ", *******)";
    }
}
