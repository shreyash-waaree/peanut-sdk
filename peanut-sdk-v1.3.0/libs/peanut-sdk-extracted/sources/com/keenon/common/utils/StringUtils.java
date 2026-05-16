package com.keenon.common.utils;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/StringUtils.class */
public class StringUtils {
    public static <T> String toString(Collection<T> data) {
        StringBuilder sb = new StringBuilder();
        if (null != data) {
            boolean split = false;
            for (T item : data) {
                if (split) {
                    sb.append(", ");
                }
                sb.append(item);
                split = true;
            }
        }
        return sb.toString();
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static int compareVersion(String v1, String v2) {
        String t1 = get(v1, "[0-9]+\\.[0-9]+\\.[0-9]+");
        String o1 = get(v2, "[0-9]+\\.[0-9]+\\.[0-9]+");
        if (t1 == null && o1 != null) {
            return -1;
        }
        if (t1 != null && o1 == null) {
            return 1;
        }
        if (t1 != null && o1 != null && !t1.equals(o1)) {
            String[] t1Arr = t1.split("\\.");
            String[] o1Arr = o1.split("\\.");
            if (Integer.parseInt(t1Arr[0]) != Integer.parseInt(o1Arr[0])) {
                return Integer.parseInt(t1Arr[0]) - Integer.parseInt(o1Arr[0]);
            }
            if (Integer.parseInt(t1Arr[1]) != Integer.parseInt(o1Arr[1])) {
                return Integer.parseInt(t1Arr[1]) - Integer.parseInt(o1Arr[1]);
            }
            if (Integer.parseInt(t1Arr[2]) != Integer.parseInt(o1Arr[2])) {
                return Integer.parseInt(t1Arr[2]) - Integer.parseInt(o1Arr[2]);
            }
        }
        String t2 = get(v1, "rc[0-9]*");
        String o2 = get(v2, "rc[0-9]*");
        if (t2 == null && o2 != null) {
            return 1;
        }
        if (t2 != null && o2 == null) {
            return -1;
        }
        if (t2 != null && o2 != null && !t2.equals(o2)) {
            if (t2.length() > o2.length()) {
                return 1;
            }
            if (t2.length() < o2.length()) {
                return -1;
            }
            return t2.compareTo(o2);
        }
        if (compareNum(t2, o2) != 0) {
            return compareNum(t2, o2);
        }
        String t3 = get(v1, "-[0-9]+");
        String o3 = get(v2, "-[0-9]+");
        if (compareNum(t3, o3) != 0) {
            return compareNum(t3, o3);
        }
        String t4 = get(v1, "-g[0-9a-z]+");
        String o4 = get(v2, "-g[0-9a-z]+");
        if (compareStr(t4, o4) != 0) {
            return compareStr(t4, o4);
        }
        String t5 = get(v1, "-dirty");
        String o5 = get(v2, "-dirty");
        if (compareStr(t5, o5) != 0) {
            return compareStr(t5, o5);
        }
        String t6 = get(v1, "-c.*");
        String o6 = get(v2, "-c.*");
        if (compareStr(t6, o6) != 0) {
            return compareStr(t6, o6);
        }
        return 0;
    }

    public static String get(String s, String regx) {
        if (s == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(regx, 2);
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return matcher.group().toString();
        }
        return null;
    }

    public static int compareStr(String t, String o) {
        if (t == null && o != null) {
            return -1;
        }
        if (t != null && o == null) {
            return 1;
        }
        if (t != null && o != null && !t.equals(o)) {
            return t.compareTo(o);
        }
        return 0;
    }

    public static int compareNum(String t, String o) {
        if (t == null && o != null) {
            return -1;
        }
        if (t != null && o == null) {
            return 1;
        }
        if (t != null && o != null && !t.equals(o)) {
            if (t.length() > o.length()) {
                return 1;
            }
            if (t.length() < o.length()) {
                return -1;
            }
            return t.compareTo(o);
        }
        return 0;
    }

    public static byte[] SNToByteArray(String SNStr) {
        StringBuilder stringBuilder = new StringBuilder();
        if (null != SNStr && !SNStr.isEmpty()) {
            String[] split = SNStr.trim().split(":");
            for (String s : split) {
                stringBuilder.append(s);
            }
        }
        if (!stringBuilder.toString().isEmpty()) {
            return ByteUtils.hexStrToBytes(stringBuilder.toString());
        }
        return new byte[0];
    }

    public static String StringToSN(String str) {
        String sn = str.toUpperCase();
        StringBuilder sb = new StringBuilder(sn);
        for (int length = str.length(); length > 0; length--) {
            if (length != str.length() && length % 2 == 0) {
                sb.insert(length, ":");
            }
        }
        return sb.toString();
    }
}
