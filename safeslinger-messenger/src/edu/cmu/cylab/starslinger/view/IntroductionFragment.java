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
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.model.DraftData;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientRow;

public class IntroductionFragment extends Fragment {
    public static final int RESULT_SEND = 724;
    public static final int RESULT_SLINGKEYS = 723;
    public static final int RESULT_RESTART = 732;
    public static final int RESULT_RECIPSEL1 = 735;
    public static final int RESULT_RECIPSEL2 = 736;

    private TextView mTextViewRecipName2;
    private TextView mTextViewRecipName1;
    private TextView mTextViewRecipKey2;
    private TextView mTextViewRecipKey1;
    private ImageView mImageViewRecipPhoto2;
    private ImageView mImageViewRecipPhoto1;
    private EditText mEditTextMessage1;
    private EditText mEditTextMessage2;
    private Button mButtonSend;
    private Button mButtonRecip1;
    private Button mButtonRecip2;
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    private static OnIntroResultListener mResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateValues(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vFrag = inflater.inflate(R.layout.sendinvite, container, false);
        mTextViewRecipName1 = (TextView) vFrag.findViewById(R.id.SendTextViewRecipName1);
        mTextViewRecipKey1 = (TextView) vFrag.findViewById(R.id.SendTextViewRecipKey1);
        mImageViewRecipPhoto1 = (ImageView) vFrag.findViewById(R.id.SendImageViewRecipPhoto1);
        mTextViewRecipName2 = (TextView) vFrag.findViewById(R.id.SendTextViewRecipName2);
        mTextViewRecipKey2 = (TextView) vFrag.findViewById(R.id.SendTextViewRecipKey2);
        mImageViewRecipPhoto2 = (ImageView) vFrag.findViewById(R.id.SendImageViewRecipPhoto2);
        mButtonSend = (Button) vFrag.findViewById(R.id.SendButtonSend);
        mEditTextMessage1 = (EditText) vFrag.findViewById(R.id.TextView1);
        mEditTextMessage2 = (EditText) vFrag.findViewById(R.id.TextView2);
        mButtonRecip1 = (Button) vFrag.findViewById(R.id.SendButtonRecipient1);
        mButtonRecip2 = (Button) vFrag.findViewById(R.id.SendButtonRecipient2);

        updateValues(savedInstanceState);

        OnClickListener clickSender = new OnClickListener() {

            @Override
            public void onClick(View v) {
                doClickRecipient1();
            }
        };
        mButtonRecip1.setOnClickListener(clickSender);

        OnClickListener clickRecip = new OnClickListener() {

            @Override
            public void onClick(View v) {
                doClickRecipient2();
            }
        };
        mButtonRecip2.setOnClickListener(clickRecip);

        mButtonSend.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                sendResultToHost(RESULT_SEND, resultIntent().getExtras());
            }
        });

        mEditTextMessage2.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendResultToHost(RESULT_SEND, resultIntent().getExtras());
                    return true;
                }
                return false;
            }
        });

        return vFrag;
    }

    public void updateValues(Bundle extras) {
        // general state
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(getActivity());
        DraftData d = DraftData.INSTANCE;

        // make sure view is already inflated...
        if (mEditTextMessage1 == null) {
            return;
        }

        // recipient 1
        mImageViewRecipPhoto1.setImageResource(0);
        RecipientRow r1 = d.getRecip1();
        if (d.existsRecip1()) {
            drawUserData(R.string.label_SendTo, r1.getName(), r1.getPhoto(), mTextViewRecipName1,
                    mTextViewRecipKey1, mImageViewRecipPhoto1, r1.getKeyid(), r1.getKeydate());
            mTextViewRecipName1.setTextColor(Color.BLACK);
        } else {
            mTextViewRecipName1.setTextColor(Color.GRAY);
            mTextViewRecipName1.setText(R.string.label_SelectRecip);
            mTextViewRecipKey1.setText("");
            mImageViewRecipPhoto1.setImageResource(R.drawable.ic_silhouette_select);
        }

        // recipient 2
        mImageViewRecipPhoto2.setImageResource(0);
        RecipientRow r2 = d.getRecip2();
        if (d.existsRecip2()) {
            drawUserData(R.string.label_SendTo, r2.getName(), r2.getPhoto(), mTextViewRecipName2,
                    mTextViewRecipKey2, mImageViewRecipPhoto2, r2.getKeyid(), r2.getKeydate());
            mTextViewRecipName2.setTextColor(Color.BLACK);
        } else {
            mTextViewRecipName2.setTextColor(Color.GRAY);
            mTextViewRecipName2.setText(R.string.label_SelectRecip);
            mTextViewRecipKey2.setText("");
            mImageViewRecipPhoto2.setImageResource(R.drawable.ic_silhouette_select);
        }

        // message
        if (d.existsRecip1() && d.existsRecip2()) {
            // if recip1 and recip2
            mEditTextMessage1.setVisibility(View.VISIBLE);
            mEditTextMessage2.setVisibility(View.VISIBLE);
            mEditTextMessage1.setText(String.format(
                    getString(R.string.label_messageIntroduceNameToYou), r2.getName()));
            mEditTextMessage2.setText(String.format(
                    getString(R.string.label_messageIntroduceNameToYou), r1.getName()));
        } else {
            // if empty.recip1, hide
            mEditTextMessage1.setVisibility(View.GONE);
            // if empty.recip2, hide
            mEditTextMessage2.setVisibility(View.GONE);
        }
    }

    private void doClickRecipient1() {
        sendResultToHost(RESULT_RECIPSEL1, resultIntent().getExtras());
    }

    private void doClickRecipient2() {
        sendResultToHost(RESULT_RECIPSEL2, resultIntent().getExtras());
    }

    private Intent resultIntent() {
        DraftData d = DraftData.INSTANCE;
        Intent data = new Intent();
        data.putExtra(extra.TEXT_MESSAGE1, mEditTextMessage1.getText().toString());
        data.putExtra(extra.TEXT_MESSAGE2, mEditTextMessage2.getText().toString());
        if (d.existsRecip1()) {
            data.putExtra(extra.RECIPIENT_ROW_ID1, d.getRecip1RowId());
        }
        if (d.existsRecip2()) {
            data.putExtra(extra.RECIPIENT_ROW_ID2, d.getRecip2RowId());
        }
        return data;
    }

    private void drawUserData(int dirId, String name, byte[] photo, TextView textViewUserName,
            TextView textViewKey, ImageView imageViewPhoto, String keyId, long keyDate) {

        StringBuilder detailStr = new StringBuilder();
        if (!CryptTools.isNullKeyId(keyId)) {
            if (keyDate > 0) {
                detailStr.append(getString(R.string.label_Key)).append(" ")
                        .append(DateUtils.getRelativeTimeSpanString(getActivity(), keyDate));
            }
        }

        // draw name
        if (textViewUserName != null)
            textViewUserName.setText(getString(dirId) + " " + name);

        // draw keys
        if (textViewKey != null)
            textViewKey.setText(detailStr);

        // draw photo
        if (photo != null) {
            try {
                Bitmap bm = BitmapFactory.decodeByteArray(photo, 0, photo.length, null);
                imageViewPhoto.setImageBitmap(bm);
            } catch (OutOfMemoryError e) {
                imageViewPhoto.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_silhouette));
            }
        } else {
            imageViewPhoto.setImageDrawable(getResources().getDrawable(R.drawable.ic_silhouette));
        }
    }

    public interface OnIntroResultListener {
        public void onIntroResultListener(Bundle args);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mResult = (OnIntroResultListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + OnIntroResultListener.class.getSimpleName());
        }
    }

    static private void sendResultToHost(int resultCode, Bundle args) {
        if (args == null) {
            args = new Bundle();
        }
        args.putInt(extra.RESULT_CODE, resultCode);
        mResult.onIntroResultListener(args);
    }

    public static class IntroAlertDialogFragment extends DialogFragment {

        public static IntroAlertDialogFragment newInstance(int id) {
            return newInstance(id, new Bundle());
        }

        public static IntroAlertDialogFragment newInstance(int id, Bundle args) {
            IntroAlertDialogFragment frag = new IntroAlertDialogFragment();
            args.putInt(extra.RESULT_CODE, id);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt(extra.RESULT_CODE);
            switch (id) {
                case BaseActivity.DIALOG_HELP:
                    return BaseActivity.xshowHelp(getActivity(), getArguments()).create();
                default:
                    break;
            }
            return super.onCreateDialog(savedInstanceState);
        }
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
            int readDuration = msg.length() * SafeSlingerConfig.MS_READ_PER_CHAR;
            if (readDuration <= SafeSlingerConfig.SHORT_DELAY) {
                Toast toast = Toast.makeText(this.getActivity(), msg.trim(), Toast.LENGTH_SHORT);
                toast.show();
            } else if (readDuration <= SafeSlingerConfig.LONG_DELAY) {
                Toast toast = Toast.makeText(this.getActivity(), msg.trim(), Toast.LENGTH_LONG);
                toast.show();
            } else {
                showHelp(getString(R.string.app_name), msg.trim());
            }
        }
    }

    protected void showHelp(String title, String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_TITLE, title);
        args.putString(extra.RESID_MSG, msg);
        DialogFragment newFragment = IntroAlertDialogFragment.newInstance(BaseActivity.DIALOG_HELP,
                args);
        newFragment.show(getFragmentManager(), "dialog");
    }

    public void updateKeypad() {
        // if soft input open, close it...
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        View focus = getActivity().getCurrentFocus();
        if (focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
