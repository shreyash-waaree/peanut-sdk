package com.keenon.peanut.supermarket.util;

import android.text.TextUtils;

import java.util.regex.Pattern;

public final class ValidationUtil {
  private static final Pattern EMAIL =
      Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

  private ValidationUtil() {}

  public static boolean isValidMobile(CharSequence s) {
    if (s == null) return false;
    String t = s.toString().trim();
    return t.length() == 10 && TextUtils.isDigitsOnly(t);
  }

  public static boolean isValidEmail(CharSequence s) {
    if (s == null || s.toString().trim().isEmpty()) return true;
    return EMAIL.matcher(s.toString().trim()).matches();
  }
}
