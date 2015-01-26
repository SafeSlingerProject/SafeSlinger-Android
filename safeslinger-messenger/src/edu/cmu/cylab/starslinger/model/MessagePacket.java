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

package edu.cmu.cylab.starslinger.model;

import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.text.TextUtils;
import edu.cmu.cylab.starslinger.GeneralException;
import edu.cmu.cylab.starslinger.util.SSUtil;

public class MessagePacket {
    public static final String DATE_FORMAT_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private int mFileSize = 0;
    private byte[] mFileName = new byte[0];
    private byte[] mFileType = new byte[0];
    private byte[] mText = new byte[0];
    private byte[] mPerson = new byte[0];
    private long mDateSent = 0;
    private int mVersion = 0;
    private byte[] mFileHash = new byte[0];

    public MessagePacket(int version, long dateSent, int fileSize, String fileName,
            String fileType, String text, String person, byte[] fileHash) throws GeneralException {

        if (version == 0)
            throw new GeneralException("bad message format: version required");
        if (TextUtils.isEmpty(person))
            throw new GeneralException("bad message format: person required");
        if (dateSent == 0)
            throw new GeneralException("bad message format: send date required");
        if (!TextUtils.isEmpty(fileType) && !SSUtil.isPureAscii(fileType))
            throw new GeneralException("bad message format: mime-type can be ASCII-only");

        mVersion = version;
        mDateSent = dateSent;
        mFileSize = fileSize;
        mFileName = fileName != null ? encodeUTF8(fileName) : new byte[0];
        mFileType = fileType != null ? encodeASCII(fileType) : new byte[0];
        mText = text != null ? encodeUTF8(text) : new byte[0];
        mPerson = person != null ? encodeUTF8(person) : new byte[0];
        mFileHash = fileHash != null ? fileHash : new byte[0];
    }

    public MessagePacket(byte[] formatted) throws GeneralException {
        try {
            ByteBuffer buf = ByteBuffer.wrap(formatted);
            byte[] bfnm, bfty, btxt, bper, bloc, bgmt, bfha;
            int szfnm, szfty, sztxt, szper, szloc, szgmt, szfha;

            // client version as 32-bit integer
            mVersion = buf.getInt();

            // date character encoding always ACSII-only
            szloc = buf.getInt();
            bloc = allocField(buf.remaining(), szloc);
            buf.get(bloc);
            mDateSent = iso8601Local2LocalMillis(decodeASCII(bloc));

            // file size as 32-bit integer
            mFileSize = buf.getInt();

            // file name character encoding always UTF-8
            szfnm = buf.getInt();
            bfnm = allocField(buf.remaining(), szfnm);
            buf.get(bfnm);
            mFileName = bfnm;

            // mime-type character encoding always ACSII-only
            szfty = buf.getInt();
            bfty = allocField(buf.remaining(), szfty);
            buf.get(bfty);
            mFileType = bfty;

            // text message character encoding always UTF-8
            sztxt = buf.getInt();
            btxt = allocField(buf.remaining(), sztxt);
            buf.get(btxt);
            mText = btxt;

            // person name character encoding always UTF-8
            szper = buf.getInt();
            bper = allocField(buf.remaining(), szper);
            buf.get(bper);
            mPerson = bper;

            // if formatted GMT time exists in this version, overwrite...
            // date character encoding always ACSII-only
            szgmt = buf.getInt();
            bgmt = allocField(buf.remaining(), szgmt);
            buf.get(bgmt);
            mDateSent = iso8601Zulu2GmtMillis(decodeASCII(bgmt));

            // file hash of encrypted attachment, always raw byte array
            szfha = buf.getInt();
            bfha = allocField(buf.remaining(), szfha);
            buf.get(bfha);
            mFileHash = bfha;

        } catch (BufferUnderflowException e) {
            // reached the end of the packet...
            return;
        } catch (IllegalArgumentException e) {
            throw new GeneralException(e.getLocalizedMessage());
        }
    }

    private byte[] allocField(int remaining, int lenField) {
        if (remaining < lenField) {
            return new byte[remaining];
        } else if (lenField > 0) {
            return new byte[lenField];
        } else {
            return new byte[0];
        }
    }

    public byte[] getBytes() {
        byte[] localDate = encodeASCII(localMillis2Iso8601Local(mDateSent));
        byte[] gmtDate = encodeASCII(gmtMillis2Iso8601Zulu(mDateSent));
        int capacity = 4 /* version */+ 4 + localDate.length + 4 /* fileSize */
                + 4 + mFileName.length + 4 + mFileType.length + 4 + mText.length + 4
                + mPerson.length + 4 + gmtDate.length + 4 + mFileHash.length;
        try {
            ByteBuffer buf = ByteBuffer.allocate(capacity);
            buf.putInt(mVersion);
            buf.putInt(localDate.length);
            buf.put(localDate);
            buf.putInt(mFileSize);
            buf.putInt(mFileName.length);
            buf.put(mFileName);
            buf.putInt(mFileType.length);
            buf.put(mFileType);
            buf.putInt(mText.length);
            buf.put(mText);
            buf.putInt(mPerson.length);
            buf.put(mPerson);
            buf.putInt(gmtDate.length);
            buf.put(gmtDate);
            buf.putInt(mFileHash.length);
            buf.put(mFileHash);
            return buf.array();
        } catch (BufferOverflowException e) {
            return null;
        }
    }

    private static String localMillis2Iso8601Local(long gmtMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_ISO8601, Locale.US);
        return sdf.format(new Date(gmtMillis));
    }

    private static long iso8601Local2LocalMillis(String iso8601Date) {
        DateFormat formatter = new SimpleDateFormat(DATE_FORMAT_ISO8601, Locale.US);
        try {
            return formatter.parse(iso8601Date).getTime();
        } catch (ParseException e) {
            return Date.parse(iso8601Date);
        }
    }

    private static String gmtMillis2Iso8601Zulu(long gmtMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_ISO8601, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date(gmtMillis));
    }

    private static long iso8601Zulu2GmtMillis(String iso8601Date) {
        DateFormat formatter = new SimpleDateFormat(DATE_FORMAT_ISO8601, Locale.US);
        try {
            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
            return formatter.parse(iso8601Date).getTime();
        } catch (ParseException e) {
            return Date.parse(iso8601Date);
        }
    }

    private byte[] encodeASCII(String str) {
        try {
            return str == null ? null : str.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private String decodeASCII(byte[] bytes) {
        try {
            return new String(bytes, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private byte[] encodeUTF8(String str) {
        try {
            return str == null ? null : str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private String decodeUTF8(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public long getDateSent() {
        return mDateSent;
    }

    public String getPerson() {
        return decodeUTF8(mPerson);
    }

    public String getText() {
        return decodeUTF8(mText);
    }

    public String getFileType() {
        return decodeASCII(mFileType);
    }

    public String getFileName() {
        return decodeUTF8(mFileName);
    }

    public int getFileSize() {
        return mFileSize;
    }

    public int getVersion() {
        return mVersion;
    }

    public byte[] getFileHash() {
        return mFileHash;
    }
}
