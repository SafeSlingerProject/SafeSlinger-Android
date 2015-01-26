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

import java.util.List;

import a_vcard.android.syncml.pim.vcard.ContactStruct;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.R;

public class SaveContactAdapter extends BaseAdapter {
    private Context mContext;
    private List<ContactStruct> mListSaveContacts;
    private static Boolean mChecked[];

    public SaveContactAdapter(Context context, List<ContactStruct> contacts) {
        mContext = context;
        mListSaveContacts = contacts;
        mChecked = new Boolean[contacts.size()];
    }

    @Override
    public int getCount() {
        return mListSaveContacts.size();
    }

    @Override
    public Object getItem(int pos) {
        return mListSaveContacts.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        // get selected entry
        ContactStruct mem = mListSaveContacts.get(pos);

        // always inflate the view, otherwise check box states will get recycled
        // on scrolling...
        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(R.layout.savedataitem, null);

        drawSaveContact(pos, convertView, mem);
        return convertView;
    }

    private void drawSaveContact(final int position, View convertView, ContactStruct mem) {

        // draw on screen
        CheckBox cb = (CheckBox) convertView.findViewById(R.id.cb);
        TextView name = (TextView) convertView.findViewById(R.id.name);
        ImageView avatar = (ImageView) convertView.findViewById(R.id.avatar);

        if (mChecked[position] == null) {
            mChecked[position] = true; // set default choice
        }
        cb.setChecked(mChecked[position]);
        cb.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    mChecked[position] = true;
                    v.setSelected(true);
                } else {
                    mChecked[position] = false;
                    v.setSelected(false);
                }
            }
        });

        name.setText(mem.name != null ? mem.name.toString() : mContext
                .getString(R.string.error_ContactInsertFailed));

        try {
            if (mem.photoBytes != null) {
                Bitmap bm = BitmapFactory.decodeByteArray(mem.photoBytes, 0, mem.photoBytes.length,
                        null);
                avatar.setImageBitmap(bm);
            } else {
                avatar.setImageResource(R.drawable.ic_silhouette);
            }
        } catch (OutOfMemoryError e) {
            avatar.setImageBitmap(null);
        }

    }

    public static Boolean isPositionChecked(int position) {
        if (mChecked != null && mChecked.length > position) {
            return mChecked[position];
        } else {
            return false;
        }
    }
}
