package com.keenon.common.utils;

import android.util.Base64;
import com.keenon.common.constant.PeanutConstants;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/DesUtil.class */
public class DesUtil {
    private static final String DES = "DES";
    private static final String ENCODE = "utf-8";
    private static final String defaultKey = "keenon";

    public static byte[] build3DesKey(String keyStr) throws UnsupportedEncodingException {
        byte[] key = new byte[24];
        byte[] temp = keyStr.getBytes("UTF-8");
        if (key.length > temp.length) {
            System.arraycopy(temp, 0, key, 0, temp.length);
        } else {
            System.arraycopy(temp, 0, key, 0, key.length);
        }
        return key;
    }

    public static String encrypt(String data) throws Exception {
        byte[] bt = encrypt(data.getBytes(ENCODE), build3DesKey(defaultKey));
        return new String(Base64.encode(bt, 0), ENCODE);
    }

    public static String decrypt(String data) throws Exception {
        LogUtils.i(PeanutConstants.TAG_UTIL, "decrypt Data=" + data + "---key=" + defaultKey);
        if (data == null) {
            return null;
        }
        byte[] buf = Base64.decode(data.getBytes(ENCODE), 0);
        byte[] bt = decrypt(buf, build3DesKey(defaultKey));
        return new String(bt, ENCODE);
    }

    private static byte[] encrypt(byte[] data, byte[] key) throws Exception {
        SecureRandom sr = new SecureRandom();
        DESKeySpec dks = new DESKeySpec(key);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES);
        SecretKey securekey = keyFactory.generateSecret(dks);
        Cipher cipher = Cipher.getInstance(DES);
        cipher.init(1, securekey, sr);
        return cipher.doFinal(data);
    }

    private static byte[] decrypt(byte[] data, byte[] key) throws Exception {
        DESKeySpec dks = new DESKeySpec(key);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES);
        SecretKey securekey = keyFactory.generateSecret(dks);
        Cipher cipher = Cipher.getInstance(DES);
        cipher.init(2, securekey);
        return cipher.doFinal(data);
    }
}
