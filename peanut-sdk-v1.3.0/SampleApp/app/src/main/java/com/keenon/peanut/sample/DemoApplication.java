package com.keenon.peanut.sample;

import android.content.Context;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.keenon.peanut.supermarket.vision.TrayCountingHelper;


public class DemoApplication extends MultiDexApplication {
  private static String TAG = "DemoApplication";
  public static Context mAppContext = null;

  /** Loads OpenCV JNI early — explicit paths first (rk3288). */
  private static void preloadOpenCvNative(Context ctx) {
    try {
      String dir = ctx.getApplicationInfo().nativeLibraryDir;
      if (dir != null) {
        java.io.File cpp = new java.io.File(dir, "libc++_shared.so");
        java.io.File cv = new java.io.File(dir, "libopencv_java4.so");
        if (cv.exists()) {
          if (cpp.exists()) {
            try {
              System.load(cpp.getAbsolutePath());
            } catch (Throwable ignored) {
            }
          }
          System.load(cv.getAbsolutePath());
          android.util.Log.i(TAG, "Native preload: explicit libopencv_java4 OK");
          TrayCountingHelper.notifyOpenCvNativeLoaded();
          return;
        }
      }
    } catch (Throwable t) {
      android.util.Log.w(TAG, "explicit native preload failed", t);
    }
    try {
      System.loadLibrary("c++_shared");
    } catch (Throwable ignored) {
    }
    try {
      System.loadLibrary("opencv_java4");
      android.util.Log.i(TAG, "Native preload: opencv_java4 OK");
      TrayCountingHelper.notifyOpenCvNativeLoaded();
    } catch (Throwable t) {
      android.util.Log.w(TAG, "Native preload: opencv_java4 failed (TrayCount will retry)", t);
    }
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    MultiDex.install(this);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mAppContext = getApplicationContext();
    preloadOpenCvNative(this);
  }

  public static Context getAppContext() {
    return mAppContext;
  }
}
