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
import android.content.DialogInterface.OnCancelListener;
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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.text.TextUtils;
import android.util.Base64;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.Eula;
import edu.cmu.cylab.starslinger.ExchangeException;
import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.CryptToolsLegacy;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgNonExistingKeyException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPacketSizeException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPeerKeyFormatException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider;
import edu.cmu.cylab.starslinger.exchange.ExchangeActivity;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig;
import edu.cmu.cylab.starslinger.model.ContactImpp;
import edu.cmu.cylab.starslinger.model.CryptoMsgPrivateData;
import edu.cmu.cylab.starslinger.model.ImppValue;
import edu.cmu.cylab.starslinger.model.InboxDbAdapter;
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
import edu.cmu.cylab.starslinger.model.UserData;
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
    private static final String TAG = SafeSlingerConfig.LOG_TAG;

    // constants
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
    public static final int NOTIFY_NEW_MSG_ID = 500;
    public static final int NOTIFY_BACKUP_DELAY_ID = 501;
    public static final int NOTIFY_PASS_CACHED_ID = 502;
    public static final int NOTIFY_SLINGKEYS_REMIND_ID = 503;

    private static final int MS_POLL_INTERVAL = 500;

    // static data
    private static ProgressDialog sProg = null;
    private static Handler sHandler;
    private static MessageData sSendMsg = new MessageData();
    private static String sSenderKey;
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
        COMPOSE, MESSAGE, SLINGKEYS
    }

    private Runnable updateMainView = new Runnable() {

        @Override
        public void run() {
            restoreView();
        }
    };

    private Runnable checkPassExpiration = new Runnable() {

        @Override
        public void run() {

            // uniformly handle pass timeout from any activity...
            if (!showPassphraseWhenExpired()) {
                long remain = SafeSlinger.getPassPhraseCacheTimeRemaining(SafeSlingerPrefs
                        .getKeyIdString());
                if (remain > 0) {
                    sHandler.postDelayed(this, remain);
                }
            }
        }
    };

    private BroadcastReceiver mPushRegReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            endProgress(RECOVERY_PUSHREG);

            String error = intent.getStringExtra(extra.ERROR);
            if (error != null) {
                if (error.equals(C2DMBaseReceiver.ERRREG_SERVICE_NOT_AVAILABLE)) {
                    long backoff = SafeSlingerPrefs.getPusgRegBackoff();
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

        if (SafeSlingerConfig.Intent.ACTION_MESSAGENOTIFY.equals(action)) {
            // clicked on new message notifications window, show messages
            // collapse messages to threads when looking for new messages
            MessagesFragment.setRecip(null);
            setTab(Tabs.MESSAGE);
            restart();

        } else if (SafeSlingerConfig.Intent.ACTION_BACKUPNOTIFY.equals(action)) {
            // clicked on backup reminder notifications window, show reminder
            // query
            showBackupQuery();
            restart();

        } else if (SafeSlingerConfig.Intent.ACTION_SLINGKEYSNOTIFY.equals(action)) {
            // clicked on exchange reminder, show exchange tab
            setTab(Tabs.SLINGKEYS);
            restart();

        } else if (SafeSlingerConfig.Intent.ACTION_CHANGESETTINGS.equals(action)) {

            // clicked on pass cache notification
            showSettings();
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
            sProgressMsg = savedInstanceState.getString(extra.RESID_MSG);
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
                SafeSlingerConfig.Intent.ACTION_MESSAGEUPDATE));

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
        private final FragmentActivity mAct;
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

        public TabsAdapter(FragmentActivity activity, ActionBar bar, ViewPager pager) {
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
                                cf.updateKeypad();
                                cf.updateValues(getComposeArgs());
                            }
                            break;
                        case MESSAGE:
                            MessagesFragment mf = (MessagesFragment) findFragmentByPosition(Tabs.MESSAGE
                                    .ordinal());
                            if (mf != null) {
                                mf.updateKeypad();
                                mf.updateValues(getMessagesArgs());
                            }
                            break;
                        case SLINGKEYS:
                            SlingerFragment sf = (SlingerFragment) findFragmentByPosition(Tabs.SLINGKEYS
                                    .ordinal());
                            if (sf != null) {
                                sf.updateKeypad();
                                sf.updateValues(getSlingerArgs());
                                if (SafeSlingerPrefs.getShowWalkthrough()) {
                                    sf.showWalkthrough();
                                }
                            }
                            break;
                        default:
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
                        cf.updateKeypad();
                        cf.updateValues(getComposeArgs());
                    }
                    break;
                case MESSAGE:
                    MessagesFragment mf = (MessagesFragment) findFragmentByPosition(Tabs.MESSAGE
                            .ordinal());
                    if (mf != null) {
                        mf.updateKeypad();
                        mf.updateValues(getMessagesArgs());
                    }
                    break;
                case SLINGKEYS:
                    SlingerFragment sf = (SlingerFragment) findFragmentByPosition(Tabs.SLINGKEYS
                            .ordinal());
                    if (sf != null) {
                        sf.updateKeypad();
                        sf.updateValues(getSlingerArgs());
                    }

                    break;
                default:
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
                    .findFragmentByPosition(Tabs.SLINGKEYS.ordinal());
            if (sf != null) {
                sf.updateValues(getSlingerArgs());
            }
        }
    }

    private static Bundle getSlingerArgs() {
        Bundle args = new Bundle();
        String contactLookupKey = SafeSlingerPrefs.getContactLookupKey();

        // valid key and push token is required
        SlingerIdentity si = new SlingerIdentity(SafeSlingerPrefs.getPushRegistrationId(),
                SSUtil.getLocalNotification(SafeSlinger.getApplication()), sSenderKey);
        final String token = SlingerIdentity.sidPush2DBPush(si);
        final String pubkey = SlingerIdentity.sidKey2DBKey(si);
        if (TextUtils.isEmpty(token)) {
            return args;
        } else if (TextUtils.isEmpty(pubkey)) {
            return args;
        }

        ArrayList<ImppValue> impps = new ArrayList<ImppValue>();
        impps.add(new ImppValue(SafeSlingerConfig.APP_KEY_PUSHTOKEN, SSUtil.finalEncode(token
                .getBytes())));
        impps.add(new ImppValue(SafeSlingerConfig.APP_KEY_PUBKEY, SSUtil.finalEncode(pubkey
                .getBytes())));
        args.putAll(writeSingleExportExchangeArgs(new ContactImpp(contactLookupKey, impps)));

        return args;
    }

    private void showExchange(byte[] userData) {
        Intent intent = new Intent(HomeActivity.this, ExchangeActivity.class);
        intent.putExtra(ExchangeConfig.extra.USER_DATA, userData);
        intent.putExtra(ExchangeConfig.extra.HOST_NAME, SafeSlingerConfig.HTTPURL_EXCHANGE_HOST);
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
        if (SafeSlingerPrefs.getCurrentRecipientDBVer() < 6) {
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
                SafeSlingerPrefs.setCurrentRecipientDBVer(RecipientDatabaseHelper.DATABASE_VERSION);
            }
        }

        if (SafeSlingerPrefs.getCurrentMessageDBVer() < 6) {
            Cursor c = dbMessage.fetchAllMessagesUpgradeTo6();
            if (c != null) {
                while (c.moveToNext()) {
                    long rowId = c.getLong(c.getColumnIndexOrThrow(MessageDbAdapter.KEY_ROWID));
                    long keyid_long = c.getLong(c
                            .getColumnIndexOrThrow(MessageDbAdapter.KEY_KEYIDLONG));
                    dbMessage.updateMessageKeyId2String(rowId, keyid_long);
                }
                c.close();
                SafeSlingerPrefs.setCurrentMessageDBVer(MessageDatabaseHelper.DATABASE_VERSION);
            }
        }

        // look for old messages without key ids read out
        if (dbMessage.getVersion() >= 6) {
            Cursor cm = dbMessage.fetchAllMessagesByThread(null);
            if (cm != null) {
                while (cm.moveToNext()) {
                    MessageRow messageRow = new MessageRow(cm, false);
                    byte[] encMsg = messageRow.getEncBody();
                    if (encMsg != null) {
                        CryptoMsgProvider tool = CryptoMsgProvider.createInstance(SafeSlinger
                                .isLoggable());
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
                && !SSUtil.fileExists(getApplicationContext(), CryptTools.getCurrentKeyFile())) {
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
            if (SSUtil.fileExists(getApplicationContext(), CryptTools.getCurrentKeyFile())) {
                showNote(R.string.state_BackupRestored);
            }
        }

        // show eula?
        if (!SafeSlingerPrefs.getEulaAccepted()) {
            if (!isFinishing()) {
                removeDialog(DIALOG_LICENSE_CONFIRM);
                showDialog(DIALOG_LICENSE_CONFIRM);
            }
            return false;
        }

        // we passed backup restore, now we can load databases...
        doUpgradeDatabaseInPlace();

        // contact
        String contactName = SafeSlingerPrefs.getContactName();
        if (TextUtils.isEmpty(contactName)) {
            showFindContact();
            return false;
        }

        // we need a real persons name
        if (!SafeSlingerConfig.isNameValid(contactName)) {
            showNote(R.string.error_InvalidContactName);
            showFindContact();
            return false;
        }

        boolean dateChanged = SSUtil.isDayChanged(SafeSlingerPrefs.getContactDBLastScan());
        if (!sAppOpened || dateChanged) {
            sAppOpened = true;
            SafeSlingerPrefs.setThisVersionOpened();
            runThreadBackgroundSyncUpdates();
        }

        // init local struct...
        int notify = SSUtil.getLocalNotification(getApplicationContext());
        String token = SafeSlingerPrefs.getPushRegistrationId();

        // Determine if user wants to have a valid push token or not
        if (notify == SafeSlingerConfig.NOTIFY_NOPUSH && TextUtils.isEmpty(token)) {
            showQuestion(getString(R.string.ask_GoogleAccountToReceiveMsgs), REQUEST_QRECEIVE_MGS);
            return false;
        }

        // if push token bad....
        // ...request a push token... (restart)
        if (notify != SafeSlingerConfig.NOTIFY_NOPUSH) {
            if (TextUtils.isEmpty(token) && notify == SafeSlingerConfig.NOTIFY_ANDROIDC2DM) {
                // ensure that user has registered with push service...
                runThreadGetPushReg();
                return false;
            }
        }

        // look for proper key
        boolean hasSecretKey = CryptTools.existsSecretKey(getApplicationContext());
        if (!hasSecretKey && SafeSlingerPrefs.getUser() == 0) {
            // key not found, try loading older version for base user only
            hasSecretKey = CryptToolsLegacy.existsSecretKeyOld();
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

        // pass is good, determine what view to see...
        if (sHandler == null) {
            sHandler = new Handler();
        }
        sHandler.removeCallbacks(updateMainView);
        sHandler.post(updateMainView);

        return true;
    }

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

        if (sProg != null && sProg.isShowing()) {
            outState.putInt(extra.PCT, sProg.isIndeterminate() ? 0 : sProg.getProgress());
            outState.putInt(extra.MAX, sProg.isIndeterminate() ? 0 : sProg.getMax());
            outState.putString(extra.RESID_MSG, sProgressMsg);
        } else {
            outState.remove(extra.PCT);
            outState.remove(extra.MAX);
            outState.remove(extra.RESID_MSG);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuItem item = menu.add(0, MENU_SENDINTRO, 0, R.string.title_SecureIntroduction).setIcon(
                R.drawable.ic_action_secintro);
        MenuCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0, MENU_SENDINTRO, 0, R.string.title_SecureIntroduction).setIcon(
                R.drawable.ic_action_secintro);
        menu.add(0, MENU_CONTACTINVITE, 0, R.string.menu_SelectShareApp).setIcon(
                R.drawable.ic_action_add_person);
        menu.add(0, MENU_FEEDBACK, 0, R.string.menu_sendFeedback).setIcon(
                android.R.drawable.ic_menu_send);
        menu.add(0, MENU_LOGOUT, 0, R.string.menu_Logout).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
        menu.add(0, MENU_SETTINGS, 0, R.string.menu_Settings).setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.add(0, MENU_REFERENCE, 0, R.string.menu_Help).setIcon(android.R.drawable.ic_menu_help);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_LOGOUT:
                // remove cached pass
                doLogout();
                return true;
            case MENU_CONTACTINVITE:
                showAddContactInvite();
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

    private void doLogout() {
        reInitForExit();
        SafeSlinger.removeCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
        SafeSlinger.startCacheService(HomeActivity.this);
        showPassPhrase(false, false);
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

            // save public portion
            sSenderKey = mine.getSafeSlingerString();

            SafeSlingerPrefs.setKeyIdString(mine.getKeyId());
            SafeSlingerPrefs.setKeyDate(mine.getGenDate());

            // save private portion in secret key storage...
            CryptTools.putSecretKey(mine, sEditPassPhrase);

            // update cache to avoid entering twice...
            SafeSlinger.setCachedPassPhrase(sKeyData.GetSelfKeyid(), sEditPassPhrase);
            updatePassCacheTimer();

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
                    sKeyData = CryptoMsgProvider.createInstance(SafeSlinger.isLoggable());
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
                MessageRow msg = new MessageRow(c, false);
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
            MessageRow queued = new MessageRow(c, false);
            c.close();
            if (queued.getStatus() != MessageDbAdapter.MESSAGE_STATUS_QUEUED) {
                showNote(R.string.error_UnableToUpdateMessageInDB);
                restart();
                return;
            }
        }

        // attempt to update messages if in view...
        Intent sendIntent = new Intent(SafeSlingerConfig.Intent.ACTION_MESSAGEUPDATE);
        sendIntent.putExtra(extra.MESSAGE_ROW_ID, sendMsg.getRowId());
        sendIntent.putExtra(extra.RECIPIENT_ROW_ID, recip.getRowId());
        getApplicationContext().sendBroadcast(sendIntent);

        // switch to message tab
        setTab(Tabs.MESSAGE);
        restart();

        // start background task to send
        SendFileTask task = new SendFileTask();
        task.execute(new MessageTransport(recip, sendMsg));
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
                            // we can only wait for an error, or success
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
                            if (filesize <= SafeSlingerConfig.MAX_TEXTMESSAGE) {
                                sSendMsg.removeFile();
                                sSendMsg.setText(extra_text.toString());
                            } else {
                                sSendMsg.setFileType("text/plain");
                                final byte[] textBytes = extra_text.toString().getBytes();
                                sSendMsg.setFileData(textBytes);
                                sSendMsg.setFileSize(textBytes.length);
                                SimpleDateFormat sdf = new SimpleDateFormat(
                                        SafeSlingerConfig.DATETIME_FILENAME, Locale.US);
                                sSendMsg.setFileName(sdf.format(new Date()) + ".txt");
                                sSendMsg.removeText();
                            }
                        }
                    }

                    if (filesize <= 0) {
                        showErrorExit(R.string.error_CannotSendEmptyFile);
                        return false;
                    }
                    if (filesize > SafeSlingerConfig.MAX_FILEBYTES) {
                        showErrorExit(String.format(getString(R.string.error_CannotSendFilesOver),
                                SafeSlingerConfig.MAX_FILEBYTES));
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
                    contentType = SafeSlingerConfig.MIMETYPE_OPEN_ATTACH_DEF;
                }
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[4096];
        while (is.read(buf) > -1) {
            baos.write(buf);
        }
        baos.flush();

        final byte[] fileBytes = baos.toByteArray();
        sSendMsg.setFileData(fileBytes);
        sSendMsg.setFileSize(fileBytes.length);
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
                        MessageRow msg = new MessageRow(c, false);
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
                // create new
                MessageData sendMsg = sSendMsg;
                sendMsg.setText(text);
                // remove draft
                sSendMsg = new MessageData();
                // user wants to post the file and notify recipient
                if (sRecip == null) {
                    showNote(R.string.error_InvalidRecipient);
                    restart();
                    break;
                }
                if (sRecip.getNotify() == SafeSlingerConfig.NOTIFY_NOPUSH) {
                    showNote(R.string.error_InvalidRecipient);
                    restart();
                    break;
                }
                doSendFileStart(sRecip, sendMsg);
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
        long rowIdInbox = -1;
        if (data != null) {
            text = data.getString(extra.TEXT_MESSAGE);
            rowIdMessage = data.getLong(extra.MESSAGE_ROW_ID, -1);
            rowIdInbox = data.getLong(extra.INBOX_ROW_ID, -1);
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
                        MessageRow msg = new MessageRow(c, false);
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
                if (saveMsg.getRowId() > -1 && saveMsg.getRowId() == sSendMsg.getRowId()) {
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
                if (recip.getNotify() == SafeSlingerConfig.NOTIFY_NOPUSH) {
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
                    MessageRow edit = new MessageRow(cfm, false);
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
                    MessageRow edit = new MessageRow(cem, false);
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
                MessageData inbox = new MessageData();
                inbox.setRowId(rowIdInbox);
                inbox.setMsgHash(data.getString(extra.PUSH_MSG_HASH));

                if (inbox.getMsgHash() != null) {
                    GetMessageTask getMessageTask = new GetMessageTask();
                    getMessageTask.execute(inbox);
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
                showExchange(data.getString(ExchangeConfig.extra.USER_DATA).getBytes());
                break;
            case SlingerFragment.RESULT_USEROPTIONS:
                showChangeSenderOptions();
                break;
        }
    }

    /***
     * @return When returns false we allow a new key to be generated.
     */
    protected boolean doPassEntryCheck(String pass, String passOld, boolean changePass) {
        CryptoMsgPrivateData mine = null;
        boolean changeSuccess = false;

        // check for legacy support, migrate if needed
        if (!CryptTools.existsSecretKey(getApplicationContext())) {
            // key not found, try migrating older version
            if (!CryptToolsLegacy.updateKeyFormatOld(pass)) {
                // unable to migrate old key, just continue...
            }
        }

        // decrypt correct private key
        try {
            mine = CryptTools.getSecretKey(changePass ? passOld : pass);
        } catch (IOException e) {
            e.printStackTrace(); // key not found
            // The only valid reason to generate key is when it does not exist,
            // when it should exist.
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // unable to deserialize same key format
            showNote(R.string.error_couldNotExtractPrivateKey);
        } catch (CryptoMsgException e) {
            e.printStackTrace(); // key formatted incorrectly
            showNote(R.string.error_couldNotExtractPrivateKey);
        }

        // if change, confirm unlock, and resave
        if (changePass) {
            try {
                changeSuccess = CryptTools.changeSecretKeyPassphrase(pass, passOld);
            } catch (IOException e) {
                e.printStackTrace(); // key not found
            } catch (ClassNotFoundException e) {
                e.printStackTrace(); // unable to deserialize same key format
                showNote(R.string.error_couldNotExtractPrivateKey);
            } catch (CryptoMsgException e) {
                e.printStackTrace(); // key formatted incorrectly
                showNote(R.string.error_couldNotExtractPrivateKey);
            }
            if (changeSuccess) {
                SafeSlinger.setCachedPassPhrase(SafeSlingerPrefs.getKeyIdString(), pass);
                updatePassCacheTimer();
                showNote(R.string.state_PassphraseUpdated);
                setPassphraseStatus(true);
            } else {
                setPassphraseStatus(false);
            }
        } else { // check against current key
            if (mine != null) {
                SafeSlinger.setCachedPassPhrase(SafeSlingerPrefs.getKeyIdString(), pass);
                updatePassCacheTimer();
                setPassphraseStatus(true);
                // save loaded pub key for slinging keys later
                sSenderKey = mine.getSafeSlingerString();
            } else {
                setPassphraseStatus(false);
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

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
                                String oldPass = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs
                                        .getKeyIdString());

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
                                    String p = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs
                                            .getKeyIdString());
                                    if (!TextUtils.isEmpty(p)) {
                                        showPassPhrase(false, true);
                                    } else {
                                        showPassPhraseVerify();
                                    }
                                    break;
                                }

                                // if requested, and logged in, try to decrypt
                                // messages
                                if (SafeSlingerPrefs.getAutoDecrypt()) {
                                    runThreadDecryptPending(pass);
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
                        SafeSlingerPrefs
                                .setPushRegistrationIdWriteOnlyC2dm(SafeSlingerConfig.NOTIFY_NOPUSH_TOKENDATA);
                        restart();
                        break;
                }
                break;

            case VIEW_RECIPSEL_ID:
                switch (resultCode) {
                    case PickRecipientsActivity.RESULT_SLINGKEYS:
                        setTab(Tabs.SLINGKEYS);
                        restart();
                        break;
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
                        SafeSlinger.removeCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
                        SafeSlinger.startCacheService(HomeActivity.this);
                        showPassPhraseVerify();
                        break;
                    case SettingsActivity.RESULT_LOGOUT:
                        doLogout();
                        break;
                    case SettingsActivity.RESULT_DELETE_KEYS:
                        // allow deletion of newer keys only
                        showManagePassphrases(getMoreRecentKeys());
                        break;
                    case SettingsActivity.RESULT_CHANGE_PASSTTL:
                        // update timer since ttl has changed
                        updatePassCacheTimer();
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
                            if (recip1.getNotify() == SafeSlingerConfig.NOTIFY_NOPUSH
                                    || recip2.getNotify() == SafeSlingerConfig.NOTIFY_NOPUSH) {
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
                            sendMsg1.setFileSize(vCard2.length);
                            sendMsg1.setFileName(SafeSlingerConfig.INTRODUCTION_VCF);
                            sendMsg1.setFileType(SafeSlingerConfig.MIMETYPE_CLASS + "/"
                                    + SafeSlingerConfig.MIMETYPE_FUNC_SECINTRO);

                            sendMsg2.setFileData(vCard1);
                            sendMsg2.setFileSize(vCard1.length);
                            sendMsg2.setFileName(SafeSlingerConfig.INTRODUCTION_VCF);
                            sendMsg2.setFileType(SafeSlingerConfig.MIMETYPE_CLASS + "/"
                                    + SafeSlingerConfig.MIMETYPE_FUNC_SECINTRO);

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
                    case ExchangeActivity.RESULT_EXCHANGE_OK:
                        showSave(data.getExtras());
                        break;
                    case ExchangeActivity.RESULT_EXCHANGE_CANCELED:
                        restart();
                        break;
                }
                break;

            case VIEW_SAVE_ID:
                switch (resultCode) {
                    case SaveActivity.RESULT_SAVE:
                        // locally store trusted exchanged items
                        SafeSlingerPrefs.setFirstExchangeComplete(true);
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
                                SafeSlingerPrefs.setContactLookupKey(contactLookupKey);
                                SafeSlingerPrefs.setContactName(name);
                            }
                            c.close();
                        }
                        restart();
                        break;
                    case RESULT_CANCELED:
                        restart();
                        break;
                    default:
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
                    default:
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
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updatePassCacheTimer() {
        SafeSlinger.startCacheService(HomeActivity.this);
        long remain = SafeSlinger
                .getPassPhraseCacheTimeRemaining(SafeSlingerPrefs.getKeyIdString());
        if (remain > 0) {
            if (sHandler == null) {
                sHandler = new Handler();
            }
            sHandler.removeCallbacks(checkPassExpiration);
            sHandler.postDelayed(checkPassExpiration, remain);
        }
    }

    private ArrayList<UserData> getMoreRecentKeys() {
        ArrayList<UserData> recentKeys = new ArrayList<UserData>();
        int totalUsers = SafeSlinger.getTotalUsers();
        for (int i = 0; i < totalUsers; i++) {
            long date = SafeSlingerPrefs.getKeyDate(i);
            String name = SafeSlingerPrefs.getContactName(i);
            if (i > SafeSlingerPrefs.getUser()) {
                recentKeys.add(new UserData(name, date, false));
            }
        }
        return recentKeys;
    }

    private void runThreadDecryptPending(final String pass) {

        final Handler decMsgHandler = new Handler(new Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                postProgressMsgList(msg.arg1 == 1, msg.arg2, getString(R.string.prog_decrypting));
                return false;
            }
        });

        final Handler doneMsgHandler = new Handler(new Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                postProgressMsgList(msg.arg1 == 1, msg.arg2, null);
                return false;
            }
        });

        Thread t = new Thread() {

            @Override
            public void run() {
                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
                InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(getApplicationContext());
                Message msg = new Message();

                Cursor c = dbInbox.fetchAllInboxDecryptPending();
                if (c != null) {
                    while (c.moveToNext()) {
                        try {
                            MessageData inRow = new MessageRow(c, true);
                            StringBuilder keyidout = new StringBuilder();
                            msg = new Message();
                            msg.arg1 = inRow.isInboxTable() ? 1 : 0;
                            msg.arg2 = (int) inRow.getRowId();
                            decMsgHandler.sendMessage(msg);

                            byte[] plain = CryptTools.decryptMessage(inRow.getEncBody(), pass,
                                    keyidout);
                            MessagePacket push = new MessagePacket(plain);

                            // move encrypted message to decrypted storage...
                            // add decrypted
                            long rowIdMsg = dbMessage.createMessageDecrypted(inRow, push,
                                    keyidout.toString());
                            if (rowIdMsg == -1) {
                                return; // unable to save progress
                            } else {
                                // remove encrypted
                                dbInbox.deleteInbox(inRow.getRowId());
                            }
                            msg = new Message();
                            msg.arg1 = inRow.isInboxTable() ? 1 : 0;
                            msg.arg2 = (int) inRow.getRowId();
                            doneMsgHandler.sendMessage(msg);

                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (CryptoMsgException e) {
                            e.printStackTrace();
                        } catch (GeneralException e) {
                            e.printStackTrace();
                        }
                    }
                    c.close();
                }
            }
        };
        t.start();
    }

    private void doLoadAttachment(String path) throws FileNotFoundException {
        File phy = new File(path); // physical
        File vir = new File(path); // virtual, change if needed
        FileInputStream is = new FileInputStream(phy.getAbsolutePath());

        try {
            byte[] outFileData = new byte[is.available()];
            is.read(outFileData);
            sSendMsg.setFileData(outFileData);
            sSendMsg.setFileSize(outFileData.length);

            if (sSendMsg.getFileSize() > SafeSlingerConfig.MAX_FILEBYTES) {
                is.close();
                showNote(String.format(getString(R.string.error_CannotSendFilesOver),
                        SafeSlingerConfig.MAX_FILEBYTES));
                restart();
                return;
            }

            String type = URLConnection.guessContentTypeFromStream(is);
            if (type != null)
                sSendMsg.setFileType(type);
            else {
                String extension = SSUtil.getFileExtensionOnly(vir.getName());
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (type != null) {
                    sSendMsg.setFileType(type);
                } else {
                    sSendMsg.setFileType(SafeSlingerConfig.MIMETYPE_OPEN_ATTACH_DEF);
                }
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

    private void doSelectRecipientPostExchange(Bundle args, int recipSource) {
        // check for presence of one key only to pre-select recipient
        CryptoMsgProvider p = CryptoMsgProvider.createInstance(SafeSlinger.isLoggable());
        byte[] keyBytes = null;
        String keyStr = null;
        String keyId = null;
        int exchanged = 0;
        do {
            keyBytes = args.getByteArray(SafeSlingerConfig.APP_KEY_PUBKEY + exchanged);
            if (keyBytes != null) {
                keyStr = new String(keyBytes);
                exchanged++;
            }
        } while (keyBytes != null);

        if (!TextUtils.isEmpty(keyStr)) {
            try {
                keyId = p.ExtractKeyIDfromSafeSlingerString(keyStr);
            } catch (CryptoMsgPeerKeyFormatException e) {
                e.printStackTrace();
            }
        }

        if (exchanged == 1 && !TextUtils.isEmpty(keyId)
                && recipSource == RecipientDbAdapter.RECIP_SOURCE_EXCHANGE) {
            RecipientDbAdapter dbRecipient = RecipientDbAdapter
                    .openInstance(getApplicationContext());
            Cursor cr = dbRecipient.fetchRecipientByKeyId(keyId);
            if (cr != null) {
                sRecip = new RecipientRow(cr);
                cr.close();
            }
            if (sRecip != null) {
                sSendMsg = new MessageData();
                setTab(Tabs.COMPOSE);
                restart();
            }
        }
    }

    private void doProcessSafeSlingerMimeType(MessageData recvMsg) {
        String[] types = recvMsg.getFileType().split("/");
        if (types.length == 2) {
            if (types[1].compareToIgnoreCase(SafeSlingerConfig.MIMETYPE_FUNC_SECINTRO) == 0) {

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
                    inviteMsg = new MessageRow(c, false);
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

                    doSelectRecipientPostExchange(args, recipSource);
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
                SafeSlingerPrefs.setContactDBLastScan(System.currentTimeMillis());
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
                        SafeSlingerConfig.APP_KEY_OLD1, //
                        SafeSlingerConfig.APP_KEY_OLD2, //
                        SafeSlingerConfig.APP_KEY_PUBKEY, //
                        SafeSlingerConfig.APP_KEY_PUSHTOKEN, //
                };
                doCleanupOldKeyData(keyNames);
            }
        };
        t.start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // require pass on wake...
        if (hasFocus) {
            showPassphraseWhenExpired();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreView();

        // require pass on wake...
        showPassphraseWhenExpired();
    }

    private boolean showPassphraseWhenExpired() {
        String contactName = SafeSlingerPrefs.getContactName();
        if (CryptTools.existsSecretKey(getApplicationContext()) && !TextUtils.isEmpty(contactName)) {
            if (SafeSlinger.isCacheEmpty()) {
                if (SafeSlinger.isAppVisible()) {
                    showPassPhrase(false, false);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        restoreView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

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

    protected void postProgressMsgList(boolean inInboxTable, long rowId, String msg) {
        try {
            if (mTabsAdapter != null) {
                MessagesFragment mf = (MessagesFragment) mTabsAdapter
                        .findFragmentByPosition(Tabs.MESSAGE.ordinal());
                if (mf != null) {
                    mf.postProgressMsgList(inInboxTable, rowId, msg);
                }
            }
        } catch (IllegalStateException e) {
            // Can not perform this action after onSaveInstanceState...
            e.printStackTrace();
        }
    }

    private class GetMessageTask extends AsyncTask<MessageData, String, String> {
        private long mRowId;
        private WebEngine mWeb = new WebEngine(HomeActivity.this,
                SafeSlingerConfig.HTTPURL_MESSENGER_HOST);
        private MessageData mInbox;

        @Override
        protected String doInBackground(MessageData... mds) {
            mInbox = mds[0];
            mRowId = mInbox.getRowId();

            try {
                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
                InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(getApplicationContext());

                if (!SafeSlinger.getApplication().isOnline()) {
                    return getString(R.string.error_CorrectYourInternetConnection);
                }

                publishProgress(getString(R.string.prog_RequestingMessage));
                String pass = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
                byte[] msgHashBytes;
                try {
                    msgHashBytes = Base64.decode(mInbox.getMsgHash().getBytes(), Base64.NO_WRAP);
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

                if (Arrays.equals(msgHashBytes, CryptTools.computeSha3Hash(encMsg))) {
                    CryptoMsgProvider tool = CryptoMsgProvider.createInstance(SafeSlinger
                            .isLoggable());
                    String keyid = null;
                    try {
                        keyid = tool.ExtractKeyIDfromPacket(encMsg);
                    } catch (CryptoMsgPacketSizeException e) {
                        e.printStackTrace();
                    }
                    // save downloaded initial message
                    if (!dbInbox.updateInboxDownloaded(mRowId, encMsg,
                            MessageDbAdapter.MESSAGE_IS_SEEN, keyid)) {
                        return getString(R.string.error_UnableToUpdateMessageInDB);
                    }
                } else {
                    return getString(R.string.error_InvalidMsg);
                }

                publishProgress(getString(R.string.prog_decrypting));
                StringBuilder keyIdOut = new StringBuilder();
                byte[] rawMsg = CryptTools.decryptMessage(encMsg, pass, keyIdOut);
                MessagePacket push = new MessagePacket(rawMsg);
                mInbox.setFileHash(push.getFileHash());

                // move encrypted message to decrypted storage...
                // add decrypted
                long rowIdMsg = dbMessage.createMessageDecrypted(mInbox, push, keyIdOut.toString());
                if (rowIdMsg == -1) {
                    return getString(R.string.error_UnableToSaveMessageInDB);
                } else {
                    // remove encrypted
                    dbInbox.deleteInbox(mRowId);
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
                InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(getApplicationContext());
                if (!dbInbox.updateInboxExpired(mInbox.getRowId())) {
                    return getString(R.string.error_UnableToUpdateMessageInDB);
                }

            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            postProgressMsgList(mInbox.isInboxTable(), mRowId, progress[0]);
        }

        @Override
        protected void onPostExecute(String error) {
            postProgressMsgList(mInbox.isInboxTable(), mRowId, null);
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
        private WebEngine mWeb = new WebEngine(HomeActivity.this,
                SafeSlingerConfig.HTTPURL_MESSENGER_HOST);
        private Handler mHandler = new Handler();
        private int mRxTotalSize = 0;
        private MessageData mRecvMsg;
        private Runnable mUpdateRxProgress = new Runnable() {

            @Override
            public void run() {
                int rxCurr = (int) mWeb.get_rxCurrentBytes();
                if (rxCurr > 0) {
                    int pct = (int) ((rxCurr / (float) mRxTotalSize) * 100);
                    if (pct > 0 && pct < 100) {
                        postProgressMsgList(mRecvMsg.isInboxTable(), mRowId, String.format(
                                "%s %d%%", getString(R.string.prog_ReceivingFile), pct));
                    } else {
                        postProgressMsgList(mRecvMsg.isInboxTable(), mRowId,
                                String.format("%s", getString(R.string.prog_ReceivingFile)));
                    }
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
                mRxTotalSize += 4; // version size

                if (!SafeSlinger.getApplication().isOnline()) {
                    return getString(R.string.error_CorrectYourInternetConnection);
                }

                publishProgress(getString(R.string.prog_RequestingFile));
                String pass = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
                byte[] encFile = null;
                byte[] msgHashBytes;
                try {
                    msgHashBytes = Base64.decode(mRecvMsg.getMsgHash().getBytes(), Base64.NO_WRAP);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    return getString(R.string.error_InvalidIncomingMessage);
                }

                mRxTotalSize += 4; // file len size
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
                    return getString(R.string.error_InvalidMsg);
                }

                publishProgress(getString(R.string.prog_decrypting));
                StringBuilder keyIdOut = new StringBuilder();
                byte[] rawFile = CryptTools.decryptMessage(encFile, pass, keyIdOut);

                if (rawFile != null) {
                    mRecvMsg.setFileData(rawFile);
                    mRecvMsg.setFileSize(rawFile.length);

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
            postProgressMsgList(mRecvMsg.isInboxTable(), mRowId, progress[0]);
        }

        @Override
        protected void onPostExecute(String error) {
            mHandler.removeCallbacks(mUpdateRxProgress);
            postProgressMsgList(mRecvMsg.isInboxTable(), mRowId, null);
            if (TextUtils.isEmpty(error)) {
                if (mRecvMsg.getFileType().startsWith(SafeSlingerConfig.MIMETYPE_CLASS + "/")) {
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
        private WebEngine mWeb = new WebEngine(HomeActivity.this,
                SafeSlingerConfig.HTTPURL_MESSENGER_HOST);
        private Handler mHandler = new Handler();
        private int mTxTotalSize = 0;
        private Runnable mUpdateTxProgress = new Runnable() {

            @Override
            public void run() {
                int txCurr = (int) mWeb.get_txCurrentBytes();
                int pct = (int) ((txCurr / (float) mTxTotalSize) * 100);
                String str = String.format(getString(R.string.prog_SendingFile), "");
                if (pct > 0 && pct < 100) {
                    postProgressMsgList(mSendMsg.isInboxTable(), mRowId,
                            String.format("%s %d%%", str, pct));
                } else {
                    postProgressMsgList(mSendMsg.isInboxTable(), mRowId, String.format("%s", str));
                }
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
                mTxTotalSize += 4; // version size

                // encrypt file data...
                publishProgress(getString(R.string.prog_encrypting));
                String pass = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
                byte[] encFile = null;
                byte[] encMsg = null;
                byte[] rawFile = mSendMsg.getFileData();
                if (rawFile != null && rawFile.length > 0) {
                    encFile = CryptTools.encryptMessage(rawFile, pass, mRecip.getPubkey());
                } else {
                    encFile = new byte[0];
                }
                mTxTotalSize += 4; // file len size
                mTxTotalSize += encFile.length;

                // format message data, including hash of encrypted file
                // data...
                publishProgress(getString(R.string.prog_generatingSignature));
                byte[] msgData = new MessagePacket(SafeSlingerConfig.getVersionCode(),//
                        System.currentTimeMillis(), //
                        encFile.length,//
                        mSendMsg.getFileName(), //
                        mSendMsg.getFileType(),//
                        mSendMsg.getText(),//
                        SafeSlingerPrefs.getContactName(),//
                        CryptTools.computeSha3Hash(encFile)//
                ).getBytes();
                if (msgData == null) {
                    throw new GeneralException(getString(R.string.error_InvalidMsg));
                }

                // encrypt message data...
                publishProgress(getString(R.string.prog_encrypting));
                encMsg = CryptTools.encryptMessage(msgData, pass, mRecip.getPubkey());
                mTxTotalSize += 4; // msg len size
                mTxTotalSize += encMsg.length;

                // message id = hash of encrypted message data...
                publishProgress(getString(R.string.prog_generatingSignature));
                byte[] msgHashBytes = CryptTools.computeSha3Hash(encMsg);
                mSendMsg.setMsgHash(new String(Base64.encode(msgHashBytes, Base64.NO_WRAP)));
                mTxTotalSize += 4; // hash len size
                mTxTotalSize += msgHashBytes.length;

                mTxTotalSize += 4; // token len size
                mTxTotalSize += mRecip.getPushtoken().length();

                // send...
                mHandler.postDelayed(mUpdateTxProgress, MS_POLL_INTERVAL);
                mWeb.postMessage(msgHashBytes, encMsg, encFile, mRecip.getPushtoken(),
                        mRecip.getNotify());
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
            postProgressMsgList(mSendMsg.isInboxTable(), mRowId, progress[0]);
        }

        @Override
        protected void onPostExecute(String error) {
            mHandler.removeCallbacks(mUpdateTxProgress);
            postProgressMsgList(mSendMsg.isInboxTable(), mRowId, null);
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
        private WebEngine mWeb = new WebEngine(HomeActivity.this,
                SafeSlingerConfig.HTTPURL_MESSENGER_HOST);
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
                    case SafeSlingerConfig.NOTIFY_APPLEUA:
                        mWeb.checkStatusAppleUA(recip.getPushtoken());
                        break;
                    default:
                        // do nothing for non-UA types
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
                C2DMessaging.register(getApplicationContext(),
                        SafeSlingerConfig.PUSH_SENDERID_EMAIL);
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
        SafeSlingerPrefs.setPusgRegBackoff(SafeSlingerPrefs.DEFAULT_PUSHREG_BACKOFF);
        sProg = null;
        sProgressMsg = null;
        sRecip = null;
        sSenderKey = null;
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
        contentIntent.setType(SafeSlingerConfig.MIMETYPE_ADD_ATTACH);
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
        String name = SafeSlingerPrefs.getContactName();

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
        String contactLookupKey = SafeSlingerPrefs.getContactLookupKey();
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
        if (!SafeSlinger.isPassphraseOpen()) {
            if (sHandler != null) {
                sHandler.removeCallbacks(checkPassExpiration);
            }
            Intent intent = new Intent(HomeActivity.this, PassPhraseActivity.class);
            intent.putExtra(extra.USER_TOTAL, SafeSlinger.getTotalUsers());
            intent.putExtra(extra.CREATE_PASS_PHRASE, create);
            intent.putExtra(extra.CHANGE_PASS_PHRASE, change);
            intent.putExtra(extra.VERIFY_PASS_PHRASE, false);
            int view = change ? VIEW_PASSPHRASE_CHANGE_ID : VIEW_PASSPHRASE_ID;
            startActivityForResult(intent, view);
            SafeSlinger.setPassphraseOpen(true);
        }
    }

    private void showPassPhraseVerify() {
        if (!SafeSlinger.isPassphraseOpen()) {
            if (sHandler != null) {
                sHandler.removeCallbacks(checkPassExpiration);
            }
            Intent intent = new Intent(HomeActivity.this, PassPhraseActivity.class);
            intent.putExtra(extra.USER_TOTAL, SafeSlinger.getTotalUsers());
            intent.putExtra(extra.CREATE_PASS_PHRASE, false);
            intent.putExtra(extra.CHANGE_PASS_PHRASE, false);
            intent.putExtra(extra.VERIFY_PASS_PHRASE, true);
            startActivityForResult(intent, VIEW_PASSPHRASE_VERIFY_ID);
            SafeSlinger.setPassphraseOpen(true);
        }
    }

    private void showSendIntroduction() {
        Intent intent = new Intent(HomeActivity.this, IntroductionActivity.class);
        startActivityForResult(intent, VIEW_SENDINVITE_ID);
    }

    private void showManagePassphrases(ArrayList<UserData> recentKeys) {
        StringBuilder msg = new StringBuilder();
        long myKeyDate = SafeSlingerPrefs.getKeyDate();
        msg.append(String.format(getString(R.string.label_WarnManagePassOnlyRecentDeleted),
                new Date(myKeyDate).toLocaleString()));
        msg.append(" ");
        msg.append(getString(R.string.label_WarnManagePassFollowsAreRecent));
        msg.append("\n");
        if (recentKeys.size() == 0) {
            msg.append("  ");
            msg.append(getString(R.string.label_WarnManagePassNoMoreRecent));
        } else {
            for (UserData key : recentKeys) {
                msg.append("  ");
                msg.append(key.getUserName());
                msg.append(": ");
                msg.append(new Date(key.getKeyDate()).toLocaleString());
                msg.append("\n");
            }
        }

        Bundle args = new Bundle();
        args.putString(extra.RESID_MSG, msg.toString());
        args.putBoolean(extra.ALLOW_DELETE, (recentKeys.size() != 0));
        if (!isFinishing()) {
            removeDialog(DIALOG_MANAGE_PASS);
            showDialog(DIALOG_MANAGE_PASS, args);
        }
    }

    private AlertDialog.Builder xshowManagePassphrases(final Activity act, Bundle args) {
        String msg = args.getString(extra.RESID_MSG);
        boolean allowDelete = args.getBoolean(extra.ALLOW_DELETE);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        View layout;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            layout = View.inflate(new ContextThemeWrapper(act, R.style.Theme_AppCompat),
                    R.layout.about, null);
        } else {
            LayoutInflater inflater = (LayoutInflater) act
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.about, null);
        }
        TextView textViewAbout = (TextView) layout.findViewById(R.id.TextViewAbout);
        ad.setTitle(R.string.menu_ManagePassphrases);
        textViewAbout.setText(msg);
        ad.setView(layout);
        ad.setCancelable(true);
        if (allowDelete) { // only have delete key when recent keys exist
            ad.setPositiveButton(R.string.btn_DeleteKeys, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    // delete all more recent keys for now...
                    doRemoveMoreRecentKeys();
                    restart();
                }
            });
        }
        ad.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    protected static void doRemoveMoreRecentKeys() {
        int totalUsers = SafeSlinger.getTotalUsers();
        for (int i = 0; i < totalUsers; i++) {
            if (i > SafeSlingerPrefs.getUser()) {
                deleteUser(i);
            }
        }

        // update backups as well
        SafeSlinger.queueBackup();
    }

    private static void deleteUser(int userNumber) {
        CryptTools.deleteKeyFile(userNumber);
        SafeSlingerPrefs.deletePrefs(userNumber);
        RecipientDatabaseHelper.deleteRecipientDatabase(userNumber);
        MessageDatabaseHelper.deleteMessageDatabase(userNumber);
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
            layout = View.inflate(new ContextThemeWrapper(act, R.style.Theme_AppCompat),
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
                            if (item.label.compareTo(SafeSlingerConfig.APP_KEY_PUBKEY) == 0
                                    || item.label.compareTo(SafeSlingerConfig.APP_KEY_PUSHTOKEN) == 0) {
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
                    inviteMsg = new MessageRow(c, false);
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

        String name = SafeSlingerPrefs.getContactName();
        ArrayList<UseContactItem> contacts = getUseContactItemsByName(name);

        boolean isContactInUse = !TextUtils.isEmpty(SafeSlingerPrefs.getContactLookupKey());

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
        final String pName = args.getString(extra.NAME);
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
                        // save these for lookup and display purposes
                        SafeSlingerPrefs.setContactName(pName);
                        SafeSlingerPrefs.setContactLookupKey(items.get(item).contactLookupKey);
                        restart();
                        break;
                    case CONTACT:
                        // user wants to use found contact as a personal contact
                        SafeSlingerPrefs.setContactName(getContactName(items.get(item).contactLookupKey));
                        SafeSlingerPrefs.setContactLookupKey(items.get(item).contactLookupKey);
                        restart();
                        break;
                    case ANOTHER:
                        // user wants to choose new contact for themselves
                        showPickContact(RESULT_PICK_CONTACT_SENDER);
                        break;
                    case NONE:
                        // user wants to remove link to address book
                        SafeSlingerPrefs.setContactLookupKey(null);
                        restart();
                        break;
                    case NEW:
                        // user wants to create new contact
                        showAddContact(SafeSlingerPrefs.getContactName());
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
        String currentPassPhrase = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs
                .getKeyIdString());

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
            case DIALOG_MANAGE_PASS:
                return xshowManagePassphrases(HomeActivity.this, args).create();
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
            case DIALOG_CONTACTINVITE:
                return xshowAddContactInvite(this).create();
            case DIALOG_CONTACTTYPE:
                return xshowCustomContactPicker(this, args).create();
        }
        return super.onCreateDialog(id);
    }

}
