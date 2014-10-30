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

package edu.cmu.cylab.starslinger.model;

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

/***
 * This is adapter is meant to store the encrypted messages received from other
 * users, and the state of each message download and cryptography. When a push
 * message is first received, one row is created and updated. It is an openly
 * accessible inbox; then after decryption, messages are deleted here and moved
 * to the messages database.
 */
public class InboxDbAdapter {
    private static InboxDbAdapter sInstance = null;

    private static final String TAG = SafeSlingerConfig.LOG_TAG;

    public static final String DATABASE_TABLE = "inbox";

    private Context mContext;
    private SQLiteDatabase mDatabase;
    private InboxDatabaseHelper mDbHelper;

    public static InboxDbAdapter openInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new InboxDbAdapter(ctx.getApplicationContext());
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

    private InboxDbAdapter(Context context) {
        mContext = context;
        synchronized (SafeSlinger.sDataLock) {
            mDbHelper = InboxDatabaseHelper.getInstance(mContext);
            try {
                mDatabase = mDbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                e.printStackTrace();
                mDbHelper = InboxDatabaseHelper.getInstance(mContext);
                mDatabase = mDbHelper.getWritableDatabase();
            }
        }
    }

    public int getVersion() {
        synchronized (SafeSlinger.sDataLock) {
            if (mDatabase != null) {
                return mDatabase.getVersion();
            } else {
                return InboxDatabaseHelper.DATABASE_VERSION;
            }
        }
    }

    private long insert(String table, String nullColumnHack, ContentValues values) {
        long insert = mDatabase.insert(table, nullColumnHack, values);
        // no backup needed for messages...
        return insert;
    }

    private int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        int update = mDatabase.update(table, values, whereClause, whereArgs);
        // no backup needed for messages...
        return update;
    }

    private int delete(String table, String whereClause, String[] whereArgs) {
        int delete = mDatabase.delete(table, whereClause, whereArgs);
        // no backup needed for messages...
        return delete;
    }

    private Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy) {
        Cursor query = mDatabase.query(table, columns, selection, selectionArgs, groupBy, having,
                orderBy);
        // if (query != null) {
        // MyLog.d(TAG, query.getCount() + " " + table + " " + selection + " " +
        // selectionArgs
        // + " " + groupBy + " " + having + " " + orderBy);
        // }
        return query;
    }

    private Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy, String limit) {
        Cursor query = mDatabase.query(table, columns, selection, selectionArgs, groupBy, having,
                orderBy, limit);

        return query;
    }

    private Cursor query(boolean distinct, String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        Cursor query = mDatabase.query(distinct, table, columns, selection, selectionArgs, groupBy,
                having, orderBy, limit);
        // if (query != null) {
        // MyLog.d(TAG, query.getCount() + " " + table + " " + selection + " " +
        // selectionArgs
        // + " " + groupBy + " " + having + " " + orderBy + " " + limit);
        // }
        return query;
    }

    public long createRecvEncInbox(String msgHash, int status, int seen) {
        synchronized (SafeSlinger.sDataLock) {
            // ignore duplicate message identifiers...
            String where = MessageDbAdapter.KEY_MSGHASH + "="
                    + DatabaseUtils.sqlEscapeString("" + msgHash);
            Cursor c = query(true, DATABASE_TABLE, new String[] {
                MessageDbAdapter.KEY_MSGHASH
            }, where, null, null, null, null, null);
            if (c != null) {
                if (c.getCount() > 0) {
                    c.close();
                    return -1;
                }
                c.close();
            }

            ContentValues values = new ContentValues();
            values.put(MessageDbAdapter.KEY_DATE_RECV, System.currentTimeMillis()); // Received
            // UTC
            values.put(MessageDbAdapter.KEY_READ, MessageDbAdapter.MESSAGE_IS_NOT_READ); // decoded
            values.put(MessageDbAdapter.KEY_STATUS, status); // complete/failed
            values.put(MessageDbAdapter.KEY_TYPE, MessageDbAdapter.MESSAGE_TYPE_INBOX); // inbox/sent
            values.put(MessageDbAdapter.KEY_SEEN, seen); // seen in list
            if (msgHash != null)
                values.put(MessageDbAdapter.KEY_MSGHASH, msgHash); // file
                                                                   // retrieval
                                                                   // id

            // backward compatibility for upgraded databases....
            values.put(MessageDbAdapter.KEY_KEYIDLONG, 0);

            return insert(DATABASE_TABLE, null, values);
        }
    }

    public boolean updateInboxExpired(long rowId) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();
            values.put(MessageDbAdapter.KEY_STATUS, MessageDbAdapter.MESSAGE_STATUS_EXPIRED); // complete/failed

            return update(DATABASE_TABLE, values, MessageDbAdapter.KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public boolean updateInboxDownloaded(long rowId, byte[] encbody, int seen, String keyid) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();
            values.put(MessageDbAdapter.KEY_SEEN, seen); // seen in list
            if (encbody != null)
                values.put(MessageDbAdapter.KEY_ENCBODY, encbody); // encoded
                                                                   // body
            values.put(MessageDbAdapter.KEY_STATUS, MessageDbAdapter.MESSAGE_STATUS_COMPLETE_MSG); // complete/failed
            if (!TextUtils.isEmpty(keyid))
                values.put(MessageDbAdapter.KEY_KEYID, keyid); // key id of the
                                                               // sig...

            return update(DATABASE_TABLE, values, MessageDbAdapter.KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    /**
     * Deletes message
     */
    public boolean deleteInbox(long rowId) {
        synchronized (SafeSlinger.sDataLock) {
            return delete(DATABASE_TABLE, MessageDbAdapter.KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public int deleteThread(String keyId) {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(MessageDbAdapter.KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + "="
                        + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }
            return delete(DATABASE_TABLE, where.toString(), null);
        }
    }

    public Cursor fetchInboxSmall(long rowId) throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            String where = MessageDbAdapter.KEY_ROWID + "=" + rowId;
            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    MessageDbAdapter.KEY_ROWID, MessageDbAdapter.KEY_DATE_RECV,
                    MessageDbAdapter.KEY_DATE_SENT, MessageDbAdapter.KEY_ENCBODY,
                    MessageDbAdapter.KEY_FILEDIR, MessageDbAdapter.KEY_MSGHASH_BLOB,
                    MessageDbAdapter.KEY_FILELEN, MessageDbAdapter.KEY_FILENAME,
                    MessageDbAdapter.KEY_FILETYPE, MessageDbAdapter.KEY_KEYIDLONG,
                    MessageDbAdapter.KEY_PERSON, MessageDbAdapter.KEY_READ,
                    MessageDbAdapter.KEY_SEEN, MessageDbAdapter.KEY_STATUS,
                    MessageDbAdapter.KEY_TEXT, MessageDbAdapter.KEY_TYPE,
                    MessageDbAdapter.KEY_KEYID, MessageDbAdapter.KEY_MSGHASH,
                    MessageDbAdapter.KEY_RETNOTIFY, MessageDbAdapter.KEY_RETPUSHTOKEN,
                    MessageDbAdapter.KEY_RETRECEIPT
            }, where, null, null, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    return c;
                }
            }
            return null;
        }
    }

    public int getAllInboxCountByThread(String keyId) {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(MessageDbAdapter.KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + "="
                        + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }

            Cursor c = query(DATABASE_TABLE, null, where.toString(), null, null, null, null);
            if (c != null) {
                int count = c.getCount();
                c.close();
                return count;
            }
            return -1;
        }
    }

    public Cursor fetchAllInboxGetMessagePending() {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            where.append("(");
            where.append(MessageDbAdapter.KEY_TYPE + "=" + MessageDbAdapter.MESSAGE_TYPE_INBOX);
            where.append(" AND ");
            where.append(MessageDbAdapter.KEY_STATUS + "="
                    + MessageDbAdapter.MESSAGE_STATUS_GOTPUSH);
            where.append(")");

            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    MessageDbAdapter.KEY_ROWID, MessageDbAdapter.KEY_DATE_RECV,
                    MessageDbAdapter.KEY_DATE_SENT, MessageDbAdapter.KEY_ENCBODY,
                    MessageDbAdapter.KEY_FILEDIR, MessageDbAdapter.KEY_MSGHASH_BLOB,
                    MessageDbAdapter.KEY_FILELEN, MessageDbAdapter.KEY_FILENAME,
                    MessageDbAdapter.KEY_FILETYPE, MessageDbAdapter.KEY_KEYIDLONG,
                    MessageDbAdapter.KEY_PERSON, MessageDbAdapter.KEY_READ,
                    MessageDbAdapter.KEY_SEEN, MessageDbAdapter.KEY_STATUS,
                    MessageDbAdapter.KEY_TEXT, MessageDbAdapter.KEY_TYPE,
                    MessageDbAdapter.KEY_KEYID, MessageDbAdapter.KEY_MSGHASH,
                    MessageDbAdapter.KEY_RETNOTIFY, MessageDbAdapter.KEY_RETPUSHTOKEN,
                    MessageDbAdapter.KEY_RETRECEIPT
            }, where.toString(), null, null, null, null, null);
            if (c != null) {
                return c;
            }
            return null;
        }
    }

    public Cursor fetchAllInboxDecryptPending() {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            where.append("(");
            where.append(MessageDbAdapter.KEY_TYPE + "=" + MessageDbAdapter.MESSAGE_TYPE_INBOX);
            where.append(" AND ");
            where.append(MessageDbAdapter.KEY_STATUS + "="
                    + MessageDbAdapter.MESSAGE_STATUS_COMPLETE_MSG);
            where.append(" AND ");
            where.append(MessageDbAdapter.KEY_READ + "=" + MessageDbAdapter.MESSAGE_IS_NOT_READ);
            where.append(")");

            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    MessageDbAdapter.KEY_ROWID, MessageDbAdapter.KEY_DATE_RECV,
                    MessageDbAdapter.KEY_DATE_SENT, MessageDbAdapter.KEY_ENCBODY,
                    MessageDbAdapter.KEY_FILEDIR, MessageDbAdapter.KEY_MSGHASH_BLOB,
                    MessageDbAdapter.KEY_FILELEN, MessageDbAdapter.KEY_FILENAME,
                    MessageDbAdapter.KEY_FILETYPE, MessageDbAdapter.KEY_KEYIDLONG,
                    MessageDbAdapter.KEY_PERSON, MessageDbAdapter.KEY_READ,
                    MessageDbAdapter.KEY_SEEN, MessageDbAdapter.KEY_STATUS,
                    MessageDbAdapter.KEY_TEXT, MessageDbAdapter.KEY_TYPE,
                    MessageDbAdapter.KEY_KEYID, MessageDbAdapter.KEY_MSGHASH,
                    MessageDbAdapter.KEY_RETNOTIFY, MessageDbAdapter.KEY_RETPUSHTOKEN,
                    MessageDbAdapter.KEY_RETRECEIPT
            }, where.toString(), null, null, null, null, null);
            if (c != null) {
                return c;
            }
            return null;
        }
    }

    public Cursor fetchAllInboxByThread(String keyId) {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(MessageDbAdapter.KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + "="
                        + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }

            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    MessageDbAdapter.KEY_ROWID, MessageDbAdapter.KEY_DATE_RECV,
                    MessageDbAdapter.KEY_DATE_SENT, MessageDbAdapter.KEY_ENCBODY,
                    MessageDbAdapter.KEY_FILEDIR, MessageDbAdapter.KEY_MSGHASH_BLOB,
                    MessageDbAdapter.KEY_FILELEN, MessageDbAdapter.KEY_FILENAME,
                    MessageDbAdapter.KEY_FILETYPE, MessageDbAdapter.KEY_KEYIDLONG,
                    MessageDbAdapter.KEY_PERSON, MessageDbAdapter.KEY_READ,
                    MessageDbAdapter.KEY_SEEN, MessageDbAdapter.KEY_STATUS,
                    MessageDbAdapter.KEY_TEXT, MessageDbAdapter.KEY_TYPE,
                    MessageDbAdapter.KEY_KEYID, MessageDbAdapter.KEY_MSGHASH,
                    MessageDbAdapter.KEY_RETNOTIFY, MessageDbAdapter.KEY_RETPUSHTOKEN,
                    MessageDbAdapter.KEY_RETRECEIPT
            }, where.toString(), null, null, null, null, null);
            if (c != null) {
                return c;
            }
            return null;
        }
    }

    public long fetchLastRecentMessageTime() {
        synchronized (SafeSlinger.sDataLock) {

            /*
             * SELECT MessageDbAdapter.KEY_ROWID, MessageDbAdapter.KEY_DATE_RECV
             * FROM table ORDER BY MessageDbAdapter.KEY_ROWID DESC LIMIT 1;
             */
            Cursor cursor = query(DATABASE_TABLE, new String[] {
                    MessageDbAdapter.KEY_ROWID, MessageDbAdapter.KEY_DATE_RECV
            }, null, null, null, null, MessageDbAdapter.KEY_ROWID + " DESC", "1");

            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                
                long time  = cursor.getLong(cursor.getColumnIndex(MessageDbAdapter.KEY_DATE_RECV));
                
                return time;
            }
        }
        
        return -1;
    }

    public Cursor fetchInboxRecent(String keyId) {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(MessageDbAdapter.KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + "="
                        + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }

            String groupBy = MessageDbAdapter.KEY_KEYID;
            Cursor c = query(DATABASE_TABLE, new String[] {
                    MessageDbAdapter.KEY_ROWID, MessageDbAdapter.KEY_DATE_RECV,
                    MessageDbAdapter.KEY_DATE_SENT, MessageDbAdapter.KEY_ENCBODY,
                    MessageDbAdapter.KEY_FILEDIR, MessageDbAdapter.KEY_MSGHASH_BLOB,
                    MessageDbAdapter.KEY_FILELEN, MessageDbAdapter.KEY_FILENAME,
                    MessageDbAdapter.KEY_FILETYPE, MessageDbAdapter.KEY_KEYIDLONG,
                    MessageDbAdapter.KEY_PERSON, MessageDbAdapter.KEY_READ,
                    MessageDbAdapter.KEY_SEEN, MessageDbAdapter.KEY_STATUS,
                    MessageDbAdapter.KEY_TEXT, MessageDbAdapter.KEY_TYPE,
                    MessageDbAdapter.KEY_KEYID, MessageDbAdapter.KEY_MSGHASH,
                    MessageDbAdapter.KEY_RETNOTIFY, MessageDbAdapter.KEY_RETPUSHTOKEN,
                    MessageDbAdapter.KEY_RETRECEIPT
            }, where.toString(), null, groupBy, null, null);
            if (c != null) {
                return c;
            }
            return null;
        }
    }

    public Cursor fetchInboxRecentByUniqueKeyIds() {
        synchronized (SafeSlinger.sDataLock) {
            String groupBy = MessageDbAdapter.KEY_KEYID;
            Cursor c = query(DATABASE_TABLE, new String[] {
                    MessageDbAdapter.KEY_ROWID, MessageDbAdapter.KEY_DATE_RECV,
                    MessageDbAdapter.KEY_DATE_SENT, MessageDbAdapter.KEY_ENCBODY,
                    MessageDbAdapter.KEY_FILEDIR, MessageDbAdapter.KEY_MSGHASH_BLOB,
                    MessageDbAdapter.KEY_FILELEN, MessageDbAdapter.KEY_FILENAME,
                    MessageDbAdapter.KEY_FILETYPE, MessageDbAdapter.KEY_KEYIDLONG,
                    MessageDbAdapter.KEY_PERSON, MessageDbAdapter.KEY_READ,
                    MessageDbAdapter.KEY_SEEN, MessageDbAdapter.KEY_STATUS,
                    MessageDbAdapter.KEY_TEXT, MessageDbAdapter.KEY_TYPE,
                    MessageDbAdapter.KEY_KEYID, MessageDbAdapter.KEY_MSGHASH,
                    MessageDbAdapter.KEY_RETNOTIFY, MessageDbAdapter.KEY_RETPUSHTOKEN,
                    MessageDbAdapter.KEY_RETRECEIPT
            }, null, null, groupBy, null, null);
            if (c != null) {
                return c;
            }
            return null;
        }
    }

    public int getUnseenInboxCount() throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            where.append("(");
            where.append(MessageDbAdapter.KEY_SEEN + "=" + MessageDbAdapter.MESSAGE_IS_NOT_SEEN);
            where.append(" AND ");
            where.append(MessageDbAdapter.KEY_STATUS + "!="
                    + MessageDbAdapter.MESSAGE_STATUS_GOTPUSH);
            where.append(")");

            Cursor c = query(DATABASE_TABLE, null, where.toString(), null, null, null, null);
            if (c != null) {
                int count = c.getCount();
                c.close();
                return count;
            }
            return -1;
        }
    }

    public int getActionRequiredInboxCountByThread(String keyId) throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(MessageDbAdapter.KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + "="
                        + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }
            where.append(" AND ");
            where.append(MessageDbAdapter.KEY_TYPE + "=" + MessageDbAdapter.MESSAGE_TYPE_INBOX);
            where.append(" AND ");

            where.append("(");
            where.append(MessageDbAdapter.KEY_SEEN + "=" + MessageDbAdapter.MESSAGE_IS_NOT_SEEN);
            where.append(" OR ");
            where.append(MessageDbAdapter.KEY_STATUS + "="
                    + MessageDbAdapter.MESSAGE_STATUS_GOTPUSH);
            where.append(")");

            Cursor c = query(DATABASE_TABLE, null, where.toString(), null, null, null, null);
            if (c != null) {
                int count = c.getCount();
                c.close();
                return count;
            }
            return -1;
        }
    }

    public void updateAllInboxAsSeenByThread(String keyId) throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(MessageDbAdapter.KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(MessageDbAdapter.KEY_KEYID + "="
                        + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }
            where.append(" AND ");
            where.append("(");
            where.append(MessageDbAdapter.KEY_SEEN + "=" + MessageDbAdapter.MESSAGE_IS_NOT_SEEN);
            where.append(")");
            Cursor c = query(DATABASE_TABLE, new String[] {
                    MessageDbAdapter.KEY_ROWID, MessageDbAdapter.KEY_SEEN,
                    MessageDbAdapter.KEY_KEYID
            }, where.toString(), null, null, null, null);
            if (c != null) {
                ContentValues values = new ContentValues();
                values.put(MessageDbAdapter.KEY_SEEN, MessageDbAdapter.MESSAGE_IS_SEEN); // seen
                while (c.moveToNext()) {
                    long rowId = c.getLong(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_ROWID));
                    update(DATABASE_TABLE, values, MessageDbAdapter.KEY_ROWID + "=" + rowId, null);
                }
                c.close();
            }
        }
    }

}
