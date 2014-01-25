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

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class IntegerListPreference extends ListPreference {

    public IntegerListPreference(Context context) {
        super(context);

        verifyEntryValues(null);
    }

    public IntegerListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        verifyEntryValues(null);
    }

    @Override
    public void setEntryValues(CharSequence[] entryValues) {
        CharSequence[] oldValues = getEntryValues();
        super.setEntryValues(entryValues);
        verifyEntryValues(oldValues);
    }

    @Override
    public void setEntryValues(int entryValuesResId) {
        CharSequence[] oldValues = getEntryValues();
        super.setEntryValues(entryValuesResId);
        verifyEntryValues(oldValues);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        // During initial load, there's no known default value
        int defaultIntegerValue = Integer.MIN_VALUE;
        if (defaultReturnValue != null) {
            defaultIntegerValue = Integer.parseInt(defaultReturnValue);
        }

        // When the list preference asks us to read a string, instead read an
        // integer.
        int value = getPersistedInt(defaultIntegerValue);
        return Integer.toString(value);
    }

    @Override
    protected boolean persistString(String value) {
        // When asked to save a string, instead save an integer
        return persistInt(Integer.parseInt(value));
    }

    private void verifyEntryValues(CharSequence[] oldValues) {
        CharSequence[] entryValues = getEntryValues();
        if (entryValues == null) {
            return;
        }

        for (CharSequence entryValue : entryValues) {
            try {
                Integer.parseInt(entryValue.toString());
            } catch (NumberFormatException nfe) {
                super.setEntryValues(oldValues);
                throw nfe;
            }
        }
    }
}
