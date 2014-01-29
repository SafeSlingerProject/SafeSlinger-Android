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

package edu.cmu.cylab.starslinger;

import android.util.Log;

public class MyLog {

    public static void d(String tag, String msg) {
        if (SafeSlinger.getApplication().isLoggable()) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (SafeSlinger.getApplication().isLoggable()) {
            Log.i(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (SafeSlinger.getApplication().isLoggable()) {
            Log.e(tag, msg);
        }
    }

    public static void v(String tag, String msg) {
        if (SafeSlinger.getApplication().isLoggable()) {
            Log.v(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (SafeSlinger.getApplication().isLoggable()) {
            Log.w(tag, msg);
        }
    }

    public static void d(String tag, Throwable t) {
        if (SafeSlinger.getApplication().isLoggable()) {
            Log.d(tag, t.getLocalizedMessage(), t);
        }
    }

    public static void i(String tag, Throwable t) {
        if (SafeSlinger.getApplication().isLoggable()) {
            Log.i(tag, t.getLocalizedMessage(), t);
        }
    }

    public static void e(String tag, Throwable t) {
        if (SafeSlinger.getApplication().isLoggable()) {
            Log.e(tag, t.getLocalizedMessage(), t);
        }
    }

    public static void v(String tag, Throwable t) {
        if (SafeSlinger.getApplication().isLoggable()) {
            Log.v(tag, t.getLocalizedMessage(), t);
        }
    }

    public static void w(String tag, Throwable t) {
        if (SafeSlinger.getApplication().isLoggable()) {
            Log.w(tag, t.getLocalizedMessage(), t);
        }
    }
}