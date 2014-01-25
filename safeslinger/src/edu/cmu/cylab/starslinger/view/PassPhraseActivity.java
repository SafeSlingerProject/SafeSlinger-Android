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

import java.util.ArrayList;
import java.util.Date;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.ConfigData.extra;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.model.UserData;

public class PassPhraseActivity extends BaseActivity {
    private Button mButtonOk;
    private ImageButton mButtonHelp;
    private TextView mTextViewVersion;
    private EditText mEditTextPassNext;
    private EditText mEditTextPassDone;
    private boolean mChangePassPhrase = false;
    private boolean mCreatePassPhrase = false;
    private int mUserNumber = 0;
    private Handler mHandler;
    private final static int mMsPollInterval = 1000;
    public static final int RESULT_CLOSEANDCONTINUE = 999;
    private static final int MENU_HELP = 10;
    private static final int MENU_FEEDBACK = 490;
    private ArrayList<UserData> mUsers;
    private UserAdapter mUserAdapter;
    private Spinner mSpinnerUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock_Light_NoActionBar);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.passphrase);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mButtonOk = (Button) findViewById(R.id.PassButtonOK);
        mButtonHelp = (ImageButton) findViewById(R.id.PassButtonHelp);
        mSpinnerUser = (Spinner) findViewById(R.id.PassSpinnerUserName);
        mTextViewVersion = (TextView) findViewById(R.id.PassTextViewVersion);
        mEditTextPassNext = (EditText) findViewById(R.id.EditTextPassphrase);
        mEditTextPassDone = (EditText) findViewById(R.id.EditTextPassphraseAgain);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mCreatePassPhrase = extras.getBoolean(extra.CREATE_PASS_PHRASE);
            mChangePassPhrase = extras.getBoolean(extra.CHANGE_PASS_PHRASE);
        }

        mTextViewVersion.setText(ConfigData.getVersionName(getApplicationContext()));

        // find all user accounts
        mUsers = new ArrayList<UserData>();
        int userTotal = 1;
        // future features may be able to utilize multple users
        for (int userNumber = 0; userNumber < userTotal; userNumber++) {
            String name = ConfigData.loadPrefContactName(getApplicationContext(), userNumber);
            long date = ConfigData.loadPrefKeyDate(getApplicationContext(), userNumber);
            mUsers.add(new UserData(name, date,
                    ConfigData.loadPrefUser(getApplicationContext()) == userNumber));
        }

        if (mCreatePassPhrase) {
            mEditTextPassNext.setVisibility(View.VISIBLE);
            mEditTextPassNext.setHint(R.string.label_PassHintCreate);
            mEditTextPassDone.setHint(R.string.label_PassHintRepeat);
        } else if (mChangePassPhrase) {
            mEditTextPassNext.setVisibility(View.VISIBLE);
            mEditTextPassNext.setHint(R.string.label_PassHintChange);
            mEditTextPassDone.setHint(R.string.label_PassHintRepeat);
        } else {
            mEditTextPassNext.setVisibility(View.GONE);
            mEditTextPassDone.setHint(R.string.label_PassHintEnter);

            mHandler = new Handler();
            mHandler.removeCallbacks(checkBackoffRelease);
            mHandler.post(checkBackoffRelease);
        }

        mButtonOk.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // if soft input open, close it...
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mEditTextPassDone.getWindowToken(), 0);

                doValidatePassphrase();
            }
        });

        mButtonHelp.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // open menu
                PassPhraseActivity.this.openOptionsMenu();
            }
        });

        mEditTextPassDone.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doValidatePassphrase();
                    return true;
                }
                return false;
            }
        });

        mUserAdapter = new UserAdapter(this, this, mUsers);
        mSpinnerUser.setAdapter(mUserAdapter);

        // make sure user selection is valid
        int userNumber = ConfigData.loadPrefUser(getApplicationContext());
        if (userNumber >= userTotal) {
            mUserNumber = 0; // back to default
            ConfigData.savePrefUser(getApplicationContext(), mUserNumber);
        } else {
            mUserNumber = userNumber;
        }

        mSpinnerUser.setSelection(mUserNumber);
        mSpinnerUser.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long i) {
                doUpdateUserSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // We don't need to worry about nothing being selected, since
                // Spinners don't allow
                // this.
            }
        });
    }

    private void doUpdateUserSelection(int position) {
        // Read current user selection
        if (mUserNumber != position) {
            mUserNumber = position;
            // save selected
            // if position of create new account, set new user choice and
            // exit...
            ConfigData.savePrefUser(getApplicationContext(), mUserNumber);
            setResult(RESULT_OK);
            finish();
        }
        mUserNumber = ConfigData.loadPrefUser(getApplicationContext());

        // Update the user spinner
        mUserAdapter.notifyDataSetChanged();
    }

    private void doValidatePassphrase() {
        String passPhrase2 = "" + mEditTextPassDone.getText();
        if (mChangePassPhrase || mCreatePassPhrase) {
            String passPhrase1 = "" + mEditTextPassNext.getText();
            if (!passPhrase1.equals(passPhrase2)) {
                showNote(R.string.error_passPhrasesDoNotMatch);
                return;
            } else if (passPhrase2.length() < ConfigData.MIN_PASSLEN) {
                showNote(String.format(getString(R.string.error_minPassphraseRequire),
                        ConfigData.MIN_PASSLEN));
                return;
            } else if (passPhrase2.equals("")) {
                showNote(R.string.error_noPassPhrase);
                return;
            }
        }
        Intent data = new Intent().putExtra(extra.PASS_PHRASE, passPhrase2);
        setResult(RESULT_OK, data);
        finish();
    }

    private Runnable checkBackoffRelease = new Runnable() {

        @Override
        public void run() {

            long nextPassAttemptDate = ConfigData
                    .loadPrefnextPassAttemptDate(PassPhraseActivity.this);
            long now = new Date().getTime();
            boolean enabled = (nextPassAttemptDate == 0 || now > (nextPassAttemptDate - 1000));
            if (!enabled) {
                String waitTime = DateUtils.getRelativeTimeSpanString(nextPassAttemptDate, now,
                        DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString();

                mEditTextPassDone.clearComposingText();
                mEditTextPassDone.setText("");
                mEditTextPassDone.setHint(getString(R.string.label_PassHintBackoff) + " "
                        + waitTime);
            } else {
                if (mCreatePassPhrase) {
                    mEditTextPassDone.setHint(R.string.label_PassHintRepeat);
                } else if (mChangePassPhrase) {
                    mEditTextPassNext.setVisibility(View.VISIBLE);
                    mEditTextPassDone.setHint(R.string.label_PassHintRepeat);
                } else {
                    mEditTextPassDone.setHint(R.string.label_PassHintEnter);
                }
            }
            mEditTextPassNext.setEnabled(enabled);
            mEditTextPassDone.setEnabled(enabled);
            mButtonOk.setEnabled(enabled);

            mHandler.postDelayed(this, mMsPollInterval);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        menu.add(0, MENU_HELP, 0, R.string.menu_Help).setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_FEEDBACK, 0, R.string.menu_sendFeedback).setIcon(
                android.R.drawable.ic_menu_send);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HELP:
                showHelp(getString(R.string.title_passphrase), getString(R.string.help_passphrase));
                return true;
            case MENU_FEEDBACK:
                SafeSlinger.getApplication().showFeedbackEmail(PassPhraseActivity.this);
                return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(PassPhraseActivity.this, args).create();
        }
        return super.onCreateDialog(id);
    }
}
