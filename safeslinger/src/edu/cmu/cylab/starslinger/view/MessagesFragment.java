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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.ConfigData.extra;
import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgException;
import edu.cmu.cylab.starslinger.model.MessageData;
import edu.cmu.cylab.starslinger.model.MessageDateAscendingComparator;
import edu.cmu.cylab.starslinger.model.MessageDbAdapter;
import edu.cmu.cylab.starslinger.model.MessagePacket;
import edu.cmu.cylab.starslinger.model.MessageRow;
import edu.cmu.cylab.starslinger.model.MessageRow.MsgAction;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientRow;
import edu.cmu.cylab.starslinger.model.ThreadData;
import edu.cmu.cylab.starslinger.model.ThreadDateDecendingComparator;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class MessagesFragment extends SherlockFragment {
    private static final String TAG = ConfigData.LOG_TAG;
    public static final int RESULT_GETFILE = 7124;
    public static final int RESULT_GETMESSAGE = 7125;
    public static final int RESULT_EDITMESSAGE = 7126;
    public static final int RESULT_FWDMESSAGE = 7127;
    public static final int RESULT_SEND = 7128;
    public static final int RESULT_SAVE = 7129;

    private List<ThreadData> mThreadList = new ArrayList<ThreadData>();
    private List<MessageRow> mMessageList = new ArrayList<MessageRow>();
    private TextView mTvInstruct;
    private ListView mListViewMsgs;
    private ListView mListViewThreads;
    private MessagesAdapter mAdapterMsg;
    private ThreadsAdapter mAdapterThread;
    private NotificationManager mNm;
    private OnMessagesResultListener mResult;
    private static RecipientRow mRecip;
    private static int mListMsgVisiblePos;
    private static int mListMsgTopOffset;
    private static int mListThreadVisiblePos;
    private static int mListThreadTopOffset;
    private static EditText mEditTextMessage;
    private Button mButtonSend;
    private LinearLayout mComposeWidget;
    private static MessageData mDraft;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateValues(savedInstanceState);

        String ns = Context.NOTIFICATION_SERVICE;
        mNm = (NotificationManager) this.getActivity().getSystemService(ns);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        doSave(mEditTextMessage.getText().toString(), mRecip != null);

        // save
        if (mDraft != null && mDraft.getRowId() != -1) {
            outState.putLong(extra.MESSAGE_ROW_ID, mDraft.getRowId());
        }
        if (mRecip != null && mRecip.getRowId() != -1) {
            outState.putLong(extra.RECIPIENT_ROW_ID, mRecip.getRowId());
        }
    }

    public void updateValues(Bundle extras) {
        long msgRowId = -1;
        long recipRowId = 1;

        if (extras != null) {
            msgRowId = extras.getLong(extra.MESSAGE_ROW_ID, -1);
            recipRowId = extras.getLong(extra.RECIPIENT_ROW_ID, -1);

            // set position to top when incoming message in
            // background...
            MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity()
                    .getApplicationContext());
            if (dbMessage.fetchUnseenMessageCount() > 0) {
                mListThreadTopOffset = 0;
                mListThreadVisiblePos = 0;
            }

            RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this.getActivity()
                    .getApplicationContext());
            if (recipRowId != -1) {
                Cursor c = dbRecipient.fetchRecipient(recipRowId);
                if (c != null) {
                    mRecip = new RecipientRow(c);
                    c.close();
                }
            } else if (msgRowId == -1) {
                mRecip = null;
            }
        }

        updateMessageList(msgRowId != -1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vFrag = inflater.inflate(R.layout.messagelist, container, false);

        mTvInstruct = (TextView) vFrag.findViewById(R.id.tvInstruct);

        mListViewThreads = (ListView) vFrag.findViewById(R.id.listThread);
        mListViewThreads.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // nothing to do...
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                // save list position
                if (visibleItemCount != 0) {
                    mListThreadVisiblePos = firstVisibleItem;
                    View v = mListViewThreads.getChildAt(0);
                    mListThreadTopOffset = (v == null) ? 0 : v.getTop();
                }
            }
        });

        mListViewMsgs = (ListView) vFrag.findViewById(R.id.listMessage);
        mListViewMsgs.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // nothing to do...
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                // save list position
                if (visibleItemCount != 0) {
                    mListMsgVisiblePos = firstVisibleItem;
                    View v = mListViewMsgs.getChildAt(0);
                    mListMsgTopOffset = (v == null) ? 0 : v.getTop();
                }
            }
        });

        mComposeWidget = (LinearLayout) vFrag.findViewById(R.id.ComposeLayout);

        mEditTextMessage = (EditText) vFrag.findViewById(R.id.SendEditTextMessage);

        mButtonSend = (Button) vFrag.findViewById(R.id.SendButtonSend);
        mButtonSend.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // if soft input open, close it...
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mEditTextMessage.getWindowToken(), 0);

                // requested send from send button...
                if (!TextUtils.isEmpty(mEditTextMessage.getText())) {
                    doSend(mEditTextMessage.getText().toString(), mRecip != null);
                }
            }
        });

        mEditTextMessage.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    // requested send from keyboard...
                    if (!TextUtils.isEmpty(mEditTextMessage.getText())) {
                        doSend(mEditTextMessage.getText().toString(), mRecip != null);
                    }
                    return true;
                }
                return false;
            }
        });

        updateMessageList(true);

        return vFrag;
    }

    private void doSave(String text, boolean save) {
        Intent intent = new Intent();
        if (save) {
            intent.putExtra(extra.TEXT_MESSAGE, text);
            if (mDraft != null) {
                intent.putExtra(extra.MESSAGE_ROW_ID, mDraft.getRowId());
            }
            if (mRecip != null) {
                intent.putExtra(extra.RECIPIENT_ROW_ID, mRecip.getRowId());
            }
            // always keep local version, unless we need to delete
            if (mDraft != null && TextUtils.isEmpty(text)) {
                mDraft = null;
                mEditTextMessage.setTextKeepState("");
            }
            sendResultToHost(RESULT_SAVE, intent.getExtras());
        }
    }

    private void doSend(String text, boolean send) {
        Intent intent = new Intent();
        if (send) {
            // recipient required to send anything
            if (mDraft != null) {
                intent.putExtra(extra.MESSAGE_ROW_ID, mDraft.getRowId());
            }
            intent.putExtra(extra.TEXT_MESSAGE, text);
            if (mRecip != null) {
                intent.putExtra(extra.RECIPIENT_ROW_ID, mRecip.getRowId());
            }
            // remove local version after sending
            mDraft = null;
            mEditTextMessage.setTextKeepState("");
            sendResultToHost(RESULT_SEND, intent.getExtras());
        }
    }

    private void doGetMessage(MessageRow msg) {
        Intent intent = new Intent();
        intent.putExtra(extra.PUSH_MSG_HASH, msg.getMsgHash());
        intent.putExtra(extra.MESSAGE_ROW_ID, msg.getRowId());
        sendResultToHost(RESULT_GETMESSAGE, intent.getExtras());
    }

    private void doForward(MessageRow msg) {
        Intent intent = new Intent();
        intent.putExtra(extra.MESSAGE_ROW_ID, msg.getRowId());
        sendResultToHost(RESULT_FWDMESSAGE, intent.getExtras());
    }

    public void doEditMessage(MessageRow msg) {
        Intent intent = new Intent();
        intent.putExtra(extra.MESSAGE_ROW_ID, msg.getRowId());
        if (mRecip != null)
            intent.putExtra(extra.RECIPIENT_ROW_ID, mRecip.getRowId());
        sendResultToHost(RESULT_EDITMESSAGE, intent.getExtras());
    }

    public void doGetFile(MessageRow msg) {
        Intent intent = new Intent();
        intent.putExtra(extra.PUSH_MSG_HASH, msg.getMsgHash());
        intent.putExtra(extra.PUSH_FILE_NAME, msg.getFileName());
        intent.putExtra(extra.PUSH_FILE_TYPE, msg.getFileType());
        intent.putExtra(extra.PUSH_FILE_SIZE, msg.getFileSize());
        intent.putExtra(extra.MESSAGE_ROW_ID, msg.getRowId());
        sendResultToHost(RESULT_GETFILE, intent.getExtras());
    }

    private void setThreadListClickListener() {
        mListViewThreads.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {

                ThreadData thread = mThreadList.get(pos);
                if (mRecip == null) {
                    // requested messages list
                    // assign recipient
                    if (TextUtils.isEmpty(thread.getKeyId())) {
                        // able to view null key id messages
                        mRecip = RecipientRow.createEmptyRecipient();
                    } else {
                        RecipientDbAdapter dbRecipient = RecipientDbAdapter
                                .openInstance(getActivity().getApplicationContext());
                        Cursor c = dbRecipient.fetchRecipientByKeyId(thread.getKeyId());
                        if (c != null) {
                            // messages with matching key ids in database
                            mRecip = new RecipientRow(c);
                            c.close();
                        } else {
                            // messages without matching key ids
                            mRecip = RecipientRow.createKeyIdOnlyRecipient(thread.getKeyId());
                        }
                    }
                } else {
                    // requested threads list
                    // remove recipient
                    mRecip = null;
                }
                updateMessageList(true);
            }
        });
    }

    private void setMessageListClickListener() {
        mListViewMsgs.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                String pass = SafeSlinger.getCachedPassPhrase(ConfigData
                        .loadPrefKeyIdString(getActivity().getApplicationContext()));
                if (TextUtils.isEmpty(pass)) {
                    sendResultToHost(HomeActivity.RESULT_PASSPHRASE_EXPIRED, null);
                    return;
                }

                // construct activity to download message...
                MessageRow msg = mMessageList.get(pos);
                MsgAction action = msg.getMessageAction();

                // ensure last item remains fully in view
                if (pos == mMessageList.size() - 1) {
                    mListMsgTopOffset = 0;
                    mListMsgVisiblePos = pos;
                }

                switch (action) {
                    case DISPLAY_ONLY:
                    case MSG_PROGRESS:
                    case MSG_EXPIRED:
                        // no action...
                        break;
                    case MSG_EDIT:
                        doEditMessage(msg);
                        break;
                    case MSG_DOWNLOAD:
                        doGetMessage(msg);
                        break;
                    case MSG_DECRYPT:
                        doDecryptMessage(pass, msg);
                        break;
                    case FILE_DOWNLOAD_DECRYPT:
                        doGetFile(msg);
                        break;
                    case FILE_OPEN:
                        doOpenFile(msg);
                        break;
                }

                updateMessageList(false);
            }
        });
    }

    private void updateMessageList(boolean recentMsg) {
        // make sure view is already inflated...
        if (mListViewMsgs == null) {
            return;
        }

        String contactLookupKey = ConfigData.loadPrefContactLookupKey(this.getActivity()
                .getApplicationContext());
        byte[] myPhoto = ((BaseActivity) this.getActivity()).getContactPhoto(contactLookupKey);

        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity()
                .getApplicationContext());
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this.getActivity()
                .getApplicationContext());

        if (isResumed() && mRecip != null) {
            // when shown, current thread all are now seen
            dbMessage.updateAllMessagesAsSeenByThread(mRecip.getKeyid());

            // remove notify when every unseen thread has been seen
            if (dbMessage.fetchUnseenMessageCount() == 0) {
                mNm.cancel(HomeActivity.NOTIFY_NEW_MSG_ID);
            }
        }

        mTvInstruct.setVisibility(View.GONE);
        boolean showCompose = false;

        // draw threads list/title bar
        mThreadList.clear();
        Cursor ct;
        if (mRecip == null) {
            ct = dbMessage.fetchMessagesRecentByUniqueKeyIds();
        } else {
            ct = dbMessage.fetchAllMessagesByThread(mRecip.getKeyid());
            if (ct.getCount() <= 0) { // case selected key is bogus
                mRecip = null;
                ct = dbMessage.fetchMessagesRecentByUniqueKeyIds();
            }
        }
        if (ct != null) {
            if (ct.getCount() <= 0) {
                mTvInstruct.setVisibility(View.VISIBLE);
            } else {
                while (ct.moveToNext()) {
                    MessageRow messageRow = new MessageRow(ct);
                    RecipientRow recipientRow = null;
                    String person = null;
                    boolean newerExists = false;

                    Cursor cr = dbRecipient.fetchRecipientByKeyId(messageRow.getKeyId());
                    if (cr != null) {
                        recipientRow = new RecipientRow(cr);
                        cr.close();
                        person = recipientRow.getName();

                        Cursor crn = dbRecipient.fetchAllNewerRecipients(recipientRow, true);
                        if (crn != null) {
                            if (crn.moveToNext()) {
                                // there are some newer keys, we should warn
                                newerExists = true;
                            }
                            crn.close();
                        }
                    }

                    int msgs = 0;
                    Cursor cmt = dbMessage.fetchAllMessagesByThread(messageRow.getKeyId());
                    if (cmt != null) {
                        msgs = cmt.getCount();
                        while (cmt.moveToNext()) {
                            if (TextUtils.isEmpty(person)) {
                                MessageRow mr = new MessageRow(cmt);
                                person = mr.getPerson();
                            } else {
                                break;
                            }
                        }
                        cmt.close();
                    }

                    int newMsgs = dbMessage.fetchActionRequiredMessageCountByThread(messageRow
                            .getKeyId());
                    int draftMsgs = dbMessage.fetchDraftMessageCountByThread(messageRow.getKeyId());
                    mThreadList.add(new ThreadData(messageRow.getKeyId(), msgs, newMsgs, messageRow
                            .getProbableDate(), draftMsgs > 0, person, mRecip != null, newerExists,
                            recipientRow));
                    if (mRecip != null) {
                        if (recipientRow != null && recipientRow.isSendable(getActivity())
                                && !newerExists) {
                            showCompose = true;
                        }
                        break;
                    } else {
                        showCompose = false;
                    }
                }
                ct.close();
            }
        }
        Collections.sort(mThreadList, new ThreadDateDecendingComparator());

        if (showCompose) {
            mComposeWidget.setVisibility(View.VISIBLE);
        } else {
            mComposeWidget.setVisibility(View.GONE);
        }

        // draw messages list/compose draft
        mMessageList.clear();
        if (mRecip != null) {
            Cursor cm = dbMessage.fetchAllMessagesByThread(mRecip.getKeyid());
            if (cm != null) {
                while (cm.moveToNext()) {
                    RecipientRow recipientRow = null;
                    MessageRow messageRow = new MessageRow(cm);
                    Cursor cr = dbRecipient.fetchRecipientByKeyId(messageRow.getKeyId());
                    if (cr != null) {
                        recipientRow = new RecipientRow(cr);
                        cr.close();
                    }
                    if (!messageRow.isInbox()) {
                        messageRow.setPhoto(myPhoto);
                    } else {
                        if (recipientRow != null) {
                            messageRow.setPhoto(recipientRow.getPhoto());
                        }
                    }

                    if (mDraft == null
                            && messageRow.getStatus() == MessageDbAdapter.MESSAGE_STATUS_DRAFT
                            && TextUtils.isEmpty(messageRow.getFileName())
                            && mRecip.isSendable(getActivity())) {
                        // if recent draft, remove from list put in edit box
                        mDraft = messageRow;
                        mEditTextMessage.setTextKeepState(mDraft.getText());
                        mEditTextMessage.forceLayout();
                    } else if (mDraft != null && mDraft.getRowId() == messageRow.getRowId()) {
                        // draft has already been updated
                        continue;
                    } else {
                        // show message normally
                        mMessageList.add(messageRow);
                    }
                }
                cm.close();
            }
            Collections.sort(mMessageList, new MessageDateAscendingComparator());
        } else {
            // clear draft in thread view
            doSave(mEditTextMessage.getText().toString(), true);
            mDraft = null;
            mEditTextMessage.setTextKeepState("");
        }

        // set position to top when incoming message in foreground...
        if (recentMsg) {
            mListMsgTopOffset = 0;
            mListMsgVisiblePos = mMessageList.size() - 1;
        }

        unregisterForContextMenu(mListViewMsgs);
        unregisterForContextMenu(mListViewThreads);

        mAdapterThread = new ThreadsAdapter(this.getActivity(), mThreadList);
        mListViewThreads.setAdapter(mAdapterThread);
        setThreadListClickListener();
        mListViewThreads.setSelectionFromTop(mListThreadVisiblePos, mListThreadTopOffset);

        mAdapterMsg = new MessagesAdapter(this.getActivity(), mMessageList);
        mListViewMsgs.setAdapter(mAdapterMsg);
        setMessageListClickListener();
        mListViewMsgs.setSelectionFromTop(mListMsgVisiblePos, mListMsgTopOffset);

        registerForContextMenu(mListViewMsgs);
        registerForContextMenu(mListViewThreads);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.equals(mListViewThreads)) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            RecipientRow recip = mThreadList.get(info.position).getRecipient();
            String delThreadStr = String.format(
                    getString(R.string.menu_deleteThread),
                    ThreadsAdapter.getBestIdentityName(getActivity(),
                            mThreadList.get(info.position), recip));
            menu.add(Menu.NONE, R.id.item_delete_thread, Menu.NONE, delThreadStr);
            menu.add(Menu.NONE, R.id.item_thread_details, Menu.NONE, R.string.menu_Details);
        } else if (v.equals(mListViewMsgs)) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.layout.messagecontext, menu);

            menu.add(Menu.NONE, R.id.item_message_copytext, Menu.NONE,
                    R.string.menu_messageCopyText);
            menu.add(Menu.NONE, R.id.item_message_forward, Menu.NONE, R.string.menu_messageForward);
            menu.add(Menu.NONE, R.id.item_message_details, Menu.NONE, R.string.menu_Details);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        String pass = SafeSlinger.getCachedPassPhrase(ConfigData.loadPrefKeyIdString(this
                .getActivity().getApplicationContext()));
        if (TextUtils.isEmpty(pass)) {
            sendResultToHost(HomeActivity.RESULT_PASSPHRASE_EXPIRED, null);
            return false;
        }

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == R.id.item_delete_message) {
            doDeleteMessage(mMessageList.get(info.position));
            updateMessageList(false);
            return true;
        } else if (item.getItemId() == R.id.item_delete_thread) {
            doDeleteThread(mThreadList.get(info.position).getKeyId());
            updateMessageList(false);
            return true;
        } else if (item.getItemId() == R.id.item_thread_details) {
            showHelp(getString(R.string.title_RecipientDetail),
                    BaseActivity.formatThreadDetails(getActivity(), mThreadList.get(info.position)));
            return true;
        } else if (item.getItemId() == R.id.item_message_details) {
            showHelp(
                    getString(R.string.title_MessageDetail),
                    BaseActivity.formatMessageDetails(getActivity(),
                            mMessageList.get(info.position)));
            return true;
        } else if (item.getItemId() == R.id.item_message_copytext) {
            SafeSlinger.getApplication().copyPlainTextToClipboard(
                    mMessageList.get(info.position).getText());
            showNote(getString(R.string.state_TextCopiedToClipboard));
            return true;
        } else if (item.getItemId() == R.id.item_message_forward) {
            doForward(mMessageList.get(info.position));
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    private void doDecryptMessage(String pass, MessageRow msg) {
        try {
            StringBuilder keyidout = new StringBuilder();
            byte[] plain = CryptTools.decryptMessage((BaseActivity) this.getActivity(),
                    msg.getEncBody(), pass, keyidout);

            MessagePacket push = new MessagePacket(plain);
            MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity()
                    .getApplicationContext());
            if (!dbMessage.updateMessageDecrypted(msg.getRowId(), push, keyidout.toString())) {
                showNote(getString(R.string.error_UnableToUpdateMessageInDB));
            }

        } catch (IOException e) {
            showNote(e.getLocalizedMessage());
        } catch (GeneralException e) {
            showNote(e.getLocalizedMessage());
        } catch (ClassNotFoundException e) {
            showNote(e.getLocalizedMessage());
        } catch (CryptoMsgException e) {
            showNote(e.getLocalizedMessage());
        }
    }

    public void doOpenFile(MessageRow msg) {
        if (SSUtil.isExternalStorageReadable()) {
            File f;
            if (!TextUtils.isEmpty(msg.getFileDir())) {
                f = new File(msg.getFileDir());
            } else {
                f = SSUtil.getOldDefaultDownloadPath(msg.getFileType(), msg.getFileName());
            }
            ((BaseActivity) this.getActivity()).showFileActionChooser(f, msg.getFileType());
        } else {
            showNote(R.string.error_FileStorageUnavailable);
        }
    }

    public void doDeleteMessage(MessageRow msg) {
        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity()
                .getApplicationContext());
        if (dbMessage.deleteMessage(msg.getRowId())) {
            showNote(String.format(getString(R.string.state_MessagesDeleted), 1));
        }
    }

    public void doDeleteThread(String keyId) {
        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity()
                .getApplicationContext());
        int deleted = dbMessage.deleteThread(keyId);
        if (deleted > 0) {
            showNote(String.format(getString(R.string.state_MessagesDeleted), deleted));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // The activity has become not visible (it is now "paused").

        // save draft when view is lost
        doSave(mEditTextMessage.getText().toString(), mRecip != null);
    }

    @Override
    public void onResume() {
        super.onResume();
        // The activity has become visible (it is now "resumed").

        updateMessageList(false);
    }

    public interface OnMessagesResultListener {
        public void onMessageResultListener(Bundle args);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mResult = (OnMessagesResultListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + OnMessagesResultListener.class.getSimpleName());
        }
    }

    private void sendResultToHost(int resultCode, Bundle args) {
        if (args == null) {
            args = new Bundle();
        }
        args.putInt(extra.RESULT_CODE, resultCode);
        mResult.onMessageResultListener(args);
    }

    protected void showNote(int resId) {
        showNote(getString(resId));
    }

    protected void showNote(Exception e) {
        String msg = e.getLocalizedMessage();
        if (TextUtils.isEmpty(msg)) {
            showNote(e.getClass().getSimpleName());
        } else {
            showNote(msg);
        }
    }

    protected void showNote(String msg) {
        MyLog.i(TAG, msg);
        if (msg != null) {
            int readDuration = msg.length() * ConfigData.MS_READ_PER_CHAR;
            if (readDuration <= ConfigData.SHORT_DELAY) {
                Toast toast = Toast.makeText(this.getActivity(), msg, Toast.LENGTH_SHORT);
                toast.show();
            } else if (readDuration <= ConfigData.LONG_DELAY) {
                Toast toast = Toast.makeText(this.getActivity(), msg, Toast.LENGTH_LONG);
                toast.show();
            } else {
                showHelp(getString(R.string.app_name), msg);
            }
        }
    }

    protected void showHelp(String title, String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_TITLE, title);
        args.putString(extra.RESID_MSG, msg);
        DialogFragment newFragment = MessagesAlertDialogFragment.newInstance(
                BaseActivity.DIALOG_HELP, args);
        newFragment.show(getFragmentManager(), "dialog");
    }

    public static class MessagesAlertDialogFragment extends DialogFragment {

        public static MessagesAlertDialogFragment newInstance(int id) {
            return newInstance(id, new Bundle());
        }

        public static MessagesAlertDialogFragment newInstance(int id, Bundle args) {
            MessagesAlertDialogFragment frag = new MessagesAlertDialogFragment();
            args.putInt(extra.RESULT_CODE, id);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt(extra.RESULT_CODE);
            switch (id) {
                case BaseActivity.DIALOG_HELP:
                    return BaseActivity.xshowHelp(getActivity(), getArguments()).create();
            }
            return super.onCreateDialog(savedInstanceState);
        }
    }

    public void postProgressMsgList(long rowId, String msg) {
        for (int i = 0; i < mMessageList.size(); i++) {
            if (mMessageList.get(i).getRowId() == rowId) {
                MessageRow mr = mMessageList.get(i);
                mr.setProgress(msg);
                mMessageList.set(i, mr);

                // ensure last item remains fully in view
                if (rowId == mMessageList.get(mMessageList.size() - 1).getRowId()) {
                    mListMsgTopOffset = 0;
                    mListMsgVisiblePos = mMessageList.size() - 1;
                }

                mAdapterMsg = new MessagesAdapter(this.getActivity(), mMessageList);
                mListViewMsgs.setAdapter(mAdapterMsg);
                mListViewMsgs.setSelectionFromTop(mListMsgVisiblePos, mListMsgTopOffset);
                break;
            }
        }
        if (msg == null) {
            updateMessageList(false);
        }
    }

    public static void setRecip(RecipientRow recip) {
        mRecip = recip;
    }

    public static RecipientRow getRecip() {
        return mRecip;
    }

}
