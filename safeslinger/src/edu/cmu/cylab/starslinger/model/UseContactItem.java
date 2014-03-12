
package edu.cmu.cylab.starslinger.model;

public class UseContactItem {
    public final String text;
    public final byte[] icon;
    public final boolean contact;
    public final String contactLookupKey;
    public final UCType type;

    public static enum UCType {
        NONE, PROFILE, CONTACT, ANOTHER, NEW, EDIT_CONTACT, EDIT_NAME
    }

    public UseContactItem(String text, byte[] icon, String lookupKey, UCType t) {
        this.text = text;
        this.icon = icon;
        this.contact = true;
        this.contactLookupKey = lookupKey;
        this.type = t;
    }

    public UseContactItem(String text, UCType t) {
        this.text = text;
        this.icon = null;
        this.contact = false;
        this.contactLookupKey = null;
        this.type = t;
    }

    @Override
    public String toString() {
        return text;
    }
}
