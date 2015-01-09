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
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import edu.cmu.cylab.starslinger.MyLog;
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

public class ComposeFragment extends Fragment {
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
    private TextView mTextViewSenderName;
    private TextView mTextViewRecipKey;
    private TextView mTextViewSenderKey;
    private TextView mTextViewFile;
    private TextView mTextViewFileSize;
    private ImageView mImageViewRecipPhoto;
    private ImageView mImageViewSenderPhoto;
    private ImageView mImageViewFile;
    private static EditText mEditTextMessage;
    private Button mButtonFile;
    private Button mButtonSend;
    private Button mButtonSender;
    private Button mButtonRecip;
    private byte[] mThumb = null;
    private static RecipientRow mRecip;
    private long mRowIdRecipient = -1;
    private static OnComposeResultListener mResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateValues(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vFrag = inflater.inflate(R.layout.send, container, false);
        mTextViewSenderName = (TextView) vFrag.findViewById(R.id.SendTextViewSenderName);
        mTextViewSenderKey = (TextView) vFrag.findViewById(R.id.SendTextViewSenderKey);
        mImageViewSenderPhoto = (ImageView) vFrag.findViewById(R.id.SendImageViewSenderPhoto);
        mTextViewRecipName = (TextView) vFrag.findViewById(R.id.SendTextViewRecipName);
        mTextViewRecipKey = (TextView) vFrag.findViewById(R.id.SendTextViewRecipKey);
        mImageViewRecipPhoto = (ImageView) vFrag.findViewById(R.id.SendImageViewRecipPhoto);
        mTextViewFile = (TextView) vFrag.findViewById(R.id.SendTextViewFile);
        mTextViewFileSize = (TextView) vFrag.findViewById(R.id.SendTextViewFileSize);
        mButtonFile = (Button) vFrag.findViewById(R.id.SendButtonFile);
        mButtonSend = (Button) vFrag.findViewById(R.id.SendButtonSend);
        mImageViewFile = (ImageView) vFrag.findViewById(R.id.SendImageViewFile);
        mEditTextMessage = (EditText) vFrag.findViewById(R.id.SendEditTextMessage);
        mButtonSender = (Button) vFrag.findViewById(R.id.SendButtonSender);
        mButtonRecip = (Button) vFrag.findViewById(R.id.SendButtonRecipient);

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

        OnClickListener clickSender = new OnClickListener() {

            @Override
            public void onClick(View v) {
                doChangeUser();
            }
        };
        mButtonSender.setOnClickListener(clickSender);

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

        return vFrag;
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
        if (mTextViewSenderName == null) {
            return;
        }

        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this.getActivity());
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

        // sender
        mImageViewSenderPhoto.setImageResource(0);
        String name = SafeSlingerPrefs.getContactName();
        if (!TextUtils.isEmpty(name)) {
            byte[] photo = ((BaseActivity) this.getActivity()).getContactPhoto(contactLookupKey);
            drawUserData(R.string.label_SendFrom, name, photo, mTextViewSenderName,
                    mTextViewSenderKey, mImageViewSenderPhoto, myKeyId, myKeyDate);
        } else {
            mTextViewRecipName.setTextColor(Color.GRAY);
            mTextViewSenderName.setText(R.string.label_UserName);
            mTextViewSenderKey.setText("");
            mImageViewSenderPhoto.setImageResource(R.drawable.ic_silhouette);
        }

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
            mTextViewFile.setTextColor(Color.BLACK);
        } else {
            mTextViewFile.setTextColor(Color.GRAY);
            mTextViewFile.setText(R.string.btn_SelectFile);
            mImageViewFile.setImageResource(R.drawable.ic_attachment_select);
            mTextViewFileSize.setText("");
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
        sendResultToHost(RESULT_SAVE, intent.getExtras());
    }

    private void doSend(String text) {
        Intent intent = new Intent();
        intent.putExtra(extra.TEXT_MESSAGE, text.trim());
        if (mRecip != null) {
            intent.putExtra(extra.RECIPIENT_ROW_ID, mRecip.getRowId());
        }
        // remove local version after sending
        mEditTextMessage.setTextKeepState("");
        sendResultToHost(RESULT_SEND, intent.getExtras());
    }

    private void doClickRecipient() {
        Intent intent = new Intent();
        sendResultToHost(RESULT_RECIPSEL, intent.getExtras());
    }

    private static void doFileRemove() {
        Intent intent = new Intent();
        sendResultToHost(RESULT_FILEREMOVE, intent.getExtras());
    }

    private static void doFileSelect() {
        Intent intent = new Intent();
        sendResultToHost(RESULT_FILESEL, intent.getExtras());
    }

    private static void doChangeUser() {
        Intent intent = new Intent();
        sendResultToHost(RESULT_USEROPTIONS, intent.getExtras());
    }

    private void showChangeFileOptions() {
        DialogFragment newFragment = ComposeAlertDialogFragment
                .newInstance(BaseActivity.DIALOG_FILEOPTIONS);
        newFragment.show(getFragmentManager(), "dialog");
    }

    public static AlertDialog.Builder xshowChangeFileOptions(Activity act) {
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
            mImageViewFile.setImageDrawable(tn);
        } else {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setType(mime);
            PackageManager pm = getActivity().getPackageManager();
            List<ResolveInfo> lract = pm.queryIntentActivities(viewIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);

            boolean resolved = false;

            for (ResolveInfo ri : lract) {
                if (!resolved) {
                    try {
                        Drawable icon = pm.getApplicationIcon(ri.activityInfo.packageName);
                        mImageViewFile.setImageDrawable(icon);
                        resolved = true;
                    } catch (NameNotFoundException e) {
                        mImageViewFile.setImageDrawable(getResources().getDrawable(
                                R.drawable.ic_menu_file));
                    }
                }
            }

        }
    }

    private void drawFileData() {
        mTextViewFile.setText(mFilePath);
        mTextViewFileSize
                .setText(" ("
                        + SSUtil.getSizeString(this.getActivity().getApplicationContext(),
                                mFileSize) + ")");
    }

    private void drawUserData(int dirId, String name, byte[] photo, TextView textViewUserName,
            TextView textViewKey, ImageView imageViewPhoto, String keyId, long keyDate) {

        StringBuilder detailStr = new StringBuilder();
        if (!CryptTools.isNullKeyId(keyId)) {
            if (keyDate > 0) {
                detailStr
                        .append(getString(R.string.label_Key))
                        .append(" ")
                        .append(DateUtils.getRelativeTimeSpanString(getActivity()
                                .getApplicationContext(), keyDate));
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

    public interface OnComposeResultListener {
        public void onComposeResultListener(Bundle args);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mResult = (OnComposeResultListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + OnComposeResultListener.class.getSimpleName());
        }
    }

    private static void sendResultToHost(int resultCode, Bundle args) {
        if (args == null) {
            args = new Bundle();
        }
        args.putInt(extra.RESULT_CODE, resultCode);
        mResult.onComposeResultListener(args);
    }

    public static class ComposeAlertDialogFragment extends DialogFragment {

        public static ComposeAlertDialogFragment newInstance(int id) {
            return newInstance(id, new Bundle());
        }

        public static ComposeAlertDialogFragment newInstance(int id, Bundle args) {
            ComposeAlertDialogFragment frag = new ComposeAlertDialogFragment();
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
                case BaseActivity.DIALOG_FILEOPTIONS:
                    return ComposeFragment.xshowChangeFileOptions(getActivity()).create();
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
        DialogFragment newFragment = ComposeAlertDialogFragment.newInstance(
                BaseActivity.DIALOG_HELP, args);
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

    private boolean isSendableText() {
        return TextUtils.getTrimmedLength(mEditTextMessage.getText()) != 0;
    }
}
