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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.ContactsContract;
import android.view.WindowManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.ConfigData.extra;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.model.AccountData;
import edu.cmu.cylab.starslinger.model.IntegerListPreference;
import edu.cmu.cylab.starslinger.util.SSUtil;
import edu.cmu.cylab.starslinger.util.StorageOptions;

public class SettingsActivity extends SherlockPreferenceActivity {

    public static final int RESULT_NEW_PASSPHRASE = 5;
    private static final int VIEW_FILEDIR_ID = 6;
    private IntegerListPreference mPassPhraseCacheTtl = null;
    private CheckBoxPreference mRemindBackupDelay = null;
    private ListPreference mAccountNameType = null;
    private ListPreference mFileManagerDirectory = null;
    private EditTextPreference mContactName = null;
    private Preference mContactPubKeyId = null;
    private Preference mContactPushToken = null;
    private Preference mBackupRequestDate = null;
    private Preference mBackupCompleteDate = null;
    private Preference mRestoreCompleteDate = null;
    private Preference mShowLicense = null;
    private CheckBoxPreference mShowTutorial = null;
    private Preference mShowAbout = null;
    private Preference mChangePassphrase = null;
    private Preference mChangeDownloadDir = null;
    private ArrayList<AccountData> mAccounts = new ArrayList<AccountData>();
    private String mUnsyncName = null;
    private String mUnsyncType = null;
    private static final String UNSYNC_PKG = "unsynchronized";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);

        mUnsyncName = getString(R.string.label_None);
        mUnsyncType = getString(R.string.label_phoneOnly);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        addPreferencesFromResource(R.xml.ss_preferences);

        setupShowAbout();
        setupShowLicense();
        setupChangePassphrase();
        setupFileManagerRootDirectory();
        setupChangeDownloadDir();
        setupShowTutorial();
        setupPassPhraseCacheTtl();
        setupRemindBackupDelay();
        setupContactPushToken();
        setupContactPubKeyId();
        setupBackupRequestDate();
        setupBackupCompleteDate();
        setupRestoreCompleteDate();
        setupContactName();
        setupContactSyncAccount();
    }

    protected void setupContactSyncAccount() {
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        Account[] a = accountManager.getAccounts();

        mAccounts.clear();
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

        String prefAccountName = ConfigData.loadPrefAccountName(getApplicationContext());
        String prefAccountType = ConfigData.loadPrefAccountType(getApplicationContext());

        final CharSequence[] entries = new CharSequence[mAccounts.size()];
        CharSequence[] entryValues = new CharSequence[mAccounts.size()];

        int index = 0;
        int i = 0;
        for (AccountData acct : mAccounts) {
            entryValues[i] = String.valueOf(i);
            if (acct.getName().equals(mUnsyncName)) {
                entries[i] = mUnsyncName + "\n" + mUnsyncType;
            } else {
                entries[i] = acct.getName() + "\n" + acct.getType();
            }

            if (acct.getName().equals(prefAccountName) && acct.getType().equals(prefAccountType)) {
                index = i;
            } else if (acct.getName().equals(mUnsyncName) && prefAccountName == null) {
                index = i;
            }
            i++;
        }

        mAccountNameType = (ListPreference) findPreference(ConfigData.pref.TEMPKEY_SYNCACCOUNT_LIST);
        mAccountNameType.setEntries(entries);
        mAccountNameType.setSummary(prefAccountName);
        mAccountNameType.setEntryValues(entryValues);
        if (mAccounts.size() < index) {
            mAccountNameType.setValueIndex(index);
        }
        mAccountNameType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int newIndex = Integer.valueOf((String) newValue);
                mAccountNameType.setSummary(mAccounts.get(newIndex).getName());
                mAccountNameType.setValueIndex(newIndex);

                // save selected
                if (mAccounts.get(newIndex).getName().contentEquals(mUnsyncName)) {
                    ConfigData.savePrefAccountName(getApplicationContext(), null);
                    ConfigData.savePrefAccountType(getApplicationContext(), null);
                } else {
                    ConfigData.savePrefAccountName(getApplicationContext(), mAccounts.get(newIndex)
                            .getName());
                    ConfigData.savePrefAccountType(getApplicationContext(), mAccounts.get(newIndex)
                            .getType());
                }
                return false;
            }
        });
    }

    protected void setupFileManagerRootDirectory() {

        StorageOptions.determineStorageOptions(this);
        final int count = StorageOptions.count;
        final String[] labels = StorageOptions.labels;
        final String[] paths = StorageOptions.paths;
        final CharSequence[] entries = new CharSequence[count];
        final CharSequence[] entryValues = new CharSequence[count];
        final String selection = ConfigData.loadPrefFileManagerRootDir(getApplicationContext());

        int index = 0;
        int i = 0;
        for (String path : paths) {
            entryValues[i] = String.valueOf(i);
            entries[i] = labels[i] + "\n" + path;
            if (path.equalsIgnoreCase(selection)) {
                index = i;
            }
            i++;
        }

        mFileManagerDirectory = (ListPreference) findPreference(ConfigData.pref.FILEMANAGER_ROOTDIR);
        mFileManagerDirectory.setEntries(entries);
        mFileManagerDirectory.setSummary(selection);
        mFileManagerDirectory.setEntryValues(entryValues);
        if (count < index) {
            mAccountNameType.setValueIndex(index);
        }
        mFileManagerDirectory
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        int newIndex = Integer.valueOf((String) newValue);
                        mFileManagerDirectory.setSummary(paths[newIndex]);
                        mFileManagerDirectory.setValueIndex(newIndex);

                        // save selected
                        ConfigData.savePrefFileManagerRootDir(getApplicationContext(),
                                paths[newIndex]);
                        return false;
                    }
                });
    }

    protected void setupShowAbout() {
        mShowAbout = findPreference(ConfigData.pref.SHOW_ABOUT);
        mShowAbout.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!isFinishing()) {
                    removeDialog(BaseActivity.DIALOG_ABOUT);
                    showDialog(BaseActivity.DIALOG_ABOUT);
                }
                return false;
            }
        });
    }

    protected void setupShowLicense() {
        mShowLicense = findPreference(ConfigData.pref.SHOW_LICENSE);
        mShowLicense.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!isFinishing()) {
                    removeDialog(BaseActivity.DIALOG_LICENSE);
                    showDialog(BaseActivity.DIALOG_LICENSE);
                }
                return false;
            }
        });
    }

    protected void setupChangePassphrase() {
        mChangePassphrase = findPreference(ConfigData.pref.CHANGE_PASSPHRASE);
        mChangePassphrase.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                setResult(RESULT_NEW_PASSPHRASE);
                finish();
                return true;
            }
        });
    }

    protected void setupChangeDownloadDir() {
        mChangeDownloadDir = findPreference(ConfigData.pref.DOWNLOAD_DIRECTORY);
        mChangeDownloadDir.setSummary(ConfigData.loadPrefDownloadDir(getApplicationContext()));
        mChangeDownloadDir.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                showFileDir();
                return true;
            }
        });
    }

    private void showFileDir() {
        if (SSUtil.isExternalStorageWritable()) {
            Intent intent = new Intent(SettingsActivity.this, FileSaveActivity.class);
            startActivityForResult(intent, VIEW_FILEDIR_ID);
        } else {
            showNote(R.string.error_FileStorageUnavailable);
        }
    }

    protected void setupShowTutorial() {
        mShowTutorial = (CheckBoxPreference) findPreference(ConfigData.pref.SHOW_WALKTHROUGH);
        mShowTutorial.setChecked(ConfigData.loadPrefShowWalkthrough(getApplicationContext()));
        mShowTutorial.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mShowTutorial.setChecked((Boolean) newValue);
                ConfigData.savePrefShowWalkthrough(getApplicationContext(), (Boolean) newValue);
                return false;
            }
        });
    }

    protected void setupRemindBackupDelay() {
        mRemindBackupDelay = (CheckBoxPreference) findPreference(ConfigData.pref.REMIND_BACKUP_DELAY);
        mRemindBackupDelay
                .setChecked(ConfigData.loadPrefRemindBackupDelay(getApplicationContext()));
        mRemindBackupDelay
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mRemindBackupDelay.setChecked((Boolean) newValue);
                        ConfigData.savePrefRemindBackupDelay(getApplicationContext(),
                                (Boolean) newValue);
                        return false;
                    }
                });
    }

    protected void setupPassPhraseCacheTtl() {
        mPassPhraseCacheTtl = (IntegerListPreference) findPreference(ConfigData.pref.PASSPHRASE_CACHE_TTL);
        mPassPhraseCacheTtl.setValue(""
                + ConfigData.loadPrefPassPhraseCacheTtl(getApplicationContext()));
        mPassPhraseCacheTtl.setSummary(mPassPhraseCacheTtl.getEntry());
        mPassPhraseCacheTtl
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mPassPhraseCacheTtl.setValue(newValue.toString());
                        mPassPhraseCacheTtl.setSummary(mPassPhraseCacheTtl.getEntry());
                        ConfigData.savePrefPassPhraseCacheTtl(getApplicationContext(),
                                Integer.parseInt(newValue.toString()));
                        SafeSlinger.startCacheService(SettingsActivity.this);
                        return false;
                    }
                });
    }

    protected void setupContactName() {
        mContactName = (EditTextPreference) findPreference(ConfigData.pref.CONTACT_NAME);
        mContactName.setText(ConfigData.loadPrefContactName(getApplicationContext()));
        mContactName.setSummary(ConfigData.loadPrefContactName(getApplicationContext()));
        mContactName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mContactName.setText((String) newValue);
                mContactName.setSummary((String) newValue);
                ConfigData.savePrefContactName(getApplicationContext(), (String) newValue);
                return false;
            }
        });
    }

    protected void setupContactPushToken() {
        mContactPushToken = findPreference(ConfigData.pref.PUSH_REGISTRATION_ID_DIRECT);
        mContactPushToken.setEnabled(false);
        mContactPushToken
                .setSummary(ConfigData.loadPrefPushRegistrationId(getApplicationContext()));
    }

    protected void setupContactPubKeyId() {
        mContactPubKeyId = findPreference(ConfigData.pref.KEYID_STRING);
        mContactPubKeyId.setEnabled(false);
        mContactPubKeyId.setSummary(ConfigData.loadPrefKeyIdString(getApplicationContext()));
    }

    protected void setupBackupRequestDate() {
        mBackupRequestDate = findPreference(ConfigData.pref.BACKUP_REQUEST_DATE);
        mBackupRequestDate.setEnabled(false);
        long date = ConfigData.loadPrefbackupRequestDate(getApplicationContext());
        String str = date > 0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                Locale.getDefault()).format(new Date(date)) : getString(R.string.label_None);
        mBackupRequestDate.setSummary(str);
    }

    protected void setupBackupCompleteDate() {
        mBackupCompleteDate = findPreference(ConfigData.pref.BACKUP_COMPLETE_DATE);
        mBackupCompleteDate.setEnabled(false);
        long date = ConfigData.loadPrefbackupCompleteDate(getApplicationContext());
        String str = date > 0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                Locale.getDefault()).format(new Date(date)) : getString(R.string.label_None);
        mBackupCompleteDate.setSummary(str);
    }

    protected void setupRestoreCompleteDate() {
        mRestoreCompleteDate = findPreference(ConfigData.pref.RESTORE_COMPLETE_DATE);
        mRestoreCompleteDate.setEnabled(false);
        long date = ConfigData.loadPrefrestoreCompleteDate(getApplicationContext());
        String str = date > 0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                Locale.getDefault()).format(new Date(date)) : getString(R.string.label_None);
        mRestoreCompleteDate.setSummary(str);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case VIEW_FILEDIR_ID:
                switch (resultCode) {
                    case RESULT_OK:
                        String chosenPath = data.getStringExtra(extra.FPATH);
                        ConfigData.savePrefDownloadDir(getApplicationContext(), chosenPath);
                        mChangeDownloadDir = findPreference(ConfigData.pref.DOWNLOAD_DIRECTORY);
                        mChangeDownloadDir.setSummary(ConfigData
                                .loadPrefDownloadDir(getApplicationContext()));
                        break;
                    case RESULT_CANCELED:
                        break;
                }
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case BaseActivity.DIALOG_ABOUT:
                return BaseActivity.xshowAbout(SettingsActivity.this).create();
            case BaseActivity.DIALOG_LICENSE:
                return BaseActivity.xshowLicense(SettingsActivity.this).create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void onUserInteraction() {
        // update the cache timeout
        SafeSlinger.updateCachedPassPhrase(ConfigData.loadPrefKeyIdString(getApplicationContext()));
    }

    private void showNote(int resId) {
        Toast.makeText(SettingsActivity.this, resId, Toast.LENGTH_LONG).show();
    }
}
