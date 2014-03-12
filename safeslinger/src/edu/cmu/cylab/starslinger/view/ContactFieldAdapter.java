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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.model.ContactField;

public class ContactFieldAdapter extends BaseAdapter {
    private Context mContext;
    private List<ContactField> mListContactFields;

    public ContactFieldAdapter(Context context, List<ContactField> mContactFields) {
        mContext = context;
        mListContactFields = mContactFields;
    }

    @Override
    public int getCount() {
        return mListContactFields.size();
    }

    @Override
    public Object getItem(int pos) {
        return mListContactFields.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        // get selected entry
        ContactField field = mListContactFields.get(pos);

        // always inflate the view, otherwise check box states will get recycled
        // on scrolling...
        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(R.layout.contactfielditem, null);

        drawContactField(convertView, field);
        return convertView;
    }

    private void drawContactField(View convertView, ContactField field) {

        CheckBox cb = (CheckBox) convertView.findViewById(R.id.cb);
        ImageView img = (ImageView) convertView.findViewById(R.id.img);
        TextView name = (TextView) convertView.findViewById(R.id.name);
        TextView value = (TextView) convertView.findViewById(R.id.value);

        String n = field.getName();
        String v = field.getValue();
        int icon = field.getIconResId();
        boolean forceCheck = field.getForceChecked();

        final String compare = getFieldForCompare(n, v);

        if (forceCheck) {
            cb.setChecked(true);
            cb.setEnabled(false);
        } else {
            cb.setChecked(ConfigData.loadPrefContactField(mContext, compare));
            cb.setEnabled(true);
        }

        // TODO highlight checked lines for clarity

        try {
            if (icon != 0) {
                img.setImageResource(icon);
            } else {
                img.setBackgroundResource(0);
            }
        } catch (OutOfMemoryError e) {
            img.setImageBitmap(null);
        }

        name.setText(n);
        value.setText(v);

        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ConfigData.savePrefContactField(mContext, compare, isChecked);
            }
        });
    }

    public static String getFieldForCompare(String n, String v) {
        final String compare = (n != null ? n.trim() : "") + (v != null ? v.trim() : "");
        return compare;
    }
}
