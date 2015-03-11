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

package edu.cmu.cylab.starslinger.transaction;

import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

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
import android.net.TrafficStats;
import android.os.SystemClock;
import android.text.TextUtils;
import edu.cmu.cylab.starslinger.ExchangeException;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.exchange.CheckedHttpClient;

/**
 * This class does all of the TCP connection setup to the server and handles the
 * HTTP functions GET and POST. In addition to basic GET and POST, it also has
 * web_spate specific functions to get the group size, get the commitments,
 * create the group on the server, send data, ....
 */
public class WebEngine {
    private static final String TAG = SafeSlingerConfig.LOG_TAG;

    private String mUrlPrefix;
    private String mHost;
    private String mUrlSuffix;
    private boolean mCancelable = false;
    private int mVersion;
    private int mVersionLen = 4;
    private HttpClient mHttpClient;
    private long mTxTotalBytes;
    private long mTxStartBytes;
    private long mRxStartBytes;
    private long mTxCurrentBytes;
    private long mRxCurrentBytes;
    private int mLatestServerVersion = 0;
    private Context mCtx;

    private boolean mNotRegistered = false;

    public WebEngine(Context ctx, String hostName) {
        mCtx = ctx;
        mUrlPrefix = SafeSlingerConfig.HTTPURL_PREFIX;
        mHost = hostName;
        mUrlSuffix = SafeSlingerConfig.HTTPURL_SUFFIX;

        mVersion = SafeSlingerConfig.getVersionCode();
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

            mTxTotalBytes = requestBody.length;

            final long totalTxBytes = TrafficStats.getTotalTxBytes();
            final long totalRxBytes = TrafficStats.getTotalRxBytes();
            if (totalTxBytes != TrafficStats.UNSUPPORTED) {
                mTxStartBytes = totalTxBytes;
            }
            if (totalRxBytes != TrafficStats.UNSUPPORTED) {
                mRxStartBytes = totalRxBytes;
            }
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

    /***
     * put a push registration id on the server with a submission token so the
     * user of this key id can authenticate a change later if the registration
     * id they use for the key changes due to migrating to a new device, service
     * migration, registration expiration, others.
     */
    public byte[] postRegistration(String keyId, String submisisonToken, String senderPushRegId,
            int notifyType) throws ExchangeException, MessageNotFoundException {

        int capacity = mVersionLen //
                + 4 + keyId.length() //
                + 4 + submisisonToken.length() //
                + 4 + senderPushRegId.length() //
                + 4;
        ByteBuffer msg = ByteBuffer.allocate(capacity);
        msg.putInt(mVersion);
        msg.putInt(keyId.length());
        msg.put(keyId.getBytes());
        msg.putInt(submisisonToken.length());
        msg.put(submisisonToken.getBytes());
        msg.putInt(senderPushRegId.length());
        msg.put(senderPushRegId.getBytes());
        msg.putInt(notifyType);

        byte[] resp = doPost(mUrlPrefix + mHost + "/postRegistration" + mUrlSuffix, msg.array());

        resp = handleResponseExceptions(resp, 0);

        return resp;
    }

    /**
     * put a message, file, its meta-data, and retrieval id on the server
     * 
     * @throws MessageNotFoundException
     */
    public byte[] postMessage(byte[] msgHashBytes, byte[] msgData, byte[] fileData,
            String recipientPushRegId, int notifyType) throws ExchangeException,
            MessageNotFoundException {

        handleSizeRestictions(fileData);

        int capacity = mVersionLen //
                + 4 + msgHashBytes.length //
                + 4 + recipientPushRegId.length() //
                + 4 + msgData.length //
                + 4 + fileData.length //
                + 4;
        ByteBuffer msg = ByteBuffer.allocate(capacity);
        msg.putInt(mVersion);
        msg.putInt(msgHashBytes.length);
        msg.put(msgHashBytes);
        msg.putInt(recipientPushRegId.length());
        msg.put(recipientPushRegId.getBytes());
        msg.putInt(msgData.length);
        msg.put(msgData);
        msg.putInt(fileData.length);
        msg.put(fileData);
        msg.putInt(notifyType);

        mNotRegistered = false;

        byte[] resp = doPost(mUrlPrefix + mHost + "/postMessage" + mUrlSuffix, msg.array());

        mNotRegistered = isNotRegisteredErrorCodes(resp);

        resp = handleResponseExceptions(resp, 0);

        return resp;
    }

    /**
     * check for iOS activation
     * 
     * @throws MessageNotFoundException
     */
    public byte[] checkStatusAppleUA(String recipientToken) throws ExchangeException,
            MessageNotFoundException {

        int capacity = mVersionLen //
                + 4 + recipientToken.length() //
                + 4;
        ByteBuffer msg = ByteBuffer.allocate(capacity);
        msg.putInt(mVersion);
        msg.putInt(recipientToken.length());
        msg.put(recipientToken.getBytes());
        msg.putInt(SafeSlingerConfig.NOTIFY_APPLEUA);

        mNotRegistered = false;

        byte[] resp = doPost(mUrlPrefix + mHost + "/checkStatus" + mUrlSuffix, msg.array());

        mNotRegistered = isNotRegisteredErrorCodes(resp);

        resp = handleResponseExceptions(resp, 0);

        return resp;
    }

    /**
     * get a file, based on the retrieval id from the server
     * 
     * @throws MessageNotFoundException
     */
    public byte[] getFile(byte[] msgHashBytes) throws ExchangeException, MessageNotFoundException {

        ByteBuffer msg = ByteBuffer.allocate(mVersionLen //
                + 4 + msgHashBytes.length //
        );
        msg.putInt(mVersion);
        msg.putInt(msgHashBytes.length);
        msg.put(msgHashBytes);

        byte[] resp = doPost(mUrlPrefix + mHost + "/getFile" + mUrlSuffix, msg.array());

        resp = handleResponseExceptions(resp, 0);

        return resp;
    }

    /**
     * get a message meta-data, based on the retrieval id from the server
     * 
     * @throws MessageNotFoundException
     */
    public byte[] getMessage(byte[] msgHashBytes) throws ExchangeException,
            MessageNotFoundException {

        ByteBuffer msg = ByteBuffer.allocate(mVersionLen //
                + 4 + msgHashBytes.length //
        );
        msg.putInt(mVersion);
        msg.putInt(msgHashBytes.length);
        msg.put(msgHashBytes);

        byte[] resp = doPost(mUrlPrefix + mHost + "/getMessage" + mUrlSuffix, msg.array());

        resp = handleResponseExceptions(resp, 0);

        return resp;
    }

    private void handleMessagingErrorCodes(byte[] resp) throws ExchangeException,
            MessageNotFoundException {
        String checkError = new String(resp) + "";
        if (checkError.contains(C2DMessaging.ERRMSG_ERROR_PREFIX)) {
            if (checkError.contains(C2DMessaging.ERRMSG_QUOTA_EXCEEDED)) {
                throw new ExchangeException(mCtx.getString(R.string.error_PushMsgQuotaExceeded));
            } else if (checkError.contains(C2DMessaging.ERRMSG_DEVICE_QUOTA_EXCEEDED)) {
                throw new ExchangeException(
                        mCtx.getString(R.string.error_PushMsgDeviceQuotaExceeded));
            } else if (checkError.contains(C2DMessaging.ERRMSG_INVALID_REGISTRATION)) {
                throw new ExchangeException(
                        mCtx.getString(R.string.error_PushMsgInvalidRegistration));
            } else if (checkError.contains(C2DMessaging.ERRMSG_NOT_REGISTERED)) {
                throw new ExchangeException(mCtx.getString(R.string.error_PushMsgNotRegistered));
            } else if (checkError.contains(C2DMessaging.ERRMSG_MESSAGE_TOO_BIG)) {
                throw new ExchangeException(mCtx.getString(R.string.error_PushMsgMessageTooBig));
            } else if (checkError.contains(C2DMessaging.ERRMSG_MISSING_COLLAPSE_KEY)) {
                throw new ExchangeException(mCtx.getString(R.string.error_PushMsgNotSucceed));
            } else if (checkError.contains(C2DMessaging.ERRMSG_NOTIFCATION_FAIL)) {
                throw new ExchangeException(mCtx.getString(R.string.error_PushMsgNotSucceed));
            } else if (checkError.contains(C2DMessaging.ERRMSG_SERVICE_FAIL)) {
                throw new ExchangeException(mCtx.getString(R.string.error_PushMsgServiceFail));
            } else if (checkError.contains(C2DMessaging.ERRMSG_MESSAGE_NOT_FOUND)) {
                throw new MessageNotFoundException(
                        mCtx.getString(R.string.error_PushMsgMessageNotFound));
            }
        }
    }

    private boolean isNotRegisteredErrorCodes(byte[] resp) {
        String checkError = new String(resp) + "";
        if (checkError.contains(C2DMessaging.ERRMSG_ERROR_PREFIX)) {
            if (checkError.contains(C2DMessaging.ERRMSG_INVALID_REGISTRATION)) {
                return true;
            } else if (checkError.contains(C2DMessaging.ERRMSG_NOT_REGISTERED)) {
                return true;
            }
        }
        return false;
    }

    private byte[] handleResponseExceptions(byte[] resp, int errMax) throws ExchangeException,
            MessageNotFoundException {

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
                MyLog.e(TAG, "server error code: " + firstInt);

                // first, look for expected error string codes from 3rd-party
                handleMessagingErrorCodes(resp);

                // second, use message directly from server
                throw new ExchangeException(String.format(
                        mCtx.getString(R.string.error_ServerAppMessage), new String(bytes).trim()));
            }
            // else strip off server version
            mLatestServerVersion = firstInt;
            return bytes;
        }
    }

    private void handleSizeRestictions(byte[] fileData) throws ExchangeException {

        if (fileData.length > SafeSlingerConfig.MAX_FILEBYTES)
            throw new ExchangeException(String.format(
                    mCtx.getString(R.string.error_CannotSendFilesOver),
                    SafeSlingerConfig.MAX_FILEBYTES));

        return;
    }

    public boolean isCancelable() {
        return mCancelable;
    }

    public void setCancelable(boolean cancelable) {
        mCancelable = cancelable;
    }

    public long get_txTotalBytes() {
        return mTxTotalBytes;
    }

    public long get_txCurrentBytes() {
        final long totalTxBytes = TrafficStats.getTotalTxBytes();
        if (totalTxBytes != TrafficStats.UNSUPPORTED) {
            mTxCurrentBytes = totalTxBytes - mTxStartBytes;
        } else {
            mTxCurrentBytes = 0;
        }
        return mTxCurrentBytes;
    }

    public long get_rxCurrentBytes() {
        final long totalRxBytes = TrafficStats.getTotalRxBytes();
        if (totalRxBytes != TrafficStats.UNSUPPORTED) {
            mRxCurrentBytes = totalRxBytes - mRxStartBytes;
        } else {
            mRxCurrentBytes = 0;
        }
        return mRxCurrentBytes;
    }

    public static byte[] parseMessageResponse(byte[] resp) throws BufferUnderflowException {
        ByteBuffer buffer = ByteBuffer.wrap(resp);
        byte[] encfile = null;

        int errCode = buffer.getInt();
        if (errCode != 1)
            return null;

        int fileLen = buffer.getInt();
        encfile = new byte[fileLen];
        try {
            buffer.get(encfile);
            return encfile;
        } catch (BufferUnderflowException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isNotRegistered() {
        return mNotRegistered;
    }

    public int getLatestServerVersion() {
        return mLatestServerVersion;
    }
}
