
package edu.cmu.cylab.starslinger.exchange;

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

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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
    private Button mButtonOk;
    private TextView mTextViewInstruct;
    private TextView mTextViewUserId;
    private EditText mEditTextPrompt;
    private TextView mTextViewCompareNDevices;

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
                showHelp(getString(R.string.title_userid), getString(R.string.help_userid));
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

        int numUsers = 2;
        int usrid = 0;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usrid = extras.getInt(extra.USER_ID, usrid);
            numUsers = extras.getInt(extra.NUM_USERS);
        }
        if (usrid > 0) {
            mTextViewUserId.setText(String.valueOf(usrid));
        }

        mTextViewCompareNDevices.setText(String.format(
                getString(R.string.label_CompareScreensNDevices), numUsers));
        mTextViewInstruct.setText(R.string.label_PromptInstruct);
        mEditTextPrompt.setText("");

        mButtonOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent data = new Intent().putExtra(extra.GROUP_ID, mEditTextPrompt.getText()
                        .toString());
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
                    setResultForParent(RESULT_OK, data);
                    finish();
                    return true;
                }
                return false;
            }
        });

        InputMethodManager imm = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    @Override
    public void onDestroy() {
        InputMethodManager imm = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, 0);

        super.onDestroy();
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
}
