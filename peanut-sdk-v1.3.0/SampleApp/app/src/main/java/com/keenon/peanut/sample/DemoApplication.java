package com.keenon.peanut.sample;

import android.content.Context;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;


public class DemoApplication extends MultiDexApplication {
  private static String TAG = "DemoApplication";
  public static Context mAppContext = null;

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    MultiDex.install(this);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mAppContext = getApplicationContext();
  }

  public static Context getAppContext() {
    return mAppContext;
  }
}
