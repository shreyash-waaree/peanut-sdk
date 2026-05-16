package com.keenon.common.utils;

import java.security.MessageDigest;
import java.util.UUID;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/ByteUtils.class */
public class ByteUtils {
    private static final String TAG = "Hedera-ByteUtils";
    private static final char[] bcdLookup = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final char[] upBcdLookup = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String toString(Byte[] bytes) {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (Byte aByte : bytes) {
            int v = aByte.byteValue() & 255;
            String hv = Integer.toHexString(v).toUpperCase();
            if (hv.length() < 2) {
                sb.append(0);
            }
            sb.append(hv);
            sb.append(" ");
        }
        return sb.toString();
    }

    public static String byteToBitString(byte byt) {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(byt & 1);
            byt = (byte) (byt >> 1);
        }
        return sb.reverse().toString();
    }

    public static String byteToBitString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 8);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(byteToBitString(bytes[i]));
            sb.append("  ");
            int j = i + 1;
            if (j % 4 == 0) {
                sb.append('\n');
            }
        }
        return " Size " + bytes.length + " and binary bits : \n" + sb.toString();
    }

    public static String BytesToHexStr(Byte[] bcd) {
        return bytesToHexStr(ObjectToByte(bcd));
    }

    public static String bytesToHexStr(byte[] bcd) {
        StringBuffer s = new StringBuffer(bcd.length * 2);
        for (int i = 0; i < bcd.length; i++) {
            s.append(bcdLookup[(bcd[i] >>> 4) & 15]);
            s.append(bcdLookup[bcd[i] & 15]);
        }
        return s.toString().replace("\n", "");
    }

    public static String bytesToUpperHexStr(byte[] bcd) {
        StringBuffer s = new StringBuffer(bcd.length * 2);
        for (int i = 0; i < bcd.length; i++) {
            s.append(upBcdLookup[(bcd[i] >>> 4) & 15]);
            s.append(upBcdLookup[bcd[i] & 15]);
        }
        return s.toString();
    }

    public static byte[] hexStrToBytes(String s) {
        byte[] bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(s.substring(2 * i, (2 * i) + 2), 16);
        }
        return bytes;
    }

    public static Byte[] hexStrToByte(String s) {
        if (s == null || s.length() == 0) {
            return null;
        }
        byte[] bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(s.substring(2 * i, (2 * i) + 2), 16);
        }
        return ByteToObject(bytes);
    }

    public static byte[] ObjectToByte(Byte[] data, int size) {
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = data[i].byteValue();
        }
        return result;
    }

    public static byte[] ObjectToByte(Byte[] data) {
        return ObjectToByte(data, data.length);
    }

    public static Byte[] ByteToObject(byte[] data, int size) {
        Byte[] result = new Byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = Byte.valueOf(data[i]);
        }
        return result;
    }

    public static Byte[] ByteToObject(byte[] data) {
        return ByteToObject(data, data.length);
    }

    public static byte[] intToBytes(int value) {
        byte[] src = {(byte) (value & 255), (byte) ((value >> 8) & 255), (byte) ((value >> 16) & 255), (byte) ((value >> 24) & 255)};
        return src;
    }

    public static byte[] intToBytes2(int value) {
        byte[] src = {(byte) ((value >> 24) & 255), (byte) ((value >> 16) & 255), (byte) ((value >> 8) & 255), (byte) (value & 255)};
        return src;
    }

    public static int bytesToInt(byte[] bytes, int offset) {
        int value = 0;
        for (int i = 0; i < bytes.length; i++) {
            int shift = ((bytes.length - 1) - i) * 8;
            value += (bytes[i] & 255) << shift;
        }
        return value;
    }

    public static int bytesToInt3(byte[] src, int offset) {
        int value = (src[offset] & 255) | ((src[offset + 1] & 255) << 8) | ((src[offset + 2] & 255) << 16) | ((src[offset + 3] & 255) << 24);
        return value;
    }

    public static int bytesToInt2(byte[] src, int offset) {
        int value = ((src[offset] & 255) << 24) | ((src[offset + 1] & 255) << 16) | ((src[offset + 2] & 255) << 8) | (src[offset + 3] & 255);
        return value;
    }

    public static String getMd5(String source) {
        String s = "";
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(source.getBytes());
            byte[] tmp = md.digest();
            char[] str = new char[32];
            int k = 0;
            for (int i = 0; i < 16; i++) {
                byte byte0 = tmp[i];
                int i2 = k;
                int k2 = k + 1;
                str[i2] = hexDigits[(byte0 >>> 4) & 15];
                k = k2 + 1;
                str[k2] = hexDigits[byte0 & 15];
            }
            s = new String(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    public static String buildAppId() {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();
        return str.replace("-", "");
    }

    public static String buildSecret(String appId, String packageName, String sn, long expire) {
        return getMd5(getMd5(appId + packageName) + "keenon" + sn + expire);
    }

    public static short getShort(byte a, byte b) {
        return (short) ((a & 255) | (b << 8));
    }
}
