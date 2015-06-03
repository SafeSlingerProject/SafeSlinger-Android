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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
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
import edu.cmu.cylab.starslinger.model.DraftData;
import edu.cmu.cylab.starslinger.model.InboxDbAdapter;
import edu.cmu.cylab.starslinger.model.MessageData;
import edu.cmu.cylab.starslinger.model.MessageDateAscendingComparator;
import edu.cmu.cylab.starslinger.model.MessageDbAdapter;
import edu.cmu.cylab.starslinger.model.MessagePacket;
import edu.cmu.cylab.starslinger.model.MessageRow;
import edu.cmu.cylab.starslinger.model.MessageRow.MsgAction;
import edu.cmu.cylab.starslinger.model.RecipientData;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientRow;
import edu.cmu.cylab.starslinger.model.ThreadData;
import edu.cmu.cylab.starslinger.model.ThreadDateDecendingComparator;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class MessagesFragment extends Fragment {
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    public static final int RESULT_GETFILE = 7124;
    public static final int RESULT_GETMESSAGE = 7125;
    public static final int RESULT_EDITMESSAGE = 7126;
    public static final int RESULT_FWDMESSAGE = 7127;
    public static final int RESULT_SEND = 7128;
    public static final int RESULT_SAVE = 7129;
    public static final int RESULT_PROCESS_SSMIME = 7130;
    public static final int RESULT_FILESEL = 7131;
    public static final int RESULT_FILEREMOVE = 7132;

    private List<ThreadData> mThreadList = new ArrayList<ThreadData>();
    private List<MessageRow> mMessageList = new ArrayList<MessageRow>();
    private TextView mTvInstruct;
    private LinearLayout mLayoutLoadProgress;
    private ListView mListViewMsgs;
    private ListView mListViewThreads;
    private MessagesAdapter mAdapterMsg;
    private ThreadsAdapter mAdapterThread;
    private NotificationManager mNm;
    private static OnMessagesResultListener mResult;
    private static DraftData d = DraftData.INSTANCE;
    private static int mListMsgVisiblePos;
    private static int mListMsgTopOffset;
    private static int mListThreadVisiblePos;
    private static int mListThreadTopOffset;

    private TextView mTextViewFile;
    private TextView mTextViewFileSize;
    private ImageView mImageViewFile;
    private Button mButtonFile;
    private RelativeLayout mrlFile;

    private static EditText mEditTextMessage;
    private ImageButton mButtonSend;
    private LinearLayout mComposeWidget;

    private static MessageData mDraft;
    private Handler mMsgFragmentHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String ns = Context.NOTIFICATION_SERVICE;
        mNm = (NotificationManager) this.getActivity().getSystemService(ns);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        doSave(d.existsRecip());
    }

    public void updateValues(Bundle extras) {
        long msgRowId = -1L;
        long recipRowId = -1L;
        if (extras != null) {
            msgRowId = extras.getLong(extra.MESSAGE_ROW_ID, -1L);
            recipRowId = extras.getLong(extra.RECIPIENT_ROW_ID, -1L);

            // set position to top when incoming message in
            // background...
            InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(this.getActivity());
            MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity());
            int inCount = dbInbox.getUnseenInboxCount();
            int msgCount = dbMessage.getUnseenMessageCount();
            int allCount = inCount + msgCount;
            if (allCount > 0) {
                mListThreadTopOffset = 0;
                mListThreadVisiblePos = 0;
            }

            RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this.getActivity());
            if (recipRowId != -1) {
                Cursor c = dbRecipient.fetchRecipient(recipRowId);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            d.setRecip(new RecipientRow(c));
                        }
                    } finally {
                        c.close();
                    }
                }
            } else if (msgRowId == -1) {
                d.clearRecip();
            }
        }
        updateList(msgRowId != -1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vFrag = inflater.inflate(R.layout.messagelist, container, false);

        mTvInstruct = (TextView) vFrag.findViewById(R.id.tvInstruct);

        mLayoutLoadProgress = (LinearLayout) vFrag.findViewById(R.id.layoutLoadProgress);

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

        mTextViewFile = (TextView) vFrag.findViewById(R.id.SendTextViewFile);
        mTextViewFileSize = (TextView) vFrag.findViewById(R.id.SendTextViewFileSize);
        mImageViewFile = (ImageView) vFrag.findViewById(R.id.SendImageViewFile);
        mButtonFile = (Button) vFrag.findViewById(R.id.SendButtonFile);
        mrlFile = (RelativeLayout) vFrag.findViewById(R.id.SendFrameButtonFile);

        mEditTextMessage = (EditText) vFrag.findViewById(R.id.SendEditTextMessage);
        mButtonSend = (ImageButton) vFrag.findViewById(R.id.SendButtonSend);

        mButtonFile.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mDraft == null || TextUtils.isEmpty(mDraft.getFileName())) {
                    doFileSelect();
                } else {
                    showChangeFileOptions();
                }
            }
        });

        mButtonSend.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // requested send from send button...
                doSend(d.existsRecip());
            }
        });

        mEditTextMessage.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    // requested send from keyboard...
                    doSend(d.existsRecip());
                    return true;
                }
                return false;
            }
        });

        // updateList(false);
        return vFrag;
    }

    private static void doSave(boolean save) {
        if (save) {
            String text = mEditTextMessage.getText().toString();
            Intent intent = new Intent();
            intent.putExtra(extra.TEXT_MESSAGE, text.trim());
            if (mDraft != null) {
                intent.putExtra(extra.FPATH, mDraft.getFileDir());
                intent.putExtra(extra.MESSAGE_ROW_ID, mDraft.getRowId());
            }
            if (d.existsRecip()) {
                RecipientData recip = d.getRecip();
                intent.putExtra(extra.RECIPIENT_ROW_ID, recip.getRowId());
            }
            // always keep local version, unless we need to delete
            if (mDraft != null && !isSendableMessage()) {
                mDraft = null;
                mEditTextMessage.setTextKeepState("");
            }
            sendResultToHost(RESULT_SAVE, intent.getExtras());
        }
    }

    private void doSend(boolean send) {
        if (send && isSendableMessage()) {
            String text = mEditTextMessage.getText().toString();
            Intent intent = new Intent();

            // recipient required to send anything
            if (mDraft != null) {
                intent.putExtra(extra.FPATH, mDraft.getFileDir());
                intent.putExtra(extra.MESSAGE_ROW_ID, mDraft.getRowId());
            }
            intent.putExtra(extra.TEXT_MESSAGE, text.trim());
            if (d.existsRecip()) {
                RecipientData recip = d.getRecip();
                intent.putExtra(extra.RECIPIENT_ROW_ID, recip.getRowId());
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
        if (d.existsRecip()) {
            RecipientData recip = d.getRecip();
            intent.putExtra(extra.RECIPIENT_ROW_ID, recip.getRowId());
        }
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

    private static void doFileRemove() {
        Intent intent = new Intent();
        if (mDraft != null) {
            intent.putExtra(extra.FPATH, mDraft.getFileDir());
            intent.putExtra(extra.MESSAGE_ROW_ID, mDraft.getRowId());
        }
        if (d.existsRecip()) {
            RecipientData recip = d.getRecip();
            intent.putExtra(extra.RECIPIENT_ROW_ID, recip.getRowId());
        }
        // always keep local version, unless we need to delete
        mDraft.removeFile();
        if (mDraft != null && !isSendableMessage()) {
            mDraft = null;
            mEditTextMessage.setTextKeepState("");
        }
        sendResultToHost(RESULT_FILEREMOVE, intent.getExtras());
    }

    private static void doFileSelect() {
        Intent intent = new Intent();
        if (mDraft != null) {
            intent.putExtra(extra.MESSAGE_ROW_ID, mDraft.getRowId());
        }
        if (d.existsRecip()) {
            RecipientData recip = d.getRecip();
            intent.putExtra(extra.RECIPIENT_ROW_ID, recip.getRowId());
        }
        sendResultToHost(RESULT_FILESEL, intent.getExtras());
    }

    private void setThreadListClickListener() {
        mListViewThreads.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {

                ThreadData t = mThreadList.get(pos);
                if (!d.existsRecip()) {
                    // requested messages list
                    // assign recipient
                    if (TextUtils.isEmpty(t.getMsgRow().getKeyId())) {
                        // able to view null key id messages
                        d.setRecip(RecipientRow.createEmptyRecipient());
                    } else {
                        RecipientDbAdapter dbRecipient = RecipientDbAdapter
                                .openInstance(getActivity().getApplicationContext());
                        Cursor c = dbRecipient.fetchRecipientByKeyId(t.getMsgRow().getKeyId());
                        if (c != null) {
                            try {
                                if (c.moveToFirst()) {
                                    // messages with matching key ids in
                                    // database
                                    d.setRecip(new RecipientRow(c));
                                } else {
                                    // messages without matching key ids
                                    d.setRecip(RecipientRow.createKeyIdOnlyRecipient(t.getMsgRow()
                                            .getKeyId()));
                                }
                            } finally {
                                c.close();
                            }
                        }
                    }
                } else {
                    // requested threads list
                    // remove recipient
                    removeRecip();
                }

                updateList(true);
            }
        });
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

                updateList(false);
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setThreadListClickListener();
        setMessageListClickListener();
    }

    private void setUpListAdapters() {

        if ((getActivity()) == null) {
            return;
        }

        synchronized (mThreadList) {
            unregisterForContextMenu(mListViewThreads);
            mAdapterThread = new ThreadsAdapter(getActivity(), mThreadList);
            mListViewThreads.setAdapter(mAdapterThread);
            if (mListThreadVisiblePos < mThreadList.size()) {
                mListViewThreads.setSelectionFromTop(mListThreadVisiblePos, mListThreadTopOffset);
            }
            registerForContextMenu(mListViewThreads);
        }
        synchronized (mMessageList) {
            unregisterForContextMenu(mListViewMsgs);
            mAdapterMsg = new MessagesAdapter(getActivity(), mMessageList);
            mListViewMsgs.setAdapter(mAdapterMsg);
            if (mListMsgVisiblePos < mMessageList.size()) {
                mListViewMsgs.setSelectionFromTop(mListMsgVisiblePos, mListMsgTopOffset);
            }
            registerForContextMenu(mListViewMsgs);
        }
    }

    private void updateList(final boolean recentMsg) {

        mMsgFragmentHandler.removeCallbacks(null);
        mMsgFragmentHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if ((getActivity()) == null) {
                    return;
                }

                boolean showCompose = false;
                ThreadData t = null;

                getActivity().supportInvalidateOptionsMenu();
                // update action bar options

                MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(getActivity());
                InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(getActivity());

                // make sure view is already inflated...
                if (mListViewMsgs == null) {
                    return;
                }

                String contactLookupKey = SafeSlingerPrefs.getContactLookupKey();
                byte[] myPhoto = ((BaseActivity) getActivity()).getContactPhoto(contactLookupKey);

                RecipientData recip = d.getRecip();
                if (isResumed() && d.existsRecip()) {
                    // when shown, current thread all are now seen
                    dbMessage.updateAllMessagesAsSeenByThread(recip.getKeyid());
                    dbInbox.updateAllInboxAsSeenByThread(recip.getKeyid());

                    // remove notify when every unseen thread has been seen
                    int inCount = dbInbox.getUnseenInboxCount();
                    int msgCount = dbMessage.getUnseenMessageCount();
                    int allCount = inCount + msgCount;
                    updateNotification(allCount);
                }
                manageInstructVisibility(View.GONE);

                synchronized (mThreadList) {
                    // draw threads list/title bar
                    mThreadList.clear();
                    int totalThreads = 0;
                    // inbox threads
                    Cursor cmi = null;
                    if (!d.existsRecip()) {
                        cmi = dbInbox.fetchInboxRecentByUniqueKeyIds();
                    } else {
                        cmi = dbInbox.fetchInboxRecent(recip.getKeyid());
                    }
                    if (cmi != null) {
                        try {
                            totalThreads += cmi.getCount();
                            if (cmi.moveToFirst()) {
                                do {
                                    MessageRow inboxRow = new MessageRow(cmi, true);
                                    t = addInboxThread(inboxRow);
                                    mergeInThreads(t);
                                } while (cmi.moveToNext());
                            }
                        } finally {
                            cmi.close();
                        }
                    }
                    // message threads
                    Cursor cmt = null;
                    if (!d.existsRecip()) {
                        cmt = dbMessage.fetchMessagesRecentByUniqueKeyIds();
                    } else {
                        cmt = dbMessage.fetchMessageRecent(recip.getKeyid());
                    }
                    if (cmt != null) {
                        try {
                            totalThreads += cmt.getCount();
                            if (cmt.moveToFirst()) {
                                do {
                                    MessageRow messageRow = new MessageRow(cmt, false);
                                    t = addMessageThread(messageRow);
                                    mergeInThreads(t);
                                } while (cmt.moveToNext());
                            }
                        } finally {
                            cmt.close();
                        }
                    }
                    // add thread for draft thread state
                    if (d.existsRecip() && t == null) {
                        totalThreads += 1;
                        t = new ThreadData(new MessageRow(null, false), 0, 0, false, recip
                                .getName(), d.existsRecip(), recip);
                        mergeInThreads(t);
                    }

                    if (totalThreads <= 0) {
                        manageInstructVisibility(View.VISIBLE);
                    }

                    Collections.sort(mThreadList, new ThreadDateDecendingComparator());
                }

                synchronized (mMessageList) {
                    mMessageList.clear();
                    if (d.existsRecip()) {
                        // recipient data
                        if (recip.isSendable() && t != null) {
                            showCompose = true;
                        }
                        // encrypted msgs
                        Cursor ci = dbInbox.fetchAllInboxByThread(recip.getKeyid());
                        if (ci != null) {
                            try {
                                if (ci.moveToFirst()) {
                                    do {
                                        MessageRow inRow = new MessageRow(ci, true);
                                        if (d.existsRecip()) {
                                            inRow.setPhoto(recip.getPhoto());
                                        }
                                        mMessageList.add(inRow);
                                    } while (ci.moveToNext());
                                }
                            } finally {
                                ci.close();
                            }
                        }
                        // decrypted msgs and outbox msgs
                        Cursor cm = dbMessage.fetchAllMessagesByThread(recip.getKeyid());
                        if (cm != null) {
                            try {
                                if (cm.moveToFirst()) {
                                    do {
                                        MessageRow messageRow = new MessageRow(cm, false);
                                        if (!messageRow.isInbox()) {
                                            messageRow.setPhoto(myPhoto);
                                        } else {
                                            if (d.existsRecip()) {
                                                messageRow.setPhoto(recip.getPhoto());
                                            }
                                        }

                                        if (mDraft == null
                                                && messageRow.getStatus() == MessageDbAdapter.MESSAGE_STATUS_DRAFT
                                                && recip.isSendable()) {
                                            // if recent draft, remove from list
                                            mDraft = messageRow;

                                        } else if (mDraft != null
                                                && mDraft.getRowId() == messageRow.getRowId()) {
                                            // file data must reloaded from db,
                                            // text message is edited on this ui
                                            mDraft.setFileData(messageRow.getFileData());
                                            mDraft.setFileDir(messageRow.getFileDir());
                                            mDraft.setFileName(messageRow.getFileName());
                                            mDraft.setFileSize(messageRow.getFileSize());
                                            mDraft.setFileType(messageRow.getFileType());
                                            // draft has already been updated
                                            continue;

                                        } else {
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
                        mDraft = null;
                        mEditTextMessage.setTextKeepState("");
                        doSave(true);
                    }

                    if (showCompose) {
                        manageComposeVisibility(View.VISIBLE);
                        mrlFile.setVisibility(View.GONE);
                        if (mDraft != null) {
                            // put in edit box
                            String text = mDraft.getText();
                            if (text != null) {
                                mEditTextMessage.setTextKeepState(text);
                                mEditTextMessage.forceLayout();
                            }
                            byte[] thumb = null;
                            // load file data if exists
                            if (!TextUtils.isEmpty(mDraft.getFileType())
                                    && mDraft.getFileType().contains("image")) {
                                try {
                                    mDraft = SSUtil.addAttachmentFromPath(mDraft,
                                            mDraft.getFileDir());
                                } catch (FileNotFoundException e) {
                                    showNote(e);
                                }
                                int dimension = (int) SafeSlinger.getApplication().getResources()
                                        .getDimension(R.dimen.avatar_size_list);
                                thumb = SSUtil.makeThumbnail(mDraft.getFileData(), dimension);
                            }
                            if (!(TextUtils.isEmpty(mDraft.getFileName()))) {
                                drawFileImage(getActivity(), mDraft, thumb, mImageViewFile);
                                drawFileData(mDraft);
                                mTextViewFile.setTextColor(Color.BLACK);
                                mrlFile.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
                        manageComposeVisibility(View.GONE);
                    }
                }

                // set position to top when incoming message in
                // foreground...
                if (recentMsg) {
                    mListMsgTopOffset = 0;
                    mListMsgVisiblePos = mMessageList.size() - 1;
                }

                setUpListAdapters();

                // remove progress, UI update complete
                if (mLayoutLoadProgress != null) {
                    mLayoutLoadProgress.setVisibility(View.GONE);
                }

            }
        }, 200);
    }

    private void updateNotification(int allCount) {
        if (allCount == 0) {
            mNm.cancel(HomeActivity.NOTIFY_NEW_MSG_ID);
        }
    }

    private void manageInstructVisibility(final int visibility) {
        mTvInstruct.setVisibility(visibility);
    }

    private void manageComposeVisibility(final int visibility) {
        mComposeWidget.setVisibility(visibility);
    }

    private void mergeInThreads(ThreadData t1) {
        boolean exists = false;
        for (int i = 0; i < mThreadList.size(); i++) {
            ThreadData t2 = mThreadList.get(i);

            // if matching key is more recent use it
            String k1 = "" + t2.getMsgRow().getKeyId();
            String k2 = "" + t1.getMsgRow().getKeyId();
            if (k1.equals(k2)) {
                exists = true;
                t2 = new ThreadData(t2, t1);
                mThreadList.set(i, t2);
            }
        }
        if (!exists) {
            mThreadList.add(t1);
        }
    }

    private ThreadData addInboxThread(MessageRow inboxRow) throws SQLException {
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this.getActivity());
        InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(this.getActivity());
        ThreadData t;
        String person = null;
        boolean newerExists = false;
        RecipientRow recipientRow = null;

        Cursor cr = dbRecipient.fetchRecipientByKeyId(inboxRow.getKeyId());
        if (cr != null) {
            try {
                if (cr.moveToFirst()) {
                    recipientRow = new RecipientRow(cr);
                    person = recipientRow.getName();
                }
            } finally {
                cr.close();
            }
        }

        int msgs = dbInbox.getAllInboxCountByThread(inboxRow.getKeyId());

        if (TextUtils.isEmpty(person)) {
            person = findMissingPersonName(inboxRow.getKeyId());
        }

        int newMsgs = dbInbox.getActionRequiredInboxCountByThread(inboxRow.getKeyId());
        int draftMsgs = 0; // inbox does not store drafts
        t = new ThreadData(inboxRow, msgs, newMsgs, draftMsgs > 0, person, d.existsRecip(),
                recipientRow);
        return t;
    }

    private ThreadData addMessageThread(MessageRow messageRow) throws SQLException {
        RecipientDbAdapter dbRecipient = RecipientDbAdapter.openInstance(this.getActivity());
        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity());
        ThreadData t;
        String person = null;
        boolean newerExists = false;
        RecipientRow recipientRow = null;

        Cursor cr = dbRecipient.fetchRecipientByKeyId(messageRow.getKeyId());
        if (cr != null) {
            try {
                if (cr.moveToFirst()) {
                    recipientRow = new RecipientRow(cr);
                    person = recipientRow.getName();
                }
            } finally {
                cr.close();
            }
        }

        int msgs = dbMessage.getAllMessageCountByThread(messageRow.getKeyId());

        if (TextUtils.isEmpty(person)) {
            person = findMissingPersonName(messageRow.getKeyId());
        }

        int newMsgs = dbMessage.getActionRequiredMessageCountByThread(messageRow.getKeyId());
        int draftMsgs = dbMessage.getDraftMessageCountByThread(messageRow.getKeyId());
        t = new ThreadData(messageRow, msgs, newMsgs, draftMsgs > 0, person, d.existsRecip(),
                recipientRow);
        return t;
    }

    private String findMissingPersonName(String keyId) {
        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity());
        String person = null;
        Cursor cmt = dbMessage.fetchAllMessagesByThread(keyId);
        if (cmt != null) {
            try {
                if (cmt.moveToFirst()) {
                    do {
                        if (TextUtils.isEmpty(person)) {
                            MessageRow mr = new MessageRow(cmt, false);
                            person = mr.getPerson();
                        } else {
                            break;
                        }
                    } while (cmt.moveToNext());
                }
            } finally {
                cmt.close();
            }
        }
        return person;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        if (v.equals(mListViewThreads)) {
            RecipientData recip = mThreadList.get(info.position).getRecipient();
            String delThreadStr = String.format(
                    getString(R.string.menu_deleteThread),
                    ThreadsAdapter.getBestIdentityName(getActivity(),
                            mThreadList.get(info.position), recip));
            if (mThreadList.get(info.position).getMsgCount() > 0) {
                menu.add(Menu.NONE, R.id.item_delete_thread, Menu.NONE, delThreadStr);
            }
            if (recip != null && recip.getRowId() > -1
                    && recip.getSource() != RecipientDbAdapter.RECIP_SOURCE_INVITED) {
                if (!recip.isValidContactLink()) {
                    menu.add(Menu.NONE, R.id.item_link_contact_add, Menu.NONE,
                            R.string.menu_link_contact_add);
                } else {
                    menu.add(Menu.NONE, R.id.item_link_contact_change, Menu.NONE,
                            R.string.menu_link_contact_change);
                }

                if (recip.isValidContactLink()) {
                    menu.add(Menu.NONE, R.id.item_edit_contact, Menu.NONE,
                            R.string.menu_EditContact);
                }
            }
            menu.add(Menu.NONE, R.id.item_thread_details, Menu.NONE, R.string.menu_Details);
        } else if (v.equals(mListViewMsgs)) {
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
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == R.id.item_delete_message) {
            doDeleteMessage(mMessageList.get(info.position));
            updateList(false);
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
        } else if (item.getItemId() == R.id.item_delete_thread) {
            doDeleteThread(mThreadList.get(info.position).getMsgRow().getKeyId());
            updateList(false);
            return true;
        } else if (item.getItemId() == R.id.item_thread_details) {
            showHelp(getString(R.string.title_RecipientDetail),
                    BaseActivity.formatThreadDetails(getActivity(), mThreadList.get(info.position)));
            return true;
        } else if (item.getItemId() == R.id.item_link_contact_add
                || item.getItemId() == R.id.item_link_contact_change) {
            ((BaseActivity) getActivity()).showUpdateContactLink(mThreadList.get(info.position)
                    .getRecipient().getRowId());
            return true;
        } else if (item.getItemId() == R.id.item_edit_contact) {
            ((BaseActivity) getActivity()).showEditContact(mThreadList.get(info.position)
                    .getRecipient().getContactlu());
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

        // if this was the last message...
        int msgsInView = 0;
        RecipientData recip = d.getRecip();
        Cursor ci = dbInbox.fetchAllInboxByThread(recip.getKeyid());
        if (ci != null) {
            try {
                msgsInView += ci.getCount();
            } finally {
                ci.close();
            }
        }
        Cursor cm = dbMessage.fetchAllMessagesByThread(recip.getKeyid());
        if (cm != null) {
            try {
                msgsInView += cm.getCount();
            } finally {
                cm.close();
            }
        }
        // remove recip when 0 msgs and not sendable
        if (msgsInView == 0) {
            if (d.existsRecip()) {
                RecipientDbAdapter dbRecipient = RecipientDbAdapter
                        .openInstance(this.getActivity());
                int newerRecips = dbRecipient.getAllNewerRecipients(d.getRecip(), true);
                if (!d.getRecip().isSendable() || newerRecips > 0) {
                    d.clearRecip();
                }
            }
        }
    }

    public void doDeleteThread(String keyId) {
        MessageDbAdapter dbMessage = MessageDbAdapter.openInstance(this.getActivity());
        InboxDbAdapter dbInbox = InboxDbAdapter.openInstance(this.getActivity());
        int deletedMsg = dbMessage.deleteThread(keyId);
        int deletedIn = dbInbox.deleteThread(keyId);
        int deleted = deletedMsg + deletedIn;
        if (deleted > 0) {
            showNote(String.format(getString(R.string.state_MessagesDeleted), deleted));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // // if soft input open, close it...
        // InputMethodManager imm = (InputMethodManager)
        // getActivity().getSystemService(
        // Context.INPUT_METHOD_SERVICE);
        // View focus = getActivity().getCurrentFocus();
        // if (focus != null) {
        // imm.hideSoftInputFromWindow(focus.getWindowToken(),
        // InputMethodManager.HIDE_NOT_ALWAYS);
        // }

        // save draft when view is lost
        doSave(d.existsRecip());
    }

    @Override
    public void onResume() {
        super.onResume();

        updateList(false);
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

    private static void sendResultToHost(int resultCode, Bundle args) {
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
        DialogFragment newFragment = new MessagesAlertDialogFragment().newInstance(
                BaseActivity.DIALOG_HELP, args);
        newFragment.show(getFragmentManager(), "dialog");
    }

    public static class MessagesAlertDialogFragment extends DialogFragment {

        public MessagesAlertDialogFragment newInstance(int id) {
            return newInstance(id, new Bundle());
        }

        public MessagesAlertDialogFragment newInstance(int id, Bundle args) {
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
                case BaseActivity.DIALOG_FILEOPTIONS:
                    return xshowChangeFileOptions(getActivity()).create();
                default:
                    break;
            }
            return super.onCreateDialog(savedInstanceState);
        }
    }

    public void postProgressMsgList(boolean isInboxTable, long msgRowId, String msg) {
        if (!d.existsRecip()) {
            synchronized (mThreadList) {
                for (int i = 0; i < mThreadList.size(); i++) {
                    if (mThreadList.get(i).getMsgRow().getRowId() == msgRowId) {
                        ThreadData t = mThreadList.get(i);
                        t.setProgress(msg);
                        mThreadList.set(i, t);

                        // set top items as most recent
                        mListThreadVisiblePos = 0;

                        mAdapterThread = new ThreadsAdapter(this.getActivity(), mThreadList);
                        mListViewThreads.setAdapter(mAdapterThread);
                        if (mListThreadVisiblePos < mThreadList.size()) {
                            mListThreadTopOffset = 0;
                            mListViewThreads.setSelectionFromTop(mListThreadVisiblePos,
                                    mListThreadTopOffset);
                        }
                        break;
                    }
                }
            }
        } else {
            synchronized (mMessageList) {
                for (int i = 0; i < mMessageList.size(); i++) {
                    if (mMessageList.get(i).isInboxTable() == isInboxTable
                            && mMessageList.get(i).getRowId() == msgRowId) {
                        MessageRow mr = mMessageList.get(i);
                        mr.setProgress(msg);
                        mMessageList.set(i, mr);

                        mAdapterMsg = new MessagesAdapter(this.getActivity(), mMessageList);
                        mListViewMsgs.setAdapter(mAdapterMsg);
                        break;
                    }
                }
            }
        }
        if (msg == null) {
            updateList(false);
        }
    }

    public static void removeRecip() {
        doSave(d.existsRecip());
        d.clearRecip();
    }

    public void updateKeypad() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (!d.existsRecip()) {
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

    private static boolean isSendableMessage() {
        return TextUtils.getTrimmedLength(mEditTextMessage.getText()) != 0
                || (mDraft != null && !TextUtils.isEmpty(mDraft.getFileName()));
    }

    @SuppressWarnings("deprecation")
    public static void drawFileImage(Context ctx, MessageData draft, byte[] thumb, ImageView iv) {
        String filenameArray[] = draft.getFileName().split("\\.");
        String extension = filenameArray[filenameArray.length - 1];
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        if (thumb != null && thumb.length > 0) {
            ByteArrayInputStream in = new ByteArrayInputStream(thumb);
            BitmapDrawable tn = new BitmapDrawable(in);
            iv.setImageDrawable(tn);
        } else {
            // default, there should always be some image
            iv.setImageDrawable(ctx.getResources().getDrawable(R.drawable.ic_menu_file));

            // is there a more specific file type available?
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setType(mime);
            PackageManager pm = ctx.getPackageManager();
            List<ResolveInfo> lract = pm.queryIntentActivities(viewIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);

            boolean resolved = false;

            for (ResolveInfo ri : lract) {
                if (!resolved) {
                    try {
                        Drawable icon = pm.getApplicationIcon(ri.activityInfo.packageName);
                        iv.setImageDrawable(icon);
                        resolved = true;
                    } catch (NameNotFoundException e) {
                        iv.setImageDrawable(ctx.getResources().getDrawable(R.drawable.ic_menu_file));
                    }
                }
            }
        }
    }

    private void drawFileData(MessageData draft) {
        mTextViewFile.setText(draft.getFileName());
        mTextViewFileSize.setText(" (" + SSUtil.getSizeString(getActivity(), draft.getFileSize())
                + ")");
    }

    private void showChangeFileOptions() {
        DialogFragment newFragment = new MessagesAlertDialogFragment()
                .newInstance(BaseActivity.DIALOG_FILEOPTIONS);
        newFragment.show(getFragmentManager(), "dialog");
    }

    public static AlertDialog.Builder xshowChangeFileOptions(Activity act) {
        final CharSequence[] items = new CharSequence[] {
                act.getText(R.string.menu_Remove), act.getText(R.string.menu_Change)
        };
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.title_FileOptions);
        ad.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
                switch (item) {
                    case 0: // remove
                        doFileRemove();
                        break;
                    case 1: // change
                        doFileSelect();
                        break;
                    default:
                        break;
                }
            }

        });
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        return ad;
    }
}
