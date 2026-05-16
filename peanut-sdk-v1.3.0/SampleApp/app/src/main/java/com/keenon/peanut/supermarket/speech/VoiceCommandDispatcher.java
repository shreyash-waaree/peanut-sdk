package com.keenon.peanut.supermarket.speech;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.keenon.peanut.sample.chassis.ChargerDemo;
import com.keenon.peanut.sample.receiver.RobotMovementManager;
import com.keenon.peanut.supermarket.SupermarketActivity;
import com.keenon.peanut.supermarket.fragment.HardwareFragment;
import com.keenon.sdk.constant.ApiConstants;

import java.util.Locale;

/**
 * Maps a voice transcript to a robot action (burst move, stop, dock-to-charger). Returns the
 * phrase that should be spoken back, or {@code unhandled()} so the caller falls through to FAQ.
 *
 * <p>This file does NOT modify any sample/SDK code. It uses {@link RobotMovementManager}
 * unchanged and launches {@link ChargerDemo} the same way the original sample did.
 */
public final class VoiceCommandDispatcher {

  private static final String TAG = "VoiceCmdDispatcher";

  /** Burst duration for "tap to nudge" voice direction commands. */
  private static final long BURST_MS = 500L;

  public static class Result {
    public final String reply;
    public final boolean handled;

    private Result(boolean handled, String reply) {
      this.handled = handled;
      this.reply = reply;
    }

    public static Result unhandled() {
      return new Result(false, null);
    }

    public static Result handled(String reply) {
      return new Result(true, reply);
    }
  }

  private VoiceCommandDispatcher() {}

  public static Result dispatch(SupermarketActivity activity, String transcript) {
    if (activity == null || transcript == null) return Result.unhandled();
    String t = transcript.toLowerCase(Locale.US).trim();
    if (t.isEmpty()) return Result.unhandled();

    // STOP first — highest priority for safety.
    if (containsAny(t, "stop", "halt", "wait", "pause")) {
      try {
        RobotMovementManager.getInstance().stop();
      } catch (Throwable ignored) {
      }
      try {
        activity.getPatrolNavManager().pausePatrol();
      } catch (Throwable ignored) {
      }
      return Result.handled("Stopping now.");
    }

    // Charging — launch ChargerDemo with EXTRA_START_AUTO_CHARGE (sample's voice-flow contract).
    if (containsAny(t, "charge", "charging", "dock", "go home")) {
      try {
        activity.getPatrolNavManager().pausePatrol();
      } catch (Throwable ignored) {
      }
      try {
        Intent intent = new Intent(activity, ChargerDemo.class);
        intent.putExtra(ChargerDemo.EXTRA_START_AUTO_CHARGE, true);
        activity.startActivity(intent);
      } catch (Throwable t2) {
        Log.e(TAG, "failed to launch ChargerDemo", t2);
      }
      return Result.handled("Heading back to the charger.");
    }

    // Direction nudges — short burst then auto-stop.
    HardwareFragment hw = findHardware(activity);
    if (containsAny(t, "forward", "go forward", "front", "move up", "go up",
            "ahead", "straight") || wordEquals(t, "up")) {
      if (hw != null) {
        hw.burst(ApiConstants.MotorMove.FRONT);
      } else {
        burstDirect(activity, ApiConstants.MotorMove.FRONT);
      }
      return Result.handled("Moving forward.");
    }
    if (containsAny(t, "backward", "back up", "move back", "reverse", "go back",
            "move down", "go down") || wordEquals(t, "down") || wordEquals(t, "back")) {
      if (hw != null) {
        hw.burst(ApiConstants.MotorMove.BACK);
      } else {
        burstDirect(activity, ApiConstants.MotorMove.BACK);
      }
      return Result.handled("Moving back.");
    }
    if (containsAny(t, "turn left", "left")) {
      if (hw != null) {
        hw.burst(ApiConstants.MotorMove.LEFT);
      } else {
        burstDirect(activity, ApiConstants.MotorMove.LEFT);
      }
      return Result.handled("Turning left.");
    }
    if (containsAny(t, "turn right", "right")) {
      if (hw != null) {
        hw.burst(ApiConstants.MotorMove.RIGHT);
      } else {
        burstDirect(activity, ApiConstants.MotorMove.RIGHT);
      }
      return Result.handled("Turning right.");
    }

    return Result.unhandled();
  }

  /**
   * Burst-move when no HardwareFragment is currently visible. Always re-acquires the motor
   * lock before driving so a released safety/lock doesn't silently swallow the command.
   */
  private static void burstDirect(FragmentActivity activity, int direction) {
    if (activity == null) return;
    try {
      if (activity instanceof SupermarketActivity) {
        try {
          ((SupermarketActivity) activity).getPatrolNavManager().pausePatrol();
        } catch (Throwable ignored) {
        }
      }
      final RobotMovementManager mgr = RobotMovementManager.getInstance();
      // Always re-assert the motor lock — firmware / other activities can release it.
      mgr.ensureMotorEnabled();
      // Give the enable call ~120 ms to take effect before the first manual() heartbeat.
      new Handler(Looper.getMainLooper())
          .postDelayed(
              () -> {
                try {
                  mgr.executeDirection(direction);
                } catch (Throwable t) {
                  Log.e(TAG, "executeDirection failed", t);
                }
                new Handler(Looper.getMainLooper())
                    .postDelayed(
                        () -> {
                          try {
                            mgr.stop();
                          } catch (Throwable ignored) {
                          }
                        },
                        BURST_MS);
              },
              120L);
    } catch (Throwable t) {
      Log.e(TAG, "burstDirect failed", t);
    }
  }

  private static HardwareFragment findHardware(FragmentActivity activity) {
    if (activity == null) return null;
    try {
      for (Fragment f : activity.getSupportFragmentManager().getFragments()) {
        if (f == null || !f.isAdded()) continue;
        if (f instanceof HardwareFragment) return (HardwareFragment) f;
        try {
          for (Fragment child : f.getChildFragmentManager().getFragments()) {
            if (child instanceof HardwareFragment && child.isAdded()) {
              return (HardwareFragment) child;
            }
          }
        } catch (Throwable ignored) {
        }
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  private static boolean containsAny(String haystack, String... needles) {
    for (String n : needles) {
      if (n != null && !n.isEmpty() && haystack.contains(n)) {
        return true;
      }
    }
    return false;
  }

  /** Exact-word match within a normalized transcript ("up", "up ", " up", " up ", "up."). */
  private static boolean wordEquals(String haystack, String word) {
    if (haystack == null || word == null || word.isEmpty()) return false;
    if (haystack.equals(word)) return true;
    if (haystack.startsWith(word + " ")) return true;
    if (haystack.endsWith(" " + word)) return true;
    return haystack.contains(" " + word + " ");
  }
}
