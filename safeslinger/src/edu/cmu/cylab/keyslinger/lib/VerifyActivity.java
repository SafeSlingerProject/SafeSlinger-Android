
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

import java.security.SecureRandom;
import java.util.Locale;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;

import edu.cmu.cylab.keyslinger.lib.KsConfig.extra;
import edu.cmu.cylab.starslinger.R;

public class VerifyActivity extends ContactActivity {

    private static final int MENU_HELP = 1;
    public static final int RESULT_DECOYWORDLIST = 30;
    private int mCorrectButton;
    private TextView mTextViewCenter;
    private Button mButtonSame;
    private Button mButtonDiffer;
    private RadioButton mRadioPrimary1;
    private RadioButton mRadioPrimary2;
    private RadioButton mRadioPrimary3;
    private TextView mTextViewSecondary1;
    private TextView mTextViewSecondary2;
    private TextView mTextViewSecondary3;

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
                showHelp(getString(R.string.title_verify), getString(R.string.help_verify));
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
        // bar.setSubtitle(R.string.title_verify);
        bar.setSubtitle(R.string.title_verify);

        int bytesOfHash = 3;
        mCorrectButton = 0;

        // load from controller
        byte[] flagHash = null;
        byte[] decoyHash1 = null;
        byte[] decoyHash2 = null;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            flagHash = extras.getByteArray(extra.FLAG_HASH);
            decoyHash1 = extras.getByteArray(extra.DECOY1_HASH);
            decoyHash2 = extras.getByteArray(extra.DECOY2_HASH);
            mCorrectButton = extras.getInt(extra.RANDOM_POS);
        }

        setContentView(R.layout.verifywords);

        mTextViewCenter = (TextView) findViewById(R.id.VerifyTextViewCenter);
        mButtonSame = (Button) findViewById(R.id.VerifyButtonMatch);
        mButtonDiffer = (Button) findViewById(R.id.VerifyButtonDiffer);

        boolean english = Locale.getDefault().getLanguage().equals("en");

        if (english) {
            mRadioPrimary1 = (RadioButton) findViewById(R.id.VerifyTextViewEnglishOne);
            mRadioPrimary2 = (RadioButton) findViewById(R.id.VerifyTextViewEnglishTwo);
            mRadioPrimary3 = (RadioButton) findViewById(R.id.VerifyTextViewEnglishThree);
            mTextViewSecondary1 = (TextView) findViewById(R.id.VerifyTextViewNumbersOne);
            mTextViewSecondary2 = (TextView) findViewById(R.id.VerifyTextViewNumbersTwo);
            mTextViewSecondary3 = (TextView) findViewById(R.id.VerifyTextViewNumbersThree);
        } else {
            mRadioPrimary1 = (RadioButton) findViewById(R.id.VerifyTextViewNumbersOne);
            mRadioPrimary2 = (RadioButton) findViewById(R.id.VerifyTextViewNumbersTwo);
            mRadioPrimary3 = (RadioButton) findViewById(R.id.VerifyTextViewNumbersThree);
            mTextViewSecondary1 = (TextView) findViewById(R.id.VerifyTextViewEnglishOne);
            mTextViewSecondary2 = (TextView) findViewById(R.id.VerifyTextViewEnglishTwo);
            mTextViewSecondary3 = (TextView) findViewById(R.id.VerifyTextViewEnglishThree);
        }

        mTextViewCenter.setText(R.string.label_VerifyInstruct);

        // set visual hash
        byte[][] hashes = new byte[3][];
        SecureRandom rand = new SecureRandom();
        boolean topDecoy = rand.nextBoolean();
        boolean first = false;

        for (int i = 0; i < 3; i++) {
            // drop all hashes into one of three places
            if (i == mCorrectButton) {
                // correct list (random)
                hashes[i] = flagHash;
            } else {
                if (first) {
                    // decoy list 1
                    hashes[i] = topDecoy ? decoyHash1 : decoyHash2;
                } else {
                    // decoy list 2
                    hashes[i] = topDecoy ? decoyHash2 : decoyHash1;
                    first = true;
                }
            }
        }

        // update buttons
        if (english) {
            mRadioPrimary1.setText(getWordList(hashes[0], bytesOfHash));
            mTextViewSecondary1.setText(getNumbersList(hashes[0], bytesOfHash));
            mRadioPrimary2.setText(getWordList(hashes[1], bytesOfHash));
            mTextViewSecondary2.setText(getNumbersList(hashes[1], bytesOfHash));
            mRadioPrimary3.setText(getWordList(hashes[2], bytesOfHash));
            mTextViewSecondary3.setText(getNumbersList(hashes[2], bytesOfHash));
        } else {
            mRadioPrimary1.setText(getNumbersList(hashes[0], bytesOfHash));
            mTextViewSecondary1.setText(getWordList(hashes[0], bytesOfHash));
            mRadioPrimary2.setText(getNumbersList(hashes[1], bytesOfHash));
            mTextViewSecondary2.setText(getWordList(hashes[1], bytesOfHash));
            mRadioPrimary3.setText(getNumbersList(hashes[2], bytesOfHash));
            mTextViewSecondary3.setText(getWordList(hashes[2], bytesOfHash));
        }

        // set button handlers
        mButtonSame.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (mRadioPrimary1.isChecked()) {
                    if (mCorrectButton == 0) {
                        setResultForParent(RESULT_OK);
                    } else {
                        setResultForParent(RESULT_DECOYWORDLIST);
                    }
                    finish();
                } else if (mRadioPrimary2.isChecked()) {
                    if (mCorrectButton == 1) {
                        setResultForParent(RESULT_OK);
                    } else {
                        setResultForParent(RESULT_DECOYWORDLIST);
                    }
                    finish();
                } else if (mRadioPrimary3.isChecked()) {
                    if (mCorrectButton == 2) {
                        setResultForParent(RESULT_OK);
                    } else {
                        setResultForParent(RESULT_DECOYWORDLIST);
                    }
                    finish();
                } else {
                    showNote(R.string.error_NoWordListSelected);
                }
            }
        });

        mButtonDiffer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                setResultForParent(RESULT_CANCELED);
                finish();
            }
        });
    }

    private String getWordList(byte[] hash, int length) {
        StringBuilder hashList = new StringBuilder();
        for (int i = 0; i < length; i++) {
            hashList.append(WordList.getWord(hash[i], (i % 2 == 0)));
            hashList.append(" ");
        }
        return hashList.toString().trim();
    }

    private String getNumbersList(byte[] hash, int length) {
        StringBuilder hashList = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = WordList.btoi(hash[i]);
            // even = 1-256, odd = 257-512
            hashList.append(((i % 2 == 0) ? number : (number + 256)) + 1);
            hashList.append("  ");
        }
        return hashList.toString().trim();
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(VerifyActivity.this, args).create();
        }
        return super.onCreateDialog(id);
    }
}
