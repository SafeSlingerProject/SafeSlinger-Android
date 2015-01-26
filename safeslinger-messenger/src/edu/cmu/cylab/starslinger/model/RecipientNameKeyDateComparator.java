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

import java.util.Comparator;

public class RecipientNameKeyDateComparator implements Comparator<RecipientRow> {

    @Override
    public int compare(RecipientRow m1, RecipientRow m2) {

        if (m1 == null && m2 == null)
            return 0;
        else if (m1 == null)
            return -1;
        else if (m2 == null)
            return 1;

        if (m1.getName() == null && m2.getName() == null)
            return 0;
        else if (m1.getName() == null)
            return -1;
        else if (m2.getName() == null)
            return 1;

        // 1st by name
        int name = m1.getName().compareToIgnoreCase(m2.getName());
        if (name != 0)
            return name;

        // 2nd by exchange date
        int exchdate = Long.valueOf(m1.getExchdate()).compareTo(Long.valueOf(m2.getExchdate()));
        if (exchdate != 0)
            return exchdate;

        // 3rd by key date
        int keydate = Long.valueOf(m1.getKeydate()).compareTo(Long.valueOf(m2.getKeydate()));

        return keydate;
    }
}
