
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig.extra;

/***
 * Controller Activity: used to run the full protocol, main controller, launch
 * and receive calls from other activities
 */
public class ExchangeActivity extends BaseActivity {

    private static final String TAG = ExchangeConfig.LOG_TAG;

    private Intent mCurrIntent = null;
    private int mCurrView = 0;
    private ExchangeController mProt;
    private byte[][] mExcgMemData;
    private static boolean mLaunched = false;
    private Handler mHandler;
    private ProgressDialog mDlgProg;
    private String mProgressMsg = null;

    private static final int VIEW_PROMPT_ID = 76;
    private static final int VIEW_VERIFY_ID = 4;
    private static final int RESULT_ERROR_EXIT = 6;
    private static final int RESULT_CONFIRM_EXIT_PROMPT = 12;
    private static final int RESULT_CONFIRM_EXIT_VERIFY = 13;

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

            showProgressUpdate(numUsers, msg);
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

        // method
        mProt = new ExchangeController(this);

        byte[] userData = null;
        String hostName = null;
        boolean pairwiseLocal = false; // TODO: future BT/NFC option
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            userData = extras.getByteArray(extra.USER_DATA);
            hostName = extras.getString(extra.HOST_NAME);

            // confirm data exists
            if (userData == null || userData.length == 0) {
                showError(getString(R.string.error_NoDataToExchange));
                return;
            }
            mProt.setData(userData);

            if (pairwiseLocal) {
                // TODO: future ensure proper BT/NFC permissions
                mProt.setHostName(null);
            } else {
                // confirm server
                try {
                    URI uri = new URI("http", hostName, "", "");
                    URL url = uri.toURL();
                } catch (URISyntaxException e) {
                    showError("Hostname " + hostName + " is not well formed.");
                    return;
                } catch (MalformedURLException e) {
                    showError("Hostname " + hostName + " is not well formed.");
                    return;
                }
                mProt.setHostName(hostName);
            }
        }

        // initialize exchange
        if (handled(mProt.doInitialize())) {
            if (handled(mProt.doGenerateCommitment())) {
                if (pairwiseLocal) {
                    // when pairwise and local we are always 2 users
                    // in a local exchange, in the same group
                    mProt.setNumUsers(2);
                    mProt.setUserIdLink(1);
                    runThreadGetCommitmentsGetData();
                } else {
                    // Server supports multiple users in remote,
                    // arbitrary configurations so request group
                    // size as well as unique group id
                    showGroupSizePicker();
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
                        runThreadSendInvalidSignature();
                        break;
                    case RESULT_CANCELED:
                        // return to current activity
                        startActivityForResult(mCurrIntent, mCurrView);
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
                runThreadSendValidSignatureGetSignatures();
                break;
            case VerifyActivity.RESULT_DECOYWORDLIST1: // send bad match
                mProt.setHashSelection(1);
                runThreadSendInvalidSignature();
                break;
            case VerifyActivity.RESULT_DECOYWORDLIST2: // send bad match
                mProt.setHashSelection(2);
                runThreadSendInvalidSignature();
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
                runThreadGetCommitmentsGetData();
                break;
            case RESULT_CANCELED:
                showExitConfirm(RESULT_CONFIRM_EXIT_PROMPT);
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

    private void runThreadGetUserId() {
        showProgress(getString(R.string.prog_RequestingUserId), true);
        mDlgProg.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (handled(!mProt.isError()) && !mProt.isCanceled())
                    showLowestUserIdPrompt(mProt.getUserId());
            }
        });
        setProgressCancelHandler();

        Thread t = new Thread() {

            @Override
            public void run() {
                mProt.doRequestUserId();
                hideProgress();
            }
        };
        t.start();
    }

    private void runThreadGetCommitmentsGetData() {
        showProgress(getString(R.string.prog_CollectingOthersItems), true);
        mDlgProg.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (handled(!mProt.isError()) && !mProt.isCanceled())
                    showVerify(mProt.getHash(), mProt.getDecoyHash(1), mProt.getDecoyHash(2),
                            mProt.getRandomPos(3));
            }
        });
        setProgressCancelHandler();

        Thread t = new Thread() {

            @Override
            public void run() {
                mProt.doGetCommitmentsGetData();
                hideProgress();
            }
        };
        t.start();
    }

    private void runThreadSendValidSignatureGetSignatures() {
        showProgress(getString(R.string.prog_CollectingOthersCommitVerify), true);
        mDlgProg.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (handled(!mProt.isError()) && !mProt.isCanceled())
                    runThreadCreateSharedSecretGetNodesAndMatchNonces();
            }
        });
        setProgressCancelHandler();

        Thread t = new Thread() {

            @Override
            public void run() {
                mProt.doSendValidSignatureGetSignatures();
                hideProgress();
            }
        };
        t.start();
    }

    private void runThreadCreateSharedSecretGetNodesAndMatchNonces() {
        showProgress(getString(R.string.prog_ConstructingGroupKey), true);
        mDlgProg.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (handled(!mProt.isError()) && !mProt.isCanceled())
                    doVerifyFinalMatchDone();
            }
        });
        setProgressCancelHandler();

        Thread t = new Thread() {

            @Override
            public void run() {
                mProt.doCreateSharedSecretGetNodesAndMatchNonces();
                hideProgress();
            }
        };
        t.start();
    }

    private void runThreadSendInvalidSignature() {
        showProgress(getString(R.string.prog_CollectingOthersCommitVerify), true);
        mDlgProg.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!mProt.isCanceled()) {
                    showError(mProt.getErrorMsg());
                }
            }
        });
        setProgressCancelHandler();

        Thread t = new Thread() {

            @Override
            public void run() {
                mProt.doSendInvalidSignature();
                hideProgress();
            }
        };
        t.start();
    }

    private void setProgressCancelHandler() {
        mDlgProg.setCanceledOnTouchOutside(false);
        mDlgProg.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mProt.cancelProtocol();
                showExit(RESULT_CANCELED);
            }
        });
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

        items = new CharSequence[ExchangeConfig.MIN_USERS_AUTOCOUNT - 2];

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
                    runThreadGetUserId();
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

    private void showProgress(String msg, boolean indeterminate) {
        Log.i(TAG, msg);
        mDlgProg = new ProgressDialog(this);
        mDlgProg.setTitle(mProt.getStatusBanner(ExchangeActivity.this));
        mDlgProg.setProgressStyle(indeterminate ? ProgressDialog.STYLE_SPINNER
                : ProgressDialog.STYLE_HORIZONTAL);
        mDlgProg.setMessage(msg);
        mProgressMsg = msg;
        mDlgProg.setCancelable(true);
        mDlgProg.setIndeterminate(indeterminate);
        mDlgProg.setProgress(0);
        mDlgProg.show();
    }

    private void showProgressUpdate(int value, String msg) {
        if (mDlgProg != null) {
            mDlgProg.setTitle(mProt.getStatusBanner(ExchangeActivity.this));
            mDlgProg.setProgress(value);
            if (msg != null) {
                mDlgProg.setMessage(msg);
            }
        }
    }

    private void hideProgress() {
        if (mDlgProg != null) {
            mDlgProg.dismiss();
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
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

}
