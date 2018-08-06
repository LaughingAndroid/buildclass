package com.kibey.plugin;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author mchwind
 * @version V1.0
 * @since 18/7/27
 * <p>
 * |       |    |   |   |````  |   |   ---  |\  |   |````
 * |      |_|   |   |   |  `|  |---|    |   | \ |   |  `|
 * |___  |   |   |_|     \_/   |   |   ___  |  \|    \_/
 */
public class Logs {
    public static final String TAG = "plugin_log ->";
    public static File LOG_FILE = new File("");
    public static boolean DEBUG = false;

    public static void d(Object object) {
        System.out.println(TAG + object);
        try {
            if (DEBUG) {
                FileUtils.write(LOG_FILE, object + "\n", "utf-8", true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
