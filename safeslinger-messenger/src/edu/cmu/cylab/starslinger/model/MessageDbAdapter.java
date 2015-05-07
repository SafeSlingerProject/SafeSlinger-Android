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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.util.SSUtil;

/***
 * This is adapter is meant to store the messages sent and received from the
 * user. When a message is first decrypted, one row is created and updated. When
 * a message is first composed one row is created and updated.
 */
public class MessageDbAdapter {
    private static MessageDbAdapter sInstance = null;
    private static int sUserNumber = 0;

    public static final String DATABASE_TABLE = "message";

    // Database fields
    public static final String KEY_ROWID = "_id";
    public static final String KEY_DATE_RECV = "date"; // Received UTC (req)
    public static final String KEY_DATE_SENT = "datesent"; // sent UTC
    public static final String KEY_READ = "read"; // decoded (req)
    public static final String KEY_STATUS = "status"; // complete/failed (req)
    public static final String KEY_TYPE = "type"; // inbox/sent (req)
    public static final String KEY_ENCBODY = "body"; // encoded body
    public static final String KEY_SEEN = "seen"; // seen in list (req)
    public static final String KEY_PERSON = "person"; // name of other person
    public static final String KEY_KEYID = "keyid_str"; // key id of the
    public static final String KEY_FILENAME = "filename"; // name of file
    public static final String KEY_FILELEN = "filelen"; // len encoded file
    public static final String KEY_FILETYPE = "filetype"; // file mime type
    public static final String KEY_FILEDIR = "filedir"; // location file saved
    public static final String KEY_TEXT = "text"; // text message
    public static final String KEY_RAWFILE = "file"; // special files (was enc)
    public static final String KEY_MSGHASH = "fileid_str"; // file retrieval id
    public static final String KEY_RETNOTIFY = "ret_notify"; // return notify
    public static final String KEY_RETPUSHTOKEN = "ret_pushtoken"; // ret. token
    public static final String KEY_RETRECEIPT = "ret_receipt"; // ret. receipt

    @Deprecated
    public static final String KEY_KEYIDLONG = "keyid";
    @Deprecated
    public static final String KEY_MSGHASH_BLOB = "fileid";

    public static final int MESSAGE_TYPE_INBOX = 1;
    public static final int MESSAGE_TYPE_SENT = 2;

    public static final int MESSAGE_STATUS_NONE = -1;
    public static final int MESSAGE_STATUS_COMPLETE_MSG = 0;
    public static final int MESSAGE_STATUS_EXPIRED = 4;
    public static final int MESSAGE_STATUS_QUEUED = 8;
    public static final int MESSAGE_STATUS_DRAFT = 16;
    public static final int MESSAGE_STATUS_FILE_DECRYPTED = 32;
    public static final int MESSAGE_STATUS_GOTPUSH = 64;
    public static final int MESSAGE_STATUS_FAILED = 128;

    public static final int MESSAGE_IS_NOT_READ = 0;
    public static final int MESSAGE_IS_READ = 1;

    public static final int MESSAGE_IS_NOT_SEEN = 0;
    public static final int MESSAGE_IS_SEEN = 1;

    private Context mContext;
    private SQLiteDatabase mDatabase;
    private MessageDatabaseHelper mDbHelper;

    public static MessageDbAdapter openInstance(Context ctx) {
        if (sInstance == null) {
            // open for currently selected user
            sUserNumber = SafeSlingerPrefs.getUser();
            sInstance = new MessageDbAdapter(ctx.getApplicationContext());
        } else {
            // if user has changed in this instance, close instance and reopen
            if (sUserNumber != SafeSlingerPrefs.getUser()) {
                sUserNumber = SafeSlingerPrefs.getUser();
                closeInstance();
                sInstance = new MessageDbAdapter(ctx.getApplicationContext());
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

    private MessageDbAdapter(Context context) {
        mContext = context;
        synchronized (SafeSlinger.sDataLock) {
            mDbHelper = MessageDatabaseHelper.getInstance(mContext);
            try {
                mDatabase = mDbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                e.printStackTrace();
                mDbHelper = MessageDatabaseHelper.getInstance(mContext);
                mDatabase = mDbHelper.getWritableDatabase();
            }
        }
    }

    public int getVersion() {
        synchronized (SafeSlinger.sDataLock) {
            if (mDatabase != null) {
                return mDatabase.getVersion();
            } else {
                return MessageDatabaseHelper.DATABASE_VERSION;
            }
        }
    }

    private long insert(String table, String nullColumnHack, ContentValues values) {
        if (!mDatabase.isOpen()) {
            sInstance = new MessageDbAdapter(mContext);
        }
        long insert = mDatabase.insert(table, nullColumnHack, values);
        // no backup needed for messages...
        return insert;
    }

    private int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        if (!mDatabase.isOpen()) {
            sInstance = new MessageDbAdapter(mContext);
        }
        int update = mDatabase.update(table, values, whereClause, whereArgs);
        // no backup needed for messages...
        return update;
    }

    private int delete(String table, String whereClause, String[] whereArgs) {
        if (!mDatabase.isOpen()) {
            sInstance = new MessageDbAdapter(mContext);
        }
        int delete = mDatabase.delete(table, whereClause, whereArgs);
        // no backup needed for messages...
        return delete;
    }

    private Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy, String limit) {
        if (!mDatabase.isOpen()) {
            sInstance = new MessageDbAdapter(mContext);
        }
        Cursor query = mDatabase.query(table, columns, selection, selectionArgs, groupBy, having,
                orderBy, limit);
        return query;
    }

    private Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy) {
        if (!mDatabase.isOpen()) {
            sInstance = new MessageDbAdapter(mContext);
        }
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

    public long createDraftMessage(RecipientRow recip, MessageData msg, long submitTime) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();

            values.put(KEY_DATE_SENT, submitTime); // Sent UTC
            values.put(KEY_READ, MESSAGE_IS_READ); // decoded
            values.put(KEY_STATUS, MESSAGE_STATUS_DRAFT); // complete/failed
            values.put(KEY_TYPE, MESSAGE_TYPE_SENT); // inbox/sent
            values.put(KEY_SEEN, MESSAGE_IS_SEEN); // seen in list
            if (recip != null) {
                if (recip.getName() != null)
                    values.put(KEY_PERSON, recip.getName()); // name of other
                                                             // person
                values.put(KEY_KEYID, recip.getKeyid()); // key id of the sig...
            }
            if (msg != null) {
                if (msg.getFileName() != null)
                    values.put(KEY_FILENAME, msg.getFileName()); // name of file

                values.put(KEY_FILELEN, msg.getFileSize()); // len encoded file

                if (msg.getFileType() != null)
                    values.put(KEY_FILETYPE, msg.getFileType()); // file mime
                                                                 // type
                if (msg.getFileDir() != null)
                    values.put(KEY_FILEDIR, msg.getFileDir()); // location file
                                                               // saved
                if (msg.getText() != null)
                    values.put(KEY_TEXT, msg.getText()); // text message
            }

            // backward compatibility for upgraded databases....
            values.put(KEY_KEYIDLONG, 0);

            return insert(DATABASE_TABLE, null, values);
        }
    }

    public boolean updateDraftMessage(long rowId, RecipientRow recip, MessageData msg) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();

            // sent time should be updated, as user has edited draft
            values.put(KEY_DATE_SENT, System.currentTimeMillis()); // Sent UTC

            values.put(KEY_STATUS, MESSAGE_STATUS_DRAFT); // complete/failed
            if (recip != null) {
                if (recip.getName() != null)
                    values.put(KEY_PERSON, recip.getName()); // name of other
                                                             // person
                values.put(KEY_KEYID, recip.getKeyid()); // key id of the sig...
            }
            if (msg != null) {
                if (msg.getFileName() != null)
                    values.put(KEY_FILENAME, msg.getFileName()); // name of file

                values.put(KEY_FILELEN, msg.getFileSize()); // len encoded file

                if (msg.getFileType() != null)
                    values.put(KEY_FILETYPE, msg.getFileType()); // file mime
                                                                 // type
                if (msg.getFileDir() != null)
                    values.put(KEY_FILEDIR, msg.getFileDir()); // location file
                                                               // saved
                if (msg.getText() != null)
                    values.put(KEY_TEXT, msg.getText()); // text message
            }

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public boolean updateEnqueuedMessage(long rowId) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();
            values.put(KEY_STATUS, MESSAGE_STATUS_QUEUED); // complete/failed

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public boolean updateMessageSent(long rowId, String person, String keyid, String msgHash,
            String filename, int filelen, String filetype, String filedir, String text, int status)
            throws GeneralException {
        synchronized (SafeSlinger.sDataLock) {
            if (TextUtils.isEmpty(person))
                throw new GeneralException("bad sent message: person required");
            if (CryptTools.isNullKeyId(keyid))
                throw new GeneralException("bad sent message: key id required");
            if (msgHash == null || msgHash.length() == 0)
                throw new GeneralException("bad sent message: message hash id required");

            ContentValues values = new ContentValues();

            // sent time: do not set here, we want to preserve last user
            // time the user has edited...

            values.put(KEY_READ, MESSAGE_IS_READ); // decoded
            values.put(KEY_STATUS, status); // complete/failed
            values.put(KEY_TYPE, MESSAGE_TYPE_SENT); // inbox/sent
            values.put(KEY_SEEN, MESSAGE_IS_SEEN); // seen in list
            if (msgHash != null)
                values.put(KEY_MSGHASH, msgHash); // file retrieval id
            if (person != null)
                values.put(KEY_PERSON, person); // name of other person
            values.put(KEY_KEYID, keyid); // key id of the sig...
            if (filename != null)
                values.put(KEY_FILENAME, filename); // name of file
            values.put(KEY_FILELEN, filelen); // len encoded file
            if (filetype != null)
                values.put(KEY_FILETYPE, filetype); // file mime type
            if (filedir != null)
                values.put(KEY_FILEDIR, filedir); // location file saved
            if (text != null)
                values.put(KEY_TEXT, text); // text message

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    /*
     * TODO: MSgFragment doDecrypt
     */
    public long createMessageDecrypted(MessageData data, MessagePacket msg, String keyid) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();

            // move values from inbox database
            values.put(KEY_DATE_RECV, data.getDateRecv()); // Received
            values.put(KEY_TYPE, MESSAGE_TYPE_INBOX); // inbox/sent
            values.put(KEY_SEEN, MESSAGE_IS_NOT_SEEN); // seen
            values.put(KEY_MSGHASH, data.getMsgHash()); // file
            values.put(KEY_ENCBODY, data.getEncBody()); // encoded
            values.put(KEY_STATUS, data.getStatus()); // complete/failed

            // new values from decrypted message packet
            values.put(KEY_DATE_SENT, msg.getDateSent()); // Sent UTC
            values.put(KEY_READ, MESSAGE_IS_READ); // decoded
            values.put(KEY_KEYID, keyid); // key id of the sig...
            if (!TextUtils.isEmpty(msg.getFileName()))
                values.put(KEY_FILENAME, msg.getFileName()); // name of file
            values.put(KEY_FILELEN, msg.getFileSize()); // len encoded file
            if (!TextUtils.isEmpty(msg.getFileType()))
                values.put(KEY_FILETYPE, msg.getFileType()); // file mime type
            if (!TextUtils.isEmpty(msg.getText()))
                values.put(KEY_TEXT, msg.getText()); // text message
            if (!TextUtils.isEmpty(msg.getPerson()))
                values.put(KEY_PERSON, msg.getPerson()); // name

            // backward compatibility for upgraded databases....
            values.put(KEY_KEYIDLONG, 0);

            return insert(DATABASE_TABLE, null, values);
        }
    }

    public boolean updateFileDecrypted(long rowId) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();
            values.put(KEY_STATUS, MESSAGE_STATUS_FILE_DECRYPTED); // complete/failed

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public boolean updateRawSafeSlingerFile(long rowId, byte[] rawFile) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();
            values.put(KEY_RAWFILE, rawFile);

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public boolean updateMessageFileLocation(long rowId, String filename, String filedir) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();
            if (filename != null)
                values.put(KEY_FILENAME, filename); // name of file
            if (filedir != null)
                values.put(KEY_FILEDIR, filedir); // location file saved

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public boolean updateMessageKeyId2String(long rowId, long keyid_long) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();
            values.put(KEY_KEYID, SSUtil.longKeyId2Base64String(keyid_long));

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public boolean updateMessageKeyId(long rowId, String keyid) {
        synchronized (SafeSlinger.sDataLock) {
            ContentValues values = new ContentValues();
            values.put(KEY_KEYID, keyid); // key id of the sig...

            return update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    /*
     * TODO:MsgFragment doDecrypt
     */
    /**
     * Deletes message
     */
    public boolean deleteMessage(long rowId) {
        synchronized (SafeSlinger.sDataLock) {
            return delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
        }
    }

    public int deleteThread(String keyId) {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(KEY_KEYID + "=" + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }
            return delete(DATABASE_TABLE, where.toString(), null);
        }
    }

    public Cursor fetchAllMessagesUpgradeTo6() {
        synchronized (SafeSlinger.sDataLock) {
            Cursor c = query(DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_KEYIDLONG
            }, null, null, null, null, null);
            return c;
        }
    }

    public Cursor fetchAllMessagesQueuedDraft() {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            where.append(KEY_STATUS + "=" + MESSAGE_STATUS_DRAFT);
            where.append(" OR ");
            where.append(KEY_STATUS + "=" + MESSAGE_STATUS_QUEUED);

            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_DATE_RECV, KEY_DATE_SENT, KEY_ENCBODY, KEY_FILEDIR,
                    KEY_MSGHASH_BLOB, KEY_FILELEN, KEY_FILENAME, KEY_FILETYPE, KEY_KEYIDLONG,
                    KEY_PERSON, KEY_READ, KEY_SEEN, KEY_STATUS, KEY_TEXT, KEY_TYPE, KEY_KEYID,
                    KEY_MSGHASH, KEY_RETNOTIFY, KEY_RETPUSHTOKEN, KEY_RETRECEIPT
            }, where.toString(), null, null, null, null, null);
            return c;
        }
    }

    public Cursor fetchMessageSmall(long rowId) throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            String where = KEY_ROWID + "=" + rowId;
            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_DATE_RECV, KEY_DATE_SENT, KEY_ENCBODY, KEY_FILEDIR,
                    KEY_MSGHASH_BLOB, KEY_FILELEN, KEY_FILENAME, KEY_FILETYPE, KEY_KEYIDLONG,
                    KEY_PERSON, KEY_READ, KEY_SEEN, KEY_STATUS, KEY_TEXT, KEY_TYPE, KEY_KEYID,
                    KEY_MSGHASH, KEY_RETNOTIFY, KEY_RETPUSHTOKEN, KEY_RETRECEIPT
            }, where, null, null, null, null, null);
            return c;
        }
    }

    public int getAllMessageCountByThread(String keyId) {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(KEY_KEYID + "=" + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }

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

    public Cursor fetchAllMessagesByThread(String keyId) {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(KEY_KEYID + "=" + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }

            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_DATE_RECV, KEY_DATE_SENT, KEY_ENCBODY, KEY_FILEDIR,
                    KEY_MSGHASH_BLOB, KEY_FILELEN, KEY_FILENAME, KEY_FILETYPE, KEY_KEYIDLONG,
                    KEY_PERSON, KEY_READ, KEY_SEEN, KEY_STATUS, KEY_TEXT, KEY_TYPE, KEY_KEYID,
                    KEY_MSGHASH, KEY_RETNOTIFY, KEY_RETPUSHTOKEN, KEY_RETRECEIPT
            }, where.toString(), null, null, null, null, null);
            return c;
        }
    }

    // public long fetchLastRecentMessageTime() {
    // synchronized (SafeSlinger.sDataLock) {
    // /*
    // * SELECT MessageDbAdapter.KEY_ROWID, MessageDbAdapter.KEY_DATE_RECV
    // * FROM table ORDER BY MessageDbAdapter.KEY_ROWID DESC LIMIT 1;
    // */
    // Cursor c = query(DATABASE_TABLE, new String[] {
    // MessageDbAdapter.KEY_ROWID, MessageDbAdapter.KEY_DATE_RECV
    // }, null, null, null, null, MessageDbAdapter.KEY_ROWID + " DESC", "1");
    // if (c != null) {
    // try {
    // if (c.moveToFirst()) {
    // long time = c.getLong(c.getColumnIndex(MessageDbAdapter.KEY_DATE_RECV));
    // return time;
    // }
    // } finally {
    // c.close();
    // }
    // }
    // return -1;
    // }
    // }

    public Cursor fetchMessageRecent(String keyId) {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(KEY_KEYID + "=" + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }

            String groupBy = KEY_KEYID;
            Cursor c = query(DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_DATE_RECV, KEY_DATE_SENT, KEY_ENCBODY, KEY_FILEDIR,
                    KEY_MSGHASH_BLOB, KEY_FILELEN, KEY_FILENAME, KEY_FILETYPE, KEY_KEYIDLONG,
                    KEY_PERSON, KEY_READ, KEY_SEEN, KEY_STATUS, KEY_TEXT, KEY_TYPE, KEY_KEYID,
                    KEY_MSGHASH, KEY_RETNOTIFY, KEY_RETPUSHTOKEN, KEY_RETRECEIPT
            }, where.toString(), null, groupBy, null, null);
            return c;
        }
    }

    public Cursor fetchMessagesRecentByUniqueKeyIds() {
        synchronized (SafeSlinger.sDataLock) {
            String groupBy = KEY_KEYID;
            Cursor c = query(DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_DATE_RECV, KEY_DATE_SENT, KEY_ENCBODY, KEY_FILEDIR,
                    KEY_MSGHASH_BLOB, KEY_FILELEN, KEY_FILENAME, KEY_FILETYPE, KEY_KEYIDLONG,
                    KEY_PERSON, KEY_READ, KEY_SEEN, KEY_STATUS, KEY_TEXT, KEY_TYPE, KEY_KEYID,
                    KEY_MSGHASH, KEY_RETNOTIFY, KEY_RETPUSHTOKEN, KEY_RETRECEIPT
            }, null, null, groupBy, null, null);
            return c;
        }
    }

    public int getUnseenMessageCount() throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            where.append("(");
            where.append(KEY_SEEN + "=" + MESSAGE_IS_NOT_SEEN);
            where.append(" AND ");
            where.append(KEY_STATUS + "!=" + MESSAGE_STATUS_GOTPUSH);
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

    public int getAllMessageCount() throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            Cursor c = query(DATABASE_TABLE, null, null, null, null, null, null);
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

    public byte[] getRawFile(long rowId) {
        synchronized (SafeSlinger.sDataLock) {
            byte[] rawFile = null;
            String where = KEY_ROWID + "=" + rowId;
            Cursor c = query(true, DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_RAWFILE
            }, where, null, null, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        rawFile = c.getBlob(c.getColumnIndexOrThrow(KEY_RAWFILE));
                    }
                } finally {
                    c.close();
                }
            }
            return rawFile;
        }
    }

    public int getActionRequiredMessageCountByThread(String keyId) throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(KEY_KEYID + "=" + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }
            where.append(" AND ");
            where.append(KEY_TYPE + "=" + MESSAGE_TYPE_INBOX);
            where.append(" AND ");

            where.append("(");
            where.append(KEY_SEEN + "=" + MESSAGE_IS_NOT_SEEN);
            where.append(" OR ");
            where.append(KEY_STATUS + "=" + MESSAGE_STATUS_GOTPUSH);
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

    public int getDraftMessageCountByThread(String keyId) throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(KEY_KEYID + "=" + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }
            where.append(" AND ");
            where.append("(");
            where.append(KEY_STATUS + "=" + MESSAGE_STATUS_DRAFT);
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

    public void updateAllMessagesAsSeenByThread(String keyId) throws SQLException {
        synchronized (SafeSlinger.sDataLock) {
            StringBuilder where = new StringBuilder();
            if (TextUtils.isEmpty(keyId)) {
                where.append("(");
                where.append(KEY_KEYID + " IS NULL");
                where.append(" OR ");
                where.append(KEY_KEYID + "=\'\'");
                where.append(")");
            } else {
                where.append("(");
                where.append(KEY_KEYID + "=" + DatabaseUtils.sqlEscapeString("" + keyId));
                where.append(")");
            }
            where.append(" AND ");
            where.append("(");
            where.append(KEY_SEEN + "=" + MESSAGE_IS_NOT_SEEN);
            where.append(")");
            Cursor c = query(DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_SEEN, KEY_KEYID
            }, where.toString(), null, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        ContentValues values = new ContentValues();
                        values.put(KEY_SEEN, MESSAGE_IS_SEEN); // seen
                        do {
                            long rowId = c.getLong(c.getColumnIndexOrThrow(KEY_ROWID));
                            update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null);
                        } while (c.moveToNext());
                    }
                } finally {
                    c.close();
                }
            }
        }
    }

    public static String getStatusCode(int stat) {
        String status;
        switch (stat) {
            case MESSAGE_STATUS_COMPLETE_MSG:
                status = ("COMPLETE");
                break;
            case MESSAGE_STATUS_DRAFT:
                status = ("DRAFT");
                break;
            case MESSAGE_STATUS_EXPIRED:
                status = ("EXPIRED");
                break;
            case MESSAGE_STATUS_FAILED:
                status = ("FAILED");
                break;
            case MESSAGE_STATUS_FILE_DECRYPTED:
                status = ("DECFIL");
                break;
            case MESSAGE_STATUS_NONE:
                status = ("NONE");
                break;
            case MESSAGE_STATUS_GOTPUSH:
                status = ("GOTPUSH");
                break;
            case MESSAGE_STATUS_QUEUED:
                status = ("QUEUED");
                break;
            default:
                status = Integer.toString(stat);
                break;
        }
        return status;
    }

}
