package com.keenon.common.utils.gsontypeadapter;

import android.text.TextUtils;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/gsontypeadapter/NumberUtils.class */
public class NumberUtils {
    public static boolean isIntOrLong(String str) {
        return TextUtils.isDigitsOnly(str);
    }

    public static boolean isFloatOrDouble(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[.\\d]*$");
        return pattern.matcher(str).matches();
    }
}
