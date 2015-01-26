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

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.model.AccountData;

public class AccountAdapter extends ArrayAdapter<AccountData> {

    private Context mContext;
    private Activity mActivity;

    public AccountAdapter(Context context, Activity activity, ArrayList<AccountData> accountData) {
        super(context, android.R.layout.simple_spinner_item, accountData);
        mContext = context;
        mActivity = activity;
        setDropDownViewResource(R.layout.account_entry);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        // Inflate a view template
        if (convertView == null) {
            LayoutInflater layoutInflater = mActivity.getLayoutInflater();
            convertView = layoutInflater.inflate(R.layout.account_entry, parent, false);
        }
        TextView firstAccountLine = (TextView) convertView.findViewById(R.id.firstAccountLine);
        TextView secondAccountLine = (TextView) convertView.findViewById(R.id.secondAccountLine);
        ImageView accountIcon = (ImageView) convertView.findViewById(R.id.accountIcon);

        // Populate template
        AccountData data = getItem(position);
        firstAccountLine.setText(data.getName());
        secondAccountLine.setText(data.getTypeLabel());
        Drawable icon = data.getIcon();
        if (icon == null) {
            icon = mContext.getResources().getDrawable(android.R.drawable.ic_menu_search);
        }
        accountIcon.setImageDrawable(icon);
        return convertView;
    }
}
