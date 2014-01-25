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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.ConfigData.extra;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.model.FileItem;
import edu.cmu.cylab.starslinger.model.FileNameComparator;

public class FileSaveActivity extends BaseActivity {
    private static final String TAG = ConfigData.LOG_TAG;
    private List<FileItem> mFileList = new ArrayList<FileItem>();
    private ListAdapter mAdapter;

    // recover these...
    private ArrayList<String> mDirs = new ArrayList<String>();
    private boolean mFirstLvl = true;
    private File mPath;
    private String mChosenFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mPath = new File(ConfigData.loadPrefFileManagerRootDir(getApplicationContext()));

        if (savedInstanceState != null) {
            mDirs = savedInstanceState.getStringArrayList(extra.DIRS);
            mPath = new File(savedInstanceState.getString(extra.FPATH));
            mFirstLvl = savedInstanceState.getBoolean(extra.FIRSTLVL);
            mChosenFile = savedInstanceState.getString(extra.FNAME);
            loadFileList();
        } else {
            loadFileList();
            showFileLoader();
        }
    }

    private void loadFileList() {
        try {
            mPath.mkdirs();
        } catch (SecurityException e) {
            MyLog.e(TAG, e.getMessage());
        }

        // Checks whether path exists
        if (mPath.exists()) {
            FilenameFilter filter = new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    // Filters based on whether file is a directory or not
                    return (sel.isDirectory()) && !sel.isHidden();
                }
            };

            String[] fList = mPath.list(filter);
            mFileList = new ArrayList<FileItem>();

            for (int i = 0; i < fList.length; i++) {
                mFileList.add(new FileItem(fList[i], R.drawable.ic_menu_directory));
            }

            Collections.sort(mFileList, new FileNameComparator());

            // Save file option added by default
            mFileList.add(0, new FileItem(getString(R.string.choice_SaveDirectory),
                    R.drawable.ic_menu_save_file));

            if (!mFirstLvl) {
                mFileList.add(0, new FileItem(getString(R.string.choice_Up),
                        R.drawable.ic_menu_directory_up));
            }
        } else {
            MyLog.e(TAG, "Path does not exist.");
        }

        mAdapter = new ArrayAdapter<FileItem>(this, android.R.layout.select_dialog_item,
                android.R.id.text1, mFileList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // creates view
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);

                // put the image on the text view
                textView.setCompoundDrawablesWithIntrinsicBounds(mFileList.get(position).icon, 0,
                        0, 0);

                // add margin between image and text (support various screen
                // densities)
                int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                textView.setCompoundDrawablePadding(dp5);

                return view;
            }
        };
    }

    private void showFileLoader() {
        MyLog.d(TAG, mPath.getAbsolutePath());
        if (!isFinishing()) {
            removeDialog(DIALOG_LOAD_FILE);
            showDialog(DIALOG_LOAD_FILE);
        }
    }

    private AlertDialog.Builder xshowFileLoader(Activity act) {
        AlertDialog.Builder ad = new Builder(act);
        ad.setTitle(R.string.title_ChooseDirectory);
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
        ad.setAdapter(mAdapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                mChosenFile = mFileList.get(which).file;
                File sel = new File(mPath + File.separator + mChosenFile);
                if (sel.isDirectory()) {
                    mFirstLvl = false;

                    // Adds chosen directory to list
                    mDirs.add(mChosenFile);
                    mFileList = null;
                    mPath = new File(sel + "");

                    loadFileList();
                    showFileLoader();
                }

                // Checks if 'up' was clicked
                else if (mChosenFile.equalsIgnoreCase(getString(R.string.choice_Up))
                        && !sel.exists()) {

                    // present directory removed from list
                    String s = mDirs.remove(mDirs.size() - 1);

                    // path modified to exclude present directory
                    mPath = new File(mPath.toString().substring(0, mPath.toString().lastIndexOf(s)));
                    mFileList = null;

                    // if there are no more directories in the list,
                    // then
                    // its the first level
                    if (mDirs.isEmpty()) {
                        mFirstLvl = true;
                    }

                    loadFileList();
                    showFileLoader();
                }
                // Chosen directory picked
                else {
                    // Chosen directory for default saving
                    doDirectorySelected();
                }
            }

        });
        return ad;
    }

    private void doDirectorySelected() {
        String chosenDirectory = "";
        File root = new File(ConfigData.loadPrefFileManagerRootDir(getApplicationContext()));
        if (mDirs.size() != 0) {
            chosenDirectory = mDirs.get(mDirs.size() - 1);
        }
        MyLog.d(TAG, "Chosen Directory=" + root.getAbsolutePath() + File.separator
                + chosenDirectory);

        finishWithResult(root + File.separator + chosenDirectory, null);
    }

    protected void showFileSaveAs() {
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        if (!isFinishing()) {
            removeDialog(DIALOG_TEXT_ENTRY);
            showDialog(DIALOG_TEXT_ENTRY, args);
        }
    }

    private AlertDialog.Builder xshowFileSaveAs(Activity act, Bundle args) {
        String fileName = args.getString(extra.FNAME);
        LayoutInflater factory = LayoutInflater.from(this);
        View textEntryView = factory.inflate(R.layout.filesave, null);

        final EditText et = (EditText) textEntryView.findViewById(R.id.saveFileName);
        et.setText(fileName);

        AlertDialog.Builder ad = new Builder(act);
        ad.setTitle(R.string.title_SaveFileAs);
        ad.setView(textEntryView);
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
        ad.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();

                showFileLoader();
            }
        });
        ad.setPositiveButton(R.string.btn_Save, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                String chosenDirectory = "";
                File root = new File(ConfigData.loadPrefFileManagerRootDir(getApplicationContext()));
                if (mDirs.size() != 0) {
                    chosenDirectory = mDirs.get(mDirs.size() - 1);
                }
                MyLog.d(TAG, "Chosen Directory=" + root.getAbsolutePath() + File.separator
                        + chosenDirectory);
                MyLog.d(TAG, "File Name=" + "" + et.getText());

                finishWithResult(root + File.separator + chosenDirectory, et.getText().toString());
            }
        });
        return ad;
    }

    private void finishWithResult(String filePath, String fileName) {
        Intent result = new Intent();
        Bundle bd = new Bundle();
        if (!TextUtils.isEmpty(filePath)) {
            bd.putString(extra.FPATH, filePath);
        }
        if (!TextUtils.isEmpty(fileName)) {
            bd.putString(extra.FNAME, fileName);
        }
        result.putExtras(bd);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save
        outState.putStringArrayList(extra.DIRS, mDirs);
        outState.putString(extra.FPATH, mPath.getAbsolutePath());
        outState.putBoolean(extra.FIRSTLVL, mFirstLvl);
        outState.putString(extra.FNAME, mChosenFile);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_LOAD_FILE:
                return xshowFileLoader(FileSaveActivity.this).create();
            case DIALOG_TEXT_ENTRY:
                return xshowFileSaveAs(FileSaveActivity.this, args).create();
        }
        return super.onCreateDialog(id);
    }
}
