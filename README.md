[![Build Status](https://travis-ci.org/SafeSlingerProject/SafeSlinger-Android.png?branch=master)](https://travis-ci.org/SafeSlingerProject/SafeSlinger-Android)

Quick Links:
[Downloads](https://github.com/SafeSlingerProject/SafeSlinger-Media/wiki/Platforms),
[Wiki](https://github.com/SafeSlingerProject/SafeSlinger-Media/wiki),
[Support, Translations, Contributing](https://github.com/SafeSlingerProject/SafeSlinger-Media/wiki/Contributing),
[Research Paper](http://sparrow.ece.cmu.edu/group/pub/farb_safeslinger_mobicom2013.pdf),
[Project Website](http://www.cylab.cmu.edu/safeslinger)

SafeSlinger Android Client Projects
===================

- **/safeslinger-exchange** Contains the library project you can add to your own Android applications. Both the safeslinger-demo and safeslinger-messenger application projects utilize this library to execute the exchange.
- **/safeslinger-demo** Contains the simple [SafeSlinger Exchange Developer's App](http://play.google.com/store/apps/details?id=edu.cmu.cylab.starslinger.demo) application project which shows the minimum requirements to run a safeslinger secure exchange.
- **/safeslinger-messenger** Contains the full application project source for the [SafeSlinger Messenger](http://play.google.com/store/apps/details?id=edu.cmu.cylab.starslinger) application. This project is a very rich implementation of a safeslinger secure exchange if you want an example of how to use the exchange to verify public keys in your own applications.
- **/sha3** Contains only the Keccak portions of the [sphlib 3.0](http://www.saphir2.com/sphlib) library.
- **/android-vcard** This is a modified version of the [android-vcard](http://code.google.com/p/android-vcard) library.


Running the Developer's App
========
Developer's App Requirements:

1. Must be installed on a minimum of 2 devices.
2. An Internet connection must be active.
3. 'Server Host Name' can be your own server, OR use ours: `slinger-dev.appspot.com`
4. 'My Secret' can be any information.

![Developer's App Main Screen](http://www.cylab.cmu.edu/safeslinger/images/android-StartDemo.png?raw=true)

Add Secure Exchange to your Android App
========

Modify your `AndroidManifest.xml` to include required permissions.

	...
    <uses-permission android:name="android.permission.INTERNET" />
	...

Modify your `AndroidManifest.xml` to include required activities.

	...
    <!-- we must include all activities declared by library manifests as well: -->
    <activity
        android:name="edu.cmu.cylab.starslinger.exchange.ExchangeActivity"
        android:label="@string/lib_name"
        android:screenOrientation="portrait" >
    </activity>
    <activity
        android:name="edu.cmu.cylab.starslinger.exchange.GroupingActivity"
        android:label="@string/lib_name"
        android:screenOrientation="portrait" >
    </activity>
    <activity
        android:name="edu.cmu.cylab.starslinger.exchange.VerifyActivity"
        android:label="@string/lib_name"
        android:screenOrientation="portrait" >
    </activity>
	...

Modify your `Activity` that starts the exchange to import `Bundle` string extras for convenience and to avoid potential refactoring of names in future versions of the library.

	...
	import edu.cmu.cylab.starslinger.exchange.ExchangeActivity;
	import edu.cmu.cylab.starslinger.exchange.ExchangeConfig;

	public class MainActivity extends Activity {

    	private static final int RESULT_EXCHANGE = 1;
	...

Call the `ExchangeActivity` with required extras `USER_DATA` and `HOST_NAME`. The UX will ask for the number of users, grouping number, and 3-word phrase.

    private void beginExchange(String hostName, byte[] mySecret) {
        Intent intent = new Intent(getActivity(), ExchangeActivity.class);
        intent.putExtra(ExchangeConfig.extra.USER_DATA, mySecret);
        intent.putExtra(ExchangeConfig.extra.HOST_NAME, hostName);
        startActivityForResult(intent, RESULT_EXCHANGE);
    }
    
Optionally, you may pass in the number of users using the `NUM_USERS` extra. The UX will ask for the grouping number and 3-word phrase only. 

    private void beginExchange(String hostName, byte[] mySecret, int numUsersIn) {
        Intent intent = new Intent(getActivity(), ExchangeActivity.class);
        intent.putExtra(ExchangeConfig.extra.USER_DATA, mySecret);
        intent.putExtra(ExchangeConfig.extra.HOST_NAME, hostName);
        intent.putExtra(ExchangeConfig.extra.NUM_USERS, numUsersIn);
        startActivityForResult(intent, RESULT_EXCHANGE);
    }

Handle the `ExchangeActivity` results.

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case RESULT_EXCHANGE:
                switch (resultCode) {
                    case ExchangeActivity.RESULT_EXCHANGE_OK:
                        // use newly exchanged data from 'theirSecrets'
                        ArrayList<byte[]> theirSecrets = endExchange(data);
                        // ...
                        break;
                    case ExchangeActivity.RESULT_EXCHANGE_CANCELED:
                        // handle canceled result
                        // ...
                        break;
                }
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

License
=======
	The MIT License (MIT)

	Copyright (c) 2010-2015 Carnegie Mellon University

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.
