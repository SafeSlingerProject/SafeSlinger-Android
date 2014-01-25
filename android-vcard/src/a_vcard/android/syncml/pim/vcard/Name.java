
package a_vcard.android.syncml.pim.vcard;

import android.text.TextUtils;

public class Name {

    public static final int NAME_ORDER_TYPE_ENGLISH = 0;
    public static final int NAME_ORDER_TYPE_JAPANESE = 1;

    private String mFamily;
    private String mGiven;
    private String mMiddle;
    private String mPrefix;
    private String mSuffix;
    private String mAsString = "";
    private int mNameOrderType;

    public String getFamily() {
        return mFamily;
    }

    public void setFamily(String family) {
        mFamily = family;
    }

    public String getGiven() {
        return mGiven;
    }

    public void setGiven(String given) {
        mGiven = given;
    }

    public String getMiddle() {
        return mMiddle;
    }

    public void setMiddle(String middle) {
        mMiddle = middle;
    }

    public String getPrefix() {
        return mPrefix;
    }

    public void setPrefix(String prefix) {
        mPrefix = prefix;
    }

    public String getSuffix() {
        return mSuffix;
    }

    public void setSuffix(String suffix) {
        mSuffix = suffix;
    }

    @Override
    public String toString() {
        if (!TextUtils.isEmpty(mAsString)) {
            return (mAsString);
        } else {
            StringBuilder builder = new StringBuilder();
            boolean builderIsEmpty = true;
            // Prefix
            builderIsEmpty = appendNamePart(builder, mPrefix, builderIsEmpty);
            String first, second;
            if (mNameOrderType == Name.NAME_ORDER_TYPE_JAPANESE) {
                first = mFamily;
                second = mGiven;
            } else {
                first = mGiven;
                second = mFamily;
            }
            builderIsEmpty = appendNamePart(builder, first, builderIsEmpty);
            // Middle name
            builderIsEmpty = appendNamePart(builder, mMiddle, builderIsEmpty);
            builderIsEmpty = appendNamePart(builder, second, builderIsEmpty);
            // Suffix
            builderIsEmpty = appendNamePart(builder, mSuffix, builderIsEmpty);
            return builder.toString().trim();
        }
    }

    public boolean appendNamePart(StringBuilder builder, String part, boolean builderIsEmpty) {
        if (!TextUtils.isEmpty(part)) {
            if (!builderIsEmpty) {
                builder.append(' ');
            }
            builder.append(part);
            builderIsEmpty = false;
        }
        return builderIsEmpty;
    }

    public Name(String formatted) {
        mAsString = formatted;
    }

    public Name(String family, String given, String middle, String prefix, String suffix,
            int nameOrderType) {
        mFamily = family;
        mGiven = given;
        mMiddle = middle;
        mPrefix = prefix;
        mSuffix = suffix;
        mNameOrderType = nameOrderType;
    }

    public void setNameOrderType(int nameOrderType) {
        mNameOrderType = nameOrderType;
    }

    public int getNameOrderType() {
        return mNameOrderType;
    }

}
