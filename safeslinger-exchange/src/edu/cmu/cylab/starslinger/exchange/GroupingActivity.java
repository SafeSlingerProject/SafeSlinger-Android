
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

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig.extra;

public class GroupingActivity extends BaseActivity {

    private static final int MENU_HELP = 1;
    private static final String PREF_SHOW_HELP = "prefAutoShowHelp";
    private Button mButtonOk;
    private TextView mTextViewInstruct;
    private TextView mTextViewUserId;
    private EditText mEditTextPrompt;
    private TextView mTextViewCompareNDevices;
    private int mNumUsers;
    private int mUserId;
    private InputMethodManager mInputMgr;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem helpItem = menu.add(0, MENU_HELP, 0, "Help").setIcon(
                android.R.drawable.ic_menu_help);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            helpItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HELP:
                showHelp();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            setTheme(android.R.style.Theme_Holo_Light_DarkActionBar);
        } else {
            setTheme(android.R.style.Theme_Light);
        }
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar bar = getActionBar();
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            bar.setTitle(R.string.lib_name);
            bar.setSubtitle(R.string.title_userid);
        } else {
            setTitle(getString(R.string.lib_name) + ": " + getString(R.string.title_userid));
        }

        setContentView(R.layout.sse__promptdialog);

        mButtonOk = (Button) findViewById(R.id.PromptButtonOK);
        mTextViewCompareNDevices = (TextView) findViewById(R.id.TextViewCompareNDevices);
        mTextViewInstruct = (TextView) findViewById(R.id.PromptTextViewInstruct);
        mTextViewUserId = (TextView) findViewById(R.id.PromptTextViewUserId);
        mEditTextPrompt = (EditText) findViewById(R.id.PromptEditText);

        Bundle extras = savedInstanceState != null ? savedInstanceState : getIntent().getExtras();
        if (extras != null) {
            mUserId = extras.getInt(extra.USER_ID, mUserId);
            mNumUsers = extras.getInt(extra.NUM_USERS);
        }

        // check for required Bundle values
        if (mUserId <= 0) {
            finishInvalidBundle("VerifyActivity mUserId=" + mUserId);
            return;
        }
        if (mNumUsers < 2) {
            finishInvalidBundle("VerifyActivity mNumUsers=" + mNumUsers);
            return;
        }

        mTextViewUserId.setText(String.valueOf(mUserId));

        mTextViewCompareNDevices.setText(String.format(
                getString(R.string.label_CompareScreensNDevices), mNumUsers));
        mTextViewInstruct.setText(R.string.label_PromptInstruct);
        mEditTextPrompt.setText("");

        mButtonOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent data = new Intent().putExtra(extra.GROUP_ID, mEditTextPrompt.getText()
                        .toString());
                if (mInputMgr != null) {
                    mInputMgr.hideSoftInputFromWindow(mEditTextPrompt.getWindowToken(), 0);
                }
                setResultForParent(RESULT_OK, data);
                finish();
            }
        });

        mEditTextPrompt.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Intent data = new Intent().putExtra(extra.GROUP_ID, mEditTextPrompt.getText()
                            .toString());
                    if (mInputMgr != null) {
                        mInputMgr.hideSoftInputFromWindow(mEditTextPrompt.getWindowToken(), 0);
                    }
                    setResultForParent(RESULT_OK, data);
                    finish();
                    return true;
                }
                return false;
            }

        });

        mInputMgr = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        mInputMgr.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);

        // show help automatically only for first time installers
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        if (sharedPref.getBoolean(PREF_SHOW_HELP, true)) {
            // show help, turn off for next time
            showHelp();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(PREF_SHOW_HELP, false);
            editor.commit();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(GroupingActivity.this, args).create();
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mUserId > 0) {
            outState.putInt(extra.USER_ID, mUserId);
        }
        if (mNumUsers >= 2) {
            outState.putInt(extra.NUM_USERS, mNumUsers);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (mInputMgr != null) {
            mInputMgr.hideSoftInputFromWindow(mEditTextPrompt.getWindowToken(), 0);
        }
        super.onBackPressed();
    }

    private void showHelp() {
        showHelp(getString(R.string.title_userid), getString(R.string.help_userid));
    }
}
