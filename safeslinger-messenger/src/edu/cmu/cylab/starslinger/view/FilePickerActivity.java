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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.model.FileItem;
import edu.cmu.cylab.starslinger.model.FileNameComparator;

public class FilePickerActivity extends BaseActivity {
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    private List<FileItem> mFileList = new ArrayList<FileItem>();
    private ListAdapter mAdapter;

    // recover these...
    private ArrayList<String> mDirs = new ArrayList<String>();
    private boolean mFirstLvl = true;
    private File mPath;
    private String mChosenFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat);
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mPath = new File(SafeSlingerPrefs.getFileManagerRootDir());

        if (savedInstanceState != null) {
            mDirs = savedInstanceState.getStringArrayList(extra.DIRS);
            mPath = new File(savedInstanceState.getString(extra.FPATH));
            mFirstLvl = savedInstanceState.getBoolean(extra.FIRSTLVL);
            mChosenFile = savedInstanceState.getString(extra.FNAME);
            loadFileList();
        } else {
            loadFileList();
            showFilePicker();
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
                    // Filters based on whether the file is hidden or not
                    return (sel.isFile() || sel.isDirectory()) && !sel.isHidden();
                }
            };

            String[] fList = mPath.list(filter);
            mFileList = new ArrayList<FileItem>();
            if (fList != null) {
                for (int i = 0; i < fList.length; i++) {
                    // Convert into file path
                    File sel = new File(mPath, fList[i]);

                    // Set drawables
                    if (sel.isDirectory()) {
                        mFileList.add(new FileItem(fList[i], R.drawable.ic_menu_directory));
                        MyLog.d(TAG, "DIRECTORY=" + mFileList.get(i).file);
                    } else {
                        mFileList.add(new FileItem(fList[i], R.drawable.ic_menu_file));
                        MyLog.d(TAG, "FILE=" + mFileList.get(i).file);
                    }
                }
            }
            Collections.sort(mFileList, new FileNameComparator());

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
                int avatar_size_list = (int) getResources().getDimension(R.dimen.avatar_size_list);
                Drawable d = getResources().getDrawable(mFileList.get(position).icon);
                d.setBounds(0, 0, avatar_size_list, avatar_size_list);
                textView.setCompoundDrawables(d, null, null, null);

                // add margin between image and text (support various screen
                // densities)
                textView.setCompoundDrawablePadding((int) getResources().getDimension(
                        R.dimen.size_5dp));

                return view;
            }
        };
    }

    private void showFilePicker() {
        MyLog.d(TAG, mPath.getAbsolutePath());
        if (!isFinishing()) {
            try {
                removeDialog(DIALOG_LOAD_FILE);
                showDialog(DIALOG_LOAD_FILE);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    private AlertDialog.Builder xshowFilePicker(Activity act) {
        AlertDialog.Builder ad = new Builder(act);
        ad.setTitle(R.string.label_InstSharedFile);
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
                    showFilePicker();
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
                    showFilePicker();

                }
                // File picked
                else {
                    Intent result = new Intent();
                    Bundle b = new Bundle();
                    b.putString(extra.FNAME, mChosenFile);
                    b.putString(extra.FPATH, mPath.getAbsolutePath());
                    result.putExtras(b);
                    setResult(Activity.RESULT_OK, result);
                    finish();
                }
            }
        });
        return ad;
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
                return xshowFilePicker(FilePickerActivity.this).create();
            default:
                break;
        }
        return super.onCreateDialog(id);
    }
}
