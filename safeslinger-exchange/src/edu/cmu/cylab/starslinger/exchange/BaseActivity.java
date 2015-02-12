
package edu.cmu.cylab.starslinger.exchange;

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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager.BadTokenException;
import android.widget.Toast;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig.extra;

public class BaseActivity extends Activity {

    protected static final String TAG = ExchangeConfig.LOG_TAG;
    protected static final int DIALOG_HELP = 1;
    protected static final int DIALOG_ERROR = 2;
    protected static final int DIALOG_QUESTION = 3;
    protected static final int DIALOG_GRP_SIZE = 4;
    protected static final int DIALOG_PROGRESS = 5;

    protected void showHelp(String title, String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_TITLE, title);
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            try {
                removeDialog(DIALOG_HELP);
                showDialog(DIALOG_HELP, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    protected static AlertDialog.Builder xshowHelp(Activity act, Bundle args) {
        String title = args.getString(extra.RESID_TITLE);
        String msg = args.getString(extra.RESID_MSG);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(title);
        ad.setMessage(msg);
        ad.setCancelable(true);
        ad.setNeutralButton(android.R.string.ok, new OnClickListener() {

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
        Log.i(TAG, msg);
        if (msg != null) {
            int readDuration = msg.length() * ExchangeConfig.MS_READ_PER_CHAR;
            if (readDuration <= ExchangeConfig.SHORT_DELAY) {
                Toast toast = Toast.makeText(BaseActivity.this, msg.trim(), Toast.LENGTH_SHORT);
                toast.show();
            } else if (readDuration <= ExchangeConfig.LONG_DELAY) {
                Toast toast = Toast.makeText(BaseActivity.this, msg.trim(), Toast.LENGTH_LONG);
                toast.show();
            } else {
                showHelp(getString(R.string.lib_name), msg.trim());
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

    protected void finishInvalidBundle(String logError) {
        Log.e(TAG, logError);
        setResultForParent(RESULT_CANCELED);
        finish();
    }
}
