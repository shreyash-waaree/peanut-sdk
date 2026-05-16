package com.keenon.peanut.sample;

/**
 * Activities that host {@link HomeFragment} must implement this (SDK link mode + re-init).
 */
public interface SdkDemoHost {

  String getType();

  void saveToSP(String type);

  void reinitSDK(String ip);

  /** Pause any active autonomous patrol so chassis demos can take over the SDK. Default no-op. */
  default void pausePatrolIfActive() {}
}
