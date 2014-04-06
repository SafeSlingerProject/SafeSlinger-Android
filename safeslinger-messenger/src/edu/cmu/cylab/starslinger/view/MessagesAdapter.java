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

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.model.MessageRow;

public class MessagesAdapter extends BaseAdapter {
    private Context mCtx;
    private List<MessageRow> mListMessages;

    public MessagesAdapter(Context context, List<MessageRow> list) {
        mCtx = context;
        mListMessages = list;
    }

    @Override
    public int getCount() {
        return mListMessages.size();
    }

    @Override
    public Object getItem(int pos) {
        return mListMessages.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        // get selected entry
        MessageRow msg = mListMessages.get(pos);

        // always inflate the view, otherwise check box states will get recycled
        // on scrolling...
        LayoutInflater inflater = LayoutInflater.from(mCtx);
        if (msg.isInbox()) {
            convertView = inflater.inflate(R.layout.messageitem_recv, null);
        } else {
            convertView = inflater.inflate(R.layout.messageitem_send, null);
        }

        convertView.setTag(Long.valueOf(msg.getRowId()));
        drawMessageItem(convertView, msg);

        // Return the view for rendering
        return convertView;
    }

    private void drawMessageItem(View convertView, MessageRow msg) {
        ImageView ivAvatar = (ImageView) convertView.findViewById(R.id.imgAvatar);
        TextView tvMessage = (TextView) convertView.findViewById(R.id.tvMessage); // med
        TextView tvDirection = (TextView) convertView.findViewById(R.id.tvDirection); // sma
        TextView tvFile = (TextView) convertView.findViewById(R.id.tvFileInfo); // mic
        TextView tvDate = (TextView) convertView.findViewById(R.id.tvTime1); // sma

        tvMessage.setVisibility(View.GONE);
        tvDirection.setVisibility(View.GONE);
        tvFile.setVisibility(View.GONE);

        float dimension;
        switch (SafeSlingerPrefs.getFontSize()) {
            case 12:
                dimension = mCtx.getResources().getDimension(R.dimen.text_size_micro);
                tvDirection.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimension);
                tvDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimension);
                break;
            case 14:
                dimension = mCtx.getResources().getDimension(R.dimen.text_size_small);
                break;
            default:
            case 18:
                dimension = mCtx.getResources().getDimension(R.dimen.text_size_medium);
                break;
            case 22:
                dimension = mCtx.getResources().getDimension(R.dimen.text_size_large);
                break;
        }
        tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimension);

        // set avatar
        try {
            if (!msg.isRead()) {
                ivAvatar.setImageBitmap(null);
            } else if (msg.getPhoto() != null) {
                Bitmap photo = BitmapFactory.decodeByteArray(msg.getPhoto(), 0,
                        msg.getPhoto().length, null);
                ivAvatar.setImageBitmap(photo);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_silhouette);
            }
        } catch (OutOfMemoryError e) {
            ivAvatar.setImageBitmap(null);
        }

        // set time
        long date = msg.getProbableDate();
        String dateTime = DateUtils.getRelativeTimeSpanString(mCtx, date).toString();
        long diff = System.currentTimeMillis() - Long.valueOf(date);
        if (!TextUtils.isEmpty(msg.getProgress())) {
            tvDate.setText(msg.getProgress());
        } else {
            tvDate.setText(dateTime);
        }

        // set text
        if (!TextUtils.isEmpty(msg.getText())) {
            tvMessage.setText(msg.getText());
            tvMessage.setVisibility(View.VISIBLE);
        }

        // set file/instruct
        StringBuilder timeLeft = new StringBuilder();
        if (diff > SafeSlingerConfig.MESSAGE_EXPIRATION_MS) {
            timeLeft.append(" (");
            timeLeft.append(mCtx.getText(R.string.label_expired));
            timeLeft.append(")");
        } else {
            long endTime = Long.valueOf(date) + SafeSlingerConfig.MESSAGE_EXPIRATION_MS;
            String remainTime = DateUtils.getRelativeTimeSpanString(endTime).toString();
            timeLeft.append(" (");
            timeLeft.append(String.format(mCtx.getString(R.string.label_expires), remainTime));
            timeLeft.append(")");
        }
        StringBuilder fileInfo = new StringBuilder();
        if (!TextUtils.isEmpty(msg.getFileName()) || msg.getFileSize() > 0) {
            fileInfo.append(msg.getFileName());
            if (msg.isInbox() && TextUtils.isEmpty(msg.getFileDir())) {
                fileInfo.append(timeLeft);
            }
        }

        switch (msg.getMessageAction()) {
            case DISPLAY_ONLY:
                if (!TextUtils.isEmpty(fileInfo)) {
                    tvFile.setText(fileInfo);
                    // disable when cannot download
                    if (msg.isInbox() && TextUtils.isEmpty(msg.getFileDir())) {
                        if (diff > SafeSlingerConfig.MESSAGE_EXPIRATION_MS) {
                            tvFile.setTextAppearance(mCtx, R.style.fromFileExpiredText);
                        } else {
                            tvFile.setTextAppearance(mCtx, R.style.fromDirectionAvailableText);
                        }
                    }
                    tvFile.setVisibility(View.VISIBLE);
                }
                break;
            case FILE_DOWNLOAD_DECRYPT:
                tvDirection.setText(mCtx.getString(R.string.label_TapToDownloadFile));
                tvDirection.setTextAppearance(mCtx, R.style.fromDirectionAvailableText);
                tvDirection.setVisibility(View.VISIBLE);

                tvFile.setText(fileInfo);
                tvFile.setTextAppearance(mCtx, R.style.fromDirectionAvailableText);
                tvFile.setVisibility(View.VISIBLE);
                break;
            case FILE_OPEN:
                tvDirection.setText(mCtx.getString(R.string.label_TapToOpenFile));
                tvDirection.setVisibility(View.VISIBLE);

                tvFile.setText(fileInfo);
                tvFile.setVisibility(View.VISIBLE);
                break;
            case MSG_DECRYPT:
                tvDirection.setText(R.string.label_TapToDecryptMessage);
                tvDirection.setTextAppearance(mCtx, R.style.fromDirectionAvailableText);
                tvDirection.setVisibility(View.VISIBLE);
                break;
            case MSG_DOWNLOAD:
                tvDirection.setText(mCtx.getString(R.string.label_TapToDownloadMessage));
                tvDirection.setTextAppearance(mCtx, R.style.fromDirectionAvailableText);
                tvDirection.setVisibility(View.VISIBLE);
                break;
            case MSG_EXPIRED:
                tvDirection.setText(mCtx.getString(R.string.error_PushMsgMessageNotFound));
                tvDirection.setTextAppearance(mCtx, R.style.fromFileExpiredText);
                tvDirection.setVisibility(View.VISIBLE);
                break;
            case MSG_EDIT:
                tvDirection.setText(R.string.label_TapToResumeDraft);
                tvDirection.setTextAppearance(mCtx, R.style.fromDirectionAvailableText);
                tvDirection.setVisibility(View.VISIBLE);

                tvFile.setText(fileInfo);
                tvFile.setVisibility(View.VISIBLE);
                break;
            case MSG_PROGRESS:
                tvFile.setText(fileInfo);
                tvFile.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }
}
