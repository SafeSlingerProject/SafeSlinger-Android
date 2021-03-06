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

package edu.cmu.cylab.starslinger.view;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
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
import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LabeledIntent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.MessagingException;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs.pref;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.CryptToolsLegacy;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgNonExistingKeyException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPacketSizeException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgPeerKeyFormatException;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgProvider;
import edu.cmu.cylab.starslinger.exchange.ExchangeActivity;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig;
import edu.cmu.cylab.starslinger.model.CryptoMsgPrivateData;
import edu.cmu.cylab.starslinger.model.DraftData;
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
import edu.cmu.cylab.starslinger.transaction.C2DMessaging;
import edu.cmu.cylab.starslinger.transaction.MessageNotFoundException;
import edu.cmu.cylab.starslinger.transaction.WebEngine;
import edu.cmu.cylab.starslinger.util.NotificationPlayer;
import edu.cmu.cylab.starslinger.util.SSUtil;
import edu.cmu.cylab.starslinger.view.IntroductionFragment.OnIntroResultListener;
import edu.cmu.cylab.starslinger.view.MessagesFragment.OnMessagesResultListener;
import edu.cmu.cylab.starslinger.view.SlingerFragment.OnSlingerResultListener;

@SuppressLint("InflateParams")
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class HomeActivity extends BaseActivity implements OnMessagesResultListener,
        OnSlingerResultListener, OnIntroResultListener {
    private static final String TAG = SafeSlingerConfig.LOG_TAG;

    // constants
    private static final int RESULT_PICK_CONTACT_SENDER = 1;
    private static final int RESULT_ERROR = 9;
    private static final int VIEW_FILEATTACH_ID = 120;
    private static final int VIEW_FILESAVE_ID = 130;
    private static final int VIEW_RECIPSEL_ID = 140;
    private static final int VIEW_RECIPSEL_FORFILE_ID = 150;
    private static final int VIEW_RECIPSEL_FORFWD_ID = 155;
    private static final int VIEW_EXCHANGE_ID = 160;
    private static final int VIEW_RECIPSEL1 = 170;
    private static final int VIEW_RECIPSEL2 = 180;
    private static final int VIEW_FINDCONTACT_ID = 190;
    private static final int VIEW_PASSPHRASE_ID = 220;
    private static final int VIEW_PASSPHRASE_CHANGE_ID = 230;
    private static final int VIEW_SETTINGS_ID = 260;
    private static final int VIEW_SAVE_ID = 280;
    public static final int NOTIFY_NEW_MSG_ID = 500;
    public static final int NOTIFY_BACKUP_DELAY_ID = 501;
    public static final int NOTIFY_PASS_CACHED_ID = 502;
    public static final int NOTIFY_SLINGKEYS_REMIND_ID = 503;

    private static final int MS_POLL_INTERVAL = 500;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    // static data
    private static ProgressDialog sProg = null;
    private static Handler sHandler = new Handler();
    private static String sProgressMsg = null;
    private static DraftData d = DraftData.INSTANCE;
    private static MessageData mImported = new MessageData();

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;

    private enum Tabs {
        MESSAGE, //
        SLINGKEYS, //
        INTRO, //
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
            String error = intent.getStringExtra(extra.ERROR);
            if (error != null) {
                if (error.equals(C2DMessaging.ERRREG_SERVICE_NOT_AVAILABLE)) {
                    long backoff = SafeSlingerPrefs.getPusgRegBackoff();
                    MyLog.d(TAG, String.format(
                            getString(R.string.error_C2DMRegServiceNotAvailable),
                            backoff / 2 / 1000));
                    setProgressCancelHandler();
                    return;
                } else if (error.equals(C2DMessaging.ERRREG_ACCOUNT_MISSING)) {
                    showErrorExit(R.string.error_C2DMRegAccountMissing);
                    return;
                } else if (error.equals(C2DMessaging.ERRREG_INVALID_SENDER)) {
                    showErrorExit(R.string.error_C2DMRegInvalidSender);
                    return;
                } else if (error.equals(C2DMessaging.ERRREG_AUTHENTICATION_FAILED)) {
                    showErrorExit(R.string.error_C2DMRegAuthenticationFailed);
                    return;
                } else if (error.equals(C2DMessaging.ERRREG_TOO_MANY_REGISTRATIONS)) {
                    showErrorExit(R.string.error_C2DMRegTooManyRegistrations);
                    return;
                } else if (error.equals(C2DMessaging.ERRREG_PHONE_REGISTRATION_ERROR)) {
                    showErrorExit(R.string.error_C2DMRegPhoneRegistrationError);
                    return;
                } else {
                    // Unexpected registration errors.
                    showErrorExit(error);
                    return;
                }
            }
            isSetupCheckComplete();
        }
    };

    /***
     * Only for arriving incoming messages. Sent via OrderedBroadcast when a
     * push message arrives. It is used to update the messages tab only when the
     * messages tab is currently visible.
     */
    private BroadcastReceiver mMsgIncomingReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean abort = abortNextBroadcast(intent);

            // update current message list if in view...
            final int position = getSupportActionBar().getSelectedNavigationIndex();
            if (position == Tabs.MESSAGE.ordinal()) {
                if (mTabsAdapter != null) {
                    MessagesFragment mf = (MessagesFragment) mTabsAdapter
                            .findFragmentByPosition(Tabs.MESSAGE.ordinal());
                    if (mf != null) {
                        mf.updateValues(intent.getExtras());

                        // if not current recipient in thread, user must get
                        // written notice so do not abort
                        RecipientRow r = d.getRecip();
                        String inkey = intent.getExtras().getString(extra.KEYID);
                        if (abort && r != null && inkey != null) {
                            if (!inkey.equals(r.getKeyid())) {
                                abort = false;
                            }
                        }
                    }
                }
            }

            if (abort) {
                abortBroadcast();
                // Reset the last timestamp when aborting broadcast ..get
                // notified immediately in other states
                SafeSlingerPrefs.setLastTimeStamp(0);

                if (!TextUtils.isEmpty(SafeSlingerPrefs.getNotificationRingTone())) {
                    // need some audible notification...
                    playInConversationNotificationSound(getApplicationContext());
                }
            }

        }
    };

    /***
     * Only for after sending outgoing messages. Sent via normal Intent and is
     * meant to always switch the messages tab into view for sent messages.
     */
    private BroadcastReceiver mMsgOutgoingReceiver = new BroadcastReceiver() {

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

    /**
     * Play the in-conversation notification sound (it's the regular
     * notification sound, but played at half-volume
     */
    private static void playInConversationNotificationSound(Context context) {
        String ringtoneStr = SafeSlingerPrefs.getNotificationRingTone();
        if (TextUtils.isEmpty(ringtoneStr)) {
            // Nothing to play
            return;
        }
        Uri ringtoneUri = Uri.parse(ringtoneStr);
        final NotificationPlayer player = new NotificationPlayer(TAG);
        player.play(context, ringtoneUri, false, AudioManager.STREAM_NOTIFICATION,
                SafeSlingerPrefs.IN_CONVERSATION_NOTIFICATION_VOLUME);

        // Stop the sound after five seconds to handle continuous ringtones
        sHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                player.stop();
            }
        }, 5000);
    }

    private boolean abortNextBroadcast(Intent intent) {
        // conditions to abort, all must be true:
        // - message tab is in view
        // - key id matches current thread, or all threads in view
        boolean abortBroadcast = false;
        if (intent.getExtras() != null) {

            int allCount = intent.getExtras().getInt(extra.NOTIFY_COUNT);
            if (allCount != 0
                    && (getSupportActionBar().getSelectedNavigationIndex() == Tabs.MESSAGE
                            .ordinal() && SafeSlinger.getApplication().isMessageFragActive())) {
                abortBroadcast = true;
            }
        }

        return abortBroadcast;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        // handle caller send action once send only
        // show send screen once only
        if (loadCurrentPassPhrase()) {
            processIntent(intent);
        }
    }

    public void processIntent(Intent intent) {
        // prevent the intent from canceling active key exchange
        if (SafeSlinger.getApplication().isExchangeActive()) {
            return;
        }

        String action = intent.getAction();
        if (SafeSlingerConfig.Intent.ACTION_MESSAGEINCOMING.equals(action)) {
            // clicked on new message notifications window, show messages
            // collapse messages to threads when looking for new messages
            MessagesFragment.removeRecip();
            setTab(Tabs.MESSAGE);
            refreshView();

        } else if (SafeSlingerConfig.Intent.ACTION_BACKUPNOTIFY.equals(action)) {
            // clicked on backup reminder notifications window, show reminder
            // query
            showBackupQuery();
            refreshView();

        } else if (SafeSlingerConfig.Intent.ACTION_SLINGKEYSNOTIFY.equals(action)) {
            // clicked on exchange reminder, show exchange tab
            setTab(Tabs.SLINGKEYS);
            refreshView();

        } else if (SafeSlingerConfig.Intent.ACTION_CHANGESETTINGS.equals(action)) {
            // clicked on pass cache notification
            showSettings();
            refreshView();

        } else if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // clicked share externally, load file, show compose
            d.clearRecip();
            if (handleSendToAction()) {
                showRecipientSelect(VIEW_RECIPSEL_FORFILE_ID);
            }
        }
    }

    public void setProperDefaultTab() throws SQLException {
        // if nothing else, make sure proper default tab is selected
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(getApplicationContext());

        if (dbRecipient.getTrustedRecipientCount() == 0) {
            // Sling Keys should be the default when there are 0 keys
            // exchanged.
            setTab(Tabs.SLINGKEYS);
        } else {
            // Messages should be the default when there are > 1 messages.
            setTab(Tabs.MESSAGE);
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
        // setTheme(R.style.Theme_Safeslinger);
        super.onCreate(savedInstanceState);

        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.pager);
        setContentView(mViewPager);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setTitle(R.string.app_name);

        mTabsAdapter = new TabsAdapter(this, bar, mViewPager);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.menu_TagListMessages),
                MessagesFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.menu_TagExchange), SlingerFragment.class,
                null);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.menu_Introduction),
                IntroductionFragment.class, null);

        if (savedInstanceState != null) {
            setTab(Tabs.values()[savedInstanceState.getInt(extra.RECOVERY_TAB)]);
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // prepare for push registration...
        registerReceiver(mPushRegReceiver, new IntentFilter(C2DMessaging.PUSH_REGISTERED));
        IntentFilter intentFilter = new IntentFilter(
                SafeSlingerConfig.Intent.ACTION_MESSAGEINCOMING);
        intentFilter.setPriority(2);
        registerReceiver(mMsgIncomingReceiver, intentFilter);
        registerReceiver(mMsgOutgoingReceiver, new IntentFilter(
                SafeSlingerConfig.Intent.ACTION_MESSAGEOUTGOING));

        if (savedInstanceState == null) {
            // init app launch once all time

            if (loadCurrentPassPhrase()) {
                processIntent(getIntent());
            }

            boolean dateChanged = SSUtil.isDayChanged(SafeSlingerPrefs.getContactDBLastScan());
            if (dateChanged) {
                SafeSlingerPrefs.setThisVersionOpened();
                BackgroundSyncUpdatesTask backgroundSyncUpdates = new BackgroundSyncUpdatesTask();
                backgroundSyncUpdates.execute(new String());
            }

            // init on reload once all time
            if (!isSetupCheckComplete()) {
                return;
            }
        }
    }

    private void showWalkthroughDialog() {
        sHandler.removeCallbacks(null);
        sHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                BaseActivity.xshowWalkthrough(HomeActivity.this).create().show();

            }
        }, 200);
    }

    public class TabsAdapter extends FragmentPagerAdapter implements ActionBar.TabListener,
            ViewPager.OnPageChangeListener {
        private final FragmentActivity mActivity;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        /**
         * @return the mTabs
         */
        public ArrayList<TabInfo> getmTabs() {
            return mTabs;
        }

        final class TabInfo {
            private final Class<?> clss;
            private Bundle args;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter(FragmentActivity activity, ActionBar bar, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mActivity = activity;
            mActionBar = bar;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
            TabInfo info = new TabInfo(clss, args);
            tab.setTag(info);
            tab.setTabListener(this);
            mTabs.add(info);
            mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mActivity, info.clss.getName(), info.args);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
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
                    supportInvalidateOptionsMenu(); // update action bar options

                    // refresh all that matter
                    switch (Tabs.values()[tab.getPosition()]) {
                        case MESSAGE:
                            MessagesFragment mf = (MessagesFragment) findFragmentByPosition(Tabs.MESSAGE
                                    .ordinal());
                            if (mf != null) {
                                mf.updateKeypad();
                                mf.updateValues(null);
                            }
                            break;
                        case SLINGKEYS:
                            SlingerFragment sf = (SlingerFragment) findFragmentByPosition(Tabs.SLINGKEYS
                                    .ordinal());
                            if (sf != null) {
                                sf.updateKeypad();
                                sf.updateValues(null);
                            }
                            break;
                        case INTRO:
                            IntroductionFragment sif = (IntroductionFragment) findFragmentByPosition(Tabs.INTRO
                                    .ordinal());
                            if (sif != null) {
                                sif.updateKeypad();
                                sif.updateValues(null);
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
            supportInvalidateOptionsMenu(); // update action bar options

            // refresh all that matter
            switch (Tabs.values()[tab.getPosition()]) {
                case MESSAGE:
                    MessagesFragment mf = (MessagesFragment) findFragmentByPosition(Tabs.MESSAGE
                            .ordinal());
                    if (mf != null) {
                        mf.updateKeypad();
                        mf.updateValues(null);
                    }
                    break;
                case SLINGKEYS:
                    SlingerFragment sf = (SlingerFragment) findFragmentByPosition(Tabs.SLINGKEYS
                            .ordinal());
                    if (sf != null) {
                        sf.updateKeypad();
                        sf.updateValues(null);
                    }
                    break;
                case INTRO:
                    IntroductionFragment sif = (IntroductionFragment) findFragmentByPosition(Tabs.INTRO
                            .ordinal());
                    if (sif != null) {
                        sif.updateKeypad();
                        sif.updateValues(null);
                    }
                    break;
                default:
                    break;
            }
        }

        public Fragment findFragmentByPosition(int position) {
            final FragmentManager fm = mActivity.getSupportFragmentManager();
            return fm.findFragmentByTag("android:switcher:" + mViewPager.getId() + ":"
                    + getItemId(position));
        }
    }

    protected void restoreView() {
        // all tabs with data...
        if (mTabsAdapter != null) {
            MessagesFragment mf = (MessagesFragment) mTabsAdapter
                    .findFragmentByPosition(Tabs.MESSAGE.ordinal());
            if (mf != null) {
                mf.updateValues(null);
            }
            SlingerFragment sf = (SlingerFragment) mTabsAdapter
                    .findFragmentByPosition(Tabs.SLINGKEYS.ordinal());
            if (sf != null) {
                sf.updateValues(null);
            }
            IntroductionFragment sif = (IntroductionFragment) mTabsAdapter
                    .findFragmentByPosition(Tabs.INTRO.ordinal());
            if (sif != null) {
                sif.updateValues(null);
            }
        }
    }

    private void showExchange(byte[] userData) {
        Intent intent = new Intent(HomeActivity.this, ExchangeActivity.class);
        intent.putExtra(ExchangeConfig.extra.USER_DATA, userData);
        intent.putExtra(ExchangeConfig.extra.HOST_NAME, SafeSlingerConfig.HTTPURL_EXCHANGE_HOST);
        startActivityForResult(intent, VIEW_EXCHANGE_ID);
        SafeSlinger.getApplication().setExchangeActive(true);
        // update foreground notification to prevent closing exchange
        SafeSlinger.startCacheService(HomeActivity.this);
    }

    private void showSave(Bundle args) {
        Intent intent = new Intent(HomeActivity.this, SaveActivity.class);
        intent.replaceExtras(args);
        startActivityForResult(intent, VIEW_SAVE_ID);
    }

    private boolean isSetupCheckComplete() {

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

        // init local struct...
        int notify = SSUtil.getLocalNotification(getApplicationContext());
        String token = SafeSlingerPrefs.getPushRegistrationId();

        // if push token bad....
        // ...request a push token... (restart)
        if (notify != SafeSlingerConfig.NOTIFY_NOPUSH) {
            if (TextUtils.isEmpty(token) && notify == SafeSlingerConfig.NOTIFY_ANDROIDGCM) {
                // ensure that user has registered with push service...
                doGetPushRegistration();
                return false;
            }
        }

        // look for proper key
        boolean hasSecretKey = CryptTools.existsSecretKey(getApplicationContext());
        if (!hasSecretKey && SafeSlingerPrefs.getUser() == 0) {
            // key not found, try loading older version for base user only
            hasSecretKey = CryptToolsLegacy.existsSecretKeyOld();
        }
        if (!hasSecretKey) {
            showFindContact(); // new
        }

        if (!isUserLoggedIn()) {
            return false;
        }

        setProperDefaultTab();

        if (SafeSlingerPrefs.getShowWalkthrough()) {
            // BaseActivity.xshowWalkthrough(this).create().show();
            showWalkthroughDialog();
        }

        return true;
    }

    private boolean isUserLoggedIn() {

        // if no pass exists...
        // ...request pass entry... (restart)
        // now with contact, enter pass phrase and check
        if (!loadCurrentPassPhrase()) {
            if (!SafeSlinger.isPassphraseOpen()) {
                // update incoming messages if needed
                if (SafeSlingerPrefs.getMessagesIncoming() > 0) {
                    SafeSlinger.getApplication().checkForMissedMessages();
                }
                showPassPhrase(false, false); // normal
            }
            return false;
        }

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save
        if (SafeSlinger.getTempCameraFileUri() != null) {
            outState.putString(extra.FPATH, SafeSlinger.getTempCameraFileUri().getPath());
        }
        final int position = getSupportActionBar().getSelectedNavigationIndex();
        outState.putInt(extra.RECOVERY_TAB, position);

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
        final int position = getSupportActionBar().getSelectedNavigationIndex();
        if (position == Tabs.MESSAGE.ordinal()) {
            if (d.existsRecip()) {
                RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this);
                int newerRecips = dbRecipient.getAllNewerRecipients(d.getRecip(), false);
                if (d.getRecip().isSendable() && newerRecips <= 0) {
                    // user can attach when in conversation and not disabled
                    MenuItem iAttach = menu.add(0, MENU_ATTACH, 0, R.string.btn_SelectFile)
                            .setIcon(R.drawable.ic_action_attach_file);
                    MenuCompat.setShowAsAction(iAttach, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
                }
            } else {
                // user can add message from all threads view
                MenuItem iNewMsg = menu.add(0, MENU_NEWMESSAGE, 0, R.string.menu_TagComposeMessage)
                        .setIcon(R.drawable.ic_action_add_message);
                MenuCompat.setShowAsAction(iNewMsg, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
            }
        }

        MenuItem iInvite = menu.add(0, MENU_CONTACTINVITE, 0, R.string.menu_SelectShareApp)
                .setIcon(R.drawable.ic_action_add_person);
        MenuItem iNewMsg = menu.add(0, MENU_NEWMESSAGE, 0, R.string.menu_TagComposeMessage)
                .setIcon(R.drawable.ic_action_add_message);
        MenuItem iFeedback = menu.add(0, MENU_FEEDBACK, 0, R.string.menu_sendFeedback).setIcon(
                android.R.drawable.ic_menu_send);
        MenuItem iLogout = menu.add(0, MENU_LOGOUT, 0, R.string.menu_Logout).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
        MenuItem iSettings = menu.add(0, MENU_SETTINGS, 0, R.string.menu_Settings).setIcon(
                android.R.drawable.ic_menu_preferences);
        MenuItem iHelp = menu.add(0, MENU_REFERENCE, 0, R.string.menu_Help).setIcon(
                android.R.drawable.ic_menu_help);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SpannableString spanString;

            spanString = new SpannableString(iInvite.getTitle().toString());
            // fix the color to white
            spanString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spanString.length(), 0);
            iInvite.setTitle(spanString);

            spanString = new SpannableString(iNewMsg.getTitle().toString());
            // fix the color to white
            spanString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spanString.length(), 0);
            iNewMsg.setTitle(spanString);

            spanString = new SpannableString(iFeedback.getTitle().toString());
            // fix the color to white
            spanString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spanString.length(), 0);
            iFeedback.setTitle(spanString);

            spanString = new SpannableString(iLogout.getTitle().toString());
            // fix the color to white
            spanString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spanString.length(), 0);
            iLogout.setTitle(spanString);

            spanString = new SpannableString(iSettings.getTitle().toString());
            // fix the color to white
            spanString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spanString.length(), 0);
            iSettings.setTitle(spanString);

            spanString = new SpannableString(iHelp.getTitle().toString());
            // fix the color to white
            spanString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spanString.length(), 0);
            iHelp.setTitle(spanString);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_LOGOUT:
                // remove cached pass
                doManualLogout();
                return true;
            case MENU_CONTACTINVITE:
                showAddContactInvite();
                return true;
            case MENU_SETTINGS:
                showSettings();
                return true;
            case MENU_FEEDBACK:
                SafeSlinger.getApplication().showFeedbackEmail(HomeActivity.this);
                return true;
            case MENU_REFERENCE:
                showReference();
                return true;
            case MENU_NEWMESSAGE:
                d.clearRecip();
                mImported = new MessageData();
                showRecipientSelect(VIEW_RECIPSEL_ID);
                return true;
            case MENU_ATTACH:
                showFileAttach();
                return true;
        }
        return false;
    }

    private void doManualLogout() {
        reInitForExit();
        sendBroadcast(new Intent(SafeSlingerConfig.Intent.ACTION_LOGOUT));
    }

    private class CreateKeyTask extends AsyncTask<String, String, String> {
        private CryptoMsgProvider mKeyData;
        private String mEditPassPhrase;

        @Override
        protected String doInBackground(String... arg0) {
            mEditPassPhrase = arg0[0];
            publishProgress(getString(R.string.prog_GeneratingKey));
            try {
                mKeyData = CryptoMsgProvider.createInstance(SafeSlinger.isLoggable());
                mKeyData.GenKeyPairs();
                if (!mKeyData.isGenerated()) {
                    throw new CryptoMsgException(
                            getString(R.string.error_couldNotExtractPrivateKey));
                }
            } catch (OutOfMemoryError e) {
                return getString(R.string.error_OutOfMemoryError);
            } catch (InvalidParameterException e) {
                return e.getLocalizedMessage();
            } catch (CryptoMsgException e) {
                return e.getLocalizedMessage();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            showProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            endProgress();
            if (result != null) {
                showErrorExit(result);
                return;
            }

            try {
                if (!mKeyData.isGenerated()) {
                    throw new CryptoMsgException(
                            getString(R.string.error_couldNotExtractPrivateKey));
                }

                CryptoMsgPrivateData mine = new CryptoMsgPrivateData(mKeyData);

                // save public portion
                SafeSlinger.setSenderKey(mine.getSafeSlingerString());

                SafeSlingerPrefs.setKeyIdString(mine.getKeyId());
                SafeSlingerPrefs.setKeyDate(mine.getGenDate());

                // save private portion in secret key storage...
                CryptTools.putSecretKey(mine, mEditPassPhrase);

                // update cache to avoid entering twice...
                SafeSlinger.setCachedPassPhrase(mKeyData.GetSelfKeyid(), mEditPassPhrase);
                updatePassCacheTimer();

                // now that we have new key id, use it when updating contacts...
                BackgroundSyncUpdatesTask backgroundSyncUpdates = new BackgroundSyncUpdatesTask();
                backgroundSyncUpdates.execute(new String());

                isSetupCheckComplete();
            } catch (IOException e) {
                showErrorExit(e);
            } catch (CryptoMsgException e) {
                showErrorExit(e);
            } catch (CryptoMsgNonExistingKeyException e) {
                showErrorExit(e);
            }
        }
    }

    private MessageData doSaveDraft(RecipientRow recip, MessageData saveMsg) throws SQLException {
        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this);
        if (saveMsg.getRowId() < 0) {
            // create draft (need at least recipient(file) or text
            // chosen...
            if (recip != null
                    && (!TextUtils.isEmpty(saveMsg.getText()) || saveMsg.getFileData() != null)) {
                long rowId = dbMessage.createDraftMessage(recip, saveMsg,
                        System.currentTimeMillis());
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
                try {
                    if (c.moveToFirst()) {
                        MessageRow msg = new MessageRow(c, false);
                        if (msg.getStatus() != MessageDbAdapter.MESSAGE_STATUS_DRAFT) {
                            return saveMsg;
                        }
                    }
                } finally {
                    c.close();
                }
            }

            if (!TextUtils.isEmpty(saveMsg.getText()) || saveMsg.getFileData() != null) {
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
        return saveMsg;
    }

    private void doSendMessageStart(MessageTransport... mts) {
        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());

        // pre-test for network, saves time later
        if (!SafeSlinger.getApplication().isOnline()) {
            showNote(R.string.error_CorrectYourInternetConnection);
            refreshView();
            return;
        }

        // update all message data before sending
        for (int i = 0; i < mts.length; i++) {
            RecipientRow recip = mts[i].getRecipient();
            MessageData sendMsg = mts[i].getMessageData();

            // must have either file or text to send
            if (TextUtils.isEmpty(sendMsg.getText()) && TextUtils.isEmpty(sendMsg.getFileName())) {
                showNote(R.string.error_selectDataToSend);
                return;
            }

            if (TextUtils.isEmpty(sendMsg.getFileName())) {
                sendMsg.removeFile();
            } else {
                if (sendMsg.getFileData() == null || sendMsg.getFileSize() == 0) {
                    showNote(R.string.error_InvalidMsg);
                    refreshView();
                    return;
                }
            }

            if (recip == null) {
                showNote(R.string.error_InvalidRecipient);
                refreshView();
                return;
            }

            // manage this draft...
            if (sendMsg.getRowId() < 0) {
                // create draft (need at least recipient(file) or text chosen...
                if (!TextUtils.isEmpty(sendMsg.getText())
                        || !TextUtils.isEmpty(sendMsg.getFileName())) {
                    long rowId = dbMessage
                            .createDraftMessage(recip, sendMsg, sendMsg.getDateSent());
                    sendMsg.setRowId(rowId);
                    if (rowId < 0) {
                        showNote(R.string.error_UnableToSaveMessageInDB);
                        refreshView();
                        return;
                    }
                }
            } else {
                Cursor c = dbMessage.fetchMessageSmall(sendMsg.getRowId());
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            MessageRow msg = new MessageRow(c, false);
                            if (msg.getMessageAction() != MsgAction.MSG_EDIT) {
                                return;
                            }
                        }
                    } finally {
                        c.close();
                    }
                }

                // update draft
                if (!dbMessage.updateDraftMessage(sendMsg.getRowId(), recip, sendMsg)) {
                    showNote(R.string.error_UnableToUpdateMessageInDB);
                    refreshView();
                    return;
                }
            }

            // update status as queued for transmission
            if (!dbMessage.updateEnqueuedMessage(sendMsg.getRowId())) {
                showNote(R.string.error_UnableToUpdateMessageInDB);
                refreshView();
                return;
            }
            // confirm msg is queued
            Cursor c = dbMessage.fetchMessageSmall(sendMsg.getRowId());
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        MessageRow queued = new MessageRow(c, false);
                        if (queued.getStatus() != MessageDbAdapter.MESSAGE_STATUS_QUEUED) {
                            showNote(R.string.error_UnableToUpdateMessageInDB);
                            refreshView();
                            return;
                        }
                    }
                } finally {
                    c.close();
                }
            }

            // pass on any changes...
            mts[i] = new MessageTransport(recip, sendMsg, mts[i].keepDraft());
        }

        // attempt to update messages if in view...
        Intent sendIntent = new Intent(SafeSlingerConfig.Intent.ACTION_MESSAGEOUTGOING);
        if (mts.length == 1) {
            // single should show conversation detail
            sendIntent.putExtra(extra.MESSAGE_ROW_ID, mts[0].getMessageData().getRowId());
            sendIntent.putExtra(extra.RECIPIENT_ROW_ID, mts[0].getRecipient().getRowId());
        } else {
            // multiple should show all threads
            sendIntent.putExtra(extra.MESSAGE_ROW_ID, -1L);
            sendIntent.putExtra(extra.RECIPIENT_ROW_ID, -1L);
        }
        getApplicationContext().sendBroadcast(sendIntent);

        // switch to message tab
        setTab(Tabs.MESSAGE);
        refreshView();

        // start background task to send
        SendMessageTask task = new SendMessageTask();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mts);
        else
            task.execute(mts);
        // task.execute(mts);
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

    private void endProgress() {
        if (sProg != null) {
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
                mImported = new MessageData();
                showErrorExit(R.string.error_MultipleSendNotSupported);
                return false;

            } else if (action.equals(Intent.ACTION_SEND)) {
                mImported = new MessageData();
                mImported.setMsgHash(null);

                String type = intent.getType();
                Uri stream = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                CharSequence extra_text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);

                // if this is from a share menu
                try {
                    // Get resource path from intent caller
                    if (Intent.ACTION_SEND.equals(action)) {
                        if (stream != null) {
                            mImported = SSUtil.addAttachmentFromUri(this, mImported, stream, type);
                            filesize = mImported.getFileSize();
                        } else if (!TextUtils.isEmpty(extra_text)) {
                            filesize = extra_text.length();
                            if (filesize <= SafeSlingerConfig.MAX_TEXTMESSAGE) {
                                mImported.removeFile();
                                mImported.setText(extra_text.toString());
                            } else {
                                mImported.setFileType("text/plain");
                                final byte[] textBytes = extra_text.toString().getBytes();
                                mImported.setFileData(textBytes);
                                mImported.setFileSize(textBytes.length);
                                SimpleDateFormat sdf = new SimpleDateFormat(
                                        SafeSlingerConfig.DATETIME_FILENAME, Locale.US);
                                mImported.setFileName(sdf.format(new Date()) + ".txt");
                                mImported.removeText();
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

    @Override
    public void onMessageResultListener(Bundle data) {
        int resultCode = data.getInt(extra.RESULT_CODE);
        String text = null;
        String fpath = null;
        RecipientRow recip = null;
        long rowIdMessage = -1;
        long rowIdInbox = -1;
        if (data != null) {
            text = data.getString(extra.TEXT_MESSAGE);
            fpath = data.getString(extra.FPATH);
            rowIdMessage = data.getLong(extra.MESSAGE_ROW_ID, -1L);
            rowIdInbox = data.getLong(extra.INBOX_ROW_ID, -1L);
            long rowIdRecipient = data.getLong(extra.RECIPIENT_ROW_ID, -1L);
            if (rowIdRecipient > -1) {
                RecipientDbAdapter dbRecipient = RecipientDbAdapter
                        .openInstance(getApplicationContext());
                Cursor c = dbRecipient.fetchRecipient(rowIdRecipient);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            recip = new RecipientRow(c);
                        } else {
                            showNote(R.string.error_InvalidRecipient);
                            return;
                        }
                    } finally {
                        c.close();
                    }
                }
            }
        }

        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());

        switch (resultCode) {
            case MessagesFragment.RESULT_SAVE:
                MessageData saveMsg = new MessageData();
                saveMsg.setRowId(rowIdMessage);
                saveMsg.setText(text);
                try {
                    if (!TextUtils.isEmpty(fpath)) {
                        saveMsg = SSUtil.addAttachmentFromPath(saveMsg, fpath);
                    }
                } catch (FileNotFoundException e) {
                    showNote(e);
                    refreshView();
                    break;
                }
                // save state for attachment updates
                mImported = doSaveDraft(recip, saveMsg);
                break;
            case MessagesFragment.RESULT_SEND:
                MessageData sendMsg = new MessageData();
                // set sent time closest to UI command
                sendMsg.setDateSent(System.currentTimeMillis());
                sendMsg.setRowId(rowIdMessage);
                sendMsg.setText(text);
                try {
                    if (!TextUtils.isEmpty(fpath)) {
                        sendMsg = SSUtil.addAttachmentFromPath(sendMsg, fpath);
                    }
                } catch (FileNotFoundException e) {
                    showNote(e);
                    refreshView();
                    break;
                }
                // user wants to post the file and notify recipient
                if (recip == null) {
                    showNote(R.string.error_InvalidRecipient);
                    refreshView();
                    break;
                }
                if (recip.getNotify() == SafeSlingerConfig.NOTIFY_NOPUSH) {
                    showNote(R.string.error_InvalidRecipient);
                    refreshView();
                    break;
                }
                // manual message, keep message tab draft
                doSendMessageStart(new MessageTransport(recip, sendMsg, true));
                mImported = new MessageData();
                break;
            case MessagesFragment.RESULT_FWDMESSAGE:
                mImported = new MessageData();
                Cursor cfm = dbMessage.fetchMessageSmall(rowIdMessage);
                if (cfm != null) {
                    try {
                        if (cfm.moveToFirst()) {
                            MessageRow fwd = new MessageRow(cfm, false);
                            mImported.setText(fwd.getText());
                            try {
                                if (!TextUtils.isEmpty(fwd.getFileDir())) {
                                    mImported = SSUtil.addAttachmentFromPath(mImported,
                                            fwd.getFileDir());
                                }
                            } catch (FileNotFoundException e) {
                                showNote(e);
                                refreshView();
                                break;
                            }
                        }
                    } finally {
                        cfm.close();
                    }
                }
                d.clearRecip();
                showRecipientSelect(VIEW_RECIPSEL_FORFWD_ID);
                break;
            case MessagesFragment.RESULT_EDITMESSAGE:
                Cursor cem = dbMessage.fetchMessageSmall(rowIdMessage);
                if (cem != null) {
                    try {
                        if (cem.moveToFirst()) {
                            MessageRow edit = new MessageRow(cem, false);
                            d.setRecip(recip);
                            // "touch" the message to make it most recent draft
                            edit.setDateSent(System.currentTimeMillis());
                            if (!dbMessage.updateDraftMessage(edit.getRowId(), recip, edit)) {
                                showNote(R.string.error_UnableToUpdateMessageInDB);
                            }
                        }
                    } finally {
                        cem.close();
                    }
                }
                if (!d.existsRecip()) {
                    showRecipientSelect(VIEW_RECIPSEL_ID);
                } else {
                    setTab(Tabs.MESSAGE);
                    refreshView();
                }
                break;
            case MessagesFragment.RESULT_GETMESSAGE:
                MessageData inbox = new MessageData();
                inbox.setRowId(rowIdInbox);
                inbox.setMsgHash(data.getString(extra.PUSH_MSG_HASH));

                if (inbox.getMsgHash() != null) {
                    GetMessageTask getMessageTask = new GetMessageTask();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        getMessageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, inbox);
                    else
                        getMessageTask.execute(inbox);
                    // getMessageTask.execute(inbox);
                } else {
                    showNote(R.string.error_InvalidIncomingMessage);
                    refreshView();
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

                if (recvFile.getMsgHash() != null) {
                    GetFileTask getFileTask = new GetFileTask();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        getFileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recvFile);
                    else
                        getFileTask.execute(recvFile);
                    // getFileTask.execute(recvFile);
                } else {
                    showNote(R.string.error_InvalidIncomingMessage);
                    refreshView();
                }
                break;
            case MessagesFragment.RESULT_PROCESS_SSMIME:
                MessageData ssFile = new MessageData();
                ssFile.setRowId(rowIdMessage);
                ssFile.setMsgHash(data.getString(extra.PUSH_MSG_HASH));
                ssFile.setFileName(data.getString(extra.PUSH_FILE_NAME));
                ssFile.setFileType(data.getString(extra.PUSH_FILE_TYPE));
                ssFile.setFileSize(data.getInt(extra.PUSH_FILE_SIZE, 0));

                byte[] rawFile = dbMessage.getRawFile(rowIdMessage);
                if (rawFile != null) {
                    // new ss-mime way: do separate file load
                    ssFile.setFileData(rawFile);
                    doProcessSafeSlingerMimeType(ssFile);
                } else {
                    // old ss-mime way: complete re-download
                    ssFile.setFileData(null);

                    if (ssFile.getMsgHash() != null && ssFile.getFileDir() != null) {
                        GetFileTask getFileTask = new GetFileTask();
                        getFileTask.execute(ssFile);
                    } else {
                        showNote(R.string.error_InvalidIncomingMessage);
                        refreshView();
                    }
                }
                break;
            case MessagesFragment.RESULT_FILESEL:
                // user wants to pick a file to send
                showFileAttach();
                break;
            case MessagesFragment.RESULT_FILEREMOVE:
                // user wants to remove file
                Cursor cfrm = dbMessage.fetchMessageSmall(rowIdMessage);
                if (cfrm != null) {
                    try {
                        if (cfrm.moveToFirst()) {
                            MessageRow rem = new MessageRow(cfrm, false);
                            rem.removeFile();

                            if (!TextUtils.isEmpty(rem.getText()) || rem.getFileData() != null) {
                                // update draft
                                if (!dbMessage.updateDraftMessage(rem.getRowId(), recip, rem)) {
                                    showNote(R.string.error_UnableToUpdateMessageInDB);
                                }
                            } else {
                                // message is empty, we should remove from
                                // database...
                                if (!dbMessage.deleteMessage(rem.getRowId())) {
                                    showNote(String.format(
                                            getString(R.string.state_MessagesDeleted), 0));
                                }
                            }
                        }
                    } finally {
                        cfrm.close();
                    }
                }
                setTab(Tabs.MESSAGE);
                refreshView();
                break;
        }
    }

    @Override
    public void onSlingerResultListener(Bundle data) {
        int resultCode = data.getInt(extra.RESULT_CODE);

        switch (resultCode) {
            case SlingerFragment.RESULT_BEGINEXCHANGE:
                if (SafeSlinger.getApplication().isOnline()) {
                    showExchange(data.getString(ExchangeConfig.extra.USER_DATA).getBytes());
                } else {
                    showNote(R.string.error_CorrectYourInternetConnection);
                }
                break;
            case SlingerFragment.RESULT_USEROPTIONS:
                showChangeSenderOptions();
                break;
        }
    }

    @Override
    public void onIntroResultListener(Bundle data) {
        int resultCode = data.getInt(extra.RESULT_CODE);

        switch (resultCode) {
            case IntroductionFragment.RESULT_RECIPSEL1:
                // user wants to pick a recipient
                showRecipientSelect(VIEW_RECIPSEL1);
                break;
            case IntroductionFragment.RESULT_RECIPSEL2:
                // user wants to pick a recipient
                showRecipientSelect(VIEW_RECIPSEL2);
                break;

            case IntroductionFragment.RESULT_SLINGKEYS:
                setTab(Tabs.SLINGKEYS);
                refreshView();
                break;
            case IntroductionFragment.RESULT_SEND:
                if (data != null) {
                    long groupSentTime = System.currentTimeMillis();

                    // TODO: place in AsyncTask with progress for slower devices

                    MessageData sendMsg1 = new MessageData();
                    MessageData sendMsg2 = new MessageData();
                    // set sent time closest to UI command
                    sendMsg1.setDateSent(groupSentTime);
                    sendMsg2.setDateSent(groupSentTime);

                    RecipientRow recip1 = null;
                    RecipientRow recip2 = null;

                    RecipientDbAdapter dbRecipient = RecipientDbAdapter
                            .openInstance(getApplicationContext());
                    // update
                    long rowIdRecipient1 = data.getLong(extra.RECIPIENT_ROW_ID1, -1L);
                    if (rowIdRecipient1 > -1) {
                        Cursor c = dbRecipient.fetchRecipient(rowIdRecipient1);
                        if (c != null) {
                            try {
                                if (c.moveToFirst()) {
                                    recip1 = new RecipientRow(c);
                                } else {
                                    showNote(R.string.error_InvalidRecipient);
                                    break;
                                }
                            } finally {
                                c.close();
                            }
                        }
                    }

                    long rowIdRecipient2 = data.getLong(extra.RECIPIENT_ROW_ID2, -1L);
                    if (rowIdRecipient2 > -1) {
                        Cursor c = dbRecipient.fetchRecipient(rowIdRecipient2);
                        if (c != null) {
                            try {
                                if (c.moveToFirst()) {
                                    recip2 = new RecipientRow(c);
                                } else {
                                    showNote(R.string.error_InvalidRecipient);
                                    break;
                                }
                            } finally {
                                c.close();
                            }
                        }
                    }

                    // user wants to post the file and notify recipient
                    if (recip1 == null || recip2 == null) {
                        showNote(R.string.error_InvalidRecipient);
                        refreshView();
                        break;
                    }
                    if (recip1.getNotify() == SafeSlingerConfig.NOTIFY_NOPUSH
                            || recip2.getNotify() == SafeSlingerConfig.NOTIFY_NOPUSH) {
                        showNote(R.string.error_InvalidRecipient);
                        refreshView();
                        break;
                    }

                    if (recip1.isDeprecated() || recip2.isDeprecated()) {
                        showNote(R.string.error_AllMembersMustUpgradeBadKeyFormat);
                        refreshView();
                        break;
                    }

                    String text1 = data.getString(extra.TEXT_MESSAGE1);
                    if (!TextUtils.isEmpty(text1))
                        sendMsg1.setText(text1);

                    String text2 = data.getString(extra.TEXT_MESSAGE2);
                    if (!TextUtils.isEmpty(text2))
                        sendMsg2.setText(text2);

                    // create vcard data
                    String vCard1 = null;
                    String vCard2 = null;
                    try {
                        vCard1 = SSUtil.generateRecipientVCard(recip1);
                        vCard2 = SSUtil.generateRecipientVCard(recip2);
                    } catch (VCardException e) {
                        showNote(e.getLocalizedMessage());
                        refreshView();
                        break;
                    }

                    if (TextUtils.isEmpty(vCard1) || TextUtils.isEmpty(vCard2)) {
                        showNote(R.string.error_VcardParseFailure);
                        refreshView();
                        break;
                    }

                    // ensure push token and pub key in vCard
                    StringBuilder errors = new StringBuilder();
                    if (!vCard1.contains(SafeSlingerConfig.APP_KEY_PUSHTOKEN)) {
                        errors.append(recip1.getName() + " Push is missing").append("\n");
                    }
                    if (!vCard1.contains(SafeSlingerConfig.APP_KEY_PUBKEY)) {
                        errors.append(recip1.getName() + " PubKey is missing").append("\n");
                    }
                    if (!vCard2.contains(SafeSlingerConfig.APP_KEY_PUSHTOKEN)) {
                        errors.append(recip2.getName() + " Push is missing").append("\n");
                    }
                    if (!vCard2.contains(SafeSlingerConfig.APP_KEY_PUBKEY)) {
                        errors.append(recip2.getName() + " PubKey is missing").append("\n");
                    }
                    if (errors.length() > 0) {
                        showNote(errors.toString());
                        refreshView();
                        break;
                    }

                    byte[] bVC1 = vCard1.getBytes();
                    byte[] bVC2 = vCard2.getBytes();

                    sendMsg1.setFileData(bVC2);
                    sendMsg1.setFileSize(bVC2.length);
                    sendMsg1.setFileName(SafeSlingerConfig.INTRODUCTION_VCF);
                    sendMsg1.setFileType(SafeSlingerConfig.MIMETYPE_CLASS + "/"
                            + SafeSlingerConfig.MIMETYPE_FUNC_SECINTRO);

                    sendMsg2.setFileData(bVC1);
                    sendMsg2.setFileSize(bVC1.length);
                    sendMsg2.setFileName(SafeSlingerConfig.INTRODUCTION_VCF);
                    sendMsg2.setFileType(SafeSlingerConfig.MIMETYPE_CLASS + "/"
                            + SafeSlingerConfig.MIMETYPE_FUNC_SECINTRO);

                    // automatic, do not keep introduction tab drafts
                    doSendMessageStart(new MessageTransport[] {
                            new MessageTransport(recip1, sendMsg1, false),
                            new MessageTransport(recip2, sendMsg2, false)
                    });

                    // reset after complete, little slow, better than nothing
                    d.clearRecip1();
                    d.clearRecip2();
                }
                break;
            case Activity.RESULT_CANCELED:
                // nothing to change...
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
                if (mine != null) {
                    // save loaded pub key for slinging keys later
                    SafeSlinger.setSenderKey(mine.getSafeSlingerString());
                }
            } else {
                setPassphraseStatus(false);
            }
        } else { // check against current key
            if (mine != null) {
                SafeSlinger.setCachedPassPhrase(SafeSlingerPrefs.getKeyIdString(), pass);
                updatePassCacheTimer();
                setPassphraseStatus(true);
                // save loaded pub key for slinging keys later
                SafeSlinger.setSenderKey(mine.getSafeSlingerString());
            } else {
                setPassphraseStatus(false);
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {

            case VIEW_FINDCONTACT_ID:
            case VIEW_PASSPHRASE_CHANGE_ID:
            case VIEW_PASSPHRASE_ID:
                SafeSlinger.setPassphraseOpen(false);
                switch (resultCode) {
                    case RESULT_OK:
                        if (data != null) {
                            if (requestCode == VIEW_FINDCONTACT_ID) {
                                String passNew = data.getStringExtra(extra.PASS_PHRASE_NEW);
                                boolean preExistingKey = doPassEntryCheck(passNew, null, false);
                                if (!preExistingKey) {
                                    CreateKeyTask createKey = new CreateKeyTask();
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                                        createKey.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                                passNew);
                                    else
                                        createKey.execute(passNew);
                                    // createKey.execute(passNew);
                                    break;
                                }
                            } else if (requestCode == VIEW_PASSPHRASE_CHANGE_ID) {
                                String passOld = data.getStringExtra(extra.PASS_PHRASE_OLD);
                                String passNew = data.getStringExtra(extra.PASS_PHRASE_NEW);
                                doPassEntryCheck(passNew, passOld, true);
                            } else { // login
                                String passOld = data.getStringExtra(extra.PASS_PHRASE_OLD);
                                doPassEntryCheck(passOld, null, false);
                            }

                            if (loadCurrentPassPhrase()) {
                                processIntent(getIntent());
                            }

                            // if requested, logged in, try to decrypt pending
                            String passCached = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs
                                    .getKeyIdString());
                            if (SafeSlingerPrefs.getAutoDecrypt() && !TextUtils.isEmpty(passCached)) {
                                DecryptPendingTask decryptPending = new DecryptPendingTask();
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                                    decryptPending.executeOnExecutor(
                                            AsyncTask.THREAD_POOL_EXECUTOR, passCached);
                                else
                                    decryptPending.execute(passCached);
                                // decryptPending.execute(passCached);
                            }

                            // if logged in, post any pending registrations
                            if (!TextUtils.isEmpty(passCached)) {
                                String pushRegistrationId = SafeSlingerPrefs
                                        .getPushRegistrationId();
                                int notify = SSUtil.getLocalNotification(getApplicationContext());
                                SlingerIdentity myId = new SlingerIdentity(pushRegistrationId,
                                        notify, null);
                                UpdateServerRegistrationIdTask regUpdate = new UpdateServerRegistrationIdTask();
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                                    regUpdate.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                            myId);
                                else
                                    regUpdate.execute(myId);
                                // regUpdate.execute(myId);
                            }
                        }
                        isSetupCheckComplete();
                        break;
                    case PassPhraseActivity.RESULT_CLOSEANDCONTINUE:
                        isSetupCheckComplete();
                        break;
                    case RESULT_CANCELED:
                        if (requestCode == VIEW_FINDCONTACT_ID) {
                            // if cancel setting up new profile, save nothing
                            // and push user selection back one
                            int user = SafeSlingerPrefs.getUser();
                            if (user > 0) {
                                SafeSlingerPrefs.setUser(user - 1);
                                deleteUser(user);
                            }
                        }
                        // this separate task is now finished
                        showExit(RESULT_CANCELED);
                        break;
                    case PassPhraseActivity.RESULT_BACKPRESSED:
                        // this separate task is now finished
                        showExit(RESULT_CANCELED);
                        break;
                }
                break;

            case VIEW_RECIPSEL_ID:
            case VIEW_RECIPSEL_FORFILE_ID:
            case VIEW_RECIPSEL_FORFWD_ID:
                switch (resultCode) {
                    case PickRecipientsActivity.RESULT_SLINGKEYS:
                        setTab(Tabs.SLINGKEYS);
                        refreshView();
                        break;
                    case PickRecipientsActivity.RESULT_RECIPSEL:
                        RecipientDbAdapter dbRecipient = RecipientDbAdapter
                                .openInstance(getApplicationContext());
                        MessageDbAdapter dbMessage = MessageDbAdapter
                                .openInstance(getApplicationContext());
                        long rowIdRecipient = data.getLongExtra(extra.RECIPIENT_ROW_ID, -1L);
                        Cursor c = dbRecipient.fetchRecipient(rowIdRecipient);
                        if (c != null) {
                            try {
                                if (c.moveToFirst()) {
                                    RecipientRow recip = new RecipientRow(c);
                                    if (recip != null && recip.getNotRegDate() > 0) {
                                        // reset registration to 0 to enable
                                        // test reply
                                        if (!dbRecipient.updateRecipientRegistrationState(
                                                recip.getRowId(), false)) {
                                            showNote(R.string.error_UnableToUpdateMessageInDB);
                                        }
                                        recip.setNotRegDate(0L);
                                    }
                                    d.setRecip(recip);

                                    if (requestCode == VIEW_RECIPSEL_FORFILE_ID
                                            || requestCode == VIEW_RECIPSEL_FORFWD_ID) {
                                        // create draft
                                        if (!TextUtils.isEmpty(mImported.getText())
                                                || !TextUtils.isEmpty(mImported.getFileName())) {
                                            long rowId = dbMessage.createDraftMessage(recip,
                                                    mImported, System.currentTimeMillis());
                                            mImported.setRowId(rowId);
                                        }
                                    }
                                    setTab(Tabs.MESSAGE);
                                    refreshView();
                                } else {
                                    d.clearRecip();
                                    showNote(R.string.error_InvalidRecipient);
                                    showRecipientSelect(requestCode);
                                    break;
                                }
                            } finally {
                                c.close();
                            }
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // clear the selection
                        d.clearRecip();
                        mImported = new MessageData();
                        setTab(Tabs.MESSAGE);
                        refreshView();
                        break;
                }
                break;

            case VIEW_RECIPSEL1:
                switch (resultCode) {
                    case PickRecipientsActivity.RESULT_SLINGKEYS:
                        setTab(Tabs.SLINGKEYS);
                        refreshView();
                        break;
                    case PickRecipientsActivity.RESULT_RECIPSEL:
                        long rowIdRecipient1 = data.getLongExtra(extra.RECIPIENT_ROW_ID, -1L);

                        if (d.existsRecip2() && rowIdRecipient1 == d.getRecip2RowId()) {
                            showNote(R.string.error_InvalidRecipient);
                            break;
                        }

                        RecipientDbAdapter dbRecipient = RecipientDbAdapter
                                .openInstance(getApplicationContext());
                        Cursor c = dbRecipient.fetchRecipient(rowIdRecipient1);
                        if (c != null) {
                            try {
                                if (c.moveToFirst()) {
                                    d.setRecip1(new RecipientRow(c));
                                    refreshView();
                                } else {
                                    showNote(R.string.error_InvalidRecipient);
                                    break;
                                }
                            } finally {
                                c.close();
                            }
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // clear the selection
                        d.clearRecip1();
                        setTab(Tabs.INTRO);
                        refreshView();
                        break;
                }
                break;

            case VIEW_RECIPSEL2:
                switch (resultCode) {
                    case PickRecipientsActivity.RESULT_SLINGKEYS:
                        setTab(Tabs.SLINGKEYS);
                        refreshView();
                        break;
                    case PickRecipientsActivity.RESULT_RECIPSEL:
                        long rowIdRecipient2 = data.getLongExtra(extra.RECIPIENT_ROW_ID, -1L);

                        if (d.existsRecip1() && rowIdRecipient2 == d.getRecip1RowId()) {
                            showNote(R.string.error_InvalidRecipient);
                            break;
                        }

                        RecipientDbAdapter dbRecipient = RecipientDbAdapter
                                .openInstance(getApplicationContext());
                        Cursor c = dbRecipient.fetchRecipient(rowIdRecipient2);
                        if (c != null) {
                            try {
                                if (c.moveToFirst()) {
                                    d.setRecip2(new RecipientRow(c));
                                    refreshView();
                                } else {
                                    showNote(R.string.error_InvalidRecipient);
                                    break;
                                }
                            } finally {
                                c.close();
                            }
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // clear the selection
                        d.clearRecip2();
                        setTab(Tabs.INTRO);
                        refreshView();
                        break;
                }
                break;

            case VIEW_SETTINGS_ID:
                // handle potential language change
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    recreate();
                } else {
                    // TODO: find better update for <= 2.3
                    // startActivity(getIntent());
                    // finish();
                }

                switch (resultCode) {
                    case SettingsActivity.RESULT_NEW_PASSPHRASE:
                        // remove cached pass
                        SafeSlinger.removeCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
                        SafeSlinger.startCacheService(HomeActivity.this);
                        showPassPhrase(false, true);
                        break;
                    case SettingsActivity.RESULT_LOGOUT:
                        doManualLogout();
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
                            refreshView();
                            break;
                        }
                        break;
                    case RESULT_CANCELED:
                        refreshView();
                        break;
                }
                break;

            case VIEW_EXCHANGE_ID:
                switch (resultCode) {
                    case ExchangeActivity.RESULT_EXCHANGE_OK:
                        showSave(data.getExtras());
                        break;
                    case ExchangeActivity.RESULT_EXCHANGE_CANCELED:
                        SafeSlinger.getApplication().setExchangeActive(false);
                        // update foreground notification to allow pending
                        // intents
                        SafeSlinger.startCacheService(HomeActivity.this);
                        break;
                }
                break;

            case VIEW_SAVE_ID:
                switch (resultCode) {
                    case SaveActivity.RESULT_SAVE:
                        // locally store trusted exchanged items
                        Bundle args = data.getExtras();
                        args.putInt(extra.RECIP_SOURCE, RecipientDbAdapter.RECIP_SOURCE_EXCHANGE);
                        ImportFromExchangeTask importFromExchange = new ImportFromExchangeTask();
                        importFromExchange.execute(args);
                        break;
                    case SaveActivity.RESULT_SELNONE:
                        int exchanged = data.getExtras().getInt(extra.EXCHANGED_TOTAL);
                        showNote(String.format(getString(R.string.state_SomeContactsImported), "0/"
                                + exchanged));
                        SafeSlingerPrefs.setFirstExchangeComplete(true);
                        SafeSlinger.getApplication().setExchangeActive(false);
                        // update foreground notification to allow pending
                        // intents
                        SafeSlinger.startCacheService(HomeActivity.this);
                        break;
                    case RESULT_CANCELED:
                        showNote(String.format(getString(R.string.state_SomeContactsImported), "0"));
                        SafeSlinger.getApplication().setExchangeActive(false);
                        // update foreground notification to allow pending
                        // intents
                        SafeSlinger.startCacheService(HomeActivity.this);
                        break;
                    default:
                        showNote(String.format(getString(R.string.state_SomeContactsImported), "?"));
                        SafeSlinger.getApplication().setExchangeActive(false);
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
                            try {
                                if (c.moveToFirst()) {
                                    String contactLookupKey = c.getString(c
                                            .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                                    String name = c
                                            .getString(c
                                                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                                    // save these for lookup and display
                                    // purposes
                                    SafeSlingerPrefs.setContactLookupKey(contactLookupKey);
                                    SafeSlingerPrefs.setContactName(name);
                                }
                            } finally {
                                c.close();
                            }
                        }
                        refreshView();
                        break;
                    case RESULT_CANCELED:
                        refreshView();
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
                                path = SafeSlinger.getTempCameraFileUri().getPath();
                                mImported = SSUtil.addAttachmentFromPath(mImported, path);
                            } else {
                                String chosenFile = data.getStringExtra(extra.FNAME);
                                String chosenPath = data.getStringExtra(extra.FPATH);

                                if (chosenFile != null || chosenPath != null) {
                                    // from our own File Manager
                                    path = chosenPath + File.separator + chosenFile;
                                    mImported = SSUtil.addAttachmentFromPath(mImported, path);
                                } else if (data.getData() != null) {
                                    // String action = data.getAction();
                                    // act=null
                                    // act=android.intent.action.GET_CONTENT
                                    mImported = SSUtil.addAttachmentFromUri(this, mImported,
                                            data.getData(), null);
                                    long filesize = mImported.getFileSize();
                                    if (filesize <= 0) {
                                        showNote(R.string.error_CannotSendEmptyFile);
                                        refreshView();
                                        break;
                                    }
                                } else {
                                    // capture camera data from 2nd non-standard
                                    // return from hardware
                                    path = SafeSlinger.getTempCameraFileUri().getPath();
                                    mImported = SSUtil.addAttachmentFromPath(mImported, path);
                                }
                            }
                            if (mImported.getFileSize() > SafeSlingerConfig.MAX_FILEBYTES) {
                                showNote(String.format(
                                        getString(R.string.error_CannotSendFilesOver),
                                        SafeSlingerConfig.MAX_FILEBYTES));
                            } else {
                                // update draft
                                mImported = doSaveDraft(d.getRecip(), mImported);
                            }
                        } catch (OutOfMemoryError e) {
                            showNote(R.string.error_OutOfMemoryError);
                            refreshView();
                            break;
                        } catch (FileNotFoundException e) {
                            showNote(e);
                            refreshView();
                            break;
                        } catch (IOException e) {
                            showNote(e);
                            refreshView();
                            break;
                        }
                        break;
                    case RESULT_CANCELED:
                        mImported.removeFile();
                        break;
                    default:
                        break;
                }
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
        int totalUsers = SSUtil.getTotalUsers();
        for (int i = 0; i < totalUsers; i++) {
            long date = SafeSlingerPrefs.getKeyDate(i);
            String name = SafeSlingerPrefs.getContactName(i);
            if (i > SafeSlingerPrefs.getUser()) {
                recentKeys.add(new UserData(name, date, false));
            }
        }
        return recentKeys;
    }

    private class DecryptPendingTask extends AsyncTask<String, Message, String> {

        private static final int PROG_DECRYPTING = 0;
        private static final int PROG_DECRYPTING_DONE = 1;

        @Override
        protected String doInBackground(String... arg0) {
            String pass = arg0[0];
            MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
            InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(getApplicationContext());
            Message msg = new Message();

            Cursor c = dbInbox.fetchAllInboxDecryptPending();
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        do {
                            try {
                                MessageData inRow = new MessageRow(c, true);
                                StringBuilder keyidout = new StringBuilder();
                                msg = new Message();
                                msg.what = PROG_DECRYPTING;
                                msg.arg1 = inRow.isInboxTable() ? 1 : 0;
                                msg.arg2 = (int) inRow.getRowId();
                                publishProgress(msg);

                                byte[] plain = CryptTools.decryptMessage(inRow.getEncBody(), pass,
                                        keyidout);
                                MessagePacket push = new MessagePacket(plain);

                                // move encrypted message to decrypted
                                // storage...
                                // add decrypted
                                long rowIdMsg = dbMessage.createMessageDecrypted(inRow, push,
                                        keyidout.toString());
                                if (rowIdMsg == -1) {
                                    return null; // unable to save progress
                                } else {
                                    // remove encrypted
                                    dbInbox.deleteInbox(inRow.getRowId());
                                }
                                msg = new Message();
                                msg.what = PROG_DECRYPTING_DONE;
                                msg.arg1 = inRow.isInboxTable() ? 1 : 0;
                                msg.arg2 = (int) inRow.getRowId();
                                publishProgress(msg);

                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (CryptoMsgException e) {
                                e.printStackTrace();
                            } catch (GeneralException e) {
                                e.printStackTrace();
                            }
                        } while (c.moveToNext());
                    }
                } finally {
                    c.close();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Message... progress) {
            Message msg = progress[0];

            if (msg.what == PROG_DECRYPTING) {
                postProgressMsgList(msg.arg1 == 1, msg.arg2, getString(R.string.prog_decrypting));
            } else if (msg.what == PROG_DECRYPTING_DONE) {
                postProgressMsgList(msg.arg1 == 1, msg.arg2, null);
            }
        }
    }

    private void doSaveDownloadedFile(File file, MessageData recvMsg) {
        if (saveFileAtLocation(file, recvMsg)) {
            // back to messages, to tap for open...
            setTab(Tabs.MESSAGE);
            refreshView();
        } else {
            showNote(String.format(getString(R.string.error_FileSave), recvMsg.getFileName()));
            refreshView();
        }
    }

    public void sendAutomaticMessage(Bundle args, int recipSource) {
        long groupSentTime = System.currentTimeMillis();
        CryptoMsgProvider p = CryptoMsgProvider.createInstance(SafeSlinger.isLoggable());
        byte[] keyBytes = null;
        List<String> keyStr = new ArrayList<String>();
        String keyId = null;
        RecipientRow recip = null;
        int exchanged = 0;
        do {
            keyBytes = args.getByteArray(SafeSlingerConfig.APP_KEY_PUBKEY + exchanged);
            if (keyBytes != null) {
                keyStr.add(new String(keyBytes));
                exchanged++;
            }
        } while (keyBytes != null);

        List<MessageTransport> verifyMsgs = new ArrayList<MessageTransport>();

        for (int i = 0; i < keyStr.size(); i++) {
            String keyString = keyStr.get(i);
            if (!TextUtils.isEmpty(keyString)) {
                try {
                    keyId = p.ExtractKeyIDfromSafeSlingerString(keyString);
                } catch (CryptoMsgPeerKeyFormatException e) {
                    e.printStackTrace();
                }
            }

            if (!TextUtils.isEmpty(keyId)
                    && recipSource == RecipientDbAdapter.RECIP_SOURCE_EXCHANGE) {
                RecipientDbAdapter dbRecipient = RecipientDbAdapter
                        .openInstance(getApplicationContext());
                Cursor cr = dbRecipient.fetchRecipientByKeyId(keyId);
                if (cr != null) {
                    try {
                        if (cr.moveToFirst()) {
                            recip = new RecipientRow(cr);
                        }
                    } finally {
                        cr.close();
                    }
                }
            }

            if (recip == null) {
                // attempt to send other messages and continue
                continue;
            }

            MessageData sendMsg = new MessageData();
            // set sent time closest to UI command
            sendMsg.setDateSent(groupSentTime);
            String message = String.format(getString(R.string.label_messageAutomatedVerify),
                    SafeSlingerPrefs.getContactName(), recip.getName());
            sendMsg.setText(message);
            // user wants to post the file and notify recipient
            if (recip.getNotify() == SafeSlingerConfig.NOTIFY_NOPUSH) {
                // attempt to send other messages and continue
                continue;
            }

            // automatic, do not keep sling keys tab drafts
            verifyMsgs.add(new MessageTransport(recip, sendMsg, false));
        }

        doSendMessageStart(verifyMsgs.toArray(new MessageTransport[verifyMsgs.size()]));
    }

    private void doProcessSafeSlingerMimeType(MessageData recvMsg) {
        ContactStruct intro = null;
        String[] types = recvMsg.getFileType().split("/");
        if (types.length == 2) {
            if (types[1].compareToIgnoreCase(SafeSlingerConfig.MIMETYPE_FUNC_SECINTRO) == 0) {
                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());

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
                intro = parsedContacts.get(0);

                RecipientRow exchRecip = null;
                MessageRow inviteMsg = null;

                Cursor c = dbMessage.fetchMessageSmall(recvMsg.getRowId());
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            inviteMsg = new MessageRow(c, false);
                        }
                    } finally {
                        c.close();
                    }
                }
                if (inviteMsg == null) {
                    showNote(R.string.error_InvalidIncomingMessage);
                    return;
                }

                RecipientDbAdapter dbRecipient = RecipientDbAdapter
                        .openInstance(getApplicationContext());
                Cursor cr = dbRecipient.fetchRecipientByKeyId(inviteMsg.getKeyId());
                if (cr != null) {
                    try {
                        if (cr.moveToFirst()) {
                            exchRecip = new RecipientRow(cr);
                        }
                    } finally {
                        cr.close();
                    }
                }
                if (exchRecip == null) {
                    showNote(R.string.error_InvalidRecipient);
                    return;
                }

                String exchName = exchRecip.getName();
                String introName = intro.name.toString();
                byte[] introPhoto = intro.photoBytes;
                byte[] introPush = null;
                byte[] introPubKey = null;

                List<ContactMethod> contactmethodList = intro.contactmethodList;
                if (contactmethodList != null) {
                    for (ContactMethod item : contactmethodList) {
                        if (item.kind == android.provider.Contacts.KIND_IM) {
                            if (item.label.compareTo(SafeSlingerConfig.APP_KEY_PUBKEY) == 0) {
                                introPubKey = SSUtil.finalDecode(item.data.getBytes());
                            } else if (item.label.compareTo(SafeSlingerConfig.APP_KEY_PUSHTOKEN) == 0) {
                                introPush = SSUtil.finalDecode(item.data.getBytes());
                            }
                        }
                    }
                }

                showIntroductionInvite(exchName, introName, introPhoto, introPush, introPubKey,
                        recvMsg.getRowId());
            }
        } else {
            showNote(String.format(getString(R.string.error_FileSave), recvMsg.getFileName()));
            return;
        }
    }

    private class ImportFromExchangeTask extends AsyncTask<Bundle, String, String> {
        Bundle args = null;
        int recipSource = 0;
        String introkeyid = null;
        int imported = 0;

        @Override
        protected String doInBackground(Bundle... arg0) {
            args = arg0[0];
            recipSource = args.getInt(extra.RECIP_SOURCE);
            introkeyid = args.getString(extra.KEYID);

            publishProgress(getString(R.string.prog_SavingContactsToKeyDatabase));
            try {
                imported = doImportFromExchange(args, recipSource, introkeyid);
            } catch (OutOfMemoryError e) {
                return getString(R.string.error_OutOfMemoryError);
            } catch (SQLException e) {
                return getString(R.string.error_UnableToSaveRecipientInDB);
            } catch (GeneralException e) {
                return e.getLocalizedMessage();
            } catch (CryptoMsgPeerKeyFormatException e) {
                return e.getLocalizedMessage();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            showProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            endProgress();
            if (TextUtils.isEmpty(result)) {
                int exchanged = args.getInt(extra.EXCHANGED_TOTAL);
                if (imported < exchanged) {
                    showNote(String.format(getString(R.string.state_SomeContactsImported), imported
                            + "/" + exchanged));
                } else {
                    showNote(String
                            .format(getString(R.string.state_SomeContactsImported), imported));
                }

                // auto verify message only for direct slings, not introductions
                if (recipSource == RecipientDbAdapter.RECIP_SOURCE_EXCHANGE) {
                    sendAutomaticMessage(args, recipSource);
                }
            } else {
                showNote(result);
            }

            // if requested, logged in, try to decrypt pending
            String passCached = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
            if (SafeSlingerPrefs.getAutoDecrypt() && !TextUtils.isEmpty(passCached)) {
                DecryptPendingTask decryptPending = new DecryptPendingTask();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    decryptPending.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, passCached);
                else
                    decryptPending.execute(passCached);
                // decryptPending.execute(passCached);
            }

            // updated databases, restart to use...
            BackgroundSyncUpdatesTask backgroundSyncUpdates = new BackgroundSyncUpdatesTask();
            backgroundSyncUpdates.execute(new String());
            refreshView();

            // set the global completion here to be sure the
            SafeSlinger.getApplication().setExchangeActive(false);
            SafeSlingerPrefs.setFirstExchangeComplete(true);
            // update foreground notification to allow pending intents
            SafeSlinger.startCacheService(HomeActivity.this);
        }
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

        checkPlayServices();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            SafeSlinger.getApplication().setMessageFragActive(true);
        // if (sHandler == null) {
        // sHandler = new Handler();
        // }
        // sHandler.removeCallbacks(updateMainView);
        // sHandler.post(updateMainView);

        // require pass on wake...
        showPassphraseWhenExpired();
    }

    private boolean showPassphraseWhenExpired() {
        String contactName = SafeSlingerPrefs.getContactName();
        if (CryptTools.existsSecretKey(getApplicationContext()) && !TextUtils.isEmpty(contactName)) {
            if (SafeSlinger.isCacheEmpty()) {
                if (SafeSlinger.isAppVisible()) {
                    // update incoming messages if needed
                    if (SafeSlingerPrefs.getMessagesIncoming() > 0) {
                        SafeSlinger.getApplication().checkForMissedMessages();
                    }
                    showPassPhrase(false, false);
                    return true;
                } else {
                    // in the background, auto logout should close the activity
                    showExit(RESULT_OK);
                }
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            SafeSlinger.getApplication().setMessageFragActive(false);
        // restoreView();
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
            unregisterReceiver(mMsgIncomingReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        try {
            unregisterReceiver(mMsgOutgoingReceiver);
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
            String filepath = savedInstanceState.getString(extra.FPATH);
            if (!TextUtils.isEmpty(filepath)) {
                SafeSlinger.setTempCameraFileUri(Uri.fromFile(new File(filepath)));
            }

            setTab(Tabs.values()[savedInstanceState.getInt(extra.RECOVERY_TAB)]);
        }

        // several private members may need to be reloaded...
        refreshView();
    }

    private void refreshView() {
        // pass is good, determine what view to see...
        // if (sHandler == null) {
        // sHandler = new Handler();
        // }
        sHandler.removeCallbacks(updateMainView);
        sHandler.post(updateMainView);
    }

    protected void postProgressMsgList(boolean isInboxTable, long rowId, String msg) {
        try {
            if (mTabsAdapter != null) {
                MessagesFragment mf = (MessagesFragment) mTabsAdapter
                        .findFragmentByPosition(Tabs.MESSAGE.ordinal());
                if (mf != null) {
                    mf.postProgressMsgList(isInboxTable, rowId, msg);
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
                byte[] resp = null;
                try {
                    resp = mWeb.getMessage(msgHashBytes);
                } catch (MessageNotFoundException e) {
                    if (!dbInbox.updateInboxExpired(mInbox.getRowId())) {
                        return getString(R.string.error_UnableToUpdateMessageInDB);
                    }
                }

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
            } catch (MessagingException e) {
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
                refreshView();
            } else {
                showNote(error);
                refreshView();
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

                    // special attachments can be saved in db since they
                    // are occasional and small, not like large attachments
                    if (mRecvMsg.getFileType().startsWith(SafeSlingerConfig.MIMETYPE_CLASS + "/")) {
                        if (!dbMessage.updateRawSafeSlingerFile(mRowId, rawFile)) {
                            return getString(R.string.error_UnableToUpdateMessageInDB);
                        }
                    }
                }

            } catch (OutOfMemoryError e) {
                return getString(R.string.error_OutOfMemoryError);
            } catch (MessagingException e) {
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
                refreshView();
            }
        }
    }

    private class SendMessageTask extends AsyncTask<MessageTransport, Message, String> {
        private Handler mHandler = new Handler();
        private int mTxTotalSize = 0;
        private int mTotalMsgs = 0;

        private void publish(MessageData sendMsg, int resId) {
            Message msg = new Message();
            msg.what = resId;
            msg.arg1 = sendMsg.isInboxTable() ? 1 : 0;
            msg.arg2 = (int) sendMsg.getRowId();
            publishProgress(msg);
        }

        @Override
        protected String doInBackground(MessageTransport... mts) {
            mTotalMsgs = mts.length;
            String errorFinal = null;
            for (int i = 0; i < mTotalMsgs; i++) {
                String error = null;
                final WebEngine web = new WebEngine(HomeActivity.this,
                        SafeSlingerConfig.HTTPURL_MESSENGER_HOST);
                RecipientRow recip = mts[i].getRecipient();
                final MessageData sendMsg = mts[i].getMessageData();
                boolean keepDraft = mts[i].keepDraft();
                Runnable updateTxProgress = new Runnable() {

                    @Override
                    public void run() {
                        int txCurr = (int) web.get_txCurrentBytes();
                        int pct = (int) ((txCurr / (float) mTxTotalSize) * 100);
                        String str = String.format(getString(R.string.prog_SendingFile), "");
                        if (pct > 0 && pct < 100) {
                            postProgressMsgList(sendMsg.isInboxTable(), sendMsg.getRowId(),
                                    String.format("%s %d%%", str, pct));
                        } else {
                            postProgressMsgList(sendMsg.isInboxTable(), sendMsg.getRowId(),
                                    String.format("%s", str));
                        }
                        mHandler.postDelayed(this, MS_POLL_INTERVAL);
                    }
                };

                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
                RecipientDbAdapter dbRecipient = RecipientDbAdapter
                        .openInstance(getApplicationContext());
                try {
                    mTxTotalSize += 4; // version size

                    // encrypt file data...
                    publish(sendMsg, R.string.prog_encrypting);
                    String pass = SafeSlinger
                            .getCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
                    byte[] encFile = null;
                    byte[] encMsg = null;
                    byte[] rawFile = sendMsg.getFileData();
                    if (rawFile != null && rawFile.length > 0) {
                        encFile = CryptTools.encryptMessage(rawFile, pass, recip.getPubkey());
                    } else {
                        encFile = new byte[0];
                    }
                    mTxTotalSize += 4; // file len size
                    mTxTotalSize += encFile.length;

                    // format message data, including hash of encrypted file
                    // data...
                    publish(sendMsg, R.string.prog_generatingSignature);
                    byte[] msgData = new MessagePacket(SafeSlingerConfig.getVersionCode(),//
                            System.currentTimeMillis(), //
                            encFile.length,//
                            sendMsg.getFileName(), //
                            sendMsg.getFileType(),//
                            sendMsg.getText(),//
                            SafeSlingerPrefs.getContactName(),//
                            CryptTools.computeSha3Hash(encFile)//
                    ).getBytes();
                    if (msgData == null) {
                        throw new GeneralException(getString(R.string.error_InvalidMsg));
                    }

                    // encrypt message data...
                    publish(sendMsg, R.string.prog_encrypting);
                    encMsg = CryptTools.encryptMessage(msgData, pass, recip.getPubkey());
                    mTxTotalSize += 4; // msg len size
                    mTxTotalSize += encMsg.length;

                    // message id = hash of encrypted message data...
                    publish(sendMsg, R.string.prog_generatingSignature);
                    byte[] msgHashBytes = CryptTools.computeSha3Hash(encMsg);
                    sendMsg.setMsgHash(new String(Base64.encode(msgHashBytes, Base64.NO_WRAP)));
                    mTxTotalSize += 4; // hash len size
                    mTxTotalSize += msgHashBytes.length;

                    mTxTotalSize += 4; // token len size
                    mTxTotalSize += recip.getPushtoken().length();

                    // send...
                    mHandler.postDelayed(updateTxProgress, MS_POLL_INTERVAL);
                    web.postMessage(msgHashBytes, encMsg, encFile, recip.getPushtoken(),
                            recip.getNotify());
                    mHandler.removeCallbacks(updateTxProgress);

                    // file sent ok, recipient notified...
                    // update sent...
                    publish(sendMsg, R.string.prog_FileSent);

                    if (!dbMessage.updateMessageSent(sendMsg.getRowId(), recip.getName(),
                            recip.getKeyid(), sendMsg.getMsgHash(), sendMsg.getFileName(),
                            sendMsg.getFileSize(), sendMsg.getFileType(), sendMsg.getFileDir(),
                            sendMsg.getText(), MessageDbAdapter.MESSAGE_STATUS_COMPLETE_MSG)) {
                        error = getString(R.string.error_UnableToUpdateMessageInDB);
                    }

                } catch (OutOfMemoryError e) {
                    error = getString(R.string.error_OutOfMemoryError);
                } catch (MessagingException e) {
                    error = e.getLocalizedMessage();
                } catch (IOException e) {
                    error = e.getLocalizedMessage();
                } catch (GeneralException e) {
                    error = e.getLocalizedMessage();
                } catch (ClassNotFoundException e) {
                    error = e.getLocalizedMessage();
                } catch (CryptoMsgException e) {
                    error = e.getLocalizedMessage();
                } catch (MessageNotFoundException e) {
                    error = e.getLocalizedMessage();
                } finally {
                    mHandler.removeCallbacks(updateTxProgress);
                }

                if (!TextUtils.isEmpty(error)) {
                    errorFinal = error;

                    // update recipient if no longer registered
                    boolean notreg = web.isNotRegistered();
                    if (notreg && d.existsRecip()) {
                        if (!dbRecipient
                                .updateRecipientRegistrationState(d.getRecipRowId(), notreg)) {
                            // failure to update database error, not as critical
                            // as the registration loss...
                        }
                    }
                    if (keepDraft) {
                        // set queued message back to draft status
                        if (!dbMessage.updateDraftMessage(sendMsg.getRowId(), recip, sendMsg)) {
                            showNote(R.string.error_UnableToUpdateMessageInDB);
                        }
                    } else {
                        // queued draft should be removed
                        dbMessage.deleteMessage(sendMsg.getRowId());
                    }
                }
                // end progress after database/comm finished
                publish(sendMsg, 0);
            } // end for
            return errorFinal;
        }

        @Override
        protected void onProgressUpdate(Message... progress) {
            Message msg = progress[0];
            postProgressMsgList(msg.arg1 == 1, msg.arg2, msg.what != 0 ? getString(msg.what) : null);
        }

        @Override
        protected void onPostExecute(String error) {
            if (TextUtils.isEmpty(error)) {
                if (mTotalMsgs == 1) {
                    // no confirmation message, message detail list is enough
                    // send complete, remove secret sent
                    mImported = new MessageData();
                } else {
                    // more than 1 msg, likely separate recipients, show message
                    showNote(String.format(getString(R.string.state_MessagesSent), mTotalMsgs));
                }
            } else {
                showNote(error);
            }
            refreshView();
        }
    }

    private class UpdateServerRegistrationIdTask extends AsyncTask<SlingerIdentity, String, String> {
        private WebEngine mWeb = new WebEngine(HomeActivity.this,
                SafeSlingerConfig.HTTPURL_MESSENGER_HOST);

        @Override
        protected String doInBackground(SlingerIdentity... slingerIds) {
            SlingerIdentity sid = slingerIds[0];
            if (sid == null) {
                return getString(R.string.error_InvalidRecipient);
            }
            try {
                String pass = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
                if (!TextUtils.isEmpty(pass)) {
                    CryptoMsgPrivateData mine = CryptTools.getSecretKey(pass);
                    String keyId = mine.getKeyId();
                    SecureRandom sr = new SecureRandom();
                    byte[] nonce = new byte[32];
                    sr.nextBytes(nonce);
                    String pubkey = mine.getSignPubKey();
                    String SignKey = mine.getSignPriKey();

                    // is local old regs exist and have pass?
                    List<String> oldRegIdKeys = new ArrayList<String>();
                    if (SafeSlingerPrefs.existsPref(pref.PUSH_REG_ID_C2DM_INDIRECT, false)) {
                        oldRegIdKeys.add(pref.PUSH_REG_ID_C2DM_INDIRECT);
                    }
                    if (SafeSlingerPrefs.existsPref(pref.PUSH_REG_ID_C2DM_DIRECT, false)) {
                        oldRegIdKeys.add(pref.PUSH_REG_ID_C2DM_DIRECT);
                    }
                    if (SafeSlingerPrefs.existsPref(pref.PUSH_REG_ID_C2DM_DIRECT_1811, false)) {
                        oldRegIdKeys.add(pref.PUSH_REG_ID_C2DM_DIRECT_1811);
                    }
                    if (SafeSlingerPrefs.existsPref(pref.PUSH_REG_ID_C2DM_DIRECT_1812, false)) {
                        oldRegIdKeys.add(pref.PUSH_REG_ID_C2DM_DIRECT_1812);
                    }
                    boolean isOnlineUpdated = false;
                    for (String oldRegIdKey : oldRegIdKeys) {
                        if (!TextUtils.isEmpty(oldRegIdKey)) {
                            // fetch old local token
                            String senderPushRegId = SafeSlingerPrefs.getString(oldRegIdKey, null,
                                    false);
                            int notifyType = SafeSlingerConfig.NOTIFY_ANDROIDC2DM;

                            // only upload valid registration ids
                            if (!TextUtils.isEmpty(senderPushRegId)
                                    && !senderPushRegId
                                            .equals(SafeSlingerConfig.NOTIFY_NOPUSH_TOKENDATA)) {
                                // post old regs
                                byte[] result = mWeb.postRegistration(keyId, senderPushRegId,
                                        notifyType, nonce, pubkey, SignKey);

                                // remove local old regs
                                if (result != null) {
                                    // remove the pref so we won't try and add
                                    // online after this one time update
                                    SafeSlingerPrefs.removePref(oldRegIdKey, false);
                                    isOnlineUpdated = true;
                                }
                            }
                        }
                    }

                    // if we updated old reg ids online, we need to be sure last
                    // online is recent one
                    String senderPushRegId = sid.getToken();
                    int notifyType = sid.getNotification();
                    if ((isOnlineUpdated || !SafeSlingerPrefs.getPushRegistrationIdPosted())) {

                        // only upload valid registration ids
                        if (!TextUtils.isEmpty(senderPushRegId)) {
                            // post local active reg
                            byte[] result = mWeb.postRegistration(keyId, senderPushRegId,
                                    notifyType, nonce, pubkey, SignKey);

                            // update local active regisLinked
                            if (result != null) {
                                SafeSlingerPrefs.setPushRegistrationIdPosted(true);
                            } else {
                                SafeSlingerPrefs.setPushRegistrationId(null); // clear
                                SafeSlingerPrefs.setPushRegistrationIdPosted(false); // reset
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CryptoMsgException e) {
                e.printStackTrace();
            } catch (MessagingException e) {
                SafeSlingerPrefs.setPushRegistrationId(null); // clear
                SafeSlingerPrefs.setPushRegistrationIdPosted(false); // reset
                e.printStackTrace();
            } catch (MessageNotFoundException e) {
                SafeSlingerPrefs.setPushRegistrationId(null); // clear
                SafeSlingerPrefs.setPushRegistrationIdPosted(false); // reset
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String error) {
            if (!TextUtils.isEmpty(error)) {
                showNote(error);
            }
        }
    }

    private void doGetPushRegistration() {

        // play services is required
        if (!checkPlayServices()) {
            showErrorExit(R.string.error_C2DMRegPlayServicesMissing);
            return;
        }

        // user must be online...
        if (!SafeSlinger.getApplication().isOnline()) {
            showErrorExit(R.string.error_CorrectYourInternetConnection);
            return;
        }

        C2DMessaging.registerInBackground(this);
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

            // with thumbnails, do not show automatically...

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            // TODO: determine if still works on 4.4.2 android devices
            // Fix for 4.0.3
            intent.putExtra("finishActivityOnSaveCompleted", true);
        }
        if (!TextUtils.isEmpty(name)) {
            intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
        }
        boolean actionAvailable = getPackageManager().resolveActivity(intent, 0) != null;
        if (actionAvailable) {
            startActivityForResult(intent, RESULT_PICK_CONTACT_SENDER);
        } else {
            showNote(SafeSlinger.getUnsupportedFeatureString("Insert Contact"));
        }
    }

    private void showErrorExit(String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            try {
                removeDialog(DIALOG_ERREXIT);
                showDialog(DIALOG_ERREXIT, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
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
        SafeSlinger.appClosing();
    }

    private void reInitForExit() {
        SafeSlingerPrefs.setPusgRegBackoff(SafeSlingerPrefs.DEFAULT_PUSHREG_BACKOFF);
        sProg = null;
        sProgressMsg = null;
        mImported = new MessageData();
        SafeSlinger.getApplication().setExchangeActive(false);
        // update foreground notification to allow pending intents
        SafeSlinger.startCacheService(HomeActivity.this);
    }

    private void showFileAttach() {
        final List<Intent> allIntents = new ArrayList<Intent>();

        // all openable...
        Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentIntent.setType(SafeSlingerConfig.MIMETYPE_ADD_ATTACH);
        contentIntent.addCategory(Intent.CATEGORY_OPENABLE);

        // camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        SafeSlinger.setTempCameraFileUri(SSUtil.makeCameraOutputUri());
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, SafeSlinger.getTempCameraFileUri());
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
        if (!SafeSlinger.isPassphraseOpen()) {
            if (sHandler != null) {
                sHandler.removeCallbacks(checkPassExpiration);
            }
            Intent intent = new Intent(HomeActivity.this, FindContactActivity.class);
            startActivityForResult(intent, VIEW_FINDCONTACT_ID);
            SafeSlinger.setPassphraseOpen(true);
        }
    }

    private void showRecipientSelect(int requestCode) {
        Intent intent = new Intent(HomeActivity.this, PickRecipientsActivity.class);
        intent.putExtra(extra.ALLOW_EXCH, true);
        intent.putExtra(extra.ALLOW_INTRO, true);
        startActivityForResult(intent, requestCode);
    }

    private void showEditContact(int requestCode) {
        if (!SafeSlinger.doesUserHavePermission(Manifest.permission.READ_CONTACTS)) {
            showNote(R.string.iOS_RequestPermissionContacts);
            return;
        }
        String contactLookupKey = SafeSlingerPrefs.getContactLookupKey();
        Uri personUri = getPersonUri(contactLookupKey);
        if (personUri != null) {
            Intent intent = new Intent(Intent.ACTION_EDIT, personUri);
            boolean actionAvailable = getPackageManager().resolveActivity(intent, 0) != null;
            if (actionAvailable) {
                startActivityForResult(intent, requestCode);
            } else {
                showNote(SafeSlinger.getUnsupportedFeatureString("Edit Contact"));
            }
        } else {
            showNote(R.string.error_ContactUpdateFailed);
        }
    }

    private void showPickContact(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        boolean actionAvailable = getPackageManager().resolveActivity(intent, 0) != null;
        if (actionAvailable) {
            startActivityForResult(intent, requestCode);
        } else {
            showNote(SafeSlinger.getUnsupportedFeatureString("Pick Contact"));
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
            intent.putExtra(extra.USER_TOTAL, SSUtil.getTotalUsers());
            intent.putExtra(extra.CREATE_PASS_PHRASE, create);
            intent.putExtra(extra.CHANGE_PASS_PHRASE, change);
            startActivityForResult(intent, change ? VIEW_PASSPHRASE_CHANGE_ID : VIEW_PASSPHRASE_ID);
            SafeSlinger.setPassphraseOpen(true);
        }
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
            try {
                removeDialog(DIALOG_MANAGE_PASS);
                showDialog(DIALOG_MANAGE_PASS, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
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
                    refreshView();
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
        int totalUsers = SSUtil.getTotalUsers();
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
            byte[] introPush, byte[] introPubKey, long msgRowId) {
        Bundle args = new Bundle();
        args.putString(extra.EXCH_NAME, exchName);
        args.putString(extra.INTRO_NAME, introName);
        args.putByteArray(extra.PHOTO, introPhoto);
        args.putByteArray(extra.PUSH_REGISTRATION_ID, introPush);
        args.putByteArray(extra.INTRO_PUBKEY, introPubKey);
        args.putLong(extra.MESSAGE_ROW_ID, msgRowId);
        if (!isFinishing()) {
            try {
                removeDialog(DIALOG_INTRO);
                showDialog(DIALOG_INTRO, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    private AlertDialog.Builder xshowIntroductionInvite(final Activity act, final Bundle args) {
        String exchName = args.getString(extra.EXCH_NAME);
        final String introName = args.getString(extra.INTRO_NAME);
        final byte[] introPhoto = args.getByteArray(extra.PHOTO);
        final byte[] introPush = args.getByteArray(extra.PUSH_REGISTRATION_ID);
        final byte[] introPubKey = args.getByteArray(extra.INTRO_PUBKEY);
        final long msgRowId = args.getLong(extra.MESSAGE_ROW_ID, -1L);
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
                int selected = 0;
                args.putString(extra.NAME + selected, introName);
                args.putByteArray(extra.PHOTO + selected, introPhoto);
                args.putByteArray(SafeSlingerConfig.APP_KEY_PUBKEY + selected, introPubKey);
                args.putByteArray(SafeSlingerConfig.APP_KEY_PUSHTOKEN + selected, introPush);

                String contactLookupKey = getContactLookupKeyByName(introName);
                args.putString(extra.CONTACT_LOOKUP_KEY + selected, contactLookupKey);

                MessageRow inviteMsg = null;
                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getApplicationContext());
                Cursor c = dbMessage.fetchMessageSmall(msgRowId);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            inviteMsg = new MessageRow(c, false);
                        }
                    } finally {
                        c.close();
                    }
                }

                if (inviteMsg == null) {
                    showNote(R.string.error_InvalidIncomingMessage);
                    return;
                }

                // import the new contacts
                args.putInt(extra.RECIP_SOURCE, RecipientDbAdapter.RECIP_SOURCE_INTRODUCTION);
                args.putString(extra.KEYID, inviteMsg.getKeyId());
                ImportFromExchangeTask importFromExchange = new ImportFromExchangeTask();
                importFromExchange.execute(args);
                setTab(Tabs.MESSAGE);
                refreshView();
            }
        });
        ad.setNegativeButton(getString(R.string.btn_Refuse), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                showNote(String.format(getString(R.string.state_SomeContactsImported), 0));
                refreshView();
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
            try {
                removeDialog(DIALOG_USEROPTIONS);
                showDialog(DIALOG_USEROPTIONS, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
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

        if (SafeSlinger.doesUserHavePermission(Manifest.permission.READ_CONTACTS)) {
            items.add(new UseContactItem(act.getString(R.string.menu_UseAnother), UCType.ANOTHER));
            items.add(new UseContactItem(act.getString(R.string.menu_CreateNew), UCType.NEW));
        }

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
                        d = new BitmapDrawable(getResources(), bm);
                    } else {
                        d = getResources().getDrawable(R.drawable.ic_silhouette);
                    }
                    int avatar_size_list = (int) getResources().getDimension(
                            R.dimen.avatar_size_list);
                    d.setBounds(0, 0, avatar_size_list, avatar_size_list);
                    tv.setCompoundDrawables(null, null, d, null);
                    tv.setCompoundDrawablePadding((int) getResources().getDimension(
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
                        refreshView();
                        break;
                    case CONTACT:
                        // user wants to use found contact as a personal contact
                        SafeSlingerPrefs.setContactName(getContactName(items.get(item).contactLookupKey));
                        SafeSlingerPrefs.setContactLookupKey(items.get(item).contactLookupKey);
                        refreshView();
                        break;
                    case ANOTHER:
                        // user wants to choose new contact for themselves
                        showPickContact(RESULT_PICK_CONTACT_SENDER);
                        break;
                    case NONE:
                        // user wants to remove link to address book
                        SafeSlingerPrefs.setContactLookupKey(null);
                        refreshView();
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
                        refreshView();
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
            try {
                removeDialog(DIALOG_PROGRESS);
                showDialog(DIALOG_PROGRESS, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    private void showProgress(String msg, int maxValue, int newValue) {
        Bundle args = new Bundle();
        args.putInt(extra.PCT, newValue);
        args.putInt(extra.MAX, maxValue);
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            try {
                removeDialog(DIALOG_PROGRESS);
                showDialog(DIALOG_PROGRESS, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
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
        setProgressCancelHandler();

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

    private void showProgressUpdate(String msg, int maxValue, int newValue) {
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
            try {
                removeDialog(DIALOG_QUESTION);
                showDialog(DIALOG_QUESTION, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
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
        return !TextUtils.isEmpty(currentPassPhrase);
    }

    @Override
    public void onBackPressed() {
        supportInvalidateOptionsMenu(); // update action bar options

        final int position = getSupportActionBar().getSelectedNavigationIndex();
        if (d.existsRecip() && position == Tabs.MESSAGE.ordinal()) {
            // collapse messages to threads when in message view
            MessagesFragment.removeRecip();
            refreshView();
        } else {
            // exit when at top level of each tab
            super.onBackPressed();
            showExit(RESULT_CANCELED);
        }
    }

    protected void showWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        boolean actionAvailable = getPackageManager().resolveActivity(intent, 0) != null;
        if (actionAvailable) {
            startActivity(intent);
        } else {
            showNote(SafeSlinger.getUnsupportedFeatureString("View Web Page"));
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                MyLog.i(TAG, "This device is not supported.");
            }
            return false;
        }
        return true;
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
