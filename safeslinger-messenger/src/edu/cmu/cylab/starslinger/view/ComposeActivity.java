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

import java.io.ByteArrayInputStream;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.BadTokenException;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.model.DraftData;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientRow;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class ComposeActivity extends BaseActivity {
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    public static final int RESULT_FILESEL = 20;
    public static final int RESULT_RECIPSEL = 22;
    public static final int RESULT_SEND = 24;
    public static final int RESULT_FILEREMOVE = 29;
    public static final int RESULT_RESTART = 32;
    public static final int RESULT_SAVE = 33;
    public static final int RESULT_USEROPTIONS = 200;

    private String mFilePath = null;
    private int mFileSize = 0;
    private String mText = null;
    private TextView mTextViewRecipName;
    private TextView mTextViewRecipKey;
    // private TextView mTextViewFile;
    // private TextView mTextViewFileSize;
    private ImageView mImageViewRecipPhoto;
    // private ImageView mImageViewFile;
    private static EditText mEditTextMessage;
    private ImageButton mButtonFile;
    private ImageButton mButtonSend;
    private Button mButtonRecip;
    private byte[] mThumb = null;
    private static RecipientRow mRecip;
    private long mRowIdRecipient = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Safeslinger);
        super.onCreate(savedInstanceState);

        // inject view
        setContentView(R.layout.send);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.app_name);
        bar.setSubtitle(R.string.title_PickRecipient);

        mTextViewRecipName = (TextView) findViewById(R.id.SendTextViewRecipName);
        mTextViewRecipKey = (TextView) findViewById(R.id.SendTextViewRecipKey);
        mImageViewRecipPhoto = (ImageView) findViewById(R.id.SendImageViewRecipPhoto);
        // mTextViewFile = (TextView) findViewById(R.id.SendTextViewFile);
        // mTextViewFileSize = (TextView)
        // findViewById(R.id.SendTextViewFileSize);
        mButtonFile = (ImageButton) findViewById(R.id.SendButtonFile);
        mButtonSend = (ImageButton) findViewById(R.id.SendButtonSend);
        // mImageViewFile = (ImageView) findViewById(R.id.SendImageViewFile);
        mEditTextMessage = (EditText) findViewById(R.id.SendEditTextMessage);
        mButtonRecip = (Button) findViewById(R.id.SendButtonRecipient);

        updateValues(savedInstanceState);

        OnClickListener clickFile = new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mFilePath)) {
                    doFileSelect();
                } else {
                    showChangeFileOptions();
                }
            }
        };
        mButtonFile.setOnClickListener(clickFile);

        OnClickListener clickRecip = new OnClickListener() {

            @Override
            public void onClick(View v) {
                doClickRecipient();
            }
        };
        mButtonRecip.setOnClickListener(clickRecip);

        mButtonSend.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (isSendableText() || !TextUtils.isEmpty(mFilePath)) {
                    doSend(mEditTextMessage.getText().toString());
                }
            }

        });

        mEditTextMessage.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (isSendableText() || !TextUtils.isEmpty(mFilePath)) {
                        doSend(mEditTextMessage.getText().toString());
                    }
                    return true;
                }
                return false;
            }
        });
    }

    public void updateValues(Bundle extras) {
        String contactLookupKey = SafeSlingerPrefs.getContactLookupKey();
        DraftData d = DraftData.INSTANCE;

        if (d.existsRecip()) {
            mRowIdRecipient = d.getRecipRowId();
        } else {
            mRowIdRecipient = -1;
        }
        mFilePath = d.getFileName();
        mFileSize = d.getFileSize();
        mText = d.getText();
        if (!TextUtils.isEmpty(d.getFileType()) && d.getFileType().contains("image")) {
            mThumb = SSUtil.makeThumbnail(SafeSlinger.getApplication().getApplicationContext(),
                    d.getFileData());
        } else {
            mThumb = null;
        }

        // make sure view is already inflated...
        if (mTextViewRecipName == null) {
            return;
        }

        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(getApplicationContext());
        mRecip = null;
        Cursor c = dbRecipient.fetchRecipient(mRowIdRecipient);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    mRecip = new RecipientRow(c);
                }
            } finally {
                c.close();
            }
        }

        // load key here!
        String myKeyId = SafeSlingerPrefs.getKeyIdString();
        long myKeyDate = SafeSlingerPrefs.getKeyDate();

        // recipient
        mImageViewRecipPhoto.setImageResource(0);
        if (mRecip != null) {
            drawUserData(R.string.label_SendTo, mRecip.getName(), mRecip.getPhoto(),
                    mTextViewRecipName, mTextViewRecipKey, mImageViewRecipPhoto, mRecip.getKeyid(),
                    mRecip.getKeydate());
            mTextViewRecipName.setTextColor(Color.BLACK);
        } else {
            mTextViewRecipName.setTextColor(Color.GRAY);
            mTextViewRecipName.setText(R.string.label_SelectRecip);
            mTextViewRecipKey.setText("");
            mImageViewRecipPhoto.setImageResource(R.drawable.ic_silhouette_select);
        }

        // file
        if (!(TextUtils.isEmpty(mFilePath))) {
            drawFileImage();
            drawFileData();
            // mTextViewFile.setTextColor(Color.BLACK);
        } else {
            // mTextViewFile.setTextColor(Color.GRAY);
            // mTextViewFile.setText(R.string.btn_SelectFile);
            // mImageViewFile.setImageResource(R.drawable.ic_attachment_select);
            mButtonFile.setImageResource(R.drawable.ic_attachment_select);
            // mTextViewFileSize.setText("");
        }

        // message
        if (!TextUtils.isEmpty(mText)) {
            mEditTextMessage.setTextKeepState(mText);
            mEditTextMessage.forceLayout();
        } else {
            mEditTextMessage.setTextKeepState("");
        }

    }

    private void doSave(String text) {
        Intent intent = new Intent();
        intent.putExtra(extra.TEXT_MESSAGE, text.trim());
        if (mRecip != null) {
            intent.putExtra(extra.RECIPIENT_ROW_ID, mRecip.getRowId());
        }
        setResult(RESULT_SAVE, intent);
        finish();
    }

    private void doSend(String text) {
        Intent intent = new Intent();
        intent.putExtra(extra.TEXT_MESSAGE, text.trim());
        if (mRecip != null) {
            intent.putExtra(extra.RECIPIENT_ROW_ID, mRecip.getRowId());
        }
        // remove local version after sending
        mEditTextMessage.setTextKeepState("");
        setResult(RESULT_SEND, intent);
        finish();
    }

    private void doClickRecipient() {
        setResult(RESULT_RECIPSEL);
        finish();
    }

    private void doFileRemove() {
        setResult(RESULT_FILEREMOVE);
        finish();
    }

    private void doFileSelect() {
        setResult(RESULT_FILESEL);
        finish();
    }

    private void doChangeUser() {
        setResult(RESULT_USEROPTIONS);
        finish();
    }

    private void showChangeFileOptions() {
        if (!isFinishing()) {
            try {
                removeDialog(DIALOG_FILEOPTIONS);
                showDialog(DIALOG_FILEOPTIONS);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    public AlertDialog.Builder xshowChangeFileOptions(Activity act) {
        final CharSequence[] items = new CharSequence[] {
                act.getText(R.string.menu_Remove), act.getText(R.string.menu_Change)
        };
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_FileOptions);
        ad.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
                switch (item) {
                    case 0: // remove
                        doFileRemove();
                        break;
                    case 1: // change
                        doFileSelect();
                        break;
                    default:
                        break;
                }
            }

        });
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    @SuppressWarnings("deprecation")
    private void drawFileImage() {
        String filenameArray[] = mFilePath.split("\\.");
        String extension = filenameArray[filenameArray.length - 1];
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        if (mThumb != null && mThumb.length > 0) {
            ByteArrayInputStream in = new ByteArrayInputStream(mThumb);
            BitmapDrawable tn = new BitmapDrawable(in);
            // mImageViewFile.setImageDrawable(tn);
            mButtonFile.setImageDrawable(tn);
        } else {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setType(mime);
            PackageManager pm = getPackageManager();
            List<ResolveInfo> lract = pm.queryIntentActivities(viewIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);

            boolean resolved = false;

            for (ResolveInfo ri : lract) {
                if (!resolved) {
                    try {
                        Drawable icon = pm.getApplicationIcon(ri.activityInfo.packageName);
                        // mImageViewFile.setImageDrawable(icon);
                        mButtonFile.setImageDrawable(icon);
                        resolved = true;
                    } catch (NameNotFoundException e) {
                        // mImageViewFile.setImageDrawable(getResources().getDrawable(
                        // R.drawable.ic_menu_file));
                        mButtonFile.setImageDrawable(getResources().getDrawable(
                                R.drawable.ic_menu_file));
                    }
                }
            }

        }
    }

    private void drawFileData() {
        // mTextViewFile.setText(mFilePath);
        // mTextViewFileSize.setText(" (" +
        // SSUtil.getSizeString(getApplicationContext(), mFileSize)
        // + ")");
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
        if (textViewUserName != null) {
            textViewUserName.setText(getString(dirId) + " " + name);
        }

        // draw keys
        if (textViewKey != null) {
            textViewKey.setText(detailStr);
        }

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
    public void onPause() {
        super.onPause();

        // save draft when view is lost
        doSave(mEditTextMessage.getText().toString());
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public void updateKeypad() {
        // if soft input open, close it...
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private boolean isSendableText() {
        return TextUtils.getTrimmedLength(mEditTextMessage.getText()) != 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            MenuItem iAdd = menu.add(0, MENU_CONTACTINVITE, 0, R.string.menu_SelectShareApp)
                    .setIcon(R.drawable.ic_action_add_person);

            MenuItemCompat.setShowAsAction(iAdd, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

            MenuItem iHelp = menu.add(0, MENU_HELP, 0, R.string.menu_Help).setIcon(
                    R.drawable.ic_action_help);
            MenuItemCompat.setShowAsAction(iHelp, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

            menu.add(0, MENU_CONTACTINVITE, 0, R.string.menu_SelectShareApp).setIcon(
                    R.drawable.ic_action_add_person);
            menu.add(0, MENU_FEEDBACK, 0, R.string.menu_sendFeedback).setIcon(
                    android.R.drawable.ic_menu_send);
        } else {
            MenuItem iAddMenuItem = menu
                    .add(0, MENU_CONTACTINVITE, 0, R.string.menu_SelectShareApp).setIcon(
                            R.drawable.ic_action_add_person);
            SpannableString spanString = new SpannableString(iAddMenuItem.getTitle().toString());
            // fix the color to white
            spanString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spanString.length(), 0);
            iAddMenuItem.setTitle(spanString);

            MenuItemCompat.setShowAsAction(iAddMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

            MenuItem iHelpmenuItem = menu.add(0, MENU_HELP, 0, R.string.menu_Help).setIcon(
                    R.drawable.ic_action_help);
            MenuItemCompat.setShowAsAction(iHelpmenuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

            MenuItem contactInviteMenuItem = menu.add(0, MENU_CONTACTINVITE, 0,
                    R.string.menu_SelectShareApp).setIcon(R.drawable.ic_action_add_person);
            spanString = new SpannableString(contactInviteMenuItem.getTitle().toString());
            // fix the color to white
            spanString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spanString.length(), 0);
            contactInviteMenuItem.setTitle(spanString);

            MenuItem feedbackItem = menu.add(0, MENU_FEEDBACK, 0, R.string.menu_sendFeedback)
                    .setIcon(android.R.drawable.ic_menu_send);
            spanString = new SpannableString(feedbackItem.getTitle().toString());
            // fix the color to white
            spanString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spanString.length(), 0);
            feedbackItem.setTitle(spanString);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HELP:
                showHelp(getString(R.string.title_PickRecipient),
                        getString(R.string.help_PickRecip));
                return true;
            case MENU_FEEDBACK:
                SafeSlinger.getApplication().showFeedbackEmail(ComposeActivity.this);
                return true;
            case MENU_CONTACTINVITE:
                showAddContactInvite();
                return true;
            default:
                break;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(ComposeActivity.this, args).create();
            case DIALOG_FILEOPTIONS:
                return xshowChangeFileOptions(ComposeActivity.this).create();
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

}
