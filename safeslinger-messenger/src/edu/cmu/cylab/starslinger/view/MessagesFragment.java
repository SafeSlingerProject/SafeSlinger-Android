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
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.crypto.CryptTools;
import edu.cmu.cylab.starslinger.crypto.CryptoMsgException;
import edu.cmu.cylab.starslinger.model.InboxDbAdapter;
import edu.cmu.cylab.starslinger.model.MessageData;
import edu.cmu.cylab.starslinger.model.MessageDateAscendingComparator;
import edu.cmu.cylab.starslinger.model.MessageDbAdapter;
import edu.cmu.cylab.starslinger.model.MessagePacket;
import edu.cmu.cylab.starslinger.model.MessageRow;
import edu.cmu.cylab.starslinger.model.MessageRow.MsgAction;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientRow;
import edu.cmu.cylab.starslinger.model.ThreadData;
import edu.cmu.cylab.starslinger.util.FragmentCommunicationInterface;
import edu.cmu.cylab.starslinger.util.SSUtil;
import edu.cmu.cylab.starslinger.util.ThreadContent;
import edu.cmu.cylab.starslinger.view.HomeActivity.Tabs;

public class MessagesFragment extends Fragment {
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    public static final int RESULT_GETFILE = 7124;
    public static final int RESULT_GETMESSAGE = 7125;
    public static final int RESULT_EDITMESSAGE = 7126;
    public static final int RESULT_FWDMESSAGE = 7127;
    public static final int RESULT_SEND = 7128;
    public static final int RESULT_SAVE = 7129;
    public static final int RESULT_PROCESS_SSMIME = 7130;

//    private List<ThreadData> mThreadList = new ArrayList<ThreadData>();
    private List<MessageRow> mMessageList = new ArrayList<MessageRow>();
    private TextView mTvInstruct;
    private ListView mListViewMsgs;
//    private ListView mListViewThreads;
    private MessagesAdapter mAdapterMsg;
//    private ThreadsAdapter mAdapterThread;
    private NotificationManager mNm;
    private OnMessagesResultListener mResult;
    private static RecipientRow mRecip;
    private static int mListMsgVisiblePos;
    private static int mListMsgTopOffset;
//    private static int mListThreadVisiblePos;
//    private static int mListThreadTopOffset;
    private static EditText mEditTextMessage;
    private Button mButtonSend;
    private LinearLayout mComposeWidget;
    private MessageData mDraft;
    private boolean mDraftSaved = false;
    
    private ThreadData mThreadData = null;
    private FragmentCommunicationInterface mListener;
    
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

        if(!mDraftSaved)
        	initiateSaveDraft();

        // save
        if (mDraft != null && mDraft.getRowId() != -1) {
            outState.putLong(extra.MESSAGE_ROW_ID, mDraft.getRowId());
        }
        if (mRecip != null && mRecip.getRowId() != -1) {
            outState.putLong(extra.RECIPIENT_ROW_ID, mRecip.getRowId());
        }
    }

    public void onThreadItemClick(Bundle extras)
    {
        if(extras != null)
        {
            mThreadData = (ThreadData)extras.getParcelable("thread_data");
            
            if (mRecip == null || TextUtils.isEmpty(mRecip.getName()) || (mThreadData.getRecipient() == null || mRecip.getName().compareTo(mThreadData.getRecipient().getName()) != 0)) {
                // requested messages list
                // assign recipient
                if (TextUtils.isEmpty(mThreadData.getMsgRow().getKeyId())) {
                    // able to view null key id messages
                    mRecip = RecipientRow.createEmptyRecipient();
                } else {
                    RecipientDbAdapter dbRecipient = RecipientDbAdapter
                            .openInstance(getActivity().getApplicationContext());
                    Cursor c = dbRecipient.fetchRecipientByKeyId(mThreadData.getMsgRow().getKeyId());
                    if (c != null) {
                        try {
                            if (c.moveToFirst()) {
                                // messages with matching key ids in
                                // database
                                mRecip = new RecipientRow(c);
                            } else {
                                // messages without matching key ids
                                mRecip = RecipientRow.createKeyIdOnlyRecipient(mThreadData.getMsgRow()
                                        .getKeyId());
                            }
                        } finally {
                            c.close();
                        }
                    }
                }
            } 
            
            updateMessageList(true);
        }
    }
    
    public void updateValues(Bundle extras) {
        long msgRowId = -1;
        long recipRowId = 1;

        if (extras != null) {
            msgRowId = extras.getLong(extra.MESSAGE_ROW_ID, -1L);
            recipRowId = extras.getLong(extra.RECIPIENT_ROW_ID, -1L);

            // set position to top when incoming message in
            // background...
//            InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(this.getActivity());
//            MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity());
//            int inCount = dbInbox.getUnseenInboxCount();
//            int msgCount = dbMessage.getUnseenMessageCount();
//            int allCount = inCount + msgCount;
//            if (allCount > 0) {
//                mListThreadTopOffset = 0;
//                mListThreadVisiblePos = 0;
//            }
            
            RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this.getActivity());
            if (recipRowId != -1) {
                Cursor c = dbRecipient.fetchRecipient(recipRowId);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            mRecip = new RecipientRow(c);
                        }
                    } finally {
                        c.close();
                    }
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
        vFrag.findViewById(R.id.listThread).setVisibility(View.GONE);
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
        mEditTextMessage.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				mDraftSaved = false;
				
			}
		});
        
        mButtonSend = (Button) vFrag.findViewById(R.id.SendButtonSend);
        mButtonSend.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // requested send from send button...
            	ThreadContent.getInstance().setmCurrentTab(Tabs.MESSAGE);
                doSend(mEditTextMessage.getText().toString(), mRecip != null);
            }
        });

        mEditTextMessage.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    // requested send from keyboard...
                	ThreadContent.getInstance().setmCurrentTab(Tabs.MESSAGE);
                    doSend(mEditTextMessage.getText().toString(), mRecip != null);
                    return true;
                }
                return false;
            }
        });
//        if(getArguments() != null)
//            mThreadData = getArguments().getParcelable("thread_data");
        
        if(getArguments() != null && getArguments().getBoolean("thread_click"))
            onThreadItemClick(getArguments());
        else if(getArguments() != null && getArguments().getBoolean("isOutGoing"))
        	updateValues(getArguments());
        else
            updateMessageList(false);

        return vFrag;
    }

    private void doSave(String text, boolean save) {
        Intent intent = new Intent();
        if (save) {
            intent.putExtra(extra.TEXT_MESSAGE, text.trim());
            if (mDraft != null) {
                intent.putExtra(extra.MESSAGE_ROW_ID, mDraft.getRowId());
            }
            if (mRecip != null) {
                intent.putExtra(extra.RECIPIENT_ROW_ID, mRecip.getRowId());
            }
            // always keep local version, unless we need to delete
            if (mDraft != null && !isSendableText()) {
                mDraft = null;
                mEditTextMessage.setTextKeepState("");
            }
            sendResultToHost(RESULT_SAVE, intent.getExtras());
            if(!TextUtils.isEmpty(text.trim()))
            	mDraftSaved = true;
        }
    }

    private void doSend(String text, boolean send) {
        Intent intent = new Intent();
        if (send && isSendableText()) {
            // recipient required to send anything
            if (mDraft != null) {
                intent.putExtra(extra.MESSAGE_ROW_ID, mDraft.getRowId());
            }
            intent.putExtra(extra.TEXT_MESSAGE, text.trim());
            if (mRecip != null) {
                intent.putExtra(extra.RECIPIENT_ROW_ID, mRecip.getRowId());
            }
            // remove local version after sending
            mDraft = null;
            mEditTextMessage.setTextKeepState("");
            sendResultToHost(RESULT_SEND, intent.getExtras());
        }
    }

    private void doGetMessage(MessageRow inbox) {

        long inboxRowId = -1;
        if (inbox.isInboxTable()) {
            inboxRowId = inbox.getRowId();
        } else {
            // TODO deprecate this old scheme handling in next release
            // move message from old table if we still need to download,
            MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity());
            InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(this.getActivity());
            final int seen = inbox.isSeen() ? MessageDbAdapter.MESSAGE_IS_SEEN
                    : MessageDbAdapter.MESSAGE_IS_NOT_SEEN;
            inboxRowId = dbInbox.createRecvEncInbox(inbox.getMsgHash(), inbox.getStatus(), seen);
            dbMessage.deleteMessage(inbox.getRowId());
        }

        Intent intent = new Intent();
        intent.putExtra(extra.PUSH_MSG_HASH, inbox.getMsgHash());
        intent.putExtra(extra.INBOX_ROW_ID, inboxRowId);
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

     private void setMessageListClickListener() {
        mListViewMsgs.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                String pass = SafeSlinger.getCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());

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
                    default:
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

        String contactLookupKey = SafeSlingerPrefs.getContactLookupKey();
        byte[] myPhoto = ((BaseActivity) this.getActivity()).getContactPhoto(contactLookupKey);

        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity());
        InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(this.getActivity());

        if (isResumed() && mRecip != null) {
            // when shown, current thread all are now seen
            dbMessage.updateAllMessagesAsSeenByThread(mRecip.getKeyid());
            dbInbox.updateAllInboxAsSeenByThread(mRecip.getKeyid());

            // remove notify when every unseen thread has been seen
            int inCount = dbInbox.getUnseenInboxCount();
            int msgCount = dbMessage.getUnseenMessageCount();
            int allCount = inCount + msgCount;
            if (allCount == 0) {
                mNm.cancel(HomeActivity.NOTIFY_NEW_MSG_ID);
            }
        }

        mTvInstruct.setVisibility(View.GONE);
        boolean showCompose = false;

        // draw messages list/compose draft
        mMessageList.clear();
        if (mRecip != null) {
            // recipient data
            if (mRecip != null && mRecip.isSendable() /*&& mThreadData != null && !mThreadData.isNewerExists()*/) {
                showCompose = true;
            }
            // encrypted msgs
            Cursor ci = dbInbox.fetchAllInboxByThread(mRecip.getKeyid());
            if (ci != null) {
                try {
                    if (ci.moveToFirst()) {
                        do {
                            MessageRow inRow = new MessageRow(ci, true);
                            if (mRecip != null) {
                                inRow.setPhoto(mRecip.getPhoto());
                            }
                            mMessageList.add(inRow);
                        } while (ci.moveToNext());
                    }
                } finally {
                    ci.close();
                }
            }
            // decrypted msgs and outbox msgs
            Cursor cm = dbMessage.fetchAllMessagesByThread(mRecip.getKeyid());
            if(mDraft != null && mDraft.getKeyId() != mRecip.getKeyid() || mDraft == null)
            {
            	mEditTextMessage.setTextKeepState("");
            	mDraft = null;
            	mDraftSaved = false;
            }
            if (cm != null) {
                try {
                    if (cm.moveToFirst()) {
                        do {
                            MessageRow messageRow = new MessageRow(cm, false);
                            if (!messageRow.isInbox()) {
                                messageRow.setPhoto(myPhoto);
                            } else {
                                if (mRecip != null) {
                                    messageRow.setPhoto(mRecip.getPhoto());
                                }
                            }

                            if ((mDraft == null
                                    && messageRow.getStatus() == MessageDbAdapter.MESSAGE_STATUS_DRAFT
                                    && TextUtils.isEmpty(messageRow.getFileName())
                                    && mRecip.isSendable()) || (mDraft != null && mDraft.getRowId() == messageRow.getRowId() && TextUtils.isEmpty(mEditTextMessage.getText()))) {
                                // if recent draft, remove from list put in edit
                                // box
                                mDraft = messageRow;
                                mEditTextMessage.setTextKeepState(mDraft.getText());
                                mEditTextMessage.forceLayout();
                                mDraftSaved = false;
                            }
                            else if (mDraft != null && mDraft.getRowId() == messageRow.getRowId()) {
                                // draft has already been updated
                            	mDraftSaved = false;
                                continue;
                            }
                            else {
                                // show message normally
                                mMessageList.add(messageRow);
                            }
                        } while (cm.moveToNext());
                    }
                } finally {
                    cm.close();
                }
            }
            Collections.sort(mMessageList, new MessageDateAscendingComparator());
        } else {
            // clear draft in thread view
            showCompose = false;
            doSave(mEditTextMessage.getText().toString(), true);
            mDraft = null;
            mEditTextMessage.setTextKeepState("");
        }

        if (showCompose) {
            mComposeWidget.setVisibility(View.VISIBLE);
        } else {
            mComposeWidget.setVisibility(View.GONE);
        }

        // set position to top when incoming message in foreground...
        if (recentMsg) {
            mListMsgTopOffset = 0;
            mListMsgVisiblePos = mMessageList.size() - 1;
        }

//        unregisterForContextMenu(mListViewMsgs);
//        unregisterForContextMenu(mListViewThreads);

//        mAdapterThread = new ThreadsAdapter(this.getActivity(), mThreadList);
//        mListViewThreads.setAdapter(mAdapterThread);
//        setThreadListClickListener();
//        mListViewThreads.setSelectionFromTop(mListThreadVisiblePos, mListThreadTopOffset);

        mAdapterMsg = new MessagesAdapter(this.getActivity(), mMessageList);
        mListViewMsgs.setAdapter(mAdapterMsg);
        setMessageListClickListener();
        mListViewMsgs.setSelectionFromTop(mListMsgVisiblePos, mListMsgTopOffset);

        registerForContextMenu(mListViewMsgs);
//        registerForContextMenu(mListViewThreads);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
//        if (v.equals(mListViewMsgs)) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.layout.messagecontext, menu);

            final String text = mMessageList.get(info.position).getText();
            final String file = mMessageList.get(info.position).getFileName();
            if (!TextUtils.isEmpty(text)) {
                menu.add(Menu.NONE, R.id.item_message_copytext, Menu.NONE,
                        R.string.menu_messageCopyText);
            }
            if (!TextUtils.isEmpty(text) || !TextUtils.isEmpty(file)) {
                menu.add(Menu.NONE, R.id.item_message_forward, Menu.NONE,
                        R.string.menu_messageForward);
            }
            menu.add(Menu.NONE, R.id.item_message_details, Menu.NONE, R.string.menu_Details);

            if (SafeSlingerConfig.isDebug()) {
                menu.add(Menu.NONE, R.id.item_debug_transcript, Menu.NONE,
                        R.string.menu_debugTranscript);
            }
//        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == R.id.item_delete_message) {
            doDeleteMessage(mMessageList.get(info.position));
            updateMessageList(false);
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
            	Bundle bundle  = new Bundle();
            	bundle.putBoolean("update_threads", true);
            	mListener.onCommunicateData(bundle, Tabs.THREADS.toString());
            }
            return true;
        } else if (item.getItemId() == R.id.item_message_details) {
            String detailStr = BaseActivity.formatMessageDetails(getActivity(),
                    mMessageList.get(info.position));
            showHelp(getString(R.string.title_MessageDetail), detailStr);
            return true;
        } else if (item.getItemId() == R.id.item_message_copytext) {
            SafeSlinger.getApplication().copyPlainTextToClipboard(
                    mMessageList.get(info.position).getText());
            showNote(getString(R.string.state_TextCopiedToClipboard));
            return true;
        } else if (item.getItemId() == R.id.item_message_forward) {
            doForward(mMessageList.get(info.position));
            return true;
        } else if (item.getItemId() == R.id.item_debug_transcript) {
            doExportTranscript(mMessageList);
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    @SuppressWarnings("deprecation")
	private void doExportTranscript(List<MessageRow> msgs) {
        // in debug only, export fields useful for debugging message delivery
        StringBuilder debug = new StringBuilder();
        for (MessageRow m : msgs) {
            debug.append(String.format("%-3s %s %-7s %24s %s %s %s\n", //
                    (m.isInbox() ? "<I " : " O>"), //
                    (m.isRead() ? "R" : "-"), //
                    MessageDbAdapter.getStatusCode(m.getStatus()), //
                    new Date(m.getProbableDate()).toGMTString(), //
                    (TextUtils.isEmpty(m.getText()) ? "---" : "TXT"), //
                    (TextUtils.isEmpty(m.getFileName()) ? "---" : "FIL"), //
                    (TextUtils.isEmpty(m.getMsgHash()) ? m.getMsgHash() : m.getMsgHash().trim()) //
                    ));
        }
        SafeSlinger.getApplication().showDebugEmail(getActivity(), debug.toString());
    }

    private void doDecryptMessage(String pass, MessageRow inRow) {
        try {
            MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity());
            InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(this.getActivity());

            StringBuilder keyidout = new StringBuilder();
            byte[] plain = CryptTools.decryptMessage(inRow.getEncBody(), pass, keyidout);
            MessagePacket push = new MessagePacket(plain);

            // add decrypted
            long rowIdMsg = dbMessage.createMessageDecrypted(inRow, push, keyidout.toString());
            if (rowIdMsg == -1) {
                showNote(getString(R.string.error_UnableToSaveMessageInDB));
            } else {
                // remove encrypted
                if (inRow.isInboxTable()) { // new
                    dbInbox.deleteInbox(inRow.getRowId());
                } else { // old
                    dbMessage.deleteMessage(inRow.getRowId());
                }
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

        if (msg.getFileType().startsWith(SafeSlingerConfig.MIMETYPE_CLASS + "/")) {
            Intent intent = new Intent();
            intent.putExtra(extra.PUSH_MSG_HASH, msg.getMsgHash());
            intent.putExtra(extra.PUSH_FILE_NAME, msg.getFileName());
            intent.putExtra(extra.PUSH_FILE_TYPE, msg.getFileType());
            intent.putExtra(extra.PUSH_FILE_SIZE, msg.getFileSize());
            intent.putExtra(extra.MESSAGE_ROW_ID, msg.getRowId());
            sendResultToHost(RESULT_PROCESS_SSMIME, intent.getExtras());
        } else if (SSUtil.isExternalStorageReadable()) {
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
        InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(this.getActivity());
        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity());

        // make sure we know which table row id we need to use
        if (msg.isInboxTable()) {
            if (dbInbox.deleteInbox(msg.getRowId())) {
                showNote(String.format(getString(R.string.state_MessagesDeleted), 1));
            }
        } else {
            if (dbMessage.deleteMessage(msg.getRowId())) {
                showNote(String.format(getString(R.string.state_MessagesDeleted), 1));
            }
        }

        // if this was the last message, reset the recipient
        int msgsInView = 0;
        Cursor ci = dbInbox.fetchAllInboxByThread(mRecip.getKeyid());
        if (ci != null) {
            try {
                msgsInView += ci.getCount();
            } finally {
                ci.close();
            }
        }
        Cursor cm = dbMessage.fetchAllMessagesByThread(mRecip.getKeyid());
        if (cm != null) {
            try {
                msgsInView += cm.getCount();
            } finally {
                cm.close();
            }
        }
        if (msgsInView == 0) {
            mRecip = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // if soft input open, close it...
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        View focus = getActivity().getCurrentFocus();
        if (focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        // save draft when view is lost
//        doSave(mEditTextMessage.getText().toString(), mRecip != null);
        initiateSaveDraft();
    }

    public void initiateSaveDraft()
    {
    	doSave(mEditTextMessage.getText().toString(), mRecip != null);
    	
    }
    
    @Override
    public void onResume() {
        super.onResume();
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
            mListener = (FragmentCommunicationInterface) activity;
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
            int readDuration = msg.length() * SafeSlingerConfig.MS_READ_PER_CHAR;
            if (readDuration <= SafeSlingerConfig.SHORT_DELAY) {
                Toast toast = Toast.makeText(this.getActivity(), msg.trim(), Toast.LENGTH_SHORT);
                toast.show();
            } else if (readDuration <= SafeSlingerConfig.LONG_DELAY) {
                Toast toast = Toast.makeText(this.getActivity(), msg.trim(), Toast.LENGTH_LONG);
                toast.show();
            } else {
                showHelp(getString(R.string.app_name), msg.trim());
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
                default:
                    break;
            }
            return super.onCreateDialog(savedInstanceState);
        }
    }

    public void postProgressMsgList(boolean isInboxTable, long msgRowId, String msg) {
       {
            for (int i = 0; i < mMessageList.size(); i++) {
                if (mMessageList.get(i).isInboxTable() == isInboxTable
                        && mMessageList.get(i).getRowId() == msgRowId) {
                    MessageRow mr = mMessageList.get(i);
                    mr.setProgress(msg);
                    mMessageList.set(i, mr);

                    // ensure last item remains fully in view
                    if (msgRowId == mMessageList.get(mMessageList.size() - 1).getRowId()) {
                        mListMsgTopOffset = 0;
                        mListMsgVisiblePos = mMessageList.size() - 1;
                    }

                    mAdapterMsg = new MessagesAdapter(this.getActivity(), mMessageList);
                    mListViewMsgs.setAdapter(mAdapterMsg);
                    mListViewMsgs.setSelectionFromTop(mListMsgVisiblePos, mListMsgTopOffset);
                    break;
                }
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

    public void updateKeypad() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (mRecip == null) {
            // if soft input open, close it...
            View focus = getActivity().getCurrentFocus();
            if (focus != null) {
                imm.hideSoftInputFromWindow(focus.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } else {
            // if soft input open, gain the focus...
            if (imm.isActive() && mEditTextMessage != null) {
                mEditTextMessage.requestFocus();
            }
        }
    }

    private boolean isSendableText() {
        return TextUtils.getTrimmedLength(mEditTextMessage.getText()) != 0;
    }
}
