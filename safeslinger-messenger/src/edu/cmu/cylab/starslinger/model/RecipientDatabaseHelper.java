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

public class RecipientDatabaseHelper extends SQLiteOpenHelper {
    private static RecipientDatabaseHelper sInstance = null;

    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    private static int sUserNumber = 0;
    public static final String DATABASE_NAME_ROOT = "safeslinger.recipient";

    public static final int DATABASE_VERSION = 8;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + RecipientDbAdapter.DATABASE_TABLE + " (" //
            + RecipientDbAdapter.KEY_ROWID + " integer primary key autoincrement, " //
            + RecipientDbAdapter.KEY_MYKEYIDLONG + " integer not null, " //
            + RecipientDbAdapter.KEY_EXCHDATE + " integer, " //
            + RecipientDbAdapter.KEY_CONTACTID + " text not null, " //
            + RecipientDbAdapter.KEY_CONTACTLKUP + " text not null, " //
            + RecipientDbAdapter.KEY_RAWCONTACTID + " text not null, " //
            + RecipientDbAdapter.KEY_NAME + " text not null, "//
            + RecipientDbAdapter.KEY_PHOTO + " blob, " //
            + RecipientDbAdapter.KEY_KEYIDLONG + " integer not null, " //
            + RecipientDbAdapter.KEY_KEYDATE + " integer not null, " //
            + RecipientDbAdapter.KEY_KEYUSERID + " text, " //
            + RecipientDbAdapter.KEY_PUSHTOKEN + " text, " //
            + RecipientDbAdapter.KEY_NOTIFY + " integer not null, " //
            + RecipientDbAdapter.KEY_PUBKEY + " blob not null, " //
            + RecipientDbAdapter.KEY_SOURCE + " integer not null, " //
            + RecipientDbAdapter.KEY_APPVERSION + " integer not null, " //
            + RecipientDbAdapter.KEY_HISTDATE + " integer, " //
            + RecipientDbAdapter.KEY_ACTIVE + " integer not null, " //
            + RecipientDbAdapter.KEY_MYKEYID + " text not null, " //
            + RecipientDbAdapter.KEY_KEYID + " text not null, "//
            + RecipientDbAdapter.KEY_INTROKEYID + " text, " //
            + RecipientDbAdapter.KEY_NOTREGDATE + " integer, " //
            + RecipientDbAdapter.KEY_MYNOTIFY + " integer, " //
            + RecipientDbAdapter.KEY_MYPUSHTOKEN + " text " //
            + ");";

    // Upgrade from 5 to 6 to support string-type longer key ids...
    private static final String DATABASE_UPG5TO6_MYKEYID = "alter table " //
            + RecipientDbAdapter.DATABASE_TABLE + " add column " //
            + RecipientDbAdapter.KEY_MYKEYID + " text;";
    private static final String DATABASE_UPG5TO6_KEYID = "alter table " //
            + RecipientDbAdapter.DATABASE_TABLE + " add column " //
            + RecipientDbAdapter.KEY_KEYID + " text;";

    // Upgrade to support secure introduction feature record introducer's id
    private static final String DATABASE_UPG6TO7_INTROKEYID = "alter table " //
            + RecipientDbAdapter.DATABASE_TABLE + " add column " //
            + RecipientDbAdapter.KEY_INTROKEYID + " text;";

    // Upgrade to record last non-registered token date, "my" push token
    private static final String DATABASE_UPG7TO8_NOTREGDATE = "alter table " //
            + RecipientDbAdapter.DATABASE_TABLE + " add column " //
            + RecipientDbAdapter.KEY_NOTREGDATE + " integer;";
    private static final String DATABASE_UPG7TO8_MYNOTIFY = "alter table " //
            + RecipientDbAdapter.DATABASE_TABLE + " add column " //
            + RecipientDbAdapter.KEY_MYNOTIFY + " integer;";
    private static final String DATABASE_UPG7TO8_MYPUSHTOKEN = "alter table " //
            + RecipientDbAdapter.DATABASE_TABLE + " add column " //
            + RecipientDbAdapter.KEY_MYPUSHTOKEN + " text;";

    public static RecipientDatabaseHelper getInstance(Context ctx) {
        if (sInstance == null) {
            // open for currently selected user
            sUserNumber = SafeSlingerPrefs.getUser();
            sInstance = new RecipientDatabaseHelper(ctx.getApplicationContext());
        } else {
            // if user has changed in this instance, close instance and reopen
            if (sUserNumber != SafeSlingerPrefs.getUser()) {
                sUserNumber = SafeSlingerPrefs.getUser();
                sInstance.close();
                sInstance = new RecipientDatabaseHelper(ctx.getApplicationContext());
            }
        }
        return sInstance;
    }

    private RecipientDatabaseHelper(Context context) {
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
                database.execSQL(DATABASE_UPG7TO8_NOTREGDATE);
                database.execSQL(DATABASE_UPG7TO8_MYNOTIFY);
                database.execSQL(DATABASE_UPG7TO8_MYPUSHTOKEN);
                break;

            case 6:
                database.execSQL(DATABASE_UPG6TO7_INTROKEYID);
                database.execSQL(DATABASE_UPG7TO8_NOTREGDATE);
                database.execSQL(DATABASE_UPG7TO8_MYNOTIFY);
                database.execSQL(DATABASE_UPG7TO8_MYPUSHTOKEN);
                break;

            case 5:
                database.execSQL(DATABASE_UPG5TO6_MYKEYID);
                database.execSQL(DATABASE_UPG5TO6_KEYID);
                database.execSQL(DATABASE_UPG6TO7_INTROKEYID);
                database.execSQL(DATABASE_UPG7TO8_NOTREGDATE);
                database.execSQL(DATABASE_UPG7TO8_MYNOTIFY);
                database.execSQL(DATABASE_UPG7TO8_MYPUSHTOKEN);
                break;

            default:
                database.execSQL("DROP TABLE IF EXISTS " + RecipientDbAdapter.DATABASE_TABLE);
                onCreate(database);
                break;
        }
    }

    public static boolean deleteRecipientDatabase(int userNumber) {
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
