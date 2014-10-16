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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import a_vcard.android.syncml.pim.VDataBuilder;
import a_vcard.android.syncml.pim.VNode;
import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.Name;
import a_vcard.android.syncml.pim.vcard.VCardException;
import a_vcard.android.syncml.pim.vcard.VCardParser;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION_CODES;
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
import android.provider.Settings;
import android.provider.Telephony;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPeerKeyFormatException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider;
import edu.cmu.cylab.starslinger.model.ContactAccessor;
import edu.cmu.cylab.starslinger.model.ContactImpp;
import edu.cmu.cylab.starslinger.model.ContactNameMethodComparator;
import edu.cmu.cylab.starslinger.model.MessageDbAdapter;
import edu.cmu.cylab.starslinger.model.MessageRow;
import edu.cmu.cylab.starslinger.model.PushTokenKeyDateComparator;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientRow;
import edu.cmu.cylab.starslinger.model.SlingerContact;
import edu.cmu.cylab.starslinger.model.SlingerIdentity;
import edu.cmu.cylab.starslinger.model.ThreadData;
import edu.cmu.cylab.starslinger.model.UseContactItem;
import edu.cmu.cylab.starslinger.model.UseContactItem.UCType;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class BaseActivity extends ActionBarActivity {
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    public static final int RESULT_NEW_PASSPHRASE = 12358971;
    protected static final int MENU_HELP = 400;
    protected static final int MENU_ABOUT = 410;
    protected static final int MENU_EULA = 420;
    protected static final int MENU_PRIVACY = 430;
    protected static final int MENU_LOGOUT = 440;
    protected static final int MENU_CONTACTINVITE = 450;
    protected static final int MENU_SETTINGS = 460;
    protected static final int MENU_REFERENCE = 470;
    protected static final int MENU_SENDINTRO = 480;
    protected static final int MENU_FEEDBACK = 490;
    protected static final int DIALOG_HELP = 1;
    protected static final int DIALOG_ERREXIT = 2;
    protected static final int DIALOG_QUESTION = 3;
    protected static final int DIALOG_INTRO = 4;
    protected static final int DIALOG_ABOUT = 5;
    protected static final int DIALOG_PROGRESS = 9;
    protected static final int DIALOG_USEROPTIONS = 10;
    protected static final int DIALOG_FILEOPTIONS = 11;
    protected static final int DIALOG_LOAD_FILE = 12;
    protected static final int DIALOG_TEXT_ENTRY = 13;
    protected static final int DIALOG_REFERENCE = 14;
    protected static final int DIALOG_BACKUPQUERY = 15;
    protected static final int DIALOG_CONTACTINVITE = 16;
    private static final int RESULT_SEND_INVITE = 17;
    private static final int RESULT_SELECT_SMS = 18;
    private static final int RESULT_SELECT_EMAIL = 19;
    protected static final int DIALOG_MANAGE_PASS = 20;
    protected static final int DIALOG_CONTACTTYPE = 21;
    private static final int RESULT_SELECT_CONTACT_LINK = 22;
    private static final int RESULT_EDIT_CONTACT_LINK = 23;
    private static String mInviteContactLookupKey;
    private static long mContactLinkRecipientRowId;

    protected static Bundle writeSingleExportExchangeArgs(ContactImpp out) {

        Bundle args = new Bundle();
        args.putString(extra.CONTACT_LOOKUP_KEY, out.lookup);

        for (int i = 0; i < out.impps.size(); i++) {
            args.putString(extra.CONTACT_KEYNAME_PREFIX + i, out.impps.get(i).k);
            args.putByteArray(extra.CONTACT_VALUE_PREFIX + i, out.impps.get(i).v);
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
    public static String getSignersPublicKey(String publicKeyId) {
        if (TextUtils.isEmpty(publicKeyId)) {
            return null;
        }

        RecipientRow recip = null;
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(SafeSlinger
                .getApplication());
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

    private long getRecipientRowIdMatchingInvite(String contactLookupKey) {
        long rowId = -1;
        if (TextUtils.isEmpty(contactLookupKey)) {
            return rowId;
        }

        // find current aggregate contact id (must check in real time)
        String contactId = getContactIdByLookup(contactLookupKey);

        // find matching aggregate contact id (must check in real time)
        if (!TextUtils.isEmpty(contactId)) {
            RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this);
            String myName = SafeSlingerPrefs.getContactName();
            String mySecretKeyId = SafeSlingerPrefs.getKeyIdString();
            String myPushToken = SafeSlingerPrefs.getPushRegistrationId();

            Cursor c = dbRecipient.fetchAllRecipientsMessage(mySecretKeyId, myPushToken, myName);
            if (c != null) {
                while (c.moveToNext()) {
                    RecipientRow recip = new RecipientRow(c);
                    if (recip.isValidContactLink()) {
                        String otherContactId = getContactIdByLookup(recip.getContactlu());
                        if (contactId.equals(otherContactId)) {
                            rowId = recip.getRowId();
                        }
                    }
                }
                c.close();
            }
        }

        return rowId;
    }

    private int deleteMatchingInvites(String contactLookupKey) {
        int deleted = 0;
        if (TextUtils.isEmpty(contactLookupKey)) {
            return 0;
        }

        // find current aggregate contact id (must check in real time)
        String contactId = getContactIdByLookup(contactLookupKey);

        // find matching aggregate contact id (must check in real time)
        if (!TextUtils.isEmpty(contactId)) {
            RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this);
            String myName = SafeSlingerPrefs.getContactName();
            String mySecretKeyId = SafeSlingerPrefs.getKeyIdString();
            String myPushToken = SafeSlingerPrefs.getPushRegistrationId();
            ArrayList<Long> delRowIds = new ArrayList<Long>();

            Cursor c = dbRecipient.fetchAllRecipientsInvited(true, mySecretKeyId, myPushToken,
                    myName);
            if (c != null) {
                while (c.moveToNext()) {
                    RecipientRow recip = new RecipientRow(c);
                    if (recip.isValidContactLink()) {
                        String otherContactId = getContactIdByLookup(recip.getContactlu());
                        if (contactId.equals(otherContactId)) {
                            delRowIds.add(recip.getRowId());
                        }
                    }
                }
                c.close();
            }
            for (Long rowId : delRowIds) {
                if (dbRecipient.deleteRecipient(rowId)) {
                    deleted++;
                }
            }
        }

        return deleted;
    }

    private String getContactIdByLookup(String contactLookupKey) {
        String contactId = null;
        if (TextUtils.isEmpty(contactLookupKey)) {
            return contactId;
        }

        Uri personUri = getPersonUri(contactLookupKey);
        String[] projection = new String[] {
            BaseColumns._ID
        };

        if (personUri != null) {
            Cursor c = getContentResolver().query(personUri, projection, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    contactId = c.getString(c.getColumnIndexOrThrow(BaseColumns._ID));
                }
                c.close();
            }
        }

        return contactId;
    }

    private SlingerContact getContactByLookup(String contactLookupKey) {
        SlingerContact sc = null;
        if (TextUtils.isEmpty(contactLookupKey)) {
            return sc;
        }

        Uri personUri = getPersonUri(contactLookupKey);
        String[] projection = new String[] {
            BaseColumns._ID
        };

        if (personUri != null) {
            Cursor c = getContentResolver().query(personUri, projection, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    byte[] photo = getContactPhoto(contactLookupKey);
                    String name = getContactName(contactLookupKey);
                    SlingerIdentity si = new SlingerIdentity();
                    sc = SlingerContact.createContact(contactLookupKey, name, photo, si);
                }
                c.close();
            }
        }
        return sc;
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

    protected String getContactLookupKeyByRawContactId(String rawContactId) {
        if (TextUtils.isEmpty(rawContactId)) {
            return null;
        }

        String where = Data.RAW_CONTACT_ID + " = ?";
        String[] whereParameters = new String[] {
            rawContactId
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

        // TODO: is there is a better way to match names than literal match?

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

    protected ArrayList<UseContactItem> getUseContactItemsByName(String name) {
        ArrayList<UseContactItem> contacts = new ArrayList<UseContactItem>();
        if (TextUtils.isEmpty(name)) {
            return contacts;
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
                byte[] tempPhoto = getContactPhoto(tempLookup);
                if (!TextUtils.isEmpty(tempLookup)) {
                    contacts.add(new UseContactItem(tempName, tempPhoto, tempLookup, UCType.CONTACT));
                }
            }
            c.close();
        }

        return contacts;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected UseContactItem getContactProfile() {
        UseContactItem profile = null;

        if (Build.VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {
            Cursor c = getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null,
                    null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    String tempLookup = c.getString(c
                            .getColumnIndexOrThrow(ContactsContract.Profile.LOOKUP_KEY));
                    String tempName = c.getString(c
                            .getColumnIndexOrThrow(ContactsContract.Profile.DISPLAY_NAME));
                    byte[] tempPhoto = getContactPhoto(tempLookup);
                    if (!TextUtils.isEmpty(tempLookup)) {
                        profile = new UseContactItem(tempName, tempPhoto, tempLookup,
                                UCType.PROFILE);
                    }
                }
                c.close();
            }
        }

        return profile;
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
            int readDuration = msg.length() * SafeSlingerConfig.MS_READ_PER_CHAR;
            if (readDuration <= SafeSlingerConfig.SHORT_DELAY) {
                Toast toast = Toast.makeText(BaseActivity.this, msg.trim(), Toast.LENGTH_SHORT);
                toast.show();
            } else if (readDuration <= SafeSlingerConfig.LONG_DELAY) {
                Toast toast = Toast.makeText(BaseActivity.this, msg.trim(), Toast.LENGTH_LONG);
                toast.show();
            } else {
                showHelp(getString(R.string.app_name), msg.trim());
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

    private boolean doCleanupOldKeyData(String[] keyNames) {
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
            layout = View.inflate(new ContextThemeWrapper(act, R.style.Theme_AppCompat),
                    R.layout.about, null);
        } else {
            LayoutInflater inflater = (LayoutInflater) act
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.about, null);
        }
        TextView textViewAbout = (TextView) layout.findViewById(R.id.TextViewAbout);
        ad.setTitle(R.string.title_About);

        String msgHead = SafeSlingerConfig.getFullVersion();
        String msgAbout = act.getString(R.string.text_About);
        String msgAbFeat = act.getString(R.string.text_AboutFeat);
        String msgAb1 = act.getString(R.string.text_About1);
        String msgAb2 = act.getString(R.string.text_About2);
        String msgAb3 = act.getString(R.string.text_About3);
        String msgEmail = String.format(act.getString(R.string.text_AboutEmail),
                SafeSlingerConfig.HELP_EMAIL);
        String msgWeb = String.format(act.getString(R.string.text_AboutWeb),
                SafeSlingerConfig.HELP_URL);
        String msgSrc = String.format(act.getString(R.string.text_SourceCodeRepo),
                SafeSlingerConfig.SOURCE_URL);
        String msgReq = act.getString(R.string.text_Requirements);
        String msgReq1 = act.getString(R.string.text_Requirements1);
        String msgReq2 = act.getString(R.string.text_Requirements2);
        String msgLang = getCredits(act);

        textViewAbout.setText(String.format(
                "%s\n\n%s\n\n%s\n- %s\n- %s\n- %s\n\n%s\n1. %s\n2. %s\n\n%s\n%s\n%s\n\n%s",
                msgHead, msgAbout, msgAbFeat, msgAb1, msgAb2, msgAb3, msgReq, msgReq1, msgReq2,
                msgEmail, msgWeb, msgSrc, msgLang));

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

        StringBuilder cred = new StringBuilder();
        cred.append(act.getString(R.string.text_LanguagesProvidedBy));
        cred.append("\n");
        cred.append(act.getString(R.string.app_TranslatorName));
        cred.append("\n");

        return cred.toString();
    }

    protected void showFileActionChooser(File downloadedFile, String fileType) {
        // always second-guess mimetype, since send/recv mimetype could be
        // inaccurate
        String extension = SSUtil.getFileExtensionOnly(downloadedFile.getName());
        fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (fileType == null) {
            fileType = SafeSlingerConfig.MIMETYPE_OPEN_ATTACH_DEF;
        }

        Intent intent = new Intent();
        Uri uri = Uri.fromFile(downloadedFile);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, fileType);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showNote(getUnsupportedFeatureString(Intent.ACTION_VIEW));
        }
    }

    protected void setPassphraseStatus(boolean valid) {
        if (valid) {
            SafeSlingerPrefs.setPassBackoffTimeout(SafeSlingerPrefs.DEFAULT_PASSPHRASE_BACKOFF);
            SafeSlingerPrefs.setNextPassAttemptDate(new Date().getTime());
        } else {
            long passBackoffTimeout = SafeSlingerPrefs.getPassBackoffTimeout();
            passBackoffTimeout *= 2;
            SafeSlingerPrefs.setPassBackoffTimeout(passBackoffTimeout);
            SafeSlingerPrefs.setNextPassAttemptDate(new Date().getTime() + passBackoffTimeout);
        }
    }

    protected class BackgroundSyncUpdatesTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... arg0) {
            SafeSlingerPrefs.setContactDBLastScan(System.currentTimeMillis());

            try {
                doUpdateRecipientsFromContacts();
            } catch (SQLException e) {
                // ignore since we only attempt to update old data
            }

            // make sure recipient list shows correct keys...
            if (!doUpdateActiveKeyStatus()) {
                publishProgress(getString(R.string.error_UnableToUpdateRecipientInDB));
            }

            // remove deprecated key storage from contacts
            String[] keyNames = new String[] { //
                    SafeSlingerConfig.APP_KEY_OLD1, //
                    SafeSlingerConfig.APP_KEY_OLD2, //
                    SafeSlingerConfig.APP_KEY_PUBKEY, //
                    SafeSlingerConfig.APP_KEY_PUSHTOKEN, //
            };
            doCleanupOldKeyData(keyNames);
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            showNote(progress[0]);
        }
    }

    private boolean doUpdateActiveKeyStatus() throws SQLException {
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
                if (r.getNotify() <= SafeSlingerConfig.NOTIFY_NOPUSH && !TextUtils.isEmpty(t)) {
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
                boolean invited = r.isInvited();

                if (invited) {
                    // invites are always active
                    dbRecipient.updateRecipientActiveState(r, RecipientDbAdapter.RECIP_IS_ACTIVE);
                } else {
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
                        dbRecipient.updateRecipientActiveState(r,
                                RecipientDbAdapter.RECIP_IS_ACTIVE);
                    }
                }
            }
        }

        return true;
    }

    private void doUpdateRecipientsFromContacts() throws SQLException {
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

        // do some lookup for new photos/names
        for (int i = 0; i < contacts.size(); i++) {
            RecipientRow r = contacts.get(i);

            // if not valid contact link, attempt to migrate it...
            if (!r.isValidContactLink() && !TextUtils.isEmpty(r.getContactlu())) {
                String abname = getContactName(r.getContactlu());
                String rname = r.getName();
                // byte[] abphoto = getContactPhoto(r.getContactlu());
                // byte[] rphoto = r.getPhoto();
                if (rname.equalsIgnoreCase(abname)) {
                    dbRecipient.updateRecipientFromChosenLink(r.getRowId(), r.getContactlu());
                }
            }

            // this should only update if the lookup key is the only lookup
            // method since some past lookup keys referenced a raw id rather
            // than the aggregate id
            if (r.isValidContactLink()) {
                String newname = getContactName(r.getContactlu());
                if (!TextUtils.isEmpty(newname) && !newname.equals(r.getName())) {
                    dbRecipient.updateRecipientName(r.getRowId(), newname);
                }
                byte[] newphoto = getContactPhoto(r.getContactlu());
                if (!Arrays.equals(newphoto, r.getPhoto())) {
                    dbRecipient.updateRecipientPhoto(r.getRowId(), newphoto);
                }
            }
        }

        return;
    }

    protected int doImportFromExchange(Bundle args, int recipSource, String introkeyid)
            throws GeneralException, CryptoMsgPeerKeyFormatException {

        if (args == null) {
            return 0;
        }

        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(getApplicationContext());

        // build list of trusted items...
        int i = 0;
        Date exchdate = new Date();
        ArrayList<SlingerContact> cExch = new ArrayList<SlingerContact>();
        String currentKeyId = SafeSlingerPrefs.getKeyIdString();
        String currentToken = SafeSlingerPrefs.getPushRegistrationId();
        int currentNotify = SSUtil.getLocalNotification(getApplicationContext());
        CryptoMsgProvider p = CryptoMsgProvider.createInstance(SafeSlinger.isLoggable());
        String name = null;
        String push = null;
        String key = null;

        StringBuilder errors = new StringBuilder();
        do {
            name = null;
            push = null;
            key = null;

            // import trusted items from exchange...
            name = args.getString(extra.NAME + i);
            if (!TextUtils.isEmpty(name)) {
                byte[] pushBytes = args.getByteArray(SafeSlingerConfig.APP_KEY_PUSHTOKEN + i);
                if (pushBytes == null) {
                    errors.append(name + " Push is missing").append("\n");
                } else {
                    push = new String(pushBytes);
                }
                byte[] keyBytes = args.getByteArray(SafeSlingerConfig.APP_KEY_PUBKEY + i);
                if (keyBytes == null) {
                    errors.append(name + " PubKey is missing").append("\n");
                } else {
                    key = new String(keyBytes);
                }
                byte[] photo = args.getByteArray(extra.PHOTO + i);
                String contactLookupKey = args.getString(extra.CONTACT_LOOKUP_KEY + i);

                // don't add unless there are all items we need...
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(key) && !TextUtils.isEmpty(push)) {
                    SlingerIdentity si = null;
                    si = SlingerIdentity.dbAll2sidAll(push, key);
                    cExch.add(SlingerContact.createContact(contactLookupKey, name, photo, si));
                }
            }
            i++;
        } while (name != null);

        if (!TextUtils.isEmpty(errors)) {
            throw new GeneralException(errors.toString().trim());
        }

        ArrayList<String> importedKeys = new ArrayList<String>();

        // compare to list of references in address book, and save...
        for (SlingerContact ce : cExch) {
            String userid = null;
            String exchKeyId = null;
            long exchKeyDate = 0;
            if (!TextUtils.isEmpty(ce.pubKey)) {
                exchKeyId = p.ExtractKeyIDfromSafeSlingerString(ce.pubKey);
                exchKeyDate = p.ExtractDateTimefromSafeSlingerString(ce.pubKey);
            }

            long matchingInviteRowId = getRecipientRowIdMatchingInvite(ce.lookup);

            long ret = -1;
            if (recipSource == RecipientDbAdapter.RECIP_SOURCE_EXCHANGE) {
                ret = dbRecipient.createExchangedRecipient(currentKeyId, exchdate.getTime(),
                        ce.lookup, ce.name, ce.photoBytes, exchKeyId, exchKeyDate, userid,
                        ce.pushTok, ce.notify, ce.pubKey.getBytes(), currentToken, currentNotify,
                        matchingInviteRowId);
            } else if (recipSource == RecipientDbAdapter.RECIP_SOURCE_INTRODUCTION) {
                ret = dbRecipient.createIntroduceRecipient(currentKeyId, exchdate.getTime(),
                        ce.lookup, ce.name, ce.photoBytes, exchKeyId, exchKeyDate, userid,
                        ce.pushTok, ce.notify, ce.pubKey.getBytes(), introkeyid, currentToken,
                        currentNotify, matchingInviteRowId);
            }

            if (ret < 0) {
                return importedKeys.size();
            }

            if (!importedKeys.contains(exchKeyId)) {
                importedKeys.add(exchKeyId);
            }
        }

        return importedKeys.size();
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
            case RecipientDbAdapter.RECIP_SOURCE_INVITED:
                s.append(dat(SSUtil.toTitleCase(act.getString(R.string.label_inviteSent)),
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

        if (SafeSlingerConfig.isDebug()) {
            s.append(str(
                    act.getText(R.string.title_MyIdentity) + " "
                            + act.getText(R.string.label_PublicKeyID), r.getMykeyid()));
            s.append(str(
                    act.getText(R.string.title_MyIdentity) + " "
                            + act.getText(R.string.label_PushTokenID), r.getMyPushtoken()));
            s.append(str("Contactid", r.getContactid()));
            s.append(str("RawContactid", r.getRawContactid()));
            s.append(str("Contactlu", r.getContactlu()));
            s.append(dat("Histdate", r.getHistdate()));
            s.append(dat("NotRegDate", r.getNotRegDate()));
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

        // show sent and received dates when arrival time over clock skew for
        // decrypted messages, likely network lag.
        if (m.getDateRecv() > 0 && m.getDateSent() > 0
                && (m.getDateRecv() - m.getDateSent()) > SafeSlingerConfig.CLOCK_SKEW_MS) {
            s.append(dat(act, R.string.title_MessageDetail, m.getDateRecv()));
            s.append(dat(act, R.string.state_FileSent, m.getDateSent()));
        } else if (m.getDateSent() > 0) {
            s.append(dat(act, R.string.state_FileSent, m.getDateSent()));
        } else {
            s.append(dat(act, R.string.title_MessageDetail, m.getDateRecv()));
        }

        s.append(str(act, R.string.label_UserName, m.getPerson()));
        s.append(str(act, R.string.title_MessageDetail, m.getText()));
        String file = m.getFileSize() > 0 ? m.getFileName() + " "
                + SSUtil.getSizeString(act, m.getFileSize()) : null;
        s.append(str(act, R.string.label_FileNameHint, file));
        s.append(str(act, R.string.label_PublicKeyID, m.getKeyId()));

        if (SafeSlingerConfig.isDebug()) {
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

            s.append(str("Status", MessageDbAdapter.getStatusCode(m.getStatus())));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onUserInteraction() {
        // update the cache timeout
        SafeSlinger.updateCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        SafeSlinger.activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SafeSlinger.activityPaused();
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
                SafeSlingerPrefs.setRemindBackupDelay(true);
                showBackupSettings();
            }
        });
        ad.setNegativeButton(R.string.btn_NotRemind, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                SafeSlingerPrefs.setRemindBackupDelay(false);
            }
        });
        return ad;
    }

    protected static AlertDialog.Builder xshowWalkthrough(final Activity act) {
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        View layout;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            layout = View.inflate(new ContextThemeWrapper(act, R.style.Theme_AppCompat),
                    R.layout.walkthrough, null);
        } else {
            LayoutInflater inflater = (LayoutInflater) act
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.walkthrough, null);
        }

        final CheckBox checkBoxWalkthrough = (CheckBox) layout
                .findViewById(R.id.WalkthroughCheckBox);
        checkBoxWalkthrough.setChecked(SafeSlingerPrefs.getShowWalkthrough());

        ad.setTitle(R.string.title_ExchangeWalkthrough);
        ad.setView(layout);
        ad.setCancelable(true);
        ad.setNeutralButton(R.string.btn_Continue, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                SafeSlingerPrefs.setShowWalkthrough(checkBoxWalkthrough.isChecked());
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

    protected void showReference() {
        if (!isFinishing()) {
            removeDialog(DIALOG_REFERENCE);
            showDialog(DIALOG_REFERENCE);
        }
    }

    protected static AlertDialog.Builder xshowReference(final Activity act) {
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        View layout;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            layout = View.inflate(new ContextThemeWrapper(act, R.style.Theme_AppCompat),
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
        textViewWalkExchange.setText(act.getString(R.string.help_home) + "\n\n"
                + act.getString(R.string.help_identity_menu));

        TextView textViewWalkCompose = (TextView) layout.findViewById(R.id.textViewWalkCompose);
        textViewWalkCompose.setText(act.getString(R.string.help_Send) + "\n\n"
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

    protected void showAddContactInvite() {
        if (!isFinishing()) {
            removeDialog(DIALOG_CONTACTINVITE);
            showDialog(DIALOG_CONTACTINVITE);
        }
    }

    protected AlertDialog.Builder xshowAddContactInvite(final Activity act) {
        final CharSequence[] items = new CharSequence[] { //
                act.getText(R.string.menu_ContactInviteSms), //
                act.getText(R.string.menu_ContactInviteEmail), //
                act.getText(R.string.menu_UseAnother), //
        };
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.action_NewUserRequest);
        ad.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
                switch (item) {
                    case 0: // sms
                        showPickPhone();
                        break;
                    case 1: // email
                        showPickEmail();
                        break;
                    case 2: // generic
                        showSendApplication();
                        break;
                    default:
                        break;
                }
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

    private void showPickPhone() {
        boolean phonePickable = getPackageManager().resolveActivity(getPickPhoneIntent(), 0) != null;
        if (phonePickable) {
            startActivityForResult(getPickPhoneIntent(), RESULT_SELECT_SMS);
        } else {
            showCustomContactPicker(RESULT_SELECT_SMS);
        }
    }

    private void showPickEmail() {
        boolean emailPickable = getPackageManager().resolveActivity(getPickEmailIntent(), 0) != null;
        if (emailPickable) {
            startActivityForResult(getPickEmailIntent(), RESULT_SELECT_EMAIL);
        } else {
            showCustomContactPicker(RESULT_SELECT_EMAIL);
        }
    }

    protected void showUpdateContactLink(long recipientRowId) {
        mContactLinkRecipientRowId = recipientRowId;
        Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        startActivityForResult(intent, RESULT_SELECT_CONTACT_LINK);
    }

    protected void showEditContact(String contactLookupKey) {
        Uri personUri = getPersonUri(contactLookupKey);
        if (personUri != null) {
            Intent intent = new Intent(Intent.ACTION_EDIT, personUri);
            try {
                startActivityForResult(intent, RESULT_EDIT_CONTACT_LINK);
            } catch (ActivityNotFoundException e) {
                showNote(getUnsupportedFeatureString("Contacts"));
            }
        } else {
            showNote(R.string.error_ContactUpdateFailed);
        }
    }

    public static Intent getPickPhoneIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        intent.setType(Phone.CONTENT_TYPE);
        return intent;
    }

    public static Intent getPickEmailIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        intent.setType(Email.CONTENT_TYPE);
        return intent;
    }

    private String getInviteShortMessage() {
        String smsText = String
                .format("%s %s %s", getString(R.string.label_messageInviteStartMsg),
                        getString(R.string.label_messageInviteSetupInst), String.format(
                                getString(R.string.label_messageInviteInstall),
                                SafeSlingerConfig.HELP_URL));
        return smsText;
    }

    private String getInviteLongMessage() {
        return String
                .format("%s\n\n%s\n\n%s\n", getString(R.string.label_messageInviteStartMsg),
                        getString(R.string.label_messageInviteSetupInst), String.format(
                                getString(R.string.label_messageInviteInstall),
                                SafeSlingerConfig.HELP_URL));
    }

    private void sendInviteToSlingEmail(String[] emailsTo) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        if (emailsTo != null) {
            intent.putExtra(Intent.EXTRA_EMAIL, emailsTo);
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, String.format("%s %s",
                getString(R.string.title_TextInviteMsg), getString(R.string.menu_TagExchange)));
        intent.putExtra(Intent.EXTRA_TEXT, getInviteLongMessage());

        try {
            startActivityForResult(intent, RESULT_SEND_INVITE);
        } catch (ActivityNotFoundException e) {
            // If there is nothing that can send a text/html MIME type
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void sendInviteToSlingPhones(String phone) {
        Intent intent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String defaultSmsPackageName = Telephony.Sms
                    .getDefaultSmsPackage(getApplicationContext());
            intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(phone)));
            intent.putExtra("sms_body", getInviteShortMessage());
            if (defaultSmsPackageName != null) {
                intent.setPackage(defaultSmsPackageName);
            }
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            intent.putExtra("address", phone);
            intent.putExtra("sms_body", getInviteShortMessage());
        }
        try {
            startActivityForResult(intent, RESULT_SEND_INVITE);
        } catch (ActivityNotFoundException e) {
            // If there is nothing that can send a text/html MIME type
            e.printStackTrace();
        }
    }

    private void showCustomContactPicker(int resultCode) {
        Bundle args = new Bundle();
        args.putInt(extra.RESULT_CODE, resultCode);
        if (!isFinishing()) {
            removeDialog(DIALOG_CONTACTTYPE);
            showDialog(DIALOG_CONTACTTYPE, args);
        }
    }

    protected AlertDialog.Builder xshowCustomContactPicker(final Activity act, Bundle args) {
        final int resultCode = args.getInt(extra.RESULT_CODE);
        Uri contentUri;
        String contactId2;
        String data2;
        String title;
        switch (resultCode) {
            case RESULT_SELECT_EMAIL:
                title = getString(R.string.menu_ContactInviteEmail);
                contentUri = Email.CONTENT_URI;
                contactId2 = Email.CONTACT_ID;
                data2 = Email.DATA;
                break;
            case RESULT_SELECT_SMS:
                title = getString(R.string.menu_ContactInviteSms);
                contentUri = Phone.CONTENT_URI;
                contactId2 = Phone.CONTACT_ID;
                data2 = Phone.DATA;
                break;
            default:
                return null;
        }

        final ArrayList<HashMap<String, String>> contacts = new ArrayList<HashMap<String, String>>();
        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        while (c.moveToNext()) {
            String contactId = c.getString(c.getColumnIndex(BaseColumns._ID));
            String contactLookupKey = c.getString(c
                    .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            String contactName = c.getString(c
                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

            // Pull out every address for this particular contact
            Cursor cm = cr.query(contentUri, null, contactId2 + " = " + contactId, null, null);
            while (cm.moveToNext()) {
                // Add address to our array
                String data = cm.getString(cm.getColumnIndex(data2));

                HashMap<String, String> contact = new HashMap<String, String>();
                contact.put(extra.CONTACT_LOOKUP_KEY, contactLookupKey);
                contact.put(extra.NAME, contactName);
                contact.put(extra.DATA, data);

                if (!contacts.contains(contact)) {
                    contacts.add(contact);
                }
            }
            cm.close();
        }
        c.close();

        Collections.sort(contacts, new ContactNameMethodComparator());

        // Make an adapter to display the list
        SimpleAdapter adapter = new SimpleAdapter(this, contacts,
                android.R.layout.two_line_list_item, new String[] {
                        extra.NAME, extra.DATA
                }, new int[] {
                        android.R.id.text1, android.R.id.text2
                });

        // Show the list and let the user pick an address
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(title);
        ad.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                HashMap<String, String> contact = contacts.get(which);
                final String theData = contact.get(extra.DATA);
                mInviteContactLookupKey = contact.get(extra.CONTACT_LOOKUP_KEY);

                switch (resultCode) {
                    case RESULT_SELECT_EMAIL:
                        sendInviteToSlingEmail(new String[] {
                            theData
                        });
                        break;
                    case RESULT_SELECT_SMS:
                        sendInviteToSlingPhones(theData);
                        break;
                    default:
                        break;
                }
            }
        });
        return ad;
    }

    private void showSendApplication() {
        // ask them to pick the sending method...
        // query possible message methods first
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
                    targetedShareIntent.putExtra(Intent.EXTRA_SUBJECT, String.format("%s %s",
                            getString(R.string.title_TextInviteMsg),
                            getString(R.string.menu_TagExchange)));
                    targetedShareIntent.putExtra(Intent.EXTRA_TEXT, getInviteLongMessage());
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

    private void showBackupSettings() {
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

    public List<ContactStruct> parseVCards(byte[][] data) {

        // parse all data for vCard format, and pull names list, skipping owner
        List<ContactStruct> parsedContacts = new ArrayList<ContactStruct>();

        if (data != null && data.length > 0)
            for (int i = 0; i < data.length; i++) {
                VCardParser parser = new VCardParser();
                VDataBuilder builder = new VDataBuilder();
                String vcardString = new String(data[i]);

                // parse the string
                boolean parsed = false;
                try {
                    MyLog.d(TAG, vcardString);
                    parsed = parser.parse(vcardString, "UTF-8", builder);
                } catch (VCardException e) {
                    ContactStruct mem = new ContactStruct();
                    mem.name = new Name("Error: " + e.getLocalizedMessage());
                    parsedContacts.add(mem);
                    continue;
                } catch (IOException e) {
                    ContactStruct mem = new ContactStruct();
                    mem.name = new Name("Error: " + e.getLocalizedMessage());
                    parsedContacts.add(mem);
                    continue;
                }
                if (!parsed) {
                    ContactStruct mem = new ContactStruct();
                    mem.name = new Name("Error: " + getString(R.string.error_ContactInsertFailed));
                    parsedContacts.add(mem);
                    continue;
                }

                // get all parsed contacts
                List<VNode> pimContacts = builder.vNodeList;

                // do something for all the contacts
                for (VNode contact : pimContacts) {

                    ContactStruct mem = ContactStruct.constructContactFromVNode(contact,
                            Name.NAME_ORDER_TYPE_ENGLISH);
                    if (mem != null)
                        parsedContacts.add(mem);
                    continue;
                }
            }

        return parsedContacts;
    }

    protected static ContactStruct loadContactDataNoDuplicates(Context ctx,
            String contactLookupKey, ContactStruct in, boolean removeMatches) {
        // order: name, photo, phone, im, email, url, postal, title, org
        final ContactAccessor accessor = ContactAccessor.getInstance();

        // create a parallel here for storing the data, use the table rows
        ContentResolver resolver = ctx.getContentResolver();
        ContactStruct contact = (in != null) ? in : new ContactStruct();

        // get name, retain passed in name
        if (contact.name == null || TextUtils.isEmpty(contact.name.toString())) {
            Uri uriName = accessor.getUriName(resolver, contactLookupKey);
            if (uriName != null) {
                Cursor names = ctx.getContentResolver().query(uriName, accessor.getProjName(),
                        accessor.getQueryName(), null, null);
                if (names != null) {
                    while (names.moveToNext()) {
                        accessor.addName(contact, names);
                    }
                    names.close();
                }
            }
        }

        // get photo
        Uri uriPhoto = accessor.getUriPhoto(resolver, contactLookupKey);
        if (uriPhoto != null) {
            Cursor photos = ctx.getContentResolver().query(uriPhoto, accessor.getProjPhoto(),
                    accessor.getQueryPhoto(), null, null);
            if (photos != null) {
                while (photos.moveToNext()) {
                    accessor.addPhoto(contact, photos);
                }
                photos.close();
            }
        }

        // get phone
        Uri uriPhone = accessor.getUriPhone(resolver, contactLookupKey);
        if (uriPhone != null) {
            Cursor phones = ctx.getContentResolver().query(uriPhone, accessor.getProjPhone(),
                    accessor.getQueryPhone(), null, null);
            if (phones != null) {
                while (phones.moveToNext()) {
                    accessor.addPhone(ctx, contact, phones, removeMatches);
                }
                phones.close();
            }
        }

        // get IM
        Uri uriIm = accessor.getUriIM(resolver, contactLookupKey);
        if (uriIm != null) {
            Cursor ims = ctx.getContentResolver().query(uriIm, accessor.getProjIM(),
                    accessor.getQueryIM(), null, null);
            if (ims != null) {
                while (ims.moveToNext()) {
                    accessor.addIM(contact, ims, ctx, removeMatches);
                }
                ims.close();
            }
        }

        // get email
        Uri uriEmail = accessor.getUriEmail(resolver, contactLookupKey);
        if (uriEmail != null) {
            Cursor emails = ctx.getContentResolver().query(uriEmail, accessor.getProjEmail(),
                    accessor.getQueryEmail(), null, null);
            if (emails != null) {
                while (emails.moveToNext()) {
                    accessor.addEmail(contact, emails, removeMatches);
                }
                emails.close();
            }
        }

        // get Url
        Uri uriUrl = accessor.getUriUrl(resolver, contactLookupKey);
        if (uriUrl != null) {
            Cursor urls = ctx.getContentResolver().query(uriUrl, accessor.getProjUrl(),
                    accessor.getQueryUrl(), null, null);
            if (urls != null) {
                while (urls.moveToNext()) {
                    accessor.addUrl(contact, urls, ctx, removeMatches);
                }
                urls.close();
            }
        }

        // get postal
        Uri uriPostal = accessor.getUriPostal(resolver, contactLookupKey);
        if (uriPostal != null) {
            Cursor postals = ctx.getContentResolver().query(uriPostal, accessor.getProjPostal(),
                    accessor.getQueryPostal(), null, null);
            if (postals != null) {
                while (postals.moveToNext()) {
                    accessor.addPostal(contact, postals, removeMatches);
                }
                postals.close();
            }
        }

        return contact;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case RESULT_SELECT_CONTACT_LINK:
                String contactLookupKey = null;
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        Cursor c = null;
                        try {
                            c = getContentResolver().query(uri, null, null, null, null);
                            if (c != null && c.moveToFirst()) {
                                contactLookupKey = c.getString(c
                                        .getColumnIndexOrThrow(Data.LOOKUP_KEY));
                            }
                        } finally {
                            if (c != null && !c.isClosed()) {
                                c.close();
                            }
                        }
                    }
                }
                if (!TextUtils.isEmpty(contactLookupKey)) {
                    RecipientDbAdapter dbRecipient = RecipientDbAdapter
                            .openInstance(getApplicationContext());

                    // first, update selected recipient with lookup key
                    if (!dbRecipient.updateRecipientFromChosenLink(mContactLinkRecipientRowId,
                            contactLookupKey)) {
                        showNote(R.string.error_UnableToUpdateRecipientInDB);
                        break;
                    }

                    // next, lookup and remove any recipient invites that match
                    int deleted = deleteMatchingInvites(contactLookupKey);
                    MyLog.d(TAG, deleted + " invites deleted.");
                    if (deleted > 0) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            recreate();
                        } else {
                            // TODO: find better update for <= 2.3
                            // startActivity(getIntent());
                            // finish();
                        }
                    }
                    // last, update names and photos from address book
                    BackgroundSyncUpdatesTask backgroundSyncUpdates = new BackgroundSyncUpdatesTask();
                    backgroundSyncUpdates.execute(new String());

                    mContactLinkRecipientRowId = -1; // reset
                }
                break;
            case RESULT_EDIT_CONTACT_LINK:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    recreate();
                } else {
                    // TODO: find better update for <= 2.3
                    // startActivity(getIntent());
                    // finish();
                }
                // last, update names and photos from address book
                BackgroundSyncUpdatesTask backgroundSyncUpdates = new BackgroundSyncUpdatesTask();
                backgroundSyncUpdates.execute(new String());
                break;
            case RESULT_SELECT_SMS:
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        Cursor c = null;
                        try {
                            c = getContentResolver().query(uri, null, null, null, null);
                            if (c != null && c.moveToFirst()) {
                                String number = c.getString(c.getColumnIndexOrThrow(Phone.NUMBER));
                                mInviteContactLookupKey = c.getString(c
                                        .getColumnIndexOrThrow(Phone.LOOKUP_KEY));
                                sendInviteToSlingPhones(number);
                            }
                        } finally {
                            if (c != null && !c.isClosed()) {
                                c.close();
                            }
                        }
                    }
                }
                break;
            case RESULT_SELECT_EMAIL:
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        Cursor c = null;
                        try {
                            c = getContentResolver().query(uri, null, null, null, null);
                            if (c != null && c.moveToFirst()) {
                                String address = c.getString(c.getColumnIndexOrThrow(Email.DATA));
                                mInviteContactLookupKey = c.getString(c
                                        .getColumnIndexOrThrow(Email.LOOKUP_KEY));
                                sendInviteToSlingEmail(new String[] {
                                    address
                                });
                            }
                        } finally {
                            if (c != null && !c.isClosed()) {
                                c.close();
                            }
                        }
                    }
                }
                break;
            case RESULT_SEND_INVITE:
                if (!TextUtils.isEmpty(mInviteContactLookupKey)) {
                    RecipientDbAdapter dbRecipient = RecipientDbAdapter
                            .openInstance(getApplicationContext());
                    SlingerContact sc = getContactByLookup(mInviteContactLookupKey);
                    mInviteContactLookupKey = null; // reset
                    if (sc != null) {
                        long matchingInviteRowId = getRecipientRowIdMatchingInvite(sc.lookup);
                        long ret = dbRecipient.createInvitedRecipient(System.currentTimeMillis(),
                                sc.lookup, sc.name, sc.photoBytes, matchingInviteRowId);
                        if (ret > -1) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                recreate();
                            } else {
                                // TODO: find better update for <= 2.3
                                // startActivity(getIntent());
                                // finish();
                            }
                            showNote(R.string.state_InvitationAdded);
                        } else {
                            showNote(R.string.error_UnableToSaveRecipientInDB);
                        }
                    }
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
