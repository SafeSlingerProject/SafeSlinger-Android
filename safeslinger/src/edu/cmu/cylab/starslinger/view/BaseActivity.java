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
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import edu.cmu.cylab.keyslinger.lib.KsConfig;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.ConfigData.extra;
import edu.cmu.cylab.starslinger.Eula;
import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.CryptToolsLegacy;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPeerKeyFormatException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider;
import edu.cmu.cylab.starslinger.model.AccountData;
import edu.cmu.cylab.starslinger.model.ContactImpp;
import edu.cmu.cylab.starslinger.model.CryptoMsgPrivateData;
import edu.cmu.cylab.starslinger.model.MessageDbAdapter;
import edu.cmu.cylab.starslinger.model.MessageRow;
import edu.cmu.cylab.starslinger.model.PushTokenKeyDateComparator;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientRow;
import edu.cmu.cylab.starslinger.model.SlingerContact;
import edu.cmu.cylab.starslinger.model.SlingerIdentity;
import edu.cmu.cylab.starslinger.model.ThreadData;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class BaseActivity extends SherlockFragmentActivity {
    private static final String GOOGLE_TRANSLATE = "Google Translate";
    public static final int RESULT_NEW_PASSPHRASE = 12358971;
    private static final String TAG = ConfigData.LOG_TAG;
    public static final int DIALOG_HELP = 1;
    public static final int DIALOG_ERREXIT = 2;
    public static final int DIALOG_QUESTION = 3;
    public static final int DIALOG_INTRO = 4;
    public static final int DIALOG_ABOUT = 5;
    public static final int DIALOG_LICENSE = 6;
    public static final int DIALOG_TUTORIAL = 7;
    public static final int DIALOG_LICENSE_CONFIRM = 8;
    public static final int DIALOG_PROGRESS = 9;
    public static final int DIALOG_USEROPTIONS = 10;
    public static final int DIALOG_FILEOPTIONS = 11;
    public static final int DIALOG_LOAD_FILE = 12;
    public static final int DIALOG_TEXT_ENTRY = 13;
    public static final int DIALOG_REFERENCE = 14;
    public static final int DIALOG_BACKUPQUERY = 15;

    /**
     * For start activity KeySlinger with the necessary parameters.
     */
    protected Bundle writeSingleExportExchangeArgs(ContactImpp out) {

        Bundle args = new Bundle();
        args.putString(ConfigData.extra.CONTACT_LOOKUP_KEY, out.lookup);

        for (int i = 0; i < out.impps.size(); i++) {
            args.putString(KsConfig.extra.CONTACT_KEYNAME_PREFIX + i, out.impps.get(i).k);
            args.putByteArray(KsConfig.extra.CONTACT_VALUE_PREFIX + i, out.impps.get(i).v);
        }

        return args;
    }

    public Uri getPersonUri(String contactLookupKey) {
        if (!TextUtils.isEmpty(contactLookupKey)) {
            Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                    contactLookupKey);
            if (lookupUri != null) {
                return ContactsContract.Contacts.lookupContact(getContentResolver(), lookupUri);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public Uri getDataUri(String contactLookupKey) {
        if (!TextUtils.isEmpty(contactLookupKey)) {
            Uri personUri = getPersonUri(contactLookupKey);
            if (personUri != null) {
                return Uri.withAppendedPath(personUri, Contacts.Data.CONTENT_DIRECTORY);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Retrieve a user's cell phone numbers.
     */
    protected ArrayList<String> getContactMobilePhones(String contactLookupKey) {
        ArrayList<String> phones = new ArrayList<String>();
        if (TextUtils.isEmpty(contactLookupKey)) {
            return phones;
        }

        String where = Data.MIMETYPE + " = ? AND " + Phone.TYPE + " = ?";
        String[] whereParameters = new String[] {
                Phone.CONTENT_ITEM_TYPE, String.valueOf((Phone.TYPE_MOBILE))
        };

        Uri dataUri = getDataUri(contactLookupKey);
        if (dataUri != null) {
            Cursor c = getContentResolver().query(dataUri, null, where, whereParameters, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String phone = c.getString(c.getColumnIndexOrThrow(Phone.NUMBER));
                    if (!phones.contains(phone))
                        phones.add(phone);
                }
                c.close();
            }
        }
        return phones;
    }

    /**
     * Retrieve a user's application key, based on the key name.
     */
    protected String getContactName(String contactLookupKey) {
        String name = "";
        if (TextUtils.isEmpty(contactLookupKey)) {
            return null;
        }

        String where = Data.MIMETYPE + " = ?";
        String[] whereParameters = new String[] {
            StructuredName.CONTENT_ITEM_TYPE
        };

        Uri dataUri = getDataUri(contactLookupKey);
        if (dataUri != null) {
            Cursor c = getContentResolver().query(dataUri, null, where, whereParameters, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String newname = c.getString(c
                            .getColumnIndexOrThrow(StructuredName.DISPLAY_NAME));
                    boolean super_primary = (c.getInt(c
                            .getColumnIndexOrThrow(StructuredName.IS_SUPER_PRIMARY)) != 0);
                    if ((TextUtils.isEmpty(name) || super_primary)) {
                        name = newname;
                    }
                }
                c.close();
            }
        }
        return TextUtils.isEmpty(name) ? null : name;
    }

    /**
     * Retrieve a user's phone numbers.
     * 
     * @param defaultNumber
     */
    protected ArrayList<String> getContactPhones(String contactLookupKey, String defaultNumber) {
        ArrayList<String> phones = new ArrayList<String>();
        if (TextUtils.isEmpty(contactLookupKey)) {
            return phones;
        }

        String where = Data.MIMETYPE + " = ?";
        String[] whereParameters = new String[] {
            Phone.CONTENT_ITEM_TYPE
        };

        if (defaultNumber != null)
            phones.add(PhoneNumberUtils.stripSeparators(PhoneNumberUtils
                    .formatNumber(defaultNumber)));

        Uri dataUri = getDataUri(contactLookupKey);
        if (dataUri != null) {
            Cursor c = getContentResolver().query(dataUri, null, where, whereParameters, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String phone = c.getString(c.getColumnIndexOrThrow(Phone.NUMBER));
                    boolean unique = true;
                    for (String cmpPhone : phones) {
                        if (PhoneNumberUtils.compare(cmpPhone, phone))
                            unique = false;
                    }
                    if (unique)
                        phones.add(PhoneNumberUtils.stripSeparators(PhoneNumberUtils
                                .formatNumber(phone)));
                }
                c.close();
            }
        }
        return phones;
    }

    protected ArrayList<String> getContactEmails(String contactLookupKey) {
        ArrayList<String> emails = new ArrayList<String>();
        if (TextUtils.isEmpty(contactLookupKey)) {
            return emails;
        }

        String where = Data.MIMETYPE + " = ?";
        String[] whereParameters = new String[] {
            Email.CONTENT_ITEM_TYPE
        };

        Uri dataUri = getDataUri(contactLookupKey);
        if (dataUri != null) {
            Cursor c = getContentResolver().query(dataUri, null, where, whereParameters, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String email = c.getString(c.getColumnIndexOrThrow(Email.DATA));
                    boolean unique = true;
                    for (String cmpEmail : emails) {
                        if (PhoneNumberUtils.compare(cmpEmail, email))
                            unique = false;
                    }
                    if (unique)
                        emails.add(email);
                }
                c.close();
            }
        }
        return emails;
    }

    /**
     * Retrieve a user's photo.
     */
    protected byte[] getContactPhoto(String contactLookupKey) {
        byte[] photo = null;
        if (TextUtils.isEmpty(contactLookupKey)) {
            return photo;
        }

        String where = Data.MIMETYPE + " = ?";
        String[] whereParameters = new String[] {
            Photo.CONTENT_ITEM_TYPE
        };

        Uri dataUri = getDataUri(contactLookupKey);
        if (dataUri != null) {
            Cursor c = getContentResolver().query(dataUri, null, where, whereParameters, null);
            if (c != null) {
                while (c.moveToNext()) {
                    byte[] newphoto = c.getBlob(c.getColumnIndexOrThrow(Photo.PHOTO));
                    boolean super_primary = (c.getInt(c
                            .getColumnIndexOrThrow(Photo.IS_SUPER_PRIMARY)) != 0);
                    if (newphoto != null && (photo == null || super_primary)) {
                        photo = newphoto;
                    }
                }
                c.close();
            }
        }
        return photo;
    }

    /**
     * Retrieve the user data that uses the same public key.
     */
    public String getSignersContact(String publicKeyId) {
        if (TextUtils.isEmpty(publicKeyId)) {
            return null;
        }

        RecipientRow recip = null;
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(getApplicationContext());
        Cursor c = dbRecipient.fetchRecipientByKeyId(publicKeyId);
        if (c != null) {
            recip = new RecipientRow(c);
            c.close();
        }

        if (recip != null) {
            return new String(recip.getPubkey());
        }
        return null;
    }

    protected int getTotalUniqueAddressBookContacts() {
        int count = 0;
        Cursor c = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null,
                null, null);
        if (c != null) {
            count = c.getCount();
            c.close();
        }
        return count;
    }

    protected int getTotalUniqueRecipientContacts(List<RecipientRow> mcontacts) {
        ArrayList<String> u = new ArrayList<String>();
        for (RecipientRow r : mcontacts) {
            if (!TextUtils.isEmpty(r.getName()) && !u.contains(r.getName())) {
                u.add(r.getName());
            }
        }
        return u.size();
    }

    protected String getContactIdByName(String name) {
        String contactId = null;
        if (TextUtils.isEmpty(name)) {
            return contactId;
        }

        // find aggregated contact
        String[] whereParameters = new String[] {
                StructuredName.DISPLAY_NAME, StructuredName.CONTACT_ID
        };
        String where = StructuredName.DISPLAY_NAME + " = "
                + DatabaseUtils.sqlEscapeString("" + name);
        Cursor c = getContentResolver().query(Data.CONTENT_URI, whereParameters, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                String tempId = c.getString(c.getColumnIndexOrThrow(StructuredName.CONTACT_ID));
                String tempName = c.getString(c.getColumnIndexOrThrow(StructuredName.DISPLAY_NAME));
                // String tempName = getContactName(tempId);
                if (!TextUtils.isEmpty(tempName) && name.compareToIgnoreCase(tempName) == 0) {
                    contactId = tempId;
                    c.close();
                    return contactId;
                }
            }
            c.close();
        }

        return contactId;
    }

    protected String getContactLookupKeyByContactId(String contactId) {
        if (TextUtils.isEmpty(contactId)) {
            return null;
        }

        String where = Data.CONTACT_ID + " = ?";
        String[] whereParameters = new String[] {
            contactId
        };

        Cursor c = getContentResolver().query(Data.CONTENT_URI, null, where, whereParameters, null);
        if (c != null) {
            while (c.moveToNext()) {
                String lookup = c.getString(c.getColumnIndexOrThrow(Data.LOOKUP_KEY));
                c.close();
                return lookup;
            }
            c.close();
        }
        return null;
    }

    protected String getContactLookupKeyByName(String name) {
        String contactLookupKey = null;
        if (TextUtils.isEmpty(name)) {
            return contactLookupKey;
        }

        // find aggregated contact
        String[] whereParameters = new String[] {
                StructuredName.DISPLAY_NAME, StructuredName.LOOKUP_KEY
        };
        String where = StructuredName.DISPLAY_NAME + " = "
                + DatabaseUtils.sqlEscapeString("" + name);
        Cursor c = getContentResolver().query(Data.CONTENT_URI, whereParameters, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                String tempLookup = c.getString(c.getColumnIndexOrThrow(StructuredName.LOOKUP_KEY));
                String tempName = c.getString(c.getColumnIndexOrThrow(StructuredName.DISPLAY_NAME));
                // String tempName = getContactName(tempId);
                if (!TextUtils.isEmpty(tempName) && name.compareToIgnoreCase(tempName) == 0) {
                    contactLookupKey = tempLookup;
                    c.close();
                    return contactLookupKey;
                }
            }
            c.close();
        }

        return contactLookupKey;
    }

    protected String getContactLookupKeyByPhone(String phone) {
        String contactLookupKey = null;
        if (TextUtils.isEmpty(phone)) {
            return contactLookupKey;
        }

        // find aggregated contact
        String[] whereParameters = new String[] {
                Phone.NUMBER, Phone.LOOKUP_KEY
        };
        String where = Phone.NUMBER + " LIKE \'%" + phone + "%\'";
        Cursor c = getContentResolver().query(Data.CONTENT_URI, whereParameters, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                String tempLookup = c.getString(c.getColumnIndexOrThrow(Phone.LOOKUP_KEY));
                String tempPhone = c.getString(c.getColumnIndexOrThrow(Phone.NUMBER));
                if (!TextUtils.isEmpty(tempPhone) && PhoneNumberUtils.compare(phone, tempPhone)) {
                    contactLookupKey = tempLookup;
                    c.close();
                    return contactLookupKey;
                }
            }
            c.close();
        }

        return contactLookupKey;
    }

    protected String getContactLookupKeyByEmail(String email) {
        String contactLookupKey = null;
        if (TextUtils.isEmpty(email)) {
            return contactLookupKey;
        }

        // find aggregated contact
        String[] whereParameters = new String[] {
                Email.DATA, Email.LOOKUP_KEY
        };
        String where = Email.DATA + " LIKE \'%" + email + "%\'";
        Cursor c = getContentResolver().query(Data.CONTENT_URI, whereParameters, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                String tempLookup = c.getString(c.getColumnIndexOrThrow(Email.LOOKUP_KEY));
                String tempEmail = c.getString(c.getColumnIndexOrThrow(Email.DATA));
                if (!TextUtils.isEmpty(tempEmail) && email.compareToIgnoreCase(tempEmail) == 0) {
                    contactLookupKey = tempLookup;
                    c.close();
                    return contactLookupKey;
                }
            }
            c.close();
        }

        return contactLookupKey;
    }

    protected String getContactLookupKeyByPushToken(SlingerIdentity si) {
        String contactLookupKey = null;
        if (TextUtils.isEmpty(si.getToken())) {
            return contactLookupKey;
        }

        String fmtToken = SlingerIdentity.sidPush2DBPush(si);

        // find aggregated contact
        String[] whereParameters = new String[] {
                Im.DATA, Im.LOOKUP_KEY
        };
        String where = Im.DATA + " LIKE \'%" + fmtToken + "%\'";
        Cursor c = getContentResolver().query(Data.CONTENT_URI, whereParameters, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                String tempLookup = c.getString(c.getColumnIndexOrThrow(Im.LOOKUP_KEY));
                String tempPushToken = c.getString(c.getColumnIndexOrThrow(Im.DATA));
                if (!TextUtils.isEmpty(tempPushToken)
                        && fmtToken.compareToIgnoreCase(tempPushToken) == 0) {
                    contactLookupKey = tempLookup;
                    c.close();
                    return contactLookupKey;
                }
            }
            c.close();
        }

        return contactLookupKey;
    }

    protected ArrayList<AccountData> getRawContactIds(String contactId) {
        ArrayList<AccountData> accts = new ArrayList<AccountData>();
        if (TextUtils.isEmpty(contactId)) {
            return accts;
        }

        // find associated raw contact
        String rawContactId;
        String accountName;
        String accountType;
        String[] whereParameters = new String[] {
                RawContacts.CONTACT_ID, BaseColumns._ID, RawContacts.ACCOUNT_TYPE,
                RawContacts.ACCOUNT_NAME
        };
        String where = RawContacts.CONTACT_ID + " = "
                + DatabaseUtils.sqlEscapeString("" + contactId);
        Cursor c = getContentResolver().query(RawContacts.CONTENT_URI, whereParameters, where,
                null, null);
        if (c != null) {
            while (c.moveToNext()) {
                rawContactId = c.getString(c.getColumnIndexOrThrow(BaseColumns._ID));
                accountName = c.getString(c.getColumnIndexOrThrow(RawContacts.ACCOUNT_NAME));
                accountType = c.getString(c.getColumnIndexOrThrow(RawContacts.ACCOUNT_TYPE));
                accts.add(new AccountData(accountName, accountType, rawContactId));
            }
            c.close();
        }
        return accts;
    }

    protected void showNote(int resId) {
        showNote(getString(resId));
    }

    protected void showNote(Exception e) {
        String msg = e.getLocalizedMessage();
        if (TextUtils.isEmpty(msg)) {
            showNote(e.getClass().getSimpleName());
        } else {
            showNote(msg);
        }
    }

    protected void showNote(String msg) {
        MyLog.i(TAG, msg);
        if (msg != null) {
            int readDuration = msg.length() * ConfigData.MS_READ_PER_CHAR;
            if (readDuration <= ConfigData.SHORT_DELAY) {
                Toast toast = Toast.makeText(BaseActivity.this, msg, Toast.LENGTH_SHORT);
                toast.show();
            } else if (readDuration <= ConfigData.LONG_DELAY) {
                Toast toast = Toast.makeText(BaseActivity.this, msg, Toast.LENGTH_LONG);
                toast.show();
            } else {
                showHelp(getString(R.string.app_name), msg);
            }
        }
    }

    protected void showHelp(String title, String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_TITLE, title);
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            removeDialog(DIALOG_HELP);
            showDialog(DIALOG_HELP, args);
        }
    }

    protected static AlertDialog.Builder xshowHelp(Activity act, Bundle args) {
        String title = args.getString(extra.RESID_TITLE);
        String msg = args.getString(extra.RESID_MSG);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(title);
        ad.setMessage(msg);
        ad.setCancelable(true);
        ad.setNeutralButton(R.string.btn_Close, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        ad.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    protected boolean doCleanupOldKeyData(String[] keyNames) {
        if (keyNames == null || keyNames.length == 0)
            return false;

        String[] args = new String[2 + keyNames.length];
        StringBuilder where = new StringBuilder();

        where.append(Data.MIMETYPE);
        where.append(" = ? AND ");
        args[0] = (Im.CONTENT_ITEM_TYPE);

        where.append(Im.PROTOCOL);
        where.append(" = ? AND ");
        args[1] = (String.valueOf(Im.PROTOCOL_CUSTOM));

        where.append("(");
        for (int i = 0; i < keyNames.length; i++) {
            where.append(Im.CUSTOM_PROTOCOL);
            where.append(" LIKE ?");
            args[2 + i] = (keyNames[i]);

            if ((1 + i) < keyNames.length)
                where.append(" OR ");
        }
        where.append(")");

        String query = where.toString();

        Cursor c = getContentResolver().query(Data.CONTENT_URI, null, query, args, null);
        if (c != null) {
            int rowsOutdated = c.getCount();
            if (rowsOutdated > 0) {
                MyLog.d(TAG, String.format("%d deprecated IM rows found", rowsOutdated));
                int deletedRows = getContentResolver().delete(Data.CONTENT_URI, query, args);
                MyLog.d(TAG, String.format("%d deprecated IM rows removed", deletedRows));
            }
            c.close();
        }
        return true;
    }

    protected void showAbout() {
        if (!isFinishing()) {
            removeDialog(DIALOG_ABOUT);
            showDialog(DIALOG_ABOUT);
        }
    }

    static AlertDialog.Builder xshowAbout(Activity act) {
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        View layout;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            layout = View.inflate(new ContextThemeWrapper(act, R.style.Theme_Sherlock),
                    R.layout.about, null);
        } else {
            LayoutInflater inflater = (LayoutInflater) act
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.about, null);
        }
        TextView textViewAbout = (TextView) layout.findViewById(R.id.TextViewAbout);
        ad.setTitle(R.string.title_About);
        textViewAbout.setText(String.format(act.getString(R.string.text_About),
                ConfigData.getFullVersion(act), ConfigData.HELP_EMAIL, ConfigData.HELP_URL)
                + "\n\n" + act.getString(R.string.text_Requirements) + "\n\n" + getCredits(act));
        ad.setView(layout);
        ad.setCancelable(true);
        ad.setNeutralButton(R.string.btn_Close, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        ad.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    private static String getCredits(Activity act) {
        Locale deflocale = Locale.getDefault();

        StringBuilder cred = new StringBuilder();
        cred.append(act.getString(R.string.text_LanguagesProvidedBy)).append(" ");

        ArrayList<String> lines = new ArrayList<String>();

        lines.add(getTranslatorLine(act, new Locale("af"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("sq"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ar"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("hy"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("az"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("eu"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("be"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("bn"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("bs"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("bg"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ca"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("cb"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("zh", "CN"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("zh", "TW"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("hr"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("cs"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("da"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("nl"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("en"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("eo"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("et"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("tl"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("fi"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("fr"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("gl"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ka"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("de"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("el"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("gu"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ht"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ha"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("iw"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("hi"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("hm"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("hu"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("is"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ig"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("in"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ga"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("it"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ja"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("jv"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("kn"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("km"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ko"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("lo"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("la"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("lv"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("lt"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("mk"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ms"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("mt"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("mi"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("mr"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("mn"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ne"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("no"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("fa"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("pl"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("pt"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("pa"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ro"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ru"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("sr"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("sk"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("sl"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("so"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("es"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("sw"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("sv"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ta"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("te"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("th"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("tr"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("uk"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ur"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("vi"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("cy"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("ji"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("yo"), deflocale));
        lines.add(getTranslatorLine(act, new Locale("zu"), deflocale));

        Locale.setDefault(deflocale);
        Collections.sort(lines);
        for (String line : lines) {
            cred.append(line);
        }

        Configuration config = new Configuration();
        config.locale = deflocale;
        act.getResources().updateConfiguration(config, act.getResources().getDisplayMetrics());

        cred.append("\n  ").append(GOOGLE_TRANSLATE);

        return cred.toString();
    }

    private static String getTranslatorLine(Activity act, Locale locale, Locale deflocale) {
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        act.getResources().updateConfiguration(config, act.getResources().getDisplayMetrics());
        String transName = act.getString(R.string.app_TranslatorName);
        if (transName.compareToIgnoreCase(GOOGLE_TRANSLATE) != 0) {
            return new StringBuilder().append("\n  ").append(locale.getDisplayName(deflocale))
                    .append(": ").append(transName).toString();
        } else {
            return "";
        }
    }

    protected void showLicense() {
        if (!isFinishing()) {
            removeDialog(DIALOG_LICENSE);
            showDialog(DIALOG_LICENSE);
        }
    }

    static AlertDialog.Builder xshowLicense(Activity act) {
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        View layout;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            layout = View.inflate(new ContextThemeWrapper(act, R.style.Theme_Sherlock),
                    R.layout.about, null);
        } else {
            LayoutInflater inflater = (LayoutInflater) act
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.about, null);
        }
        TextView textViewAbout = (TextView) layout.findViewById(R.id.TextViewAbout);
        ad.setTitle(R.string.title_Eula);
        textViewAbout.setText(Eula.readEula(act));
        ad.setView(layout);
        ad.setCancelable(true);
        ad.setNeutralButton(R.string.btn_Close, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    protected void showFileActionChooser(File downloadedFile, String fileType) {
        if (TextUtils.isEmpty(fileType)) {
            String extension = SSUtil.getFileExtensionOnly(downloadedFile.getName());
            fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        Intent intent = new Intent();
        Uri uri = Uri.fromFile(downloadedFile);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, fileType);

        Intent intentChooser = Intent.createChooser(intent,
                String.format(getString(R.string.action_ViewFile), downloadedFile.getName()));
        try {
            startActivity(intentChooser);
        } catch (ActivityNotFoundException e) {
            showNote(getUnsupportedFeatureString(Intent.ACTION_VIEW));
        }
    }

    /***
     * @return When returns false we allow a new key to be generated.
     */
    protected boolean doPassEntryCheck(String pass, String passOld, boolean changePass) {
        CryptoMsgPrivateData mine = null;
        boolean changeSuccess = false;

        // check for legacy support, migrate if needed
        if (!CryptTools.existsSecretKey(getApplicationContext())) {
            // key not found, try migrating older version
            if (!CryptToolsLegacy.updateKeyFormatOld(getApplicationContext(), pass)) {
                // unable to migrate old key
                showNote(R.string.error_couldNotExtractPrivateKey);
            }
        }

        // decrypt correct private key
        try {
            mine = CryptTools.getSecretKey(getApplicationContext(), changePass ? passOld : pass);
        } catch (IOException e) {
            e.printStackTrace(); // key not found
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // unable to deserialize same key format
            showNote(R.string.error_couldNotExtractPrivateKey);
        } catch (CryptoMsgException e) {
            e.printStackTrace(); // key formatted incorrectly
            showNote(R.string.error_couldNotExtractPrivateKey);
        }

        // if change, confirm unlock, and resave
        if (changePass) {
            try {
                changeSuccess = CryptTools.changeSecretKeyPassphrase(getApplicationContext(), pass,
                        passOld);
            } catch (IOException e) {
                e.printStackTrace(); // key not found
            } catch (ClassNotFoundException e) {
                e.printStackTrace(); // unable to deserialize same key format
                showNote(R.string.error_couldNotExtractPrivateKey);
            } catch (CryptoMsgException e) {
                e.printStackTrace(); // key formatted incorrectly
                showNote(R.string.error_couldNotExtractPrivateKey);
            }
            if (changeSuccess) {
                SafeSlinger.setCachedPassPhrase(
                        ConfigData.loadPrefKeyIdString(getApplicationContext()), pass);
                SafeSlinger.startCacheService(BaseActivity.this);
                showNote(R.string.state_PassphraseUpdated);
                setPassphraseStatus(true);
            } else {
                setPassphraseStatus(false);
            }
        } else { // check against current key
            if (mine != null) {
                SafeSlinger.setCachedPassPhrase(
                        ConfigData.loadPrefKeyIdString(getApplicationContext()), pass);
                setPassphraseStatus(true);
            } else {
                setPassphraseStatus(false);
            }
        }
        return true;
    }

    private void setPassphraseStatus(boolean valid) {
        if (valid) {
            ConfigData.savePrefpassBackoffTimeout(getApplicationContext(),
                    ConfigData.DEFAULT_PASSPHRASE_BACKOFF);
            ConfigData.savePrefnextPassAttemptDate(getApplicationContext(), new Date().getTime());
        } else {
            long passBackoffTimeout = ConfigData
                    .loadPrefpassBackoffTimeout(getApplicationContext());
            passBackoffTimeout *= 2;
            ConfigData.savePrefpassBackoffTimeout(getApplicationContext(), passBackoffTimeout);
            ConfigData.savePrefnextPassAttemptDate(getApplicationContext(), new Date().getTime()
                    + passBackoffTimeout);
        }
    }

    protected boolean doUpdateActiveKeyStatus() throws SQLException {
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(getApplicationContext());
        ArrayList<RecipientRow> contacts = new ArrayList<RecipientRow>();

        // seek multiple active keys under same name...
        Cursor c = dbRecipient.fetchAllPublicKeys();
        if (c != null) {
            while (c.moveToNext()) {
                RecipientRow recipientRow = new RecipientRow(c);
                contacts.add(recipientRow);
            }
            c.close();
        }

        // correct invalid notification types...
        for (int i = 0; i < contacts.size(); i++) {
            RecipientRow r = contacts.get(i);

            if (r != null) {
                // find tokens that have been mis-labeled
                String t = r.getPushtoken();
                if (r.getNotify() <= ConfigData.NOTIFY_NOPUSH && !TextUtils.isEmpty(t)) {
                    dbRecipient.updateRecipientNotifyFromToken(r.getRowId(), t);
                }
            }
        }

        // 1st sort by push token, remove older tokens...
        Collections.sort(contacts, new PushTokenKeyDateComparator());
        for (int i = 0; i < contacts.size(); i++) {
            RecipientRow r = contacts.get(i);
            RecipientRow r2 = null;
            if (i + 1 != contacts.size())
                r2 = contacts.get(i + 1);

            String t1 = r != null ? r.getPushtoken() : null;
            String t2 = r2 != null ? r2.getPushtoken() : null;

            if (r != null) {
                boolean pushable = r.isPushable();
                boolean deprecated = r.isDeprecated();

                if (TextUtils.isEmpty(t1) || !pushable || deprecated) {
                    // unusable tokens should be removed...
                    dbRecipient.updateRecipientActiveState(r,
                            RecipientDbAdapter.RECIP_IS_NOT_ACTIVE);

                } else if (!TextUtils.isEmpty(t1) && !TextUtils.isEmpty(t2)
                        && t1.compareToIgnoreCase(t2) == 0) {
                    // in date order, mark inactive only when previous was
                    // active...
                    dbRecipient.updateRecipientActiveState(r,
                            RecipientDbAdapter.RECIP_IS_NOT_ACTIVE);

                } else if (!TextUtils.isEmpty(t1)) {
                    // show the most recent one...
                    dbRecipient.updateRecipientActiveState(r, RecipientDbAdapter.RECIP_IS_ACTIVE);
                }
            }
        }

        return true;
    }

    protected void doUpdateRecipientsFromContacts() throws SQLException {
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(getApplicationContext());
        ArrayList<RecipientRow> contacts = new ArrayList<RecipientRow>();

        // seek multiple active keys under same name...
        Cursor c = dbRecipient.fetchAllPublicKeys();
        if (c != null) {
            while (c.moveToNext()) {
                RecipientRow recipientRow = new RecipientRow(c);
                contacts.add(recipientRow);
            }
            c.close();
        }

        // do some lookup for new photos
        for (int i = 0; i < contacts.size(); i++) {
            RecipientRow r = contacts.get(i);
            byte[] newphoto = getContactPhoto(r.getContactlu());
            if (newphoto != null && !Arrays.equals(newphoto, r.getPhoto())) {
                dbRecipient.updateRecipientPhoto(r.getRowId(), newphoto);
            }
        }

        return;
    }

    protected int doImportFromExchange(Bundle args, int recipSource, String introkeyid)
            throws GeneralException {

        if (args == null) {
            return 0;
        }

        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(getApplicationContext());

        // build list of trusted items...
        int i = 0;
        Date exchdate = new Date();
        ArrayList<SlingerContact> cExch = new ArrayList<SlingerContact>();
        String currentKeyId = ConfigData.loadPrefKeyIdString(getApplicationContext());
        String currentToken = ConfigData.loadPrefPushRegistrationId(getApplicationContext());
        int currentNotify = SSUtil.getLocalNotification(getApplicationContext());
        CryptoMsgProvider p = CryptoMsgProvider.createInstance(SafeSlinger.getApplication()
                .isLoggable());
        String name = null;
        String push = null;
        String key = null;
        int selected = 0;

        if (recipSource == RecipientDbAdapter.RECIP_SOURCE_INTRODUCTION) {
            selected = 1;
        } else {
            selected = args.getInt(extra.SELECTED_TOTAL);
        }

        StringBuilder errors = new StringBuilder();
        do {
            name = null;
            push = null;
            key = null;

            // import trusted items from exchange...
            name = args.getString(extra.NAME + i);
            if (!TextUtils.isEmpty(name)) {
                byte[] pushBytes = args.getByteArray(ConfigData.APP_KEY_PUSHTOKEN + i);
                if (pushBytes == null) {
                    errors.append(name + " Push is missing").append("\n");
                } else {
                    push = new String(pushBytes);
                }
                byte[] keyBytes = args.getByteArray(ConfigData.APP_KEY_PUBKEY + i);
                if (keyBytes == null) {
                    errors.append(name + " PubKey is missing").append("\n");
                } else {
                    key = new String(keyBytes);
                }
                byte[] photo = args.getByteArray(extra.PHOTO + i);
                String contactLookupKey = args.getString(extra.CONTACT_LOOKUP_KEY + i);
                if (TextUtils.isEmpty(contactLookupKey)) {
                    contactLookupKey = getContactLookupKeyByName(name);
                }
                String contactId = getContactIdByName(name);
                String rawContactId = null;
                if (contactId != null) {
                    ArrayList<AccountData> v = getRawContactIds(contactId);
                    if (v != null && v.size() > 0)
                        rawContactId = v.get(0).getRawContactId();
                }

                // don't add unless there are all items we need...
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(key) && !TextUtils.isEmpty(push)) {
                    SlingerIdentity si = null;
                    si = SlingerIdentity.dbAll2sidAll(push, key);
                    cExch.add(SlingerContact.createContact(contactId, contactLookupKey,
                            rawContactId, name, photo, si));
                }
            }
            i++;
        } while (name != null);

        if (!TextUtils.isEmpty(errors)) {
            throw new GeneralException(errors.toString().trim());
        }

        // add redundant check for correct number of contacts imported...
        if (cExch.size() != selected) {
            return 0;
        }

        ArrayList<String> importedKeys = new ArrayList<String>();
        try {

            // compare to list of references in address book, and save...
            for (SlingerContact ce : cExch) {
                String userid = null;
                String exchKeyId = null;
                long exchKeyDate = 0;
                if (!TextUtils.isEmpty(ce.pubKey)) {
                    exchKeyId = p.ExtractKeyIDfromSafeSlingerString(ce.pubKey);
                    exchKeyDate = p.ExtractDateTimefromSafeSlingerString(ce.pubKey);
                }

                long ret = -1;

                if (recipSource == RecipientDbAdapter.RECIP_SOURCE_EXCHANGE) {
                    ret = dbRecipient.createExchangedRecipient(currentKeyId, exchdate.getTime(),
                            ce.contactId, ce.lookup, ce.rawid, ce.name, ce.photoBytes, exchKeyId,
                            exchKeyDate, userid, ce.pushTok, ce.notify, ce.pubKey.getBytes(),
                            currentToken, currentNotify);
                } else if (recipSource == RecipientDbAdapter.RECIP_SOURCE_INTRODUCTION) {
                    ret = dbRecipient.createIntroduceRecipient(currentKeyId, exchdate.getTime(),
                            ce.contactId, ce.lookup, ce.rawid, ce.name, ce.photoBytes, exchKeyId,
                            exchKeyDate, userid, ce.pushTok, ce.notify, ce.pubKey.getBytes(),
                            introkeyid, currentToken, currentNotify);
                }

                if (ret < 0) {
                    return importedKeys.size();
                }

                if (!importedKeys.contains(exchKeyId)) {
                    importedKeys.add(exchKeyId);
                }
            }

        } catch (CryptoMsgPeerKeyFormatException e) {
            return importedKeys.size();
        }

        return importedKeys.size();
    }

    protected String getMyPhoneNumber() {
        TelephonyManager mTelephonyMgr;
        mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return mTelephonyMgr.getLine1Number();
    }

    protected String getMy10DigitPhoneNumber() {
        String s = getMyPhoneNumber();
        return s.substring(2);
    }

    public static String formatRecpientDetails(Activity act, RecipientRow r) {
        StringBuilder s = new StringBuilder();
        if (r != null) {
            writeRecipDetail(act, s, r);
        }

        return s.toString();
    }

    public static String formatThreadDetails(Activity act, ThreadData t) {
        StringBuilder s = new StringBuilder();

        s.append(dat(act, R.string.title_MessageDetail, t.getLastDate()));

        RecipientRow r = t.getRecipient();
        if (r != null) {
            writeRecipDetail(act, s, r);
        } else {
            s.append(str(act, R.string.label_PublicKeyID, t.getKeyId()));
        }

        return s.toString();
    }

    private static void writeRecipDetail(Activity act, StringBuilder s, RecipientRow r) {
        switch (r.getSource()) {
            default:
            case RecipientDbAdapter.RECIP_SOURCE_CONTACTSDB:
                break;
            case RecipientDbAdapter.RECIP_SOURCE_EXCHANGE:
                s.append(dat(SSUtil.toTitleCase(act.getString(R.string.label_exchanged)),
                        r.getExchdate()));
                break;
            case RecipientDbAdapter.RECIP_SOURCE_INTRODUCTION:
                s.append(dat(SSUtil.toTitleCase(act.getString(R.string.label_introduced)),
                        r.getExchdate()));
                break;
        }
        s.append(str(act, R.string.label_UserName, r.getName()));
        s.append(str(act, R.string.label_Device,
                SSUtil.getDetailedDeviceDisplayName(act, r.getNotify())));

        s.append(str(act, R.string.label_PushTokenID, r.getPushtoken()));
        s.append(dat(act, R.string.label_Key, r.getKeydate()));

        s.append(str(act, R.string.label_PublicKeyID, r.getKeyid()));
        s.append(str(
                act.getText(R.string.title_MyIdentity) + " " + act.getText(R.string.label_Device),
                SSUtil.getDetailedDeviceDisplayName(act, r.getMyNotify())));
        s.append(str(
                act.getText(R.string.menu_Introduction) + " "
                        + act.getText(R.string.label_PublicKeyID), r.getIntroKeyid()));

        if (ConfigData.isDebug(act)) {
            s.append(str(
                    act.getText(R.string.title_MyIdentity) + " "
                            + act.getText(R.string.label_PublicKeyID), r.getMykeyid()));
            s.append(str(
                    act.getText(R.string.title_MyIdentity) + " "
                            + act.getText(R.string.label_PushTokenID), r.getMyPushtoken()));
            s.append(str("Contactid", r.getContactid()));
            s.append(str("Contactlu", r.getContactlu()));
            s.append(dat("Histdate", r.getHistdate()));
            s.append(dat("NotRegDate", r.getNotRegDate()));
            s.append(str("Rawid", r.getRawid()));
            s.append(str("RowId", r.getRowId()));
            s.append(str("Keyuserid", r.getKeyuserid()));
            s.append(str(act, "Photo", r.getPhoto()));
            s.append(str(act, "Pubkey", r.getPubkey()));
            s.append(str("Active", r.isActive()));
            s.append(ver("Appver", r.getAppver()));
        }
    }

    public static String formatMessageDetails(Activity act, MessageRow m) {
        StringBuilder s = new StringBuilder();
        s.append(dat(act, R.string.title_MessageDetail, m.getProbableDate()));
        s.append(str(act, R.string.label_UserName, m.getPerson()));
        s.append(str(act, R.string.title_MessageDetail, m.getText()));
        String file = m.getFileSize() > 0 ? m.getFileName() + " "
                + SSUtil.getSizeString(act, m.getFileSize()) : null;
        s.append(str(act, R.string.label_FileNameHint, file));
        s.append(str(act, R.string.label_PublicKeyID, m.getKeyId()));

        if (ConfigData.isDebug(act)) {
            s.append(str("FileDir", m.getFileDir()));
            s.append(str("FileType", m.getFileType()));
            s.append(str("RowId", m.getRowId()));
            s.append(str("MsgHash", m.getMsgHash()));
            s.append(str(act, "Photo", m.getPhoto()));
            s.append(str("Inbox", m.isInbox()));
            s.append(str("Read", m.isRead()));
            s.append(str("Seen", m.isSeen()));
            s.append(str(act, "EncBody", m.getEncBody()));
            s.append(str(act, "FileHash", m.getFileHash()));

            switch (m.getStatus()) {
                case MessageDbAdapter.MESSAGE_STATUS_COMPLETE_MSG:
                    s.append(str("Status", "COMPLETE_MSG"));
                    break;
                case MessageDbAdapter.MESSAGE_STATUS_DRAFT:
                    s.append(str("Status", "DRAFT"));
                    break;
                case MessageDbAdapter.MESSAGE_STATUS_EXPIRED:
                    s.append(str("Status", "EXPIRED"));
                    break;
                case MessageDbAdapter.MESSAGE_STATUS_FAILED:
                    s.append(str("Status", "FAILED"));
                    break;
                case MessageDbAdapter.MESSAGE_STATUS_FILE_DECRYPTED:
                    s.append(str("Status", "FILE_DECRYPTED"));
                    break;
                case MessageDbAdapter.MESSAGE_STATUS_NONE:
                    s.append(str("Status", "NONE"));
                    break;
                case MessageDbAdapter.MESSAGE_STATUS_GOTPUSH:
                    s.append(str("Status", "GOTPUSH"));
                    break;
                case MessageDbAdapter.MESSAGE_STATUS_QUEUED:
                    s.append(str("Status", "QUEUED"));
                    break;
                default:
                    s.append(str("Status", m.getStatus()));
                    break;
            }

        }

        return s.toString();
    }

    public static String str(Activity act, String name, byte[] b) {
        if (b != null)
            return name.replaceAll("\\.|:", "") + ": " + SSUtil.getSizeString(act, b.length) + "\n";
        else
            return name.replaceAll("\\.|:", "") + ":\n";
    }

    public static String str(String name, String s) {
        if (!TextUtils.isEmpty(s))
            return name.replaceAll("\\.|:", "") + String.format(": %s\n", s);
        else
            return name.replaceAll("\\.|:", "") + ":\n";
    }

    public static String str(String name, boolean b) {
        return name.replaceAll("\\.|:", "") + String.format(": %s\n", String.valueOf(b));
    }

    public static String str(String name, int i) {
        return name.replaceAll("\\.|:", "") + String.format(": %s\n", String.valueOf(i));
    }

    public static String str(String name, long l) {
        return name.replaceAll("\\.|:", "") + String.format(": %s\n", String.valueOf(l));
    }

    public static String dat(String name, long d) {
        if (d > 0)
            return name.replaceAll("\\.|:", "")
                    + String.format(
                            ": %s\n",
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                                    Locale.getDefault()).format(new Date(d)));
        else
            return name.replaceAll("\\.|:", "") + ":\n";
    }

    public static String ver(String name, long v) {
        if (v > 0) {
            String ver = String.format("%08X", v);
            return name.replaceAll("\\.|:", "")
                    + String.format(Locale.US, ": %d.%d.%d.%d\n",
                            Integer.parseInt(ver.substring(0, 2), 16),
                            Integer.parseInt(ver.substring(2, 4), 16),
                            Integer.parseInt(ver.substring(4, 6), 16),
                            Integer.parseInt(ver.substring(6, 8), 16));
        } else
            return name.replaceAll("\\.|:", "") + ":\n";
    }

    public static String str(Activity act, int id, byte[] b) {
        return str(act, act.getString(id), b);
    }

    public static String str(Activity act, int id, String s) {
        return str(act.getString(id), s);
    }

    public static String str(Activity act, int id, boolean b) {
        return str(act.getString(id), b);
    }

    public static String str(Activity act, int id, int i) {
        return str(act.getString(id), i);
    }

    public static String str(Activity act, int id, long l) {
        return str(act.getString(id), l);
    }

    public static String dat(Activity act, int id, long d) {
        return dat(act.getString(id), d);
    }

    public static String ver(Activity act, int id, long v) {
        return ver(act.getString(id), v);
    }

    protected InputMethodInfo getCurrentImeInfo() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> mInputMethodProperties = imm.getEnabledInputMethodList();

        final int n = mInputMethodProperties.size();
        for (int i = 0; i < n; i++) {

            InputMethodInfo imeInfo = mInputMethodProperties.get(i);

            if (imeInfo.getId().equals(
                    Settings.Secure.getString(getContentResolver(),
                            Settings.Secure.DEFAULT_INPUT_METHOD))) {

                return imeInfo;
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onUserInteraction() {
        // update the cache timeout
        SafeSlinger.updateCachedPassPhrase(ConfigData.loadPrefKeyIdString(getApplicationContext()));
    }

    protected void showBackupQuery() {
        if (!isFinishing()) {
            removeDialog(DIALOG_BACKUPQUERY);
            showDialog(DIALOG_BACKUPQUERY);
        }
    }

    protected AlertDialog.Builder xshowBackupQuery(final Activity act) {
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.label_RemindBackupDelay);
        ad.setMessage(R.string.ask_BackupDisabledRemindLater);
        ad.setCancelable(false);
        ad.setPositiveButton(R.string.btn_Remind, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                ConfigData.savePrefRemindBackupDelay(act, true);
                showBackupSettings();
            }
        });
        ad.setNegativeButton(R.string.btn_NotRemind, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                ConfigData.savePrefRemindBackupDelay(act, false);
            }
        });
        return ad;
    }

    protected void showWalkthrough() {
        if (!isFinishing()) {
            removeDialog(DIALOG_TUTORIAL);
            showDialog(DIALOG_TUTORIAL);
        }
    }

    protected static AlertDialog.Builder xshowWalkthrough(final HomeActivity act) {
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        View layout;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            layout = View.inflate(new ContextThemeWrapper(act, R.style.Theme_Sherlock),
                    R.layout.walkthrough, null);
        } else {
            LayoutInflater inflater = (LayoutInflater) act
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.walkthrough, null);
        }

        final CheckBox checkBoxWalkthrough = (CheckBox) layout
                .findViewById(R.id.WalkthroughCheckBox);
        checkBoxWalkthrough.setChecked(ConfigData.loadPrefShowWalkthrough(act));

        ad.setTitle(R.string.title_ExchangeWalkthrough);
        ad.setView(layout);
        ad.setCancelable(true);
        ad.setNeutralButton(R.string.btn_Continue, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                ConfigData.savePrefShowWalkthrough(act, checkBoxWalkthrough.isChecked());
                act.showExchange();
                dialog.dismiss();
            }
        });
        ad.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                act.showExchange();
                dialog.dismiss();
            }
        });
        return ad;
    }

    protected void showReference() {
        if (!isFinishing()) {
            removeDialog(DIALOG_REFERENCE);
            showDialog(DIALOG_REFERENCE);
        }
    }

    protected static AlertDialog.Builder xshowReference(final HomeActivity act) {
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        View layout;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            layout = View.inflate(new ContextThemeWrapper(act, R.style.Theme_Sherlock),
                    R.layout.reference, null);
        } else {
            LayoutInflater inflater = (LayoutInflater) act
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.reference, null);
        }

        TextView textViewStepExchange = (TextView) layout.findViewById(R.id.textViewStepExchange);
        textViewStepExchange.setText(String.format("%s (%s)",
                act.getString(R.string.menu_TagExchange), act.getString(R.string.label_step_1)));

        TextView textViewStepCompose = (TextView) layout.findViewById(R.id.textViewStepCompose);
        textViewStepCompose.setText(String.format("%s (%s)",
                act.getString(R.string.menu_TagComposeMessage),
                act.getString(R.string.label_step_2)));

        TextView textViewWalkExchange = (TextView) layout.findViewById(R.id.textViewWalkExchange);
        textViewWalkExchange.setText(act.getString(R.string.help_home)
                + act.getString(R.string.help_identity_menu));

        TextView textViewWalkCompose = (TextView) layout.findViewById(R.id.textViewWalkCompose);
        textViewWalkCompose.setText(act.getString(R.string.help_Send)
                + act.getString(R.string.help_identity_menu));

        String title = String.format("%s %s", act.getString(R.string.app_name),
                act.getString(R.string.menu_Help));
        ad.setTitle(title);
        ad.setView(layout);
        ad.setCancelable(true);
        ad.setNeutralButton(R.string.btn_Continue, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        ad.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    protected void showSendApplication() {
        // ask them to pick the sending method...
        // query possible message methods first
        String msg = ConfigData.URL_SS_INSTALL;
        CharSequence title = getString(R.string.action_NewUserRequest);
        List<Intent> targetedShareIntents = new ArrayList<Intent>();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(shareIntent, 0);

        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                String packageName = resolveInfo.activityInfo.packageName;

                // exclude our own app...
                if (!packageName.startsWith(getPackageName())) {
                    Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
                    targetedShareIntent.setType("text/plain");
                    targetedShareIntent.putExtra(Intent.EXTRA_SUBJECT,
                            getString(R.string.title_TextInviteMsg));
                    targetedShareIntent.putExtra(Intent.EXTRA_TEXT, msg);
                    targetedShareIntent.setPackage(packageName);

                    targetedShareIntents.add(targetedShareIntent);
                }
            }

            Intent chooserIntent = null;
            if (targetedShareIntents.size() > 0) {
                chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), title);
            } else {
                chooserIntent = Intent.createChooser(shareIntent, title);
            }
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                    targetedShareIntents.toArray(new Parcelable[] {}));
            try {
                startActivity(chooserIntent);
            } catch (ActivityNotFoundException e) {
                showNote(getUnsupportedFeatureString(Intent.ACTION_SEND));
            }
        }
    }

    protected void showBackupSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_PRIVACY_SETTINGS));
        } catch (ActivityNotFoundException e) {
            showNote(getUnsupportedFeatureString(Settings.ACTION_PRIVACY_SETTINGS));
        }
    }

    protected void showAccountsSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT));
        } catch (ActivityNotFoundException e) {
            showNote(getUnsupportedFeatureString(Settings.ACTION_ADD_ACCOUNT));
        }
    }

    public String getUnsupportedFeatureString(String feature) {
        return String.format(getString(R.string.error_FeatureIsNotSupport), feature, Build.BRAND
                + " " + Build.MODEL);
    }

}
