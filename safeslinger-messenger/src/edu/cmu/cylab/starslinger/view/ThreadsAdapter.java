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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.model.MessageDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientRow;
import edu.cmu.cylab.starslinger.model.ThreadData;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class ThreadsAdapter extends BaseAdapter {
    private Context mCtx;
    private List<ThreadData> mListThreads;

    public ThreadsAdapter(Context context, List<ThreadData> list) {
        mCtx = context;
        mListThreads = list;
    }

    @Override
    public int getCount() {
        return mListThreads.size();
    }

    @Override
    public Object getItem(int pos) {
        return mListThreads.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        // get selected entry
        ThreadData t = mListThreads.get(pos);

        // always inflate the view, otherwise check box states will get recycled
        // on scrolling...
        LayoutInflater inflater = LayoutInflater.from(mCtx);
        convertView = inflater.inflate(R.layout.threaditem, null);

        // convertView.setTag(Long.valueOf(thread.getRowId()));
        drawThreadItem(convertView, t);

        // Return the view for rendering
        return convertView;
    }

    private void drawThreadItem(View convertView, ThreadData t) {
        RecipientRow recip = t.getRecipient();

        ImageView ivAvatar = (ImageView) convertView.findViewById(R.id.imgAvatar);
        if (t.isDetail()) {
            ivAvatar.setVisibility(View.GONE);
        } else {
            ivAvatar.setVisibility(View.VISIBLE);
            try {
                if (recip == null || recip.getPhoto() == null) {
                    ivAvatar.setImageResource(R.drawable.ic_silhouette);
                } else {
                    Bitmap photo = BitmapFactory.decodeByteArray(recip.getPhoto(), 0,
                            recip.getPhoto().length, null);
                    ivAvatar.setImageBitmap(photo);
                }
            } catch (OutOfMemoryError e) {
                ivAvatar.setImageBitmap(null);
            }
            if (recip == null || !recip.isSendable() || t.isNewerExists()) {
                ivAvatar.setAlpha(75);
            }
        }

        TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
        tvName.setText(getBestIdentityName(mCtx, t, recip));

        TextView tvCount = (TextView) convertView.findViewById(R.id.tvCount);
        tvCount.setText(Integer.toString(t.getMsgCount()));

        TextView tvNew = (TextView) convertView.findViewById(R.id.tvNew);
        if (t.getNewCount() > 0) {
            tvNew.setVisibility(View.VISIBLE);
            tvNew.setText("(" + Integer.toString(t.getNewCount()) + ")");
        } else {
            tvNew.setVisibility(View.GONE);
        }

        TextView tvStatus = (TextView) convertView.findViewById(R.id.tvStatus);
        if (!t.isDetail() && t.hasDraft()) {
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(R.string.label_Draft);
        } else {
            tvStatus.setVisibility(View.GONE);
        }

        TextView tvNotify = (TextView) convertView.findViewById(R.id.tvNotify);
        if (t.isDetail()) {
            tvNotify.setVisibility(View.VISIBLE);
            if (recip != null) {
                tvNotify.setText(SSUtil.getSimpleDeviceDisplayName(mCtx, recip.getNotify()));
            }
        } else {
            tvNotify.setVisibility(View.GONE);
        }

        TextView tvDate = (TextView) convertView.findViewById(R.id.tvDate);
        if (t.isDetail()) {
            tvDate.setVisibility(View.GONE);
        } else {
            tvDate.setVisibility(View.VISIBLE);
            String dateTime = DateUtils.getRelativeTimeSpanString(mCtx,
                    t.getMsgRow().getProbableDate()).toString();
            if (t.getMsgRow().getStatus() == MessageDbAdapter.MESSAGE_STATUS_QUEUED) {
                if (!TextUtils.isEmpty(t.getProgress())) {
                    tvDate.setText(t.getProgress());
                } else {
                    tvDate.setText(mCtx.getString(R.string.prog_pending));
                }
            } else {
                tvDate.setText(dateTime);
            }
        }

        if (t.isDetail()) {
            convertView.setBackgroundColor(mCtx.getResources().getColor(R.color.cmu_darkgray));
            tvName.setTextColor(mCtx.getResources().getColor(android.R.color.white));
            tvNew.setTextColor(mCtx.getResources().getColor(android.R.color.white));
            tvNotify.setTextColor(mCtx.getResources().getColor(android.R.color.white));
        }
    }

    public static String getBestIdentityName(Context ctx, ThreadData t, RecipientRow recip) {
        String person = null;
        if (TextUtils.isEmpty(t.getMsgRow().getKeyId())) {
            person = ctx.getString(R.string.label_undefinedTypeLabel);
        } else {
            if (recip == null) {
                if (TextUtils.isEmpty(t.getLastPerson())) {
                    person = t.getMsgRow().getKeyId();
                } else {
                    person = t.getLastPerson();
                }
            } else {
                person = recip.getName();
            }
        }
        return person;
    }
}
