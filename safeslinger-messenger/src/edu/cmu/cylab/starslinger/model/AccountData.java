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

import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import edu.cmu.cylab.starslinger.R;

public class AccountData {
    private String mName = null;
    private String mType = null;
    private CharSequence mTypeLabel = null;
    private Drawable mIcon = null;

    public AccountData(Context ctx, String name, AuthenticatorDescription description) {
        mName = name;
        if (description != null) {
            mType = description.type;

            // The type string is stored in a resource, so we need to
            // convert it into something
            // human readable.
            String packageName = description.packageName;
            PackageManager pm = ctx.getPackageManager();

            if (description.labelId != 0) {
                mTypeLabel = pm.getText(packageName, description.labelId, null);
                if (mTypeLabel == null) {
                    mTypeLabel = ctx.getString(R.string.label_undefinedTypeLabel);
                }
            } else {
                mTypeLabel = "";
            }

            if (description.iconId != 0) {
                mIcon = pm.getDrawable(packageName, description.iconId, null);
            } else {
                mIcon = ctx.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            }
        }
    }

    public String getName() {
        return mName;
    }

    public String getType() {
        return mType;
    }

    public CharSequence getTypeLabel() {
        return mTypeLabel;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    @Override
    public String toString() {
        return mName;
    }

}
