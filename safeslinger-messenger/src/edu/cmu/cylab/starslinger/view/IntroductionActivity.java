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
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientRow;

public class IntroductionActivity extends BaseActivity {
    public static final int VIEW_RECIPSEL1 = 721;
    public static final int VIEW_RECIPSEL2 = 722;
    public static final int RESULT_SEND = 724;
    public static final int RESULT_SLINGKEYS = 723;
    public static final int RESULT_RESTART = 732;

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
    private RecipientRow mRecip1;
    private RecipientRow mRecip2;

    /**
     * Called when the activity is first created. Responsible for initializing
     * the UI.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_SafeSlinger);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sendinvite);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.app_name);
        bar.setSubtitle(R.string.title_SecureIntroduction);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Obtain handles to UI objects
        mTextViewRecipName1 = (TextView) findViewById(R.id.SendTextViewRecipName1);
        mTextViewRecipKey1 = (TextView) findViewById(R.id.SendTextViewRecipKey1);
        mImageViewRecipPhoto1 = (ImageView) findViewById(R.id.SendImageViewRecipPhoto1);
        mTextViewRecipName2 = (TextView) findViewById(R.id.SendTextViewRecipName2);
        mTextViewRecipKey2 = (TextView) findViewById(R.id.SendTextViewRecipKey2);
        mImageViewRecipPhoto2 = (ImageView) findViewById(R.id.SendImageViewRecipPhoto2);
        mButtonSend = (Button) findViewById(R.id.SendButtonSend);
        mEditTextMessage1 = (EditText) findViewById(R.id.TextView1);
        mEditTextMessage2 = (EditText) findViewById(R.id.TextView2);
        mButtonRecip1 = (Button) findViewById(R.id.SendButtonRecipient1);
        mButtonRecip2 = (Button) findViewById(R.id.SendButtonRecipient2);

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

        updateValues(getIntent().getExtras());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuItem item = menu.add(0, MENU_HELP, 0, R.string.menu_Help).setIcon(
                R.drawable.ic_action_help);
        MenuCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        menu.add(0, MENU_CONTACTINVITE, 0, R.string.menu_SelectShareApp).setIcon(
                R.drawable.ic_action_add_person);
        menu.add(0, MENU_FEEDBACK, 0, R.string.menu_sendFeedback).setIcon(
                android.R.drawable.ic_menu_send);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HELP:
                showHelp(getString(R.string.title_SecureIntroduction),
                        getString(R.string.help_SecureIntroduction));
                return true;
            case MENU_FEEDBACK:
                SafeSlinger.getApplication().showFeedbackEmail(IntroductionActivity.this);
                return true;
            case MENU_CONTACTINVITE:
                showAddContactInvite();
                return true;
        }
        return false;
    }

    public void updateValues(Bundle extras) {

        // make sure view is already inflated...
        if (mEditTextMessage1 == null) {
            return;
        }

        // recipient 1
        mImageViewRecipPhoto1.setBackgroundResource(0);
        if (mRecip1 != null) {
            drawUserData(R.string.label_SendTo, mRecip1.getName(), mRecip1.getPhoto(),
                    mTextViewRecipName1, mTextViewRecipKey1, mImageViewRecipPhoto1,
                    mRecip1.getKeyid(), mRecip1.getKeydate());
            mTextViewRecipName1.setTextColor(Color.BLACK);
        } else {
            mTextViewRecipName1.setTextColor(Color.GRAY);
            mTextViewRecipName1.setText(R.string.label_SelectRecip);
            mTextViewRecipKey1.setText("");
            mImageViewRecipPhoto1.setBackgroundResource(R.drawable.ic_silhouette_select);
        }

        // recipient 2
        mImageViewRecipPhoto2.setBackgroundResource(0);
        if (mRecip2 != null) {
            drawUserData(R.string.label_SendTo, mRecip2.getName(), mRecip2.getPhoto(),
                    mTextViewRecipName2, mTextViewRecipKey2, mImageViewRecipPhoto2,
                    mRecip2.getKeyid(), mRecip2.getKeydate());
            mTextViewRecipName2.setTextColor(Color.BLACK);
        } else {
            mTextViewRecipName2.setTextColor(Color.GRAY);
            mTextViewRecipName2.setText(R.string.label_SelectRecip);
            mTextViewRecipKey2.setText("");
            mImageViewRecipPhoto2.setBackgroundResource(R.drawable.ic_silhouette_select);
        }

        // message
        if (mRecip1 != null && mRecip2 != null) {
            // if recip1 and recip2
            mEditTextMessage1.setVisibility(View.VISIBLE);
            mEditTextMessage2.setVisibility(View.VISIBLE);
            mEditTextMessage1.setText(String.format(
                    getString(R.string.label_messageIntroduceNameToYou), mRecip2.getName()));
            mEditTextMessage2.setText(String.format(
                    getString(R.string.label_messageIntroduceNameToYou), mRecip1.getName()));
        } else {
            // if empty.recip1, hide
            mEditTextMessage1.setVisibility(View.GONE);
            // if empty.recip2, hide
            mEditTextMessage2.setVisibility(View.GONE);
        }
    }

    private void doClickRecipient1() {
        showRecipientSelect(VIEW_RECIPSEL1);
    }

    private void doClickRecipient2() {
        showRecipientSelect(VIEW_RECIPSEL2);
    }

    private Intent resultIntent() {
        Intent data = new Intent();
        data.putExtra(extra.TEXT_MESSAGE1, mEditTextMessage1.getText().toString());
        data.putExtra(extra.TEXT_MESSAGE2, mEditTextMessage2.getText().toString());
        if (mRecip1 != null)
            data.putExtra(extra.RECIPIENT_ROW_ID1, mRecip1.getRowId());
        if (mRecip2 != null)
            data.putExtra(extra.RECIPIENT_ROW_ID2, mRecip2.getRowId());
        return data;
    }

    private void drawUserData(int dirId, String name, byte[] photo, TextView textViewUserName,
            TextView textViewKey, ImageView imageViewPhoto, String keyId, long keyDate) {

        StringBuilder detailStr = new StringBuilder();
        if (!CryptTools.isNullKeyId(keyId)) {
            if (keyDate > 0) {
                detailStr
                        .append(getString(R.string.label_Key))
                        .append(" ")
                        .append(DateUtils.getRelativeTimeSpanString(getApplicationContext(),
                                keyDate));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {

            case VIEW_RECIPSEL1:
                switch (resultCode) {
                    case PickRecipientsActivity.RESULT_SLINGKEYS:
                        setResult(RESULT_SLINGKEYS);
                        finish();
                        break;
                    case PickRecipientsActivity.RESULT_RECIPSEL:
                        long rowIdRecipient1 = data.getLongExtra(extra.RECIPIENT_ROW_ID, -1);

                        if (mRecip2 != null && rowIdRecipient1 == mRecip2.getRowId()) {
                            showNote(R.string.error_InvalidRecipient);
                            break;
                        }

                        RecipientDbAdapter dbRecipient = RecipientDbAdapter
                                .openInstance(getApplicationContext());
                        Cursor c = dbRecipient.fetchRecipient(rowIdRecipient1);
                        if (c != null) {
                            mRecip1 = new RecipientRow(c);
                            c.close();
                            updateValues(null);
                        } else {
                            showNote(R.string.error_InvalidRecipient);
                            break;
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // nothing to change...
                        break;
                }
                break;

            case VIEW_RECIPSEL2:
                switch (resultCode) {
                    case PickRecipientsActivity.RESULT_SLINGKEYS:
                        setResult(RESULT_SLINGKEYS);
                        finish();
                        break;
                    case PickRecipientsActivity.RESULT_RECIPSEL:
                        long rowIdRecipient2 = data.getLongExtra(extra.RECIPIENT_ROW_ID, -1);

                        if (mRecip1 != null && rowIdRecipient2 == mRecip1.getRowId()) {
                            showNote(R.string.error_InvalidRecipient);
                            break;
                        }

                        RecipientDbAdapter dbRecipient = RecipientDbAdapter
                                .openInstance(getApplicationContext());
                        Cursor c = dbRecipient.fetchRecipient(rowIdRecipient2);
                        if (c != null) {
                            mRecip2 = new RecipientRow(c);
                            c.close();
                            updateValues(null);
                        } else {
                            showNote(R.string.error_InvalidRecipient);
                            break;
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // nothing to change...
                        break;
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showRecipientSelect(int requestCode) {
        Intent intent = new Intent(IntroductionActivity.this, PickRecipientsActivity.class);
        intent.putExtra(extra.ALLOW_EXCH, true);
        intent.putExtra(extra.ALLOW_INTRO, true);
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateValues(null);
    }

    private void sendResultToHost(int resultCode, Bundle args) {
        Intent data = new Intent();
        data.putExtras(args);
        setResult(resultCode, data);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save
        if (mRecip1 != null)
            outState.putLong(extra.RECIPIENT_ROW_ID1, mRecip1.getRowId());
        else
            outState.putLong(extra.RECIPIENT_ROW_ID1, -1);

        if (mRecip2 != null)
            outState.putLong(extra.RECIPIENT_ROW_ID2, mRecip2.getRowId());
        else
            outState.putLong(extra.RECIPIENT_ROW_ID2, -1);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // general state
        if (savedInstanceState != null) {
            long rowId1 = savedInstanceState.getLong(extra.RECIPIENT_ROW_ID1);
            long rowId2 = savedInstanceState.getLong(extra.RECIPIENT_ROW_ID2);

            RecipientDbAdapter dbRecipient = RecipientDbAdapter
                    .openInstance(getApplicationContext());
            Cursor c1 = dbRecipient.fetchRecipient(rowId1);
            if (c1 != null) {
                mRecip1 = new RecipientRow(c1);
                c1.close();
            }
            Cursor c2 = dbRecipient.fetchRecipient(rowId2);
            if (c2 != null) {
                mRecip2 = new RecipientRow(c2);
                c2.close();
            }
            updateValues(null);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(IntroductionActivity.this, args).create();
            case DIALOG_CONTACTINVITE:
                return xshowAddContactInvite(this).create();
            case DIALOG_CONTACTTYPE:
                return xshowCustomContactPicker(this, args).create();
        }
        return super.onCreateDialog(id);
    }
}
