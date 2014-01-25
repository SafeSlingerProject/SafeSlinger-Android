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

package edu.cmu.cylab.starslinger.view;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.ConfigData.extra;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;

public class SlingerFragment extends SherlockFragment {

    private static final String TAG = ConfigData.LOG_TAG;
    public static final int RESULT_SHOWCOMPOSE = 99;
    private OnSlingerResultListener mResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateValues(savedInstanceState);
    }

    public void updateValues(Bundle extras) {
        if (extras != null) {
            // nothing yet
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            View vFrag = inflater.inflate(R.layout.splash, container, false);
            return vFrag;
        } catch (OutOfMemoryError e) {
            showNote(R.string.error_OutOfMemoryError);
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sendResultToHost(RESULT_SHOWCOMPOSE, new Bundle());
    }

    public interface OnSlingerResultListener {
        public void onSlingerResultListener(Bundle args);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mResult = (OnSlingerResultListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + OnSlingerResultListener.class.getSimpleName());
        }
    }

    private void sendResultToHost(int resultCode, Bundle args) {
        args.putInt(extra.RESULT_CODE, resultCode);
        mResult.onSlingerResultListener(args);
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
            Toast toast = Toast.makeText(this.getActivity(), msg, Toast.LENGTH_LONG);
            toast.show();
        }
    }
}
