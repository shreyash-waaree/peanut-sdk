package com.keenon.peanut.sample.chassis;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.DemoInfo;
import com.keenon.peanut.sample.util.DemoListAdapter;


public class ChassisList extends BaseActivity {

  private static final DemoInfo[] DEMOS = {
      new DemoInfo(R.string.demo_title_navigation, R.string.demo_desc_navigation, NavigationDemo.class),
      new DemoInfo(R.string.demo_title_charger, R.string.demo_desc_charger, ChargerDemo.class),
      new DemoInfo(R.string.demo_title_motor, R.string.demo_desc_motor, MotorDemo.class),
      new DemoInfo(R.string.demo_title_t8_light, R.string.demo_desc_t8_light, T8LightDemo.class),
      new DemoInfo(R.string.demo_desc_w3_door, R.string.demo_desc_w3_door, DoorDemo.class),
      new DemoInfo(R.string.demo_desc_t3_door, R.string.demo_desc_t3_door, T3DoorDemo.class),
      new DemoInfo(R.string.demo_title_map, R.string.demo_desc_map, MapDemo.class)
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_list);
    setButtonBack();
    ListView demoList = (ListView) findViewById(R.id.advanceList);
    demoList.setAdapter(new DemoListAdapter(ChassisList.this, DEMOS));
    demoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      public void onItemClick(AdapterView<?> arg0, View v, int index, long arg3) {
        onListItemClick(index);
      }
    });
  }

  void onListItemClick(int index) {
    Intent intent;
    intent = new Intent(this, DEMOS[index].demoClass);
    this.startActivity(intent);
  }
}