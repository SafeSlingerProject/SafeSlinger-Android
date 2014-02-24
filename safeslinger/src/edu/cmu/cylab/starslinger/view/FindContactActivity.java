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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
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
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;

import edu.cmu.cylab.keyslinger.lib.AccountData;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.ConfigData.extra;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;

public final class FindContactActivity extends BaseActivity {

    public static final int RESULT_CONTACTADD = 34;
    private static final int MENU_HELP = 170;
    private static final int MENU_FEEDBACK = 490;

    private ArrayList<AccountData> mAccounts;
    private EditText mEditTextName;
    private Button mButtonDone;
    private String mUnsyncName = null;
    private String mUnsyncType = null;
    private static final String UNSYNC_PKG = "unsynchronized";

    private static AccountData mSelectedAccount;
    private static String mSelectedName = null;

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
        mEditTextName = (EditText) findViewById(R.id.contactNameEditText);
        mButtonDone = (Button) findViewById(R.id.contactDoneButton);

        // read defaults and set them
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mSelectedName = extras.getString(extra.NAME);
        }

        // see if name is there...
        if (mSelectedName != null)
            mEditTextName.setText(mSelectedName);

        mAccounts = new ArrayList<AccountData>();
        updateAccountSelection();

        mEditTextName.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mSelectedName = mEditTextName.getText().toString();
            }
        });

        mButtonDone.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onDoneButtonClicked();
            }
        });

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

    private void onDoneButtonClicked() {
        String name = mEditTextName.getText().toString();

        if (ConfigData.isNameValid(name, getApplicationContext())) {

            // save preferences...
            ConfigData.savePrefContactName(this, name);

            Intent data = new Intent();
            String contactLookupKey = null;

            // check for existing email/phone next....
            contactLookupKey = getContactLookupKeyByData(name);

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

    private String createContactEntry() {
        // Get values from UI
        String name = mEditTextName.getText().toString();

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

    private void updateAccountSelection() {

        AccountManager accountManager = AccountManager.get(getApplicationContext());
        Account[] a = accountManager.getAccounts();
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

        mSelectedAccount = mAccounts.get(0); // use first item as default

        // save selected
        if (mSelectedAccount.getName().contentEquals(mUnsyncName)) {
            ConfigData.savePrefAccountName(this, null);
            ConfigData.savePrefAccountType(this, null);
        } else {
            ConfigData.savePrefAccountName(this, mSelectedAccount.getName());
            ConfigData.savePrefAccountType(this, mSelectedAccount.getType());
        }
    }

    private String getContactLookupKeyByData(String name) {

        // by name exact
        if (!TextUtils.isEmpty(name)) {
            String lookup = getContactLookupKeyByName(name);
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
