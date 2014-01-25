
package edu.cmu.cylab.keyslinger.lib;

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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;

/**
 * This class does all of the TCP connection setup to the server and handles the
 * HTTP functions GET and POST. In addition to basic GET and POST, it also has
 * web_spate specific functions to get the group size, get the commitments,
 * create the group on the server, send data, ....
 */
public class WebEngine {

    private static final String TAG = KsConfig.LOG_TAG;

    private String mUrlPrefix;
    private String mHost;
    private String mUrlSuffix;
    private boolean mCancelable = false;
    private int mVersion;
    private int mLatestServerVersion = 0;
    private Context mCtx;
    private HttpClient mHttpClient;

    private Date mExchStartTimer;

    public WebEngine(Context ctx) {
        mCtx = ctx;
        mUrlPrefix = KsConfig.HTTPURL_PREFIX;
        mHost = KsConfig.HTTPURL_HOST;
        mUrlSuffix = KsConfig.HTTPURL_SUFFIX;

        mVersion = KsConfig.getVersionCode(ctx);
    }

    private byte[] doPost(String uri, byte[] requestBody) throws ExchangeException {
        mCancelable = false;

        if (!SafeSlinger.getApplication().isOnline()) {
            throw new ExchangeException(
                    mCtx.getString(R.string.error_CorrectYourInternetConnection));
        }

        // sets up parameters
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "utf-8");
        params.setBooleanParameter("http.protocol.expect-continue", false);

        if (mHttpClient == null) {
            mHttpClient = new KsHttpClient(params, mCtx);
        }
        HttpPost httppost = new HttpPost(uri);
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        byte[] reqData = null;
        HttpResponse response = null;
        long startTime = SystemClock.elapsedRealtime();
        int statCode = 0;
        String statMsg = "";
        String error = "";

        try {
            // Execute HTTP Post Request
            httppost.addHeader("Content-Type", "application/octet-stream");
            httppost.setEntity(new ByteArrayEntity(requestBody));
            response = mHttpClient.execute(httppost);
            reqData = responseHandler.handleResponse(response).getBytes("8859_1");

        } catch (UnsupportedEncodingException e) {
            error = e.getLocalizedMessage() + " (" + e.getClass().getSimpleName() + ")";
        } catch (HttpResponseException e) {
            // this subclass of java.io.IOException contains useful data for
            // users, do not swallow, handle properly
            statCode = e.getStatusCode();
            statMsg = e.getLocalizedMessage();
            error = (String.format(mCtx.getString(R.string.error_HttpCode), statCode) + ", \'"
                    + statMsg + "\'");
        } catch (java.io.IOException e) {
            // just show a simple Internet connection error, so as not to
            // confuse users
            error = mCtx.getString(R.string.error_CorrectYourInternetConnection);
        } catch (RuntimeException e) {
            error = e.getLocalizedMessage() + " (" + e.getClass().getSimpleName() + ")";
        } catch (OutOfMemoryError e) {
            error = mCtx.getString(R.string.error_OutOfMemoryError);
        } finally {
            long msDelta = SystemClock.elapsedRealtime() - startTime;
            if (response != null) {
                StatusLine status = response.getStatusLine();
                if (status != null) {
                    statCode = status.getStatusCode();
                    statMsg = status.getReasonPhrase();
                }
            }
            MyLog.d(TAG, uri + ", " + requestBody.length + "b sent, "
                    + (reqData != null ? reqData.length : 0) + "b recv, " + statCode + " code, "
                    + msDelta + "ms");
        }

        if (!TextUtils.isEmpty(error) || reqData == null) {
            throw new ExchangeException(error);
        }
        return reqData;
    }

    public void shutdownConnection() {
        if (mHttpClient != null) {
            ClientConnectionManager cm = mHttpClient.getConnectionManager();
            if (cm != null)
                cm.shutdown();
        }
    }

    /**
     * send commitment, receives unique short user id
     */
    public byte[] assign_user(byte[] commitB) throws ExchangeException {
        ByteBuffer msg = ByteBuffer.allocate(4 + commitB.length);
        msg.putInt(mVersion);
        msg.put(commitB);

        byte[] resp = doPost(mUrlPrefix + mHost + "/assignUser" + mUrlSuffix, msg.array());
        handleResponseExceptions(resp, 0);

        mExchStartTimer = new Date();

        MyLog.i(TAG, "User id created");
        return resp;
    }

    /**
     * send our own matches once, list of all users we have, gathers all others
     * when available receives the group id, total user number (including ours),
     * actual user number, list of users we did not get yet
     */
    public byte[] sync_commits(int usrid, int sameusrid, int[] usridList, byte[] commitB)
            throws ExchangeException {

        handleTimeoutException();

        ByteBuffer msg = ByteBuffer.allocate(4 + 4 + 4 + 4 + (usridList.length * 4)
                + commitB.length);
        msg.putInt(mVersion);
        msg.putInt(usrid);
        msg.putInt(sameusrid);
        msg.putInt(usridList.length);
        for (int user : usridList) {
            msg.putInt(user);
        }
        msg.put(commitB);

        byte[] resp = doPost(mUrlPrefix + mHost + "/syncUsers_1_2" + mUrlSuffix, msg.array());
        handleResponseExceptions(resp, 0);

        MyLog.i(TAG, "User created");
        return resp;
    }

    /**
     * send our own data once, list of all data we have, gathers all others when
     * available receives the total data number (including ours), actual data
     * number, list of data we did not get yet
     */
    public byte[] sync_data(int usrid, int[] usridList, byte[] data) throws ExchangeException {

        handleTimeoutException();

        ByteBuffer msg = ByteBuffer.allocate(4 + 4 + 4 + (usridList.length * 4) + data.length);
        msg.putInt(mVersion);
        msg.putInt(usrid);
        msg.putInt(usridList.length);
        for (int user : usridList) {
            msg.putInt(user);
        }
        msg.put(data);

        byte[] resp = doPost(mUrlPrefix + mHost + "/syncData_1_2" + mUrlSuffix, msg.array());
        handleResponseExceptions(resp, 0);

        MyLog.i(TAG, "User updated");
        return resp;
    }

    /**
     * send our own signature once, list of all signatures we have, gathers all
     * others when available receives the total signatures number (including
     * ours), actual sig number, list of signatures we did not get yet
     */
    public byte[] sync_signatures(int usrid, int[] usridList, byte[] signature)
            throws ExchangeException {

        handleTimeoutException();

        ByteBuffer msg = ByteBuffer.allocate(4 + 4 + 4 + (usridList.length * 4) + signature.length);
        msg.putInt(mVersion);
        msg.putInt(usrid);
        msg.putInt(usridList.length);
        for (int user : usridList) {
            msg.putInt(user);
        }
        msg.put(signature);

        byte[] resp = doPost(mUrlPrefix + mHost + "/syncSignatures_1_2" + mUrlSuffix, msg.array());
        handleResponseExceptions(resp, 0);

        MyLog.i(TAG, "Signature sent");
        return resp;
    }

    /**
     * this method is used by members to post one public key node. send: node to
     * submit (user id, node length, node);
     */
    public byte[] put_keynode(int usrid, int nodeusrid, byte[] node) throws ExchangeException {

        handleTimeoutException();

        ByteBuffer msg = ByteBuffer.allocate(4 + 4 + 4 + 4 + node.length);
        msg.putInt(mVersion);
        msg.putInt(usrid);
        msg.putInt(nodeusrid);
        msg.putInt(node.length);
        msg.put(node);

        byte[] resp = doPost(mUrlPrefix + mHost + "/syncKeyNodes_1_3" + mUrlSuffix, msg.array());
        handleResponseExceptions(resp, 2);

        MyLog.i(TAG, "key node sent");
        return resp;
    }

    /**
     * this method is used by members to discover if their key node node has
     * been submitted. receive: the total nodes number for themselves (0 or 1),
     * our own key node if available.
     */
    public byte[] get_keynode(int usrid) throws ExchangeException {

        handleTimeoutException();

        ByteBuffer msg = ByteBuffer.allocate(4 + 4);
        msg.putInt(mVersion);
        msg.putInt(usrid);

        byte[] resp = doPost(mUrlPrefix + mHost + "/syncKeyNodes_1_3" + mUrlSuffix, msg.array());
        handleResponseExceptions(resp, 2);

        MyLog.i(TAG, "key node requested");
        return resp;
    }

    /**
     * send our own match nonce once, list of all match nonces we have, gathers
     * all others when available receives the total match nonces number
     * (including ours), actual match nonce number, list of match nonces we did
     * not get yet
     */
    public byte[] sync_match(int usrid, int[] usridList, byte[] matchNonce)
            throws ExchangeException {

        handleTimeoutException();

        ByteBuffer msg = ByteBuffer
                .allocate(4 + 4 + 4 + (usridList.length * 4) + matchNonce.length);
        msg.putInt(mVersion);
        msg.putInt(usrid);
        msg.putInt(usridList.length);
        for (int user : usridList) {
            msg.putInt(user);
        }
        msg.put(matchNonce);

        byte[] resp = doPost(mUrlPrefix + mHost + "/syncMatch_1_2" + mUrlSuffix, msg.array());
        handleResponseExceptions(resp, 0);

        MyLog.i(TAG, "Match nonce sent");
        return resp;
    }

    public void handleTimeoutException() throws ExchangeException {
        long elapsedMs = new Date().getTime() - mExchStartTimer.getTime();
        if (elapsedMs > KsConfig.MSSVR_EXCH_PROT_MAX) {
            throw new ExchangeException(
                    mCtx.getString(R.string.error_ExchangeProtocolTimeoutExceeded));
        }
    }

    private byte[] handleResponseExceptions(byte[] resp, int errMax) throws ExchangeException {
        int firstInt = 0;
        ByteBuffer result = ByteBuffer.wrap(resp);
        if (mCancelable)
            throw new ExchangeException(mCtx.getString(R.string.error_WebCancelledByUser));
        else if (resp == null)
            throw new ExchangeException(mCtx.getString(R.string.error_ServerNotResponding));
        else if (resp.length < 4)
            throw new ExchangeException(mCtx.getString(R.string.error_ServerNotResponding));
        else {
            firstInt = result.getInt();
            byte[] bytes = new byte[result.remaining()];
            result.get(bytes);
            if (firstInt <= errMax) { // error int
                MyLog.e(TAG, "Server Error Code: " + firstInt);
                throw new ExchangeException(String.format(
                        mCtx.getString(R.string.error_ServerAppMessage), new String(bytes).trim()));
            }
            // else strip off server version
            mLatestServerVersion = firstInt;
            return bytes;
        }
    }

    public boolean isCancelable() {
        return mCancelable;
    }

    public void setCancelable(boolean cancelable) {
        mCancelable = cancelable;
    }

    public int getLatestServerVersion() {
        return mLatestServerVersion;
    }
}
