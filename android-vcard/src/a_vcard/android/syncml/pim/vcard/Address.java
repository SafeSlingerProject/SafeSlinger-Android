
package a_vcard.android.syncml.pim.vcard;

import java.util.Locale;

import android.text.TextUtils;

public class Address {
    private String mPoBox;
    private String mExtended;
    private String mStreet;
    private String mCity;
    private String mState;
    private String mPostal;
    private String mCountry;
    private String mAsString = "";
    private int mType;
    private String mLabel;
    private boolean mIsPrimary;

    public String getPoBox() {
        return mPoBox;
    }

    public void setPoBox(String poBox) {
        mPoBox = poBox;
    }

    public void setExtended(String extended) {
        mExtended = extended;
    }

    public String getExtended() {
        return mExtended;
    }

    public String getStreet() {
        return mStreet;
    }

    public void setStreet(String street) {
        mStreet = street;
    }

    public String getCity() {
        return mCity;
    }

    public void setCity(String city) {
        mCity = city;
    }

    public String getState() {
        return mState;
    }

    public void setState(String state) {
        mState = state;
    }

    public String getPostalCode() {
        return mPostal;
    }

    public void setPostalCode(String postalCode) {
        mPostal = postalCode;
    }

    public String getCountry() {
        return mCountry;
    }

    public void setCountry(String country) {
        mCountry = country;
    }

    public void setType(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setPrimary(boolean isPrimary) {
        mIsPrimary = isPrimary;
    }

    public boolean isPrimary() {
        return mIsPrimary;
    }

    @Override
    public String toString() {
        if (!TextUtils.isEmpty(mAsString)) {
            return mAsString;
        } else {
            StringBuilder builder = new StringBuilder();
            boolean builderIsEmpty = true;
            if (Locale.getDefault().getCountry().equals(Locale.JAPAN.getCountry())) {
                // In Japan, the order is reversed.
                builderIsEmpty = appendAddressPart(builder, mCountry, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mPostal, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mState, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mCity, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mStreet, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mExtended, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mPoBox, builderIsEmpty);
            } else {
                builderIsEmpty = appendAddressPart(builder, mPoBox, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mExtended, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mStreet, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mCity, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mState, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mPostal, builderIsEmpty);
                builderIsEmpty = appendAddressPart(builder, mCountry, builderIsEmpty);
            }
            return builder.toString().trim();
        }
    }

    public boolean appendAddressPart(StringBuilder builder, String part, boolean builderIsEmpty) {
        if (!TextUtils.isEmpty(part)) {
            if (!builderIsEmpty) {
                builder.append(' ');
            }
            builder.append(part);
            builderIsEmpty = false;
        }
        return builderIsEmpty;
    }

    public Address(int type, String formatted, String label, boolean isPrimary) {
        mAsString = formatted;
        mType = type;
        mLabel = label;
        mIsPrimary = isPrimary;
    }

    public Address(int type, String formatted, String poBox, String extended, String street,
            String city, String state, String postal, String country, String label,
            boolean isPrimary) {
        mAsString = formatted;
        mPoBox = poBox;
        mExtended = extended;
        mStreet = street;
        mCity = city;
        mState = state;
        mPostal = postal;
        mCountry = country;
        mType = type;
        mLabel = label;
        mIsPrimary = isPrimary;
    }

}
