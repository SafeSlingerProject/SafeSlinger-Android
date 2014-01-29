
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
import a_vcard.android.provider.Contacts.ContactMethodsColumns;
import a_vcard.android.syncml.pim.vcard.Address;
import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.ContactStruct.ContactMethod;
import a_vcard.android.syncml.pim.vcard.ContactStruct.PhoneData;
import a_vcard.android.syncml.pim.vcard.Name;
import a_vcard.android.syncml.pim.vcard.VCardComposer;
import a_vcard.android.syncml.pim.vcard.VCardException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;

import edu.cmu.cylab.keyslinger.lib.KsConfig.extra;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.R;

public class StartActivity extends ContactActivity {
    private final ContactAccessor mAccessor = ContactAccessor.getInstance();
    private static final String TAG = KsConfig.LOG_TAG;
    private static final int MENU_HELP = 1;
    public static final int RESULT_SMLGROUP = 12;
    public static final int RESULT_LRGGROUP = 13;

    private String mContactLookupKey = null;
    private String mVCardString = null;
    private ContactStruct mContact;
    private Context mCtx;
    private static String mPreferredName = null;
    private static String mPreferredPhone = null;
    private static String mPreferredEmail = null;
    private static int mPreferredPhoneType = Phone.TYPE_MOBILE;
    private static int mPreferredEmailType = Email.TYPE_HOME;
    private ArrayList<ImppValue> mImpps;
    private List<ContactField> mContactFields;
    private static int mListVisiblePos;
    private static int mListTopOffset;
    private TextView mTextViewUserName;
    private ListView mListViewContactFields;
    private ImageView mImageViewPhoto;

    public class ImppValue {
        public String k = null;
        public byte[] v = null;

        public ImppValue(String key, byte[] value) {
            k = key;
            v = value;
        }
    }

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
                showHelp(getString(R.string.title_home), getString(R.string.help_home)
                        + getString(R.string.help_identity_menu));
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
        bar.setSubtitle(R.string.title_home);
        mCtx = this.getApplicationContext();

        setContentView(R.layout.home);

        mTextViewUserName = (TextView) findViewById(R.id.HomeTextViewInstructContact);
        mListViewContactFields = (ListView) findViewById(R.id.HomeScrollViewFields);
        mImageViewPhoto = (ImageView) findViewById(R.id.HomeImageViewPhoto);

        // init
        mTextViewUserName.setText("");

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            mContactLookupKey = extras.getString(extra.CONTACT_LOOKUP_KEY);
            mImpps = new ArrayList<ImppValue>();

            if (!TextUtils.isEmpty(mContactLookupKey)) {
                String keyName = null;
                byte[] keyData = null;

                int i = 0;
                do {
                    // import trusted items from exchange...
                    keyName = extras.getString(extra.CONTACT_KEYNAME_PREFIX + i);
                    keyData = extras.getByteArray(extra.CONTACT_VALUE_PREFIX + i);
                    if (keyData != null) {
                        mImpps.add(new ImppValue(keyName, keyData));
                    }
                    i++;
                } while (keyName != null);
            }

            mPreferredName = extras.getString(extra.NAME);
            mPreferredPhone = extras.getString(extra.PHONE);
            mPreferredEmail = extras.getString(extra.EMAIL);
            mPreferredPhoneType = extras.getInt(extra.PHONE_TYPE);
            mPreferredEmailType = extras.getInt(extra.EMAIL_TYPE);
        }

        // Read in saved
        if (!TextUtils.isEmpty(mContactLookupKey)) {
            // create a contact
            mContact = new ContactStruct();
            ContactStruct pref = new ContactStruct();
            if (!TextUtils.isEmpty(mPreferredName)) {
                pref.name = new Name(mPreferredName);
            }
            if (!TextUtils.isEmpty(mPreferredPhone)) {
                pref.addPhone(mPreferredPhoneType, mPreferredPhone, null, true);
            }
            if (!TextUtils.isEmpty(mPreferredEmail)) {
                pref.addContactmethod(Contacts.KIND_EMAIL, mPreferredEmailType, mPreferredEmail,
                        null, true);
            }
            mContact = loadContactDataNoDuplicates(this, mContactLookupKey, pref, false);

            // add custom IMPP values directly from third-party...
            for (ImppValue impp : mImpps) {
                mContact.addContactmethod(Contacts.KIND_IM, ContactMethodsColumns.TYPE_CUSTOM,
                        new String(KsConfig.finalEncode(impp.v)), impp.k, false);
            }

            // we need a real persons name
            if (mContact.name == null || mContact.name.toString() == null
                    || TextUtils.isEmpty(mContact.name.toString().trim())) {
                showNote(R.string.error_InvalidContactName);
                setResultForParent(RESULT_CANCELED);
                finish();
                return;
            }

            drawContactData(mTextViewUserName, mListViewContactFields, mImageViewPhoto);
        }

        Button buttonStartExchange = (Button) findViewById(R.id.HomeButtonStartExchangeProximity);
        Button buttonSender = (Button) findViewById(R.id.HomeButtonSender);

        buttonSender.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showChangeSenderOptions();
            }
        });

        buttonStartExchange.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (generateVCardFromSelected()) {
                    Intent data = new Intent().putExtra(extra.USER_DATA, mVCardString);
                    setResultForParent(RESULT_OK, data);
                    finish();
                }
            }
        });

        mListViewContactFields.setOnScrollListener(new OnScrollListener() {

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
                    View v = mListViewContactFields.getChildAt(0);
                    mListTopOffset = (v == null) ? 0 : v.getTop();
                }
            }
        });
    }

    protected void showChangeSenderOptions() {
        if (!isFinishing()) {
            removeDialog(DIALOG_USEROPTIONS);
            showDialog(DIALOG_USEROPTIONS);
        }
    }

    protected AlertDialog.Builder xshowChangeSenderOptions(final Activity act) {
        final CharSequence[] items = new CharSequence[] {
                act.getText(R.string.menu_Edit), act.getText(R.string.menu_CreateNew),
                act.getText(R.string.menu_UseAnother)
        };
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_MyIdentity);
        ad.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
                switch (item) {
                    case 0: // edit
                        setResultForParent(RESULT_KEYSLINGERCONTACTEDIT);
                        finish();
                        break;
                    case 1: // new
                        setResultForParent(RESULT_KEYSLINGERCONTACTADD);
                        finish();
                        break;
                    case 2: // change
                        setResultForParent(RESULT_KEYSLINGERCONTACTSEL);
                        finish();
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

    private boolean generateVCardFromSelected() {
        // create vCard representation
        try {
            ContactStruct contact = createSelectedContact(mContact);
            VCardComposer composer = new VCardComposer();
            mVCardString = composer.createVCard(contact, VCardComposer.VERSION_VCARD30_INT);

            MyLog.d(TAG, mVCardString);
            return true;
        } catch (VCardException e) {
            showNote(e.getLocalizedMessage());
            return false;
        }
    }

    private ContactStruct createSelectedContact(ContactStruct contact) {
        // order: name, photo, phone, im, email, url, postal, title, org

        ContactStruct cs = new ContactStruct();
        int row = 0;

        cs.name = contact.name;

        if (contact.photoBytes != null) {
            if (isDataChecked(row)) {
                cs.photoBytes = contact.photoBytes;
            }
            row++;
        }
        if (contact.phoneList != null) {
            cs.phoneList = new ArrayList<PhoneData>();
            for (PhoneData p : contact.phoneList) {
                if (isDataChecked(row)) {
                    cs.phoneList.add(p);
                }
                row++;
            }
        }
        if (contact.contactmethodList != null) {
            cs.contactmethodList = new ArrayList<ContactMethod>();
            for (ContactMethod i : contact.contactmethodList) {
                if (i.kind == Contacts.KIND_IM) {
                    if (isDataChecked(row))
                        cs.contactmethodList.add(i);
                    row++;
                }
            }
            for (ContactMethod e : contact.contactmethodList) {
                if (e.kind == Contacts.KIND_EMAIL) {
                    if (isDataChecked(row))
                        cs.contactmethodList.add(e);
                    row++;
                }
            }
            for (ContactMethod u : contact.contactmethodList) {
                if (u.kind == Contacts.KIND_URL) {
                    if (isDataChecked(row))
                        cs.contactmethodList.add(u);
                    row++;
                }
            }
        }
        if (contact.addressList != null) {
            cs.addressList = new ArrayList<Address>();
            for (Address a : contact.addressList) {
                if (isDataChecked(row)) {
                    cs.addressList.add(a);
                }
                row++;
            }
        }
        return cs;
    }

    private boolean isDataChecked(int i) {
        boolean add = false;
        ContactField item = mContactFields.get(i);
        if (item != null) {
            boolean forceChecked = item.getForceChecked();
            boolean checked = ConfigData.loadPrefContactField(this,
                    ContactFieldAdapter.getFieldForCompare(item.getName(), item.getValue()));
            if (checked || forceChecked) {
                add = true;
            }
        }
        return add;
    }

    private void drawContactData(TextView textViewUserName, ListView listViewContactFields,
            ImageView imageViewPhoto) {
        // order: name, photo, phone, im, email, url, postal, title, org

        // make sure view is already inflated...
        if (listViewContactFields == null) {
            return;
        }

        mContactFields = new ArrayList<ContactField>();
        ContactFieldAdapter mAdapter;

        // draw name
        if (mContact.name != null)
            textViewUserName.setText(mContact.name.toString());

        // draw photo
        imageViewPhoto.setBackgroundColor(Color.TRANSPARENT);
        if (mContact.photoBytes != null) {
            try {
                Bitmap bm = BitmapFactory.decodeByteArray(mContact.photoBytes, 0,
                        mContact.photoBytes.length, null);
                imageViewPhoto.setImageBitmap(bm);
                imageViewPhoto.setAdjustViewBounds(true);
            } catch (OutOfMemoryError e) {
                imageViewPhoto.setImageDrawable(null);
            }

            mContactFields.add(new ContactField( //
                    R.drawable.ic_ks_photo, //
                    getString(R.string.label_Photo), //
                    ""));
        }

        // draw from phones
        if (mContact.phoneList != null)
            for (PhoneData m : mContact.phoneList) {

                mContactFields.add(new ContactField( //
                        R.drawable.ic_ks_phone, //
                        getPhoneTypeLabel(m.type, m.label),//
                        m.data));
            }

        // draw from contact methods
        if (mContact.contactmethodList != null) {

            // ims
            for (ContactMethod i : mContact.contactmethodList) {
                switch (i.kind) {
                    case Contacts.KIND_IM:

                        boolean isCustom = (i.kind == Contacts.KIND_IM)
                                && mAccessor.isCustomIm(i.label);
                        String data = i.data;
                        String typeDisplay = "";
                        int iconid = 0;
                        boolean forceCheck = false;

                        if (isCustom) {
                            iconid = R.drawable.ic_ks_key;
                            data = i.label;
                            typeDisplay = "";
                            forceCheck = true;
                        } else {
                            iconid = R.drawable.ic_ks_im;
                            typeDisplay = mAccessor.getDesc(i.kind, i.type, i.label);
                        }

                        mContactFields.add(new ContactField( //
                                iconid, //
                                typeDisplay, //
                                delimitedString(data), forceCheck));
                        break;
                }
            }

            // emails
            for (ContactMethod e : mContact.contactmethodList) {
                switch (e.kind) {
                    case Contacts.KIND_EMAIL:
                        String data = e.data;
                        String typeDisplay = "";
                        int iconid = 0;
                        boolean forceCheck = false;

                        iconid = R.drawable.ic_ks_email;
                        typeDisplay = getEmailTypeLabel(e.type, e.label);

                        mContactFields.add(new ContactField( //
                                iconid, //
                                typeDisplay, //
                                delimitedString(data), forceCheck));
                        break;
                }
            }

            // urls
            for (ContactMethod u : mContact.contactmethodList) {
                switch (u.kind) {
                    case Contacts.KIND_URL:

                        String data = u.data;
                        String typeDisplay = "";
                        int iconid = 0;
                        boolean forceCheck = false;

                        iconid = R.drawable.ic_ks_url;

                        mContactFields.add(new ContactField( //
                                iconid, //
                                typeDisplay, //
                                delimitedString(data), forceCheck));
                        break;
                }
            }
        }

        // draw from addresses
        if (mContact.addressList != null)
            for (Address m : mContact.addressList) {

                mContactFields.add(new ContactField( //
                        R.drawable.ic_ks_postal, //
                        getStructuredPostalTypeLabel(m.getType(), m.getLabel()),//
                        delimitedString(m.toString())));
            }

        mAdapter = new ContactFieldAdapter(StartActivity.this, mContactFields);
        listViewContactFields.setAdapter(mAdapter);

        // restore list position
        listViewContactFields.setSelectionFromTop(mListVisiblePos, mListTopOffset);
    }

    private String delimitedString(String str) {
        str = str.replaceAll("\r", "\n");
        str = str.replaceAll("\n\n", "\n");
        str = str.replaceAll("\n\n", "\n");
        return str.replaceAll("\n", ", ").trim();
    }

    private String getPhoneTypeLabel(int type, String label) {
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
                return Phone.getTypeLabel(this.getResources(), type, label).toString();
        }
    }

    private String getEmailTypeLabel(int type, String label) {
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
                return Email.getTypeLabel(this.getResources(), type, label).toString();
        }
    }

    private String getStructuredPostalTypeLabel(int type, String label) {
        switch (type) {
            case StructuredPostal.TYPE_HOME:
                return getString(R.string.label_hometag);
            case StructuredPostal.TYPE_WORK:
                return getString(R.string.label_worktag);
            case StructuredPostal.TYPE_OTHER:
                return getString(R.string.label_othertag);
            default:
                return StructuredPostal.getTypeLabel(this.getResources(), type, label).toString();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(StartActivity.this, args).create();
            case DIALOG_USEROPTIONS:
                return xshowChangeSenderOptions(StartActivity.this).create();
        }
        return super.onCreateDialog(id);
    }
}
