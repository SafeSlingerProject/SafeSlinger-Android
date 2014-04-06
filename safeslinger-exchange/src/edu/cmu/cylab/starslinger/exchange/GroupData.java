
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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

/**
 * This class handles managing the data blobs the server sends back to the
 * client. This includes parsing the data, checking commitments, calculating
 * hashes (normal binary data and hex strings), and writing the data to files.
 */
public class GroupData implements Comparable<GroupData> {

    protected int mGrpSize;
    protected int[] mUsrIds;
    protected byte[][] mGrpData;

    public int grpSize() {
        return mGrpSize;
    }

    public int[] usrIDs() {
        return mUsrIds;
    }

    public byte[][] grpData() {
        return mGrpData;
    }

    public GroupData(int numMem) {
        mGrpSize = numMem;
        mUsrIds = null;
        mGrpData = null;
    }

    public GroupData() {
        mGrpSize = 0;
        mUsrIds = null;
        mGrpData = null;
    }

    /**
     * Save all of the packedInfo to the internal structures.
     * 
     * @param packedInfo should be of the format:
     *            numEntry||user1id||user1Len||user1Data
     *            ||user2id||user2len||user2Data... userXlen is the length of
     *            the data in userXData.
     */
    public int save_ID_data(byte[] packedInfo) {

        ByteBuffer buffer = ByteBuffer.wrap(packedInfo);

        int numEntry = buffer.getInt();
        if (numEntry > mGrpSize)
            return -1;

        // len gives us the number of entries in the usrID and grpData lists
        // append enough entries to hold the expected data
        mUsrIds = new int[numEntry];
        mGrpData = new byte[numEntry][];

        // future versions should do some checking to ensure
        // enough bytes are present and anything else smart
        try {
            int currEntry = 0;
            while (currEntry < numEntry) {
                mUsrIds[currEntry] = buffer.getInt();
                int sizeData = buffer.getInt();
                if (sizeData < 0)
                    return -1;
                mGrpData[currEntry] = new byte[sizeData];
                buffer.get(mGrpData[currEntry], 0, sizeData);
                currEntry = currEntry + 1;
            }
        } catch (BufferUnderflowException e) {
            return -1;
        } catch (IndexOutOfBoundsException e) {
            return -1;
        }

        return 0;
    }

    /**
     * Save the packedInfo to the internal structure, but here we assume it is
     * an update such that the userIDs already exist.
     */
    public int save_data(byte[] packedInfo) {
        ByteBuffer buffer = ByteBuffer.wrap(packedInfo);

        int numEntry = buffer.getInt();
        if (numEntry != mGrpSize)
            return -1;

        // now we're updating data, so return error if not enough spaces
        if ((numEntry > mUsrIds.length) || (numEntry > mGrpData.length))
            return -1;

        // future versions should do some checking to ensure
        // enough bytes are present and anything else smart
        try {
            int currEntry = 0;
            while (currEntry < numEntry) {
                int currID = buffer.getInt();
                int sizeData = buffer.getInt();
                if (sizeData < 0)
                    return -1;

                int position = currEntry;
                // find the appropriate usrID
                // assume server always sends them in the same order,
                // but can be wrong
                if (mUsrIds[position] != currID) {
                    int i = 0;
                    while (i < numEntry) {
                        if (mUsrIds[i] == currID) {
                            position = i;
                            i = numEntry;
                        }
                        i = i + 1;
                    }
                }
                mGrpData[position] = new byte[sizeData];
                buffer.get(mGrpData[position], 0, sizeData);
                currEntry = currEntry + 1;
            }
        } catch (BufferUnderflowException e) {
            return -1;
        } catch (IndexOutOfBoundsException e) {
            return -1;
        }

        return 0;
    }

    /**
     * This function checks if the other is the proper decommitment for this
     * group's data. IMPORTANT: hash(other.grpData) == self.grpData.
     * 
     * @return 0: all is well, 1: still waiting for data, -1: one of the hashes
     *         is incorrect, -2: the group has more data, -3: self has a user
     *         missing from the new group
     */
    public int isDecommitUpdate(GroupData other) {
        if (mGrpSize > other.grpSize())
            return 1;

        if (mGrpSize < other.grpSize())
            return -2;

        for (int i = 0; i < mGrpSize; i++) {
            int currID = mUsrIds[i];

            // verify the other has a user with the same ID
            int otherPos = -1;
            int j = 0;
            while (j < other.grpSize()) {
                if (currID == other.usrIDs()[j]) {
                    otherPos = j;
                    j = other.grpSize();
                }
                j = j + 1;
            }
            if (otherPos == -1)
                return -3;

            // verify the other data hashes to self's data
            byte[] ourHash = new byte[ExchangeConfig.HASH_LEN];
            ByteBuffer.wrap(mGrpData[i]).get(ourHash, 0, ExchangeConfig.HASH_LEN);
            if (!Arrays.equals(ourHash, CryptoAccess.computeSha3Hash(other.grpData()[otherPos])))
                return -1;
        }
        // if all of the data existed and hash correctly
        return 0;
    }

    /**
     * This function checks if the other is the proper one time signature for
     * this group's data.
     * 
     * @return 0: all is well and everyone says match, 2: at least one person
     *         says wrong, 1: still waiting for data, -1: one of the hashes is
     *         incorrect, -2: the groups are different sizes, -3: self has a
     *         user missing from the new group
     */
    public int isSignatureUpdate(GroupData other) {
        if (mGrpSize > other.grpSize())
            return 1;

        if (mGrpSize != other.grpSize())
            return -2;

        for (int i = 0; i < mGrpSize; i++) {
            int currID = mUsrIds[i];

            // verify the other has a user with the same ID
            int otherPos = -1;
            int j = 0;
            while (j < other.grpSize()) {
                if (currID == other.usrIDs()[j]) {
                    otherPos = j;
                    j = other.grpSize();
                }
                j = j + 1;
            }
            if (otherPos == -1)
                return -3;

            // verify the other data hashes to self's data
            byte[] hashA, hashB;
            byte[] otherCommit1 = new byte[ExchangeConfig.HASH_LEN];
            byte[] otherCommit2 = new byte[ExchangeConfig.HASH_LEN];
            byte[] thisCommit = new byte[ExchangeConfig.HASH_LEN];
            ByteBuffer.wrap(other.grpData()[otherPos])
                    .get(otherCommit1, 0, ExchangeConfig.HASH_LEN)
                    .get(otherCommit2, 0, ExchangeConfig.HASH_LEN);
            ByteBuffer.wrap(mGrpData[i]).get(thisCommit, 0, ExchangeConfig.HASH_LEN);

            // wrong
            hashA = otherCommit1;
            hashB = CryptoAccess.computeSha3Hash(otherCommit2);
            // if the "wrong" signature was correct return 2
            byte[] otherCommitW = CryptoAccess.computeSha3Hash2(hashA, hashB);
            if (Arrays.equals(thisCommit, otherCommitW))
                return 2;

            // match
            hashA = CryptoAccess.computeSha3Hash(otherCommit1);
            hashB = otherCommit2;
            // if the signature is invalid return -1
            byte[] otherCommitM = CryptoAccess.computeSha3Hash2(hashA, hashB);
            if (!Arrays.equals(thisCommit, otherCommitM))
                return -1;
        }
        // if all of the data existed and hash correctly
        return 0;
    }

    public int[] getOrderedIDs() {
        int[] orderedIDs = mUsrIds.clone();
        Arrays.sort(orderedIDs);
        return orderedIDs;
    }

    public byte[] getOrderedData() {
        // first determine the order based on user ID
        int[] orderedIDs = getOrderedIDs();
        int dataLen = 0;
        for (int x = 0; x < mGrpData.length; x++)
            dataLen += mGrpData[x].length;
        ByteBuffer allData = ByteBuffer.allocate(dataLen);

        // now concatenate the data in the specified order
        for (int i = 0; i < mGrpSize; i++) {
            int j = 0;
            while (j < mGrpSize) {
                if (mUsrIds[j] == orderedIDs[i]) {
                    allData.put(mGrpData[j]);
                    j = mGrpSize;
                }
                j = j + 1;
            }
        }
        return allData.array();
    }

    /**
     * This function returns the hash of the data.
     */
    public byte[] getHash() {
        byte[] orderedData = getOrderedData();
        return CryptoAccess.computeSha3Hash(orderedData);
    }

    public String getHexHash() {
        return String.format(Locale.US, "%#x", getHash());
    }

    public byte[][] sortAllHalfKeys() {
        ByteBuffer buf;
        byte[][] retVal = new byte[mGrpSize][];
        int j = 0;

        // first determine the order based on user ID
        int[] orderedIDs = getOrderedIDs();
        for (int o = 0; o < mGrpSize; o++) {
            for (int a = 0; a < mGrpSize; a++) {
                if (orderedIDs[o] == mUsrIds[a]) {
                    byte[] clip = new byte[ExchangeConfig.HASH_LEN];
                    byte[] dump = new byte[ExchangeConfig.HALFKEY_LEN];
                    buf = ByteBuffer.wrap(mGrpData[a]);
                    buf.get(clip, 0, ExchangeConfig.HASH_LEN);
                    buf.get(dump, 0, ExchangeConfig.HALFKEY_LEN);
                    retVal[j] = dump;
                    j++;
                }
            }
        }
        return retVal.clone();
    }

    public byte[][] sortOthersDataNew(int thisUserId) {
        ByteBuffer buf;
        byte[][] retVal = new byte[mGrpSize - 1][];
        int j = 0;

        // first determine the order based on user ID
        int[] orderedIDs = getOrderedIDs();
        for (int o = 0; o < mGrpSize; o++) {
            if (orderedIDs[o] != thisUserId) { // skip the calling user
                for (int a = 0; a < mGrpSize; a++) {
                    if (orderedIDs[o] == mUsrIds[a]) {
                        byte[] clip = new byte[ExchangeConfig.HASH_LEN + ExchangeConfig.HALFKEY_LEN];
                        byte[] dump;
                        buf = ByteBuffer.wrap(mGrpData[a]);
                        buf.get(clip, 0, ExchangeConfig.HASH_LEN + ExchangeConfig.HALFKEY_LEN);
                        dump = new byte[buf.remaining()];
                        buf.get(dump);
                        retVal[j] = dump;
                        j++;
                    }
                }
            }
        }
        return retVal.clone();
    }

    public byte[][] sortOthersData(int thisUserId) {
        ByteBuffer buf;
        byte[][] retVal = new byte[mGrpSize - 1][];
        int j = 0;

        // first determine the order based on user ID
        int[] orderedIDs = getOrderedIDs();
        for (int o = 0; o < mGrpSize; o++) {
            if (orderedIDs[o] != thisUserId) { // skip the calling user
                for (int a = 0; a < mGrpSize; a++) {
                    if (orderedIDs[o] == mUsrIds[a]) {
                        byte[] clip = new byte[ExchangeConfig.HASH_LEN];
                        byte[] dump;
                        buf = ByteBuffer.wrap(mGrpData[a]);
                        buf.get(clip, 0, ExchangeConfig.HASH_LEN);
                        dump = new byte[buf.remaining()];
                        buf.get(dump);
                        retVal[j] = dump;
                        j++;
                    }
                }
            }
        }
        return retVal.clone();
    }

    public byte[][] sortAllMatchNonce() {
        byte[][] retVal = new byte[mGrpSize][];
        int j = 0;

        // first determine the order based on user ID
        int[] orderedIDs = getOrderedIDs();
        for (int o = 0; o < mGrpSize; o++) {
            for (int a = 0; a < mGrpSize; a++) {
                if (orderedIDs[o] == mUsrIds[a]) {
                    retVal[j] = mGrpData[a];
                    j++;
                }
            }
        }
        return retVal.clone();
    }

    public byte[][] sortOthersMatchNonce(int thisUserId) {
        byte[][] retVal = new byte[mGrpSize - 1][];
        int j = 0;

        // first determine the order based on user ID
        int[] orderedIDs = getOrderedIDs();
        for (int o = 0; o < mGrpSize; o++) {
            if (orderedIDs[o] != thisUserId) { // skip the calling user
                for (int a = 0; a < mGrpSize; a++) {
                    if (orderedIDs[o] == mUsrIds[a]) {
                        retVal[j] = mGrpData[a];
                        j++;
                    }
                }
            }
        }
        return retVal.clone();
    }

    /**
     * Do a comparison based on the data contained really it is only useful for
     * != and ==. a > or < don't make much sense.
     */
    @Override
    public int compareTo(GroupData other) {
        if (mGrpSize != other.grpSize())
            return -1;

        for (int i = 0; i < mGrpSize; i++) {
            int currID = mUsrIds[i];

            // verify the other has a user with the same ID
            int otherPos = -1;
            for (int j = 0; i < other.grpSize(); j++) {
                if (currID == other.usrIDs()[j]) {
                    otherPos = j;
                    j = other.grpSize();
                }
            }
            if (otherPos == -1)
                return -1;

            // verify the data is the same
            if (mGrpData[i] != other.grpData()[otherPos])
                return -1;
        }
        // if everyone was the same return 0
        return 0;
    }

    @Override
    public String toString() {
        String retVal = mGrpSize + " members in the group, data sample:\n";
        int showLen = ExchangeConfig.HASH_LEN;
        ByteBuffer buf;
        byte[] hash = new byte[ExchangeConfig.HASH_LEN];
        byte[] dump;
        for (int i = 0; i < mGrpSize; i++) {
            retVal += "\n" + mUsrIds[i];
            retVal += " (" + i + "): ";
            retVal += mGrpData[i].length + "b ";
            buf = ByteBuffer.wrap(mGrpData[i]);
            buf.get(hash, 0, ExchangeConfig.HASH_LEN);
            dump = new byte[buf.remaining() > showLen ? showLen : buf.remaining()];
            buf.get(dump);
            retVal += new String(dump);
        }
        return retVal;
    }

}
