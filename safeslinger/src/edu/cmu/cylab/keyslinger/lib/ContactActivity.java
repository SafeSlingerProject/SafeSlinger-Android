
package edu.cmu.cylab.keyslinger.lib;

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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;

import edu.cmu.cylab.keyslinger.lib.KsConfig.extra;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;

public class ContactActivity extends SherlockActivity {

    private static final String TAG = KsConfig.LOG_TAG;
    public static final int DIALOG_HELP = 1;
    public static final int DIALOG_ERROR = 2;
    public static final int DIALOG_QUESTION = 3;
    public static final int DIALOG_GRP_SIZE = 4;

    public Uri getPersonUri(String contactLookupKey) {
        if (!TextUtils.isEmpty(contactLookupKey)) {
            Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                    contactLookupKey);
            if (lookupUri != null) {
                return ContactsContract.Contacts.lookupContact(getContentResolver(), lookupUri);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public Uri getDataUri(String contactLookupKey) {
        if (!TextUtils.isEmpty(contactLookupKey)) {
            Uri personUri = getPersonUri(contactLookupKey);
            if (personUri != null) {
                return Uri.withAppendedPath(personUri, Contacts.Data.CONTENT_DIRECTORY);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected void showHelp(String title, String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_TITLE, title);
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            removeDialog(DIALOG_HELP);
            showDialog(DIALOG_HELP, args);
        }
    }

    protected static AlertDialog.Builder xshowHelp(Activity act, Bundle args) {
        String title = args.getString(extra.RESID_TITLE);
        String msg = args.getString(extra.RESID_MSG);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(title);
        ad.setMessage(msg);
        ad.setCancelable(true);
        ad.setNeutralButton(R.string.btn_Close, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        ad.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    protected void showNote(int resId) {
        showNote(getString(resId));
    }

    protected void showNote(Exception e) {
        String msg = e.getLocalizedMessage();
        if (TextUtils.isEmpty(msg)) {
            showNote(e.getClass().getSimpleName());
        } else {
            showNote(msg);
        }
    }

    protected void showNote(String msg) {
        MyLog.i(TAG, msg);
        if (msg != null) {
            int readDuration = msg.length() * ConfigData.MS_READ_PER_CHAR;
            if (readDuration <= ConfigData.SHORT_DELAY) {
                Toast toast = Toast.makeText(ContactActivity.this, msg, Toast.LENGTH_SHORT);
                toast.show();
            } else if (readDuration <= ConfigData.LONG_DELAY) {
                Toast toast = Toast.makeText(ContactActivity.this, msg, Toast.LENGTH_LONG);
                toast.show();
            } else {
                showHelp(getString(R.string.app_name), msg);
            }
        }
    }

    protected void setResultForParent(int resultCode) {
        if (getParent() == null) {
            setResult(resultCode);
        } else {
            getParent().setResult(resultCode);
        }
    }

    protected void setResultForParent(int resultCode, Intent data) {
        if (getParent() == null) {
            setResult(resultCode, data);
        } else {
            getParent().setResult(resultCode, data);
        }
    }

    @Override
    public void onUserInteraction() {
        // update the cache timeout
        SafeSlinger.updateCachedPassPhrase(ConfigData.loadPrefKeyIdString(getApplicationContext()));
    }

}
