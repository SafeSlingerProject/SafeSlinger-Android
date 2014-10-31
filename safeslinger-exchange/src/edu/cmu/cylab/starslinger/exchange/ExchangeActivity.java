
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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig.extra;

/***
 * Controller Activity: used to run the full protocol, main controller, launch
 * and receive calls from other activities
 */
public class ExchangeActivity extends BaseActivity {

    private Intent mCurrIntent = null;
    private int mCurrView = 0;
    private static ExchangeController mProt;
    private byte[][] mExcgMemData;
    private static boolean mLaunched = false;
    private Handler mHandler;
    private static ProgressDialog mDlgProg;
    private static String mProgressMsg = null;
    private byte[] mUserData;
    private String mHostName;

    private static final int VIEW_PROMPT_ID = 76;
    private static final int VIEW_VERIFY_ID = 4;
    private static final int RESULT_ERROR_EXIT = 6;
    private static final int RESULT_CONFIRM_EXIT_PROMPT = 12;
    private static final int RESULT_CONFIRM_EXIT_VERIFY = 13;
    private static final int RESULT_CONFIRM_EXIT_PROGRESS = 14;

    public static final int RESULT_EXCHANGE_OK = 300;
    public static final int RESULT_EXCHANGE_CANCELED = 301;

    private static final int MS_POLL_INTERVAL = 500;

    private Runnable mUpdateReceivedProg = new Runnable() {

        @Override
        public void run() {

            int num = 0;
            int numUsers = mProt.getNumUsers();
            int numUsersMatchNonces = mProt.getNumUsersMatchNonces();
            int numUsersKeyNodes = mProt.getNumUsersKeyNodes();
            int numUsersSigs = mProt.getNumUsersSigs();
            int numUsersData = mProt.getNumUsersData();
            int numUsersCommit = mProt.getNumUsersCommit();
            int total = numUsersCommit + numUsersData + numUsersSigs + numUsersKeyNodes
                    + numUsersMatchNonces;

            if (numUsersMatchNonces > 0 && total > (4 * numUsers)) {
                num = numUsersMatchNonces;
            } else if (numUsersKeyNodes > 2 && total > (3 * numUsers)) {
                num = numUsersKeyNodes;
            } else if (numUsersSigs > 0 && total > (2 * numUsers)) {
                num = numUsersSigs;
            } else if (numUsersData > 0 && total > (1 * numUsers)) {
                num = numUsersData;
            } else if (numUsersCommit > 0 && total > (0 * numUsers)) {
                num = numUsersCommit;
            }

            String msg;
            if (num > 0) {
                String msgNRecvItems = String.format(getString(R.string.label_ReceivedNItems), num);
                msg = String.format("%s\n\n%s", mProgressMsg, msgNRecvItems);
            } else {
                msg = mProgressMsg;
            }

            showProgressUpdate(msg);
            mHandler.postDelayed(this, MS_POLL_INTERVAL);
        }
    };

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
        } else {
            setTitle(R.string.lib_name);
        }

        try {
            setContentView(R.layout.sse__splash);
        } catch (OutOfMemoryError e) {
            showError(getString(R.string.error_OutOfMemoryError));
            return;
        }

        int numUsersIn = 0;
        Bundle extras = savedInstanceState != null ? savedInstanceState : getIntent().getExtras();
        if (extras != null) {
            mUserData = extras.getByteArray(extra.USER_DATA);
            mHostName = extras.getString(extra.HOST_NAME);
            numUsersIn = extras.getInt(extra.NUM_USERS, 0);
        }

        // only initialize on new requests, this activity has very little ui
        if (savedInstanceState == null) {

            // new call from 3rd party, proceed
            mProt = new ExchangeController(this);

            // check for required Bundle values
            if (mUserData == null || mUserData.length == 0) {
                showError(getString(R.string.error_NoDataToExchange));
                return;
            }
            if (TextUtils.isEmpty(mHostName)) {
                showError("Hostname " + mHostName + " is not well formed.");
                return;
            }

            try {
                URI uri = new URI("http", mHostName, "", "");
                URL url = uri.toURL();
            } catch (URISyntaxException e) {
                showError("Hostname " + mHostName + " is not well formed.");
                return;
            } catch (MalformedURLException e) {
                showError("Hostname " + mHostName + " is not well formed.");
                return;
            }

            mProt.setData(mUserData);
            mProt.setHostName(mHostName);

            // initialize exchange
            if (handled(mProt.doInitialize())) {
                if (handled(mProt.doGenerateCommitment())) {
                    if (numUsersIn == 0) {
                        // default mode, uninitialized number of users
                        // so ask how many users?
                        showGroupSizePicker();

                    } else if (numUsersIn >= ExchangeConfig.MIN_USERS
                            && numUsersIn <= ExchangeConfig.MAX_USERS) {
                        // optional mode, 3rd party will pass in num users
                        mProt.setNumUsers(numUsersIn);
                        AssignUserTask assignUser = new AssignUserTask();
                        assignUser.execute(new String());
                        // TODO: option can be removed for combo-lock UI

                    } else {
                        // reset and error, number out of range
                        mProt.setNumUsers(0);
                        showNote(String.format(getString(R.string.error_MinUsersRequired),
                                ExchangeConfig.MIN_USERS));
                        showGroupSizePicker();
                    }
                }
            }
        }

        if (!mLaunched) {
            mLaunched = true;
        }
    }

    public static boolean callerHasBlueToothPermission(Context ctx) {
        String PERMISSION_BLUETOOTH = "android.permission.BLUETOOTH";
        String PERMISSION_BLUETOOTH_ADMIN = "android.permission.BLUETOOTH_ADMIN";
        PackageManager pm = ctx.getPackageManager();
        int bt = pm.checkPermission(PERMISSION_BLUETOOTH, ctx.getPackageName());
        int bt_admin = pm.checkPermission(PERMISSION_BLUETOOTH_ADMIN, ctx.getPackageName());
        return (bt == PackageManager.PERMISSION_GRANTED && bt_admin == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean callerHasNfcPermission(Context ctx) {
        String PERMISSION_NFC = "android.permission.NFC";
        PackageManager pm = ctx.getPackageManager();
        int nfc = pm.checkPermission(PERMISSION_NFC, ctx.getPackageName());
        return (nfc == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {

            case VIEW_PROMPT_ID:
                handleGroupingActivity(resultCode, data);
                break;

            case VIEW_VERIFY_ID:
                handleVerifyActivity(resultCode);
                break;

            case RESULT_CONFIRM_EXIT_PROMPT:
                switch (resultCode) {
                    case RESULT_OK:
                        showExit(RESULT_CANCELED);
                        break;
                    case RESULT_CANCELED:
                        // return to current activity
                        startActivityForResult(mCurrIntent, mCurrView);
                        break;
                    default:
                        break;
                }
                break;

            case RESULT_CONFIRM_EXIT_VERIFY:
                switch (resultCode) {
                    case RESULT_OK:
                        // confirmed exit from verify, send invalid sig
                        SyncBadSigTask syncBadSig = new SyncBadSigTask();
                        syncBadSig.execute(new String());
                        break;
                    case RESULT_CANCELED:
                        // return to current activity
                        startActivityForResult(mCurrIntent, mCurrView);
                        break;
                    default:
                        break;
                }
                break;

            case RESULT_CONFIRM_EXIT_PROGRESS:
                switch (resultCode) {
                    case RESULT_OK:
                        mProt.cancelProtocol();
                        showExit(RESULT_CANCELED);
                        break;
                    case RESULT_CANCELED:
                        showProgress(mProgressMsg);
                        break;
                    default:
                        break;
                }
                break;

            case RESULT_ERROR_EXIT:
                showExit(RESULT_CANCELED);
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleVerifyActivity(int resultCode) {
        switch (resultCode) {
            case VerifyActivity.RESULT_CORRECTWORDLIST: // Verify button Match
                mProt.setHashSelection(0);
                SyncGoodSigsTask syncSigs = new SyncGoodSigsTask();
                syncSigs.execute(new String());
                break;
            case VerifyActivity.RESULT_DECOYWORDLIST1: // send bad match
                mProt.setHashSelection(1);
                SyncBadSigTask syncBadSig1 = new SyncBadSigTask();
                syncBadSig1.execute(new String());
                break;
            case VerifyActivity.RESULT_DECOYWORDLIST2: // send bad match
                mProt.setHashSelection(2);
                SyncBadSigTask syncBadSig2 = new SyncBadSigTask();
                syncBadSig2.execute(new String());
                break;
            case RESULT_CANCELED: // Verify button No Match
                showExitConfirm(RESULT_CONFIRM_EXIT_VERIFY);
                break;
            default:
                break;
        }
    }

    private void handleGroupingActivity(int resultCode, Intent data) {
        switch (resultCode) {
            case RESULT_OK:
                int result = 0;
                try {
                    String stringExtra = data.getStringExtra(extra.GROUP_ID);
                    result = Integer.parseInt(stringExtra);
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                    showNote(R.string.error_InvalidCommonUserId);
                    showLowestUserIdPrompt(mProt.getUserId()); // do-over
                }

                mProt.setUserIdLink(result); // save
                SyncCommitsDataTask syncCommitData = new SyncCommitsDataTask();
                syncCommitData.execute(new String());
                break;
            case RESULT_CANCELED:
                showExit(RESULT_CANCELED);
                break;
            default:
                break;
        }
    }

    private void doVerifyFinalMatchDone() {
        mExcgMemData = mProt.getGroupData().sortOthersDataNew(mProt.getUserId());
        // decrypt the data with each others match nonce
        try {
            final byte[][] decryptMemData = mProt.decryptMemData(mExcgMemData, mProt.getUserId());

            Intent data = new Intent();
            for (int i = 0; i < decryptMemData.length; i++) {
                data.putExtra(extra.MEMBER_DATA + i, decryptMemData[i]);
            }
            showExitImportOK(data);
        } catch (InvalidKeyException e) {
            showError(e.getLocalizedMessage());
        } catch (NoSuchAlgorithmException e) {
            showError(e.getLocalizedMessage());
        } catch (NoSuchPaddingException e) {
            showError(e.getLocalizedMessage());
        } catch (IllegalBlockSizeException e) {
            showError(e.getLocalizedMessage());
        } catch (BadPaddingException e) {
            showError(e.getLocalizedMessage());
        } catch (InvalidAlgorithmParameterException e) {
            showError(e.getLocalizedMessage());
        }
    }

    private class AssignUserTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... arg0) {
            publishProgress(getString(R.string.prog_RequestingUserId));
            mProt.doRequestUserId();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            showProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            endProgress();
            if (handled(!mProt.isError()) && !mProt.isCanceled()) {
                showLowestUserIdPrompt(mProt.getUserId());
            }
        }
    }

    private class SyncCommitsDataTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... arg0) {
            publishProgress(getString(R.string.prog_CollectingOthersItems));
            mProt.doGetCommitmentsGetData();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            showProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            endProgress();
            if (handled(!mProt.isError()) && !mProt.isCanceled()) {
                showVerify(mProt.getHash(), mProt.getDecoyHash(1), mProt.getDecoyHash(2),
                        mProt.getRandomPos(3));
            }
        }
    }

    private class SyncGoodSigsTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... arg0) {
            publishProgress(getString(R.string.prog_CollectingOthersCommitVerify));
            mProt.doSendValidSignatureGetSignatures();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            showProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            endProgress();
            if (handled(!mProt.isError()) && !mProt.isCanceled()) {
                SyncNodesNoncesTask syncNodesNonces = new SyncNodesNoncesTask();
                syncNodesNonces.execute(new String());
            }
        }
    }

    private class SyncNodesNoncesTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... arg0) {
            publishProgress(getString(R.string.prog_ConstructingGroupKey));
            mProt.doCreateSharedSecretGetNodesAndMatchNonces();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            showProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            endProgress();
            if (handled(!mProt.isError()) && !mProt.isCanceled()) {
                doVerifyFinalMatchDone();
            }
        }
    }

    private class SyncBadSigTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... arg0) {
            publishProgress(getString(R.string.prog_CollectingOthersCommitVerify));
            mProt.doSendInvalidSignature();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            showProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            endProgress();
            if (!mProt.isCanceled()) {
                showError(mProt.getErrorMsg());
            }
        }
    }

    private void setProgressCancelHandler() {
        if (mDlgProg != null) {
            mDlgProg.setCanceledOnTouchOutside(false);
            mDlgProg.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    // if we've already grouped, check for confirm, else cancel
                    if (mProt.getUserIdLink() > 0) {
                        showExitConfirm(RESULT_CONFIRM_EXIT_PROGRESS);
                    } else {
                        mProt.cancelProtocol();
                        showExit(RESULT_CANCELED);
                    }
                }
            });
        }
        mHandler = new Handler();
        mHandler.removeCallbacks(mUpdateReceivedProg);
        mHandler.postDelayed(mUpdateReceivedProg, MS_POLL_INTERVAL);
    }

    private boolean handled(boolean success) {
        if (!success) {
            showError(mProt.getErrorMsg());
        }
        return success;
    }

    private void showVerify(byte[] hashVal, byte[] decoyHash1, byte[] decoyHash2, int randomPos) {
        mCurrIntent = new Intent(ExchangeActivity.this, VerifyActivity.class);
        mCurrIntent.putExtra(extra.FLAG_HASH, hashVal);
        mCurrIntent.putExtra(extra.DECOY1_HASH, decoyHash1);
        mCurrIntent.putExtra(extra.DECOY2_HASH, decoyHash2);
        mCurrIntent.putExtra(extra.RANDOM_POS, randomPos);
        mCurrIntent.putExtra(extra.NUM_USERS, mProt.getNumUsers());
        mCurrView = VIEW_VERIFY_ID;
        startActivityForResult(mCurrIntent, mCurrView);
    }

    private void showExitConfirm(int resultCode) {
        showQuestion(getString(R.string.ask_QuitConfirmation), resultCode);
    }

    private void showError(String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            removeDialog(DIALOG_ERROR);
            showDialog(DIALOG_ERROR, args);
        }
    }

    private AlertDialog.Builder xshowError(Activity act, Bundle args) {
        String msg = args.getString(extra.RESID_MSG);
        Log.e(TAG, msg);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.lib_name);
        ad.setMessage(msg);
        ad.setCancelable(false);
        ad.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                onActivityResult(RESULT_ERROR_EXIT, RESULT_OK, null);
            }
        });
        return ad;
    }

    private void showLowestUserIdPrompt(int usrid) {
        mCurrIntent = new Intent(ExchangeActivity.this, GroupingActivity.class);
        mCurrIntent.putExtra(extra.USER_ID, usrid);
        mCurrIntent.putExtra(extra.NUM_USERS, mProt.getNumUsers());
        mCurrView = VIEW_PROMPT_ID;
        startActivityForResult(mCurrIntent, mCurrView);
    }

    private void showGroupSizePicker() {
        if (!isFinishing()) {
            removeDialog(DIALOG_GRP_SIZE);
            showDialog(DIALOG_GRP_SIZE);
        }
    }

    private AlertDialog.Builder xshowGroupSizePicker(Activity act) {
        final CharSequence[] items;
        int i = 0;

        items = new CharSequence[ExchangeConfig.MAX_USERS - 1];

        for (i = 0; i < items.length; i++) {
            items[i] = String.format(getString(R.string.choice_NumUsers), i
                    + ExchangeConfig.MIN_USERS);
        }
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_size);
        ad.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                mProt.setNumUsers(item + 2);
            }
        });
        ad.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (mProt.getNumUsers() > 0) {
                    dialog.dismiss();
                    AssignUserTask assignUser = new AssignUserTask();
                    assignUser.execute(new String());
                } else {
                    // reset and error
                    mProt.setNumUsers(0);
                    showNote(String.format(getString(R.string.error_MinUsersRequired),
                            ExchangeConfig.MIN_USERS));
                    showGroupSizePicker();
                }
            }
        });
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                showExit(RESULT_CANCELED);
            }
        });
        return ad;
    }

    private void showExitImportOK(Intent data) {
        Thread t = new Thread() {

            @Override
            public void run() {
                if (mHandler != null) {
                    mHandler.removeCallbacks(mUpdateReceivedProg);
                }
                mProt.endProtocol();
            }
        };
        t.start();

        setResultForParent(RESULT_EXCHANGE_OK, data);
        this.finish();
    }

    private void showExit(int resultCode) {
        Thread t = new Thread() {

            @Override
            public void run() {
                if (mHandler != null) {
                    mHandler.removeCallbacks(mUpdateReceivedProg);
                }
                mProt.endProtocol();
            }
        };
        t.start();

        if (resultCode == RESULT_OK) {
            setResultForParent(RESULT_EXCHANGE_OK);
        } else if (resultCode == RESULT_CANCELED) {
            setResultForParent(RESULT_EXCHANGE_CANCELED);
        } else {
            setResultForParent(resultCode);
        }
        this.finish();
    }

    private void showQuestion(String msg, final int requestCode) {
        Bundle args = new Bundle();
        args.putInt(extra.REQUEST_CODE, requestCode);
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            removeDialog(DIALOG_QUESTION);
            showDialog(DIALOG_QUESTION, args);
        }
    }

    private AlertDialog.Builder xshowQuestion(Activity act, Bundle args) {
        final int requestCode = args.getInt(extra.REQUEST_CODE);
        String msg = args.getString(extra.RESID_MSG);
        Log.i(TAG, msg);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.lib_name);
        ad.setMessage(msg);
        ad.setCancelable(false);
        ad.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                onActivityResult(requestCode, RESULT_OK, null);
            }
        });
        ad.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                onActivityResult(requestCode, RESULT_CANCELED, null);
            }
        });
        return ad;
    }

    private void showProgress(String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            removeDialog(DIALOG_PROGRESS);
            showDialog(DIALOG_PROGRESS, args);
        }
    }

    private Dialog xshowProgress(Activity act, Bundle args) {
        String msg = args.getString(extra.RESID_MSG);
        Log.i(TAG, msg);

        if (mDlgProg != null) {
            mDlgProg = null;
            mProgressMsg = null;
        }
        mDlgProg = new ProgressDialog(act);
        mDlgProg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDlgProg.setMessage(msg);
        mProgressMsg = msg;
        mDlgProg.setCancelable(true);
        setProgressCancelHandler();

        return mDlgProg;
    }

    private void showProgressUpdate(String msg) {
        if (mDlgProg != null) {
            mDlgProg.setTitle(mProt.getStatusBanner(ExchangeActivity.this));
            if (msg != null) {
                mDlgProg.setMessage(msg);
            }
        }
    }

    private void endProgress() {
        if (mDlgProg != null) {
            mDlgProg.dismiss();
            mDlgProg = null;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_ERROR:
                return xshowError(ExchangeActivity.this, args).create();
            case DIALOG_QUESTION:
                return xshowQuestion(ExchangeActivity.this, args).create();
            case DIALOG_GRP_SIZE:
                return xshowGroupSizePicker(ExchangeActivity.this).create();
            case DIALOG_PROGRESS:
                return xshowProgress(ExchangeActivity.this, args);
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mUserData != null) {
            outState.putByteArray(extra.USER_DATA, mUserData);
        }
        if (mHostName != null) {
            outState.putString(extra.HOST_NAME, mHostName);
        }
        super.onSaveInstanceState(outState);
    }
}
