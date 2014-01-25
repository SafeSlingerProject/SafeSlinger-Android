
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import a_vcard.android.syncml.pim.VDataBuilder;
import a_vcard.android.syncml.pim.VNode;
import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.Name;
import a_vcard.android.syncml.pim.vcard.VCardException;
import a_vcard.android.syncml.pim.vcard.VCardParser;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;

import edu.cmu.cylab.keyslinger.lib.KsConfig.extra;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;

public class ContactActivity extends SherlockActivity {

    private final ContactAccessor mAccessor = ContactAccessor.getInstance();
    private static final String TAG = KsConfig.LOG_TAG;
    protected ProgressDialog mDlgProg;
    protected String mProgressMsg = null;
    public static final int DIALOG_HELP = 1;
    public static final int DIALOG_ERROR = 2;
    public static final int DIALOG_QUESTION = 3;
    public static final int DIALOG_GRP_SIZE = 4;
    public static final int DIALOG_USEROPTIONS = 5;
    public static final int RESULT_KEYSLINGERIMPORTED = 300;
    public static final int RESULT_KEYSLINGERCANCELED = 301;
    public static final int RESULT_KEYSLINGERCONTACTSEL = 302;
    public static final int RESULT_KEYSLINGERCONTACTEDIT = 303;
    public static final int RESULT_KEYSLINGERCONTACTADD = 304;

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
                Toast toast = Toast.makeText(ContactActivity.this, msg, Toast.LENGTH_SHORT);
                toast.show();
            } else if (readDuration <= ConfigData.LONG_DELAY) {
                Toast toast = Toast.makeText(ContactActivity.this, msg, Toast.LENGTH_LONG);
                toast.show();
            } else {
                showHelp(getString(R.string.app_name), msg);
            }
        }
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

    protected String getContactLookupKeyByName(String name) {
        String contactLookupKey = null;

        // find aggregated contact
        Uri uriLookup = mAccessor.getUriPersonLookupKey();
        if (uriLookup != null) {
            Cursor c = getContentResolver().query(uriLookup, mAccessor.getProjPersonLookupKey(),
                    mAccessor.getQueryPersonLookupKey(name), null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    contactLookupKey = c.getString(c.getColumnIndexOrThrow(Data.LOOKUP_KEY));
                }
                c.close();
            }
        }
        return contactLookupKey;
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

    protected ContactStruct loadContactDataNoDuplicates(Context ctx, String contactLookupKey,
            ContactStruct in, boolean removeMatches) {
        // order: name, photo, phone, im, email, url, postal, title, org

        // create a parallel here for storing the data, use the table rows
        ContentResolver resolver = getContentResolver();
        ContactStruct contact = (in != null) ? in : new ContactStruct();

        // get name, retain passed in name
        if (contact.name == null || TextUtils.isEmpty(contact.name.toString())) {
            Uri uriName = mAccessor.getUriName(resolver, contactLookupKey);
            if (uriName != null) {
                Cursor names = getContentResolver().query(uriName, mAccessor.getProjName(),
                        mAccessor.getQueryName(), null, null);
                if (names != null) {
                    while (names.moveToNext()) {
                        mAccessor.addName(contact, names);
                    }
                    names.close();
                }
            }
        }

        // get photo
        Uri uriPhoto = mAccessor.getUriPhoto(resolver, contactLookupKey);
        if (uriPhoto != null) {
            Cursor photos = getContentResolver().query(uriPhoto, mAccessor.getProjPhoto(),
                    mAccessor.getQueryPhoto(), null, null);
            if (photos != null) {
                while (photos.moveToNext()) {
                    mAccessor.addPhoto(contact, photos);
                }
                photos.close();
            }
        }

        // get phone
        Uri uriPhone = mAccessor.getUriPhone(resolver, contactLookupKey);
        if (uriPhone != null) {
            Cursor phones = getContentResolver().query(uriPhone, mAccessor.getProjPhone(),
                    mAccessor.getQueryPhone(), null, null);
            if (phones != null) {
                while (phones.moveToNext()) {
                    mAccessor.addPhone(ctx, contact, phones, removeMatches);
                }
                phones.close();
            }
        }

        // get IM
        Uri uriIm = mAccessor.getUriIM(resolver, contactLookupKey);
        if (uriIm != null) {
            Cursor ims = getContentResolver().query(uriIm, mAccessor.getProjIM(),
                    mAccessor.getQueryIM(), null, null);
            if (ims != null) {
                while (ims.moveToNext()) {
                    mAccessor.addIM(contact, ims, this, removeMatches);
                }
                ims.close();
            }
        }

        // get email
        Uri uriEmail = mAccessor.getUriEmail(resolver, contactLookupKey);
        if (uriEmail != null) {
            Cursor emails = getContentResolver().query(uriEmail, mAccessor.getProjEmail(),
                    mAccessor.getQueryEmail(), null, null);
            if (emails != null) {
                while (emails.moveToNext()) {
                    mAccessor.addEmail(contact, emails, removeMatches);
                }
                emails.close();
            }
        }

        // get Url
        Uri uriUrl = mAccessor.getUriUrl(resolver, contactLookupKey);
        if (uriUrl != null) {
            Cursor urls = getContentResolver().query(uriUrl, mAccessor.getProjUrl(),
                    mAccessor.getQueryUrl(), null, null);
            if (urls != null) {
                while (urls.moveToNext()) {
                    mAccessor.addUrl(contact, urls, this, removeMatches);
                }
                urls.close();
            }
        }

        // get postal
        Uri uriPostal = mAccessor.getUriPostal(resolver, contactLookupKey);
        if (uriPostal != null) {
            Cursor postals = getContentResolver().query(uriPostal, mAccessor.getProjPostal(),
                    mAccessor.getQueryPostal(), null, null);
            if (postals != null) {
                while (postals.moveToNext()) {
                    mAccessor.addPostal(contact, postals, removeMatches);
                }
                postals.close();
            }
        }

        return contact;
    }

    protected void setResultForParent(int resultCode) {
        if (getParent() == null) {
            setResult(resultCode);
        } else {
            getParent().setResult(resultCode);
        }
    }

    protected void setResultForParent(int resultCode, Intent data) {
        if (getParent() == null) {
            setResult(resultCode, data);
        } else {
            getParent().setResult(resultCode, data);
        }
    }

    protected void showProgress(String msg, boolean indeterminate) {
        MyLog.i(TAG, msg);
        mDlgProg = new ProgressDialog(this);
        mDlgProg.setProgressStyle(indeterminate ? ProgressDialog.STYLE_SPINNER
                : ProgressDialog.STYLE_HORIZONTAL);
        mDlgProg.setMessage(msg);
        mProgressMsg = msg;
        mDlgProg.setCancelable(true);
        mDlgProg.setIndeterminate(indeterminate);
        mDlgProg.setProgress(0);
        mDlgProg.show();
    }

    protected void showProgressUpdate(int value, String msg) {
        if (mDlgProg != null) {
            mDlgProg.setProgress(value);
            if (msg != null) {
                mDlgProg.setMessage(msg);
            }
        }
    }

    protected void hideProgress() {
        if (mDlgProg != null) {
            mDlgProg.dismiss();
        }
    }

    @Override
    public void onUserInteraction() {
        // update the cache timeout
        SafeSlinger.updateCachedPassPhrase(ConfigData.loadPrefKeyIdString(getApplicationContext()));
    }

}
