package com.keenon.sdk.config;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.FileUtil;
import com.keenon.common.utils.LogUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/config/PeanutProperties.class */
public class PeanutProperties {
    private static final String TAG = "[PeanutProperties]";
    public static final int PRODUCT = 1;
    public static final int MACHINE = 2;
    public static final int USER = 3;
    public static HashMap<Integer, Properties> map = new HashMap<>();

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/config/PeanutProperties$Prop.class */
    @Retention(RetentionPolicy.SOURCE)
    public @interface Prop {
    }

    public static Properties loadProperties(int prop) {
        if (map.get(Integer.valueOf(prop)) == null) {
            Properties properties = null;
            if (prop == 1) {
                properties = loadProductProperties();
            } else if (prop == 2) {
                properties = loadMachineProperties();
            } else if (prop == 3) {
                properties = loadUserProperties();
            }
            map.put(Integer.valueOf(prop), properties);
        }
        return map.get(Integer.valueOf(prop));
    }

    public static String getValue(String key, String defaultValue) {
        String value = defaultValue;
        if (loadProperties(3) != null && loadProperties(3).get(key) != null) {
            value = loadProperties(3).getProperty(key);
        } else if (loadProperties(2) != null && loadProperties(2).get(key) != null) {
            value = loadProperties(2).getProperty(key);
        } else if (loadProperties(1) != null && loadProperties(1).get(key) != null) {
            value = loadProperties(1).getProperty(key);
        }
        return value;
    }

    public static int getValue(String key, int defaultValue) {
        try {
            String value = getValue(key, defaultValue + "");
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG, (Exception) e);
            return defaultValue;
        }
    }

    public static boolean getValue(String key, boolean defaultValue) {
        try {
            String value = getValue(key, "");
            return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG, (Exception) e);
            return defaultValue;
        }
    }

    public static float getValue(String key, float defaultValue) {
        try {
            String value = getValue(key, "");
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG, (Exception) e);
            return defaultValue;
        }
    }

    public static void setProperties(String key, String value) {
        if (key == null || key.equals("")) {
            return;
        }
        Properties properties = loadUserProperties();
        properties.setProperty(key, value);
        try {
            FileOutputStream fos = new FileOutputStream(new File(PeanutConstants.PATH_PARAMS_PROPERTIES + "/user.properties"));
            properties.store(fos, (String) null);
            fos.flush();
            fos.close();
            map.put(3, properties);
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG, e);
        }
    }

    public static HashMap<String, String> loadRobotParams() {
        HashMap<String, String> map2 = new HashMap<>();
        for (Map.Entry<Object, Object> entry : loadProductProperties().entrySet()) {
            if (((String) entry.getKey()).startsWith("robot.")) {
                map2.put((String) entry.getKey(), (String) entry.getValue());
            }
        }
        for (Map.Entry<Object, Object> entry2 : loadMachineProperties().entrySet()) {
            if (((String) entry2.getKey()).startsWith("robot.")) {
                map2.put((String) entry2.getKey(), (String) entry2.getValue());
            }
        }
        for (Map.Entry<Object, Object> entry3 : loadUserProperties().entrySet()) {
            if (((String) entry3.getKey()).startsWith("robot.")) {
                map2.put((String) entry3.getKey(), (String) entry3.getValue());
            }
        }
        return map2;
    }

    public static Properties loadProperties(String name) {
        File file = new File(PeanutConstants.PATH_PARAMS_PROPERTIES);
        if (!file.exists()) {
            file.mkdir();
        }
        File[] files = FileUtil.getFiles(file.getAbsolutePath());
        for (int i = 0; files != null && i < files.length; i++) {
            if (files[i].getName().equals(name)) {
                try {
                    InputStream fis = new FileInputStream(files[i]);
                    Properties properties = new Properties();
                    properties.load(fis);
                    fis.close();
                    return properties;
                } catch (Exception e) {
                    LogUtils.e(PeanutConstants.TAG_SDK, TAG, e);
                }
            }
        }
        return new Properties();
    }

    public static String loadPropertiesAsJson(String name) {
        File file = new File(PeanutConstants.PATH_PARAMS_PROPERTIES);
        if (!file.exists()) {
            file.mkdir();
        }
        File[] files = FileUtil.getFiles(file.getAbsolutePath());
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(name)) {
                try {
                    InputStream fis = new FileInputStream(files[i]);
                    Properties properties = new Properties();
                    properties.load(fis);
                    Set<Map.Entry<Object, Object>> entrySet = properties.entrySet();
                    HashMap<String, String> map2 = new HashMap<>();
                    for (Map.Entry<Object, Object> entry : entrySet) {
                        map2.put((String) entry.getKey(), (String) entry.getValue());
                    }
                    return new JSONObject(map2).toString();
                } catch (IOException e) {
                    LogUtils.e(PeanutConstants.TAG_SDK, TAG, (Exception) e);
                }
            }
        }
        return null;
    }

    public static Properties loadMachineProperties() {
        return loadProperties("machine.properties");
    }

    public static Properties loadUserProperties() {
        return loadProperties("user.properties");
    }

    public static Properties loadProductProperties() {
        return loadProperties("product.properties");
    }
}
