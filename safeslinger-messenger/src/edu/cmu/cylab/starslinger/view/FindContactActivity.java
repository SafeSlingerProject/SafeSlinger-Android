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

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;

public final class FindContactActivity extends BaseActivity {

    private EditText mEditTextName;
    private Button mButtonDone;
    private static String mSelectedName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_SafeSlinger);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contact_adder);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.app_name);
        bar.setSubtitle(R.string.title_find);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Obtain handles to UI objects
        mEditTextName = (EditText) findViewById(R.id.contactNameEditText);
        mButtonDone = (Button) findViewById(R.id.contactDoneButton);

        // read defaults and set them
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mSelectedName = extras.getString(extra.NAME);
        }

        // see if name is there...
        if (mSelectedName != null) {
            mEditTextName.setText(mSelectedName);
        }

        mEditTextName.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mSelectedName = mEditTextName.getText().toString();
            }
        });

        mButtonDone.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onDoneButtonClicked();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuItem item = menu.add(0, MENU_HELP, 0, R.string.menu_Help).setIcon(
                R.drawable.ic_action_help);
        MenuCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        menu.add(0, MENU_FEEDBACK, 0, R.string.menu_sendFeedback).setIcon(
                android.R.drawable.ic_menu_send);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HELP:
                showHelp(getString(R.string.title_find), getString(R.string.help_find));
                return true;
            case MENU_FEEDBACK:
                SafeSlinger.getApplication().showFeedbackEmail(FindContactActivity.this);
                return true;
        }
        return false;
    }

    private void onDoneButtonClicked() {
        String name = mEditTextName.getText().toString();

        if (SafeSlingerConfig.isNameValid(name)) {
            // save preferences...
            SafeSlingerPrefs.setContactName(name);
            setResult(RESULT_OK);
            finish();
        } else {
            showNote(R.string.error_InvalidContactName);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(FindContactActivity.this, args).create();
            default:
                break;
        }
        return super.onCreateDialog(id);
    }
}
