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

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.util.SSUtil;

/***
 * This is adapter is meant to store the public key and push token data for each
 * exchanged user. This data is meant to be the primary source for looking up
 * exchanged public key data for messaging. When the recipients are accepted
 * after an exchange one row is created or updated for each new key id here.
 * This data can also be used to push keys and tokens back to the synchronized
 * contacts account if the sync provider removes it.
 */
public class RecipientDbAdapter {
    private static RecipientDbAdapter sInstance = null;
    private static int sUserNumber = 0;

    public static final String DATABASE_TABLE = "recipient";

    // Database fields
    public static final String KEY_ROWID = "_id";

    public static final String KEY_MYKEYID = "mykeyid_str"; // my key id
    public static final String KEY_EXCHDATE = "exchdate"; // exchanged date
    public static final String KEY_CONTACTLKUP = "contactlu"; // contact lookup
    public static final String KEY_NAME = "name"; // name
    public static final String KEY_PHOTO = "photo"; // photo
    public static final String KEY_KEYID = "keyid_str"; // key id
    public static final String KEY_KEYDATE = "keydate"; // key date
    public static final String KEY_KEYUSERID = "keyuserid"; // key user id
    public static final String KEY_PUSHTOKEN = "pushtoken"; // push token
    public static final String KEY_NOTIFY = "notify"; // notify type
    public static final String KEY_PUBKEY = "pubkey"; // public key
    public static final String KEY_SOURCE = "source"; // from exchange?
    public static final String KEY_APPVERSION = "appver"; // which version
    public static final String KEY_HISTDATE = "histdate"; // id link to history
    public static final String KEY_ACTIVE = "active"; // is in active use
    public static final String KEY_INTROKEYID = "introkeyid_str";// sec. intro
    public static final String KEY_NOTREGDATE = "notregdate"; // bad-reg date
    public static final String KEY_MYNOTIFY = "mynotify"; // my notify type
    public static final String KEY_MYPUSHTOKEN = "mypushtoken"; // my push token

    @Deprecated
    public static final String KEY_CONTACTID = "contactid";
    @Deprecated
    public static final String KEY_RAWCONTACTID = "rawid";
    @Deprecated
    public static final String KEY_MYKEYIDLONG = "mykeyid";
    @Deprecated
    public static final String KEY_KEYIDLONG = "keyid";

    public static final int RECIP_PUSH_NONE = 0;
    public static final int RECIP_PUSH_C2DM = 1;
    public static final int RECIP_PUSH_UA_IOS = 2;

    public static final int RECIP_SOURCE_INVITED = 0;
    public static final int RECIP_SOURCE_EXCHANGE = 1;
    @Deprecated
    public static final int RECIP_SOURCE_CONTACTSDB = 2;
    public static final int RECIP_SOURCE_INTRODUCTION = 3;

    public static final int RECIP_IS_NOT_ACTIVE = 0;
    public static final int RECIP_IS_ACTIVE = 1;

    private Context mContext;
    private SQLiteDatabase mDatabase;
    private RecipientDatabaseHelper mDbHelper;

    public static RecipientDbAdapter openInstance(Context ctx) {
        if (sInstance == null) {
            // open for currently selected user
            sUserNumber = SafeSlingerPrefs.getUser();
            sInstance = new RecipientDbAdapter(ctx.getApplicationContext());
        } else {
            // if user has changed in this instance, close instance and reopen
            if (sUserNumber != SafeSlingerPrefs.getUser()) {
                sUserNumber = SafeSlingerPrefs.getUser();
                closeInstance();
                sInstance = new RecipientDbAdapter(ctx.getApplicationContext());
            }
        }
        return sInstance;
    }

    public static void closeInstance() {
        synchronized (SafeSlinger.sDataLock) {
            if (sInstance == null && sInstance.mDbHelper != null) {
                sInstance.mDbHelper.close();
            }
        }
    }

    private RecipientDbAdapter(Context context) {
        mContext = context;
        synchronized (SafeSlinger.sDataLock) {
            mDbHelper = RecipientDatabaseHelper.getInstance(mContext);
            try {
                mDatabase = mDbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                e.printStackTrace();
                mDbHelper = RecipientDatabaseHelper.getInstance(mContext);
                mDatabase = mDbHelper.getWritableDatabase();
            }
        }
    }

    public int getVersion() {
        synchronized (SafeSlinger.sDataLock) {
            if (mDatabase != null) {
                return mDatabase.getVersion();
            } else {
                return RecipientDatabaseHelper.DATABASE_VERSION;
            }
        }
    }

    private long insert(String table, String nullColumnHack, ContentValues values) {
        long insert = mDatabase.insert(table, nullColumnHack, values);
        if (insert != -1) { // when no errors, queue recipients backup...
            SafeSlinger.queueBackup();
        }
        return insert;
    }

    private int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        int update = mDatabase.update(table, values, whereClause, whereArgs);
        if (update > 0) { // when any rows effected, queue recipients backup...
            SafeSlinger.queueBackup();
        }
        return update;
    }

    private int delete(String table, String whereClause, String[] whereArgs) {
        int delete = mDatabase.delete(table, whereClause, whereArgs);
        if (delete > 0) { // when any rows effected, queue recipients backup...
            SafeSlinger.queueBackup();
        }
        return delete;
    }

    private Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy) {
        Cursor query = mDatabase.query(table, columns, selection, selectionArgs, groupBy, having,
                orderBy);
        return query;
    }

    private Cursor query(boolean distinct, String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        Cursor query = mDatabase.query(distinct, table, columns, selection, selectionArgs, groupBy,
                having, orderBy, limit);
        return query;
    }

    /***
     * Add invitation from add contact invite method.
     */
    public long createInvitedRecipient(long exchdate, String contactlu, String name, byte[] photo,
            long matchingInviteRowId) {
        synchronized (SafeSlinger.sDataLock) {
            if (TextUtils.isEmpty(name)) {
                return -1;
            }

            ContentValues values = new ContentValues();
            values.put(KEY_MYKEYID, "");
            values.put(KEY_EXCHDATE, exchdate);

            // make sure db constraint is satisfied
            values.put(KEY_CONTACTID, "");
            values.put(KEY_RAWCONTACTID, "");
            if (!TextUtils.isEmpty(contactlu)) {
                values.put(KEY_CONTACTLKUP, contactlu);
            } else {
                values.put(KEY_CONTACTLKUP, "");
            }

            if (!TextUtils.isEmpty(name))
                values.put(KEY_NAME, name);
            if (photo != null)
                values.put(KEY_PHOTO, photo);
            values.put(KEY_KEYID, "");
            values.put(KEY_KEYDATE, 0);
            values.put(KEY_PUSHTOKEN, "");
            values.put(KEY_NOTIFY, SafeSlingerConfig.NOTIFY_NOPUSH);
            values.put(KEY_PUBKEY, new byte[0]);
            values.put(KEY_SOURCE, RECIP_SOURCE_INVITED);
            values.put(KEY_APPVERSION, SafeSlingerConfig.getVersionCode());
            values.put(KEY_ACTIVE, RECIP_IS_ACTIVE);

            if (matchingInviteRowId > -1) {
                StringBuilder where = new StringBuilder();
                where.append("(");
                where.append(KEY_ROWID + "="
                        + DatabaseUtils.sqlEscapeString("" + matchingInviteRowId));
                where.append(")");

                Cursor c = query(true, DATABASE_TABLE, new String[] {
                        KEY_ROWID, KEY_SOURCE
                }, where.toString(), null, null, null, null, null);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            long rowId = c.getLong(c.getColumnIndexOrThrow(KEY_ROWID));

                            int source = c.getInt(c.getColumnIndexOrThrow(KEY_SOURCE));
                            if (source == RECIP_SOURCE_EXCHANGE
                                    || source == RECIP_SOURCE_INTRODUCTION
                                    || source == RECIP_SOURCE_CONTACTSDB) {
                                // invite is lower priority than exchange,
                                // introduction, and address book
                                return rowId;
                            }

                            if (update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0) {
                                return rowId;
                            }
                            return -1;
                        }
                    } finally {
                        c.close();
                    }
                }
            }

            // backward compatibility for upgraded databases....
            values.put(KEY_MYKEYIDLONG, 0);
            values.put(KEY_KEYIDLONG, 0);

            return insert(DATABASE_TABLE, null, values);
        }
    }

    /***
     * Import or update keys from secure exchange
     */
    public long createExchangedRecipient(String mykeyid, long exchdate, String contactlu,
            String name, byte[] photo, String keyid, long keydate, String keyuserid,
            String pushtoken, int notify, byte[] pubkey, String mypushtoken, int mynotify,
            long matchingInviteRowId) {
        synchronized (SafeSlinger.sDataLock) {
            if (TextUtils.isEmpty(name) || keydate == 0 || notify == -1 || pubkey == null
                    || TextUtils.isEmpty(mykeyid) || TextUtils.isEmpty(keyid)
                    || TextUtils.isEmpty(pushtoken) || mynotify == -1
                    || TextUtils.isEmpty(mypushtoken)) {
                return -1;
            }
            ContentValues values = new ContentValues();
            values.put(KEY_MYKEYID, mykeyid);
            if (!TextUtils.isEmpty(mypushtoken)) {
                values.put(KEY_MYPUSHTOKEN, mypushtoken);
            }
            values.put(KEY_MYNOTIFY, mynotify);
            values.put(KEY_EXCHDATE, exchdate);

            // make sure db constraint is satisfied
            values.put(KEY_CONTACTID, "");
            values.put(KEY_RAWCONTACTID, "");
            if (!TextUtils.isEmpty(contactlu)) {
                values.put(KEY_CONTACTLKUP, contactlu);
            } else {
                values.put(KEY_CONTACTLKUP, "");
            }

            if (!TextUtils.isEmpty(name))
                values.put(KEY_NAME, name);
            if (photo != null)
                values.put(KEY_PHOTO, photo);
            values.put(KEY_KEYID, keyid);
            values.put(KEY_KEYDATE, keydate);
            if (!TextUtils.isEmpty(keyuserid))
                values.put(KEY_KEYUSERID, keyuserid);
            if (!TextUtils.isEmpty(pushtoken)) {
                values.put(KEY_PUSHTOKEN, pushtoken);
            }
            values.put(KEY_NOTIFY, notify);
            if (pubkey != null)
                values.put(KEY_PUBKEY, pubkey);
            values.put(KEY_SOURCE, RECIP_SOURCE_EXCHANGE);
            values.put(KEY_APPVERSION, SafeSlingerConfig.getVersionCode());
            values.put(KEY_ACTIVE, RECIP_IS_ACTIVE);

            StringBuilder where = new StringBuilder();
            where.append("(");
            where.append(KEY_PUSHTOKEN + "=" + DatabaseUtils.sqlEscapeString("" + pushtoken));
            where.append(" AND ");
            where.append(KEY_KEYID + "=" + DatabaseUtils.sqlEscapeString("" + keyid));
            where.append(")");
            if (matchingInviteRowId > -1) {
                where.append(" OR ");
                where.append("(");
                where.append(KEY_ROWID + "="
                        + DatabaseUtils.sqlEscapeString("" + matchingInviteRowId));
                where.append(" AND ");
                where.append(KEY_SOURCE + "=" + RECIP_SOURCE_INVITED);
                where.append(")");
            }

            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    KEY_KEYID, KEY_ROWID, KEY_PUSHTOKEN
            }, where.toString(), null, null, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        long rowId = c.getLong(c.getColumnIndexOrThrow(KEY_ROWID));
                        if (update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0) {
                            return rowId;
                        }
                        return -1;
                    }
                } finally {
                    c.close();
                }
            }

            // backward compatibility for upgraded databases....
            values.put(KEY_MYKEYIDLONG, 0);
            values.put(KEY_KEYIDLONG, 0);

            return insert(DATABASE_TABLE, null, values);
        }
    }

    /***
     * Import or update keys from secure exchange
     */
    public long createIntroduceRecipient(String mykeyid, long exchdate, String contactlu,
            String name, byte[] photo, String keyid, long keydate, String keyuserid,
            String pushtoken, int notify, byte[] pubkey, String introkeyid, String mypushtoken,
            int mynotify, long matchingInviteRowId) {
        synchronized (SafeSlinger.sDataLock) {
            if (TextUtils.isEmpty(name) || keydate == 0 || notify == -1 || pubkey == null
                    || TextUtils.isEmpty(mykeyid) || TextUtils.isEmpty(keyid)
                    || TextUtils.isEmpty(pushtoken) || TextUtils.isEmpty(introkeyid)
                    || mynotify == -1 || TextUtils.isEmpty(mypushtoken)) {
                return -1;
            }
            ContentValues values = new ContentValues();
            values.put(KEY_MYKEYID, mykeyid);
            if (!TextUtils.isEmpty(mypushtoken)) {
                values.put(KEY_MYPUSHTOKEN, mypushtoken);
            }
            values.put(KEY_MYNOTIFY, mynotify);
            values.put(KEY_EXCHDATE, exchdate);

            // make sure db constraint is satisfied
            values.put(KEY_CONTACTID, "");
            values.put(KEY_RAWCONTACTID, "");
            if (!TextUtils.isEmpty(contactlu)) {
                values.put(KEY_CONTACTLKUP, contactlu);
            } else {
                values.put(KEY_CONTACTLKUP, "");
            }

            if (!TextUtils.isEmpty(name))
                values.put(KEY_NAME, name);
            if (photo != null)
                values.put(KEY_PHOTO, photo);
            values.put(KEY_KEYID, keyid);
            values.put(KEY_KEYDATE, keydate);
            if (!TextUtils.isEmpty(keyuserid))
                values.put(KEY_KEYUSERID, keyuserid);
            if (!TextUtils.isEmpty(pushtoken)) {
                values.put(KEY_PUSHTOKEN, pushtoken);
            }
            values.put(KEY_NOTIFY, notify);
            if (pubkey != null)
                values.put(KEY_PUBKEY, pubkey);
            values.put(KEY_SOURCE, RECIP_SOURCE_INTRODUCTION);
            values.put(KEY_APPVERSION, SafeSlingerConfig.getVersionCode());
            values.put(KEY_ACTIVE, RECIP_IS_ACTIVE);
            values.put(KEY_INTROKEYID, introkeyid);

            StringBuilder where = new StringBuilder();
            where.append("(");
            where.append(KEY_PUSHTOKEN + "=" + DatabaseUtils.sqlEscapeString("" + pushtoken));
            where.append(" AND ");
            where.append(KEY_KEYID + "=" + DatabaseUtils.sqlEscapeString("" + keyid));
            where.append(")");
            if (matchingInviteRowId > -1) {
                where.append(" OR ");
                where.append("(");
                where.append(KEY_ROWID + "="
                        + DatabaseUtils.sqlEscapeString("" + matchingInviteRowId));
                where.append(" AND ");
                where.append(KEY_SOURCE + "=" + RECIP_SOURCE_INVITED);
                where.append(")");
            }

            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    KEY_KEYID, KEY_ROWID, KEY_SOURCE, KEY_PUSHTOKEN
            }, where.toString(), null, null, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        long rowId = c.getLong(c.getColumnIndexOrThrow(KEY_ROWID));

                        int source = c.getInt(c.getColumnIndexOrThrow(KEY_SOURCE));
                        if (source == RECIP_SOURCE_EXCHANGE) {
                            // introduction is lower priority than exchange
                            return rowId;
                        }

                        if (update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0) {
                            return rowId;
                        }
                        return -1;
                    }
                } finally {
                    c.close();
                }
            }

            // backward compatibility for upgraded databases....
            values.put(KEY_MYKEYIDLONG, 0);
            values.put(KEY_KEYIDLONG, 0);

            return insert(DATABASE_TABLE, null, values);
        }
    }

    public boolean updateRecipientKeyIds2String(long rowId, long mykeyid_long, long keyid_long) {
        synchronized (SafeSlinger.sDataLock) {
            if (mykeyid_long == 0 || keyid_long == 0) {
                return false;
            }

            ContentValues values = new ContentValues();
            values.put(KEY_MYKEYID, SSUtil.longKeyId2Base64String(mykeyid_long));
            values.put(KEY_KEYID, SSUtil.longKeyId2Base64String(keyid_long));

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    /**
     * Deletes recipient
     */
    public boolean deleteRecipient(long rowId) {
        synchronized (SafeSlinger.sDataLock) {
            return delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public int getAllNewerRecipients(RecipientRow r, boolean sameNotification) {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            // where active and (name=Rname or keyid=Rkeyid or token=Rtoken) and
            // (exchdat>excahdat or keydate>keydate)

            where.append(KEY_ACTIVE + "=" + RECIP_IS_ACTIVE);
            where.append(" AND (");

            where.append(KEY_NAME + "=" + DatabaseUtils.sqlEscapeString("" + r.getName()));
            where.append(" OR ");
            where.append(KEY_PUSHTOKEN + "=" + DatabaseUtils.sqlEscapeString("" + r.getPushtoken()));
            where.append(" OR ");
            where.append(KEY_KEYID + "=" + DatabaseUtils.sqlEscapeString("" + r.getKeyid()));
            where.append(") AND (");

            if (sameNotification) {
                where.append(KEY_NOTIFY + "=" + r.getNotify());
                where.append(") AND (");
            }

            where.append(KEY_EXCHDATE + ">" + r.getExchdate());
            where.append(")");

            Cursor c = query(DATABASE_TABLE, null, where.toString(), null, null, null, null);
            if (c != null) {
                try {
                    int count = c.getCount();
                    return count;
                } finally {
                    c.close();
                }
            }
            return -1;
        }
    }

    /*
     * TODO: updateValues PickRecipientsActivity
     */
    public Cursor fetchAllRecipientsMessage(String myKeyId, String myPushToken, String contactName) {
        synchronized (SafeSlinger.sDataLock) {
            if (TextUtils.isEmpty(contactName)) {
                return null; // valid name required
            }

            StringBuilder where = new StringBuilder();
            filterRecipientSelf(myKeyId, myPushToken, where);

            Cursor c = query(DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_MYKEYIDLONG, KEY_EXCHDATE, KEY_CONTACTID, KEY_CONTACTLKUP,
                    KEY_RAWCONTACTID, KEY_NAME, KEY_PHOTO, KEY_KEYIDLONG, KEY_KEYDATE,
                    KEY_KEYUSERID, KEY_PUSHTOKEN, KEY_NOTIFY, KEY_PUBKEY, KEY_SOURCE,
                    KEY_APPVERSION, KEY_HISTDATE, KEY_ACTIVE, KEY_MYKEYID, KEY_KEYID,
                    KEY_INTROKEYID, KEY_NOTREGDATE, KEY_MYNOTIFY, KEY_MYPUSHTOKEN
            }, where.toString(), null, null, null, null);
            return c;
        }
    }

    /*
     * TODO: updateValues PickRecipientsActivity
     */
    /**
     * @param myKeyId
     * @param myPushToken
     * @param contactName
     * @return
     */
    public Cursor fetchAllRecipientsIntro(String myKeyId, String myPushToken, String contactName) {
        synchronized (SafeSlinger.sDataLock) {
            if (TextUtils.isEmpty(contactName)) {
                return null; // valid name required
            }

            StringBuilder where = new StringBuilder();
            filterRecipientSelf(myKeyId, myPushToken, where);
            where.append(" AND ").append(KEY_SOURCE + "!=" + RECIP_SOURCE_INTRODUCTION);
            where.append(" AND ").append(KEY_SOURCE + "!=" + RECIP_SOURCE_INVITED);

            Cursor c = query(DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_MYKEYIDLONG, KEY_EXCHDATE, KEY_CONTACTID, KEY_CONTACTLKUP,
                    KEY_RAWCONTACTID, KEY_NAME, KEY_PHOTO, KEY_KEYIDLONG, KEY_KEYDATE,
                    KEY_KEYUSERID, KEY_PUSHTOKEN, KEY_NOTIFY, KEY_PUBKEY, KEY_SOURCE,
                    KEY_APPVERSION, KEY_HISTDATE, KEY_ACTIVE, KEY_MYKEYID, KEY_KEYID,
                    KEY_INTROKEYID, KEY_NOTREGDATE, KEY_MYNOTIFY, KEY_MYPUSHTOKEN
            }, where.toString(), null, null, null, null);
            return c;
        }
    }

    public Cursor fetchAllRecipientsInvited(boolean hideInactive, String myKeyId,
            String myPushToken, String contactName) {
        synchronized (SafeSlinger.sDataLock) {
            if (TextUtils.isEmpty(contactName)) {
                return null; // valid name required
            }

            StringBuilder where = new StringBuilder();
            filterRecipientSelf(myKeyId, myPushToken, where);
            if (hideInactive) {
                where.append(" AND ").append(KEY_ACTIVE + "=" + RECIP_IS_ACTIVE);
            }
            where.append(" AND ").append(KEY_SOURCE + "=" + RECIP_SOURCE_INVITED);

            Cursor c = query(DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_MYKEYIDLONG, KEY_EXCHDATE, KEY_CONTACTID, KEY_CONTACTLKUP,
                    KEY_RAWCONTACTID, KEY_NAME, KEY_PHOTO, KEY_KEYIDLONG, KEY_KEYDATE,
                    KEY_KEYUSERID, KEY_PUSHTOKEN, KEY_NOTIFY, KEY_PUBKEY, KEY_SOURCE,
                    KEY_APPVERSION, KEY_HISTDATE, KEY_ACTIVE, KEY_MYKEYID, KEY_KEYID,
                    KEY_INTROKEYID, KEY_NOTREGDATE, KEY_MYNOTIFY, KEY_MYPUSHTOKEN
            }, where.toString(), null, null, null, null);
            return c;
        }
    }

    private void filterRecipientSelf(String myKeyId, String myPushToken, StringBuilder filter) {
        filter.append(KEY_KEYID + "!=" + DatabaseUtils.sqlEscapeString("" + myKeyId));

        filter.append(" AND ");
        filter.append(KEY_PUSHTOKEN + "!=" + DatabaseUtils.sqlEscapeString("" + myPushToken));
    }

    public Cursor fetchAllPublicKeys() {
        synchronized (SafeSlinger.sDataLock) {
            Cursor c = query(DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_MYKEYIDLONG, KEY_EXCHDATE, KEY_CONTACTID, KEY_CONTACTLKUP,
                    KEY_RAWCONTACTID, KEY_NAME, KEY_PHOTO, KEY_KEYIDLONG, KEY_KEYDATE,
                    KEY_KEYUSERID, KEY_PUSHTOKEN, KEY_NOTIFY, KEY_PUBKEY, KEY_SOURCE,
                    KEY_APPVERSION, KEY_HISTDATE, KEY_ACTIVE, KEY_MYKEYID, KEY_KEYID,
                    KEY_INTROKEYID, KEY_NOTREGDATE, KEY_MYNOTIFY, KEY_MYPUSHTOKEN
            }, null, null, null, null, null);
            return c;
        }
    }

    public Cursor fetchAllRecipientsUpgradeTo6() {
        synchronized (SafeSlinger.sDataLock) {
            Cursor c = query(DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_MYKEYIDLONG, KEY_KEYIDLONG
            }, null, null, null, null, null);
            return c;
        }
    }

    /**
     * Return a Cursor positioned at the defined recipient
     */
    public Cursor fetchRecipient(long rowId) throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            String where = KEY_ROWID + "=" + rowId;
            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_MYKEYIDLONG, KEY_EXCHDATE, KEY_CONTACTID, KEY_CONTACTLKUP,
                    KEY_RAWCONTACTID, KEY_NAME, KEY_PHOTO, KEY_KEYIDLONG, KEY_KEYDATE,
                    KEY_KEYUSERID, KEY_PUSHTOKEN, KEY_NOTIFY, KEY_PUBKEY, KEY_SOURCE,
                    KEY_APPVERSION, KEY_HISTDATE, KEY_ACTIVE, KEY_MYKEYID, KEY_KEYID,
                    KEY_INTROKEYID, KEY_NOTREGDATE, KEY_MYNOTIFY, KEY_MYPUSHTOKEN
            }, where, null, null, null, null, null);
            return c;
        }
    }

    public Cursor fetchRecipientByKeyId(String keyId) {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            where.append(KEY_KEYID + "=" + DatabaseUtils.sqlEscapeString("" + keyId));
            String orderBy = KEY_EXCHDATE + " DESC";
            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_MYKEYIDLONG, KEY_EXCHDATE, KEY_CONTACTID, KEY_CONTACTLKUP,
                    KEY_RAWCONTACTID, KEY_NAME, KEY_PHOTO, KEY_KEYIDLONG, KEY_KEYDATE,
                    KEY_KEYUSERID, KEY_PUSHTOKEN, KEY_NOTIFY, KEY_PUBKEY, KEY_SOURCE,
                    KEY_APPVERSION, KEY_HISTDATE, KEY_ACTIVE, KEY_MYKEYID, KEY_KEYID,
                    KEY_INTROKEYID, KEY_NOTREGDATE, KEY_MYNOTIFY, KEY_MYPUSHTOKEN
            }, where.toString(), null, null, null, orderBy, null);
            return c;
        }
    }

    public boolean updateRecipientActiveState(RecipientRow r, int recipIsActive) {
        synchronized (SafeSlinger.sDataLock) {
            if (r.isActive() != (recipIsActive == RECIP_IS_ACTIVE)) {
                ContentValues values = new ContentValues();
                values.put(KEY_ACTIVE, recipIsActive);

                // update change
                return update(DATABASE_TABLE, values, KEY_ROWID + "=" + r.getRowId(), null) > 0;
            } else {
                // avoid writing to DB when no change is needed
                return true;
            }
        }
    }

    public boolean updateRecipientRegistrationState(long rowId, boolean notreg) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();
            if (notreg) {
                values.put(KEY_NOTREGDATE, new Date().getTime());
            } else {
                values.put(KEY_NOTREGDATE, 0);
            }

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public boolean updateRecipientName(long rowId, String name) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();
            if (!TextUtils.isEmpty(name))
                values.put(KEY_NAME, name);

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public boolean updateRecipientPhoto(long rowId, byte[] photo) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();
            values.put(KEY_PHOTO, photo);

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public boolean updateRecipientFromChosenLink(long rowId, String contactlu) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();

            // make sure db constraint is satisfied
            values.put(KEY_CONTACTID, "");
            values.put(KEY_RAWCONTACTID, "");
            if (!TextUtils.isEmpty(contactlu)) {
                values.put(KEY_CONTACTLKUP, contactlu);
            } else {
                values.put(KEY_CONTACTLKUP, "");
            }

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public int getTrustedRecipientCount() throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            where.append(KEY_SOURCE + "!=" + RECIP_SOURCE_INVITED);
            Cursor c = query(DATABASE_TABLE, null, where.toString(), null, null, null, null);
            if (c != null) {
                try {
                    int count = c.getCount();
                    return count;
                } finally {
                    c.close();
                }
            }
            return -1;
        }
    }

}
