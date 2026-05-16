package com.keenon.peanut.sample.chassis;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.constant.ApiConstants;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTouch;

public class MotorDemo extends BaseActivity {

  @BindView(R.id.tv_api_log)
  TextView tvApiLog;
  @BindView(R.id.sv_api_log)
  ScrollView svApiLog;
  boolean manualControlLongClicked, isRunning;
  private StringBuilder sb = new StringBuilder();
  private ScheduledThreadPoolExecutor executor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_motor);
    ButterKnife.bind(this);
    setButtonBack();

    initData();
  }

  @Override
  protected void onPause() {
    super.onPause();
    manualControlLongClicked = false;
  }

  @Override
  protected void onDestroy() {
    try {
      if (executor != null && !executor.isShutdown()) {
        executor.shutdownNow();
      }
    } catch (Throwable ignored) {
    }
    try {
      PeanutRuntime.getInstance().setWorkMode(ApiConstants.WorkMode.AUTO);
    } catch (Throwable ignored) {
    }
    try {
      PeanutSDK.getInstance().unSubscribe(TopicName.BOTTOM_RAW, bottomCallback);
    } catch (Throwable ignored) {
    }
    super.onDestroy();
  }

  IDataCallback bottomCallback = new IDataCallback() {
    @Override
    public void success(String result) {
      PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "raw data success = " + result);
    }

    @Override
    public void error(ApiError error) {
      PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "raw data error = " + error.toString());
    }
  };

  private void initData() {
    // step 1: switch work mode to factory mode
    try {
      PeanutRuntime.getInstance().setWorkMode(ApiConstants.WorkMode.MFG_TEST);
      PeanutSDK.getInstance().subscribe(TopicName.BOTTOM_RAW, bottomCallback);

      PeanutSDK.getInstance().motor().getState(new IDataCallback() {
        @Override
        public void success(String result) {
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "motor state success = " + result);
        }

        @Override
        public void error(ApiError error) {
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "motor state error = " + error.toString());
        }
      });
    } catch (Throwable t) {
      PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "initData failed: " + t.getMessage());
    }
  }

  IDataCallback motorCallback = new IDataCallback() {
    @Override
    public void success(String result) {
      PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "motor status success = " + result);
    }

    @Override
    public void error(ApiError error) {
      PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "motor status error = " + error.toString());
    }
  };

  @OnCheckedChanged({
      R.id.sw_motor_enable,
      R.id.sw_motor_hrc,
      R.id.cb_motor_status
  })
  public void onViewChecked(CompoundButton view, boolean isChecked) {
    switch (view.getId()) {
      case R.id.sw_motor_enable:
        if (isChecked) {
          PeanutSDK.getInstance().motor().enable(null, ApiConstants.MOTOR_ENABLE_UNLOCK);
        } else {
          PeanutSDK.getInstance().motor().enable(null, ApiConstants.MOTOR_ENABLE_LOCK);
        }
        break;
      case R.id.sw_motor_hrc:
        if (isChecked) {
          PeanutSDK.getInstance().motor().hrc(null, true);
        } else {
          PeanutSDK.getInstance().motor().hrc(null, false);
        }
        break;
      case R.id.cb_motor_status:
        if (isChecked) {
          PeanutSDK.getInstance().subscribe(TopicName.MOTOR_STATUS, motorCallback);
        } else {
          PeanutSDK.getInstance().unSubscribe(TopicName.MOTOR_STATUS,motorCallback);
        }
        break;
      default:
        break;
    }
  }

  @OnClick({
      R.id.btn_motor_encoder,
      R.id.btn_motor_speed,
      R.id.btn_motor_health})
  public void onViewClicked(View view) {
    switch (view.getId()) {
      case R.id.btn_motor_encoder:
        PeanutSDK.getInstance().motor().getEncoder(new IDataCallback() {
          @Override
          public void success(String response) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor encoder success = " + response);
          }

          @Override
          public void error(ApiError error) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor encoder error = " + error.toString());
          }
        });
        break;
      case R.id.btn_motor_speed:
        PeanutSDK.getInstance().motor().getSpeed(new IDataCallback() {
          @Override
          public void success(String response) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor speed success = " + response);
          }

          @Override
          public void error(ApiError error) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor speed error = " + error.toString());
          }
        });
        break;
      case R.id.btn_motor_health:
        PeanutSDK.getInstance().motor().getHealth(new IDataCallback() {
          @Override
          public void success(String response) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor health success = " + response);
          }

          @Override
          public void error(ApiError error) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor health error = " + error.toString());
          }
        });
        break;
      default:
        break;
    }
  }

  @OnTouch({R.id.btn_move_forward, R.id.btn_move_backward, R.id.btn_turn_right, R.id.btn_turn_left})
  public boolean onViewTouched(View view, MotionEvent event) {
    switch (view.getId()) {
      case R.id.btn_move_forward:
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          front();
          manualControlLongClicked = true;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "down info front ");
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
          if (executor != null) executor.shutdownNow();
          manualControlLongClicked = false;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "up info front ");
        }
        break;
      case R.id.btn_move_backward:
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          back();
          manualControlLongClicked = true;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "down info back ");
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
          if (executor != null) executor.shutdownNow();
          manualControlLongClicked = false;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "up info back ");
        }
        break;
      case R.id.btn_turn_right:
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          right();
          manualControlLongClicked = true;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "down info right ");
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
          if (executor != null) executor.shutdownNow();
          manualControlLongClicked = false;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "up info right ");
        }
        break;
      case R.id.btn_turn_left:
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          left();
          manualControlLongClicked = true;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "down info left ");
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
          if (executor != null) executor.shutdownNow();
          manualControlLongClicked = false;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "up info left ");
        }
        break;
      default:
        break;
    }
    return true;
  }


  private void front() {
    if (manualControlLongClicked) {
      return;
    }
    executor = new ScheduledThreadPoolExecutor(5);
    executor.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        PeanutSDK.getInstance().motor().manual(new IDataCallback() {
          @Override
          public void success(String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success = " + result);
          }

          @Override
          public void error(ApiError error) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front error = " + error.toString());
          }
        }, ApiConstants.MotorMove.FRONT);
      }
    }, 0, 100, TimeUnit.MILLISECONDS);
  }

  private void back() {
    if (manualControlLongClicked) {
      return;
    }
    executor = new ScheduledThreadPoolExecutor(5);
    executor.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        PeanutSDK.getInstance().motor().manual(new IDataCallback() {
          @Override
          public void success(String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success = " + result);
          }

          @Override
          public void error(ApiError error) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front error = " + error.toString());
          }
        }, ApiConstants.MotorMove.BACK);
      }
    }, 0, 100, TimeUnit.MILLISECONDS);
  }

  private void left() {
    if (manualControlLongClicked) {
      return;
    }
    executor = new ScheduledThreadPoolExecutor(5);
    executor.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        PeanutSDK.getInstance().motor().manual(new IDataCallback() {
          @Override
          public void success(String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success = " + result);
          }

          @Override
          public void error(ApiError error) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front error = " + error.toString());
          }
        }, ApiConstants.MotorMove.LEFT);
      }
    }, 0, 100, TimeUnit.MILLISECONDS);
  }

  private void right() {
    if (manualControlLongClicked) {
      return;
    }
    executor = new ScheduledThreadPoolExecutor(5);
    executor.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        PeanutSDK.getInstance().motor().manual(new IDataCallback() {
          @Override
          public void success(String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success = " + result);
          }

          @Override
          public void error(ApiError error) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front error = " + error.toString());
          }
        }, ApiConstants.MotorMove.RIGHT);
      }
    }, 0, 100, TimeUnit.MILLISECONDS);
  }

}
