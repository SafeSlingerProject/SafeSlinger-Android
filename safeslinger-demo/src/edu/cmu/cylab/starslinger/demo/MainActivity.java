
package edu.cmu.cylab.starslinger.demo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import edu.cmu.cylab.starslinger.exchange.ExchangeActivity;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig;

public class MainActivity extends ActionBarActivity {

    private static final int RESULT_EXCHANGE = 2;
    private static byte[] mMySecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private static final String PREF_HOST = "HOST";
        private static final String PREF_SECRET = "SECRET";

        private Button buttonBeginExchange;
        private EditText editTextMySecret;
        private EditText editTextServerHostName;
        private TextView textViewResults;
        private TextView textViewWarning;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            buttonBeginExchange = (Button) rootView.findViewById(R.id.buttonBeginExchange);
            editTextMySecret = (EditText) rootView.findViewById(R.id.editTextMySecret);
            editTextServerHostName = (EditText) rootView.findViewById(R.id.editTextServerHostName);
            textViewResults = (TextView) rootView.findViewById(R.id.textViewResults);
            textViewWarning = (TextView) rootView.findViewById(R.id.textViewWarning);
            final SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

            // load simple prefs from last visit
            editTextMySecret.setText(sharedPref.getString(PREF_SECRET, null));
            editTextServerHostName.setText(sharedPref.getString(PREF_HOST, null));

            textViewWarning.setMovementMethod(LinkMovementMethod.getInstance());

            buttonBeginExchange.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    textViewResults.setText("");

                    mMySecret = editTextMySecret.getText().toString().getBytes();
                    String server = editTextServerHostName.getText().toString();

                    // save simple prefs from this visit
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(PREF_SECRET, new String(mMySecret));
                    editor.putString(PREF_HOST, server);
                    editor.commit();

                    // begin the exchange
                    Intent intent = new Intent(getActivity(), ExchangeActivity.class);
                    intent.putExtra(ExchangeConfig.extra.USER_DATA, mMySecret);
                    intent.putExtra(ExchangeConfig.extra.HOST_NAME, server);
                    startActivityForResult(intent, RESULT_EXCHANGE);
                }
            });

            return rootView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {

            switch (requestCode) {
                case RESULT_EXCHANGE:
                    StringBuilder results = new StringBuilder(getText(R.string.demo_results) + ": ");
                    switch (resultCode) {
                        case ExchangeActivity.RESULT_EXCHANGE_OK:
                            results.append(getText(R.string.demo_result_success));
                            results.append(" \n"
                                    + String.format(getString(R.string.demo_result_mine), 0,
                                            new String(mMySecret)));
                            Bundle extras = data.getExtras();
                            if (extras != null) {
                                byte[] d = null;
                                int i = 0;
                                do {
                                    d = extras.getByteArray(ExchangeConfig.extra.MEMBER_DATA + i);
                                    if (d != null) {
                                        results.append(" \n"
                                                + String.format(
                                                        getString(R.string.demo_result_theirs),
                                                        i + 1, new String(d)));
                                        i++;
                                    }
                                } while (d != null);
                            }
                            break;
                        case ExchangeActivity.RESULT_EXCHANGE_CANCELED:
                            results.append(getText(R.string.demo_result_canceled));
                            break;
                        default:
                            results.append(getText(R.string.demo_result_indeterminate));
                            break;
                    }
                    textViewResults.setText(results);
                    break;
                default:
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
