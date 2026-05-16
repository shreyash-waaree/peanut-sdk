package com.keenon.peanut.supermarket.util;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public final class RobotDeviceId {
  private RobotDeviceId() {}

  public static String get(Context context) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        String s = Build.getSerial();
        if (s != null && !Build.UNKNOWN.equals(s)) {
          return s;
        }
      } else {
        @SuppressWarnings("deprecation")
        String s = Build.SERIAL;
        if (s != null && !Build.UNKNOWN.equals(s)) {
          return s;
        }
      }
    } catch (SecurityException ignored) {
    }
    return Settings.Secure.getString(
        context.getContentResolver(), Settings.Secure.ANDROID_ID);
  }
}
