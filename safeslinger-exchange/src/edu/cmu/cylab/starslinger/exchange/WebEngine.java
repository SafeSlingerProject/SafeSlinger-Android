
package edu.cmu.cylab.starslinger.exchange;

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

import java.io.UnsupportedEncodingException;
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

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

/**
 * This class does all of the TCP connection setup to the server and handles the
 * HTTP functions GET and POST. In addition to basic GET and POST, it also has
 * web_spate specific functions to get the group size, get the commitments,
 * create the group on the server, send data, ....
 */
public class WebEngine extends ConnectionEngine {

    private static final String TAG = ExchangeConfig.LOG_TAG;
    private String mUrlPrefix = ExchangeConfig.HTTPURL_PREFIX;
    private String mHost;
    private String mUrlSuffix = ExchangeConfig.HTTPURL_SUFFIX;
    private HttpClient mHttpClient;

    public void setHost(String host) {
        mHost = host;
    }

    private byte[] doPost(String uri, byte[] requestBody) throws ExchangeException {
        mCancelable = false;

        // sets up parameters
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "utf-8");
        params.setBooleanParameter("http.protocol.expect-continue", false);

        if (mHttpClient == null) {
            mHttpClient = new CheckedHttpClient(params, mCtx);
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
            e.printStackTrace();
            statCode = e.getStatusCode();
            statMsg = e.getLocalizedMessage();
            error = (String.format(mCtx.getString(R.string.error_HttpCode), statCode) + ", \'"
                    + statMsg + "\'");
        } catch (java.io.IOException e) {
            // just show a simple Internet connection error, so as not to
            // confuse users
            e.printStackTrace();
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
            Log.d(TAG, uri + ", " + requestBody.length + "b sent, "
                    + (reqData != null ? reqData.length : 0) + "b recv, " + statCode + " code, "
                    + msDelta + "ms");
        }

        if (!TextUtils.isEmpty(error) || reqData == null) {
            throw new ExchangeException(error);
        }
        return reqData;
    }

    @Override
    public void shutdownConnection() {
        if (mHttpClient != null) {
            ClientConnectionManager cm = mHttpClient.getConnectionManager();
            if (cm != null) {
                cm.shutdown();
                mHttpClient = null;
            }
        }
    }

    @Override
    protected byte[] assignUser(byte[] requestBody) throws ExchangeException {
        mExchStartTimer = new Date(); // total timeout begins at first online
                                      // call
        return doPost(mUrlPrefix + mHost + "/assignUser" + mUrlSuffix, requestBody);
    }

    @Override
    protected byte[] syncUsers(byte[] requestBody) throws ExchangeException {
        return doPost(mUrlPrefix + mHost + "/syncUsers" + mUrlSuffix, requestBody);
    }

    @Override
    protected byte[] syncData(byte[] requestBody) throws ExchangeException {
        return doPost(mUrlPrefix + mHost + "/syncData" + mUrlSuffix, requestBody);
    }

    @Override
    protected byte[] syncSignatures(byte[] requestBody) throws ExchangeException {
        return doPost(mUrlPrefix + mHost + "/syncSignatures" + mUrlSuffix, requestBody);
    }

    @Override
    protected byte[] syncKeyNodes(byte[] requestBody) throws ExchangeException {
        return doPost(mUrlPrefix + mHost + "/syncKeyNodes" + mUrlSuffix, requestBody);
    }

    @Override
    protected byte[] syncMatch(byte[] requestBody) throws ExchangeException {
        return doPost(mUrlPrefix + mHost + "/syncMatch" + mUrlSuffix, requestBody);
    }
}
