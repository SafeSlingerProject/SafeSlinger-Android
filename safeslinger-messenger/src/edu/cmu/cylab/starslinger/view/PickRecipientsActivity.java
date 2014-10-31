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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientNameKeyDateComparator;
import edu.cmu.cylab.starslinger.model.RecipientRow;

public class PickRecipientsActivity extends BaseActivity implements OnItemClickListener {
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    protected static final int RESULT_RECIPSEL = 4;
    protected static final int RESULT_SLINGKEYS = 5;

    private boolean mallowExch = false;
    private boolean mallowIntro = false;
    private List<RecipientRow> mcontacts = new ArrayList<RecipientRow>();
    private ListView listViewRecipients;
    private TextView tvInstruct;
    private String mySecretKeyId;
    private String myPushToken;
    private String myName;
    private CheckBox cbMostRecentOnly;
    private static int mListVisiblePos;
    private static int mListTopOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_SafeSlinger);
        super.onCreate(savedInstanceState);

        // inject view
        setContentView(R.layout.pickrecipients);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.app_name);
        bar.setSubtitle(R.string.title_PickRecipient);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        cbMostRecentOnly = (CheckBox) findViewById(R.id.ShowRecentCheckBox);
        listViewRecipients = (ListView) findViewById(R.id.RecipPickTableLayoutMembers);
        tvInstruct = (TextView) findViewById(R.id.tvInstruct);

        // always default to checked on view creation
        SafeSlingerPrefs.setShowRecentRecipOnly(true);

        cbMostRecentOnly.setChecked(SafeSlingerPrefs.getShowRecentRecipOnly());

        listViewRecipients.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // nothing to do...
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                // save list position
                if (visibleItemCount != 0) {
                    mListVisiblePos = firstVisibleItem;
                    View v = listViewRecipients.getChildAt(0);
                    mListTopOffset = (v == null) ? 0 : v.getTop();
                }
            }
        });

        cbMostRecentOnly.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SafeSlingerPrefs.setShowRecentRecipOnly(isChecked);
                updateValues(null);
            }
        });

        updateValues(getIntent().getExtras());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuItem iAdd = menu.add(0, MENU_CONTACTINVITE, 0, R.string.menu_SelectShareApp).setIcon(
                R.drawable.ic_action_add_person);

        MenuCompat.setShowAsAction(iAdd, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        MenuItem iHelp = menu.add(0, MENU_HELP, 0, R.string.menu_Help).setIcon(
                R.drawable.ic_action_help);
        MenuCompat.setShowAsAction(iHelp, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        menu.add(0, MENU_CONTACTINVITE, 0, R.string.menu_SelectShareApp).setIcon(
                R.drawable.ic_action_add_person);
        menu.add(0, MENU_FEEDBACK, 0, R.string.menu_sendFeedback).setIcon(
                android.R.drawable.ic_menu_send);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HELP:
                showHelp(getString(R.string.title_PickRecipient),
                        getString(R.string.help_PickRecip));
                return true;
            case MENU_FEEDBACK:
                SafeSlinger.getApplication().showFeedbackEmail(PickRecipientsActivity.this);
                return true;
            case MENU_CONTACTINVITE:
                showAddContactInvite();
                return true;
            default:
                break;
        }
        return false;
    }

    private void updateValues(Bundle extras) {

        if (extras != null) {
            mallowExch = extras.getBoolean(extra.ALLOW_EXCH);
            mallowIntro = extras.getBoolean(extra.ALLOW_INTRO);
        }

        // draw list
        mcontacts.clear();
        StringBuilder inst = new StringBuilder();
        if (mallowExch && mallowIntro) {
            inst.append(getText(R.string.label_InstRecipients));
        } else if (mallowExch) {
            inst.append(getText(R.string.label_InstSendInvite));
        }
        tvInstruct.setText(inst);

        myName = SafeSlingerPrefs.getContactName();
        mySecretKeyId = SafeSlingerPrefs.getKeyIdString();
        myPushToken = SafeSlingerPrefs.getPushRegistrationId();

        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this);
        Cursor c = null;
        if (mallowExch && mallowIntro) {
            c = dbRecipient.fetchAllRecipientsMessage(mySecretKeyId, myPushToken, myName);
        } else if (mallowExch) {
            c = dbRecipient.fetchAllRecipientsIntro(mySecretKeyId, myPushToken, myName);
        }
        if (c != null) {
            if (c.getCount() <= 0) {
                // turn reminder on so they will know how to find recipients
                SafeSlingerPrefs.setShowSlingKeysReminder(true);

                tvInstruct.setText(R.string.label_InstNoRecipients);
                cbMostRecentOnly.setVisibility(View.GONE);
            } else {
                cbMostRecentOnly.setVisibility(View.VISIBLE);
            }
            while (c.moveToNext()) {
                RecipientRow recipientRow = new RecipientRow(c);

                boolean foundNewer = false;
                // ignore if needed...
                if (cbMostRecentOnly.isChecked() && !recipientRow.isInvited()) {
                    int newerRecips = dbRecipient.getAllNewerRecipients(recipientRow, true);
                    if (newerRecips > 0) {
                        // there are some newer keys, this should not be
                        foundNewer = true;
                    }
                }

                if (!foundNewer) {
                    mcontacts.add(recipientRow);
                }
            }
            c.close();
        }

        // display relative number of secured contacts...
        int totalAdressBookContacts = getTotalUniqueAddressBookContacts();
        int totalRecipientContacts = getTotalUniqueRecipientContacts(mcontacts);
        if (totalAdressBookContacts > 0) {
            getSupportActionBar().setSubtitle(
                    String.format("%s (%d/%d)", getText(R.string.title_PickRecipient),
                            totalRecipientContacts, totalAdressBookContacts - 1));
        }

        // sort by name
        Collections.sort(mcontacts, new RecipientNameKeyDateComparator());

        unregisterForContextMenu(listViewRecipients);
        RecipientAdapter adapter = new RecipientAdapter(this, mcontacts);
        listViewRecipients.setAdapter(adapter);
        listViewRecipients.setOnItemClickListener(this);

        // restore list position
        listViewRecipients.setSelectionFromTop(mListVisiblePos, mListTopOffset);

        registerForContextMenu(listViewRecipients);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        RecipientRow recip = mcontacts.get(pos);
        boolean doselection = true;

        if (recip.isInvited()) {
            doSlingKeys();
        } else {
            RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this);
            int newerRecips = dbRecipient.getAllNewerRecipients(recip, false);
            if (newerRecips > 0) {
                // there are some newer keys, we should warn them
                showQuestion(
                        String.format(getString(R.string.ask_ConfirmUseOlderDeviceOrKey),
                                recip.getName()), pos);
                doselection = false;
            }

            if (doselection) {
                doRecipientSelection(recip);
            }
        }
    }

    public void doSlingKeys() {
        setResult(RESULT_SLINGKEYS);
        finish();
    }

    public void doRecipientSelection(RecipientRow recip) {
        boolean pushable = recip.isPushable();
        boolean fromExch = recip.isFromTrustedSource();
        boolean secretChanged = recip.hasMyKeyChanged();
        boolean deprecated = recip.isDeprecated();
        boolean useableKey = pushable && !secretChanged && !deprecated && fromExch;
        if (useableKey) {
            Intent data = new Intent();
            data.putExtra(extra.RECIPIENT_ROW_ID, recip.getRowId());
            setResult(RESULT_RECIPSEL, data);
            finish();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.recipcontext, menu);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        menu.add(Menu.NONE, R.id.item_key_details, Menu.NONE, R.string.menu_Details);
        menu.add(Menu.NONE, R.id.item_delete_recipient, Menu.NONE, R.string.menu_delete);
        if (mcontacts.get(info.position).getSource() != RecipientDbAdapter.RECIP_SOURCE_INVITED) {
            if (!mcontacts.get(info.position).isValidContactLink()) {
                menu.add(Menu.NONE, R.id.item_link_contact_add, Menu.NONE,
                        R.string.menu_link_contact_add);
            } else {
                menu.add(Menu.NONE, R.id.item_link_contact_change, Menu.NONE,
                        R.string.menu_link_contact_change);
            }

            if (mcontacts.get(info.position).isValidContactLink()) {
                menu.add(Menu.NONE, R.id.item_edit_contact, Menu.NONE, R.string.menu_EditContact);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        RecipientRow recip = mcontacts.get(info.position);
        if (item.getItemId() == R.id.item_key_details) {
            showHelp(getString(R.string.title_RecipientDetail),
                    formatRecpientDetails(PickRecipientsActivity.this, recip));
            return true;
        } else if (item.getItemId() == R.id.item_delete_recipient) {
            doDeleteRecipient(recip);
            updateValues(null);
            return true;
        } else if (item.getItemId() == R.id.item_link_contact_add
                || item.getItemId() == R.id.item_link_contact_change) {
            showUpdateContactLink(recip.getRowId());
            return true;
        } else if (item.getItemId() == R.id.item_edit_contact) {
            showEditContact(recip.getContactlu());
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    public void doDeleteRecipient(RecipientRow recip) {
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this);
        if (dbRecipient.deleteRecipient(recip.getRowId())) {
            showNote(String.format(getString(R.string.state_RecipientsDeleted), 1));
        }
    }

    private void showQuestion(String msg, int position) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_MSG, msg);
        args.putInt(extra.POSITION, position);
        if (!isFinishing()) {
            removeDialog(DIALOG_QUESTION);
            showDialog(DIALOG_QUESTION, args);
        }
    }

    private AlertDialog.Builder xshowQuestion(Activity act, Bundle args) {
        String msg = args.getString(extra.RESID_MSG);
        int pos = args.getInt(extra.POSITION);
        final RecipientRow recip = mcontacts.get(pos);
        MyLog.i(TAG, msg);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_Question);
        ad.setMessage(msg);
        ad.setCancelable(false);
        ad.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                doRecipientSelection(recip);
            }
        });
        ad.setNegativeButton(R.string.btn_No, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_HELP:
                return xshowHelp(PickRecipientsActivity.this, args).create();
            case DIALOG_QUESTION:
                return xshowQuestion(PickRecipientsActivity.this, args).create();
            case DIALOG_CONTACTINVITE:
                return xshowAddContactInvite(this).create();
            case DIALOG_CONTACTTYPE:
                return xshowCustomContactPicker(this, args).create();
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

}
