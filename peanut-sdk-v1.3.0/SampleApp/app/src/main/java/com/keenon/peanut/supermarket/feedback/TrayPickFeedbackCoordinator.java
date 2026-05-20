package com.keenon.peanut.supermarket.feedback;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.receiver.RobotMovementManager;
import com.keenon.peanut.sample.receiver.SensorDataManager;
import com.keenon.peanut.supermarket.fragment.TrayFeedbackDialogFragment;
import com.keenon.peanut.supermarket.util.TtsHelper;
import com.keenon.peanut.supermarket.vision.ObjectDetectorHelper;
import com.keenon.sdk.constant.ApiConstants;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Item picked from tray → smooth rotate toward hand → find customer on camera 3 →
 * star-rating dialog → resume at post-pick count (not zero).
 *
 * <p>Uses {@link SensorDataManager} obstacle / movement-block signals during rotation when
 * monitoring is enabled (see {@link #start(int, int, float)}).
 */
public class TrayPickFeedbackCoordinator {

    private static final String TAG = "TrayPickFeedback";
    private static final String PERSON_SCAN_MODEL = "models/ssd_mobilenet_v3.tflite";
    private static final int PERSON_CAMERA_ID = 3;

    /** Tunable motion + scan behaviour (e.g. Count 1.0 vs Count 2.0). */
    public static final class Timing {
        public final long initialHandTurnMs;
        public final long scanRotateStepMs;
        public final long scanLookStepMs;
        public final int scanMaxSteps;
        public final long personSearchTimeoutMs;
        public final int personConfirmFrames;
        public final long pauseBeforeDialogMs;

        public Timing(
                long initialHandTurnMs,
                long scanRotateStepMs,
                long scanLookStepMs,
                int scanMaxSteps,
                long personSearchTimeoutMs,
                int personConfirmFrames,
                long pauseBeforeDialogMs) {
            this.initialHandTurnMs = initialHandTurnMs;
            this.scanRotateStepMs = scanRotateStepMs;
            this.scanLookStepMs = scanLookStepMs;
            this.scanMaxSteps = scanMaxSteps;
            this.personSearchTimeoutMs = personSearchTimeoutMs;
            this.personConfirmFrames = personConfirmFrames;
            this.pauseBeforeDialogMs = pauseBeforeDialogMs;
        }

        /** Original Count-tab timing. */
        public static Timing defaults() {
            return new Timing(2400L, 2200L, 400L, 2, 22_000L, 2, 500L);
        }

        /** Stronger initial turn + wider person sweep for Count 2.0. */
        public static Timing count20() {
            return new Timing(3800L, 2600L, 520L, 4, 30_000L, 2, 650L);
        }
    }

    private final Timing timing;

    public interface PersonFrameSink {
        void onNv21Frame(byte[] nv21, int width, int height);
    }

    public interface PickFeedbackHost {
        Fragment getHostFragment();
        boolean isSdkReady();
        int getTrayCameraIdToRestore();
        void switchToPersonCamera(Runnable onReady);
        void restoreTrayCamera(int trayCameraId, Runnable onReady);
        void setPersonFrameSink(PersonFrameSink sink);
        /** Live hand track from hand_detect model, or {@code -1f}. */
        float getTrackedHandNormX();
    }

    public interface Callback {
        void onFeedbackFinished(int remainingCount);
    }

    private final PickFeedbackHost host;
    private final TtsHelper ttsHelper;
    private final Callback callback;
    private final ObjectDetectorHelper personDetector;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private int pendingPrev;
    private int pendingNew;
    private int scanDirection = ApiConstants.MotorMove.RIGHT;
    private int scanStep;
    private int personSeenFrames;
    private long searchStartedAtMs;
    private boolean scanning;
    private Runnable pendingStepRunnable;
    private final PersonFrameSink personFrameSink = this::onPersonNv21Frame;

    public TrayPickFeedbackCoordinator(
            Context context,
            PickFeedbackHost host,
            TtsHelper ttsHelper,
            Callback callback) {
        this(context, host, ttsHelper, callback, null);
    }

    public TrayPickFeedbackCoordinator(
            Context context,
            PickFeedbackHost host,
            TtsHelper ttsHelper,
            Callback callback,
            @Nullable Timing timingOverride) {
        this.host = host;
        this.ttsHelper = ttsHelper;
        this.callback = callback;
        this.timing = timingOverride != null ? timingOverride : Timing.defaults();
        this.personDetector =
                new ObjectDetectorHelper(context.getApplicationContext(), PERSON_SCAN_MODEL);
    }

    public boolean isBusy() {
        return busy.get();
    }

    public void start(int previousCount, int newCount, float slotPickNormX) {
        if (!busy.compareAndSet(false, true)) return;
        if (!host.getHostFragment().isAdded()) {
            busy.set(false);
            return;
        }

        pendingPrev = previousCount;
        pendingNew = newCount;
        scanStep = 0;
        personSeenFrames = 0;
        searchStartedAtMs = SystemClock.elapsedRealtime();
        scanning = false;

        float handX = host.getTrackedHandNormX();
        if (handX < 0f) {
            handX = slotPickNormX;
        }
        scanDirection = directionTowardHand(handX);

        Log.i(TAG, "Pick " + previousCount + " → " + newCount
                + " handX=" + handX + " rotate="
                + (scanDirection == ApiConstants.MotorMove.LEFT ? "LEFT" : "RIGHT"));

        SensorDataManager.getInstance().ensureSafetyMonitoring();

        mainHandler.post(() -> {
            rotateSmoothly(scanDirection, timing.initialHandTurnMs, () -> {
                host.setPersonFrameSink(personFrameSink);
                host.switchToPersonCamera(() -> {
                    scanning = true;
                    searchStartedAtMs = SystemClock.elapsedRealtime();
                    mainHandler.postDelayed(this::doScanStep, timing.scanLookStepMs);
                });
            });
        });
    }

    public void cancel() {
        busy.set(false);
        scanning = false;
        cancelStepRunnable();
        releaseMotorsFully();
        host.setPersonFrameSink(null);
        host.restoreTrayCamera(host.getTrayCameraIdToRestore(), null);
    }

    /**
     * Tray cameras look down: hand on the left of the image means the customer is on the robot's
     * right side, so we rotate RIGHT (and vice versa).
     */
    private static int directionTowardHand(float handNormX) {
        if (handNormX < 0f) {
            return ApiConstants.MotorMove.RIGHT;
        }
        return handNormX < 0.5f
                ? ApiConstants.MotorMove.RIGHT
                : ApiConstants.MotorMove.LEFT;
    }

    private void onPersonNv21Frame(byte[] nv21, int width, int height) {
        if (!scanning || personDetector == null) return;
        try {
            List<ObjectDetectorHelper.Result> results =
                    personDetector.detect(nv21, width, height, 0, false);
            boolean person = false;
            if (results != null) {
                for (ObjectDetectorHelper.Result r : results) {
                    if (r != null && "person".equalsIgnoreCase(r.label)) {
                        person = true;
                        break;
                    }
                }
            }
            if (person) {
                personSeenFrames++;
            } else {
                personSeenFrames = 0;
            }
            if (personSeenFrames >= timing.personConfirmFrames) {
                mainHandler.post(this::onPersonFound);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Person detect on cam " + PERSON_CAMERA_ID + " failed", t);
        }
    }

    private void onPersonFound() {
        if (!busy.get() || !scanning) return;
        scanning = false;
        cancelStepRunnable();
        stopMotorSoft();
        Log.i(TAG, "Customer found on camera " + PERSON_CAMERA_ID);
        beginFeedback();
    }

    private void doScanStep() {
        if (!busy.get() || !scanning) return;

        if (scanStep >= timing.scanMaxSteps
                || SystemClock.elapsedRealtime() - searchStartedAtMs > timing.personSearchTimeoutMs) {
            Log.i(TAG, "Person scan finished (steps=" + scanStep + ") — showing feedback");
            scanning = false;
            stopMotorSoft();
            beginFeedback();
            return;
        }

        if (SensorDataManager.getInstance().isMovementBlocked()) {
            Log.w(TAG, "Rotation paused — obstacle / collision (step " + scanStep + ")");
            pendingStepRunnable = this::doScanStep;
            mainHandler.postDelayed(pendingStepRunnable, 800L);
            return;
        }

        if (!host.isSdkReady()) {
            scanStep++;
            mainHandler.postDelayed(this::doScanStep, timing.scanLookStepMs);
            return;
        }

        final int stepDir = scanDirection;
        rotateSmoothly(stepDir, timing.scanRotateStepMs, () -> {
            scanStep++;
            mainHandler.postDelayed(this::doScanStep, timing.scanLookStepMs);
        });
    }

    /** Continuous rotation for {@code durationMs}; does not lock motors (no {@link RobotMovementManager#release()}). */
    private void rotateSmoothly(int direction, long durationMs, Runnable onDone) {
        if (!host.isSdkReady()) {
            if (onDone != null) onDone.run();
            return;
        }
        try {
            RobotMovementManager mgr = RobotMovementManager.getInstance();
            mgr.ensureMotorEnabled();
            mgr.executeDirection(direction);
            cancelStepRunnable();
            pendingStepRunnable = () -> {
                stopMotorSoft();
                if (onDone != null) onDone.run();
            };
            mainHandler.postDelayed(pendingStepRunnable, durationMs);
        } catch (Throwable t) {
            Log.w(TAG, "Motor rotate failed: " + t.getMessage());
            if (onDone != null) onDone.run();
        }
    }

    private void beginFeedback() {
        host.setPersonFrameSink(null);
        Fragment f = host.getHostFragment();
        String prompt = f.isAdded()
                ? f.getString(R.string.yolo_tts_feedback_prompt)
                : "Please give us your feedback.";
        ttsHelper.speak(prompt, () ->
                mainHandler.postDelayed(
                        () -> showFeedbackDialog(pendingPrev, pendingNew),
                        timing.pauseBeforeDialogMs));
    }

    private void showFeedbackDialog(int previousCount, int newCount) {
        Fragment f = host.getHostFragment();
        if (!f.isAdded() || f.isStateSaved()) {
            finishWithRestore(newCount);
            return;
        }

        androidx.fragment.app.FragmentManager fm = f.getParentFragmentManager();
        androidx.fragment.app.Fragment existing =
                fm.findFragmentByTag(TrayFeedbackDialogFragment.TAG);
        if (existing != null) {
            ((TrayFeedbackDialogFragment) existing).dismissAllowingStateLoss();
        }

        TrayFeedbackDialogFragment dialog =
                TrayFeedbackDialogFragment.newInstance(previousCount, newCount);
        dialog.setCallback(new TrayFeedbackDialogFragment.Callback() {
            @Override
            public void onFeedbackSubmitted(int stars) {
                Log.i(TAG, "Feedback: " + stars + " stars, tray continues at " + newCount);
                ttsHelper.speak("Thank you for your feedback!");
                finishWithRestore(newCount);
            }

            @Override
            public void onFeedbackDismissed() {
                finishWithRestore(newCount);
            }
        });
        dialog.show(fm, TrayFeedbackDialogFragment.TAG);
    }

    private void finishWithRestore(int remainingCount) {
        int restoreCam = host.getTrayCameraIdToRestore();
        host.restoreTrayCamera(restoreCam, () -> {
            releaseMotorsFully();
            busy.set(false);
            if (callback != null) {
                callback.onFeedbackFinished(remainingCount);
            }
        });
    }

    /** Stop motion only — keep MFG_TEST + unlocked so the next turn is smooth. */
    private void stopMotorSoft() {
        cancelStepRunnable();
        if (!host.isSdkReady()) return;
        try {
            RobotMovementManager.getInstance().stop();
        } catch (Throwable ignored) {
        }
    }

    /** End of flow: lock motors and restore AUTO work mode. */
    private void releaseMotorsFully() {
        cancelStepRunnable();
        if (!host.isSdkReady()) return;
        try {
            RobotMovementManager mgr = RobotMovementManager.getInstance();
            mgr.stop();
            mgr.release();
        } catch (Throwable ignored) {
        }
    }

    private void cancelStepRunnable() {
        if (pendingStepRunnable != null) {
            mainHandler.removeCallbacks(pendingStepRunnable);
            pendingStepRunnable = null;
        }
    }
}
