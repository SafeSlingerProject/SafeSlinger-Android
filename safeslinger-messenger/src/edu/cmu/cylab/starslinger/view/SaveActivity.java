
package edu.cmu.cylab.starslinger.view;

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

import java.util.ArrayList;
import java.util.List;

import a_vcard.android.provider.Contacts;
import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.ContactStruct.ContactMethod;
import a_vcard.android.syncml.pim.vcard.Name;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.OnAccountsUpdateListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncAdapterType;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager.BadTokenException;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig;
import edu.cmu.cylab.starslinger.model.AccountData;
import edu.cmu.cylab.starslinger.model.ContactAccessor;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class SaveActivity extends BaseActivity implements OnAccountsUpdateListener {

    private final ContactAccessor mAccessor = ContactAccessor.getInstance();
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    public static final int RESULT_SELNONE = 23;
    public static final int RESULT_SAVE = 24;
    private static final String PREF_SHOW_HELP = "prefAutoShowHelp";

    private byte[][] mMemData;
    private List<ContactStruct> mContacts;
    protected String mSelectedAcctType = null;
    protected String mSelectedAcctName = null;
    private static int mListVisiblePos;
    private static int mListTopOffset;
    private ListView mListViewSaveContacts;
    private Button mButtonSave;

    private ArrayList<AccountData> mAccounts;
    private AccountAdapter mAccountAdapter;
    private Spinner mAccountSpinner;
    private AccountData mSelectedAccount;
    private String mPrefAccountName = null;
    private String mPrefAccountType = null;
    private boolean mPrefsSelected;
    private String mUnsyncName = null;
    private String mUnsyncType = null;
    private static final String UNSYNC_PKG = "unsynchronized";
    private TableLayout mTableLayoutSpin;
    private ProgressDialog mDlgProg;
    private String mProgressMsg = null;
    private static boolean mCloseConfirmed;

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
                showHelp();
                return true;
            case MENU_FEEDBACK:
                SafeSlinger.getApplication().showFeedbackEmail(SaveActivity.this);
                return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Safeslinger);
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mListTopOffset = 0;
            mListVisiblePos = 0;
            mCloseConfirmed = false;
        }

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.app_name);
        bar.setSubtitle(R.string.title_save);

        setContentView(R.layout.savedata);

        mListViewSaveContacts = (ListView) findViewById(R.id.SaveScrollViewMembers);
        mButtonSave = (Button) findViewById(R.id.SaveButtonSave);

        // init
        mContacts = new ArrayList<ContactStruct>();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            byte[] data = null;
            int length = extras.size();
            mMemData = new byte[length][];
            int i = 0;
            do {
                data = extras.getByteArray(ExchangeConfig.extra.MEMBER_DATA + i);
                if (data != null) {
                    mMemData[i] = data;
                    i++;
                }
            } while (data != null);
        }

        // display names list so users can selectively choose which to save
        if (mMemData != null) {
            mContacts = parseVCards(mMemData);

            SaveContactAdapter adapter = new SaveContactAdapter(SaveActivity.this, mContacts);
            mListViewSaveContacts.setAdapter(adapter);

            // restore list position
            mListViewSaveContacts.setSelectionFromTop(mListVisiblePos, mListTopOffset);
        }

        mButtonSave.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                SaveSelectedContactsTask saveSelected = new SaveSelectedContactsTask();
                saveSelected.execute(new String());
            }
        });

        mListViewSaveContacts.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // nothing to do...
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                // save list position
                if (visibleItemCount != 0) {
                    mListVisiblePos = firstVisibleItem;
                    View v = mListViewSaveContacts.getChildAt(0);
                    mListTopOffset = (v == null) ? 0 : v.getTop();
                }
            }
        });

        mUnsyncName = getString(R.string.label_None);
        mUnsyncType = getString(R.string.label_phoneOnly);

        mPrefsSelected = false;

        mTableLayoutSpin = (TableLayout) findViewById(R.id.accountLayout);
        TextView textView = new TextView(this);
        mAccountSpinner = new Spinner(this);
        textView.setText(R.string.label_SaveAccount);
        textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        mAccountSpinner.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
        mTableLayoutSpin.addView(textView);
        mTableLayoutSpin.addView(mAccountSpinner);

        // Prepare model for account spinner
        mAccounts = new ArrayList<AccountData>();
        mAccountAdapter = new AccountAdapter(this, this, mAccounts);
        mAccountSpinner.setAdapter(mAccountAdapter);

        // Load account preference if any
        mPrefAccountName = SafeSlingerPrefs.getAccountName();
        mPrefAccountType = SafeSlingerPrefs.getAccountType();

        // Prepare the system account manager. On registering the listener
        // below, we also ask for
        // an initial callback to pre-populate the account list.
        AccountManager.get(this).addOnAccountsUpdatedListener(this, null, true);

        // Register handlers for UI elements
        mAccountSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long i) {
                updateAccountSelection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // We don't need to worry about nothing being selected, since
                // Spinners don't allow
                // this.
            }
        });

        // show help automatically only for first time installers
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        if (sharedPref.getBoolean(PREF_SHOW_HELP, true)) {
            // show help, turn off for next time
            showHelp();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(PREF_SHOW_HELP, false);
            editor.commit();
        }
    }

    private class SaveSelectedContactsTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... arg0) {
            publishProgress(getString(R.string.prog_SavingContactsToAddressBook));
            saveSelectedContacts();
            endProgress();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            showProgress(values[0]);
        }
    }

    private void saveSelectedContacts() {
        StringBuilder errors = new StringBuilder();

        // save the contacts
        int selected = 0;
        Intent data = new Intent();
        int exchanged = mContacts.size();
        for (int i = 0; i < exchanged; i++) {

            // save if selected
            String contactLookupKey = null;
            ContactStruct mem = mContacts.get(i);

            // create custom data for export to third party as well...
            data.putExtra(extra.NAME + selected, mem.name.toString());
            data.putExtra(extra.PHOTO + selected, mem.photoBytes);
            if (mem.contactmethodList != null) {
                for (ContactMethod item : mem.contactmethodList) {
                    if (item.kind == Contacts.KIND_IM && mAccessor.isCustomIm(item.label)) {
                        data.putExtra(item.label + selected,
                                SSUtil.finalDecode(item.data.getBytes()));
                    }
                }
            }

            boolean checked = SaveContactAdapter.isPositionChecked(i);
            if (checked && isValidContact(mem)) {
                Name name = mem.name;
                if (name == null || name.toString() == null
                        || TextUtils.isEmpty(name.toString().trim())) {
                    errors.append("\n  Bad Name found");
                    continue;
                }
                contactLookupKey = getContactLookupKeyByName(name.toString());

                String rawContactId = null;
                if (!TextUtils.isEmpty(contactLookupKey)) {
                    String where = Data.LOOKUP_KEY + " = ?";
                    String[] whereParameters = new String[] {
                        contactLookupKey
                    };
                    Cursor c = getContentResolver().query(Data.CONTENT_URI, null, where,
                            whereParameters, null);
                    if (c != null) {
                        try {
                            if (c.moveToFirst()) {
                                do {
                                    rawContactId = c.getString(c
                                            .getColumnIndex(Data.RAW_CONTACT_ID));
                                } while (c.moveToNext());
                            }
                        } finally {
                            c.close();
                        }
                    }

                    // for an update we have to be careful to prevent import
                    // of duplicate data, so here we can query the aggregate
                    // contact for equivalently matching contact fields and
                    // prevent the import so we won't get duplicate phone
                    // numbers for example, one with international prefix
                    // and one without.
                    mem = loadContactDataNoDuplicates(this, contactLookupKey, mem, true);
                }

                // for name-only contact don't add to address book, just
                // return a null lookup key.
                if (hasAddressBookData(mem)) {

                    if (!TextUtils.isEmpty(rawContactId)) {
                        // name match, so update...
                        if (!mAccessor.updateOldContact(mem, SaveActivity.this, mSelectedAcctType,
                                mSelectedAcctName, rawContactId)) {
                            errors.append("\n  ").append(name.toString()).append(" ")
                                    .append(getString(R.string.error_ContactUpdateFailed));
                            continue;
                        }
                    } else {
                        // no name match, so insert...
                        rawContactId = mAccessor.insertNewContact(mem, mSelectedAcctType,
                                mSelectedAcctName, SaveActivity.this);
                        if (!TextUtils.isEmpty(rawContactId)) {
                            contactLookupKey = getContactLookupKeyByRawContactId(rawContactId);
                        } else {
                            errors.append("\n  ").append(name.toString()).append(" ")
                                    .append(getString(R.string.error_ContactInsertFailed));
                            continue;
                        }
                    }

                    data.putExtra(extra.CONTACT_LOOKUP_KEY + selected, contactLookupKey);
                }
            }

            selected++;
        }

        // add redundant check for correct number of contacts imported...
        data.putExtra(extra.EXCHANGED_TOTAL, exchanged);

        if (errors.length() > 0) {
            errors.insert(0, mSelectedAcctName);
            showNote(errors.toString());
            return;
        }

        mCloseConfirmed = true;
        if (selected == 0) {
            setResult(RESULT_SELNONE, data);
        } else {
            setResult(RESULT_SAVE, data);
        }
        finish();
    }

    public static boolean hasAddressBookData(ContactStruct mem) {
        if (mem.photoBytes != null && mem.photoBytes.length > 0) {
            // any photo should be saved
            return true;
        }
        if (mem.addressList != null && mem.addressList.size() > 0) {
            // any postal should be saved
            return true;
        }
        if (mem.phoneList != null && mem.phoneList.size() > 0) {
            // any phone should be saved
            return true;
        }
        if (mem.contactmethodList != null) {
            for (ContactMethod cm : mem.contactmethodList) {
                if (cm.kind == Contacts.KIND_EMAIL) {
                    // any email should be saved
                    return true;
                }
                if (cm.kind == Contacts.KIND_URL) {
                    // any web site should be saved
                    return true;
                }

                // if we only find safeslinger id and key,
                // its not worth saving from KIND_IM
            }
        }

        return false;
    }

    private boolean isValidContact(ContactStruct mem) {
        boolean validContact = mem.name != null && mem.name.toString().trim().length() > 0
                && !mem.name.toString().contains("Error:");
        return validContact;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(SaveActivity.this, args).create();
            case DIALOG_QUESTION:
                return xshowQuestion(SaveActivity.this, args).create();
            case DIALOG_PROGRESS:
                return xshowProgress(SaveActivity.this, args);
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void onBackPressed() {
        if (!mCloseConfirmed) {
            showQuestion(getString(R.string.ask_QuitConfirmation));
        }
    }

    @Override
    protected void onPause() {
        if (!mCloseConfirmed) {
            showQuestion(getString(R.string.ask_QuitConfirmation));
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        // Remove AccountManager callback
        AccountManager.get(this).removeOnAccountsUpdatedListener(this);
        if (!mCloseConfirmed) {
            showQuestion(getString(R.string.ask_QuitConfirmation));
        }
        super.onDestroy();
    }

    @Override
    public void onAccountsUpdated(Account[] a) {
        MyLog.i(TAG, "Account list update detected");
        // Clear out any old data to prevent duplicates
        mAccounts.clear();

        // Get account data from system
        AuthenticatorDescription[] accountTypes = AccountManager.get(this).getAuthenticatorTypes();

        // Also, get a list of all sync adapters and find the ones that
        // support contacts:
        SyncAdapterType[] syncs = ContentResolver.getSyncAdapterTypes();
        ArrayList<String> contactAccountTypes = new ArrayList<String>();
        for (SyncAdapterType sync : syncs) {
            if (ContactsContract.AUTHORITY.equals(sync.authority) && sync.supportsUploading()) {
                contactAccountTypes.add(sync.accountType);
            }
        }

        // Populate tables
        for (int i = 0; i < a.length; i++) {
            // The user may have multiple accounts with the same name, so we
            // need to construct a
            // meaningful display name for each.
            String systemAccountType = a[i].type;
            AuthenticatorDescription ad = getAuthenticatorDescription(systemAccountType,
                    accountTypes);
            if (ad != null) {
                AccountData data = new AccountData(this, a[i].name, ad);

                // filter on accounts that support contacts
                if (contactAccountTypes.contains(a[i].type))
                    mAccounts.add(data);
            }
        }

        // unsync account
        AuthenticatorDescription adNull = new AuthenticatorDescription(mUnsyncType, UNSYNC_PKG, 0,
                0, 0, 0);
        AccountData aNull = new AccountData(this, mUnsyncName, adNull);
        mAccounts.add(aNull);

        // Update the account spinner
        mAccountAdapter.notifyDataSetChanged();
    }

    private AuthenticatorDescription getAuthenticatorDescription(String type,
            AuthenticatorDescription[] dictionary) {
        for (int i = 0; i < dictionary.length; i++) {
            if (dictionary[i].type.equals(type)) {
                return dictionary[i];
            }
        }
        // No match found
        return null;
    }

    private void updateAccountSelection() {
        // set selection to preferences
        if (!mPrefsSelected) {
            if (mPrefAccountName != null && mPrefAccountType != null) {
                int i = 0;
                for (AccountData acct : mAccounts) {
                    if (acct.getName().contentEquals(mPrefAccountName)
                            && acct.getType().contentEquals(mPrefAccountType)) {
                        mAccountSpinner.setSelection(i);
                    }
                    i++;
                }
            }
            mPrefsSelected = true;
        }

        // Read current account selection
        mSelectedAccount = (AccountData) mAccountSpinner.getSelectedItem();

        if (mSelectedAccount.getName().contentEquals(mUnsyncName)) {
            mSelectedAcctName = null;
            mSelectedAcctType = null;
        } else {
            mSelectedAcctName = mSelectedAccount.getName();
            mSelectedAcctType = mSelectedAccount.getType();
        }

        // save selected
        SafeSlingerPrefs.setAccountName(mSelectedAcctName);
        SafeSlingerPrefs.setAccountType(mSelectedAcctType);
    }

    private void showQuestion(String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            try {
                removeDialog(DIALOG_QUESTION);
                showDialog(DIALOG_QUESTION, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    private AlertDialog.Builder xshowQuestion(Activity act, Bundle args) {
        String msg = args.getString(extra.RESID_MSG);
        MyLog.i(TAG, msg);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_Question);
        ad.setMessage(msg);
        ad.setCancelable(false);
        ad.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                mCloseConfirmed = true;
                dialog.dismiss();
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        ad.setNegativeButton(R.string.btn_No, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    private void showProgress(String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            try {
                removeDialog(DIALOG_PROGRESS);
                showDialog(DIALOG_PROGRESS, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    private Dialog xshowProgress(Activity act, Bundle args) {
        String msg = args.getString(extra.RESID_MSG);
        MyLog.i(TAG, msg);

        if (mDlgProg != null) {
            mDlgProg = null;
            mProgressMsg = null;
        }
        mDlgProg = new ProgressDialog(act);
        mDlgProg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDlgProg.setMessage(msg);
        mProgressMsg = msg;
        mDlgProg.setCancelable(true);
        mDlgProg.setIndeterminate(true);
        mDlgProg.setProgress(0);
        mDlgProg.setCanceledOnTouchOutside(false);
        mDlgProg.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        mDlgProg.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });

        return mDlgProg;
    }

    private void endProgress() {
        if (mDlgProg != null) {
            mDlgProg.dismiss();
            mDlgProg = null;
        }
    }

    private void showHelp() {
        showHelp(getString(R.string.title_save), getString(R.string.help_save));
    }
}
