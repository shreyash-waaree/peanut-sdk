package com.keenon.peanut.supermarket.backend;

import android.content.Context;

import com.keenon.peanut.sample.BackendConfig;

/**
 * Uses the same SharedPreferences as {@link BackendConfig} for base URL.
 */
public final class PromoBackendConfig {

  private PromoBackendConfig() {}

  public static String getBaseUrl(Context context) {
    return BackendConfig.getBaseUrl(context);
  }

  public static boolean setBaseUrl(Context context, String url) {
    return BackendConfig.setBaseUrl(context, url);
  }

  public static String getInterestUrl(Context context) {
    return BackendConfig.getBaseUrl(context) + "/promo-interest";
  }

  public static String getFeedbackUrl(Context context) {
    return BackendConfig.getBaseUrl(context) + "/promo-feedback";
  }
}
