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

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import edu.cmu.cylab.starslinger.R;

public class StorageOptions {
    public static String[] labels;
    public static String[] paths;
    public static int count = 0;

    private static Context sContext;
    private static ArrayList<String> sVold = new ArrayList<String>();

    public static void determineStorageOptions(Context context) {
        sContext = context.getApplicationContext();

        readVoldFile();
        testAndCleanList();
        setProperties();
    }

    private static void readVoldFile() {
        sVold.add("/mnt/sdcard");

        try {
            Scanner scanner = new Scanner(new File("/system/etc/vold.fstab"));
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.startsWith("dev_mount")) {
                    String[] lineElements = line.split(" ");
                    String element = lineElements[2];

                    if (element.contains(":"))
                        element = element.substring(0, element.indexOf(":"));

                    if (element.contains("usb"))
                        continue;

                    // don't add the default vold path
                    // it's already in the list.
                    if (!sVold.contains(element))
                        sVold.add(element);
                }
            }
            scanner.close();
        } catch (Exception e) {
            // swallow - don't care
            e.printStackTrace();
        }
    }

    private static void testAndCleanList() {
        for (int i = 0; i < sVold.size(); i++) {
            String voldPath = sVold.get(i);
            File path = new File(voldPath);
            if (!path.exists() || !path.isDirectory() || !path.canWrite())
                sVold.remove(i--);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void setProperties() {
        ArrayList<String> labelList = new ArrayList<String>();

        int j = 0;
        if (sVold.size() > 0) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD)
                labelList.add(sContext.getString(R.string.choice_auto_storage));
            else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                if (Environment.isExternalStorageRemovable()) {
                    labelList.add(sContext.getString(R.string.choice_external_storage) + " 1");
                    j = 1;
                } else
                    labelList.add(sContext.getString(R.string.choice_internal_storage));
            } else {
                if (!Environment.isExternalStorageRemovable()
                        || Environment.isExternalStorageEmulated())
                    labelList.add(sContext.getString(R.string.choice_internal_storage));
                else {
                    labelList.add(sContext.getString(R.string.choice_external_storage) + " 1");
                    j = 1;
                }
            }

            if (sVold.size() > 1) {
                for (int i = 1; i < sVold.size(); i++) {
                    labelList.add(sContext.getString(R.string.choice_external_storage) + " "
                            + (i + j));
                }
            }
        }

        labels = new String[labelList.size()];
        labelList.toArray(labels);

        paths = new String[sVold.size()];
        sVold.toArray(paths);

        count = Math.min(labels.length, paths.length);

        sVold.clear();
    }
}
