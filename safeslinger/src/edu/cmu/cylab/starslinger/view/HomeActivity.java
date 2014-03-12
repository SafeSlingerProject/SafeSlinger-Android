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

package edu.cmu.cylab.starslinger.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import a_vcard.android.syncml.pim.VDataBuilder;
import a_vcard.android.syncml.pim.VNode;
import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.ContactStruct.ContactMethod;
import a_vcard.android.syncml.pim.vcard.Name;
import a_vcard.android.syncml.pim.vcard.VCardException;
import a_vcard.android.syncml.pim.vcard.VCardParser;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.app.backup.RestoreObserver;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LabeledIntent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Base64;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import edu.cmu.cylab.keyslinger.lib.ControllerActivity;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.ConfigData.extra;
import edu.cmu.cylab.starslinger.Eula;
import edu.cmu.cylab.starslinger.ExchangeException;
import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.CryptToolsLegacy;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgNonExistingKeyException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPacketSizeException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPeerKeyFormatException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider;
import edu.cmu.cylab.starslinger.model.ContactImpp;
import edu.cmu.cylab.starslinger.model.CryptoMsgPrivateData;
import edu.cmu.cylab.starslinger.model.ImppValue;
import edu.cmu.cylab.starslinger.model.MessageData;
import edu.cmu.cylab.starslinger.model.MessageDatabaseHelper;
import edu.cmu.cylab.starslinger.model.MessageDbAdapter;
import edu.cmu.cylab.starslinger.model.MessagePacket;
import edu.cmu.cylab.starslinger.model.MessageRow;
import edu.cmu.cylab.starslinger.model.MessageRow.MsgAction;
import edu.cmu.cylab.starslinger.model.MessageTransport;
import edu.cmu.cylab.starslinger.model.RecipientDatabaseHelper;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientRow;
import edu.cmu.cylab.starslinger.model.SlingerIdentity;
import edu.cmu.cylab.starslinger.model.UseContactItem;
import edu.cmu.cylab.starslinger.model.UseContactItem.UCType;
import edu.cmu.cylab.starslinger.transaction.C2DMBaseReceiver;
import edu.cmu.cylab.starslinger.transaction.C2DMReceiver;
import edu.cmu.cylab.starslinger.transaction.C2DMessaging;
import edu.cmu.cylab.starslinger.transaction.MessageNotFoundException;
import edu.cmu.cylab.starslinger.transaction.WebEngine;
import edu.cmu.cylab.starslinger.util.SSUtil;
import edu.cmu.cylab.starslinger.view.ComposeFragment.OnComposeResultListener;
import edu.cmu.cylab.starslinger.view.MessagesFragment.OnMessagesResultListener;
import edu.cmu.cylab.starslinger.view.SlingerFragment.OnSlingerResultListener;

public class HomeActivity extends BaseActivity implements Eula.OnEulaAgreedTo,
        OnComposeResultListener, OnMessagesResultListener, OnSlingerResultListener {
    private static final String TAG = ConfigData.LOG_TAG;

    // constants
    public static final int RESULT_PASSPHRASE_EXPIRED = 7123;
    private static final int RESULT_PICK_CONTACT_SENDER = 1;
    private static final int RESULT_ERROR = 9;
    private static final int RESULT_PICK_MSGAPP = 13;
    private static final int REQUEST_QRECEIVE_MGS = 14;
    private static final int VIEW_RECIPSEL_ID = 140;
    private static final int VIEW_EXCHANGE_ID = 160;
    private static final int VIEW_FILESAVE_ID = 130;
    private static final int VIEW_FILEATTACH_ID = 120;
    private static final int VIEW_FINDCONTACT_ID = 190;
    private static final int VIEW_PASSPHRASE_ID = 220;
    private static final int VIEW_PASSPHRASE_CHANGE_ID = 230;
    private static final int VIEW_SENDINVITE_ID = 240;
    private static final int VIEW_PASSPHRASE_VERIFY_ID = 250;
    private static final int VIEW_SETTINGS_ID = 260;
    private static final int VIEW_SAVE_ID = 280;
    private static final int RECOVERY_PUSHREG = 310;
    private static final int RECOVERY_CREATEKEY = 340;
    private static final int RECOVERY_EXCHANGEIMPORT = 380;
    private static final int RECOVERY_BACKUP_RESTORE = 390;
    private static final int MENU_LOGOUT = 440;
    private static final int MENU_SENDAPP = 450;
    private static final int MENU_SETTINGS = 460;
    private static final int MENU_REFERENCE = 470;
    private static final int MENU_SENDINTRO = 480;
    private static final int MENU_FEEDBACK = 490;
    public static final int NOTIFY_NEW_MSG_ID = 500;
    public static final int NOTIFY_BACKUP_DELAY_ID = 501;
    public static final int NOTIFY_PASS_CACHED_ID = 502;
    public static final int NOTIFY_SLINGKEYS_REMIND_ID = 503;

    private static final int MS_POLL_INTERVAL = 500;

    // static data
    private static ProgressDialog sProg = null;
    private static Handler sHandler;
    private static MessageData sSendMsg = new MessageData();
    private static SlingerIdentity sSenderKey;
    private static RecipientRow sRecip;
    private static String sWebError = null;
    private static String sProgressMsg = null;
    private static String sEditPassPhrase = null;
    private static boolean sBackupExists = false;
    private static boolean sRestoring = false;
    private static boolean sRestoreRequested = false;
    private static boolean sRestoreReported = false;
    private static CryptoMsgProvider sKeyData;
    private static boolean sAppOpened = false;
    private static ContactStruct sSecureIntro = null;
    private static Uri sTempCameraFileUri;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;

    private enum Tabs {
        COMPOSE, MESSAGE, KEYSLING
    }

    private BroadcastReceiver mPushRegReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            endProgress(RECOVERY_PUSHREG);

            String error = intent.getStringExtra(extra.ERROR);
            if (error != null) {
                if (error.equals(C2DMBaseReceiver.ERRREG_SERVICE_NOT_AVAILABLE)) {
                    long backoff = ConfigData.loadPrefPusgRegBackoff(getApplicationContext());
                    showProgressUpdate(String.format(
                            getString(R.string.error_C2DMRegServiceNotAvailable),
                            backoff / 2 / 1000));
                    setProgressCancelHandler();
                    return;
                } else if (error.equals(C2DMBaseReceiver.ERRREG_ACCOUNT_MISSING)) {
                    showErrorExit(R.string.error_C2DMRegAccountMissing);
                    return;
                } else if (error.equals(C2DMBaseReceiver.ERRREG_INVALID_SENDER)) {
                    showErrorExit(R.string.error_C2DMRegInvalidSender);
                    return;
                } else if (error.equals(C2DMBaseReceiver.ERRREG_AUTHENTICATION_FAILED)) {
                    showErrorExit(R.string.error_C2DMRegAuthenticationFailed);
                    return;
                } else if (error.equals(C2DMBaseReceiver.ERRREG_TOO_MANY_REGISTRATIONS)) {
                    showErrorExit(R.string.error_C2DMRegTooManyRegistrations);
                    return;
                } else if (error.equals(C2DMBaseReceiver.ERRREG_PHONE_REGISTRATION_ERROR)) {
                    showErrorExit(R.string.error_C2DMRegPhoneRegistrationError);
                    return;
                } else {
                    // Unexpected registration errors.
                    showErrorExit(error);
                    return;
                }
            }

            if (sSenderKey == null)
                sSenderKey = new SlingerIdentity();
            sSenderKey.setToken(ConfigData.loadPrefPushRegistrationId(getApplicationContext()));

            restart();
        }
    };

    private BroadcastReceiver mMsgUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            // if message window in view, update messages immediately...
            setTab(Tabs.MESSAGE);

            // update current message list if in view...
            final int position = getSupportActionBar().getSelectedNavigationIndex();
            if (position == Tabs.MESSAGE.ordinal()) {
                if (mTabsAdapter != null) {
                    MessagesFragment mf = (MessagesFragment) mTabsAdapter
                            .findFragmentByPosition(Tabs.MESSAGE.ordinal());
                    if (mf != null) {
                        mf.updateValues(intent.getExtras());
                    }
                }
            }
        }
    };

    private boolean requestRestore(Context ctx) {
        BackupManager bm = new BackupManager(ctx);
        RestoreObserver restoreObserver = new RestoreObserver() {

            @Override
            public void restoreStarting(int numPackages) {
                sRestoring = true;
                showProgress(getString(R.string.prog_SearchingForBackup));
            }

            @Override
            public void restoreFinished(int error) {
                sBackupExists = (error == 0);
                sRestoring = false;
                endProgress(RECOVERY_BACKUP_RESTORE);
            }
        };
        try {
            int res = bm.requestRestore(restoreObserver);
            return res == 0 ? true : false;
        } catch (NullPointerException e) {
            // catch failure of RestoreSession object to manage itself in the
            // following:
            // android.app.backup.RestoreSession.endRestoreSession(RestoreSession.java:162)
            // android.app.backup.BackupManager.requestRestore(BackupManager.java:154)
            return false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // handle caller send action once send only
        // show send screen once only
        processIntent(intent);
    }

    public void processIntent(Intent intent) {
        String action = intent.getAction();

        if (ConfigData.Intent.ACTION_MESSAGENOTIFY.equals(action)) {
            // clicked on new message notifications window, show messages
            // collapse messages to threads when looking for new messages
            MessagesFragment.setRecip(null);
            setTab(Tabs.MESSAGE);
            restart();

        } else if (ConfigData.Intent.ACTION_BACKUPNOTIFY.equals(action)) {
            // clicked on backup reminder notifications window, show reminder
            // query
            showBackupQuery();
            restart();

        } else if (ConfigData.Intent.ACTION_SLINGKEYSNOTIFY.equals(action)) {
            // clicked on exchange reminder, show exchange tab
            setTab(Tabs.KEYSLING);
            restart();

        } else if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // clicked share externally, load file, show compose
            if (handleSendToAction()) {
                setTab(Tabs.COMPOSE);
            }
            restart();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (savedInstanceState != null) {
            setTab(Tabs.values()[savedInstanceState.getInt(extra.RECOVERY_TAB)]);
            if (!TextUtils.isEmpty(sProgressMsg)) {
                int newValue = savedInstanceState.getInt(extra.PCT);
                int maxValue = savedInstanceState.getInt(extra.MAX);
                if (maxValue > 0) {
                    showProgress(sProgressMsg, maxValue, newValue);
                } else {
                    showProgress(sProgressMsg);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_SafeSlinger);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setTitle(R.string.app_name);
        if (!SSUtil.isGoogleAccountPresent(getApplicationContext())) {
            bar.setSubtitle(String.format("(%s)", getString(R.string.label_DeviceInSendOnlyMode)));
        }

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, bar, mViewPager);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.menu_TagComposeMessage),
                ComposeFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.menu_TagListMessages),
                MessagesFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.menu_TagExchange), SlingerFragment.class,
                null);

        if (savedInstanceState != null) {
            setTab(Tabs.values()[savedInstanceState.getInt(extra.RECOVERY_TAB)]);
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        restoreView();

        // prepare for push registration...
        registerReceiver(mPushRegReceiver, new IntentFilter(C2DMReceiver.PUSH_REGISTERED));
        registerReceiver(mMsgUpdateReceiver, new IntentFilter(
                ConfigData.Intent.ACTION_MESSAGEUPDATE));

        SafeSlinger.startCacheService(this);

        if (savedInstanceState == null) {
            // init app launch once all time
            initOnAppLaunchOnly();

            // init on reload once all time
            if (!initOnReload()) {
                return;
            }
        }
    }

    public static class TabsAdapter extends FragmentPagerAdapter implements
            ViewPager.OnPageChangeListener, ActionBar.TabListener {
        private final SherlockFragmentActivity mAct;
        private final ActionBar mBar;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        static final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter(SherlockFragmentActivity activity, ActionBar bar, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mAct = activity;
            mBar = bar;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<? extends Fragment> clss, Bundle args) {
            TabInfo info = new TabInfo(clss, args);
            tab.setTag(info);
            tab.setTabListener(this);
            mTabs.add(info);
            mBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mAct, info.clss.getName(), info.args);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            Object tag = tab.getTag();
            for (int i = 0; i < mTabs.size(); i++) {
                if (mTabs.get(i) == tag) {
                    mViewPager.setCurrentItem(i);

                    // refresh all that matter
                    switch (Tabs.values()[tab.getPosition()]) {
                        case COMPOSE:
                            ComposeFragment cf = (ComposeFragment) findFragmentByPosition(Tabs.COMPOSE
                                    .ordinal());
                            if (cf != null) {
                                cf.updateValues(getComposeArgs());
                            }
                            break;
                        case MESSAGE:
                            MessagesFragment mf = (MessagesFragment) findFragmentByPosition(Tabs.MESSAGE
                                    .ordinal());
                            if (mf != null) {
                                mf.updateValues(getMessagesArgs());
                            }
                            break;
                        case KEYSLING:
                            SlingerFragment sf = (SlingerFragment) findFragmentByPosition(Tabs.KEYSLING
                                    .ordinal());
                            if (sf != null) {
                                sf.updateValues(getSlingerArgs());
                            }
                            break;
                    }
                }
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // refresh all that matter
            switch (Tabs.values()[tab.getPosition()]) {
                case COMPOSE:
                    ComposeFragment cf = (ComposeFragment) findFragmentByPosition(Tabs.COMPOSE
                            .ordinal());
                    if (cf != null) {
                        cf.updateValues(getComposeArgs());
                    }
                    break;
                case MESSAGE:
                    MessagesFragment mf = (MessagesFragment) findFragmentByPosition(Tabs.MESSAGE
                            .ordinal());
                    if (mf != null) {
                        mf.updateValues(getMessagesArgs());
                    }
                    break;
                case KEYSLING:
                    SlingerFragment sf = (SlingerFragment) findFragmentByPosition(Tabs.KEYSLING
                            .ordinal());
                    if (sf != null) {
                        sf.updateValues(getSlingerArgs());
                    }
                    break;
            }
        }

        public Fragment findFragmentByPosition(int position) {
            return mAct.getSupportFragmentManager().findFragmentByTag(
                    "android:switcher:" + mViewPager.getId() + ":" + getItemId(position));
        }
    }

    private static Bundle getComposeArgs() {
        Bundle args = new Bundle();
        if (sRecip != null) {
            args.putLong(extra.RECIPIENT_ROW_ID, sRecip.getRowId());
        } else {
            args.remove(extra.RECIPIENT_ROW_ID);
        }
        args.putString(extra.FILE_PATH, sSendMsg.getFileName());
        args.putInt(extra.PUSH_FILE_SIZE, sSendMsg.getFileSize());
        args.putString(extra.TEXT_MESSAGE, sSendMsg.getText());
        if (!TextUtils.isEmpty(sSendMsg.getFileType()) && sSendMsg.getFileType().contains("image")) {
            args.putByteArray(extra.THUMBNAIL, SSUtil.makeThumbnail(SafeSlinger.getApplication()
                    .getApplicationContext(), sSendMsg.getFileData()));
        } else {
            args.remove(extra.THUMBNAIL);
        }
        return args;
    }

    private static Bundle getMessagesArgs() {
        return null;
    }

    protected void restoreView() {
        // all tabs with data...
        if (mTabsAdapter != null) {
            ComposeFragment cf = (ComposeFragment) mTabsAdapter.findFragmentByPosition(Tabs.COMPOSE
                    .ordinal());
            if (cf != null) {
                cf.updateValues(getComposeArgs());
            }
            MessagesFragment mf = (MessagesFragment) mTabsAdapter
                    .findFragmentByPosition(Tabs.MESSAGE.ordinal());
            if (mf != null) {
                mf.updateValues(getMessagesArgs());
            }
            SlingerFragment sf = (SlingerFragment) mTabsAdapter
                    .findFragmentByPosition(Tabs.KEYSLING.ordinal());
            if (sf != null) {
                sf.updateValues(getSlingerArgs());
            }
        }
    }

    private static Bundle getSlingerArgs() {
        Bundle args = new Bundle();
        String contactLookupKey = ConfigData.loadPrefContactLookupKey(SafeSlinger.getApplication()
                .getApplicationContext());

        // valid key and push token is required
        if (TextUtils.isEmpty(SlingerIdentity.sidPush2DBPush(sSenderKey))) {
            return args;
        } else if (TextUtils.isEmpty(SlingerIdentity.sidKey2DBKey(sSenderKey))) {
            return args;
        }

        ArrayList<ImppValue> impps = new ArrayList<ImppValue>();
        impps.add(new ImppValue(ConfigData.APP_KEY_PUSHTOKEN, SSUtil.finalEncode(SlingerIdentity
                .sidPush2DBPush(sSenderKey).getBytes())));
        impps.add(new ImppValue(ConfigData.APP_KEY_PUBKEY, SSUtil.finalEncode(SlingerIdentity
                .sidKey2DBKey(sSenderKey).getBytes())));
        args.putAll(writeSingleExportExchangeArgs(new ContactImpp(contactLookupKey, impps)));

        return args;
    }

    private void showExchange(byte[] userData) {
        Intent intent = new Intent(HomeActivity.this, ControllerActivity.class);
        intent.putExtra(extra.USER_DATA, userData);
        startActivityForResult(intent, VIEW_EXCHANGE_ID);
    }

    private void showSave(Bundle args) {
        Intent intent = new Intent(HomeActivity.this, SaveActivity.class);
        intent.replaceExtras(args);
        startActivityForResult(intent, VIEW_SAVE_ID);
    }

    public void doUpgradeDatabaseInPlace() throws IllegalArgumentException {
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(getApplicationContext());
        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());

        // only do this once for version 5, if done again will break recip db
        if (ConfigData.loadPrefCurrentRecipientDBVer(getApplicationContext()) < 6) {
            Cursor c = dbRecipient.fetchAllRecipientsUpgradeTo6();
            if (c != null) {
                while (c.moveToNext()) {
                    long rowId = c.getLong(c.getColumnIndexOrThrow(RecipientDbAdapter.KEY_ROWID));
                    long mykeyid_long = c.getLong(c
                            .getColumnIndexOrThrow(RecipientDbAdapter.KEY_MYKEYIDLONG));
                    long keyid_long = c.getLong(c
                            .getColumnIndexOrThrow(RecipientDbAdapter.KEY_KEYIDLONG));
                    dbRecipient.updateRecipientKeyIds2String(rowId, mykeyid_long, keyid_long);
                }
                c.close();
                ConfigData.savePrefCurrentRecipientDBVer(getApplicationContext(),
                        RecipientDatabaseHelper.DATABASE_VERSION);
            }
        }

        if (ConfigData.loadPrefCurrentMessageDBVer(getApplicationContext()) < 6) {
            Cursor c = dbMessage.fetchAllMessagesUpgradeTo6();
            if (c != null) {
                while (c.moveToNext()) {
                    long rowId = c.getLong(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_ROWID));
                    long keyid_long = c.getLong(c
                            .getColumnIndexOrThrow(MessageDbAdapter.KEY_KEYIDLONG));
                    dbMessage.updateMessageKeyId2String(rowId, keyid_long);
                }
                c.close();
                ConfigData.savePrefCurrentMessageDBVer(getApplicationContext(),
                        MessageDatabaseHelper.DATABASE_VERSION);
            }
        }

        // look for old messages without key ids read out
        if (dbMessage.getVersion() >= 6) {
            Cursor cm = dbMessage.fetchAllMessagesByThread(null);
            if (cm != null) {
                while (cm.moveToNext()) {
                    MessageRow messageRow = new MessageRow(cm);
                    byte[] encMsg = messageRow.getEncBody();
                    if (encMsg != null) {
                        CryptoMsgProvider tool = CryptoMsgProvider.createInstance(SafeSlinger
                                .getApplication().isLoggable());
                        String keyid = null;
                        try {
                            keyid = tool.ExtractKeyIDfromPacket(encMsg);
                        } catch (CryptoMsgPacketSizeException e) {
                            e.printStackTrace();
                        }
                        if (!TextUtils.isEmpty(keyid)) {
                            dbMessage.updateMessageKeyId(messageRow.getRowId(), keyid);
                        }
                    }
                }
                cm.close();
            }
        }

    }

    private void initOnAppLaunchOnly() {

        // notify user if there are connectivity issues...
        if (!SafeSlinger.getApplication().isOnline()) {
            showNote(R.string.error_CorrectYourInternetConnection);
        }

        processIntent(getIntent());
    }

    private boolean initOnReload() {

        // check early for key and attempt restore if not found...
        if (sRestoring) {
            return false;
        }
        if (!sRestoreRequested
                && !SSUtil.fileExists(getApplicationContext(),
                        CryptTools.getCurrentKeyFile(getApplicationContext()))) {
            try {
                sRestoreRequested = true;
                sBackupExists = requestRestore(getApplicationContext());
                if (sBackupExists) {
                    return false;
                }
            } catch (SecurityException e) {
                // some devices do not grant permission for restore operation...
                sRestoreReported = true;
                showNote(R.string.error_BackupNotSupportedDevice);
            }
        }
        if (sRestoreRequested && !sRestoreReported) {
            // even though backup has reported success, it may not have
            // worked...
            sRestoreReported = true;
            if (SSUtil.fileExists(getApplicationContext(),
                    CryptTools.getCurrentKeyFile(getApplicationContext()))) {
                showNote(R.string.state_BackupRestored);
            }
        }

        // show eula?
        if (!ConfigData.loadPrefEulaAccepted(getApplicationContext())) {
            if (!isFinishing()) {
                removeDialog(DIALOG_LICENSE_CONFIRM);
                showDialog(DIALOG_LICENSE_CONFIRM);
            }
            return false;
        }

        // we passed backup restore, now we can load databases...
        doUpgradeDatabaseInPlace();

        // contact
        String contactName = ConfigData.loadPrefContactName(getApplicationContext());
        if (TextUtils.isEmpty(contactName)) {
            showFindContact();
            return false;
        }

        // we need a real persons name
        if (!ConfigData.isNameValid(contactName, getApplicationContext())) {
            showNote(R.string.error_InvalidContactName);
            showFindContact();
            return false;
        }

        boolean dateChanged = SSUtil.isDayChanged(ConfigData.loadPrefContactDBLastScan(this));
        if (!sAppOpened || dateChanged) {
            sAppOpened = true;
            ConfigData.savePrefThisVersionOpened(this);
            runThreadBackgroundSyncUpdates();
        }

        // init local struct...
        int notify = SSUtil.getLocalNotification(getApplicationContext());
        String token = ConfigData.loadPrefPushRegistrationId(getApplicationContext());

        // Determine if user wants to have a valid push token or not
        if (notify == ConfigData.NOTIFY_NOPUSH && TextUtils.isEmpty(token)) {
            showQuestion(getString(R.string.ask_GoogleAccountToReceiveMsgs), REQUEST_QRECEIVE_MGS);
            return false;
        }

        boolean savePub = false;
        // if push token bad....
        // ...request a push token... (restart)
        if (sSenderKey == null) {
            sSenderKey = new SlingerIdentity();
        }
        sSenderKey.setNotification(notify);
        sSenderKey.setToken(token);
        // ensure a good push token is available
        if (notify != ConfigData.NOTIFY_NOPUSH) {
            if (TextUtils.isEmpty(sSenderKey.getToken()) && notify == ConfigData.NOTIFY_ANDROIDC2DM) {
                // ensure that user has registered with push service...
                runThreadGetPushReg();
                return false;
            }
        }

        // look for proper key
        boolean hasSecretKey = CryptTools.existsSecretKey(getApplicationContext());
        if (!hasSecretKey) {
            // key not found, try loading older version
            hasSecretKey = CryptToolsLegacy.existsSecretKeyOld(getApplicationContext());
        }

        // if no pass exists...
        // ...request pass entry... (restart)
        // now with contact, enter pass phrase and check
        if (!loadCurrentPassPhrase()) {
            if (!SafeSlinger.isPassphraseOpen()) {
                if (!hasSecretKey) {
                    showPassPhrase(true, false); // new
                } else {
                    showPassPhrase(false, false); // normal
                }
            }
            return false;
        }

        // ensure that a valid pub key is installed...
        CryptoMsgPrivateData secret = null;
        try {
            String pass = SafeSlinger.getCachedPassPhrase(ConfigData.loadPrefKeyIdString(this
                    .getApplicationContext()));
            secret = CryptTools.getSecretKey(getApplicationContext(), pass);
            if (secret != null) {
                sSenderKey.setPublicKey(secret.getSafeSlingerString());
            } else {
                return false;
            }
        } catch (IOException e) {
            // key not found
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // unable to deserialize same key format
            e.printStackTrace();
        } catch (CryptoMsgException e) {
            // key formatted incorrectly
            e.printStackTrace();
        } finally {
            if (secret == null) {
                return false;
            }
        }

        // update key details
        try {
            // public must be in contact, or we must store it
            String pubKey = sSenderKey.getPublicKey();
            if (pubKey == null) {
                savePub = true;
            } else {
                CryptoMsgProvider tool = CryptoMsgProvider.createInstance(SafeSlinger
                        .getApplication().isLoggable());
                String keyIdFromConfig = ""
                        + ConfigData.loadPrefKeyIdString(getApplicationContext());
                String keyIdFromContactsDB = "" + tool.ExtractKeyIDfromSafeSlingerString(pubKey);
                String keyIdFromSecretFile = "" + secret.getKeyId();

                if (keyIdFromSecretFile.compareTo(keyIdFromContactsDB) != 0) {
                    savePub = true;
                } else if (keyIdFromSecretFile.compareTo(keyIdFromConfig) != 0) {
                    ConfigData.savePrefKeyIdString(getApplicationContext(), keyIdFromSecretFile);
                    ConfigData.savePrefKeyDate(getApplicationContext(),
                            tool.ExtractDateTimefromSafeSlingerString(pubKey));
                }
            }

            if (savePub) {
                sSenderKey.setPublicKey(secret.getSafeSlingerString());
            }
        } catch (CryptoMsgPeerKeyFormatException e) {
            showErrorExit(R.string.error_MessageInvalidPeerKeyFormat);
            return false;
        }

        // pass is good, determine what view to see...
        sHandler = new Handler();
        sHandler.removeCallbacks(updateMainView);
        sHandler.post(updateMainView);

        return true;
    }

    private Runnable updateMainView = new Runnable() {

        @Override
        public void run() {
            restoreView();
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save
        if (sTempCameraFileUri != null) {
            outState.putString(extra.FILE_PATH, sTempCameraFileUri.getPath());
        }
        final int position = getSupportActionBar().getSelectedNavigationIndex();
        outState.putInt(extra.RECOVERY_TAB, position);
        outState.putAll(getComposeArgs());
        outState.putAll(getSlingerArgs());

        if (sProg != null) {
            if (sProg.isShowing()) {
                outState.putInt(extra.PCT, sProg.isIndeterminate() ? 0 : sProg.getProgress());
                outState.putInt(extra.MAX, sProg.isIndeterminate() ? 0 : sProg.getMax());
                outState.putString(extra.RESID_MSG, sProgressMsg);
            } else {
                outState.remove(extra.PCT);
                outState.remove(extra.MAX);
                outState.remove(extra.RESID_MSG);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {

        int showAsActionIfRoom = com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_IF_ROOM;
        menu.add(0, MENU_SENDINTRO, 0, R.string.title_SecureIntroduction)
                .setIcon(R.drawable.ic_action_secintro).setShowAsAction(showAsActionIfRoom);

        // do not crowd default action bar with these items, leave room for
        // tabs...
        menu.add(0, MENU_REFERENCE, 0, R.string.menu_Help).setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_SENDINTRO, 0, R.string.title_SecureIntroduction).setIcon(
                R.drawable.ic_action_secintro);
        menu.add(0, MENU_SENDAPP, 0, R.string.menu_SelectShareApp).setIcon(
                android.R.drawable.ic_menu_share);
        menu.add(0, MENU_FEEDBACK, 0, R.string.menu_sendFeedback).setIcon(
                android.R.drawable.ic_menu_send);
        menu.add(0, MENU_LOGOUT, 0, R.string.menu_Logout).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
        menu.add(0, MENU_SETTINGS, 0, R.string.menu_Settings).setIcon(
                android.R.drawable.ic_menu_preferences);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case MENU_LOGOUT:
                // remove cached pass
                SafeSlinger.removeCachedPassPhrase(ConfigData.loadPrefKeyIdString(this
                        .getApplicationContext()));
                SafeSlinger.startCacheService(HomeActivity.this);
                showPassPhrase(false, false);
                return true;
            case MENU_SENDAPP:
                showSendApplication();
                return true;
            case MENU_SETTINGS:
                showSettings();
                return true;
            case MENU_SENDINTRO:
                showSendIntroduction();
                return true;
            case MENU_FEEDBACK:
                SafeSlinger.getApplication().showFeedbackEmail(HomeActivity.this);
                return true;
            case MENU_REFERENCE:
                showReference();
                return true;
        }
        return false;
    }

    private void doGenerateNewKey() {
        // key generation...
        runThreadCreateKey();
    }

    private void doCreateKeyEnd() {
        if (sWebError != null) {
            showErrorExit(sWebError);
            return;
        }

        try {
            if (!sKeyData.isGenerated()) {
                throw new CryptoMsgException(getString(R.string.error_couldNotExtractPrivateKey));
            }

            CryptoMsgPrivateData mine = new CryptoMsgPrivateData(sKeyData);

            // save public portion in address book...
            if (sSenderKey == null)
                sSenderKey = new SlingerIdentity();
            sSenderKey.setPublicKey(mine.getSafeSlingerString());

            ConfigData.savePrefKeyIdString(getApplicationContext(), mine.getKeyId());
            ConfigData.savePrefKeyDate(getApplicationContext(), mine.getGenDate());

            // save private portion in secret key storage...
            CryptTools.putSecretKey(getApplicationContext(), mine, sEditPassPhrase);

            // update cache to avoid entering twice...
            SafeSlinger.setCachedPassPhrase(sKeyData.GetSelfKeyid(), sEditPassPhrase);
            SafeSlinger.startCacheService(HomeActivity.this);

            // now that we have new key id, use it when updating contacts...
            runThreadBackgroundSyncUpdates();

            restart();
        } catch (IOException e) {
            showErrorExit(e);
        } catch (CryptoMsgException e) {
            showErrorExit(e);
        } catch (CryptoMsgNonExistingKeyException e) {
            showErrorExit(e);
        }
    }

    private void runThreadCreateKey() {
        sWebError = null;
        showProgress(getString(R.string.prog_GeneratingKey));
        setProgressCancelHandler();

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    sKeyData = CryptoMsgProvider.createInstance(SafeSlinger.getApplication()
                            .isLoggable());
                    sKeyData.GenKeyPairs();
                    if (!sKeyData.isGenerated()) {
                        throw new CryptoMsgException(
                                getString(R.string.error_couldNotExtractPrivateKey));
                    }
                } catch (OutOfMemoryError e) {
                    sWebError = getString(R.string.error_OutOfMemoryError);
                } catch (InvalidParameterException e) {
                    sWebError = e.getLocalizedMessage();
                } catch (CryptoMsgException e) {
                    sWebError = e.getLocalizedMessage();
                }
                endProgress(RECOVERY_CREATEKEY);
            }
        };
        t.start();
    }

    private void doSendFileStart(RecipientRow recip, MessageData sendMsg) {

        // must have either file or text to send
        if (TextUtils.isEmpty(sendMsg.getText()) && TextUtils.isEmpty(sendMsg.getFileName())) {
            showNote(R.string.error_selectDataToSend);
            setTab(Tabs.COMPOSE);
            restart();
            return;
        }

        if (TextUtils.isEmpty(sendMsg.getFileName())) {
            sendMsg.removeFile();
        } else {
            if (sendMsg.getFileData() == null || sendMsg.getFileSize() == 0) {
                showNote(R.string.error_InvalidMsg);
                restart();
                return;
            }
        }

        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
        // manage this draft...
        if (sendMsg.getRowId() < 0) {
            // create draft (need at least recipient(file) or text chosen...
            if (!TextUtils.isEmpty(sendMsg.getText()) || !TextUtils.isEmpty(sendMsg.getFileName())) {
                long rowId = dbMessage.createDraftMessage(recip, sendMsg);
                sendMsg.setRowId(rowId);
                if (rowId < 0) {
                    showNote(R.string.error_UnableToSaveMessageInDB);
                    restart();
                    return;
                }
            }
        } else {
            Cursor c = dbMessage.fetchMessageSmall(sendMsg.getRowId());
            if (c != null) {
                MessageRow msg = new MessageRow(c);
                c.close();
                if (msg.getMessageAction() != MsgAction.MSG_EDIT) {
                    return;
                }
            }

            // update draft
            if (!dbMessage.updateDraftMessage(sendMsg.getRowId(), recip, sendMsg)) {
                showNote(R.string.error_UnableToUpdateMessageInDB);
                restart();
                return;
            }
        }

        String pass = SafeSlinger.getCachedPassPhrase(ConfigData
                .loadPrefKeyIdString(getApplicationContext()));
        if (TextUtils.isEmpty(pass)) {
            if (!SafeSlinger.isPassphraseOpen()) {
                showPassPhrase(false, false);
            }
            return;
        }
        if (recip == null) {
            showNote(R.string.error_InvalidRecipient);
            restart();
            return;
        }
        if (!SafeSlinger.getApplication().isOnline()) {
            showNote(R.string.error_CorrectYourInternetConnection);
            restart();
            return;
        }

        // update status as queued for transmission
        if (!dbMessage.updateEnqueuedMessage(sendMsg.getRowId())) {
            showNote(R.string.error_UnableToUpdateMessageInDB);
            restart();
            return;
        }
        // confirm msg is queued
        Cursor c = dbMessage.fetchMessageSmall(sendMsg.getRowId());
        if (c != null) {
            MessageRow queued = new MessageRow(c);
            c.close();
            if (queued.getStatus() != MessageDbAdapter.MESSAGE_STATUS_QUEUED) {
                showNote(R.string.error_UnableToUpdateMessageInDB);
                restart();
                return;
            }
        }

        // switch to message tab
        setTab(Tabs.MESSAGE);
        restart();

        // start background task to send
        SendFileTask task = new SendFileTask();
        task.execute(new MessageTransport(recip, sendMsg));

        // attempt to update messages if in view...
        Intent sendIntent = new Intent(ConfigData.Intent.ACTION_MESSAGEUPDATE);
        sendIntent.putExtra(extra.MESSAGE_ROW_ID, sendMsg.getRowId());
        sendIntent.putExtra(extra.RECIPIENT_ROW_ID, recip.getRowId());
        getApplicationContext().sendBroadcast(sendIntent);
    }

    private void setTab(Tabs tab) {
        try {
            final ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setSelectedNavigationItem(tab.ordinal());
            }
        } catch (Exception e) {
            // since this may be called from another thread,
            // catch, but not critically...
        }
    }

    private void endProgress(final int recover) {

        if (sProg != null) {
            sProg.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    switch (recover) {
                        default:
                            restart();
                            // nothing to do...
                            break;
                        case RECOVERY_CREATEKEY:
                            doCreateKeyEnd();
                            break;
                        case RECOVERY_PUSHREG:
                            // we can only wait for an error, or success from
                            // receiver...
                            break;
                        case RECOVERY_EXCHANGEIMPORT:
                            // updated databases, restart to use...
                            runThreadBackgroundSyncUpdates();
                            restart();
                            break;
                        case RECOVERY_BACKUP_RESTORE:
                            // backup complete, read data...
                            restart();
                            break;
                    }
                }
            });
            sProg.dismiss();
            sProg = null;
            sProgressMsg = null;
        }

    }

    private boolean handleSendToAction() {
        Intent intent = getIntent();
        String action = intent.getAction();
        long filesize = 0;

        if (action != null) {
            if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {

                showErrorExit(R.string.error_MultipleSendNotSupported);
                return false;

            } else if (action.equals(Intent.ACTION_SEND)) {

                sSendMsg.setMsgHash(null);

                String type = intent.getType();
                Uri stream = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                CharSequence extra_text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);

                // if this is from a share menu
                try {
                    // Get resource path from intent caller
                    if (Intent.ACTION_SEND.equals(action)) {
                        if (stream != null) {
                            filesize = getOutStreamSizeAndData(stream, type);
                        } else if (!TextUtils.isEmpty(extra_text)) {
                            filesize = extra_text.length();
                            if (filesize <= ConfigData.MAX_TEXTMESSAGE) {
                                sSendMsg.removeFile();
                                sSendMsg.setText(extra_text.toString());
                            } else {
                                sSendMsg.setFileType("text/plain");
                                sSendMsg.setFileData(extra_text.toString().getBytes());
                                SimpleDateFormat sdf = new SimpleDateFormat(
                                        ConfigData.DATETIME_FILENAME, Locale.US);
                                sSendMsg.setFileName(sdf.format(new Date()) + ".txt");
                                sSendMsg.removeText();
                            }
                        }
                    }

                    if (filesize <= 0) {
                        showErrorExit(R.string.error_CannotSendEmptyFile);
                        return false;
                    }
                    if (filesize > ConfigData.MAX_FILEBYTES) {
                        showErrorExit(String.format(getString(R.string.error_CannotSendFilesOver),
                                ConfigData.MAX_FILEBYTES));
                        return false;
                    }
                } catch (IOException e) {
                    showErrorExit(e);
                    return false;
                } catch (OutOfMemoryError e) {
                    showErrorExit(R.string.error_OutOfMemoryError);
                    return false;
                }
            }
        }
        return true;
    }

    private long getOutStreamSizeAndData(Uri uri, String contentType) throws IOException {

        String name = null;
        try {
            Cursor c = getContentResolver().query(uri, new String[] {
                MediaColumns.DISPLAY_NAME
            }, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    name = c.getString(c.getColumnIndex(MediaColumns.DISPLAY_NAME));
                }
                c.close();
            }
        } catch (IllegalArgumentException e) {
            // column may not exist
        }

        long size = -1;
        try {
            Cursor c = getContentResolver().query(uri, new String[] {
                MediaColumns.SIZE
            }, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    size = c.getInt(c.getColumnIndex(MediaColumns.SIZE));
                }
                c.close();
            }
        } catch (IllegalArgumentException e) {
            // column may not exist
        }

        String data = null;
        try {
            Cursor c = getContentResolver().query(uri, new String[] {
                MediaColumns.DATA
            }, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    data = c.getString(c.getColumnIndex(MediaColumns.DATA));
                }
                c.close();
            }
        } catch (IllegalArgumentException e) {
            // column may not exist
        }

        if (name == null) {
            name = uri.getLastPathSegment();
        }

        File f = null;
        if (size <= 0) {
            String uriString = uri.toString();
            if (uriString.startsWith("file://")) {
                MyLog.v(TAG, uriString.substring("file://".length()));
                f = new File(uriString.substring("file://".length()));
                size = f.length();
            } else {
                MyLog.v(TAG, "not a file: " + uriString);
            }
        }

        ContentResolver cr = getContentResolver();
        InputStream is;
        // read file bytes
        try {
            is = cr.openInputStream(uri);
        } catch (FileNotFoundException e) {
            if (!TextUtils.isEmpty(data)) {
                is = new FileInputStream(data);
            } else {
                return -1; // unable to load file at all
            }
        }

        if ((contentType != null) && (contentType.indexOf('*') != -1)) {
            contentType = getContentResolver().getType(uri);
        }

        if (contentType == null) {
            contentType = URLConnection.guessContentTypeFromStream(is);
            if (contentType == null) {
                String extension = SSUtil.getFileExtensionOnly(name);
                contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (contentType == null) {
                    contentType = "*/*";
                }
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[4096];
        while (is.read(buf) > -1) {
            baos.write(buf);
        }
        baos.flush();

        sSendMsg.setFileData(baos.toByteArray());
        sSendMsg.setFileType(contentType);
        sSendMsg.setFileName(name);
        if (f != null && f.exists()) {
            sSendMsg.setFileDir(f.getAbsolutePath());
        } else if (!TextUtils.isEmpty(data)) {
            sSendMsg.setFileDir(new File(data).getAbsolutePath());
        }
        return sSendMsg.getFileSize();
    }

    @Override
    public void onComposeResultListener(Bundle data) {
        int resultCode = data.getInt(extra.RESULT_CODE);
        String text = null;
        if (data != null) {
            text = data.getString(extra.TEXT_MESSAGE);
            long rowIdRecipient = data.getLong(extra.RECIPIENT_ROW_ID, -1);
            if (rowIdRecipient > -1) {
                RecipientDbAdapter dbRecipient = RecipientDbAdapter
                        .openInstance(getApplicationContext());
                Cursor c = dbRecipient.fetchRecipient(rowIdRecipient);
                if (c != null) {
                    sRecip = new RecipientRow(c);
                    c.close();
                } else {
                    showNote(R.string.error_InvalidRecipient);
                    return;
                }

            }
        }

        switch (resultCode) {
            case ComposeFragment.RESULT_SAVE:
                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());

                sSendMsg.setText(text);
                if (sSendMsg.getRowId() < 0) {
                    // create draft (need at least recipient(file) or text
                    // chosen...
                    if (!TextUtils.isEmpty(sSendMsg.getText())
                            || !TextUtils.isEmpty(sSendMsg.getFileName())) {
                        long rowId = dbMessage.createDraftMessage(sRecip, sSendMsg);
                        sSendMsg.setRowId(rowId);
                        if (rowId < 0) {
                            showNote(R.string.error_UnableToSaveMessageInDB);
                        } else {
                            showNote(R.string.state_MessageSavedAsDraft);
                        }
                    }
                } else {
                    Cursor c = dbMessage.fetchMessageSmall(sSendMsg.getRowId());
                    if (c != null) {
                        MessageRow msg = new MessageRow(c);
                        c.close();
                        if (msg.getStatus() != MessageDbAdapter.MESSAGE_STATUS_DRAFT) {
                            break;
                        }
                    }

                    if (!TextUtils.isEmpty(sSendMsg.getText())
                            || !TextUtils.isEmpty(sSendMsg.getFileName())) {
                        // update draft
                        if (!dbMessage.updateDraftMessage(sSendMsg.getRowId(), sRecip, sSendMsg)) {
                            showNote(R.string.error_UnableToUpdateMessageInDB);
                        }
                    } else {
                        // message is empty, we should remove from database...
                        if (!dbMessage.deleteMessage(sSendMsg.getRowId())) {
                            showNote(String.format(getString(R.string.state_MessagesDeleted), 0));
                        }
                        sSendMsg = new MessageData();
                    }
                }

                break;
            case ComposeFragment.RESULT_SEND:
                sSendMsg.setText(text);
                // user wants to post the file and notify recipient
                if (sRecip == null) {
                    showNote(R.string.error_InvalidRecipient);
                    restart();
                    break;
                }
                if (sRecip.getNotify() == ConfigData.NOTIFY_NOPUSH) {
                    showNote(R.string.error_InvalidRecipient);
                    restart();
                    break;
                }
                doSendFileStart(sRecip, sSendMsg);
                break;
            case ComposeFragment.RESULT_RESTART:
                restart();
                break;
            case ComposeFragment.RESULT_USEROPTIONS:
                showChangeSenderOptions();
                break;
            case ComposeFragment.RESULT_FILESEL:
                // user wants to pick a file to send
                showFileAttach();
                break;
            case ComposeFragment.RESULT_RECIPSEL:
                // user wants to pick a recipient
                showRecipientSelect();
                break;
            case ComposeFragment.RESULT_FILEREMOVE:
                // user wants to remove file
                sSendMsg.removeFile();
                setTab(Tabs.COMPOSE);
                restart();
                break;
        }
    }

    @Override
    public void onMessageResultListener(Bundle data) {
        int resultCode = data.getInt(extra.RESULT_CODE);
        String text = null;
        RecipientRow recip = null;
        long rowIdMessage = -1;
        if (data != null) {
            text = data.getString(extra.TEXT_MESSAGE);
            rowIdMessage = data.getLong(extra.MESSAGE_ROW_ID, -1);
            long rowIdRecipient = data.getLong(extra.RECIPIENT_ROW_ID, -1);
            if (rowIdRecipient > -1) {
                RecipientDbAdapter dbRecipient = RecipientDbAdapter
                        .openInstance(getApplicationContext());
                Cursor c = dbRecipient.fetchRecipient(rowIdRecipient);
                if (c != null) {
                    recip = new RecipientRow(c);
                    c.close();
                } else {
                    showNote(R.string.error_InvalidRecipient);
                    return;
                }
            }

            String pass = SafeSlinger.getCachedPassPhrase(ConfigData
                    .loadPrefKeyIdString(getApplicationContext()));
            if (TextUtils.isEmpty(pass)) {
                if (!SafeSlinger.isPassphraseOpen()) {
                    showPassPhrase(false, false);
                }
                return;
            }
        }

        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());

        switch (resultCode) {
            case MessagesFragment.RESULT_SAVE:
                MessageData saveMsg = new MessageData();
                saveMsg.setRowId(rowIdMessage);
                saveMsg.setText(text);
                if (saveMsg.getRowId() < 0) {
                    // create draft (need at least recipient(file) or text
                    // chosen...
                    if (!TextUtils.isEmpty(saveMsg.getText())
                            || !TextUtils.isEmpty(saveMsg.getFileName())) {
                        long rowId = dbMessage.createDraftMessage(recip, saveMsg);
                        saveMsg.setRowId(rowId);
                        if (rowId < 0) {
                            showNote(R.string.error_UnableToSaveMessageInDB);
                        } else {
                            showNote(R.string.state_MessageSavedAsDraft);
                        }
                    }
                } else {
                    Cursor c = dbMessage.fetchMessageSmall(saveMsg.getRowId());
                    if (c != null) {
                        MessageRow msg = new MessageRow(c);
                        c.close();
                        if (msg.getStatus() != MessageDbAdapter.MESSAGE_STATUS_DRAFT) {
                            break;
                        }
                    }

                    if (!TextUtils.isEmpty(saveMsg.getText())
                            || !TextUtils.isEmpty(saveMsg.getFileName())) {
                        // update draft
                        if (!dbMessage.updateDraftMessage(saveMsg.getRowId(), recip, saveMsg)) {
                            showNote(R.string.error_UnableToUpdateMessageInDB);
                        }
                    } else {
                        // message is empty, we should remove from database...
                        if (!dbMessage.deleteMessage(saveMsg.getRowId())) {
                            showNote(String.format(getString(R.string.state_MessagesDeleted), 0));
                        }
                        saveMsg = new MessageData();
                    }
                }
                // update local as well
                if (saveMsg.getRowId() == sSendMsg.getRowId()) {
                    sSendMsg = saveMsg;
                }
                break;
            case MessagesFragment.RESULT_SEND:
                MessageData sendMsg = new MessageData();
                sendMsg.setRowId(rowIdMessage);
                sendMsg.setText(text);
                // user wants to post the file and notify recipient
                if (recip == null) {
                    showNote(R.string.error_InvalidRecipient);
                    restart();
                    break;
                }
                if (recip.getNotify() == ConfigData.NOTIFY_NOPUSH) {
                    showNote(R.string.error_InvalidRecipient);
                    restart();
                    break;
                }
                doSendFileStart(recip, sendMsg);
                break;
            case MessagesFragment.RESULT_FWDMESSAGE:
                sSendMsg = new MessageData();
                sSendMsg.setRowId(rowIdMessage);
                Cursor cfm = dbMessage.fetchMessageSmall(sSendMsg.getRowId());
                if (cfm != null) {
                    MessageRow edit = new MessageRow(cfm);
                    cfm.close();
                    sSendMsg.setText(edit.getText());
                    try {
                        if (!TextUtils.isEmpty(edit.getFileDir())) {
                            doLoadAttachment(edit.getFileDir());
                        }
                    } catch (FileNotFoundException e) {
                        showNote(e);
                        restart();
                        break;
                    }
                }
                sRecip = null;

                // create draft
                if (!TextUtils.isEmpty(sSendMsg.getText())
                        || !TextUtils.isEmpty(sSendMsg.getFileName())) {
                    long rowId = dbMessage.createDraftMessage(sRecip, sSendMsg);
                    sSendMsg.setRowId(rowId);
                }
                setTab(Tabs.COMPOSE);
                restart();
                break;
            case MessagesFragment.RESULT_EDITMESSAGE:
                sSendMsg = new MessageData();
                sSendMsg.setRowId(rowIdMessage);
                Cursor cem = dbMessage.fetchMessageSmall(sSendMsg.getRowId());
                if (cem != null) {
                    MessageRow edit = new MessageRow(cem);
                    cem.close();
                    sRecip = recip;
                    sSendMsg.setText(edit.getText());
                    sSendMsg.setKeyId(edit.getKeyId());
                    try {
                        if (!TextUtils.isEmpty(edit.getFileDir())) {
                            doLoadAttachment(edit.getFileDir());
                        }
                    } catch (FileNotFoundException e) {
                        showNote(e);
                        restart();
                        break;
                    }
                }
                setTab(Tabs.COMPOSE);
                restart();
                break;
            case MessagesFragment.RESULT_GETMESSAGE:
                MessageData recvMsg = new MessageData();
                recvMsg.setRowId(rowIdMessage);
                recvMsg.setMsgHash(data.getString(extra.PUSH_MSG_HASH));

                if (recvMsg.getMsgHash() != null) {
                    GetMessageTask getMessageTask = new GetMessageTask();
                    getMessageTask.execute(recvMsg);
                } else {
                    showNote(R.string.error_InvalidIncomingMessage);
                    restart();
                }
                break;
            case MessagesFragment.RESULT_GETFILE:
                MessageData recvFile = new MessageData();
                recvFile.setRowId(rowIdMessage);
                recvFile.setMsgHash(data.getString(extra.PUSH_MSG_HASH));
                recvFile.setFileName(data.getString(extra.PUSH_FILE_NAME));
                recvFile.setFileType(data.getString(extra.PUSH_FILE_TYPE));
                recvFile.setFileSize(data.getInt(extra.PUSH_FILE_SIZE, 0));
                recvFile.setFileData(null);

                if (recvFile.getMsgHash() != null && recvFile.getFileName() != null) {
                    GetFileTask getFileTask = new GetFileTask();
                    getFileTask.execute(recvFile);
                } else {
                    showNote(R.string.error_InvalidIncomingMessage);
                    restart();
                }
                break;
        }
    }

    @Override
    public void onSlingerResultListener(Bundle data) {
        int resultCode = data.getInt(extra.RESULT_CODE);

        switch (resultCode) {
            case SlingerFragment.RESULT_BEGINEXCHANGE:
                showExchange(data.getString(extra.USER_DATA).getBytes());
                break;
            case SlingerFragment.RESULT_USEROPTIONS:
                showChangeSenderOptions();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // uniformly handle pass timeout from any activity...
        if (resultCode == HomeActivity.RESULT_PASSPHRASE_EXPIRED) {
            showPassphraseWhenExpired();
        }

        switch (requestCode) {

            case VIEW_PASSPHRASE_ID:
            case VIEW_PASSPHRASE_VERIFY_ID:
            case VIEW_PASSPHRASE_CHANGE_ID:
                SafeSlinger.setPassphraseOpen(false);
                switch (resultCode) {
                    case RESULT_OK:
                        String pass = null;
                        if (data != null) {
                            pass = data.getStringExtra(extra.PASS_PHRASE);
                            if (pass != null) {
                                String oldPass = SafeSlinger.getCachedPassPhrase(ConfigData
                                        .loadPrefKeyIdString(getApplicationContext()));

                                // load key...or gen one
                                boolean preExistingKey = doPassEntryCheck(pass, oldPass,
                                        requestCode == VIEW_PASSPHRASE_CHANGE_ID);
                                if (!preExistingKey) {
                                    sEditPassPhrase = pass;
                                    doGenerateNewKey();
                                    break;
                                }

                                // now enter changed one...
                                if (requestCode == VIEW_PASSPHRASE_VERIFY_ID) {
                                    String p = SafeSlinger.getCachedPassPhrase(ConfigData
                                            .loadPrefKeyIdString(getApplicationContext()));
                                    if (!TextUtils.isEmpty(p)) {
                                        showPassPhrase(false, true);
                                    } else {
                                        showPassPhraseVerify();
                                    }
                                    break;
                                }

                            }
                        }
                        restart();
                        break;
                    case PassPhraseActivity.RESULT_CLOSEANDCONTINUE:
                        restart();
                        break;
                    case RESULT_CANCELED:
                        // this separate task is now finished
                        showExit(RESULT_CANCELED);
                        break;
                }
                break;

            case REQUEST_QRECEIVE_MGS:
                switch (resultCode) {
                    case RESULT_OK:
                        runThreadGetPushReg();
                        break;
                    case RESULT_CANCELED:
                        ConfigData.savePrefPushRegistrationIdWriteOnlyC2dm(getApplicationContext(),
                                ConfigData.NOTIFY_NOPUSH_TOKENDATA);

                        if (sSenderKey == null)
                            sSenderKey = new SlingerIdentity();
                        sSenderKey.setNotification(ConfigData.NOTIFY_NOPUSH);
                        sSenderKey.setToken(ConfigData.NOTIFY_NOPUSH_TOKENDATA);

                        restart();
                        break;
                }
                break;

            case VIEW_RECIPSEL_ID:
                switch (resultCode) {
                    case PickRecipientsActivity.RESULT_RECIPSEL:
                        RecipientDbAdapter dbRecipient = RecipientDbAdapter
                                .openInstance(getApplicationContext());
                        long rowIdRecipient = data.getLongExtra(extra.RECIPIENT_ROW_ID, -1);
                        Cursor c = dbRecipient.fetchRecipient(rowIdRecipient);
                        if (c != null) {
                            sRecip = new RecipientRow(c);
                            c.close();

                            CheckRegistrationStateTask task = new CheckRegistrationStateTask();
                            task.execute(sRecip);

                            setTab(Tabs.COMPOSE);
                            restart();
                        } else {
                            showNote(R.string.error_InvalidRecipient);
                            setTab(Tabs.COMPOSE);
                            restart();
                            break;
                        }

                        break;
                    case Activity.RESULT_CANCELED:
                        // nothing to change...
                        break;
                }
                break;

            case VIEW_SETTINGS_ID:
                switch (resultCode) {
                    case SettingsActivity.RESULT_NEW_PASSPHRASE:
                        // remove cached pass
                        SafeSlinger.removeCachedPassPhrase(ConfigData.loadPrefKeyIdString(this
                                .getApplicationContext()));
                        SafeSlinger.startCacheService(HomeActivity.this);
                        showPassPhraseVerify();
                        break;
                    case Activity.RESULT_CANCELED:
                        // nothing to change...
                        break;
                }
                break;

            case VIEW_SENDINVITE_ID:
                switch (resultCode) {
                    case IntroductionActivity.RESULT_SEND:
                        if (data != null) {

                            MessageData sendMsg1 = new MessageData();
                            MessageData sendMsg2 = new MessageData();
                            RecipientRow recip1 = null;
                            RecipientRow recip2 = null;

                            RecipientDbAdapter dbRecipient = RecipientDbAdapter
                                    .openInstance(getApplicationContext());
                            // update
                            long rowIdRecipient1 = data.getLongExtra(extra.RECIPIENT_ROW_ID1, -1);
                            if (rowIdRecipient1 > -1) {
                                Cursor c = dbRecipient.fetchRecipient(rowIdRecipient1);
                                if (c != null) {
                                    recip1 = new RecipientRow(c);
                                    c.close();
                                } else {
                                    showNote(R.string.error_InvalidRecipient);
                                    break;
                                }
                            }

                            long rowIdRecipient2 = data.getLongExtra(extra.RECIPIENT_ROW_ID2, -1);
                            if (rowIdRecipient2 > -1) {
                                Cursor c = dbRecipient.fetchRecipient(rowIdRecipient2);
                                if (c != null) {
                                    recip2 = new RecipientRow(c);
                                    c.close();
                                } else {
                                    showNote(R.string.error_InvalidRecipient);
                                    break;
                                }
                            }

                            // user wants to post the file and notify recipient
                            if (recip1 == null || recip2 == null) {
                                showNote(R.string.error_InvalidRecipient);
                                restart();
                                break;
                            }
                            if (recip1.getNotify() == ConfigData.NOTIFY_NOPUSH
                                    || recip2.getNotify() == ConfigData.NOTIFY_NOPUSH) {
                                showNote(R.string.error_InvalidRecipient);
                                restart();
                                break;
                            }

                            if (recip1.isDeprecated() || recip2.isDeprecated()) {
                                showNote(R.string.error_AllMembersMustUpgradeBadKeyFormat);
                                restart();
                                break;
                            }

                            String text1 = data.getStringExtra(extra.TEXT_MESSAGE1);
                            if (!TextUtils.isEmpty(text1))
                                sendMsg1.setText(text1);

                            String text2 = data.getStringExtra(extra.TEXT_MESSAGE2);
                            if (!TextUtils.isEmpty(text2))
                                sendMsg2.setText(text2);

                            // create vcard data
                            byte[] vCard1 = null;
                            byte[] vCard2 = null;
                            try {
                                vCard1 = SSUtil.generateRecipientVCard(recip1);
                                vCard2 = SSUtil.generateRecipientVCard(recip2);
                            } catch (VCardException e) {
                                showNote(e.getLocalizedMessage());
                                restart();
                                break;
                            }

                            if (vCard1 == null || vCard2 == null) {
                                showNote(R.string.error_VcardParseFailure);
                                restart();
                                break;
                            }

                            sendMsg1.setFileData(vCard2);
                            sendMsg1.setFileName(ConfigData.INTRODUCTION_VCF);
                            sendMsg1.setFileType(ConfigData.MIMETYPE_CLASS + "/"
                                    + ConfigData.MIMETYPE_FUNC_SECINTRO);

                            sendMsg2.setFileData(vCard1);
                            sendMsg2.setFileName(ConfigData.INTRODUCTION_VCF);
                            sendMsg2.setFileType(ConfigData.MIMETYPE_CLASS + "/"
                                    + ConfigData.MIMETYPE_FUNC_SECINTRO);

                            doSendFileStart(recip1, sendMsg1);
                            doSendFileStart(recip2, sendMsg2);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // nothing to change...
                        break;
                }
                break;

            case VIEW_FILESAVE_ID:
                switch (resultCode) {
                    case RESULT_OK:
                        try {
                            String chosenFile = data.getStringExtra(extra.FNAME);
                            String chosenPath = data.getStringExtra(extra.FPATH);
                            MessageData recvMsg = new MessageData();
                            recvMsg.setFileName(chosenFile);
                            doSaveDownloadedFile(new File(chosenPath, chosenFile), recvMsg);
                        } catch (OutOfMemoryError e) {
                            showNote(R.string.error_OutOfMemoryError);
                            restart();
                            break;
                        }
                        break;
                    case RESULT_CANCELED:
                        restart();
                        break;
                }
                break;

            case VIEW_EXCHANGE_ID:
                switch (resultCode) {
                    case ControllerActivity.RESULT_KEYSLINGERIMPORTED:
                        showSave(data.getExtras());
                        break;
                    case ControllerActivity.RESULT_KEYSLINGERCANCELED:
                        restart();
                        break;
                }
                break;

            case VIEW_SAVE_ID:
                switch (resultCode) {
                    case SaveActivity.RESULT_SAVE:
                        // locally store trusted exchanged items
                        ConfigData.savePrefFirstExchangeComplete(getApplicationContext(), true);
                        runThreadImportFromExchange(data.getExtras(),
                                RecipientDbAdapter.RECIP_SOURCE_EXCHANGE, null);
                        break;
                    case SaveActivity.RESULT_SELNONE:
                        int exchanged = data.getExtras().getInt(extra.EXCHANGED_TOTAL);
                        showNote(String.format(getString(R.string.state_SomeContactsImported), "0/"
                                + exchanged));
                        break;
                    case RESULT_CANCELED:
                        showNote(String.format(getString(R.string.state_SomeContactsImported), "0"));
                        break;
                    default:
                        showNote(String.format(getString(R.string.state_SomeContactsImported), "?"));
                        break;
                }
                break;

            case VIEW_FINDCONTACT_ID:
                switch (resultCode) {
                    case RESULT_OK:
                        restart();
                        break;
                    case RESULT_CANCELED:
                        showExit(RESULT_CANCELED);
                        break;
                }
                break;

            case RESULT_PICK_CONTACT_SENDER:
                // parse newly selected sender contact
                switch (resultCode) {
                    case RESULT_OK:
                        Uri contactData = data.getData();
                        Cursor c = getContentResolver().query(contactData, null, null, null, null);
                        if (c != null) {
                            if (c.moveToFirst()) {
                                String contactLookupKey = c.getString(c
                                        .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                                String name = c.getString(c
                                        .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                                // save these for lookup and display purposes
                                ConfigData.savePrefContactLookupKey(getApplicationContext(),
                                        contactLookupKey);
                                ConfigData.savePrefContactName(getApplicationContext(), name);
                            }
                            c.close();
                        }
                        restart();
                        break;
                    case RESULT_CANCELED:
                        restart();
                        break;
                }
                break;

            case VIEW_FILEATTACH_ID:
                // file should be assigned for real now
                switch (resultCode) {
                    case RESULT_OK:
                        try {
                            String path = null;
                            if (data == null) {
                                // capture camera data from 1st non-standard
                                // return from hardware
                                path = sTempCameraFileUri.getPath();
                                doLoadAttachment(path);
                            } else {
                                String chosenFile = data.getStringExtra(extra.FNAME);
                                String chosenPath = data.getStringExtra(extra.FPATH);

                                if (chosenFile != null || chosenPath != null) {
                                    // from our own File Manager
                                    path = chosenPath + File.separator + chosenFile;
                                    doLoadAttachment(path);
                                } else if (data.getData() != null) {
                                    // String action = data.getAction();
                                    // act=null
                                    // act=android.intent.action.GET_CONTENT
                                    long filesize = getOutStreamSizeAndData(data.getData(), null);
                                    if (filesize <= 0) {
                                        showNote(R.string.error_CannotSendEmptyFile);
                                        restart();
                                        break;
                                    }
                                } else {
                                    // capture camera data from 2nd non-standard
                                    // return from hardware
                                    path = sTempCameraFileUri.getPath();
                                    doLoadAttachment(path);
                                }
                            }
                        } catch (OutOfMemoryError e) {
                            showNote(R.string.error_OutOfMemoryError);
                            restart();
                            break;
                        } catch (FileNotFoundException e) {
                            showNote(e);
                            restart();
                            break;
                        } catch (IOException e) {
                            showNote(e);
                            restart();
                            break;
                        }

                        setTab(Tabs.COMPOSE);
                        restart();
                        break;
                    case RESULT_CANCELED:
                        sSendMsg.removeFile();
                        setTab(Tabs.COMPOSE);
                        restart();
                        break;
                }
                break;

            case RESULT_PICK_MSGAPP:
                setTab(Tabs.COMPOSE);
                restart();
                break;

            case RESULT_ERROR:
                showExit(RESULT_CANCELED);
                break;

        }
    }

    private void doLoadAttachment(String path) throws FileNotFoundException {
        File phy = new File(path); // physical
        File vir = new File(path); // virtual, change if needed
        FileInputStream is = new FileInputStream(phy.getAbsolutePath());

        try {
            byte[] outFileData = new byte[is.available()];
            is.read(outFileData);
            sSendMsg.setFileData(outFileData);

            if (sSendMsg.getFileSize() > ConfigData.MAX_FILEBYTES) {
                is.close();
                showNote(String.format(getString(R.string.error_CannotSendFilesOver),
                        ConfigData.MAX_FILEBYTES));
                restart();
                return;
            }

            String type = URLConnection.guessContentTypeFromStream(is);
            if (type != null)
                sSendMsg.setFileType(type);
            else {
                String extension = SSUtil.getFileExtensionOnly(vir.getName());
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (type != null)
                    sSendMsg.setFileType(type);
                else
                    sSendMsg.setFileType("*/*");
            }
            is.close();

        } catch (OutOfMemoryError e) {
            showNote(R.string.error_OutOfMemoryError);
            restart();
            return;
        } catch (IOException e) {
            showNote(e);
            restart();
            return;
        }
        sSendMsg.setFileName(vir.getName());
        sSendMsg.setFileDir(phy.getPath());
        sSendMsg.setMsgHash(null);
    }

    private void doSaveDownloadedFile(File file, MessageData recvMsg) {
        if (saveFileAtLocation(file, recvMsg)) {
            // back to messages, to tap for open...
            setTab(Tabs.MESSAGE);
            restart();
        } else {
            showNote(String.format(getString(R.string.error_FileSave), recvMsg.getFileName()));
            restart();
        }
    }

    private void doProcessSafeSlingerMimeType(MessageData recvMsg) {
        String[] types = recvMsg.getFileType().split("/");
        if (types.length == 2) {
            if (types[1].compareToIgnoreCase(ConfigData.MIMETYPE_FUNC_SECINTRO) == 0) {

                byte[] vcard = recvMsg.getFileData();

                // parse vcard
                VCardParser parser = new VCardParser();
                VDataBuilder builder = new VDataBuilder();
                String vcardString = new String(vcard);

                // parse the string
                MyLog.d(TAG, vcardString);
                try {
                    if (!parser.parse(vcardString, "UTF-8", builder)) {
                        showNote(R.string.error_VcardParseFailure);
                        return;
                    }
                } catch (VCardException e) {
                    showNote(e.getLocalizedMessage());
                    return;
                } catch (IOException e) {
                    showNote(e.getLocalizedMessage());
                    return;
                }

                // get all parsed contacts
                List<VNode> pimContacts = builder.vNodeList;

                // do something for all the contacts
                List<ContactStruct> parsedContacts = new ArrayList<ContactStruct>();
                for (VNode contact : pimContacts) {

                    ContactStruct mem = ContactStruct.constructContactFromVNode(contact,
                            Name.NAME_ORDER_TYPE_ENGLISH);
                    if (mem != null)
                        parsedContacts.add(mem);
                }

                // there should be exactly one contact only...
                if (parsedContacts.size() != 1) {
                    showNote(R.string.error_InvalidIncomingMessage);
                    return;
                }

                // save for processing...
                sSecureIntro = parsedContacts.get(0);

                RecipientRow exchRecip = null;
                MessageRow inviteMsg = null;

                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
                Cursor c = dbMessage.fetchMessageSmall(recvMsg.getRowId());
                if (c != null) {
                    inviteMsg = new MessageRow(c);
                    c.close();
                }
                if (inviteMsg == null) {
                    showNote(R.string.error_InvalidIncomingMessage);
                    sSecureIntro = null;
                    return;
                }

                RecipientDbAdapter dbRecipient = RecipientDbAdapter
                        .openInstance(getApplicationContext());
                Cursor cr = dbRecipient.fetchRecipientByKeyId(inviteMsg.getKeyId());
                if (cr != null) {
                    exchRecip = new RecipientRow(cr);
                    cr.close();
                }
                if (exchRecip == null) {
                    showNote(R.string.error_InvalidRecipient);
                    sSecureIntro = null;
                    return;
                }

                String exchName = exchRecip.getName();
                String introName = sSecureIntro.name.toString();
                byte[] introPhoto = sSecureIntro.photoBytes;
                showIntroductionInvite(exchName, introName, introPhoto, recvMsg.getRowId());
            }
        } else {
            showNote(String.format(getString(R.string.error_FileSave), recvMsg.getFileName()));
            sSecureIntro = null;
            return;
        }
    }

    private void runThreadImportFromExchange(final Bundle args, final int recipSource,
            final String introkeyid) {

        final Handler importMsgHandler = new Handler(new Callback() {

            @Override
            public boolean handleMessage(Message msg) {
                Bundle args = msg.getData();
                String error = args != null ? args.getString(extra.ERROR) : null;
                if (TextUtils.isEmpty(error)) {
                    int imported = msg.arg2;
                    int exchanged = msg.getData().getInt(extra.EXCHANGED_TOTAL);
                    if (imported < exchanged) {
                        showNote(String.format(getString(R.string.state_SomeContactsImported),
                                imported + "/" + exchanged));
                    } else {
                        showNote(String.format(getString(R.string.state_SomeContactsImported),
                                imported));
                    }
                } else {
                    showNote(error);
                }
                return false;
            }
        });

        showProgress(getString(R.string.prog_SavingContactsToKeyDatabase));
        setProgressCancelHandler();

        Thread t = new Thread() {

            @Override
            public void run() {
                int imported = 0;
                Message msg = new Message();
                try {
                    imported = doImportFromExchange(args, recipSource, introkeyid);
                } catch (OutOfMemoryError e) {
                    args.putString(extra.ERROR, getString(R.string.error_OutOfMemoryError));
                } catch (SQLException e) {
                    args.putString(extra.ERROR, getString(R.string.error_UnableToSaveRecipientInDB));
                } catch (GeneralException e) {
                    args.putString(extra.ERROR, e.getLocalizedMessage());
                }

                msg.arg2 = imported;
                msg.setData(args);
                importMsgHandler.sendMessage(msg);

                endProgress(RECOVERY_EXCHANGEIMPORT);
            }
        };
        t.start();
    }

    private void runThreadBackgroundSyncUpdates() {

        final Handler syncMsgHandler = new Handler(new Callback() {

            @Override
            public boolean handleMessage(Message msg) {
                if (msg.arg1 != 0)
                    showNote(msg.arg1);
                return false;
            }
        });

        Thread t = new Thread() {

            @Override
            public void run() {
                ConfigData.savePrefContactDBLastScan(getApplicationContext(),
                        System.currentTimeMillis());
                Message msg = new Message();

                // look for updates in address book...
                try {
                    doUpdateRecipientsFromContacts();
                } catch (SQLException e) {
                    // ignore since we only attempt to update old data
                }

                // make sure recipient list shows correct keys...
                if (!doUpdateActiveKeyStatus()) {
                    msg = new Message();
                    msg.arg1 = R.string.error_UnableToUpdateRecipientInDB;
                    syncMsgHandler.sendMessage(msg);
                }

                // remove deprecated key storage from contacts
                String[] keyNames = new String[] { //
                        ConfigData.APP_KEY_OLD1, //
                        ConfigData.APP_KEY_OLD2, //
                        ConfigData.APP_KEY_PUBKEY, //
                        ConfigData.APP_KEY_PUSHTOKEN, //
                };
                doCleanupOldKeyData(keyNames);
            }
        };
        t.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // The activity is about to become visible.
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // require pass on wake...
        if (hasFocus)
            showPassphraseWhenExpired();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The activity has become visible (it is now "resumed").

        restoreView();

        // require pass on wake...
        showPassphraseWhenExpired();
    }

    private void showPassphraseWhenExpired() {
        String contactName = ConfigData.loadPrefContactName(getApplicationContext());
        if (CryptTools.existsSecretKey(getApplicationContext()) && !TextUtils.isEmpty(contactName)) {

            String pass = SafeSlinger.getCachedPassPhrase(ConfigData
                    .loadPrefKeyIdString(getApplicationContext()));
            if (TextUtils.isEmpty(pass)) {
                if (!SafeSlinger.isPassphraseOpen())
                    showPassPhrase(false, false);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Another activity is taking focus (this activity is about to be
        // "paused").

        restoreView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // The activity is no longer visible (it is now "stopped")
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // The activity is about to be destroyed.

        // attempt to unregister, however we can safely ignore the
        // "IllegalArgumentException: Receiver not registered" called when
        // some hardware experiences a race condition here.
        try {
            unregisterReceiver(mPushRegReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        try {
            unregisterReceiver(mMsgUpdateReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        // make sure we are cleared out...
        if (this.isFinishing())
            reInitForExit();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // general state
        if (savedInstanceState != null) {
            String filepath = savedInstanceState.getString(extra.FILE_PATH);
            if (!TextUtils.isEmpty(filepath)) {
                sTempCameraFileUri = Uri.fromFile(new File(filepath));
            }

            setTab(Tabs.values()[savedInstanceState.getInt(extra.RECOVERY_TAB)]);
        }

        // several private members may need to be reloaded...
        restart();
    }

    @Override
    public void onEulaAgreedTo() {
        restart();
    }

    private void restart() {
        initOnReload();
    }

    protected void postProgressMsgList(long rowId, String msg) {
        try {
            if (mTabsAdapter != null) {
                MessagesFragment mf = (MessagesFragment) mTabsAdapter
                        .findFragmentByPosition(Tabs.MESSAGE.ordinal());
                if (mf != null) {
                    mf.postProgressMsgList(rowId, msg);
                }
            }
        } catch (IllegalStateException e) {
            // Can not perform this action after onSaveInstanceState...
            e.printStackTrace();
        }
    }

    private class GetMessageTask extends AsyncTask<MessageData, String, String> {
        private long mRowId;
        private WebEngine mWeb = new WebEngine(HomeActivity.this);
        private MessageData mRecvMsg;

        @Override
        protected String doInBackground(MessageData... mds) {
            mRecvMsg = mds[0];
            mRowId = mRecvMsg.getRowId();

            try {
                if (!SafeSlinger.getApplication().isOnline()) {
                    return getString(R.string.error_CorrectYourInternetConnection);
                }

                publishProgress(getString(R.string.prog_RequestingMessage));
                String pass = SafeSlinger.getCachedPassPhrase(ConfigData
                        .loadPrefKeyIdString(getApplicationContext()));
                byte[] msgHashBytes;
                try {
                    msgHashBytes = Base64.decode(mRecvMsg.getMsgHash().getBytes(), Base64.NO_WRAP);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    return getString(R.string.error_InvalidIncomingMessage);
                }

                publishProgress(getString(R.string.prog_ReceivingFile));
                byte[] resp = mWeb.getMessage(msgHashBytes);

                if (resp == null || resp.length == 0) {
                    return getString(R.string.error_InvalidIncomingMessage);
                }

                byte[] encMsg = WebEngine.parseMessageResponse(resp);
                if (encMsg == null) {
                    return getString(R.string.error_InvalidIncomingMessage);
                }

                publishProgress(getString(R.string.prog_verifyingIntegrity));

                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
                if (Arrays.equals(msgHashBytes, CryptTools.computeSha3Hash(encMsg))) {
                    CryptoMsgProvider tool = CryptoMsgProvider.createInstance(SafeSlinger
                            .getApplication().isLoggable());
                    String keyid = null;
                    try {
                        keyid = tool.ExtractKeyIDfromPacket(encMsg);
                    } catch (CryptoMsgPacketSizeException e) {
                        e.printStackTrace();
                    }
                    // save downloaded initial message
                    if (!dbMessage.updateMessageDownloaded(mRowId, encMsg,
                            MessageDbAdapter.MESSAGE_IS_SEEN, keyid)) {
                        return getString(R.string.error_UnableToUpdateMessageInDB);
                    }
                } else {
                    return getString(R.string.error_InvalidCommitVerify);
                }

                publishProgress(getString(R.string.prog_decrypting));
                StringBuilder keyIdOut = new StringBuilder();
                byte[] rawMsg = CryptTools
                        .decryptMessage(HomeActivity.this, encMsg, pass, keyIdOut);
                MessagePacket msg = new MessagePacket(rawMsg);
                mRecvMsg.setFileHash(msg.getFileHash());

                if (!dbMessage.updateMessageDecrypted(mRowId, msg, keyIdOut.toString())) {
                    return getString(R.string.error_UnableToUpdateMessageInDB);
                }

            } catch (OutOfMemoryError e) {
                return getString(R.string.error_OutOfMemoryError);
            } catch (ExchangeException e) {
                return e.getLocalizedMessage();
            } catch (FileNotFoundException e) {
                return e.getLocalizedMessage();
            } catch (IOException e) {
                return e.getLocalizedMessage();
            } catch (GeneralException e) {
                return e.getLocalizedMessage();
            } catch (ClassNotFoundException e) {
                return e.getLocalizedMessage();
            } catch (CryptoMsgException e) {
                return e.getLocalizedMessage();
            } catch (MessageNotFoundException e) {
                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
                if (!dbMessage.updateMessageExpired(mRecvMsg.getRowId())) {
                    return getString(R.string.error_UnableToUpdateMessageInDB);
                }

            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            postProgressMsgList(mRowId, progress[0]);
        }

        @Override
        protected void onPostExecute(String error) {
            postProgressMsgList(mRowId, null);
            if (TextUtils.isEmpty(error)) {
                setTab(Tabs.MESSAGE);
                restart();
            } else {
                showNote(error);
                restart();
            }
        }
    }

    private class GetFileTask extends AsyncTask<MessageData, String, String> {
        private long mRowId;
        private WebEngine mWeb = new WebEngine(HomeActivity.this);
        private Handler mHandler = new Handler();
        private int mRxTotalSize = 0;
        private MessageData mRecvMsg;
        private Runnable mUpdateRxProgress = new Runnable() {

            @Override
            public void run() {
                int rxCurr = (int) mWeb.get_rxCurrentBytes();
                if (rxCurr > 0) {
                    int pct = (int) ((rxCurr / (float) mRxTotalSize) * 100);
                    postProgressMsgList(mRowId, String.format("%s %d%%",
                            getString(R.string.prog_ReceivingFile), pct > 100 ? 100 : pct));
                }
                mHandler.postDelayed(this, MS_POLL_INTERVAL);
            }
        };

        @Override
        protected String doInBackground(MessageData... mds) {
            mRecvMsg = mds[0];
            mRowId = mRecvMsg.getRowId();
            mRxTotalSize = 0;

            try {
                if (!SafeSlinger.getApplication().isOnline()) {
                    return getString(R.string.error_CorrectYourInternetConnection);
                }

                publishProgress(getString(R.string.prog_RequestingFile));
                String pass = SafeSlinger.getCachedPassPhrase(ConfigData
                        .loadPrefKeyIdString(getApplicationContext()));
                byte[] encFile = null;
                byte[] msgHashBytes;
                try {
                    msgHashBytes = Base64.decode(mRecvMsg.getMsgHash().getBytes(), Base64.NO_WRAP);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    return getString(R.string.error_InvalidIncomingMessage);
                }

                // TODO implement more accurate calculation of ciphertext size
                mRxTotalSize += mRecvMsg.getFileSize();

                mHandler.postDelayed(mUpdateRxProgress, MS_POLL_INTERVAL);
                byte[] resp = mWeb.getFile(msgHashBytes);
                mHandler.removeCallbacks(mUpdateRxProgress);

                if (resp == null || resp.length == 0) {
                    return getString(R.string.error_InvalidIncomingMessage);
                }

                encFile = WebEngine.parseMessageResponse(resp);
                if (encFile == null) {
                    return getString(R.string.error_InvalidIncomingMessage);
                }

                publishProgress(getString(R.string.prog_verifyingIntegrity));
                if (Arrays.equals(mRecvMsg.getFileHash(), CryptTools.computeSha3Hash(encFile))) {
                    return getString(R.string.error_InvalidCommitVerify);
                }

                publishProgress(getString(R.string.prog_decrypting));
                StringBuilder keyIdOut = new StringBuilder();
                byte[] rawFile = CryptTools.decryptMessage(HomeActivity.this, encFile, pass,
                        keyIdOut);

                if (rawFile != null) {
                    mRecvMsg.setFileData(rawFile);

                    MessageDbAdapter dbMessage = MessageDbAdapter
                            .openInstance(getApplicationContext());
                    if (!dbMessage.updateFileDecrypted(mRowId)) {
                        return getString(R.string.error_UnableToUpdateMessageInDB);
                    }

                }

            } catch (OutOfMemoryError e) {
                return getString(R.string.error_OutOfMemoryError);
            } catch (ExchangeException e) {
                return e.getLocalizedMessage();
            } catch (FileNotFoundException e) {
                return e.getLocalizedMessage();
            } catch (IOException e) {
                return e.getLocalizedMessage();
            } catch (ClassNotFoundException e) {
                return e.getLocalizedMessage();
            } catch (CryptoMsgException e) {
                return e.getLocalizedMessage();
            } catch (GeneralException e) {
                return e.getLocalizedMessage();
            } catch (MessageNotFoundException e) {
                return e.getLocalizedMessage();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            postProgressMsgList(mRowId, progress[0]);
        }

        @Override
        protected void onPostExecute(String error) {
            mHandler.removeCallbacks(mUpdateRxProgress);
            postProgressMsgList(mRowId, null);
            if (TextUtils.isEmpty(error)) {
                if (mRecvMsg.getFileType().startsWith(ConfigData.MIMETYPE_CLASS + "/")) {
                    doProcessSafeSlingerMimeType(mRecvMsg);
                } else {
                    // ask user what to do with downloaded data
                    if (SSUtil.isExternalStorageWritable()) {
                        // file must exist
                        if (mRecvMsg.getFileData() != null) {
                            doSaveDownloadedFile(
                                    SSUtil.getDefaultDownloadPath(mRecvMsg.getFileName()), mRecvMsg);
                        } else {
                            showNote(String.format(getString(R.string.error_FileSave),
                                    mRecvMsg.getFileName()));
                        }
                    } else {
                        showNote(R.string.error_FileStorageUnavailable);
                    }
                }
            } else {
                showNote(error);
                restart();
            }
        }
    }

    private class SendFileTask extends AsyncTask<MessageTransport, String, String> {
        private RecipientRow mRecip;
        private MessageData mSendMsg;
        private long mRowId;
        private WebEngine mWeb = new WebEngine(HomeActivity.this);
        private Handler mHandler = new Handler();
        private int mTxTotalSize = 0;
        private Runnable mUpdateTxProgress = new Runnable() {

            @Override
            public void run() {
                int txCurr = (int) mWeb.get_txCurrentBytes();
                int pct = (int) ((txCurr / (float) mTxTotalSize) * 100);
                String str = String.format(getString(R.string.prog_SendingFile), "");
                postProgressMsgList(mRowId, String.format("%s %d%%", str, pct > 100 ? 100 : pct));
                mHandler.postDelayed(this, MS_POLL_INTERVAL);
            }
        };

        @Override
        protected String doInBackground(MessageTransport... mts) {
            mRecip = mts[0].getRecipient();
            mSendMsg = mts[0].getMessageData();
            mRowId = mSendMsg.getRowId();
            mTxTotalSize = 0;

            try {
                // encrypt file data...
                publishProgress(getString(R.string.prog_encrypting));
                String pass = SafeSlinger.getCachedPassPhrase(ConfigData
                        .loadPrefKeyIdString(getApplicationContext()));
                byte[] encFile = null;
                byte[] encMsg = null;
                byte[] rawFile = mSendMsg.getFileData();
                if (rawFile != null && rawFile.length > 0) {
                    encFile = CryptTools.encryptMessage(getApplicationContext(), rawFile, pass,
                            mRecip.getPubkey());
                } else {
                    encFile = new byte[0];
                }
                mTxTotalSize += encFile.length;

                // format message data, including hash of encrypted file
                // data...
                publishProgress(getString(R.string.prog_generatingSignature));
                byte[] msgData = new MessagePacket(
                        ConfigData.getVersionCode(getApplicationContext()),//
                        System.currentTimeMillis(), //
                        encFile.length,//
                        mSendMsg.getFileName(), //
                        mSendMsg.getFileType(),//
                        mSendMsg.getText(),//
                        ConfigData.loadPrefContactName(getApplicationContext()),//
                        CryptTools.computeSha3Hash(encFile)//
                ).getBytes();
                if (msgData == null) {
                    throw new GeneralException(getString(R.string.error_InvalidMsg));
                }

                // encrypt message data...
                publishProgress(getString(R.string.prog_encrypting));
                encMsg = CryptTools.encryptMessage(getApplicationContext(), msgData, pass,
                        mRecip.getPubkey());
                mTxTotalSize += encMsg.length;

                // message id = hash of encrypted message data...
                publishProgress(getString(R.string.prog_generatingSignature));
                byte[] msgHashBytes = CryptTools.computeSha3Hash(encMsg);
                mSendMsg.setMsgHash(new String(Base64.encode(msgHashBytes, Base64.NO_WRAP)));

                // send...
                mHandler.postDelayed(mUpdateTxProgress, MS_POLL_INTERVAL);
                switch (mRecip.getNotify()) {

                    default:
                    case ConfigData.NOTIFY_NOPUSH:
                        return getString(R.string.error_InvalidRecipient);

                    case ConfigData.NOTIFY_ANDROIDC2DM:
                        mWeb.postFileAndroidC2DM(msgHashBytes, encMsg, encFile,
                                mRecip.getPushtoken());
                        break;

                    case ConfigData.NOTIFY_APPLEUA:
                        mWeb.postFileAppleUA(msgHashBytes, encMsg, encFile, mRecip.getPushtoken());
                        break;
                }
                mHandler.removeCallbacks(mUpdateTxProgress);

                // file sent ok, recipient notified...
                // update sent...
                publishProgress(getString(R.string.prog_FileSent));

                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
                if (!dbMessage.updateMessageSent(mSendMsg.getRowId(), mRecip.getName(),
                        mRecip.getKeyid(), mSendMsg.getMsgHash(), mSendMsg.getFileName(),
                        mSendMsg.getFileSize(), mSendMsg.getFileType(), mSendMsg.getFileDir(),
                        mSendMsg.getText(), MessageDbAdapter.MESSAGE_STATUS_COMPLETE_MSG)) {
                    return getString(R.string.error_UnableToUpdateMessageInDB);
                }

            } catch (OutOfMemoryError e) {
                return getString(R.string.error_OutOfMemoryError);
            } catch (ExchangeException e) {
                return e.getLocalizedMessage();
            } catch (IOException e) {
                return e.getLocalizedMessage();
            } catch (GeneralException e) {
                return e.getLocalizedMessage();
            } catch (ClassNotFoundException e) {
                return e.getLocalizedMessage();
            } catch (CryptoMsgException e) {
                return e.getLocalizedMessage();
            } catch (MessageNotFoundException e) {
                return e.getLocalizedMessage();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            postProgressMsgList(mRowId, progress[0]);
        }

        @Override
        protected void onPostExecute(String error) {
            mHandler.removeCallbacks(mUpdateTxProgress);
            postProgressMsgList(mRowId, null);
            if (TextUtils.isEmpty(error)) {
                // send complete, remove secret sent
                sSendMsg = new MessageData();

                showNote(R.string.state_FileSent);
            } else {
                // update recipient if no longer registered
                boolean notreg = mWeb.isNotRegistered();
                if (notreg) {
                    RecipientDbAdapter dbRecipient = RecipientDbAdapter
                            .openInstance(getApplicationContext());
                    if (!dbRecipient.updateRecipientRegistrationState(sRecip.getRowId(), notreg)) {
                        // failure to update database error, not as critical as
                        // the registration loss...
                    }
                }
                showNote(error);

                // set message back to draft status
                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
                if (!dbMessage.updateDraftMessage(mSendMsg.getRowId(), mRecip, mSendMsg)) {
                    showNote(R.string.error_UnableToUpdateMessageInDB);
                }
            }
        }
    }

    private class CheckRegistrationStateTask extends AsyncTask<RecipientRow, String, String> {
        private WebEngine mWeb = new WebEngine(HomeActivity.this);
        private long mRowId;

        @Override
        protected String doInBackground(RecipientRow... recips) {
            RecipientRow recip = recips[0];
            if (recip == null) {
                return getString(R.string.error_InvalidRecipient);
            }
            mRowId = recip.getRowId();

            try {
                int notify = recip.getNotify();
                switch (notify) {
                    default:
                    case ConfigData.NOTIFY_NOPUSH:
                    case ConfigData.NOTIFY_ANDROIDC2DM:
                        break;
                    case ConfigData.NOTIFY_APPLEUA:
                        mWeb.checkStatusAppleUA(recip.getPushtoken());
                        break;
                }
            } catch (ExchangeException e) {
                return e.getLocalizedMessage();
            } catch (MessageNotFoundException e) {
                return e.getLocalizedMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String error) {
            if (!TextUtils.isEmpty(error)) {
                showNote(error);
            }
            boolean notreg = mWeb.isNotRegistered();
            if (notreg) {
                RecipientDbAdapter dbRecipient = RecipientDbAdapter
                        .openInstance(getApplicationContext());
                if (!dbRecipient.updateRecipientRegistrationState(mRowId, notreg)) {
                    showNote(R.string.error_UnableToUpdateMessageInDB);
                }
            }
        }
    }

    private void runThreadGetPushReg() {
        sWebError = null;

        // google account is required
        if (!SSUtil.isGoogleAccountPresent(getApplicationContext())) {
            showAccountsSettings();
            showErrorExit(R.string.error_C2DMRegAccountMissing);
            return;
        }

        // user must be online...
        if (!SafeSlinger.getApplication().isOnline()) {
            showErrorExit(R.string.error_CorrectYourInternetConnection);
            return;
        }

        showProgress(getString(R.string.prog_RequestingPushReg));
        setProgressCancelHandler();
        Thread t = new Thread() {

            @Override
            public void run() {
                C2DMessaging.register(getApplicationContext(), ConfigData.PUSH_SENDERID_EMAIL);
            }
        };
        t.start();
    }

    private boolean saveFileAtLocation(File downloadedFile, MessageData recvMsg) {
        try {
            File saveFile = downloadedFile;

            // make sure filename is unique
            int index = 1;
            while (saveFile.exists()) {
                saveFile = new File(SSUtil.getEnumeratedFilename(downloadedFile.getAbsolutePath(),
                        index));
                index++;
            }

            FileOutputStream f = new FileOutputStream(saveFile);
            ByteArrayInputStream in = new ByteArrayInputStream(recvMsg.getFileData());
            byte[] buffer = new byte[1024];
            int len1 = 0;
            while ((len1 = in.read(buffer)) > 0) {
                f.write(buffer, 0, len1);
            }
            f.close();

            MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
            if (!dbMessage.updateMessageFileLocation(recvMsg.getRowId(), saveFile.getName(),
                    saveFile.getPath())) {
                showNote(R.string.error_UnableToUpdateMessageInDB);
            }

            // always show file automatically...
            showFileActionChooser(saveFile, recvMsg.getFileType());

            return true;
        } catch (FileNotFoundException e) {
            showNote(e.getLocalizedMessage());
            return false;
        } catch (SecurityException e) {
            showNote(e.getLocalizedMessage());
            return false;
        } catch (IOException e) {
            showNote(e.getLocalizedMessage());
            return false;
        }
    }

    private void setProgressCancelHandler() {
        if (sProg != null) {
            sProg.setCanceledOnTouchOutside(false);
            sProg.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    showExit(RESULT_CANCELED);
                }
            });
        }
    }

    private void showAddContact(String name) {

        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

        if (!TextUtils.isEmpty(name))
            intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
        try {
            startActivityForResult(intent, RESULT_PICK_CONTACT_SENDER);
        } catch (ActivityNotFoundException e) {
            showNote(getUnsupportedFeatureString("Contacts"));
        }
    }

    private void showErrorExit(String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            removeDialog(DIALOG_ERREXIT);
            showDialog(DIALOG_ERREXIT, args);
        }
    }

    private AlertDialog.Builder xshowErrorExit(Activity act, Bundle args) {
        String msg = args.getString(extra.RESID_MSG);
        MyLog.e(TAG, msg);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_Error);
        ad.setMessage(msg);
        ad.setCancelable(false);
        ad.setNeutralButton(R.string.btn_OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                onActivityResult(RESULT_ERROR, RESULT_OK, null);
                dialog.dismiss();
            }
        });
        return ad;
    }

    private void showErrorExit(Exception e) {
        showErrorExit(e.getLocalizedMessage());
    }

    private void showErrorExit(int resId) {
        showErrorExit(getString(resId));
    }

    private void showExit(int resultCode) {
        setResult(resultCode);
        finish();
    }

    private void reInitForExit() {
        ConfigData.savePrefPusgRegBackoff(getApplicationContext(),
                ConfigData.DEFAULT_PUSHREG_BACKOFF);
        sProg = null;
        sProgressMsg = null;
        sSendMsg = new MessageData();
        sWebError = null;
        sEditPassPhrase = null;
        sBackupExists = false;
        sRestoreRequested = false;
        sRestoreReported = false;
        sRestoring = false;
        sSecureIntro = null;
    }

    public boolean hasImageCaptureBug() {

        // list of known devices that have the bug
        ArrayList<String> devices = new ArrayList<String>();
        devices.add("android-devphone1/dream_devphone/dream");
        devices.add("generic/sdk/generic");
        devices.add("vodafone/vfpioneer/sapphire");
        devices.add("tmobile/kila/dream");
        devices.add("verizon/voles/sholes");
        devices.add("google_ion/google_ion/sapphire");
        devices.add("samsung/GT-I9000/GT-I9000");
        devices.add("samsung/SGH-I777/SGH-I777");

        return devices.contains(android.os.Build.BRAND + "/" + android.os.Build.PRODUCT + "/"
                + android.os.Build.DEVICE);
    }

    private void showFileAttach() {
        final List<Intent> allIntents = new ArrayList<Intent>();

        // all openable...
        Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentIntent.setType("*/*");
        contentIntent.addCategory(Intent.CATEGORY_OPENABLE);

        // camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        sTempCameraFileUri = SSUtil.makeCameraOutputUri();
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, sTempCameraFileUri);
        allIntents.add(cameraIntent);

        // audio recorder
        Intent recorderIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        recorderIntent.putExtra(MediaStore.EXTRA_OUTPUT, SSUtil.makeRecorderOutputUri());
        allIntents.add(recorderIntent);

        // our custom browser
        if (SSUtil.isExternalStorageReadable()) {
            Intent filePickIntent = new Intent(HomeActivity.this, FilePickerActivity.class);
            LabeledIntent fileIntent = new LabeledIntent(filePickIntent,
                    filePickIntent.getPackage(), R.string.menu_FileManager,
                    R.drawable.ic_menu_directory);
            allIntents.add(fileIntent);
        }

        // Chooser of file system options.
        Intent chooserIntent = Intent.createChooser(contentIntent,
                getString(R.string.title_ChooseFileLoad));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                allIntents.toArray(new Parcelable[] {}));
        startActivityForResult(chooserIntent, VIEW_FILEATTACH_ID);
    }

    protected void showFileSave(String fileName) {
        if (SSUtil.isExternalStorageWritable()) {
            Intent intent = new Intent(HomeActivity.this, FileSaveActivity.class);
            intent.putExtra(extra.FNAME, fileName);
            startActivityForResult(intent, VIEW_FILESAVE_ID);
        } else {
            showNote(R.string.error_FileStorageUnavailable);
        }
    }

    private void showFindContact() {
        String name = ConfigData.loadPrefContactName(getApplicationContext());

        Intent intent = new Intent(HomeActivity.this, FindContactActivity.class);
        intent.putExtra(extra.NAME, name);
        startActivityForResult(intent, VIEW_FINDCONTACT_ID);
    }

    private void showRecipientSelect() {
        Intent intent = new Intent(HomeActivity.this, PickRecipientsActivity.class);
        intent.putExtra(extra.ALLOW_EXCH, true);
        intent.putExtra(extra.ALLOW_INTRO, true);
        startActivityForResult(intent, VIEW_RECIPSEL_ID);
    }

    private void showEditContact(int requestCode) {
        String contactLookupKey = ConfigData.loadPrefContactLookupKey(getApplicationContext());
        Uri personUri = getPersonUri(contactLookupKey);
        if (personUri != null) {
            Intent intent = new Intent(Intent.ACTION_EDIT, personUri);
            try {
                startActivityForResult(intent, requestCode);
            } catch (ActivityNotFoundException e) {
                showNote(getUnsupportedFeatureString("Contacts"));
            }
        } else {
            showNote(R.string.error_ContactUpdateFailed);
        }
    }

    private void showPickContact(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            showNote(getUnsupportedFeatureString("Contacts"));
        }
    }

    private void showSettings() {
        Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
        startActivityForResult(intent, VIEW_SETTINGS_ID);
    }

    private void showPassPhrase(boolean create, boolean change) {
        Intent intent = new Intent(HomeActivity.this, PassPhraseActivity.class);
        intent.putExtra(extra.CREATE_PASS_PHRASE, create);
        intent.putExtra(extra.CHANGE_PASS_PHRASE, change);
        intent.putExtra(extra.VERIFY_PASS_PHRASE, false);
        int view = change ? VIEW_PASSPHRASE_CHANGE_ID : VIEW_PASSPHRASE_ID;
        startActivityForResult(intent, view);
        SafeSlinger.setPassphraseOpen(true);
    }

    private void showPassPhraseVerify() {
        Intent intent = new Intent(HomeActivity.this, PassPhraseActivity.class);
        intent.putExtra(extra.CREATE_PASS_PHRASE, false);
        intent.putExtra(extra.CHANGE_PASS_PHRASE, false);
        intent.putExtra(extra.VERIFY_PASS_PHRASE, true);
        startActivityForResult(intent, VIEW_PASSPHRASE_VERIFY_ID);
        SafeSlinger.setPassphraseOpen(true);
    }

    public int getTotalUsers() {
        String userName;
        long keyDate;
        int userNumber = 0;
        do {
            userName = null;
            keyDate = 0;
            userName = ConfigData.loadPrefContactName(getApplicationContext(), userNumber);
            keyDate = ConfigData.loadPrefKeyDate(getApplicationContext(), userNumber);
            if (!TextUtils.isEmpty(userName)) {
                userNumber++;
            }
        } while (keyDate > 0);

        // always at least 1
        return userNumber > 1 ? userNumber : 1;
    }

    private void showSendIntroduction() {
        Intent intent = new Intent(HomeActivity.this, IntroductionActivity.class);
        startActivityForResult(intent, VIEW_SENDINVITE_ID);
    }

    private void showIntroductionInvite(String exchName, String introName, byte[] introPhoto,
            long msgRowId) {
        Bundle args = new Bundle();
        args.putString(extra.EXCH_NAME, exchName);
        args.putString(extra.INTRO_NAME, introName);
        args.putByteArray(extra.PHOTO, introPhoto);
        args.putLong(extra.MESSAGE_ROW_ID, msgRowId);
        if (!isFinishing()) {
            removeDialog(DIALOG_INTRO);
            showDialog(DIALOG_INTRO, args);
        }
    }

    private AlertDialog.Builder xshowIntroductionInvite(final Activity act, Bundle args) {
        String exchName = args.getString(extra.EXCH_NAME);
        String introName = args.getString(extra.INTRO_NAME);
        byte[] introPhoto = args.getByteArray(extra.PHOTO);
        final long msgRowId = args.getLong(extra.MESSAGE_ROW_ID);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        View layout;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            layout = View.inflate(new ContextThemeWrapper(act, R.style.Theme_Sherlock),
                    R.layout.secureinvite, null);
        } else {
            LayoutInflater inflater = (LayoutInflater) act
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.secureinvite, null);
        }
        TextView textViewExchName = (TextView) layout.findViewById(R.id.textViewExchName);
        TextView textViewIntroName = (TextView) layout.findViewById(R.id.textViewIntroName);
        ImageView imageViewIntroPhoto = (ImageView) layout.findViewById(R.id.imageViewIntroPhoto);
        ad.setTitle(R.string.title_SecureIntroductionInvite);
        textViewExchName.setText(exchName);
        textViewIntroName.setText(introName);
        if (introPhoto != null) {
            try {
                Bitmap bm = BitmapFactory.decodeByteArray(introPhoto, 0, introPhoto.length, null);
                imageViewIntroPhoto.setImageBitmap(bm);
            } catch (OutOfMemoryError e) {
                imageViewIntroPhoto.setImageDrawable(getResources().getDrawable(
                        R.drawable.ic_silhouette));
            }
        }
        ad.setView(layout);
        ad.setCancelable(false);
        ad.setPositiveButton(getString(R.string.btn_Accept), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                // accept secure introduction?
                Bundle args = new Bundle();
                int selected = 0;
                String name = sSecureIntro.name.toString();
                args.putString(extra.NAME + selected, name);
                args.putByteArray(extra.PHOTO + selected, sSecureIntro.photoBytes);
                List<ContactMethod> contactmethodList = sSecureIntro.contactmethodList;
                if (contactmethodList != null) {
                    for (ContactMethod item : contactmethodList) {
                        if (item.kind == android.provider.Contacts.KIND_IM) {
                            if (item.label.compareTo(ConfigData.APP_KEY_PUBKEY) == 0
                                    || item.label.compareTo(ConfigData.APP_KEY_PUSHTOKEN) == 0) {
                                args.putByteArray(item.label + selected,
                                        SSUtil.finalDecode(item.data.getBytes()));
                            }
                        }
                    }
                }

                String contactLookupKey = getContactLookupKeyByName(sSecureIntro.name.toString());
                args.putString(extra.CONTACT_LOOKUP_KEY + selected, contactLookupKey);

                sSecureIntro = null; // we're done with it now...

                MessageRow inviteMsg = null;
                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
                Cursor c = dbMessage.fetchMessageSmall(msgRowId);
                if (c != null) {
                    inviteMsg = new MessageRow(c);
                    c.close();
                }

                if (inviteMsg == null) {
                    showNote(R.string.error_InvalidIncomingMessage);
                    return;
                }

                // import the new contacts
                runThreadImportFromExchange(args, RecipientDbAdapter.RECIP_SOURCE_INTRODUCTION,
                        inviteMsg.getKeyId());
                setTab(Tabs.MESSAGE);
                restart();
            }
        });
        ad.setNegativeButton(getString(R.string.btn_Refuse), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                sSecureIntro = null;
                showNote(String.format(getString(R.string.state_SomeContactsImported), 0));
                restart();
            }
        });
        return ad;
    }

    protected void showChangeSenderOptions() {
        UseContactItem profile = getContactProfile();

        String name = ConfigData.loadPrefContactName(getApplicationContext());
        ArrayList<UseContactItem> contacts = getUseContactItemsByName(name);

        boolean isContactInUse = !TextUtils.isEmpty(ConfigData
                .loadPrefContactLookupKey(getApplicationContext()));

        Bundle args = new Bundle();
        args.putBoolean(extra.CREATED, isContactInUse);
        if (profile != null) {
            args.putString(extra.NAME, profile.text);
            args.putByteArray(extra.PHOTO, profile.icon);
            args.putString(extra.CONTACT_LOOKUP_KEY, profile.contactLookupKey);
        }
        if (contacts != null) {
            for (int i = 0; i < contacts.size(); i++) {
                UseContactItem c = contacts.get(i);
                args.putString(extra.NAME + i, c.text);
                args.putByteArray(extra.PHOTO + i, c.icon);
                args.putString(extra.CONTACT_LOOKUP_KEY + i, c.contactLookupKey);
            }
        }
        if (!isFinishing()) {
            removeDialog(DIALOG_USEROPTIONS);
            showDialog(DIALOG_USEROPTIONS, args);
        }
    }

    protected AlertDialog.Builder xshowChangeSenderOptions(final Activity act, Bundle args) {
        final ArrayList<UseContactItem> items = new ArrayList<UseContactItem>();
        boolean isContactInUse = args.getBoolean(extra.CREATED);
        String pName = args.getString(extra.NAME);
        byte[] pPhoto = args.getByteArray(extra.PHOTO);
        String pLookupKey = args.getString(extra.CONTACT_LOOKUP_KEY);
        if (!TextUtils.isEmpty(pName)) {
            items.add(new UseContactItem(String.format(
                    act.getString(R.string.menu_UseProfilePerson), pName), pPhoto, pLookupKey,
                    UCType.PROFILE));
        }
        int i = 0;
        String cName = args.getString(extra.NAME + i);
        byte[] cPhoto = args.getByteArray(extra.PHOTO + i);
        String cLookupKey = args.getString(extra.CONTACT_LOOKUP_KEY + i);
        while (!TextUtils.isEmpty(cName)) {
            items.add(new UseContactItem(String.format(
                    act.getString(R.string.menu_UseContactPerson), cName), cPhoto, cLookupKey,
                    UCType.CONTACT));
            i++;
            cName = args.getString(extra.NAME + i);
            cPhoto = args.getByteArray(extra.PHOTO + i);
            cLookupKey = args.getString(extra.CONTACT_LOOKUP_KEY + i);
        }
        items.add(new UseContactItem(act.getString(R.string.menu_UseNoContact), UCType.NONE));
        items.add(new UseContactItem(act.getString(R.string.menu_UseAnother), UCType.ANOTHER));
        items.add(new UseContactItem(act.getString(R.string.menu_CreateNew), UCType.NEW));
        items.add(new UseContactItem(act.getString(R.string.menu_EditName), UCType.EDIT_NAME));
        if (isContactInUse) {
            items.add(new UseContactItem(act.getString(R.string.menu_EditContact),
                    UCType.EDIT_CONTACT));
        }

        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_MyIdentity);
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        ListAdapter adapter = new ArrayAdapter<UseContactItem>(act,
                android.R.layout.select_dialog_item, android.R.id.text1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView) v.findViewById(android.R.id.text1);
                UseContactItem item = items.get(position);

                if (item.contact) {
                    Drawable d;
                    if (item.icon != null) {
                        Bitmap bm = BitmapFactory.decodeByteArray(item.icon, 0, item.icon.length,
                                null);
                        d = new BitmapDrawable(act.getResources(), bm);
                    } else {
                        d = act.getResources().getDrawable(R.drawable.ic_silhouette);
                    }
                    // d.setBounds(0, 0, 25, 25);
                    tv.setCompoundDrawablesWithIntrinsicBounds(null, null, d, null);
                    tv.setCompoundDrawablePadding((int) act.getResources().getDimension(
                            R.dimen.size_5dp));
                } else {
                    tv.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }

                return v;
            }
        };
        ad.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
                switch (items.get(item).type) {
                    case PROFILE:
                        // user wants to use found profile as a personal contact
                        ConfigData.savePrefContactLookupKey(getApplicationContext(),
                                items.get(item).contactLookupKey);
                        restart();
                        break;
                    case CONTACT:
                        // user wants to use found contact as a personal contact
                        ConfigData.savePrefContactLookupKey(getApplicationContext(),
                                items.get(item).contactLookupKey);
                        restart();
                        break;
                    case ANOTHER:
                        // user wants to choose new contact for themselves
                        showPickContact(RESULT_PICK_CONTACT_SENDER);
                        break;
                    case NONE:
                        // user wants to remove link to address book
                        ConfigData.savePrefContactLookupKey(getApplicationContext(), null);
                        restart();
                        break;
                    case NEW:
                        // user wants to create new contact
                        showAddContact(ConfigData.loadPrefContactName(getApplicationContext()));
                        break;
                    case EDIT_CONTACT:
                        // user wants to edit contact
                        showEditContact(RESULT_PICK_CONTACT_SENDER);
                        break;
                    case EDIT_NAME:
                        // user wants to edit name
                        showSettings();
                        restart();
                        break;
                }
            }
        });

        return ad;
    }

    private void showProgress(String msg) {
        Bundle args = new Bundle();
        args.putInt(extra.PCT, 0);
        args.putInt(extra.MAX, 0);
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            removeDialog(DIALOG_PROGRESS);
            showDialog(DIALOG_PROGRESS, args);
        }
    }

    private void showProgress(String msg, int maxValue, int newValue) {
        Bundle args = new Bundle();
        args.putInt(extra.PCT, newValue);
        args.putInt(extra.MAX, maxValue);
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            removeDialog(DIALOG_PROGRESS);
            showDialog(DIALOG_PROGRESS, args);
        }
    }

    private Dialog xshowProgress(Activity act, Bundle args) {
        int maxValue = args.getInt(extra.MAX);
        int newValue = args.getInt(extra.PCT);
        String msg = args.getString(extra.RESID_MSG);
        MyLog.i(TAG, msg);

        if (sProg != null) {
            sProg = null;
            sProgressMsg = null;
        }
        sProg = new ProgressDialog(act);
        if (maxValue > 0) {
            sProg.setMax(maxValue);
            sProg.setProgress(newValue);
            sProg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        } else {
            sProg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        if (!TextUtils.isEmpty(msg)) {
            sProg.setMessage(msg);
            sProgressMsg = msg;
        }
        sProg.setCancelable(true);

        return sProg;
    }

    private void showProgressUpdate(String msg) {
        if (sProg != null) {
            if (!sProg.isIndeterminate()) {
                // change from horizontal
                showProgress(msg);
            } else {
                if (!TextUtils.isEmpty(msg)) {
                    sProg.setMessage(msg);
                    sProgressMsg = msg;
                }
            }
        } else {
            showProgress(msg);
        }
    }

    protected void showProgressUpdate(String msg, int maxValue, int newValue) {
        if (sProg != null) {
            if (sProg.isIndeterminate()) {
                // change from indeterminate
                showProgress(msg, maxValue, newValue);
            } else {
                sProg.setProgress(newValue);
                if (!TextUtils.isEmpty(msg)) {
                    sProg.setMessage(msg);
                    sProgressMsg = msg;
                }
            }
        } else {
            showProgress(msg, maxValue, newValue);
        }
    }

    private void showQuestion(String msg, final int requestCode) {
        Bundle args = new Bundle();
        args.putInt(extra.REQUEST_CODE, requestCode);
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            removeDialog(DIALOG_QUESTION);
            showDialog(DIALOG_QUESTION, args);
        }
    }

    protected AlertDialog.Builder xshowQuestion(Activity act, Bundle args) {
        final int requestCode = args.getInt(extra.REQUEST_CODE);
        String msg = args.getString(extra.RESID_MSG);
        MyLog.i(TAG, msg);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_Question);
        ad.setMessage(msg);
        ad.setCancelable(false);
        ad.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                onActivityResult(requestCode, RESULT_OK, null);
            }
        });
        ad.setNegativeButton(R.string.btn_No, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                onActivityResult(requestCode, RESULT_CANCELED, null);
            }
        });
        return ad;
    }

    private boolean loadCurrentPassPhrase() {
        String currentPassPhrase = SafeSlinger.getCachedPassPhrase(ConfigData
                .loadPrefKeyIdString(getApplicationContext()));

        if (sEditPassPhrase != null) {
            currentPassPhrase = sEditPassPhrase;
        }

        return !TextUtils.isEmpty(currentPassPhrase);
    }

    @Override
    public void onBackPressed() {

        final int position = getSupportActionBar().getSelectedNavigationIndex();
        if (MessagesFragment.getRecip() != null && position == Tabs.MESSAGE.ordinal()) {
            // collapse messages to threads when in message view
            MessagesFragment.setRecip(null);
            restart();
        } else {
            // exit when at top level of each tab
            super.onBackPressed();
            showExit(RESULT_CANCELED);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(HomeActivity.this, args).create();
            case DIALOG_ERREXIT:
                return xshowErrorExit(HomeActivity.this, args).create();
            case DIALOG_QUESTION:
                return xshowQuestion(HomeActivity.this, args).create();
            case DIALOG_INTRO:
                return xshowIntroductionInvite(HomeActivity.this, args).create();
            case DIALOG_TUTORIAL:
                return xshowWalkthrough(HomeActivity.this).create();
            case DIALOG_LICENSE_CONFIRM:
                return Eula.show(HomeActivity.this).create();
            case DIALOG_PROGRESS:
                return xshowProgress(HomeActivity.this, args);
            case DIALOG_REFERENCE:
                return xshowReference(HomeActivity.this).create();
            case DIALOG_BACKUPQUERY:
                return xshowBackupQuery(HomeActivity.this).create();
            case DIALOG_USEROPTIONS:
                return xshowChangeSenderOptions(HomeActivity.this, args).create();
        }
        return super.onCreateDialog(id);
    }

}
