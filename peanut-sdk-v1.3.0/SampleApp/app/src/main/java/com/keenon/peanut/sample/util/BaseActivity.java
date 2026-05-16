package com.keenon.peanut.sample.util;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.keenon.peanut.sample.DemoApplication;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);


  }

  protected void setButtonBack() {
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      ((ActionBar) actionBar).setHomeButtonEnabled(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  protected void showToast(final String message) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(DemoApplication.getAppContext(), message, Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        this.finish();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
