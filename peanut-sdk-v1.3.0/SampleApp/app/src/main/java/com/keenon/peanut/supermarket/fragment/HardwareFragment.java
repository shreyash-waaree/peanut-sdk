package com.keenon.peanut.supermarket.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.receiver.CommandParser;
import com.keenon.peanut.sample.receiver.RobotMovementManager;
import com.keenon.peanut.sample.receiver.SensorDataManager;
import com.keenon.peanut.supermarket.SupermarketActivity;
import com.keenon.peanut.supermarket.speech.VoiceCommandDispatcher;
import com.keenon.sdk.constant.ApiConstants;

import org.json.JSONObject;

/**
 * Hardware diagnostic tab inside the Developer tab.
 *
 * <p>Tap any direction button for a short burst of movement (about a foot at the robot's default
 * manual speed). Stop button halts motion immediately. Sensors refresh at ~5 Hz.
 *
 * <p>We do NOT gate on an "SDK ready" flag — the flag can become a false negative if the init
 * listener hasn't fired yet but the SDK singletons are otherwise usable. Instead we try each SDK
 * call and surface any failure via Toast.
 */
public class HardwareFragment extends Fragment implements SensorDataManager.SensorDataListener {

  private static final String TAG = "HardwareFragment";

  /** Duration of each tap-to-move burst. ~500 ms ≈ one short step at the robot's manual speed. */
  private static final long BURST_MS = 500L;

  private TextView motorStateLine;
  private TextView tvBattery, tvPosition, tvMotor, tvNav, tvSonar, tvCollision, tvCharge, tvImu;
  private Button btnPrepare, btnRelease, btnFront, btnBack, btnLeft, btnRight, btnStop;

  private final Handler main = new Handler(Looper.getMainLooper());
  private RobotMovementManager movementManager;
  private SensorDataManager sensorManager;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_dev_hardware, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(v, savedInstanceState);

    motorStateLine = v.findViewById(R.id.hw_motor_state_line);

    tvBattery = v.findViewById(R.id.tv_sensor_battery);
    tvPosition = v.findViewById(R.id.tv_sensor_position);
    tvMotor = v.findViewById(R.id.tv_sensor_motor);
    tvNav = v.findViewById(R.id.tv_sensor_nav);
    tvSonar = v.findViewById(R.id.tv_sensor_sonar);
    tvCollision = v.findViewById(R.id.tv_sensor_collision);
    tvCharge = v.findViewById(R.id.tv_sensor_charge);
    tvImu = v.findViewById(R.id.tv_sensor_imu);

    btnPrepare = v.findViewById(R.id.btn_hw_prepare);
    btnRelease = v.findViewById(R.id.btn_hw_release);
    btnFront = v.findViewById(R.id.btn_hw_front);
    btnBack = v.findViewById(R.id.btn_hw_back);
    btnLeft = v.findViewById(R.id.btn_hw_left);
    btnRight = v.findViewById(R.id.btn_hw_right);
    btnStop = v.findViewById(R.id.btn_hw_stop);

    movementManager = RobotMovementManager.getInstance();
    sensorManager = SensorDataManager.getInstance();

    btnPrepare.setOnClickListener(view -> onPrepare());
    btnRelease.setOnClickListener(view -> onRelease());

    btnFront.setOnClickListener(view -> burst(ApiConstants.MotorMove.FRONT));
    btnBack.setOnClickListener(view -> burst(ApiConstants.MotorMove.BACK));
    btnLeft.setOnClickListener(view -> burst(ApiConstants.MotorMove.LEFT));
    btnRight.setOnClickListener(view -> burst(ApiConstants.MotorMove.RIGHT));

    // Direction buttons disabled until the user explicitly taps Prepare — this avoids racing the
    // first motor().manual() call against the MFG_TEST mode-switch that prepare() initiates.
    setDirectionButtonsEnabled(false);
    btnStop.setOnClickListener(view -> {
      try {
        movementManager.stop();
      } catch (Throwable t) {
        Log.e(TAG, "stop failed", t);
      }
      updateMotorStatus("STOP");
    });

    View openReceiver = v.findViewById(R.id.btn_hw_open_receiver);
    if (openReceiver != null) {
      openReceiver.setVisibility(View.GONE);
    }

    movementManager.setListener(new RobotMovementManager.Listener() {
      @Override
      public void onMovementStarted(int direction) {
        main.post(() -> updateMotorStatus("Moving: " + CommandParser.directionToString(direction)));
      }

      @Override
      public void onMovementStopped() {
        main.post(() -> updateMotorStatus("Idle"));
      }

      @Override
      public void onMovementError(String error) {
        main.post(() -> updateMotorStatus("Error: " + error));
      }
    });

    refreshStatusLine();
  }

  @Override
  public void onResume() {
    super.onResume();
    subscribeSensors();
    refreshStatusLine();
  }

  private void subscribeSensors() {
    try {
      sensorManager.init();
      sensorManager.addListener(this);
    } catch (Throwable t) {
      Log.w(TAG, "SensorDataManager init failed", t);
    }
  }

  @Override
  public void onPause() {
    try {
      sensorManager.removeListener(this);
    } catch (Throwable ignored) {
    }
    try {
      movementManager.stop();
    } catch (Throwable ignored) {
    }
    super.onPause();
  }

  private void onPrepare() {
    // Pause patrol before switching to MFG_TEST so we don't fight the autonomous navigator.
    if (getActivity() instanceof SupermarketActivity) {
      try {
        ((SupermarketActivity) getActivity()).getPatrolNavManager().pausePatrol();
      } catch (Throwable ignored) {
      }
    }
    try {
      movementManager.prepare();
      updateMotorStatus("Prepared (MFG_TEST, motors unlocked)");
      // Give the SDK a moment for MFG_TEST + motor enable to propagate before enabling direction
      // buttons. 400 ms is enough on the rk3288 robot we tested.
      main.postDelayed(() -> setDirectionButtonsEnabled(true), 400L);
    } catch (Throwable t) {
      Log.e(TAG, "prepare failed", t);
      Toast.makeText(getContext(), "Prepare failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  private void setDirectionButtonsEnabled(boolean enabled) {
    if (btnFront != null) btnFront.setEnabled(enabled);
    if (btnBack != null) btnBack.setEnabled(enabled);
    if (btnLeft != null) btnLeft.setEnabled(enabled);
    if (btnRight != null) btnRight.setEnabled(enabled);
  }

  private void onRelease() {
    setDirectionButtonsEnabled(false);
    try {
      movementManager.release();
      movementManager.setListener(null);
      updateMotorStatus("Released (AUTO mode, motors locked)");
    } catch (Throwable t) {
      Log.e(TAG, "release failed", t);
      Toast.makeText(getContext(), "Release failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  /**
   * "Tap to nudge": start the MotorDemo-style executor for {@link #BURST_MS} then stop. Requires
   * the user to have tapped Prepare first (direction buttons are disabled until then, and voice
   * commands auto-prepare via {@link com.keenon.peanut.supermarket.speech.VoiceCommandDispatcher}).
   */
  public void burst(int direction) {
    if (!movementManager.isPrepared()) {
      Toast.makeText(
              getContext(), "Tap Prepare first to unlock motors.", Toast.LENGTH_SHORT)
          .show();
      return;
    }
    try {
      // Re-assert the motor lock — the firmware or another activity may have released it
      // since prepare() was first tapped, in which case motor().manual() would silently fail.
      movementManager.ensureMotorEnabled();
      main.postDelayed(
          () -> {
            try {
              movementManager.executeDirection(direction);
              main.postDelayed(
                  () -> {
                    try {
                      movementManager.stop();
                    } catch (Throwable t) {
                      Log.e(TAG, "burst auto-stop failed", t);
                    }
                  },
                  BURST_MS);
            } catch (Throwable t) {
              Log.e(TAG, "executeDirection failed", t);
            }
          },
          120L);
    } catch (Throwable t) {
      Log.e(TAG, "burst failed", t);
      Toast.makeText(getContext(), "Move failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  /** Public entry point for voice-triggered charging — delegated to {@link VoiceCommandDispatcher}. */
  public void triggerCharging() {
    if (getActivity() instanceof SupermarketActivity) {
      try {
        ((SupermarketActivity) getActivity()).getPatrolNavManager().pausePatrol();
      } catch (Throwable ignored) {
      }
    }
    try {
      // Reuse the dispatcher's inline charge path so we don't launch a second activity.
      VoiceCommandDispatcher.dispatch((SupermarketActivity) getActivity(), "charging");
    } catch (Throwable t) {
      Log.e(TAG, "triggerCharging failed", t);
      Toast.makeText(getContext(), "Charge failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  private void refreshStatusLine() {
    if (motorStateLine == null) return;
    String status;
    if (!movementManager.isPrepared()) {
      status = "Status: not prepared — tap Prepare first";
    } else if (movementManager.isMoving()) {
      status =
          "Status: Moving "
              + CommandParser.directionToString(movementManager.getCurrentDirection());
    } else {
      status = "Status: Ready (tap a direction to nudge)";
    }
    motorStateLine.setText(status);
  }

  private void updateMotorStatus(String text) {
    if (motorStateLine != null) {
      motorStateLine.setText("Status: " + text);
    }
  }

  @Override
  public void onSensorDataUpdated(String jsonPayload) {
    if (getView() == null || !isAdded()) return;
    main.post(
        () -> {
          try {
            JSONObject root = new JSONObject(jsonPayload);
            JSONObject data = root.optJSONObject("data");
            if (data == null) return;

            int batt = data.optInt("battery_percent", -1);
            tvBattery.setText(batt >= 0 ? batt + "%" : "—");

            Object px = data.opt("position_x");
            Object py = data.opt("position_y");
            if (px != null && !JSONObject.NULL.equals(px) && py != null && !JSONObject.NULL.equals(py)) {
              tvPosition.setText(String.format("(%.2f, %.2f)", toDouble(px), toDouble(py)));
            } else {
              tvPosition.setText("—");
            }

            int motorState = data.optInt("motor_state", -1);
            tvMotor.setText(motorState >= 0 ? String.valueOf(motorState) : "—");

            int navState = data.optInt("nav_state", -1);
            tvNav.setText(navState >= 0 ? String.valueOf(navState) : "—");

            String sonar = data.optString("sonar_distance", "—");
            tvSonar.setText(sonar == null || sonar.isEmpty() ? "—" : sonar);

            String col = data.optString("collision_state", "—");
            tvCollision.setText(col);

            String charge = data.optString("charge_state", "—");
            tvCharge.setText(charge);

            String imu = data.optString("imu_summary", "—");
            tvImu.setText(imu);
          } catch (Exception ignored) {
          }
        });
  }

  private static double toDouble(Object o) {
    if (o instanceof Number) return ((Number) o).doubleValue();
    try {
      return Double.parseDouble(String.valueOf(o));
    } catch (Exception e) {
      return 0d;
    }
  }
}
