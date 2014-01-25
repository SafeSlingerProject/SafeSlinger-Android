
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
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;

import edu.cmu.cylab.keyslinger.lib.KsConfig.extra;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.R;

public class SaveActivity extends ContactActivity implements OnAccountsUpdateListener {

    private final ContactAccessor mAccessor = ContactAccessor.getInstance();
    private static final String TAG = KsConfig.LOG_TAG;
    private static final int MENU_HELP = 1;
    public static final int RESULT_SELNONE = 23;
    public static final int RESULT_SAVE = 24;

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
                showHelp(getString(R.string.title_save), getString(R.string.help_save));
                return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_SafeSlinger);
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mListTopOffset = 0;
            mListVisiblePos = 0;
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
                data = extras.getByteArray(extra.MEMBER_DATA + i);
                if (data != null) {
                    mMemData[i] = data;
                    i++;
                }
            } while (data != null);
        }

        // display names list so users can selectively choose which to save
        if (mMemData != null) {
            mContacts = parseVCards(mMemData);

            SaveContactAdapter mAdapter = new SaveContactAdapter(SaveActivity.this, mContacts);
            mListViewSaveContacts.setAdapter(mAdapter);

            // restore list position
            mListViewSaveContacts.setSelectionFromTop(mListVisiblePos, mListTopOffset);
        }

        mButtonSave.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                runThreadSaveSelectedContacts();
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
        mPrefAccountName = ConfigData.loadPrefAccountName(this);
        mPrefAccountType = ConfigData.loadPrefAccountType(this);

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
    }

    private void runThreadSaveSelectedContacts() {
        showProgress(getString(R.string.prog_SavingContactsToAddressBook), true);
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

        Thread t = new Thread() {

            @Override
            public void run() {
                saveSelectedContacts();
                hideProgress();
            }
        };
        t.start();
    }

    private void saveSelectedContacts() {
        StringBuilder errors = new StringBuilder();

        // save the contacts
        int selected = 0;
        Intent data = new Intent();
        int exchanged = mContacts.size();
        for (int i = 0; i < exchanged; i++) {

            final boolean checked = true;
            if (checked) {

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
                                    KsConfig.finalDecode(item.data.getBytes()));
                        }
                    }
                }
                if (isValidContact(mem)) {
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
                            while (c.moveToNext()) {
                                rawContactId = c.getString(c.getColumnIndex(Data.RAW_CONTACT_ID));
                            }
                            c.close();
                        }

                        // for an update we have to be careful to prevent import
                        // of duplicate data, so here we can query the aggregate
                        // contact for equivalently matching contact fields and
                        // prevent the import so we won't get duplicate phone
                        // numbers for example, one with international prefix
                        // and one without.
                        mem = loadContactDataNoDuplicates(this, contactLookupKey, mem, true);
                    }

                    if (!TextUtils.isEmpty(rawContactId)) {

                        if (!mAccessor.updateOldContact(mem, SaveActivity.this, mSelectedAcctType,
                                mSelectedAcctName, rawContactId)) {
                            errors.append("\n  ").append(name.toString()).append(" ")
                                    .append(getString(R.string.error_ContactUpdateFailed));
                            continue;
                        }
                    } else {
                        rawContactId = mAccessor.insertNewContact(mem, mSelectedAcctType,
                                mSelectedAcctName, SaveActivity.this);
                        if (!TextUtils.isEmpty(rawContactId)) {
                            contactLookupKey = getContactLookupKeyByContactId(rawContactId);
                        } else {
                            errors.append("\n  ").append(name.toString()).append(" ")
                                    .append(getString(R.string.error_ContactInsertFailed));
                            continue;
                        }
                    }

                    data.putExtra(extra.CONTACT_LOOKUP_KEY + selected, contactLookupKey);
                }
                selected++;

            }
        }

        // add redundant check for correct number of contacts imported...
        data.putExtra(extra.SELECTED_TOTAL, selected);
        data.putExtra(extra.EXCHANGED_TOTAL, exchanged);

        if (errors.length() > 0) {
            errors.insert(0, mSelectedAcctName);
            showNote(errors.toString());
            return;
        }

        if (selected == 0)
            setResultForParent(RESULT_SELNONE, data);
        else
            setResultForParent(RESULT_SAVE, data);
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
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void onDestroy() {
        // Remove AccountManager callback
        AccountManager.get(this).removeOnAccountsUpdatedListener(this);
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
        ConfigData.savePrefAccountName(this, mSelectedAcctName);
        ConfigData.savePrefAccountType(this, mSelectedAcctType);
    }
}
