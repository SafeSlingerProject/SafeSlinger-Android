/* Original source from blog post: http://sapienmobile.com/?p=204. */

package edu.cmu.cylab.starslinger.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import edu.cmu.cylab.starslinger.R;

@SuppressLint("NewApi")
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
        /*
         * Scan the /system/etc/vold.fstab file and look for lines like this:
         * dev_mount sdcard /mnt/sdcard 1
         * /devices/platform/s3c-sdhci.0/mmc_host/mmc0 When one is found, split
         * it into its elements and then pull out the path to the that mount
         * point and add it to the arraylist some devices are missing the vold
         * file entirely so we add a path here to make sure the list always
         * includes the path to the first sdcard, whether real or emulated.
         */

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
        /*
         * Now that we have a cleaned list of mount paths, test each one to make
         * sure it's a valid and available path. If it is not, remove it from
         * the list.
         */

        for (int i = 0; i < sVold.size(); i++) {
            String voldPath = sVold.get(i);
            File path = new File(voldPath);
            if (!path.exists() || !path.isDirectory() || !path.canWrite())
                sVold.remove(i--);
        }
    }

    private static void setProperties() {
        /*
         * At this point all the paths in the list should be valid. Build the
         * public properties.
         */

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

        /*
         * don't need these anymore, clear the lists to reduce memory use and to
         * prepare them for the next time they're needed.
         */
        sVold.clear();
    }
}
