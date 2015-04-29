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

package edu.cmu.cylab.starslinger.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.Name;
import a_vcard.android.syncml.pim.vcard.VCardComposer;
import a_vcard.android.syncml.pim.vcard.VCardException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.provider.MediaStore.MediaColumns;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.MimeTypeMap;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.model.MessageData;
import edu.cmu.cylab.starslinger.model.RecipientRow;
import edu.cmu.cylab.starslinger.model.SlingerIdentity;

public class SSUtil {

    // @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    // public static void executeAsyncTask(Impork, Object params)
    // {
    // if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
    // task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    // else
    // task.execute(params);
    //
    // }
    // or "ISO-8859-1" for ISO Latin 1
    private static CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();

    public static boolean isPureAscii(String v) {
        return asciiEncoder.canEncode(v);
    }

    public static byte[] makeThumbnail(Context ctx, byte[] imgData) {
        if (imgData == null) {
            return null;
        }
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
        if (Build.VERSION.SDK_INT < 8) {
            return SafeSlingerConfig.NOTIFY_NOPUSH;
        } else {
            return SafeSlingerConfig.NOTIFY_ANDROIDGCM;
        }
    }

    // public static boolean isGoogleAccountPresent(Context ctx) {
    // AccountManager am = AccountManager.get(ctx);
    // Account[] accounts = am.getAccountsByType("com.google");
    // if (accounts == null || accounts.length == 0) {
    // return false;
    // } else {
    // return true;
    // }
    // }

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
        File dir = new File(SafeSlingerPrefs.getDownloadDir());
        if (!dir.isDirectory()) {
            dir.mkdirs(); // make directory
        }
        return new File(dir, filename);
    }

    @SuppressWarnings("deprecation")
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
        SimpleDateFormat sdf = new SimpleDateFormat(SafeSlingerConfig.DATETIME_FILENAME, Locale.US);
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
        SimpleDateFormat sdf = new SimpleDateFormat(SafeSlingerConfig.DATETIME_FILENAME, Locale.US);
        String fileName = sdf.format(new Date()) + ".mp3";
        File dir = new File(root, fileName);
        return Uri.fromFile(dir);
    }

    @SuppressWarnings("deprecation")
    public static String generateRecipientVCard(RecipientRow recip) throws VCardException {

        String token = recip.getPushtoken();
        int notification = recip.getNotify();
        String pubKey = new String(recip.getPubkey());

        SlingerIdentity slinger = new SlingerIdentity(token, notification, pubKey);
        ContactStruct contact = new ContactStruct();

        contact.name = new Name(recip.getName());
        contact.photoBytes = recip.getPhoto();
        contact.addContactmethod(android.provider.Contacts.KIND_IM,
                android.provider.Contacts.ContactMethodsColumns.TYPE_HOME, new String(
                        finalEncode(SlingerIdentity.sidKey2DBKey(slinger).getBytes())),
                SafeSlingerConfig.APP_KEY_PUBKEY, false);
        contact.addContactmethod(android.provider.Contacts.KIND_IM,
                android.provider.Contacts.ContactMethodsColumns.TYPE_HOME, new String(
                        finalEncode(SlingerIdentity.sidPush2DBPush(slinger).getBytes())),
                SafeSlingerConfig.APP_KEY_PUSHTOKEN, false);

        VCardComposer composer = new VCardComposer();
        String vcardString = composer.createVCard(contact, VCardComposer.VERSION_VCARD30_INT);

        return vcardString;
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
            case SafeSlingerConfig.NOTIFY_NOPUSH:
                return ctx.getString(R.string.label_None);
            case SafeSlingerConfig.NOTIFY_ANDROIDC2DM:
                return ctx.getString(R.string.label_AndroidC2DMServiceName);
            case SafeSlingerConfig.NOTIFY_ANDROIDGCM:
                return ctx.getString(R.string.label_AndroidGCMServiceName);
            case SafeSlingerConfig.NOTIFY_APPLEUA:
                return ctx.getString(R.string.label_iOSUAServiceName);
            case SafeSlingerConfig.NOTIFY_APPLEAPNS:
                return ctx.getString(R.string.label_iOSAPNServiceName);
            case SafeSlingerConfig.NOTIFY_WINPHONEMPNS:
                return ctx.getString(R.string.label_WinPhoneMPNServiceName);
            case SafeSlingerConfig.NOTIFY_BLACKBERRYPS:
                return ctx.getString(R.string.label_BlackberryPushServiceName);
            case SafeSlingerConfig.NOTIFY_AMAZONADM:
                return ctx.getString(R.string.label_AmazonADMServiceName);
            default:
                return String.format(Locale.getDefault(), "%s %d",
                        ctx.getString(R.string.label_Device), notify);
        }
    }

    public static String getSimpleDeviceDisplayName(Context ctx, int notify) {
        switch (notify) {
            case SafeSlingerConfig.NOTIFY_NOPUSH:
                return "";
            case SafeSlingerConfig.NOTIFY_ANDROIDC2DM:
                return ctx.getString(R.string.label_AndroidOS);
            case SafeSlingerConfig.NOTIFY_ANDROIDGCM:
                return ctx.getString(R.string.label_AndroidOS);
            case SafeSlingerConfig.NOTIFY_APPLEUA:
                return ctx.getString(R.string.label_iOS);
            case SafeSlingerConfig.NOTIFY_APPLEAPNS:
                return ctx.getString(R.string.label_iOS);
            case SafeSlingerConfig.NOTIFY_WINPHONEMPNS:
                return ctx.getString(R.string.label_WinPhoneOS);
            case SafeSlingerConfig.NOTIFY_BLACKBERRYPS:
                return ctx.getString(R.string.label_BlackberryOS);
            case SafeSlingerConfig.NOTIFY_AMAZONADM:
                return ctx.getString(R.string.label_AmazonFireOS);
            default:
                return String.format(Locale.getDefault(), "%s %d",
                        ctx.getString(R.string.label_Device), notify);
        }
    }

    /***
     * Ensure arbitrary data is only Base 64 encoded once.
     */
    public static byte[] finalEncode(byte[] src) {
        byte[] dest = null;
        if (isArrayByteBase64(src)) {
            dest = src;
        } else {
            dest = Base64.encode(src, Base64.NO_WRAP);
        }
        return dest;
    }

    /***
     * Ensure arbitrary data can only be Base 64 decoded once.
     */
    public static byte[] finalDecode(byte[] src) {
        byte[] dest = null;
        try {
            dest = Base64.decode(src, Base64.NO_WRAP);
        } catch (IllegalArgumentException e) {
            return src;
        }

        if (!isArrayByteBase64(dest)) {
            dest = src;
        }
        return dest;
    }

    private static boolean isArrayByteBase64(byte[] src) {
        try {
            String s = new String(src, "UTF-8");
            final String base64Regex = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})$";
            return Pattern.matches(base64Regex, s);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String writeByteArray(byte[] bytes) {
        StringBuilder raw = new StringBuilder(String.format(Locale.US, "len %d: ", bytes.length));
        for (int i = 0; i < bytes.length; i++)
            raw.append(String.format(Locale.US, "%X ", bytes[i]));
        return raw.toString();
    }

    public static int getTotalUsers() {
        String userName;
        long keyDate;
        int userNumber = 0;
        do {
            userName = null;
            keyDate = 0;
            userName = SafeSlingerPrefs.getContactName(userNumber);
            keyDate = SafeSlingerPrefs.getKeyDate(userNumber);
            if (!TextUtils.isEmpty(userName)) {
                userNumber++;
            }
        } while (keyDate > 0);

        // always at least 1
        return userNumber > 1 ? userNumber : 1;
    }

    public static Intent updateIntentExplicitness(Context context, Intent implicitIntent) {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            // API 21 creates the need for explicit Intents,
            // so make sure only one can answer
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);
            if (resolveInfo == null) {
                return null;
            }

            ResolveInfo serviceInfo = resolveInfo.get(0);
            String packageName = serviceInfo.serviceInfo.packageName;
            String className = serviceInfo.serviceInfo.name;
            ComponentName component = new ComponentName(packageName, className);

            Intent explicitIntent = new Intent(implicitIntent);
            explicitIntent.setComponent(component);
            return explicitIntent;
        } else {
            return implicitIntent;
        }
    }

    public static MessageData addAttachmentFromUri(Context ctx, MessageData draft, Uri uri,
            String contentType) throws IOException {
        String name = null;
        try {
            Cursor c = ctx.getContentResolver().query(uri, new String[] {
                MediaColumns.DISPLAY_NAME
            }, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        name = c.getString(c.getColumnIndex(MediaColumns.DISPLAY_NAME));
                    }
                } finally {
                    c.close();
                }
            }
        } catch (IllegalArgumentException e) {
            // column may not exist
        }

        long size = -1;
        try {
            Cursor c = ctx.getContentResolver().query(uri, new String[] {
                MediaColumns.SIZE
            }, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        size = c.getInt(c.getColumnIndex(MediaColumns.SIZE));
                    }
                } finally {
                    c.close();
                }
            }
        } catch (IllegalArgumentException e) {
            // column may not exist
        }

        String data = null;
        try {
            Cursor c = ctx.getContentResolver().query(uri, new String[] {
                MediaColumns.DATA
            }, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        data = c.getString(c.getColumnIndex(MediaColumns.DATA));
                    }
                } finally {
                    c.close();
                }
            }
        } catch (IllegalArgumentException e) {
            // column may not exist
        }

        if (name == null) {
            name = uri.getLastPathSegment();
        }

        File f = null;
        if (size <= 0) {
            String uriString = uri.toString();
            if (uriString.startsWith("file://")) {
                f = new File(uriString.substring("file://".length()));
                size = f.length();
            }
        }

        ContentResolver cr = ctx.getContentResolver();
        InputStream is = null;
        // read file bytes
        try {
            is = cr.openInputStream(uri);
        } catch (FileNotFoundException e) {
            if (!TextUtils.isEmpty(data)) {
                is = new FileInputStream(data);
            } else {
                return draft; // unable to load file at all
            }
        }

        if ((contentType != null) && (contentType.indexOf('*') != -1)) {
            contentType = ctx.getContentResolver().getType(uri);
        }

        if (contentType == null) {
            contentType = URLConnection.guessContentTypeFromStream(is);
            if (contentType == null) {
                String extension = SSUtil.getFileExtensionOnly(name);
                contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (contentType == null) {
                    contentType = SafeSlingerConfig.MIMETYPE_OPEN_ATTACH_DEF;
                }
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[4096];
        while (is.read(buf) > -1) {
            baos.write(buf);
        }
        baos.flush();

        final byte[] fileBytes = baos.toByteArray();
        draft.setFileData(fileBytes);
        draft.setFileSize(fileBytes.length);
        draft.setFileType(contentType);
        draft.setFileName(name);
        if (f != null && f.exists()) {
            draft.setFileDir(f.getAbsolutePath());
        } else if (!TextUtils.isEmpty(data)) {
            draft.setFileDir(new File(data).getAbsolutePath());
        }
        return draft;
    }

    public static MessageData addAttachmentFromPath(MessageData draft, String path)
            throws FileNotFoundException {
        File phy = new File(path); // physical
        File vir = new File(path); // virtual, change if needed

        try {
            FileInputStream is = new FileInputStream(phy.getAbsolutePath());
            try {
                byte[] outFileData = new byte[is.available()];
                is.read(outFileData);
                draft.setFileData(outFileData);
                draft.setFileSize(outFileData.length);

                String type = URLConnection.guessContentTypeFromStream(is);
                if (type != null)
                    draft.setFileType(type);
                else {
                    String extension = SSUtil.getFileExtensionOnly(vir.getName());
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    if (type != null) {
                        draft.setFileType(type);
                    } else {
                        draft.setFileType(SafeSlingerConfig.MIMETYPE_OPEN_ATTACH_DEF);
                    }
                }
            } finally {
                is.close();
            }
        } catch (OutOfMemoryError e) {
            return draft;
        } catch (IOException e) {
            return draft;
        }
        draft.setFileName(vir.getName());
        draft.setFileDir(phy.getPath());
        draft.setMsgHash(null);

        return draft;
    }
}
