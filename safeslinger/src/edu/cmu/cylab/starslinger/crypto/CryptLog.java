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

package edu.cmu.cylab.starslinger.crypto;

import android.util.Log;

public class CryptLog {

    public static void d(boolean loggable, String tag, String msg) {
        if (loggable) {
            Log.d(tag, msg);
        }
    }

    public static void i(boolean loggable, String tag, String msg) {
        if (loggable) {
            Log.i(tag, msg);
        }
    }

    public static void e(boolean loggable, String tag, String msg) {
        if (loggable) {
            Log.e(tag, msg);
        }
    }

    public static void v(boolean loggable, String tag, String msg) {
        if (loggable) {
            Log.v(tag, msg);
        }
    }

    public static void w(boolean loggable, String tag, String msg) {
        if (loggable) {
            Log.w(tag, msg);
        }
    }

    public static void d(boolean loggable, String tag, Throwable t) {
        if (loggable) {
            Log.d(tag, t.getLocalizedMessage(), t);
        }
    }

    public static void i(boolean loggable, String tag, Throwable t) {
        if (loggable) {
            Log.i(tag, t.getLocalizedMessage(), t);
        }
    }

    public static void e(boolean loggable, String tag, Throwable t) {
        if (loggable) {
            Log.e(tag, t.getLocalizedMessage(), t);
        }
    }

    public static void v(boolean loggable, String tag, Throwable t) {
        if (loggable) {
            Log.v(tag, t.getLocalizedMessage(), t);
        }
    }

    public static void w(boolean loggable, String tag, Throwable t) {
        if (loggable) {
            Log.w(tag, t.getLocalizedMessage(), t);
        }
    }
}
