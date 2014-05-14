SafeSlinger Android Client
===================
The open source SafeSlinger Exchange library is a secure and easy to use method of exchanging public keys or other authentication data, with strong protection from Man-In-The-Middle (MITM) attacks. Our goal is to make exchanging public keys as simple as possible without sacrificing security. Our [research paper](http://sparrow.ece.cmu.edu/group/pub/farb_safeslinger_mobicom2013.pdf), presented at MobiCom '13, provides a technical analysis of SafeSlinger's key exchange properties.

Library Features:

- Open source makes security audits easy.
- The only secure simultaneous key exchange for up to 10 people.
- Easy to implement and use.
- Cross-platform Android and iOS ([iOS library](http://github.com/SafeSlingerProject/SafeSlinger-iOS) coming Spring 2014).
- Protection from Man-In-The-Middle attacks during key exchanges.
- Exchange keys either in person or remote.

The SafeSlinger secure key exchange is implemented cross-platform for [Android](http://github.com/SafeSlingerProject/SafeSlinger-Android) and [iOS](http://github.com/SafeSlingerProject/SafeSlinger-iOS) devices. Keys are exchanged using a simple server implementation on [App Engine](http://github.com/SafeSlingerProject/SafeSlinger-AppEngine).

Repository Android Projects
=======

- **/safeslinger-exchange** contains the library project you can add to your own Android applications. Both the safeslinger-demo and safeslinger-messenger application projects utilize this library to execute the exchange.
- **/safeslinger-demo** contains the simple [SafeSlinger Exchange Demo](http://play.google.com/store/apps/details?id=edu.cmu.cylab.starslinger.demo) application project which shows the minimum requirements to run a safeslinger secure exchange.
- **/safeslinger-messenger** contains the full application project source for the [SafeSlinger Messenger](http://play.google.com/store/apps/details?id=edu.cmu.cylab.starslinger) application. This project is a very rich implementation of a safeslinger secure exchange if you want an example of how to use the exchange to verify public keys in your own applications.
- **/sha3** contains only the Keccak portions of the [sphlib 3.0](http://www.saphir2.com/sphlib) library.
- **/android-vcard** is a modified version of the [android-vcard](http://code.google.com/p/android-vcard) library.
- **/android-support-v7-appcompat** is the backward-compatible [Android AppCompat](http://developer.android.com/reference/android/support/v7/app/package-summary.html) library to manage Action Bar features.

Running the Demo
========
Demo Requirements:

1. Must be installed on a minimum of 2 devices.
2. An Internet connection must be active.
3. 'Server Host Name' can be your own server, OR use ours: `slinger-demo.appspot.com`
4. 'My Secret' can be any information since it is just a demo.

![Demo Main Screen](http://www.cylab.cmu.edu/safeslinger/images/android-StartDemo.png?raw=true)

Add Secure Exchange to your Android App
========

Modify your `AndroidManifest.xml` to include required permissions.

	...
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
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

Call the `ExchangeActivity` with required parameters.

    private void beginExchange(String hostName, byte[] mySecret) {
        Intent intent = new Intent(getActivity(), ExchangeActivity.class);
        intent.putExtra(ExchangeConfig.extra.USER_DATA, mySecret);
        intent.putExtra(ExchangeConfig.extra.HOST_NAME, hostName);
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

Contact
=======

* SafeSlinger [Project Website](http://www.cylab.cmu.edu/safeslinger)
* Please submit [Bug Reports](http://github.com/SafeSlingerProject/SafeSlinger-Android/issues)!
* Looking for answers, try our [FAQ](http://www.cylab.cmu.edu/safeslinger/faq.html)!
* Support: <safeslingerapp@gmail.com>

License
=======
	The MIT License (MIT)

	Copyright (c) 2010-2014 Carnegie Mellon University

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
