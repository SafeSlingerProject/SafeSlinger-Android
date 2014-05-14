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

package edu.cmu.cylab.starslinger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import android.app.NotificationManager;
import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.model.RecipientDatabaseHelper;
import edu.cmu.cylab.starslinger.util.SSUtil;
import edu.cmu.cylab.starslinger.view.HomeActivity;

public class KeyBackupAgent extends BackupAgentHelper {

    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "safeslinger.prefs";
    static final String FILES_BACKUP_KEY = "safeslinger.files";

    // Allocate a helper and add it to the backup agent

    @Override
    public void onCreate() {

        SharedPreferencesBackupHelper phelper = new SharedPreferencesBackupHelper(this,
                SafeSlingerPrefs.PREFS_RECOVER_YES);
        addHelper(PREFS_BACKUP_KEY, phelper);

        FileBackupHelper fhelper = new FileBackupHelper(this, getBackupFiles());
        addHelper(FILES_BACKUP_KEY, fhelper);
    }

    public String[] getBackupFiles() {
        ArrayList<String> allFiles = new ArrayList<String>();

        // add databases
        int users = SafeSlinger.getTotalUsers();
        for (int i = 0; i < users; i++) {
            if (i == 0) {
                allFiles.add("../databases/" + RecipientDatabaseHelper.DATABASE_NAME_ROOT);
            } else {
                allFiles.add("../databases/" + RecipientDatabaseHelper.DATABASE_NAME_ROOT + i);
            }
        }

        // add encrypted private keys
        int userNumber = 0;
        boolean exists;
        do {
            exists = false;
            String keyFile = CryptTools.getKeyFile(userNumber);
            if (SSUtil.fileExists(getApplicationContext(), keyFile)) {
                exists = true;
                allFiles.add(keyFile);
            }
            userNumber++;
        } while (exists);

        // That's the file set; now back it all up
        return allFiles.toArray(new String[allFiles.size()]);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) throws IOException {
        // Hold the lock while the BackupHelper performs backup
        synchronized (SafeSlinger.sDataLock) {
            super.onBackup(oldState, data, newState);
        }

        // store backup time and cancel pending notifications...
        SafeSlingerPrefs.setBackupCompleteDate(new Date().getTime());
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) getSystemService(ns);
        nm.cancel(HomeActivity.NOTIFY_BACKUP_DELAY_ID);
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
            throws IOException {
        // Hold the lock while the BackupHelper restores
        synchronized (SafeSlinger.sDataLock) {
            super.onRestore(data, appVersionCode, newState);
        }

        // store restoration time and cancel pending notifications...
        SafeSlingerPrefs.setRestoreCompleteDate(new Date().getTime());
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) getSystemService(ns);
        nm.cancel(HomeActivity.NOTIFY_BACKUP_DELAY_ID);
    }
}
