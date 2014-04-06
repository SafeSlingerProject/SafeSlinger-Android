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

import java.util.Date;
import java.util.List;

import android.R.color;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.model.RecipientDbAdapter;
import edu.cmu.cylab.starslinger.model.RecipientRow;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class RecipientAdapter extends BaseAdapter {
    private Context mContext;
    private List<RecipientRow> mListRecipients;

    public RecipientAdapter(Context context, List<RecipientRow> mcontacts) {
        mContext = context;
        mListRecipients = mcontacts;
    }

    @Override
    public int getCount() {
        return mListRecipients.size();
    }

    @Override
    public Object getItem(int pos) {
        return mListRecipients.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        // get selected entry
        RecipientRow recip = mListRecipients.get(pos);

        // always inflate the view, otherwise check box states will get recycled
        // on scrolling...
        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(R.layout.recipientitem, null);

        convertView.setTag(Long.valueOf(recip.getRowId()));
        drawRecipientRow(convertView, recip);

        return convertView;
    }

    private void drawRecipientRow(View convertView, RecipientRow recip) {
        // ensure only push-compatible recipients
        boolean pushable = recip.isPushable();
        boolean fromExch = recip.isFromTrustedSource();
        boolean secretChanged = recip.hasMyKeyChanged();
        boolean deprecated = recip.isDeprecated();
        boolean registered = recip.isRegistered();
        boolean useableKey = pushable && !secretChanged && !deprecated && fromExch;
        StringBuilder detailStr = new StringBuilder();

        // show activation
        if (!recip.isActive()) {
            convertView.setBackgroundColor(Color.LTGRAY);
        } else if (!useableKey) {
            convertView.setBackgroundColor(0xffffffcc);
        } else {
            convertView.setBackgroundColor(color.background_light);
        }

        // set avatar
        ImageView ivAvatar = (ImageView) convertView.findViewById(R.id.imgAvatar);
        try {
            if (recip.getPhoto() != null) {
                Bitmap photo = BitmapFactory.decodeByteArray(recip.getPhoto(), 0,
                        recip.getPhoto().length, null);
                ivAvatar.setImageBitmap(photo);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_silhouette);
            }
        } catch (OutOfMemoryError e) {
            ivAvatar.setImageBitmap(null);
        }

        // set name
        TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
        tvName.setText(recip.getName());

        // set notify
        TextView tvNotify = (TextView) convertView.findViewById(R.id.tvNotify);
        tvNotify.setText(SSUtil.getSimpleDeviceDisplayName(mContext, recip.getNotify()));

        // set key dates
        TextView tvDates = (TextView) convertView.findViewById(R.id.tvDetail);
        if (recip.getKeydate() > 0) {
            detailStr.append(mContext.getText(R.string.label_Key)).append(" ")
                    .append(DateUtils.getRelativeTimeSpanString(mContext, recip.getKeydate()));
        }
        if (recip.getSource() == RecipientDbAdapter.RECIP_SOURCE_EXCHANGE) {
            detailStr.append(" (").append(mContext.getText(R.string.label_exchanged)).append(" ")
                    .append(DateUtils.getRelativeTimeSpanString(mContext, recip.getExchdate()))
                    .append(")");
        } else if (recip.getSource() == RecipientDbAdapter.RECIP_SOURCE_INTRODUCTION) {
            detailStr.append(" (").append(mContext.getText(R.string.label_introduced)).append(" ")
                    .append(DateUtils.getRelativeTimeSpanString(mContext, recip.getExchdate()))
                    .append(")");
        }
        tvDates.setText(detailStr);

        // set key id
        TextView tvDetail2 = (TextView) convertView.findViewById(R.id.tvDetailSingleLine);
        tvDetail2.setText(mContext.getText(R.string.label_Key) + " " + recip.getKeyid());

        // set device/token id
        TextView tvDetail3 = (TextView) convertView.findViewById(R.id.tvDetailSingleLine2);
        tvDetail3.setText(mContext.getText(R.string.label_Device) + " " + recip.getPushtoken());

        // set errors
        TextView tvError = (TextView) convertView.findViewById(R.id.tvError);
        StringBuilder err = new StringBuilder();
        if (secretChanged) { // error: their key out of date
            if (err.length() > 0)
                err.append("\n");
            err.append(mContext.getText(R.string.label_DisabledExchSourceKeyChange));
        } else if (deprecated) { // error: old format, usable for a time...
            if (err.length() > 0)
                err.append("\n");
            err.append(mContext.getText(R.string.label_EnabledKeyFormatDeprecated));
        } else if (!registered) { // warning: last reported non-registered...
            if (err.length() > 0)
                err.append("\n");
            err.append(String.format(mContext.getString(R.string.label_ReportedInactive), DateUtils
                    .getRelativeTimeSpanString(recip.getNotRegDate(), new Date().getTime(),
                            DateUtils.MINUTE_IN_MILLIS)));
        }

        if (TextUtils.isEmpty(err)) {
            tvError.setVisibility(View.GONE);
        } else {
            tvError.setText(err);
            tvError.setVisibility(View.VISIBLE);
        }
    }
}
