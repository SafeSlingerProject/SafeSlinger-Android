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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;

public class MessageDatabaseHelper extends SQLiteOpenHelper {

    private static MessageDatabaseHelper sInstance = null;
    private static int sUserNumber = 0;

    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    public static final String DATABASE_NAME_ROOT = "safeslinger.message";

    public static final int DATABASE_VERSION = 8;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table " + MessageDbAdapter.DATABASE_TABLE
            + " (" //
            + MessageDbAdapter.KEY_ROWID + " integer primary key autoincrement, " //
            + MessageDbAdapter.KEY_DATE_RECV + " integer, " //
            + MessageDbAdapter.KEY_DATE_SENT + " integer, " //
            + MessageDbAdapter.KEY_READ + " integer not null, " //
            + MessageDbAdapter.KEY_STATUS + " integer not null, " //
            + MessageDbAdapter.KEY_TYPE + " integer not null, " //
            + MessageDbAdapter.KEY_ENCBODY + " blob, " //
            + MessageDbAdapter.KEY_SEEN + " integer not null, " //
            + MessageDbAdapter.KEY_PERSON + " text, " //
            + MessageDbAdapter.KEY_KEYIDLONG + " integer, " //
            + MessageDbAdapter.KEY_MSGHASH_BLOB + " blob, " //
            + MessageDbAdapter.KEY_FILENAME + " text, " //
            + MessageDbAdapter.KEY_FILELEN + " integer, " //
            + MessageDbAdapter.KEY_FILETYPE + " text, " //
            + MessageDbAdapter.KEY_FILEDIR + " text, " //
            + MessageDbAdapter.KEY_TEXT + " text, " //
            + MessageDbAdapter.KEY_RAWFILE + " blob, " //
            + MessageDbAdapter.KEY_KEYID + " text, " //
            + MessageDbAdapter.KEY_MSGHASH + " text, " //
            + MessageDbAdapter.KEY_RETNOTIFY + " integer, " //
            + MessageDbAdapter.KEY_RETPUSHTOKEN + " text, " //
            + MessageDbAdapter.KEY_RETRECEIPT + " text " //
            + ");";

    // Upgrade from 5 to 6 to support string-type longer key ids...
    private static final String DATABASE_UPG5TO6_KEYID = "alter table " //
            + MessageDbAdapter.DATABASE_TABLE + " add column " //
            + MessageDbAdapter.KEY_KEYID + " text;";

    // Upgrade from 6 to 7 to support string-seekable message identifiers...
    private static final String DATABASE_UPG6TO7_MSGHASH = "alter table " //
            + MessageDbAdapter.DATABASE_TABLE + " add column " //
            + MessageDbAdapter.KEY_MSGHASH + " text;";

    // Upgrade from 7 to 8 to support senders updated token/receipt
    private static final String DATABASE_UPG7TO8_RETNOTIFY = "alter table " //
            + MessageDbAdapter.DATABASE_TABLE + " add column " //
            + MessageDbAdapter.KEY_RETNOTIFY + " integer;";
    private static final String DATABASE_UPG7TO8_RETPUSHTOKEN = "alter table " //
            + MessageDbAdapter.DATABASE_TABLE + " add column " //
            + MessageDbAdapter.KEY_RETPUSHTOKEN + " text;";
    private static final String DATABASE_UPG7TO8_RETRECEIPT = "alter table " //
            + MessageDbAdapter.DATABASE_TABLE + " add column " //
            + MessageDbAdapter.KEY_RETRECEIPT + " text;";

    public static MessageDatabaseHelper getInstance(Context ctx) {
        if (sInstance == null) {
            // open for currently selected user
            sUserNumber = SafeSlingerPrefs.getUser();
            sInstance = new MessageDatabaseHelper(ctx.getApplicationContext());
        } else {
            // if user has changed in this instance, close instance and reopen
            if (sUserNumber != SafeSlingerPrefs.getUser()) {
                sUserNumber = SafeSlingerPrefs.getUser();
                sInstance.close();
                sInstance = new MessageDatabaseHelper(ctx.getApplicationContext());
            }
        }
        return sInstance;
    }

    private MessageDatabaseHelper(Context context) {
        super(context, getDatabaseNameByUser(), null, DATABASE_VERSION);
    }

    private static String getDatabaseNameByUser() {
        return (sUserNumber == 0) ? DATABASE_NAME_ROOT : (DATABASE_NAME_ROOT + sUserNumber);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        MyLog.w(TAG, String.format(Locale.US, "Upgrading database from version %d to %d...",
                oldVersion, newVersion));
        switch (oldVersion) {
            case 7:
                database.execSQL(DATABASE_UPG7TO8_RETNOTIFY);
                database.execSQL(DATABASE_UPG7TO8_RETPUSHTOKEN);
                database.execSQL(DATABASE_UPG7TO8_RETRECEIPT);
                break;

            case 6:
                database.execSQL(DATABASE_UPG6TO7_MSGHASH);
                database.execSQL(DATABASE_UPG7TO8_RETNOTIFY);
                database.execSQL(DATABASE_UPG7TO8_RETPUSHTOKEN);
                database.execSQL(DATABASE_UPG7TO8_RETRECEIPT);
                break;

            case 5:
                database.execSQL(DATABASE_UPG5TO6_KEYID);
                database.execSQL(DATABASE_UPG6TO7_MSGHASH);
                database.execSQL(DATABASE_UPG7TO8_RETNOTIFY);
                database.execSQL(DATABASE_UPG7TO8_RETPUSHTOKEN);
                database.execSQL(DATABASE_UPG7TO8_RETRECEIPT);
                break;

            default:
                database.execSQL("DROP TABLE IF EXISTS " + MessageDbAdapter.DATABASE_TABLE);
                onCreate(database);
                break;
        }
    }

    public static boolean deleteMessageDatabase(int userNumber) {
        synchronized (SafeSlinger.sDataLock) {
            if (sUserNumber == userNumber) {
                sInstance.close();
            }

            Context ctx = SafeSlinger.getApplication();
            if (userNumber != 0) {
                return ctx.deleteDatabase(DATABASE_NAME_ROOT + userNumber);
            } else {
                return false; // never delete root
            }
        }
    }

}
