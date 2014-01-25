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

import java.util.ArrayList;
import java.util.Iterator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.OnAccountsUpdateListener;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncAdapterType;
import android.database.sqlite.SQLiteFullException;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.app.ActionBar;

import edu.cmu.cylab.keyslinger.lib.AccountData;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.ConfigData.extra;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;

public final class FindContactActivity extends BaseActivity implements OnAccountsUpdateListener {

    public static final int RESULT_CONTACTADD = 34;
    private static final int MENU_HELP = 170;
    private static final int MENU_FEEDBACK = 490;

    private ArrayList<AccountData> mAccounts;
    private AccountAdapter mAccountAdapter;
    private Spinner mSpinnerAccount;
    private EditText mEditTextEmail;
    private ArrayList<Integer> mContactEmailTypes;
    private Spinner mSpinnerEmailType;
    private EditText mEditTextName;
    private EditText mEditTextPhone;
    private ArrayList<Integer> mContactPhoneTypes;
    private Spinner mSpinnerPhoneType;
    private Button mButtonDone;
    private String mUnsyncName = null;
    private String mUnsyncType = null;
    private static final String UNSYNC_PKG = "unsynchronized";

    private static AccountData mSelectedAccount;
    private static String mSelectedName = null;
    private static String mSelectedPhone = null;
    private static String mSelectedEmail = null;
    private static int mSelectedPhoneType = Phone.TYPE_MOBILE;
    private static int mSelectedEmailType = Email.TYPE_HOME;

    /**
     * Called when the activity is first created. Responsible for initializing
     * the UI.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_SafeSlinger);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contact_adder);
        mUnsyncName = getString(R.string.label_None);
        mUnsyncType = getString(R.string.label_phoneOnly);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.app_name);
        bar.setSubtitle(R.string.title_find);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Obtain handles to UI objects
        mSpinnerAccount = (Spinner) findViewById(R.id.accountSpinner);
        mEditTextName = (EditText) findViewById(R.id.contactNameEditText);
        mEditTextPhone = (EditText) findViewById(R.id.contactPhoneEditText);
        mEditTextEmail = (EditText) findViewById(R.id.contactEmailEditText);
        mSpinnerPhoneType = (Spinner) findViewById(R.id.contactPhoneTypeSpinner);
        mSpinnerEmailType = (Spinner) findViewById(R.id.contactEmailTypeSpinner);
        mButtonDone = (Button) findViewById(R.id.contactDoneButton);

        // read defaults and set them
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mSelectedName = extras.getString(extra.NAME);
            mSelectedPhone = extras.getString(extra.PHONE);
            mSelectedEmail = extras.getString(extra.EMAIL);
            mSelectedPhoneType = extras.getInt(extra.PHONE_TYPE);
            mSelectedEmailType = extras.getInt(extra.EMAIL_TYPE);
        }

        // see if phone is there, or look it up...
        if (mSelectedPhone != null) {
            mEditTextPhone.setText(mSelectedPhone);
        } else {
            mSelectedPhone = getMyPhoneNumber();
            if (mSelectedPhone != null) {
                mEditTextPhone.setText(PhoneNumberUtils.formatNumber(mSelectedPhone));
                mSelectedPhoneType = Phone.TYPE_MOBILE;
            }
        }

        // see if email is there...
        if (mSelectedEmail != null) {
            mEditTextEmail.setText(mSelectedEmail);
        }

        // see if name is there...
        if (mSelectedName != null)
            mEditTextName.setText(mSelectedName);

        // Prepare list of supported account types
        mContactPhoneTypes = new ArrayList<Integer>();
        mContactPhoneTypes.add(Phone.TYPE_MOBILE);
        mContactPhoneTypes.add(Phone.TYPE_HOME);
        mContactPhoneTypes.add(Phone.TYPE_WORK);
        mContactPhoneTypes.add(Phone.TYPE_OTHER);
        mContactEmailTypes = new ArrayList<Integer>();
        mContactEmailTypes.add(Email.TYPE_HOME);
        mContactEmailTypes.add(Email.TYPE_WORK);
        mContactEmailTypes.add(Email.TYPE_MOBILE);
        mContactEmailTypes.add(Email.TYPE_OTHER);

        // Prepare model for account spinner
        mAccounts = new ArrayList<AccountData>();
        mAccountAdapter = new AccountAdapter(this, this, mAccounts);
        mSpinnerAccount.setAdapter(mAccountAdapter);

        // Populate list of account types for phone
        ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Iterator<Integer> iter;
        iter = mContactPhoneTypes.iterator();
        while (iter.hasNext()) {
            adapter.add(getPhoneTypeLabel(iter.next()));
        }
        mSpinnerPhoneType.setAdapter(adapter);
        mSpinnerPhoneType.setPrompt(getString(R.string.label_selectLabel));
        mSpinnerPhoneType.setSelection(adapter.getPosition(getPhoneTypeLabel(mSelectedPhoneType)));

        // Populate list of account types for email
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        iter = mContactEmailTypes.iterator();
        while (iter.hasNext()) {
            adapter.add(getEmailTypeLabel(iter.next()));
        }
        mSpinnerEmailType.setAdapter(adapter);
        mSpinnerEmailType.setPrompt(getString(R.string.label_selectLabel));
        mSpinnerEmailType.setSelection(adapter.getPosition(getEmailTypeLabel(mSelectedEmailType)));

        // Prepare the system account manager. On registering the listener
        // below, we also ask for
        // an initial callback to pre-populate the account list.
        AccountManager.get(this).addOnAccountsUpdatedListener(this, null, true);

        // Register handlers for UI elements
        mSpinnerAccount.setOnItemSelectedListener(new OnItemSelectedListener() {

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
        mEditTextName.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mSelectedName = mEditTextName.getText().toString();
            }
        });
        mEditTextPhone.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mSelectedPhone = mEditTextPhone.getText().toString();
            }
        });
        mEditTextEmail.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mSelectedEmail = mEditTextEmail.getText().toString();
            }
        });
        mSpinnerPhoneType.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long i) {
                mSelectedPhoneType = mContactPhoneTypes.get(position);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no change
            }
        });
        mSpinnerEmailType.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long i) {
                mSelectedEmailType = mContactEmailTypes.get(position);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no change
            }
        });
        mButtonDone.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onDoneButtonClicked();
            }
        });

        mEditTextEmail.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onDoneButtonClicked();
                    return true;
                }
                return false;
            }
        });

    }

    private String getPhoneTypeLabel(int type) {
        switch (type) {
            case Phone.TYPE_HOME:
                return getString(R.string.label_hometag);
            case Phone.TYPE_WORK:
                return getString(R.string.label_worktag);
            case Phone.TYPE_MOBILE:
                return getString(R.string.label_mobiletag);
            case Phone.TYPE_OTHER:
                return getString(R.string.label_othertag);
            default:
                return Phone.getTypeLabel(this.getResources(), type,
                        getString(R.string.label_undefinedTypeLabel)).toString();
        }
    }

    private String getEmailTypeLabel(int type) {
        switch (type) {
            case Email.TYPE_HOME:
                return getString(R.string.label_hometag);
            case Email.TYPE_WORK:
                return getString(R.string.label_worktag);
            case Email.TYPE_MOBILE:
                return getString(R.string.label_mobiletag);
            case Email.TYPE_OTHER:
                return getString(R.string.label_othertag);
            default:
                return Email.getTypeLabel(this.getResources(), type,
                        getString(R.string.label_undefinedTypeLabel)).toString();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {

        int showAsActionAlways = com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

        menu.add(0, MENU_HELP, 0, R.string.menu_Help).setIcon(R.drawable.ic_action_help)
                .setShowAsAction(showAsActionAlways);
        menu.add(0, MENU_FEEDBACK, 0, R.string.menu_sendFeedback).setIcon(
                android.R.drawable.ic_menu_send);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HELP:
                showHelp(getString(R.string.title_find), getString(R.string.help_find));
                return true;
            case MENU_FEEDBACK:
                SafeSlinger.getApplication().showFeedbackEmail(FindContactActivity.this);
                return true;
        }
        return false;
    }

    /**
     * Actions for when the Search button is clicked. Creates a contact entry
     * and terminates the activity.
     */
    private void onDoneButtonClicked() {
        String name = mEditTextName.getText().toString();

        if (ConfigData.isNameValid(name, getApplicationContext())) {
            String phone = mEditTextPhone.getText().toString();
            String email = mEditTextEmail.getText().toString();
            int phoneType = mContactPhoneTypes.get(mSpinnerPhoneType.getSelectedItemPosition());
            int emailType = mContactEmailTypes.get(mSpinnerEmailType.getSelectedItemPosition());

            // save preferences...
            ConfigData.savePrefContactName(this, name);
            ConfigData.savePrefContactPhone(this, phone);
            ConfigData.savePrefContactEmail(this, email);
            ConfigData.savePrefContactPhoneType(this, phoneType);
            ConfigData.savePrefContactEmailType(this, emailType);

            Intent data = new Intent();
            String contactLookupKey = null;

            // check for existing email/phone next....
            contactLookupKey = getContactLookupKeyByData(name, null, null);

            // last option, generate a new contact...
            if (TextUtils.isEmpty(contactLookupKey)) {
                contactLookupKey = createContactEntry();
            }

            if (TextUtils.isEmpty(contactLookupKey)) {
                // as a last resort, we can pick from contact activity
                setResult(RESULT_CONTACTADD);
                finish();

            } else {
                data.putExtra(extra.CONTACT_LOOKUP_KEY, contactLookupKey);
                setResult(RESULT_OK, data);
                finish();
            }
        } else {
            showNote(R.string.error_InvalidContactName);
        }
    }

    /**
     * Creates a contact entry from the current UI values in the account named
     * by mselectedAccount.
     */
    private String createContactEntry() {
        // Get values from UI
        String name = mEditTextName.getText().toString();
        String phone = mEditTextPhone.getText().toString();
        String email = mEditTextEmail.getText().toString();
        int phoneType = mContactPhoneTypes.get(mSpinnerPhoneType.getSelectedItemPosition());
        int emailType = mContactEmailTypes.get(mSpinnerEmailType.getSelectedItemPosition());

        // Prepare contact creation request
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, mSelectedAccount.getType())
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, mSelectedAccount.getName())
                .build());
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, name).build());
        if (!TextUtils.isEmpty(phone)) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, phone).withValue(Phone.TYPE, phoneType).build());
        }
        if (!TextUtils.isEmpty(email)) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE).withValue(Email.DATA, email)
                    .withValue(Email.TYPE, emailType).build());
        }

        // Ask the Contact provider to create a new contact
        try {
            ContentProviderResult[] results = getContentResolver().applyBatch(
                    ContactsContract.AUTHORITY, ops);
            if ((results != null) && (results.length > 0)) {
                long rawContactId = ContentUris.parseId(results[0].uri);
                return getContactLookupKeyByContactId(Long.toString(rawContactId));
            } else {
                return getContactLookupKeyByName(name);
            }
        } catch (IllegalArgumentException e) {
            // Unknown authority com.android.contacts, we need a database to
            // save the contact
            showNote(e.getLocalizedMessage());
            return null;
        } catch (SQLiteFullException e) {
            // An exception that indicates that the SQLite database is full.
            // Phone Memory is really low.Can't Insert or update on Contacts DB
            showNote(e.getLocalizedMessage());
            return null;
        } catch (RemoteException e) {
            // Parent exception for all Binder remote-invocation errors
            showNote(e.getLocalizedMessage());
            return null;
        } catch (OperationApplicationException e) {
            // Thrown when an application of a ContentProviderOperation fails
            // due the specified constraints.
            showNote(e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Called when this activity is about to be destroyed by the system.
     */
    @Override
    public void onDestroy() {
        // Remove AccountManager callback
        AccountManager.get(this).removeOnAccountsUpdatedListener(this);
        super.onDestroy();
    }

    /**
     * Updates account list spinner when the list of Accounts on the system
     * changes. Satisfies OnAccountsUpdateListener implementation.
     */
    @Override
    public void onAccountsUpdated(Account[] a) {
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

    /**
     * Obtain the AuthenticatorDescription for a given account type.
     * 
     * @param type The account type to locate.
     * @param dictionary An array of AuthenticatorDescriptions, as returned by
     *            AccountManager.
     * @return The description for the specified account type.
     */
    private static AuthenticatorDescription getAuthenticatorDescription(String type,
            AuthenticatorDescription[] dictionary) {
        for (int i = 0; i < dictionary.length; i++) {
            if (dictionary[i].type.equals(type)) {
                return dictionary[i];
            }
        }
        // No match found
        return null;
    }

    /**
     * Update account selection. If NO_ACCOUNT is selected, then we prohibit
     * inserting new contacts.
     */
    private void updateAccountSelection() {
        // Read current account selection
        mSelectedAccount = (AccountData) mSpinnerAccount.getSelectedItem();
        String prefAccountName = mSelectedAccount.getName();
        String prefAccountType = mSelectedAccount.getType();

        // update email address only if we need one...
        if (!mSelectedAccount.getName().contentEquals(mUnsyncName)
                && TextUtils.isEmpty(mEditTextEmail.getText())) {
            mEditTextEmail.setText(prefAccountName);
        }

        // save selected
        if (mSelectedAccount.getName().contentEquals(mUnsyncName)) {
            ConfigData.savePrefAccountName(this, null);
            ConfigData.savePrefAccountType(this, null);
        } else {
            ConfigData.savePrefAccountName(this, mSelectedAccount.getName());
            ConfigData.savePrefAccountType(this, mSelectedAccount.getType());
        }

        // set selection to preferences
        if (TextUtils.isEmpty(ConfigData.loadPrefAccountName(this))) {
            int i = 0;
            for (AccountData acct : mAccounts) {
                if (acct.getName().contentEquals(prefAccountName)
                        && acct.getType().contentEquals(prefAccountType)) {
                    mSpinnerAccount.setSelection(i);
                }
                i++;
            }
        }

        // now that email is potentially found, search for name...
        String name = mEditTextName.getText().toString();
        if (TextUtils.isEmpty(name)) {
            String phone = mEditTextPhone.getText().toString();
            String email = mEditTextEmail.getText().toString();
            String contactLookupKey = getContactLookupKeyByData(null, phone, email);
            if (!TextUtils.isEmpty(contactLookupKey)) {
                mEditTextName.setText(getContactName(contactLookupKey));
            }
        }
    }

    private String getContactLookupKeyByData(String name, String phone, String email) {

        // by name exact
        if (!TextUtils.isEmpty(name)) {
            String lookup = getContactLookupKeyByName(name);
            if (!TextUtils.isEmpty(lookup)) {
                return lookup;
            }
        }

        // then by email exact
        if (!TextUtils.isEmpty(email)) {
            String lookup = getContactLookupKeyByEmail(email);
            if (!TextUtils.isEmpty(lookup)) {
                return lookup;
            }
        }

        // then by phone exact
        if (!TextUtils.isEmpty(phone)) {
            String lookup = getContactLookupKeyByPhone(phone);
            if (!TextUtils.isEmpty(lookup)) {
                return lookup;
            }
        }

        return null;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(FindContactActivity.this, args).create();
        }
        return super.onCreateDialog(id);
    }
}
