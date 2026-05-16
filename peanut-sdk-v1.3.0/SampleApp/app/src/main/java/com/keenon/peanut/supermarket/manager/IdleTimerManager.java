package com.keenon.peanut.supermarket.manager;

import android.os.Handler;
import android.os.Looper;

public class IdleTimerManager {
  private final Handler handler = new Handler(Looper.getMainLooper());
  private Runnable globalIdleRunner;
  private Runnable confirmRunner;

  public void startGlobalIdle(long timeoutMs, Runnable onExpiry) {
    cancelGlobalIdle();
    globalIdleRunner = onExpiry;
    handler.postDelayed(globalIdleRunner, timeoutMs);
  }

  public void restartGlobalIdle(long timeoutMs) {
    if (globalIdleRunner == null) return;
    Runnable r = globalIdleRunner;
    handler.removeCallbacks(r);
    handler.postDelayed(r, timeoutMs);
  }

  public void cancelGlobalIdle() {
    if (globalIdleRunner != null) {
      handler.removeCallbacks(globalIdleRunner);
      globalIdleRunner = null;
    }
  }

  public void startConfirmIdle(long timeoutMs, Runnable onExpiry) {
    cancelConfirmIdle();
    confirmRunner = onExpiry;
    handler.postDelayed(confirmRunner, timeoutMs);
  }

  public void cancelConfirmIdle() {
    if (confirmRunner != null) {
      handler.removeCallbacks(confirmRunner);
      confirmRunner = null;
    }
  }

  public void cancelAll() {
    cancelGlobalIdle();
    cancelConfirmIdle();
    handler.removeCallbacksAndMessages(null);
  }
}
