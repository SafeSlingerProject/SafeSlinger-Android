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

package edu.cmu.cylab.starslinger.view;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;

public final class FindContactActivity extends BaseActivity {

    private EditText mEditTextName;
    private Button mButtonDone;
    private static String mSelectedName = null;
    private EditText mEditTextPassNext;
    private EditText mEditTextPassDone;
    private TextView mTextViewLicensePrivacy;
    private Spinner mSpinnerLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Safeslinger);
        super.onCreate(savedInstanceState);
        SafeSlinger.setPassphraseOpen(true);

        generateView();
    }

    public void generateView() {
        setContentView(R.layout.contact_adder);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.app_name);
        bar.setSubtitle(R.string.title_find);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Obtain handles to UI objects
        mEditTextName = (EditText) findViewById(R.id.contactNameEditText);
        mButtonDone = (Button) findViewById(R.id.contactDoneButton);
        mEditTextPassNext = (EditText) findViewById(R.id.EditTextPassphrase);
        mEditTextPassDone = (EditText) findViewById(R.id.EditTextPassphraseAgain);
        mTextViewLicensePrivacy = (TextView) findViewById(R.id.textViewLicensePrivacy);
        mSpinnerLanguage = (Spinner) findViewById(R.id.spinnerLanguage);

        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final ArrayList<String> codes = SafeSlinger.getApplication().getListLanguages(true);
        mSpinnerLanguage.setPrompt(getText(R.string.title_language));
        mSpinnerLanguage.setAdapter(adapter);
        ArrayList<String> all = SafeSlinger.getApplication().getListLanguages(false);
        for (String l : all) {
            adapter.add(l);
        }
        mSpinnerLanguage.setSelection(codes.indexOf(SafeSlingerPrefs.getLanguage()));

        // read defaults and set them
        mSelectedName = SafeSlingerPrefs.getContactName();

        // see if name is there...
        if (mSelectedName != null) {
            mEditTextName.setText(mSelectedName);
        }

        mEditTextPassNext.setVisibility(View.VISIBLE);
        mEditTextPassNext.setHint(R.string.label_PassHintCreate);
        mEditTextPassDone.setHint(R.string.label_PassHintRepeat);

        // enable hyperlinks
        mTextViewLicensePrivacy.setText(Html.fromHtml("<a href=\"" + SafeSlingerConfig.EULA_URL
                + "\">" + getText(R.string.menu_License) + "</a> / <a href=\""
                + SafeSlingerConfig.PRIVACY_URL + "\">" + getText(R.string.menu_PrivacyPolicy)
                + "</a>"));
        mTextViewLicensePrivacy.setMovementMethod(LinkMovementMethod.getInstance());

        mSpinnerLanguage.setOnItemSelectedListener(new OnItemSelectedListener() {

            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long i) {

                if (!codes.get(position).equals(SafeSlingerPrefs.getLanguage())) {
                    SafeSlingerPrefs.setLanguage(codes.get(position));
                    SafeSlinger.getApplication().updateLanguage(codes.get(position));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        recreate();
                    } else {
                        generateView();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // We don't need to worry about nothing being selected
            }
        });

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SafeSlinger.setPassphraseOpen(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuItem item = menu.add(0, MENU_HELP, 0, R.string.menu_Help).setIcon(
                R.drawable.ic_action_help);
        MenuCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        menu.add(0, MENU_ABOUT, 0, R.string.menu_About).setIcon(
                android.R.drawable.ic_menu_info_details);
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
            case MENU_ABOUT:
                showAbout();
                return true;
            default:
                break;
        }
        return false;
    }

    private void onDoneButtonClicked() {
        String name = mEditTextName.getText().toString();

        if (SafeSlingerConfig.isNameValid(name)) {
            // save preferences...
            SafeSlingerPrefs.setContactName(name);
            doValidatePassphrase();
        } else {
            showNote(R.string.error_InvalidContactName);
        }

        // if soft input open, close it...
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditTextPassDone.getWindowToken(), 0);
    }

    private void doValidatePassphrase() {
        String passPhrase2 = "" + mEditTextPassDone.getText();
        String passPhrase1 = "" + mEditTextPassNext.getText();
        if (!passPhrase1.equals(passPhrase2)) {
            showNote(R.string.error_passPhrasesDoNotMatch);
            return;
        } else if (passPhrase2.length() < SafeSlingerConfig.MIN_PASSLEN) {
            showNote(String.format(getString(R.string.error_minPassphraseRequire),
                    SafeSlingerConfig.MIN_PASSLEN));
            return;
        } else if (passPhrase2.equals("")) {
            showNote(R.string.error_noPassPhrase);
            return;
        }

        Intent data = new Intent();
        data.putExtra(extra.PASS_PHRASE_NEW, passPhrase2);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(FindContactActivity.this, args).create();
            case DIALOG_ABOUT:
                return xshowAbout(FindContactActivity.this).create();
            default:
                break;
        }
        return super.onCreateDialog(id);
    }
}
