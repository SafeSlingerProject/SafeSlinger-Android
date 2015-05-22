/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2010-2015 Carnegie Mellon University
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

package edu.cmu.cylab.starslinger.model;

import java.util.Locale;

import a_vcard.android.provider.Contacts;
import a_vcard.android.syncml.pim.vcard.Address;
import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.ContactStruct.ContactMethod;
import a_vcard.android.syncml.pim.vcard.ContactStruct.OrganizationData;
import a_vcard.android.syncml.pim.vcard.ContactStruct.PhoneData;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

public abstract class ContactAccessor {

    private static ContactAccessor sInstance;

    public static ContactAccessor getInstance() {
        if (sInstance == null) {
            String className;
            int sdkVersion = Build.VERSION.SDK_INT;
            if (sdkVersion < 5) {
                className = "ContactAccessorApi1";
            } else {
                className = "ContactAccessorApi5";
            }
            try {
                String fullClass = ContactAccessor.class.getPackage().getName() + "." + className;
                Class<? extends ContactAccessor> clazz;
                clazz = Class.forName(fullClass).asSubclass(ContactAccessor.class);
                sInstance = clazz.newInstance();
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (InstantiationException e) {
                throw new IllegalStateException(e);
            }
        }
        return sInstance;
    }

    protected static boolean isPhoneNew(Context ctx, ContactStruct contact, String number,
            int type, boolean removeMatches) {
        if (contact.phoneList != null)
            for (PhoneData m : contact.phoneList) {
                if (PhoneNumberUtils.compare(ctx, number, m.data)) {
                    if (removeMatches) {
                        contact.phoneList.remove(m);
                    }
                    return false;
                }
            }
        return true;
    }

    protected static boolean isUrlNew(ContactStruct contact, String url, int type,
            boolean removeMatches) {
        if (contact.contactmethodList != null)
            for (ContactMethod m : contact.contactmethodList) {
                if (m.kind == Contacts.KIND_URL && url.equalsIgnoreCase(m.data)) {
                    if (removeMatches) {
                        contact.contactmethodList.remove(m);
                    }
                    return false;
                }
            }
        return true;
    }

    protected static boolean isPhotoNew(ContactStruct contact, byte[] photo, boolean primary) {
        if (contact.photoBytes != null)
            if (!primary) {
                return false;
            }
        return true;
    }

    protected static boolean isOrgNew(ContactStruct contact, String company, String title,
            boolean removeMatches) {
        if (contact.organizationList != null)
            for (OrganizationData m : contact.organizationList) {
                if (company.equalsIgnoreCase(m.companyName)) {
                    if (removeMatches) {
                        contact.organizationList.remove(m);
                    }
                    return false;
                }
            }
        return true;
    }

    protected static boolean isEmailNew(ContactStruct contact, String email, int type,
            boolean removeMatches) {
        if (contact.contactmethodList != null)
            for (ContactMethod m : contact.contactmethodList) {
                if (m.kind == Contacts.KIND_EMAIL && email.equalsIgnoreCase(m.data)) {
                    if (removeMatches) {
                        contact.contactmethodList.remove(m);
                    }
                    return false;
                }
            }
        return true;
    }

    protected static boolean isImNew(ContactStruct contact, String data, String label,
            boolean removeMatches) {
        if (contact.contactmethodList != null)
            for (ContactMethod m : contact.contactmethodList) {
                if (m.kind == Contacts.KIND_IM && label.contentEquals(m.label)
                        && data.contentEquals(m.data)) {
                    if (removeMatches) {
                        contact.contactmethodList.remove(m);
                    }
                    return false;
                }
            }
        return true;
    }

    protected static boolean isNameNew(ContactStruct contact, String display, boolean primary) {
        if (contact.name != null && !TextUtils.isEmpty(contact.name.toString()))
            if (!primary) {
                return false;
            }
        return true;
    }

    protected static boolean isPostalNew(ContactStruct contact, String address, String poBox,
            String street, String nbrhood, String city, String state, String postCode, int type,
            boolean removeMatches) {
        String a1 = createComparableAddress(poBox, street, nbrhood, city, state, postCode);
        if (contact.addressList != null) {
            for (Address m : contact.addressList) {
                String a2 = createComparableAddress(m.getPoBox(), m.getStreet(), m.getExtended(),
                        m.getCity(), m.getState(), m.getPostalCode());
                int minLen = a1.length() < a2.length() ? a1.length() : a2.length();
                if (a1.substring(0, minLen).contentEquals(a2.substring(0, minLen))) {
                    if (removeMatches) {
                        contact.addressList.remove(m);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private static String createComparableAddress(String poBox, String street, String nbrhood,
            String city, String state, String postCode) {
        StringBuilder addr = new StringBuilder();
        if (poBox != null)
            addr.append(poBox.toUpperCase(Locale.getDefault()).replaceAll("[^0-9A-Z]", ""));
        if (street != null)
            addr.append(street.toUpperCase(Locale.getDefault()).replaceAll("[^0-9A-Z]", ""));
        if (nbrhood != null)
            addr.append(nbrhood.toUpperCase(Locale.getDefault()).replaceAll("[^0-9A-Z]", ""));
        if (city != null)
            addr.append(city.toUpperCase(Locale.getDefault()).replaceAll("[^0-9A-Z]", ""));
        if (state != null)
            addr.append(state.toUpperCase(Locale.getDefault()).replaceAll("[^0-9A-Z]", ""));
        if (postCode != null)
            addr.append(postCode.toUpperCase(Locale.getDefault()).replaceAll("[^0-9A-Z]", ""));
        return addr.toString();
    }

    public abstract boolean addEmail(ContactStruct contact, Cursor emails, boolean removeMatches);

    public abstract boolean addIM(ContactStruct contact, Cursor ims, Context ctx,
            boolean removeMatches);

    public abstract boolean addName(ContactStruct contact, Cursor names);

    public abstract boolean addOrg(ContactStruct contact, Cursor orgs, boolean removeMatches);

    public abstract boolean addPhone(Context ctx, ContactStruct contact, Cursor phones,
            boolean removeMatches);

    public abstract boolean addPhoto(ContactStruct contact, Cursor photos);

    public abstract boolean addPostal(ContactStruct contact, Cursor postals, boolean removeMatches);

    public abstract boolean addUrl(ContactStruct contact, Cursor urls, Context ctx,
            boolean removeMatches);

    public abstract String getDesc(int kind, int type, String label);

    public abstract String[] getProjEmail();

    public abstract String[] getProjIM();

    public abstract String[] getProjName();

    public abstract String[] getProjOrg();

    public abstract String[] getProjPersonLookupKey();

    public abstract String[] getProjPhone();

    public abstract String[] getProjPhoto();

    public abstract String[] getProjPostal();

    public abstract String[] getProjUrl();

    public abstract String getQueryEmail();

    public abstract String getQueryIM();

    public abstract String getQueryName();

    public abstract String getQueryOrg();

    public abstract String getQueryPersonLookupKey(String name);

    public abstract String getQueryPhone();

    public abstract String getQueryPhoto();

    public abstract String getQueryPostal();

    public abstract String getQueryUrl();

    public abstract Uri getUriEmail(ContentResolver resolver, String contactLookupKey);

    public abstract Uri getUriIM(ContentResolver resolver, String contactLookupKey);

    public abstract Uri getUriName(ContentResolver resolver, String contactLookupKey);

    public abstract Uri getUriOrg(ContentResolver resolver, String contactLookupKey);

    public abstract Uri getUriPersonLookupKey();

    public abstract Uri getUriPhone(ContentResolver resolver, String contactLookupKey);

    public abstract Uri getUriPhoto(ContentResolver resolver, String contactLookupKey);

    public abstract Uri getUriPostal(ContentResolver resolver, String contactLookupKey);

    public abstract Uri getUriUrl(ContentResolver resolver, String contactLookupKey);

    public abstract String insertNewContact(ContactStruct contact, String accountType,
            String accountName, Activity act);

    public abstract boolean isCustomIm(String label);

    public abstract int lookupProviderIdFromName(String name);

    public abstract String lookupProviderNameFromId(int protocol);

    public abstract boolean updateOldContact(ContactStruct contact, Activity act,
            String selectedAcctType, String selectedAcctName, String rawContactId);

}
