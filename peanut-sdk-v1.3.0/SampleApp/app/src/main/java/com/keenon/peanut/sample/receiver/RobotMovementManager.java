package com.keenon.peanut.sample.receiver;

import android.util.Log;

import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.constant.ApiConstants;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe movement wrapper that reuses the EXACT same call chain as MotorDemo:
 *
 *   1. PeanutRuntime.getInstance().setWorkMode(ApiConstants.WorkMode.MFG_TEST)
 *   2. PeanutSDK.getInstance().motor().enable(null, MOTOR_ENABLE_UNLOCK)
 *   3. ScheduledThreadPoolExecutor at 100ms → motor().manual(callback, direction)
 *   4. executor.shutdownNow() to stop
 *   5. PeanutRuntime.getInstance().setWorkMode(ApiConstants.WorkMode.AUTO) on release
 *
 * This class does NOT call PeanutSDK.init() — the SDK must already be initialized
 * by KeenonApiDemoMain before this manager is used.
 */
public class RobotMovementManager {

    private static final String TAG = "RobotReceiver";
    private static final String LOG_COMMAND = "COMMAND";
    private static final String LOG_ERROR = "ERROR";
    private static final int HEARTBEAT_INTERVAL_MS = 100;  // Same as MotorDemo

    private static volatile RobotMovementManager sInstance;

    private ScheduledThreadPoolExecutor executor;
    private final AtomicBoolean isMoving = new AtomicBoolean(false);
    private final AtomicBoolean isPrepared = new AtomicBoolean(false);
    private final AtomicInteger currentDirection = new AtomicInteger(CommandParser.COMMAND_STOP);

    private Listener listener;

    public interface Listener {
        void onMovementStarted(int direction);
        void onMovementStopped();
        void onMovementError(String error);
    }

    private RobotMovementManager() {
    }

    public static RobotMovementManager getInstance() {
        if (sInstance == null) {
            synchronized (RobotMovementManager.class) {
                if (sInstance == null) {
                    sInstance = new RobotMovementManager();
                }
            }
        }
        return sInstance;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Prepare for movement: switch to MFG_TEST mode and unlock motors.
     * Must be called once before any movement commands.
     * Mirrors MotorDemo.initData() exactly.
     */
    public synchronized void prepare() {
        if (isPrepared.get()) {
            Log.d(TAG, "Already prepared, skipping");
            return;
        }

        Log.d(TAG, "Preparing: setting WorkMode to MFG_TEST");
        PeanutRuntime.getInstance().setWorkMode(ApiConstants.WorkMode.MFG_TEST);

        Log.d(TAG, "Preparing: unlocking motors");
        PeanutSDK.getInstance().motor().enable(null, ApiConstants.MOTOR_ENABLE_UNLOCK);

        isPrepared.set(true);
        Log.d(TAG, "Preparation complete — ready for movement commands");
    }

    /**
     * Always re-asserts the motor lock (work-mode + UNLOCK enable) regardless of whether
     * {@link #isPrepared} is already true. The firmware or another activity may have released
     * the lock between commands, in which case {@code motor().manual()} silently fails. Call
     * this immediately before each voice / remote burst to guarantee the motor is driveable.
     */
    public synchronized void ensureMotorEnabled() {
        try {
            PeanutRuntime.getInstance().setWorkMode(ApiConstants.WorkMode.MFG_TEST);
        } catch (Throwable t) {
            Log.w(TAG, "setWorkMode(MFG_TEST) failed", t);
        }
        try {
            PeanutSDK.getInstance().motor().enable(null, ApiConstants.MOTOR_ENABLE_UNLOCK);
        } catch (Throwable t) {
            Log.w(TAG, "motor().enable(UNLOCK) failed", t);
        }
        isPrepared.set(true);
    }

    /**
     * Execute a movement direction. If already moving, stops previous movement first.
     * Uses the exact same ScheduledThreadPoolExecutor + motor().manual() pattern from MotorDemo.
     *
     * @param direction one of ApiConstants.MotorMove.{FRONT, BACK, LEFT, RIGHT}
     */
    public synchronized void executeDirection(final int direction) {
        if (direction == CommandParser.COMMAND_UNKNOWN) {
            Log.w(LOG_COMMAND, "Ignoring unknown direction");
            return;
        }
        if (!isPrepared.get()) {
            Log.w(TAG, "Not prepared! Calling prepare() first...");
            prepare();
        }

        // Stop any current movement before starting new direction
        if (isMoving.get()) {
            stopInternal();
        }

        currentDirection.set(direction);
        isMoving.set(true);

        Log.i(LOG_COMMAND, "Starting movement: " + CommandParser.directionToString(direction));

        // Exact same pattern as MotorDemo.front()/back()/left()/right()
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        executor = new ScheduledThreadPoolExecutor(5);
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    PeanutSDK.getInstance().motor().manual(motorCallback, direction);
                } catch (Exception e) {
                    Log.e(LOG_ERROR, "Motor manual command failed", e);
                    stop();
                }
            }
        }, 0, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        if (listener != null) {
            listener.onMovementStarted(direction);
        }
    }

    /**
     * Stop all movement immediately (no grace period).
     * Mirrors MotorDemo's ACTION_UP handler: executor.shutdownNow().
     */
    public synchronized void stop() {
        stopInternal();
        currentDirection.set(CommandParser.COMMAND_STOP);

        if (listener != null) {
            listener.onMovementStopped();
        }
    }

    /**
     * Release resources: stop movement, lock motors, restore WorkMode to AUTO.
     * Mirrors MotorDemo.onDestroy().
     */
    public synchronized void release() {
        Log.d(TAG, "Releasing RobotMovementManager");

        stopInternal();

        if (isPrepared.get()) {
            Log.d(TAG, "Restoring WorkMode to AUTO");
            PeanutRuntime.getInstance().setWorkMode(ApiConstants.WorkMode.AUTO);

            Log.d(TAG, "Locking motors");
            PeanutSDK.getInstance().motor().enable(null, ApiConstants.MOTOR_ENABLE_LOCK);

            isPrepared.set(false);
        }

        currentDirection.set(CommandParser.COMMAND_STOP);
        listener = null;
        Log.d(TAG, "RobotMovementManager released");
    }

    /**
     * Internal stop: just shuts down the executor, no listener callback.
     */
    private void stopInternal() {
        if (executor != null && !executor.isShutdown()) {
            Log.i(LOG_COMMAND, "Stopping movement");
            executor.shutdownNow();
            executor = null;
        }
        isMoving.set(false);
    }

    public boolean isMoving() {
        return isMoving.get();
    }

    public boolean isPrepared() {
        return isPrepared.get();
    }

    public int getCurrentDirection() {
        return currentDirection.get();
    }

    /**
     * Motor callback — same pattern as MotorDemo's anonymous IDataCallback.
     */
    private final IDataCallback motorCallback = new IDataCallback() {
        @Override
        public void success(String result) {
            // Heartbeat success — no-op to avoid log spam at 100ms
        }

        @Override
        public void error(ApiError error) {
            Log.e(LOG_ERROR, "Motor command error: " + String.valueOf(error));
            if (listener != null) {
                listener.onMovementError("Motor error: " + String.valueOf(error));
            }
        }
    };
}
