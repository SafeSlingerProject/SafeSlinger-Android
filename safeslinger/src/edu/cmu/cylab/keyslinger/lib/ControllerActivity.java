
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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import edu.cmu.cylab.keyslinger.lib.KsConfig.extra;
import edu.cmu.cylab.starslinger.R;

/***
 * Controller Activity: used to run the full protocol, main controller, launch
 * and receive calls from other activities
 */
public class ControllerActivity extends ContactActivity {

    private static final String TAG = KsConfig.LOG_TAG;

    private Intent mCurrIntent = null;
    private int mCurrView = 0;
    private ExchangeController mProt;
    private String mContactLookupKey = null;
    private byte[][] mExcgMemData;
    private Bundle mSavedInstanceState;
    private static boolean mLaunched = false;
    private Bundle mThirdPartyArgs;
    private Handler mHandler;

    private static final int VIEW_START_ID = 1;
    private static final int VIEW_VERIFY_ID = 4;
    private static final int RESULT_ERROR_EXIT = 6;
    private static final int RESULT_CONFIRM_EXIT = 11;
    private static final int VIEW_SAVE_ID = 20;
    private static final int VIEW_PROMPT_ID = 76;

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
        setTheme(R.style.Theme_SafeSlinger);
        super.onCreate(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        if (!handleCallerAction())
            return;

        initOnReload();

        if (!mLaunched) {
            mLaunched = true;
        }

    }

    private boolean handleCallerAction() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            mContactLookupKey = extras.getString(extra.CONTACT_LOOKUP_KEY);
            mThirdPartyArgs = extras;
        }
        if (TextUtils.isEmpty(mContactLookupKey)) {
            showError(getString(R.string.error_CannotSendEmptyFile));
            showExit(RESULT_CANCELED);
        }

        return true;
    }

    private void initOnReload() {
        try {
            setContentView(R.layout.splash);
        } catch (OutOfMemoryError e) {
            showError(getString(R.string.error_OutOfMemoryError));
            showExit(RESULT_CANCELED);
            return;
        }

        // method
        mProt = new ExchangeController(this);

        // initialize exchange
        if (handled(mProt.doInitialize())) {

            // confirm contact exists
            if (!TextUtils.isEmpty(mContactLookupKey)) {
                showStart(mThirdPartyArgs); // Read saved
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case VIEW_START_ID:
                handleStartActivity(resultCode, data);
                break;

            case VIEW_PROMPT_ID:
                handlePromptActivity(resultCode, data);
                break;

            case VIEW_SAVE_ID:
                handleSaveActivity(resultCode, data);
                break;

            case VIEW_VERIFY_ID:
                handleVerifyActivity(resultCode);
                break;

            case RESULT_CONFIRM_EXIT:
                handleConfirmExitAlert(resultCode);
                break;

            case RESULT_ERROR_EXIT:
                restart();
                break;
        }
    }

    private void handleVerifyActivity(int resultCode) {
        switch (resultCode) {
            case RESULT_OK: // Verify button Match
                runThreadSendValidSignatureGetSignatures();
                break;
            case VerifyActivity.RESULT_DECOYWORDLIST: // send bad match
                runThreadSendInvalidSignature();
                break;
            case RESULT_CANCELED: // Verify button No Match
                showExitConfirm();
                break;
        }
    }

    private void handleSaveActivity(int resultCode, Intent data) {
        switch (resultCode) {
            case SaveActivity.RESULT_SAVE:
                showExitImportOK(data);
                break;
            case SaveActivity.RESULT_SELNONE:
                int exchanged = data.getExtras().getInt(extra.EXCHANGED_TOTAL);
                showNote(String.format(getString(R.string.state_SomeContactsImported), "0/"
                        + exchanged));
                showExit(RESULT_CANCELED);
                break;
            case RESULT_CANCELED:
                showNote(String.format(getString(R.string.state_SomeContactsImported), "0"));
                showExit(RESULT_CANCELED);
                break;
            default:
                showNote(String.format(getString(R.string.state_SomeContactsImported), "?"));
                showExit(RESULT_CANCELED);
                break;
        }
    }

    private void handlePromptActivity(int resultCode, Intent data) {

        switch (resultCode) {
            case RESULT_OK:
                int result = 0;
                try {
                    String stringExtra = data.getStringExtra(extra.GROUP_ID);
                    result = Integer.parseInt(stringExtra);
                } catch (NumberFormatException e) {
                    MyLog.e(TAG, e.getLocalizedMessage());
                    showNote(R.string.error_InvalidCommonUserId);
                    showLowestUserIdPrompt(mProt.getUserId()); // do-over
                }

                mProt.setUserIdLink(result); // save
                runThreadGetCommitmentsGetData();
                break;
            case RESULT_CANCELED:
                restart();
                break;
        }
    }

    private void handleStartActivity(int resultCode, Intent data) {
        switch (resultCode) {
            case RESULT_OK:
                mProt.setData(data.getStringExtra(extra.USER_DATA).getBytes());
                showGroupSizePicker();
                break;
            case RESULT_KEYSLINGERCONTACTSEL:
                showExit(resultCode);
                break;
            case RESULT_KEYSLINGERCONTACTEDIT:
                showExit(resultCode);
                break;
            case RESULT_KEYSLINGERCONTACTADD:
                showExit(resultCode);
                break;
            case RESULT_CANCELED:
                showExit(resultCode);
                break;
        }
    }

    private void restart() {
        this.onCreate(mSavedInstanceState); // restart
    }

    private void doGroupFormation() {
        if (handled(mProt.doGenerateCommitment(this))) {
            runThreadGetUserId();
        }
    }

    private void doVerifyFinalMatchDone() {
        mExcgMemData = mProt.getGroupData().sortOthersDataNew(mProt.getUserId());
        // decrypt the data with each others match nonce
        try {
            showSave(mProt.decryptMemData(mExcgMemData, mProt.getUserId()));
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
                if (handled(!mProt.isError()) && !mProt.isCancelled())
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
                if (handled(!mProt.isError()) && !mProt.isCancelled())
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
                if (handled(!mProt.isError()) && !mProt.isCancelled())
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
                if (handled(!mProt.isError()) && !mProt.isCancelled())
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
                if (!mProt.isCancelled()) {
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
        if (!success)
            showError(mProt.getErrorMsg());
        return success;
    }

    private void handleConfirmExitAlert(int buttonId) {
        switch (buttonId) {

            case RESULT_OK:
                switch (mCurrView) {
                    case VIEW_VERIFY_ID:
                        runThreadSendInvalidSignature();
                        break;
                    default:
                        restart();
                        break;
                }
                break;

            case RESULT_CANCELED:
                // return to current activity
                startActivityForResult(mCurrIntent, mCurrView);
                break;
        }
    }

    private void showStart(Bundle args) {
        mCurrIntent = new Intent(ControllerActivity.this, StartActivity.class);
        if (args != null) {
            mCurrIntent.replaceExtras(args);
        } else {
            mCurrIntent.putExtra(extra.CONTACT_LOOKUP_KEY, mContactLookupKey);
        }

        mCurrView = VIEW_START_ID;
        startActivityForResult(mCurrIntent, mCurrView);
    }

    private void showSave(byte[][] memData) {
        mCurrIntent = new Intent(ControllerActivity.this, SaveActivity.class);
        for (int i = 0; i < memData.length; i++) {
            mCurrIntent.putExtra(extra.MEMBER_DATA + i, memData[i]);
        }
        mCurrView = VIEW_SAVE_ID;
        startActivityForResult(mCurrIntent, mCurrView);
    }

    private void showVerify(byte[] hashVal, byte[] decoyHash1, byte[] decoyHash2, int randomPos) {
        mCurrIntent = new Intent(ControllerActivity.this, VerifyActivity.class);
        mCurrIntent.putExtra(extra.FLAG_HASH, hashVal);
        mCurrIntent.putExtra(extra.DECOY1_HASH, decoyHash1);
        mCurrIntent.putExtra(extra.DECOY2_HASH, decoyHash2);
        mCurrIntent.putExtra(extra.RANDOM_POS, randomPos);
        mCurrView = VIEW_VERIFY_ID;
        startActivityForResult(mCurrIntent, mCurrView);
    }

    private void showExitConfirm() {
        showQuestion(getString(R.string.ask_QuitConfirmation), RESULT_CONFIRM_EXIT);
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
        MyLog.e(TAG, msg);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_Error);
        ad.setMessage(msg);
        ad.setCancelable(false);
        ad.setNeutralButton(R.string.btn_OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                onActivityResult(RESULT_ERROR_EXIT, RESULT_OK, null);
            }
        });
        return ad;
    }

    private void showLowestUserIdPrompt(int usrid) {
        mCurrIntent = new Intent(ControllerActivity.this, PromptActivity.class);
        mCurrIntent.putExtra(extra.MESSAGE, getString(R.string.label_PromptInstruct));
        mCurrIntent.putExtra(extra.USER_ID, usrid);
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

        items = new CharSequence[KsConfig.MIN_USERS_AUTOCOUNT - 2];

        for (i = 0; i < items.length; i++) {
            items[i] = String.format(getString(R.string.choice_NumUsers), i + KsConfig.MIN_USERS);
        }
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_size);
        ad.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                mProt.setNumUsers(item + 2);
            }
        });
        ad.setNeutralButton(R.string.btn_OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (mProt.getNumUsers() > 0) {
                    dialog.dismiss();
                    doGroupFormation();
                } else {
                    // reset and error
                    mProt.setNumUsers(0);
                    showNote(String.format(getString(R.string.error_MinUsersRequired),
                            KsConfig.MIN_USERS));
                    showGroupSizePicker();
                }
            }
        });
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                restart();
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

        setResultForParent(RESULT_KEYSLINGERIMPORTED, data);
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
            setResultForParent(RESULT_KEYSLINGERIMPORTED);
        } else if (resultCode == RESULT_CANCELED) {
            setResultForParent(RESULT_KEYSLINGERCANCELED);
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
        MyLog.i(TAG, msg);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_Question);
        ad.setMessage(msg);
        ad.setCancelable(false);
        ad.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                onActivityResult(requestCode, RESULT_OK, null);
            }
        });
        ad.setNegativeButton(R.string.btn_No, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                onActivityResult(requestCode, RESULT_CANCELED, null);
            }
        });
        return ad;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_ERROR:
                return xshowError(ControllerActivity.this, args).create();
            case DIALOG_QUESTION:
                return xshowQuestion(ControllerActivity.this, args).create();
            case DIALOG_GRP_SIZE:
                return xshowGroupSizePicker(ControllerActivity.this).create();
        }
        return super.onCreateDialog(id);
    }

}
