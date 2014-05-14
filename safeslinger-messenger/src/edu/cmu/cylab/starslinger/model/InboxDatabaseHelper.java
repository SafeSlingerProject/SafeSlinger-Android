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

import java.util.Locale;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;

public class InboxDatabaseHelper extends SQLiteOpenHelper {
    private static InboxDatabaseHelper sInstance = null;

    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    private static final String DATABASE_NAME = "safeslinger.inbox";

    public static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table " + InboxDbAdapter.DATABASE_TABLE
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
            + MessageDbAdapter.KEY_ENCFILE + " blob, " //
            + MessageDbAdapter.KEY_KEYID + " text, " //
            + MessageDbAdapter.KEY_MSGHASH + " text, " //
            + MessageDbAdapter.KEY_RETNOTIFY + " integer, " //
            + MessageDbAdapter.KEY_RETPUSHTOKEN + " text, " //
            + MessageDbAdapter.KEY_RETRECEIPT + " text " //
            + ");";

    public static InboxDatabaseHelper getInstance(Context ctx) {
        // this is a central instance accessible to all users
        if (sInstance == null) {
            sInstance = new InboxDatabaseHelper(ctx.getApplicationContext());
        }
        return sInstance;
    }

    private InboxDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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

            default:
                database.execSQL("DROP TABLE IF EXISTS " + InboxDbAdapter.DATABASE_TABLE);
                onCreate(database);
                break;
        }
    }
}
