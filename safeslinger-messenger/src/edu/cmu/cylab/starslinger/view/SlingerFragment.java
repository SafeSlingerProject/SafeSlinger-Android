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
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig;
import edu.cmu.cylab.starslinger.model.ContactAccessor;
import edu.cmu.cylab.starslinger.model.ContactField;
import edu.cmu.cylab.starslinger.model.ImppValue;
import edu.cmu.cylab.starslinger.model.SlingerIdentity;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class SlingerFragment extends Fragment {
    private final ContactAccessor mAccessor = ContactAccessor.getInstance();
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    public static final int RESULT_BEGINEXCHANGE = 301;
    public static final int RESULT_USEROPTIONS = 200;

    private String mVCardString = null;
    private ContactStruct mContact;
    private static String mPreferredName = null;
    private ArrayList<ImppValue> mImpps;
    private List<ContactField> mContactFields;
    private static int mListVisiblePos;
    private static int mListTopOffset;
    private TextView mTextViewUserName;
    private ListView mListViewContactFields;
    private ImageView mImageViewPhoto;
    private Button mButtonStartExchange;
    private Button mButtonSender;
    private static OnSlingerResultListener mResult;
    private Handler mSlingFragmentHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vFrag = inflater.inflate(R.layout.home, container, false);
        mTextViewUserName = (TextView) vFrag.findViewById(R.id.HomeTextViewInstructContact);
        mListViewContactFields = (ListView) vFrag.findViewById(R.id.HomeScrollViewFields);
        mImageViewPhoto = (ImageView) vFrag.findViewById(R.id.HomeImageViewPhoto);
        mButtonStartExchange = (Button) vFrag.findViewById(R.id.HomeButtonStartExchangeProximity);
        mButtonSender = (Button) vFrag.findViewById(R.id.HomeButtonSender);

        updateValues(savedInstanceState);

        mButtonSender.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                doChangeUser();
            }
        });

        mButtonStartExchange.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (generateVCardFromSelected()) {
                    Intent intent = new Intent().putExtra(ExchangeConfig.extra.USER_DATA,
                            mVCardString);
                    sendResultToHost(RESULT_BEGINEXCHANGE, intent.getExtras());
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

        return vFrag;
    }

    public void updateValues(final Bundle extras) {

        mSlingFragmentHandler.removeCallbacks(null);
        mSlingFragmentHandler.postDelayed(new Runnable() {

            @Override
            public void run() {

                if (extras == null) {
                    mListTopOffset = 0;
                    mListVisiblePos = 0;
                }

                // make sure view is already inflated...
                if (mTextViewUserName == null) {
                    return;
                }

                // init
                mTextViewUserName.setText("");

                String contactLookupKey = SafeSlingerPrefs.getContactLookupKey();

                // valid key and push token is required
                SlingerIdentity si = new SlingerIdentity(SafeSlingerPrefs.getPushRegistrationId(),
                        SSUtil.getLocalNotification(SafeSlinger.getApplication()), SafeSlinger
                                .getSenderKey());
                String token = SlingerIdentity.sidPush2DBPush(si);
                String pubkey = SlingerIdentity.sidKey2DBKey(si);
                if (TextUtils.isEmpty(token)) {
                    return;
                } else if (TextUtils.isEmpty(pubkey)) {
                    return;
                }

                mImpps = new ArrayList<ImppValue>();
                mImpps.add(new ImppValue(SafeSlingerConfig.APP_KEY_PUSHTOKEN, SSUtil
                        .finalEncode(token.getBytes())));
                mImpps.add(new ImppValue(SafeSlingerConfig.APP_KEY_PUBKEY, SSUtil
                        .finalEncode(pubkey.getBytes())));

                mPreferredName = SafeSlingerPrefs.getContactName();

                // create a contact
                mContact = new ContactStruct();
                ContactStruct pref = new ContactStruct();
                if (!TextUtils.isEmpty(mPreferredName)) {
                    pref.name = new Name(mPreferredName);
                }
                mContact = BaseActivity.loadContactDataNoDuplicates(getActivity(),
                        contactLookupKey, pref, false);

                // add custom IMPP values directly from third-party...
                for (ImppValue impp : mImpps) {
                    mContact.addContactmethod(Contacts.KIND_IM, ContactMethodsColumns.TYPE_CUSTOM,
                            new String(SSUtil.finalEncode(impp.v)), impp.k, false);
                }

                // we need a real persons name
                if (mContact.name == null || mContact.name.toString() == null
                        || TextUtils.isEmpty(mContact.name.toString().trim())) {
                    showNote(R.string.error_InvalidContactName);
                    return;
                }

                drawContactData(mTextViewUserName, mListViewContactFields, mImageViewPhoto);

            }
        }, 200);
    }

    private static void doChangeUser() {
        Intent intent = new Intent();
        sendResultToHost(RESULT_USEROPTIONS, intent.getExtras());
    }

    private boolean generateVCardFromSelected() {
        // create vCard representation
        try {
            ContactStruct contact = createSelectedContact(mContact);
            VCardComposer composer = new VCardComposer();
            mVCardString = composer.createVCard(contact, VCardComposer.VERSION_VCARD30_INT);
            MyLog.d(TAG, mVCardString);

            // ensure push token and pub key in vCard
            StringBuilder errors = new StringBuilder();
            String name = mContact.name != null ? mContact.name.toString() : null;
            if (TextUtils.isEmpty(name)) {
                errors.append("Your Name is missing").append("\n");
            }
            if (!mVCardString.contains(SafeSlingerConfig.APP_KEY_PUSHTOKEN)) {
                errors.append("Your Push is missing").append("\n");
            }
            if (!mVCardString.contains(SafeSlingerConfig.APP_KEY_PUBKEY)) {
                errors.append("Your PubKey is missing").append("\n");
            }
            if (errors.length() > 0) {
                showNote(errors.toString());
                return false;
            }

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
            boolean checked = SafeSlingerPrefs.getHashContactField(ContactFieldAdapter
                    .getFieldForCompare(item.getName(), item.getValue()));
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
        if (mContact.name != null && !TextUtils.isEmpty(mContact.name.toString())) {
            textViewUserName.setText(mContact.name.toString());
        } else {
            textViewUserName.setText("");
        }

        // draw photo
        imageViewPhoto.setBackgroundColor(Color.TRANSPARENT);
        try {
            if (mContact.photoBytes != null) {
                Bitmap bm = BitmapFactory.decodeByteArray(mContact.photoBytes, 0,
                        mContact.photoBytes.length, null);
                imageViewPhoto.setImageBitmap(bm);
                imageViewPhoto.setAdjustViewBounds(true);

                mContactFields.add(new ContactField( //
                        R.drawable.ic_ks_photo, //
                        getString(R.string.label_Photo), //
                        ""));
            } else {
                imageViewPhoto.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_silhouette));
            }
        } catch (OutOfMemoryError e) {
            imageViewPhoto.setImageDrawable(null);
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
                    default:
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
                    default:
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
                    default:
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

        mAdapter = new ContactFieldAdapter(getActivity(), mContactFields);
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

    public interface OnSlingerResultListener {
        public void onSlingerResultListener(Bundle args);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mResult = (OnSlingerResultListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + OnSlingerResultListener.class.getSimpleName());
        }
    }

    static private void sendResultToHost(int resultCode, Bundle args) {
        if (args == null) {
            args = new Bundle();
        }
        args.putInt(extra.RESULT_CODE, resultCode);
        mResult.onSlingerResultListener(args);
    }

    public static class SlingerAlertDialogFragment extends DialogFragment {

        public static SlingerAlertDialogFragment newInstance(int id) {
            return newInstance(id, new Bundle());
        }

        public static SlingerAlertDialogFragment newInstance(int id, Bundle args) {
            SlingerAlertDialogFragment frag = new SlingerAlertDialogFragment();
            args.putInt(extra.RESULT_CODE, id);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt(extra.RESULT_CODE);
            switch (id) {
                case BaseActivity.DIALOG_HELP:
                    return BaseActivity.xshowHelp(getActivity(), getArguments()).create();
                default:
                    break;
            }
            return super.onCreateDialog(savedInstanceState);
        }
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
                Toast toast = Toast.makeText(this.getActivity(), msg.trim(), Toast.LENGTH_SHORT);
                toast.show();
            } else if (readDuration <= SafeSlingerConfig.LONG_DELAY) {
                Toast toast = Toast.makeText(this.getActivity(), msg.trim(), Toast.LENGTH_LONG);
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
        DialogFragment newFragment = SlingerAlertDialogFragment.newInstance(
                BaseActivity.DIALOG_HELP, args);
        newFragment.show(getFragmentManager(), "dialog");
    }

    public void updateKeypad() {
        // if soft input open, close it...
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        View focus = getActivity().getCurrentFocus();
        if (focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
