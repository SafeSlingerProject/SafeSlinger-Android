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

package edu.cmu.cylab.starslinger.exchange;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class CheckedSSLSocketFactory implements LayeredSocketFactory {
    private SSLContext mSSLContext = null;

    private static SSLContext createEasySSLContext() throws IOException {
        KeyStore trusted = null;

        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {
                new CheckedX509TrustManager(trusted)
            }, null);
            return context;

        } catch (KeyManagementException e) {
            throw new IOException(e.getLocalizedMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e.getLocalizedMessage());
        } catch (KeyStoreException e) {
            throw new IOException(e.getLocalizedMessage());
        }
    }

    private SSLContext getSSLContext() throws IOException {
        if (mSSLContext == null) {
            mSSLContext = createEasySSLContext();
        }
        return mSSLContext;
    }

    @Override
    public Socket connectSocket(Socket sock, String host, int port, InetAddress localAddress,
            int localPort, HttpParams params)

    throws IOException, UnknownHostException, ConnectTimeoutException {
        int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
        int soTimeout = HttpConnectionParams.getSoTimeout(params);
        InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
        SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket());
        sslsock = limitEnabledCipherSuites(sslsock);

        if ((localAddress != null) || (localPort > 0)) {
            // we need to bind explicitly
            if (localPort < 0) {
                localPort = 0; // indicates "any"
            }
            InetSocketAddress isa = new InetSocketAddress(localAddress, localPort);
            sslsock.bind(isa);
        }

        sslsock.connect(remoteAddress, connTimeout);
        sslsock.setSoTimeout(soTimeout);
        return sslsock;
    }

    @Override
    public Socket createSocket() throws IOException {
        return getSSLContext().getSocketFactory().createSocket();
    }

    @Override
    public boolean isSecure(Socket socket) throws IllegalArgumentException {
        return true;
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    @Override
    public boolean equals(Object obj) {
        return ((obj != null) && obj.getClass().equals(CheckedSSLSocketFactory.class));
    }

    @Override
    public int hashCode() {
        return CheckedSSLSocketFactory.class.hashCode();
    }

    public static SSLSocket limitEnabledCipherSuites(SSLSocket sslEngine) {
        String[] supportedCipherSuites = sslEngine.getSupportedCipherSuites();
        List<String> favoredCipherSuites = new ArrayList<String>();

        /*
         * We don't want anonymous Diffie Hellman and no DES or 40 or 56 bit
         * keys and no null-md5 or null-sha.
         */
        String[] unwantedCipherSuites = new String[] {
                "_dh_anon", "_des", "_40", "_56", "_null_md5", "_null_sha"
        };
        for (String cs : supportedCipherSuites) {
            boolean isCSok = true;
            for (String ucs : unwantedCipherSuites)
                if (cs.toLowerCase(Locale.US).contains(ucs))
                    isCSok = false;

            if (isCSok)
                favoredCipherSuites.add(cs);
        }

        sslEngine.setEnabledCipherSuites(favoredCipherSuites.toArray(new String[favoredCipherSuites
                .size()]));
        return sslEngine;
    }
}
