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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class CheckedX509TrustManager implements X509TrustManager {
    private X509TrustManager mStandardTrustManager = null;

    public CheckedX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException,
            KeyStoreException {
        super();
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory
                .getDefaultAlgorithm());
        factory.init(keystore);
        TrustManager[] trustmanagers = factory.getTrustManagers();
        if (trustmanagers.length == 0) {
            throw new NoSuchAlgorithmException("no trust manager found");
        }
        mStandardTrustManager = (X509TrustManager) trustmanagers[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType)
            throws CertificateException {
        mStandardTrustManager.checkClientTrusted(certificates, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType)
            throws CertificateException {

        int chainLength = certificates.length;
        if (certificates.length > 1) {
            // clean the received certificates chain
            int currIndex;
            for (currIndex = 0; currIndex < certificates.length; ++currIndex) {
                boolean foundNext = false;
                for (int nextIndex = currIndex + 1; nextIndex < certificates.length; ++nextIndex) {
                    if (certificates[currIndex].getIssuerDN().equals(
                            certificates[nextIndex].getSubjectDN())) {
                        foundNext = true;
                        // Exchange certificates so that 0 through currIndex + 1
                        // are in proper order
                        if (nextIndex != currIndex + 1) {
                            X509Certificate tempCertificate = certificates[nextIndex];
                            certificates[nextIndex] = certificates[currIndex + 1];
                            certificates[currIndex + 1] = tempCertificate;
                        }
                        break;
                    }
                }
                if (!foundNext)
                    break;
            }

            // examine if the last traced certificate is self issued and it
            // is expired
            chainLength = currIndex + 1;
            X509Certificate lastCertificate = certificates[chainLength - 1];
            Date now = new Date();
            if (lastCertificate.getSubjectDN().equals(lastCertificate.getIssuerDN())
                    && now.after(lastCertificate.getNotAfter())) {
                --chainLength;
            }
        }

        mStandardTrustManager.checkServerTrusted(certificates, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return mStandardTrustManager.getAcceptedIssuers();
    }
}
