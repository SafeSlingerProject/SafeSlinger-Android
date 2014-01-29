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

package edu.cmu.cylab.starslinger.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.Name;
import a_vcard.android.syncml.pim.vcard.VCardComposer;
import a_vcard.android.syncml.pim.vcard.VCardException;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.MimeTypeMap;
import edu.cmu.cylab.keyslinger.lib.KsConfig;
import edu.cmu.cylab.starslinger.ConfigData;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.model.RecipientRow;
import edu.cmu.cylab.starslinger.model.SlingerIdentity;

public class SSUtil {

    // or "ISO-8859-1" for ISO Latin 1
    private static CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();

    public static boolean isPureAscii(String v) {
        return asciiEncoder.canEncode(v);
    }

    public static byte[] makeThumbnail(Context ctx, byte[] imgData) {

        try {
            int dimension = (int) ctx.getResources().getDimension(R.dimen.avatar_size_list);
            Bitmap scaled = decodeSampledBitmapFromByte(imgData, dimension, dimension);
            if (scaled != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                return baos.toByteArray();
            }
        } catch (OutOfMemoryError e) {
            return null;
        }
        return null;
    }

    public static Bitmap decodeSampledBitmapFromByte(byte[] res, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeByteArray(res, 0, res.length, options);
        } catch (OutOfMemoryError e) {
            return null;
        }

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        try {
            return BitmapFactory.decodeByteArray(res, 0, res.length, options);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
            int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    public static int getLocalNotification(Context ctx) {
        if (Build.VERSION.SDK_INT < 8 || !isGoogleAccountPresent(ctx)) {
            return ConfigData.NOTIFY_NOPUSH;
        } else {
            return ConfigData.NOTIFY_ANDROIDC2DM;
        }
    }

    public static boolean isGoogleAccountPresent(Context ctx) {
        AccountManager am = AccountManager.get(ctx);
        Account[] accounts = am.getAccountsByType("com.google");
        if (accounts == null || accounts.length == 0) {
            return false;
        } else {
            return true;
        }
    }

    public static File getOldDefaultDownloadPath(String mimeType, String filename) {
        if (TextUtils.isEmpty(mimeType)) {
            String extension = SSUtil.getFileExtensionOnly(filename);
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        File dir;
        String pubDir;
        // set default directory by type
        if (mimeType.startsWith("image/")) {
            pubDir = Environment.DIRECTORY_PICTURES;
        } else if (mimeType.startsWith("audio/")) {
            pubDir = Environment.DIRECTORY_MUSIC;
        } else if (mimeType.startsWith("video/")) {
            pubDir = Environment.DIRECTORY_MOVIES;
        } else {
            pubDir = Environment.DIRECTORY_DOWNLOADS;
        }
        dir = Environment.getExternalStoragePublicDirectory(pubDir);

        if (!dir.isDirectory()) {
            dir.mkdirs(); // make directory
        }
        return new File(dir, filename);
    }

    public static File getDefaultDownloadPath(String filename) {
        File dir = new File(ConfigData.loadPrefDownloadDir(SafeSlinger.getApplication()));
        if (!dir.isDirectory()) {
            dir.mkdirs(); // make directory
        }
        return new File(dir, filename);
    }

    public static boolean isDayChanged(long lastScan) {
        Date last = new Date(lastScan);
        Date now = new Date();
        long lastlong = Date.UTC(last.getYear(), last.getMonth(), last.getDay(), 0, 0, 0);
        long nowlong = Date.UTC(now.getYear(), now.getMonth(), now.getDay(), 0, 0, 0);
        return lastlong != nowlong;
    }

    public static String longKeyId2Base64String(long keyId) {
        try {
            return new String(Base64.encode(ByteBuffer.allocate(Long.SIZE / 8).putLong(keyId)
                    .array(), Base64.NO_WRAP), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static long base64StringKeyId2Long(String keyId) {
        try {
            return ByteBuffer.wrap(Base64.decode(keyId.getBytes("UTF-8"), Base64.NO_WRAP))
                    .getLong();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return 0;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean fileExists(Context ctx, String filename) {
        synchronized (SafeSlinger.sDataLock) {
            String[] files = ctx.fileList();
            if (files != null) {
                for (String file : files) {
                    if (file.equalsIgnoreCase(filename)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static Uri makeCameraOutputUri() {
        final File root = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                        + File.separator
                        + SafeSlinger.getApplication().getString(R.string.app_name)
                        + File.separator);
        root.mkdirs();
        SimpleDateFormat sdf = new SimpleDateFormat(ConfigData.DATETIME_FILENAME, Locale.US);
        String fileName = sdf.format(new Date()) + ".jpg";
        File dir = new File(root, fileName);
        return Uri.fromFile(dir);
    }

    public static Uri makeRecorderOutputUri() {
        final File root = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                        + File.separator
                        + SafeSlinger.getApplication().getString(R.string.app_name)
                        + File.separator);
        root.mkdirs();
        SimpleDateFormat sdf = new SimpleDateFormat(ConfigData.DATETIME_FILENAME, Locale.US);
        String fileName = sdf.format(new Date()) + ".mp3";
        File dir = new File(root, fileName);
        return Uri.fromFile(dir);
    }

    public static byte[] generateRecipientVCard(RecipientRow recip) throws VCardException {

        String token = recip.getPushtoken();
        int notification = recip.getNotify();
        String pubKeyPolar = new String(recip.getPubkey());

        SlingerIdentity slinger = new SlingerIdentity(token, notification, pubKeyPolar);
        ContactStruct contact = new ContactStruct();

        contact.name = new Name(recip.getName());
        contact.photoBytes = recip.getPhoto();
        contact.addContactmethod(android.provider.Contacts.KIND_IM,
                android.provider.Contacts.ContactMethodsColumns.TYPE_HOME,
                new String(KsConfig.finalEncode(SlingerIdentity.sidKey2DBKey(slinger).getBytes())),
                ConfigData.APP_KEY_PUBKEY, false);
        contact.addContactmethod(
                android.provider.Contacts.KIND_IM,
                android.provider.Contacts.ContactMethodsColumns.TYPE_HOME,
                new String(KsConfig.finalEncode(SlingerIdentity.sidPush2DBPush(slinger).getBytes())),
                ConfigData.APP_KEY_PUSHTOKEN, false);

        VCardComposer composer = new VCardComposer();
        String vcardString = composer.createVCard(contact, VCardComposer.VERSION_VCARD30_INT);

        return vcardString.getBytes();
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static String makeDebugLoggingDir(Context ctx) {
        String path = ctx.getExternalCacheDir().getAbsolutePath() + File.separator + "logs";
        File root = new File(path);
        root.mkdirs();
        return path;
    }

    public static String changeFileExtTo(String inPath, String ext) {
        File from = new File(inPath);
        File to = new File(from.getParent(), getFileNameOnly(from.getName()) + "." + ext);
        return to.getAbsolutePath();
    }

    public static String getFileNameOnly(String filename) {
        String extension = filename.substring(0, filename.lastIndexOf("."));
        return extension;
    }

    public static String getFileExtensionOnly(String filename) {
        String extension = filename.substring((filename.lastIndexOf(".") + 1), filename.length());
        return extension;
    }

    public static String getEnumeratedFilename(String inPath, int number) {
        File from = new File(inPath);
        String fName = getFileNameOnly(from.getName());
        String fExt = getFileExtensionOnly(from.getName());
        File to = new File(from.getParent(), (fName + "-" + number + "." + fExt));
        return to.getAbsolutePath();
    }

    public static String getSizeString(Context ctx, Integer size) {
        if (size >= 1000000000) {
            return (String.format(ctx.getString(R.string.label_gb), ((double) size / 1000000000.0)));
        } else if (size >= 1000000) {
            return (String.format(ctx.getString(R.string.label_mb), ((double) size / 1000000.0)));
        } else if (size >= 1000) {
            return (String.format(ctx.getString(R.string.label_kb), ((double) size / 1000.0)));
        } else {
            return (String.format(ctx.getString(R.string.label_b), (double) size));
        }
    }

    public static String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }

    public static String getDetailedDeviceDisplayName(Context ctx, int notify) {
        switch (notify) {
            case ConfigData.NOTIFY_NOPUSH:
                return ctx.getString(R.string.label_None);
            case ConfigData.NOTIFY_ANDROIDC2DM:
                return ctx.getString(R.string.label_AndroidC2DMServiceName);
            case ConfigData.NOTIFY_APPLEUA:
                return ctx.getString(R.string.label_iOSAPNServiceName);
            default:
                return ctx.getString(R.string.label_undefinedTypeLabel);
        }
    }

    public static String getSimpleDeviceDisplayName(Context ctx, int notify) {
        switch (notify) {
            case ConfigData.NOTIFY_NOPUSH:
                return "";
            case ConfigData.NOTIFY_ANDROIDC2DM:
                return ctx.getString(R.string.label_AndroidOS);
            case ConfigData.NOTIFY_APPLEUA:
                return ctx.getString(R.string.label_iOS);
            default:
                return ctx.getString(R.string.label_undefinedTypeLabel);
        }
    }
}