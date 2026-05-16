package com.keenon.peanut.sample.util;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class PrintLnLog {

  private static final String TAG = "PrintLnLog";

  public static void d(final Activity activity, final TextView tv, final ScrollView scrollView, final StringBuilder sb, final String info) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, info);
        sb.append(info);
        sb.append("\n");
        tv.setText(sb.toString());
        scrollView.post(new Runnable() {
          @Override
          public void run() {
            scrollView.fullScroll(View.FOCUS_DOWN);
          }
        });
      }
    });

  }
}
