
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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.app.ActionBar;

import edu.cmu.cylab.keyslinger.lib.KsConfig.extra;
import edu.cmu.cylab.starslinger.R;

public class PromptActivity extends ContactActivity {

    private static final int MENU_HELP = 1;
    private Button mButtonOk;
    private TextView mTextViewInstruct;
    private TextView mTextViewUserId;
    private EditText mEditTextPrompt;

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {

        int showAsActionAlways = com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

        menu.add(0, MENU_HELP, 0, R.string.menu_Help).setIcon(R.drawable.ic_action_help)
                .setShowAsAction(showAsActionAlways);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HELP:
                showHelp(getString(R.string.title_userid), getString(R.string.help_userid));
                return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_SafeSlinger);
        super.onCreate(savedInstanceState);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.app_name);
        bar.setSubtitle(R.string.title_userid);

        setContentView(R.layout.promptdialog);

        mButtonOk = (Button) findViewById(R.id.PromptButtonOK);
        mTextViewInstruct = (TextView) findViewById(R.id.PromptTextViewInstruct);
        mTextViewUserId = (TextView) findViewById(R.id.PromptTextViewUserId);
        mEditTextPrompt = (EditText) findViewById(R.id.PromptEditText);

        String msg = "";
        int usrid = 0;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            msg = extras.getString(extra.MESSAGE);
            usrid = extras.getInt(extra.USER_ID, usrid);
        }
        if (usrid > 0) {
            mTextViewUserId.setText(String.valueOf(usrid));
        }
        mTextViewInstruct.setText(msg);
        mEditTextPrompt.setText("");

        mTextViewInstruct.setText(R.string.label_PromptInstruct);

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
                return xshowHelp(PromptActivity.this, args).create();
        }
        return super.onCreateDialog(id);
    }
}
