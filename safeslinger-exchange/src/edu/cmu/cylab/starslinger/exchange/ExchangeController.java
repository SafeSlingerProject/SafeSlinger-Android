
package edu.cmu.cylab.starslinger.exchange;

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

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.BitSet;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.content.Context;
import android.util.Log;

public class ExchangeController {

    private static final String TAG = ExchangeConfig.LOG_TAG;
    private byte[] mMyData = null;
    private byte[] mNonceMatch;
    private byte[] mNonceWrong;
    private byte[] mHashHashMatch;
    private byte[] mHashMatch;
    private byte[] mHashWrong;
    private byte[] mCommitA;
    private byte[] mCommitB;
    private int mUsrId;
    private int mUsrIdLink;
    private int mNumUsers;
    private ConnectionEngine mConnect;
    private static SecureRandom mRandom = new SecureRandom();
    private byte[] mPackedData;
    private int[] mGroupIds;
    private GroupData mGrpInfo;
    private byte[] mHashVal;
    private int mLowestClientVersion;
    private String mErrMsg = null;
    private boolean mError = false;
    private int mNumUsersCommit = 0;
    private int mNumUsersData = 0;
    private int mNumUsersSigs = 0;
    private int mNumUsersKeyNodes = 0;
    private int mNumUsersMatchNonces = 0;
    private int mLatestServerVersion;
    private int mVersion;
    private Context mCtx;
    private GroupData mSigsInfo;
    private byte[] mDecoyHash1;
    private byte[] mDecoyHash2;
    private byte[] mDHSecretKey;
    private byte[] mDHHalfKey;
    private CryptoAccess mCrypto;
    private int mRandomPosSrc;
    private String mHost = null;
    private int mHashSelection;

    public ExchangeController(Context ctx) {
        mErrMsg = "";
        mCtx = ctx;

        mVersion = ExchangeConfig.getVersionCode();
        mLowestClientVersion = mVersion;

        mNonceMatch = new byte[ExchangeConfig.HASH_LEN];
        mNonceWrong = new byte[ExchangeConfig.HASH_LEN];
        mDecoyHash1 = new byte[3];
        mDecoyHash2 = new byte[3];
    }

    private boolean handleError(int resId) {
        mErrMsg = mCtx.getString(resId);
        mError = true;
        return false;
    }

    private boolean handleError(String msg) {
        mErrMsg = msg;
        mError = true;
        return false;
    }

    private boolean handleError(Exception e) {
        mErrMsg = e.getLocalizedMessage();
        mError = true;
        return false;
    }

    private static int fibonacci(int n) {
        if (n == 0) {
            return 0;
        } else if (n == 1) {
            return 1;
        } else {
            return fibonacci(n - 1) + fibonacci(n - 2);
        }
    }

    public void doSleepBackoff(int attempt, long intervalStart, long totalStart)
            throws InterruptedException {
        // backoff poll the server using Fibonacci sequence
        long now = System.currentTimeMillis();
        long diffInterval = now - intervalStart;
        long diffTotal = now - totalStart;
        long msBackoff = fibonacci(attempt) * 1000;
        if ((diffTotal + msBackoff) > ExchangeConfig.MSSVR_TIMEOUT) {
            // don't sleep past the max timeout
            msBackoff = ExchangeConfig.MSSVR_TIMEOUT - diffTotal;
        }
        if (diffInterval < msBackoff) {
            Thread.sleep(msBackoff - diffInterval);
        }
    }

    public boolean doInitialize() {
        mErrMsg = null;
        mError = false;

        mConnect = ConnectionEngine.getServerInstance(mCtx, mHost);

        mNumUsers = 0;
        mHashSelection = -1;

        mNumUsersCommit = 0;
        mNumUsersData = 0;
        mNumUsersSigs = 0;
        mNumUsersKeyNodes = 0;
        mNumUsersMatchNonces = 0;

        // Select a random user id to use to identify yourself when
        // talking with the web server.
        mUsrId = mRandom.nextInt(Integer.MAX_VALUE);

        return true;
    }

    public boolean doGenerateCommitment() {
        if (mMyData == null) {
            return handleError(R.string.error_NoDataToExchange);
        }

        try {
            // create nonces and hashes here since data may have been updated
            mRandom.nextBytes(mNonceMatch);
            mRandom.nextBytes(mNonceWrong);

            // encrypt the data with match nonce
            mCrypto = new CryptoAccess();
            mMyData = mCrypto.encryptData(mMyData, mNonceMatch);

            // based on those nonces, generate the various commitments
            mHashWrong = CryptoAccess.computeSha3Hash(mNonceWrong);
            mHashMatch = CryptoAccess.computeSha3Hash(mNonceMatch);
            mHashHashMatch = CryptoAccess.computeSha3Hash(mHashMatch);
            mCommitA = CryptoAccess.computeSha3Hash2(mHashHashMatch, mHashWrong);

            // generate DH half key for later encryption of match nonce
            mDHHalfKey = mCrypto.generateDHPublicKey();
            mCommitB = CryptoAccess.computeSha3Hash3(mCommitA, mDHHalfKey, mMyData);

        } catch (NoSuchAlgorithmException e) {
            return handleError(e);
        } catch (InvalidKeyException e) {
            return handleError(e);
        } catch (NoSuchPaddingException e) {
            return handleError(e);
        } catch (IllegalBlockSizeException e) {
            return handleError(e);
        } catch (BadPaddingException e) {
            return handleError(e);
        } catch (InvalidAlgorithmParameterException e) {
            return handleError(e);
        } catch (IllegalStateException e) {
            return handleError(e);
        }

        return true;
    }

    public boolean doRequestUserId() {
        if (mCommitB == null || mCommitB.length == 0) {
            return handleError(R.string.error_NoDataToExchange);
        }
        int id = 0;
        ByteBuffer res = null;
        try {
            res = ByteBuffer.wrap(mConnect.assign_user(mCommitB));
        } catch (ExchangeException e) {
            return handleError(e);
        }
        mLatestServerVersion = res.getInt();

        id = res.getInt();
        if (id > 0) {
            mUsrId = id;
            return true;
        }
        return handleError(R.string.error_ServerNotResponding);
    }

    private boolean syncCommitments() {
        if (mCommitB == null || mCommitB.length == 0) {
            return handleError(R.string.error_NoDataToExchange);
        }
        int[] usridList = null;
        byte[] commitList = null;
        ByteBuffer theirs = null;

        try {
            mNumUsersCommit = 0;
            mLowestClientVersion = mVersion;
            ByteBuffer ours = ByteBuffer.allocate(4 + 4 + 4 + mCommitB.length);
            ours.putInt(1).putInt(mUsrId).putInt(mCommitB.length).put(mCommitB);

            // add just our own to start
            mNumUsersCommit = 1;
            usridList = appendServerUserIds(usridList, ours.array());
            commitList = appendServerBytes(commitList, ours.array());

            int commitRet = 0;
            boolean postCommit = true;
            long getCommitWait = System.currentTimeMillis();
            int attempt = 0;
            while (commitRet == 0) {
                if (isCanceled()) {
                    return false;
                }

                long intervalStart = System.currentTimeMillis();
                attempt++;

                // get what is on the server now and create a new group
                // this should be a bunch of signatures
                theirs = ByteBuffer.wrap(mConnect.sync_commits(mUsrId, mUsrIdLink, usridList,
                        (postCommit ? mCommitB : new byte[0])));
                postCommit = false; // done!

                // add updates
                int offset = 0;
                mLatestServerVersion = theirs.getInt();
                mLowestClientVersion = theirs.getInt(); // pull out version
                offset += 8;

                byte tmpBuf[] = new byte[theirs.limit() - offset];
                mNumUsersCommit = theirs.getInt(); // pull out grand total
                offset += 4;
                tmpBuf = new byte[theirs.limit() - offset];
                theirs.get(tmpBuf, 0, theirs.remaining());
                if (mNumUsersCommit > 0) {
                    usridList = appendServerUserIds(usridList, tmpBuf);
                    commitList = appendServerBytes(commitList, tmpBuf);

                    if (mNumUsersCommit > mNumUsers) {
                        return handleError(R.string.error_MoreDataThanUsers);
                    } else if (mNumUsersCommit == mNumUsers) {
                        commitRet = 1;
                    }
                }

                // make sure we aren't waiting forever
                if ((System.currentTimeMillis() - getCommitWait) > ExchangeConfig.MSSVR_TIMEOUT) {
                    return handleError(R.string.error_TimeoutWaitingForAllMembers);
                }

                if (commitRet == 0) {
                    doSleepBackoff(attempt, intervalStart, getCommitWait);
                }
            }

            mPackedData = commitList;

        } catch (ExchangeException e) {
            return handleError(e);
        } catch (InterruptedException e) {
            return handleError(e);
        }
        return true;
    }

    private boolean syncData() {
        int[] usridList = null;
        byte[] dataList = null;
        ByteBuffer theirs = null;
        mNumUsersData = 0;
        byte[] data;

        try {
            ByteBuffer join = ByteBuffer.allocate(mCommitA.length + mDHHalfKey.length
                    + mMyData.length);
            join.put(mCommitA);
            join.put(mDHHalfKey);
            join.put(mMyData);
            data = join.array();

            ByteBuffer ours = ByteBuffer.allocate(4 + 4 + 4 + data.length);
            ours.putInt(1).putInt(mUsrId).putInt(data.length).put(data);

            // add just our own to start
            mNumUsersData = 1;
            usridList = appendServerUserIds(usridList, ours.array());
            dataList = appendServerBytes(dataList, ours.array());

            int dataRet = 0;
            boolean postData = true;
            long getDataWait = System.currentTimeMillis();
            int attempt = 0;
            while (dataRet == 0) {
                if (isCanceled()) {
                    return false;
                }

                long intervalStart = System.currentTimeMillis();
                attempt++;

                // get what is on the server now and create a new group
                // this should be a bunch of signatures
                try {
                    theirs = ByteBuffer.wrap(mConnect.sync_data(mUsrId, usridList, (postData ? data
                            : new byte[0])));
                } catch (ExchangeException e) {
                    return handleError(e);
                }
                postData = false; // done!

                // add updates
                int offset = 0;
                mLatestServerVersion = theirs.getInt();
                offset += 4;

                mNumUsersData = theirs.getInt(); // pull out grand total
                offset += 4;
                byte tmpBuf[] = new byte[theirs.limit() - offset];
                theirs.get(tmpBuf, 0, theirs.remaining());
                if (mNumUsersData > 0) {
                    usridList = appendServerUserIds(usridList, tmpBuf);
                    dataList = appendServerBytes(dataList, tmpBuf);

                    if (mNumUsersData > mNumUsers) {
                        return handleError(R.string.error_MoreDataThanUsers);
                    } else if (mNumUsersData == mNumUsers) {
                        dataRet = 1;
                    }
                }

                // make sure we aren't waiting forever
                if ((System.currentTimeMillis() - getDataWait) > ExchangeConfig.MSSVR_TIMEOUT) {
                    return handleError(R.string.error_TimeoutWaitingForAllMembers);
                }

                if (dataRet == 0) {
                    doSleepBackoff(attempt, intervalStart, getDataWait);
                }
            }

            boolean match = false;
            int lowest = Integer.MAX_VALUE;
            for (int i = 0; i < usridList.length; i++) {
                if (usridList[i] == mUsrIdLink)
                    match = true; // record match
                if (usridList[i] < lowest)
                    lowest = usridList[i]; // record lowest
            }
            if (!match)
                return handleError(mCtx.getString(R.string.error_MoreDataThanUsers));
            if (lowest != mUsrIdLink)
                return handleError(mCtx.getString(R.string.error_MoreDataThanUsers));

            mPackedData = dataList;

        } catch (InterruptedException e) {
            return handleError(e);
        }
        return true;
    }

    public boolean doGetCommitmentsGetData() {

        // commitment start
        // .................................................................
        if (!syncCommitments())
            return false;
        // .................................................................
        // commitment end

        // ensure all are using new SHA-3
        if (mLowestClientVersion < ExchangeConfig.VER_SHA3) {
            return handleError(String.format(mCtx.getString(R.string.error_AllMembersMustUpgrade),
                    "1.6"));
        }

        mGrpInfo = new GroupData(mNumUsers);
        if (mGrpInfo.save_ID_data(mPackedData) != 0) {
            return false;
        }

        // data start
        // .................................................................
        if (!syncData())
            return false;
        // data end
        // .................................................................

        // again save the data in a new group info in case one of the signatures
        // is invalid
        GroupData newInfo = new GroupData(mNumUsers);
        if (newInfo.save_ID_data(mPackedData) != 0) {
            return false;
        }

        int retVal = mGrpInfo.isDecommitUpdate(newInfo);

        if (retVal < 0) {
            Log.e(TAG, String.valueOf(retVal));
            return handleError(R.string.error_InvalidCommitVerify);
        }

        // by now the return value should be 0, i.e., the data is correct
        mGrpInfo.save_data(mPackedData);

        // get the hash of the data to generate the T-Flag
        mHashVal = mGrpInfo.getHash();

        // establish decoy hashes for all users
        if (!assignDecoys(mHashVal)) {
            return false;
        }

        return true;
    }

    public boolean doSendInvalidSignature() {
        // send the no signature and quit
        ByteBuffer sig = ByteBuffer.allocate(ExchangeConfig.HASH_LEN + ExchangeConfig.HASH_LEN);
        sig.put(mHashHashMatch).put(mNonceWrong);

        int[] usridList = null;
        byte[] sigsList = null;
        ByteBuffer theirs = null;
        mNumUsersSigs = 0;

        try {
            ByteBuffer ours = ByteBuffer.allocate(12 + sig.capacity());
            ours.putInt(1).putInt(mUsrId).putInt(sig.capacity()).put(sig.array());

            // add just our own...
            mNumUsersSigs = 1;
            usridList = appendServerUserIds(usridList, ours.array());
            sigsList = appendServerBytes(sigsList, ours.array());

            int dataRet = 0;
            long getSigsWait = System.currentTimeMillis();
            int attempt = 0;
            while (dataRet == 0) {
                if (isCanceled()) {
                    return false;
                }

                long intervalStart = System.currentTimeMillis();
                attempt++;

                // get what is on the server now and create a new group
                // this should be a bunch of signatures
                theirs = ByteBuffer.wrap(mConnect.sync_signatures(mUsrId, usridList, sig.array()));

                // add updates
                mLatestServerVersion = theirs.getInt();
                mNumUsersSigs = theirs.getInt(); // pull out grand total

                if (mNumUsersSigs > 0) {
                    dataRet = 1;
                }

                // make sure we aren't waiting forever
                if ((System.currentTimeMillis() - getSigsWait) > ExchangeConfig.MSSVR_TIMEOUT) {
                    return handleError(R.string.error_TimeoutWaitingForAllMembers);
                }

                if (dataRet == 0) {
                    doSleepBackoff(attempt, intervalStart, getSigsWait);
                }
            }

        } catch (ExchangeException e) {
            return handleError(e);
        } catch (InterruptedException e) {
            return handleError(e);
        }
        return handleError(R.string.error_LocalGroupCommitDiffer);
    }

    private boolean syncSigs(byte[] sig) {
        int[] usridList = null;
        byte[] sigsList = null;
        ByteBuffer theirs = null;
        mNumUsersSigs = 0;

        try {
            ByteBuffer ours = ByteBuffer.allocate(12 + sig.length);
            ours.putInt(1).putInt(mUsrId).putInt(sig.length).put(sig);

            // add just our own to start
            mNumUsersSigs = 1;
            usridList = appendServerUserIds(usridList, ours.array());
            sigsList = appendServerBytes(sigsList, ours.array());

            int dataRet = 0;
            boolean postSig = true;
            long getSigsWait = System.currentTimeMillis();
            int attempt = 0;
            while (dataRet == 0) {
                if (isCanceled()) {
                    return false;
                }

                long intervalStart = System.currentTimeMillis();
                attempt++;

                // get what is on the server now and create a new group
                // this should be a bunch of signatures
                theirs = ByteBuffer.wrap(mConnect.sync_signatures(mUsrId, usridList, postSig ? sig
                        : new byte[0]));
                postSig = false; // done!

                // add updates
                int offset = 0;
                mLatestServerVersion = theirs.getInt();
                offset += 4;

                mNumUsersSigs = theirs.getInt(); // pull out grand total
                offset += 4;
                byte tmpBuf[] = new byte[theirs.limit() - offset];
                theirs.get(tmpBuf, 0, theirs.remaining());
                if (mNumUsersSigs > 0) {
                    usridList = appendServerUserIds(usridList, tmpBuf);
                    sigsList = appendServerBytes(sigsList, tmpBuf);

                    if (mNumUsersSigs > mNumUsers) {
                        return handleError(R.string.error_MoreDataThanUsers);
                    } else if (mNumUsersSigs == mNumUsers) {
                        dataRet = 1;
                    }
                }

                // make sure we aren't waiting forever
                if ((System.currentTimeMillis() - getSigsWait) > ExchangeConfig.MSSVR_TIMEOUT) {
                    return handleError(R.string.error_TimeoutWaitingForAllMembers);
                }

                if (dataRet == 0) {
                    doSleepBackoff(attempt, intervalStart, getSigsWait);
                }
            }

            mPackedData = sigsList;

        } catch (InterruptedException e) {
            return handleError(e);
        } catch (ExchangeException e) {
            return handleError(e);
        }
        return true;
    }

    /**
     * begin computing nodes for a asymmetric binary public key tree of
     * diffie-hellman values
     */
    private boolean syncHalfKeysAndGenerateSecretKey() {

        ByteBuffer ours = null;
        mNumUsersKeyNodes = 0;
        byte[] pub = null;
        byte[][] excgHalfKeys = mGrpInfo.sortAllHalfKeys();
        int[] orderedIDs = mGrpInfo.getOrderedIDs();
        int pos = -1;

        for (int i = 0; i < orderedIDs.length; i++) {
            if (orderedIDs[i] == mUsrId) {
                pos = i;
                break;
            }
        }

        try {
            // assign pub when A or B
            if (pos < 2) {
                pub = excgHalfKeys[pos == 0 ? 1 : 0];
            }

            int curNodePos = 2;
            byte mynode[] = null;
            long getKeyNodesWait = System.currentTimeMillis();
            int attempt = 0;
            while (curNodePos < mNumUsers) {
                if (isCanceled()) {
                    return false;
                }

                long intervalStart = System.currentTimeMillis();
                attempt++;

                // can calculate node? then calc node.
                if (pos < 2 || mynode != null) {
                    // node = getnode(pub)
                    pub = mCrypto.createNodeKey(pub);
                }

                // can send node? then send node.
                if (pos < 2) {
                    // send(node)
                    ours = ByteBuffer.wrap(mConnect
                            .put_keynode(mUsrId, orderedIDs[curNodePos], pub));
                    mLatestServerVersion = ours.getInt();
                }

                // can recv mynode? then recv node.
                if (pos >= 2 && mynode == null) {
                    // mynode = recv()
                    ours = ByteBuffer.wrap(mConnect.get_keynode(mUsrId));

                    int offset = 0;
                    mLatestServerVersion = ours.getInt();
                    offset += 4;
                    mNumUsersKeyNodes = ours.getInt(); // grand total
                    offset += 4;
                    if (mNumUsersKeyNodes == 1) {
                        ours.getInt();
                        offset += 4;
                        mynode = new byte[ours.limit() - offset];
                        ours.get(mynode, 0, ours.remaining());

                        // mynode ok? then pub = mynode
                        if (mynode != null) {
                            curNodePos = pos + 1;
                            pub = mynode;
                        }
                    }
                }
                // can assign pub?
                else {
                    pub = excgHalfKeys[curNodePos];
                    curNodePos++;
                }

                // make sure we aren't waiting forever
                if ((System.currentTimeMillis() - getKeyNodesWait) > ExchangeConfig.MSSVR_TIMEOUT) {
                    return handleError(R.string.error_TimeoutWaitingForAllMembers);
                }

                // "get" should poll with exponential backoff, "put" should post
                // immediately, not wait...
                if (pos >= 2 || mynode == null) {
                    doSleepBackoff(attempt, intervalStart, getKeyNodesWait);
                }
            }

            // secret=getsecret(pub)
            mDHSecretKey = mCrypto.createFinalKey(pub);

        } catch (ExchangeException e) {
            return handleError(e);
        } catch (InvalidKeyException e) {
            return handleError(e);
        } catch (InvalidKeySpecException e) {
            return handleError(e);
        } catch (NoSuchAlgorithmException e) {
            return handleError(e);
        } catch (IllegalStateException e) {
            return handleError(e);
        } catch (InterruptedException e) {
            return handleError(e);
        }

        return true;
    }

    private boolean syncMatchNonce(byte[] nonceData) {
        int[] usridList = null;
        byte[] nonceList = null;
        ByteBuffer theirs = null;
        mNumUsersMatchNonces = 0;

        try {
            ByteBuffer ours = ByteBuffer.allocate(12 + nonceData.length);
            ours.putInt(1).putInt(mUsrId).putInt(nonceData.length).put(nonceData);

            // add just our own to start
            mNumUsersMatchNonces = 1;
            usridList = appendServerUserIds(usridList, ours.array());
            nonceList = appendServerBytes(nonceList, ours.array());

            int dataRet = 0;
            boolean postNonce = true;
            long getMatchNoncesWait = System.currentTimeMillis();
            int attempt = 0;
            while (dataRet == 0) {
                if (isCanceled()) {
                    return false;
                }

                long intervalStart = System.currentTimeMillis();
                attempt++;

                // get what is on the server now and create a new group
                // this should be a bunch of signatures
                theirs = ByteBuffer.wrap(mConnect.sync_match(mUsrId, usridList,
                        postNonce ? nonceData : new byte[0]));
                postNonce = false; // done!

                // add updates
                int offset = 0;
                mLatestServerVersion = theirs.getInt();
                offset += 4;

                mNumUsersMatchNonces = theirs.getInt(); // pull out grand total
                offset += 4;
                byte tmpBuf[] = new byte[theirs.limit() - offset];
                theirs.get(tmpBuf, 0, theirs.remaining());
                if (mNumUsersMatchNonces > 0) {
                    usridList = appendServerUserIds(usridList, tmpBuf);
                    nonceList = appendServerBytes(nonceList, tmpBuf);

                    if (mNumUsersMatchNonces > mNumUsers) {
                        return handleError(R.string.error_MoreDataThanUsers);
                    } else if (mNumUsersMatchNonces == mNumUsers) {
                        dataRet = 1;
                    }
                }

                // make sure we aren't waiting forever
                if ((System.currentTimeMillis() - getMatchNoncesWait) > ExchangeConfig.MSSVR_TIMEOUT) {
                    return handleError(R.string.error_TimeoutWaitingForAllMembers);
                }

                if (dataRet == 0) {
                    doSleepBackoff(attempt, intervalStart, getMatchNoncesWait);
                }
            }

            mPackedData = nonceList;

        } catch (ExchangeException e) {
            return handleError(e);
        } catch (InterruptedException e) {
            return handleError(e);
        }
        return true;
    }

    public boolean doSendValidSignatureGetSignatures() {

        // you say the hashes match so send the match signature
        ByteBuffer sig = ByteBuffer.allocate(ExchangeConfig.HASH_LEN + ExchangeConfig.HASH_LEN);
        sig.put(mHashMatch).put(mHashWrong);

        // sigs start
        // .................................................................
        if (!syncSigs(sig.array()))
            return false;
        // sigs end
        // .................................................................

        // again save the data in a new group info in case one of the
        // signatures
        // is invalid
        mSigsInfo = new GroupData(mNumUsers);
        if (mSigsInfo.save_ID_data(mPackedData) != 0) {
            return false;
        }

        int retVal = mGrpInfo.isSignatureUpdate(mSigsInfo);

        // we got a "wrong" signature so quit
        if (retVal == 2) {
            return handleError(R.string.error_OtherGroupCommitDiffer);
        }

        // there was an error so give up
        if (retVal < 0) {
            Log.e(TAG, String.valueOf(retVal));
            return handleError(R.string.error_InvalidCommitVerify);
        }

        return true;
    }

    public boolean doCreateSharedSecretGetNodesAndMatchNonces() {
        try {

            // key node start
            // .................................................................
            // true);
            if (!syncHalfKeysAndGenerateSecretKey())
                return false;

            // key node end
            // .................................................................

            // encrypt nonce with shared secret
            byte[] nonceData = null;
            nonceData = mCrypto.encryptNonce(mNonceMatch, mDHSecretKey);

            // nonce start
            // .................................................................
            if (!syncMatchNonce(nonceData))
                return false;
            // nonce end
            // .................................................................

            // decrypt nonce from all other members
            GroupData newInfo = new GroupData(mNumUsers);
            GroupData newInfoEnc = new GroupData(mNumUsers);
            if (newInfoEnc.save_ID_data(mPackedData) != 0) {
                return false;
            }

            mPackedData = decryptNonces(newInfoEnc);

            // again save the data in a new group info in case one of the
            // signatures is invalid
            if (newInfo.save_ID_data(mPackedData) != 0) {
                return false;
            }

            int retVal = mSigsInfo.isDecommitUpdate(newInfo);

            if (retVal < 0) {
                Log.e(TAG, String.valueOf(retVal));
                return handleError(R.string.error_InvalidCommitVerify);
            }

            // by now the return value should be 0, i.e., the data is correct
            mSigsInfo.save_data(mPackedData);

        } catch (NoSuchAlgorithmException e) {
            return handleError(e);
        } catch (InvalidKeyException e) {
            return handleError(e);
        } catch (NoSuchPaddingException e) {
            return handleError(e);
        } catch (IllegalBlockSizeException e) {
            return handleError(e);
        } catch (BadPaddingException e) {
            return handleError(e);
        } catch (InvalidAlgorithmParameterException e) {
            return handleError(e);
        }
        return true;
    }

    private static byte[] appendServerBytes(byte[] dest, byte[] src) {

        if (dest == null && src == null)
            return null;
        else if (dest == null)
            return src;
        else if (src == null)
            return dest;

        // pull out lengths, add, then reassemble
        ByteBuffer dBuf = ByteBuffer.wrap(dest);
        ByteBuffer sBuf = ByteBuffer.wrap(src);
        int dlen = dBuf.getInt();
        int slen = sBuf.getInt();
        int len = dlen + slen;

        if (len > dlen) {
            byte deBuf[] = new byte[dBuf.limit() - 4];
            byte srBuf[] = new byte[sBuf.limit() - 4];
            dBuf.get(deBuf, 0, dBuf.remaining());
            sBuf.get(srBuf, 0, sBuf.remaining());

            byte[] list = ByteBuffer.allocate(deBuf.length + srBuf.length).put(deBuf).put(srBuf)
                    .array();

            return ByteBuffer.allocate(4 + list.length).putInt(len).put(list).array();
        }
        return dest;
    }

    private static int[] appendServerUserIds(int[] dest, byte[] src) {

        if (dest == null && src == null)
            return null;
        else if (dest == null && src != null)
            dest = new int[0];
        else if (dest != null && src == null)
            return dest;

        if (dest == null)
            dest = new int[0];

        // pull out usrids, add to list, return new list
        ByteBuffer sBuf = ByteBuffer.wrap(src);
        int len = dest.length + sBuf.getInt();

        if (len > dest.length) {
            int[] users = new int[len];
            for (int i = 0; i < users.length; i++) {
                if (i < dest.length)
                    users[i] = dest[i];
                else {
                    users[i] = sBuf.getInt();
                    int sizeData = sBuf.getInt();
                    if (sizeData < 0)
                        return null;
                    sBuf.get(new byte[sizeData], 0, sizeData);
                }
            }
            return users;
        }
        return dest;
    }

    public byte[][] decryptMemData(byte[][] encryptMemData, int thisUserId)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        byte[][] decryptMemData = new byte[encryptMemData.length][];
        byte[][] sigsMatchData = mSigsInfo.sortOthersMatchNonce(thisUserId);
        for (int i = 0; i < sigsMatchData.length; i++) {
            byte[] key = sigsMatchData[i];
            decryptMemData[i] = mCrypto.decryptData(encryptMemData[i], key);
        }
        return decryptMemData;
    }

    private byte[] decryptNonces(GroupData newInfoEnc) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, InvalidAlgorithmParameterException {
        byte[] decryptedList = null;
        int[] orderedIds = newInfoEnc.getOrderedIDs();
        byte[][] encNonces = newInfoEnc.sortAllMatchNonce();
        for (int i = 0; i < encNonces.length; i++) {
            byte[] decNonce = mCrypto.decryptNonce(encNonces[i], mDHSecretKey);
            ByteBuffer decrypted = ByteBuffer.allocate(12 + decNonce.length).putInt(1)
                    .putInt(orderedIds[i]).putInt(decNonce.length).put(decNonce);
            decryptedList = appendServerBytes(decryptedList, decrypted.array());
        }
        return decryptedList;
    }

    /**
     * use hash value as a seed to walk hashes and assign non-colliding decoy
     * word lists
     */
    public boolean assignDecoys(byte[] hashVal) {

        BitSet even = new BitSet(WordList.wordList.length);
        BitSet odd = new BitSet(WordList.wordList.length);

        // add existing words in use from matching hash to bit vector
        even.set(WordList.btoi(hashVal[0]));
        odd.set(WordList.btoi(hashVal[1]));
        even.set(WordList.btoi(hashVal[2]));

        int[] orderedIDs = mGrpInfo.getOrderedIDs();
        boolean foundUser = false;

        // compute decoy lists for all users, until we get to ours
        for (int n = 0; n < mNumUsers; n++) {

            if (orderedIDs[n] == mUsrId)
                foundUser = true;

            // compute 2 decoy lists for each user

            // pick words that do not collide with others in the bit vector
            // also assure that we correctly seek back to the first byte if
            // collisions exceed the maximum byte value
            byte[] newHash = CryptoAccess.computeSha3Hash2(new byte[] {
                (byte) n
            }, hashVal);
            // decoy 1
            mDecoyHash1[0] = getNextClearByte(even, newHash[0]);
            even.set(WordList.btoi(mDecoyHash1[0]));
            mDecoyHash1[1] = getNextClearByte(odd, newHash[1]);
            odd.set(WordList.btoi(mDecoyHash1[1]));
            mDecoyHash1[2] = getNextClearByte(even, newHash[2]);
            even.set(WordList.btoi(mDecoyHash1[2]));

            // decoy 2
            mDecoyHash2[0] = getNextClearByte(even, newHash[3]);
            even.set(WordList.btoi(mDecoyHash2[0]));
            mDecoyHash2[1] = getNextClearByte(odd, newHash[4]);
            odd.set(WordList.btoi(mDecoyHash2[1]));
            mDecoyHash2[2] = getNextClearByte(even, newHash[5]);
            even.set(WordList.btoi(mDecoyHash2[2]));

            // last assigned decoy lists will always belong to this user
            if (foundUser)
                return true;
        }
        return false;
    }

    private static byte getNextClearByte(BitSet bits, byte start) {
        int next = bits.nextClearBit(WordList.btoi(start));
        if (next >= WordList.wordList.length)
            next = bits.nextClearBit(0);
        return WordList.itob(next);
    }

    public byte[] getDecoyHash(int decoyNum) {
        if (decoyNum == 1)
            return mDecoyHash1;
        else if (decoyNum == 2)
            return mDecoyHash2;
        else
            return null;
    }

    public String getErrorMsg() {
        return mErrMsg;
    }

    public byte[] getHash() {
        return mHashVal;
    }

    public int getNumUsers() {
        return mNumUsers;
    }

    public void setNumUsers(int numUsers) {
        mNumUsers = numUsers;
    }

    public int getUserId() {
        return mUsrId;
    }

    public boolean isError() {
        return mError;
    }

    public void setData(byte[] data) {
        mMyData = data;
    }

    public byte[] getData() {
        return mMyData;
    }

    public GroupData getGroupData() {
        return mGrpInfo;
    }

    public void setError(String msg) {
        handleError(msg);
    }

    public int[] getGroupIds() {
        return mGroupIds;
    }

    public void setUserIdLink(int usridlink) {
        mUsrIdLink = usridlink;
    }

    public int getUserIdLink() {
        return mUsrIdLink;
    }

    public int getNumUsersCommit() {
        return mNumUsersCommit;
    }

    public int getNumUsersData() {
        return mNumUsersData;
    }

    public int getNumUsersSigs() {
        return mNumUsersSigs;
    }

    public int getNumUsersKeyNodes() {
        return mNumUsersKeyNodes;
    }

    public int getNumUsersMatchNonces() {
        return mNumUsersMatchNonces;
    }

    public void cancelProtocol() {
        if (mConnect != null) {
            mConnect.setCancelable(true);
        }
    }

    public boolean isCanceled() {
        if (mConnect != null) {
            return mConnect.isCancelable();
        } else {
            return true;
        }
    }

    public void endProtocol() {
        if (mConnect != null) {
            mConnect.shutdownConnection();
        }
    }

    public int getRandomPos(int n) {
        byte[] b = new byte[1];
        mRandom.nextBytes(b);
        mRandomPosSrc = WordList.btoi(b[0]);
        double d = mRandomPosSrc / 256.0;
        double e = d * n;
        int floor = (int) Math.floor(e);
        return floor;
    }

    public int getRandomPosSrc() {
        return mRandomPosSrc;
    }

    public int getLatestServerVersion() {
        return mLatestServerVersion;
    }

    public void setHostName(String hostName) {
        mHost = hostName;
    }

    public String getHostName() {
        return mHost;
    }

    public String getStatusBanner(Context ctx) {
        StringBuilder banner = new StringBuilder();
        if (mHashVal != null) {
            byte[] selectedHash = new byte[3];
            if (mHashSelection == 0) {
                selectedHash = mHashVal;
            } else if (mHashSelection == 1) {
                selectedHash = mDecoyHash1;
            } else if (mHashSelection == 2) {
                selectedHash = mDecoyHash2;
            }
            boolean english = Locale.getDefault().getLanguage().equals("en");
            if (english) {
                banner.append(WordList.getWordList(selectedHash, 3)).append("\n")
                        .append(WordList.getNumbersList(selectedHash, 3));
            } else {
                banner.append(WordList.getNumbersList(selectedHash, 3)).append("\n")
                        .append(WordList.getWordList(selectedHash, 3));
            }
        } else if (mNumUsers > 0) {
            banner.append(String.format(ctx.getString(R.string.choice_NumUsers), mNumUsers));
            if (mUsrIdLink > 0) {
                banner.append(", ").append(ctx.getString(R.string.label_UserIdHint).toLowerCase())
                        .append(" ").append(mUsrIdLink);
            }
        }
        return banner.toString();
    }

    public long getExchStartTimeMs() {
        if (mConnect != null && mConnect.getExchStartTimer() != null) {
            return mConnect.getExchStartTimer().getTime();
        } else {
            return 0;
        }
    }

    public void setHashSelection(int hashSelection) {
        mHashSelection = hashSelection;
    }
}
