
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
import java.util.Locale;

import a_vcard.android.provider.Contacts;
import a_vcard.android.provider.Contacts.ContactMethodsColumns;
import a_vcard.android.syncml.pim.vcard.Address;
import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.ContactStruct.ContactMethod;
import a_vcard.android.syncml.pim.vcard.ContactStruct.OrganizationData;
import a_vcard.android.syncml.pim.vcard.ContactStruct.PhoneData;
import a_vcard.android.syncml.pim.vcard.Name;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.widget.Toast;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;

public class ContactAccessorApi5 extends ContactAccessor {

    private static final String TAG = KsConfig.LOG_TAG;

    private final String AIM = "AIM";
    private final String GTALK = "GTalk";
    private final String ICQ = "ICQ";
    private final String JABBER = "JABBER";
    private final String MSN = "MSN";
    private final String QQ = "QQ";
    private final String SKYPE = "SKYPE";
    private final String YAHOO = "Yahoo";
    private final String NETMEETING = "NetMeeting";

    public Uri getDataUri(ContentResolver resolver, String contactLookupKey) {
        Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                contactLookupKey);
        if (lookupUri != null) {
            Uri personUri = ContactsContract.Contacts.lookupContact(resolver, lookupUri);
            if (personUri != null) {
                return Uri.withAppendedPath(personUri,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public Uri getUriPersonLookupKey() {
        return Data.CONTENT_URI;
    }

    @Override
    public String[] getProjPersonLookupKey() {
        return new String[] {
                StructuredName.DISPLAY_NAME, Data.LOOKUP_KEY, BaseColumns._ID
        };
    }

    @Override
    public String getQueryPersonLookupKey(String name) {
        return StructuredName.DISPLAY_NAME + " = " + DatabaseUtils.sqlEscapeString("" + name);
    }

    @Override
    public Uri getUriName(ContentResolver resolver, String contactLookupKey) {
        return getDataUri(resolver, contactLookupKey);
    }

    @Override
    public String[] getProjName() {
        return new String[] {
                StructuredName.MIMETYPE, StructuredName.DISPLAY_NAME, StructuredName.FAMILY_NAME,
                StructuredName.GIVEN_NAME, StructuredName.MIDDLE_NAME, StructuredName.PREFIX,
                StructuredName.SUFFIX, StructuredName.IS_PRIMARY, StructuredName.IS_SUPER_PRIMARY
        };
    }

    @Override
    public String getQueryName() {
        return StructuredName.MIMETYPE + " = "
                + DatabaseUtils.sqlEscapeString("" + StructuredName.CONTENT_ITEM_TYPE);
    }

    @Override
    public boolean addName(ContactStruct contact, Cursor names) {
        String display = names.getString(names.getColumnIndex(StructuredName.DISPLAY_NAME));
        String family = names.getString(names.getColumnIndex(StructuredName.FAMILY_NAME));
        String given = names.getString(names.getColumnIndex(StructuredName.GIVEN_NAME));
        String middle = names.getString(names.getColumnIndex(StructuredName.MIDDLE_NAME));
        String prefix = names.getString(names.getColumnIndex(StructuredName.PREFIX));
        String suffix = names.getString(names.getColumnIndex(StructuredName.SUFFIX));
        boolean super_primary = (names.getInt(names
                .getColumnIndexOrThrow(StructuredName.IS_SUPER_PRIMARY)) != 0);
        if (!TextUtils.isEmpty(display) && isNameNew(contact, display, super_primary)) {
            contact.name = new Name(family, given, middle, prefix, suffix,
                    Name.NAME_ORDER_TYPE_ENGLISH);
            return true;
        }
        return false;
    }

    @Override
    public Uri getUriPhone(ContentResolver resolver, String contactLookupKey) {
        return getDataUri(resolver, contactLookupKey);
    }

    @Override
    public String[] getProjPhone() {
        return new String[] {
                Phone.MIMETYPE, Phone.NUMBER, Phone.TYPE, Phone.IS_PRIMARY, Phone.LABEL
        };
    }

    @Override
    public String getQueryPhone() {
        return Phone.MIMETYPE + " = " + DatabaseUtils.sqlEscapeString("" + Phone.CONTENT_ITEM_TYPE);
    }

    @Override
    public boolean addPhone(Context ctx, ContactStruct contact, Cursor phones, boolean removeMatches) {
        String number = phones.getString(phones.getColumnIndexOrThrow(Phone.NUMBER));
        String label = phones.getString(phones.getColumnIndexOrThrow(Phone.LABEL));
        int type = phones.getInt(phones.getColumnIndexOrThrow(Phone.TYPE));
        boolean primary = (phones.getInt(phones.getColumnIndexOrThrow(Phone.IS_PRIMARY)) != 0);
        if (!TextUtils.isEmpty(number) && isPhoneNew(ctx, contact, number, type, removeMatches)) {
            contact.addPhone(type, number, label, primary);
            return true;
        }
        return false;
    }

    @Override
    public Uri getUriEmail(ContentResolver resolver, String contactLookupKey) {
        return getDataUri(resolver, contactLookupKey);
    }

    @Override
    public String[] getProjEmail() {
        return new String[] {
                Email.MIMETYPE, Email.DATA, Email.TYPE, Email.IS_PRIMARY, Email.LABEL
        };
    }

    @Override
    public String getQueryEmail() {
        return Email.MIMETYPE + " = " + DatabaseUtils.sqlEscapeString("" + Email.CONTENT_ITEM_TYPE);
    }

    @Override
    public boolean addEmail(ContactStruct contact, Cursor emails, boolean removeMatches) {
        String email = emails.getString(emails.getColumnIndexOrThrow(Email.DATA));
        String label = emails.getString(emails.getColumnIndexOrThrow(Email.LABEL));
        int type = emails.getInt(emails.getColumnIndexOrThrow(Email.TYPE));
        boolean primary = (emails.getInt(emails.getColumnIndexOrThrow(Email.IS_PRIMARY)) != 0);
        if (!TextUtils.isEmpty(email) && isEmailNew(contact, email, type, removeMatches)) {
            contact.addContactmethod(Contacts.KIND_EMAIL, type, email, label, primary);
            return true;
        }
        return false;
    }

    @Override
    public Uri getUriPostal(ContentResolver resolver, String contactLookupKey) {
        return getDataUri(resolver, contactLookupKey);
    }

    @Override
    public String[] getProjPostal() {
        return new String[] {
                StructuredPostal.MIMETYPE, StructuredPostal.TYPE, StructuredPostal.IS_PRIMARY,
                StructuredPostal.LABEL, StructuredPostal.POBOX, StructuredPostal.STREET,
                StructuredPostal.CITY, StructuredPostal.REGION, StructuredPostal.POSTCODE,
                StructuredPostal.COUNTRY, StructuredPostal.NEIGHBORHOOD,
                StructuredPostal.FORMATTED_ADDRESS
        };
    }

    @Override
    public String getQueryPostal() {
        return StructuredPostal.MIMETYPE + " = "
                + DatabaseUtils.sqlEscapeString("" + StructuredPostal.CONTENT_ITEM_TYPE);
    }

    @Override
    public boolean addPostal(ContactStruct contact, Cursor postals, boolean removeMatches) {
        String address = postals.getString(postals
                .getColumnIndex(StructuredPostal.FORMATTED_ADDRESS));
        String poBox = postals.getString(postals.getColumnIndex(StructuredPostal.POBOX));
        String street = postals.getString(postals.getColumnIndex(StructuredPostal.STREET));
        String nbrhood = postals.getString(postals.getColumnIndex(StructuredPostal.NEIGHBORHOOD));
        String city = postals.getString(postals.getColumnIndex(StructuredPostal.CITY));
        String state = postals.getString(postals.getColumnIndex(StructuredPostal.REGION));
        String postCode = postals.getString(postals.getColumnIndex(StructuredPostal.POSTCODE));
        String country = postals.getString(postals.getColumnIndex(StructuredPostal.COUNTRY));
        String label = postals.getString(postals.getColumnIndexOrThrow(StructuredPostal.LABEL));
        int type = postals.getInt(postals.getColumnIndexOrThrow(StructuredPostal.TYPE));
        boolean primary = (postals.getInt(postals
                .getColumnIndexOrThrow(StructuredPostal.IS_PRIMARY)) != 0);

        if (!TextUtils.isEmpty(address)
                && isPostalNew(contact, address, poBox, street, nbrhood, city, state, postCode,
                        type, removeMatches)) {
            contact.addAddress(type, address, poBox, nbrhood, street, city, state, postCode,
                    country, label, primary);
            return true;
        }
        return false;
    }

    @Override
    public Uri getUriPhoto(ContentResolver resolver, String contactLookupKey) {
        return getDataUri(resolver, contactLookupKey);
    }

    @Override
    public String[] getProjPhoto() {
        return new String[] {
                Photo.MIMETYPE, Photo.PHOTO, Photo.IS_PRIMARY, Photo.IS_SUPER_PRIMARY
        };
    }

    @Override
    public String getQueryPhoto() {
        return Photo.MIMETYPE + " = " + DatabaseUtils.sqlEscapeString("" + Photo.CONTENT_ITEM_TYPE);
    }

    @Override
    public boolean addPhoto(ContactStruct contact, Cursor photos) {
        byte[] photo = photos.getBlob(photos.getColumnIndexOrThrow(Photo.PHOTO));
        boolean super_primary = (photos.getInt(photos.getColumnIndex(Photo.IS_SUPER_PRIMARY)) != 0);
        if (photo != null && isPhotoNew(contact, photo, super_primary)) {
            contact.photoBytes = photo;
            contact.photoType = null;
            return true;
        }
        return false;
    }

    @Override
    public Uri getUriOrg(ContentResolver resolver, String contactLookupKey) {
        return getDataUri(resolver, contactLookupKey);
    }

    @Override
    public String[] getProjOrg() {
        return new String[] {
                Organization.MIMETYPE, Organization.TITLE, Organization.COMPANY, Organization.TYPE,
                Organization.IS_PRIMARY, Organization.LABEL
        };
    }

    @Override
    public String getQueryOrg() {
        return Organization.MIMETYPE + " = "
                + DatabaseUtils.sqlEscapeString("" + Organization.CONTENT_ITEM_TYPE);
    }

    @Override
    public boolean addOrg(ContactStruct contact, Cursor orgs, boolean removeMatches) {
        boolean ret = false;
        String title = orgs.getString(orgs.getColumnIndexOrThrow(Organization.TITLE));
        String company = orgs.getString(orgs.getColumnIndexOrThrow(Organization.COMPANY));
        int type = orgs.getInt(orgs.getColumnIndexOrThrow(Organization.TYPE));
        boolean primary = (orgs.getInt(orgs.getColumnIndexOrThrow(Organization.IS_PRIMARY)) != 0);
        if (!TextUtils.isEmpty(company) && isOrgNew(contact, company, title, removeMatches)) {
            contact.addOrganization(type, company, title, primary);
            if (primary) {
                contact.title = title;
                contact.company = company;
                ret = true;
            }
        }
        return ret;
    }

    @Override
    public Uri getUriIM(ContentResolver resolver, String contactLookupKey) {
        return getDataUri(resolver, contactLookupKey);
    }

    @Override
    public String[] getProjIM() {
        return new String[] {
                Im.MIMETYPE, Im.DATA, Im.TYPE, Im.PROTOCOL, Im.CUSTOM_PROTOCOL, Im.IS_PRIMARY,
                Im.LABEL
        };
    }

    @Override
    public String getQueryIM() {
        return Im.MIMETYPE + " = " + DatabaseUtils.sqlEscapeString("" + Im.CONTENT_ITEM_TYPE);
    }

    @Override
    public boolean addIM(ContactStruct contact, Cursor ims, Context ctx, boolean removeMatches) {
        boolean ret = false;
        String data = ims.getString(ims.getColumnIndexOrThrow(Im.DATA));
        String protocol = ims.getString(ims.getColumnIndexOrThrow(Im.PROTOCOL));
        String label = ims.getString(ims.getColumnIndexOrThrow(Im.LABEL));
        int type = ims.getInt(ims.getColumnIndexOrThrow(Im.TYPE));
        boolean primary = (ims.getInt(ims.getColumnIndexOrThrow(Im.IS_PRIMARY)) != 0);
        int prot = 0;
        if (!TextUtils.isEmpty(protocol) && !TextUtils.isEmpty(data)) {
            String value = data;
            prot = Integer.valueOf(protocol);
            if (prot == Im.PROTOCOL_CUSTOM) {
                return false;
            } else {
                label = lookupProviderNameFromId(prot).toLowerCase(Locale.US);
            }
            if (!TextUtils.isEmpty(label) && isImNew(contact, value, label, removeMatches)) {
                contact.addContactmethod(Contacts.KIND_IM, type, value, label, primary);
                ret = true;
            }
        }
        return ret;
    }

    @Override
    public Uri getUriUrl(ContentResolver resolver, String contactLookupKey) {
        return getDataUri(resolver, contactLookupKey);
    }

    @Override
    public String[] getProjUrl() {
        return new String[] {
                Website.MIMETYPE, Website.URL, Website.TYPE, Website.IS_PRIMARY, Website.LABEL
        };
    }

    @Override
    public String getQueryUrl() {
        return Website.MIMETYPE + " = "
                + DatabaseUtils.sqlEscapeString("" + Website.CONTENT_ITEM_TYPE);
    }

    @Override
    public boolean addUrl(ContactStruct contact, Cursor urls, Context ctx, boolean removeMatches) {
        String url = urls.getString(urls.getColumnIndexOrThrow(Website.URL));
        String label = urls.getString(urls.getColumnIndexOrThrow(Website.LABEL));
        int type = urls.getInt(urls.getColumnIndexOrThrow(Website.TYPE));
        boolean primary = (urls.getInt(urls.getColumnIndexOrThrow(Website.IS_PRIMARY)) != 0);

        if (!TextUtils.isEmpty(url) && isUrlNew(contact, url, type, removeMatches)) {
            contact.addContactmethod(Contacts.KIND_URL,
                    (type > ContactMethodsColumns.TYPE_OTHER ? ContactMethodsColumns.TYPE_OTHER
                            : type), url, label, primary);
            return true;
        }
        return false;
    }

    @Override
    public String lookupProviderNameFromId(int protocol) {
        switch (protocol) {
            case Im.PROTOCOL_GOOGLE_TALK:
                return GTALK;
            case Im.PROTOCOL_AIM:
                return AIM;
            case Im.PROTOCOL_MSN:
                return MSN;
            case Im.PROTOCOL_YAHOO:
                return YAHOO;
            case Im.PROTOCOL_ICQ:
                return ICQ;
            case Im.PROTOCOL_JABBER:
                return JABBER;
            case Im.PROTOCOL_SKYPE:
                return SKYPE;
            case Im.PROTOCOL_QQ:
                return QQ;
            case Im.PROTOCOL_NETMEETING:
                return NETMEETING;
        }
        return null;
    }

    @Override
    public int lookupProviderIdFromName(String name) {
        if (GTALK.equalsIgnoreCase(name))
            return Im.PROTOCOL_GOOGLE_TALK;
        else if (AIM.equalsIgnoreCase(name))
            return Im.PROTOCOL_AIM;
        else if (MSN.equalsIgnoreCase(name))
            return Im.PROTOCOL_MSN;
        else if (YAHOO.equalsIgnoreCase(name))
            return Im.PROTOCOL_YAHOO;
        else if (ICQ.equalsIgnoreCase(name))
            return Im.PROTOCOL_ICQ;
        else if (JABBER.equalsIgnoreCase(name))
            return Im.PROTOCOL_JABBER;
        else if (SKYPE.equalsIgnoreCase(name))
            return Im.PROTOCOL_SKYPE;
        else if (QQ.equalsIgnoreCase(name))
            return Im.PROTOCOL_QQ;
        else if (NETMEETING.equalsIgnoreCase(name))
            return Im.PROTOCOL_NETMEETING;
        else
            return Im.PROTOCOL_CUSTOM;
    }

    @Override
    public String getDesc(int kind, int type, String label) {
        switch (kind) {
            case Contacts.KIND_IM:
                int prot = lookupProviderIdFromName(label);
                if (isCustomIm(label))
                    return label;
                return lookupProviderNameFromId(prot);
            default:
                return null;
        }
    }

    private ContentValues valuesName(ContactStruct contact) {
        ContentValues val = new ContentValues();
        val.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        if (contact.name != null) {
            val.put(StructuredName.FAMILY_NAME, contact.name.getFamily());
            val.put(StructuredName.GIVEN_NAME, contact.name.getGiven());
            val.put(StructuredName.MIDDLE_NAME, contact.name.getMiddle());
            val.put(StructuredName.PREFIX, contact.name.getPrefix());
            val.put(StructuredName.SUFFIX, contact.name.getSuffix());
            val.put(StructuredName.DISPLAY_NAME, contact.name.toString());
        }
        return val;
    }

    private ContentValues valuesPhone(PhoneData phone) {
        ContentValues val = new ContentValues();
        val.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        if (phone.data != null)
            val.put(Phone.NUMBER, phone.data);
        if (phone.label != null)
            val.put(Phone.LABEL, phone.label);
        val.put(Phone.IS_PRIMARY, phone.isPrimary ? 1 : 0);
        val.put(Phone.TYPE, phone.type);
        return val;
    }

    private ContentValues valuesEmail(ContactMethod email) {
        ContentValues val = new ContentValues();
        val.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
        if (email.label != null)
            val.put(Email.LABEL, email.label);
        val.put(Email.IS_PRIMARY, email.isPrimary ? 1 : 0);
        if (email.data != null)
            val.put(Email.DATA, email.data);
        val.put(Email.TYPE, email.type);
        return val;
    }

    private ContentValues valuesUrl(ContactMethod url) {
        ContentValues val = new ContentValues();
        val.put(Data.MIMETYPE, Website.CONTENT_ITEM_TYPE);
        if (url.label != null)
            val.put(Website.LABEL, url.label);
        val.put(Website.IS_PRIMARY, url.isPrimary ? 1 : 0);
        if (url.data != null)
            val.put(Website.DATA, url.data);
        val.put(Website.TYPE, url.type);
        return val;
    }

    private ContentValues valuesIm(ContactMethod im) {

        if ((im.data != null || im.label.contains(":"))) {
            // see if extra type from iphone was parsed
            String[] split = im.data.split(":");
            if (split.length == 2) {
                if (isCustomIm(split[0])) {
                    im.label = split[0];
                    im.data = split[1];
                }
            }
        }

        int protocol = lookupProviderIdFromName(im.label);
        boolean isCustom = isCustomIm(im.label);
        String label = isCustom ? null : "";
        String custom = isCustom ? im.label : null;
        // base-64 decode custom IM so since vCard is ASCII
        String data = isCustom ? new String(KsConfig.finalDecode(im.data.getBytes())) : im.data;

        ContentValues val = new ContentValues();
        val.put(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE);
        val.put(Im.PROTOCOL, protocol);
        if (custom != null)
            val.put(Im.CUSTOM_PROTOCOL, custom);
        if (label != null)
            val.put(Im.LABEL, label);
        val.put(Im.IS_PRIMARY, im.isPrimary ? 1 : 0);
        if (data != null)
            val.put(Im.DATA, data);
        val.put(Im.TYPE, im.type);
        return val;
    }

    private ContentValues valuesOrg(OrganizationData org) {
        ContentValues val = new ContentValues();
        val.put(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
        if (org.positionName != null)
            val.put(Organization.TITLE, org.positionName);
        if (org.companyName != null)
            val.put(Organization.COMPANY, org.companyName);
        val.put(Organization.IS_PRIMARY, org.isPrimary ? 1 : 0);
        val.put(Organization.TYPE, org.type);
        return val;
    }

    private ContentValues valuesPostal(Address postal) {
        ContentValues val = new ContentValues();
        val.put(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
        val.put(StructuredPostal.POBOX, postal.getPoBox());
        val.put(StructuredPostal.NEIGHBORHOOD, postal.getExtended());
        val.put(StructuredPostal.STREET, postal.getStreet());
        val.put(StructuredPostal.CITY, postal.getCity());
        val.put(StructuredPostal.REGION, postal.getState());
        val.put(StructuredPostal.POSTCODE, postal.getPostalCode());
        val.put(StructuredPostal.COUNTRY, postal.getCountry());
        val.put(StructuredPostal.FORMATTED_ADDRESS, postal.toString());
        val.put(StructuredPostal.IS_PRIMARY, postal.isPrimary() ? 1 : 0);
        val.put(StructuredPostal.LABEL, postal.getLabel());
        val.put(StructuredPostal.TYPE, postal.getType());
        return val;
    }

    private ContentValues valuesPhoto(ContactStruct contact) {
        ContentValues val = new ContentValues();
        val.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        if (contact.photoBytes != null)
            val.put(Photo.PHOTO, contact.photoBytes);
        return val;
    }

    @Override
    public String insertNewContact(ContactStruct contact, String accountType, String accountName,
            Activity act) {

        // Note: We use RawContacts because this data must be associated with a
        // particular account.
        // The system will aggregate this with any other data for this contact
        // and create a corresponding entry in the ContactsContract.Contacts
        // provider for us.

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(insertPerson(accountType, accountName));

        if (contact.name != null)
            ops.add(insertName(contact));
        if (contact.phoneList != null)
            for (PhoneData phone : contact.phoneList) {
                ops.add(insertPhone(phone));
            }
        if (contact.addressList != null)
            for (Address postal : contact.addressList) {
                ops.add(insertPostal(postal));
            }
        if (contact.organizationList != null)
            for (OrganizationData org : contact.organizationList) {
                ops.add(insertOrg(org));
            }
        if (contact.contactmethodList != null)
            for (ContactMethod cmethod : contact.contactmethodList) {
                switch (cmethod.kind) {
                    case Contacts.KIND_EMAIL:
                        ops.add(insertEmail(cmethod));
                        break;
                    case Contacts.KIND_IM:
                        ops.add(insertIm(cmethod));
                        break;
                    case Contacts.KIND_URL:
                        ops.add(insertUrl(cmethod));
                        break;
                }
            }
        if (contact.photoBytes != null)
            ops.add(insertPhoto(contact));

        // Ask the Contact provider to create a new contact
        try {
            ContentProviderResult[] results = act.getContentResolver().applyBatch(
                    ContactsContract.AUTHORITY, ops);
            if ((results != null) && (results.length > 0)) {
                long rawContactId = ContentUris.parseId(results[0].uri);
                return Long.toString(rawContactId);
            }
        } catch (IllegalArgumentException e) {
            // Unknown authority com.android.contacts, we need a database to
            // save the contact
            showNote(act, e.getLocalizedMessage());
        } catch (SQLiteFullException e) {
            // An exception that indicates that the SQLite database is full.
            // Phone Memory is really low.Can't Insert or update on Contacts DB
            showNote(act, e);
        } catch (RemoteException e) {
            // Parent exception for all Binder remote-invocation errors
            showNote(act, R.string.error_ContactInsertFailed);
        } catch (OperationApplicationException e) {
            // Thrown when an application of a ContentProviderOperation fails
            // due the specified constraints.
            showNote(act, R.string.error_ContactInsertFailed);
        }

        return null;
    }

    private ContentProviderOperation insertPerson(String accountType, String accountName) {
        return ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(RawContacts.ACCOUNT_NAME, accountName).build();
    }

    private ContentProviderOperation insertName(ContactStruct contact) {
        return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValues(valuesName(contact))
                .build();
    }

    private ContentProviderOperation insertPhone(PhoneData phone) {
        return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValues(valuesPhone(phone))
                .build();
    }

    private ContentProviderOperation insertEmail(ContactMethod email) {
        return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValues(valuesEmail(email))
                .build();
    }

    private ContentProviderOperation insertUrl(ContactMethod url) {
        return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValues(valuesUrl(url)).build();
    }

    private ContentProviderOperation insertIm(ContactMethod im) {
        return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValues(valuesIm(im)).build();
    }

    private ContentProviderOperation insertOrg(OrganizationData org) {
        return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValues(valuesOrg(org)).build();
    }

    private ContentProviderOperation insertPostal(Address postal) {
        return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValues(valuesPostal(postal))
                .build();
    }

    private ContentProviderOperation insertPhoto(ContactStruct contact) {
        return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValues(valuesPhoto(contact))
                .build();
    }

    @Override
    public boolean updateOldContact(ContactStruct contact, Activity act, String selectedAcctType,
            String selectedAcctName, String rawContactId) {

        if (TextUtils.isEmpty(rawContactId))
            return false;

        if (contact.phoneList != null)
            for (PhoneData phone : contact.phoneList) {
                if (!updatePhone(phone, rawContactId, act))
                    return false;
            }
        if (contact.addressList != null)
            for (Address postal : contact.addressList) {
                if (!updatePostal(postal, rawContactId, act))
                    return false;
            }
        if (contact.organizationList != null)
            for (OrganizationData org : contact.organizationList) {
                if (!updateOrg(org, rawContactId, act))
                    return false;
            }
        if (contact.contactmethodList != null)
            for (ContactMethod cmethod : contact.contactmethodList) {
                if (cmethod.kind == Contacts.KIND_EMAIL)
                    if (!updateEmail(cmethod, rawContactId, act))
                        return false;
                if (cmethod.kind == Contacts.KIND_IM)
                    if (!updateIm(cmethod, rawContactId, act))
                        return false;
                if (cmethod.kind == Contacts.KIND_URL)
                    if (!updateUrl(cmethod, rawContactId, act))
                        return false;
            }
        if (contact.photoBytes != null)
            if (!updatePhoto(contact, rawContactId, act))
                return false;

        return true;
    }

    public boolean updateDataRow(Context ctx, String[] proj, String where, String[] args,
            ContentValues values) {
        Cursor c = ctx.getContentResolver().query(Data.CONTENT_URI, proj, where, args, null);

        if (c.moveToFirst()) {
            // remove old, including dupes
            if (ctx.getContentResolver().delete(Data.CONTENT_URI, where, args) == 0) {
                c.close();
                return false; // error
            }
        }
        c.close();

        if (ctx.getContentResolver().insert(Data.CONTENT_URI, values) != null)
            return true; // inserted a new entry or updated entry
        return false; // error
    }

    private boolean updatePhoto(ContactStruct contact, String rawContactId, Context ctx) {

        // overwrite existing
        String[] proj = new String[] {
                Photo.RAW_CONTACT_ID, Data.MIMETYPE, Photo.PHOTO
        };
        String where = Photo.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + Photo.PHOTO
                + "!=NULL";
        String[] args = new String[] {
                rawContactId, Photo.CONTENT_ITEM_TYPE
        };
        ContentValues values = valuesPhoto(contact);
        values.put(Photo.RAW_CONTACT_ID, rawContactId);

        return updateDataRow(ctx, proj, where, args, values);
    }

    private boolean updateIm(ContactMethod cmethod, String rawContactId, Context ctx) {

        int protocol = lookupProviderIdFromName(cmethod.label);
        boolean isCustom = isCustomIm(cmethod.label);
        String custom = isCustom ? cmethod.label : null;

        // seek for raw contact + data + label/type = same
        String[] proj = new String[] {
                Im.RAW_CONTACT_ID, Data.MIMETYPE, Im.PROTOCOL, Im.CUSTOM_PROTOCOL
        };

        String where = null;
        String[] args = null;
        if (isCustom) {
            where = Im.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + Im.PROTOCOL
                    + "=? AND " + Im.CUSTOM_PROTOCOL + "=?";
            args = new String[] {
                    rawContactId, Im.CONTENT_ITEM_TYPE, String.valueOf(protocol), custom
            };
        } else {
            where = Im.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + Im.PROTOCOL + "=?";
            args = new String[] {
                    rawContactId, Im.CONTENT_ITEM_TYPE, String.valueOf(protocol)
            };
        }

        ContentValues values = valuesIm(cmethod);
        values.put(Im.RAW_CONTACT_ID, rawContactId);

        return updateDataRow(ctx, proj, where, args, values);
    }

    private boolean updateUrl(ContactMethod cmethod, String rawContactId, Context ctx) {

        // seek for raw contact + url = same
        String[] proj = new String[] {
                Website.RAW_CONTACT_ID, Data.MIMETYPE, Website.DATA
        };
        String where = Website.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND "
                + Website.DATA + "=?";
        String[] args = new String[] {
                rawContactId, Website.CONTENT_ITEM_TYPE, cmethod.data
        };
        ContentValues values = valuesUrl(cmethod);
        values.put(Website.RAW_CONTACT_ID, rawContactId);

        return updateDataRow(ctx, proj, where, args, values);
    }

    private boolean updateEmail(ContactMethod cmethod, String rawContactId, Context ctx) {

        // seek for raw contact + email = same
        String[] proj = new String[] {
                Email.RAW_CONTACT_ID, Data.MIMETYPE, Email.DATA
        };
        String where = Email.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + Email.DATA
                + "=?";
        String[] args = new String[] {
                rawContactId, Email.CONTENT_ITEM_TYPE, cmethod.data
        };
        ContentValues values = valuesEmail(cmethod);
        values.put(Email.RAW_CONTACT_ID, rawContactId);

        return updateDataRow(ctx, proj, where, args, values);
    }

    private boolean updateOrg(OrganizationData org, String rawContactId, Context ctx) {

        // seek for raw contact + company + title = same
        String[] proj = new String[] {
                Organization.RAW_CONTACT_ID, Data.MIMETYPE, Organization.COMPANY,
                Organization.TITLE
        };
        String where = Organization.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND ("
                + Organization.COMPANY + "=? OR " + Organization.TITLE + "=?)";
        String[] args = new String[] {
                rawContactId, Organization.CONTENT_ITEM_TYPE, org.companyName, org.positionName
        };
        ContentValues values = valuesOrg(org);
        values.put(Organization.RAW_CONTACT_ID, rawContactId);

        return updateDataRow(ctx, proj, where, args, values);
    }

    private boolean updatePostal(Address postal, String rawContactId, Context ctx) {

        // seek for raw contact + formatted address = same
        String[] proj = new String[] {
                StructuredPostal.RAW_CONTACT_ID, Data.MIMETYPE, StructuredPostal.FORMATTED_ADDRESS
        };
        String where = StructuredPostal.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND "
                + StructuredPostal.FORMATTED_ADDRESS + "=?";
        String[] args = new String[] {
                rawContactId, StructuredPostal.CONTENT_ITEM_TYPE, postal.toString()
        };
        ContentValues values = valuesPostal(postal);
        values.put(StructuredPostal.RAW_CONTACT_ID, rawContactId);

        return updateDataRow(ctx, proj, where, args, values);
    }

    private boolean updatePhone(PhoneData phone, String rawContactId, Context ctx) {

        // seek for raw contact + number = same
        String[] proj = new String[] {
                Phone.RAW_CONTACT_ID, Data.MIMETYPE, Phone.NUMBER
        };
        String where = Phone.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + Phone.NUMBER
                + "=?";
        String[] args = new String[] {
                rawContactId, Phone.CONTENT_ITEM_TYPE, phone.data
        };
        ContentValues values = valuesPhone(phone);
        values.put(Phone.RAW_CONTACT_ID, rawContactId);

        return updateDataRow(ctx, proj, where, args, values);
    }

    @Override
    public boolean isCustomIm(String label) {
        int protocol = lookupProviderIdFromName(label);
        return (protocol == Im.PROTOCOL_CUSTOM);
    }

    protected void showNote(Activity act, int resId) {
        showNote(act, act.getString(resId));
    }

    protected void showNote(Activity act, Exception e) {
        String msg = e.getLocalizedMessage();
        if (TextUtils.isEmpty(msg)) {
            showNote(act, e.getClass().getSimpleName());
        } else {
            showNote(act, msg);
        }
    }

    protected void showNote(Activity act, String msg) {
        MyLog.i(TAG, msg);
        if (msg != null) {
            Toast toast = Toast.makeText(act, msg, Toast.LENGTH_LONG);
            toast.show();
        }
    }
}
