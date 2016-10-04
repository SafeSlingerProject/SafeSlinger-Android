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

package edu.cmu.cylab.starslinger.demo;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import edu.cmu.cylab.starslinger.exchange.ExchangeActivity;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig;

public class MainActivity extends ActionBarActivity {

    private static final String DEFAULT_HOST_NAME = "slinger-dev.appspot.com";
    private static final int RESULT_EXCHANGE = 1;
    private static final int MENU_MSG = 2;
    private static final int MENU_NFC = 3;
    private static byte[] mMySecret;
    private ConnectivityManager mConnectivityManager;

    private static final String EXTRA_TITLE = "TITLE";
    private static final String EXTRA_MSG = "MSG";
    private static final String PREF_HOST = "HOST";
    private static final String PREF_SECRET = "SECRET";
    private static final String PREF_USENFC = "USENFC";
    private static final String PREF_GROUP_SIZE = "GROUPSIZE";
    private static final String PREF_GROUP_NAME = "GROUPNAME";

    private static Button buttonBeginExchange;
    private static EditText editTextMySecret;
    private static EditText editTextServerHostName;
    private static SeekBar seekBarGroupSize;
    private static TextView textViewGroupSizeStatus;
    private static TextView textViewWarning;
    private static ToggleButton toggleButtonUseNfc;
    private static ToggleButton toggleButtonGroupSize;
    private static ToggleButton toggleButtonGroupName;
    private static ToggleButton toggleButtonAttemptName;
    private static EditText editTextGroupName;
    private static EditText editTextAttemptName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuItem iUse = menu.add(0, MENU_MSG, 0, "Usage").setIcon(
                android.R.drawable.ic_menu_info_details);
        MenuCompat.setShowAsAction(iUse, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        if (getNfcState(this) != null) {
            menu.add(Menu.NONE, MENU_NFC, Menu.NONE, R.string.dev_menu_nfc_settings);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_MSG:
                showMessage(getString(R.string.dev_app_name_long), getString(R.string.dev_instruct));
                return true;
            case MENU_NFC:
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                } else {
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }
                break;
            default:
                break;
        }
        return false;
    }

    private static void updateNfcState(Activity act) {
        final SharedPreferences sharedPref = act.getPreferences(Context.MODE_PRIVATE);
        Boolean nfc = getNfcState(act);
        if (nfc == null) { // no system nfc
            toggleButtonUseNfc.setVisibility(View.GONE);
            toggleButtonUseNfc.setEnabled(false);
            toggleButtonUseNfc.setChecked(false);
        } else if (!nfc) { // system nfc is off
            toggleButtonUseNfc.setVisibility(View.VISIBLE);
            toggleButtonUseNfc.setEnabled(false);
            toggleButtonUseNfc.setChecked(false);
        } else { // system nfc is on
            toggleButtonUseNfc.setVisibility(View.VISIBLE);
            toggleButtonUseNfc.setEnabled(true);
            toggleButtonUseNfc.setChecked(sharedPref.getBoolean(PREF_USENFC, false));
        }
    }

    private static Boolean getNfcState(Context ctx) {
        Boolean nfc = null;

        // TODO: add additional communication mediums
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
        // NfcManager manager = (NfcManager)
        // ctx.getSystemService(Context.NFC_SERVICE);
        // if (manager != null) {
        // NfcAdapter adapter = manager.getDefaultAdapter();
        // if (adapter != null) {
        // nfc = adapter.isEnabled();
        // }
        // }
        // }

        return nfc;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            buttonBeginExchange = (Button) rootView.findViewById(R.id.buttonBeginExchange);
            editTextMySecret = (EditText) rootView.findViewById(R.id.editTextMySecret);
            editTextServerHostName = (EditText) rootView.findViewById(R.id.editTextServerHostName);
            textViewWarning = (TextView) rootView.findViewById(R.id.textViewWarning);
            toggleButtonUseNfc = (ToggleButton) rootView.findViewById(R.id.toggleButtonUseNfc);
            toggleButtonGroupSize = (ToggleButton) rootView
                    .findViewById(R.id.toggleButtonGroupSize);
            seekBarGroupSize = (SeekBar) rootView.findViewById(R.id.seekBarGroupSize);
            textViewGroupSizeStatus = (TextView) rootView
                    .findViewById(R.id.textViewGroupSizeStatus);
            toggleButtonGroupName = (ToggleButton) rootView.findViewById(R.id.toggleButtonGroupName);
            editTextGroupName = (EditText) rootView.findViewById(R.id.editTextGroupName);
            toggleButtonAttemptName = (ToggleButton) rootView.findViewById(R.id.toggleButtonAttemptName);
            editTextAttemptName = (EditText) rootView.findViewById(R.id.editTextAttemptName);

            final SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

            // load simple prefs from last visit
            editTextMySecret.setText(sharedPref.getString(PREF_SECRET, null));
            editTextServerHostName.setText(sharedPref.getString(PREF_HOST, DEFAULT_HOST_NAME));
            seekBarGroupSize.setProgress(sharedPref.getInt(PREF_GROUP_SIZE, 0));
            editTextGroupName.setText(sharedPref.getString(PREF_GROUP_NAME, null));
            editTextAttemptName.setText("");

            toggleButtonGroupSize.setChecked(false);
            textViewGroupSizeStatus.setText(seekBarGroupSize.getProgress() + " Members");
            toggleButtonGroupName.setChecked(false);
            toggleButtonAttemptName.setChecked(false);

            // enable hyperlinks
            textViewWarning.setMovementMethod(LinkMovementMethod.getInstance());

            // NFC test
            updateNfcState(getActivity());

            toggleButtonGroupSize
                    .setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {

                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                            // save simple prefs from this visit
                            if (isChecked) {
                                seekBarGroupSize.setEnabled(true);
                                textViewGroupSizeStatus.setEnabled(true);
                            } else {
                                seekBarGroupSize.setEnabled(false);
                                seekBarGroupSize.setProgress(0);
                                textViewGroupSizeStatus.setEnabled(false);
                            }
                        }
                    });

            toggleButtonGroupName
                    .setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {

                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                            // save simple prefs from this visit
                            if (isChecked) {
                                editTextGroupName.setEnabled(true);
                            } else {
                                editTextGroupName.setEnabled(false);
                                editTextGroupName.setText("");
                            }
                        }
                    });

            toggleButtonAttemptName
                    .setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {

                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                            // save simple prefs from this visit
                            if (isChecked) {
                                editTextAttemptName.setEnabled(true);
                            } else {
                                editTextAttemptName.setEnabled(false);
                                editTextAttemptName.setText("");
                            }
                        }
                    });

            seekBarGroupSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textViewGroupSizeStatus.setText(progress + " Members");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            toggleButtonUseNfc
                    .setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {

                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                            // save simple prefs from this visit
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putBoolean(PREF_USENFC, isChecked);
                            editor.commit();
                        }
                    });

            buttonBeginExchange.setOnClickListener(new android.view.View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mMySecret = editTextMySecret.getText().toString().getBytes();
                    String server = editTextServerHostName.getText().toString();

                    // save simple prefs from this visit
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(PREF_SECRET, new String(mMySecret));
                    editor.putString(PREF_HOST, server);
                    if (toggleButtonGroupSize.isChecked()) {
                        editor.putInt(PREF_GROUP_SIZE, seekBarGroupSize.getProgress());
                    }
                    if (toggleButtonGroupName.isChecked()) {
                        editor.putString(PREF_GROUP_NAME, editTextGroupName.getText().toString());
                    }
                    editor.commit();

                    // check online state
                    if (((MainActivity) getActivity()).isOnline()) {
                        // begin the exchange
                        beginExchange(server, mMySecret);
                    } else {
                        Toast.makeText(getActivity(), R.string.error_CorrectYourInternetConnection,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

            return rootView;
        }

        private void beginExchange(String hostName, byte[] mySecret) {
            Intent intent = new Intent(getActivity(), ExchangeActivity.class);
            intent.putExtra(ExchangeConfig.extra.USER_DATA, mySecret);
            intent.putExtra(ExchangeConfig.extra.HOST_NAME, hostName);

            if (toggleButtonGroupSize.isChecked()) {
                intent.putExtra(ExchangeConfig.extra.NUM_USERS, seekBarGroupSize.getProgress());
            }
            if (toggleButtonGroupName.isChecked()) {
                intent.putExtra(ExchangeConfig.extra.GROUP_NAME, editTextGroupName.getText().toString().getBytes());
            }
            if (toggleButtonAttemptName.isChecked()) {
                intent.putExtra(ExchangeConfig.extra.ATTEMPT_NAME, editTextAttemptName.getText().toString().getBytes());
            }

            startActivityForResult(intent, RESULT_EXCHANGE);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {

            switch (requestCode) {
                case RESULT_EXCHANGE:
                    StringBuilder results = new StringBuilder();
                    switch (resultCode) {
                        case ExchangeActivity.RESULT_EXCHANGE_OK:
                            // use newly exchanged data from 'others'
                            ArrayList<byte[]> theirSecrets = endExchange(data);
                            // ...

                            // display results
                            results.append(getText(R.string.dev_result_success));
                            results.append(" \n"
                                    + String.format(getString(R.string.dev_result_mine), 0,
                                    new String(mMySecret)));
                            for (int j = 0; j < theirSecrets.size(); j++) {
                                results.append(" \n"
                                        + String.format(getString(R.string.dev_result_theirs),
                                        j + 1, new String(theirSecrets.get(j))));
                            }
                            break;
                        case ExchangeActivity.RESULT_EXCHANGE_CANCELED:
                            // handle canceled result
                            // ...
                            results.append(getText(R.string.dev_result_canceled));
                            break;
                        default:
                            results.append(getText(R.string.dev_result_indeterminate));
                            break;
                    }
                    ((MainActivity) getActivity()).showMessage(getString(R.string.dev_results),
                            results.toString());
                    break;
                default:
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }

        private static ArrayList<byte[]> endExchange(Intent data) {
            ArrayList<byte[]> theirSecrets = new ArrayList<byte[]>();
            Bundle extras = data.getExtras();
            if (extras != null) {
                byte[] d = null;
                int i = 0;
                do {
                    d = extras.getByteArray(ExchangeConfig.extra.MEMBER_DATA + i);
                    if (d != null) {
                        theirSecrets.add(d);
                        i++;
                    }
                } while (d != null);
            }
            return theirSecrets;
        }

        @Override
        public void onResume() {
            updateNfcState(getActivity());
            super.onResume();
        }
    }

    protected void showMessage(String title, String msg) {
        Bundle args = new Bundle();
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_MSG, msg);
        if (!isFinishing()) {
            try {
                removeDialog(MENU_MSG);
                showDialog(MENU_MSG, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    protected static AlertDialog.Builder xshowMessage(Activity act, Bundle args) {
        String title = args.getString(EXTRA_TITLE);
        String msg = args.getString(EXTRA_MSG);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(title);
        ad.setMessage(msg);
        ad.setCancelable(true);
        ad.setNeutralButton(R.string.dev_btn_OK,
                new android.content.DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        ad.setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case MENU_MSG:
                return xshowMessage(MainActivity.this, args).create();
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

    public boolean isOnline() {
        boolean connected = false;
        if (mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        connected = networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
        return connected;
    }
}
