package com.keenon.peanut.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.BaseAdapter;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.peanut.sample.base.BaseDemo;
import com.keenon.peanut.sample.chassis.ChassisList;
import com.keenon.peanut.sample.receiver.ReceiverActivity;

public class HomeFragment extends Fragment {

    private static final String TAG = HomeFragment.class.getSimpleName();

    private CheckBox cbLaser;
    private CheckBox cbLabel;
    private ListView mListView;

    private static final DemoInfo[] DEMOS = {
            new DemoInfo(R.drawable.info, R.string.demo_title_baselist, R.string.demo_desc_baselist, BaseDemo.class),
            new DemoInfo(R.drawable.chassis, R.string.demo_title_chassislist, R.string.demo_desc_chassislist, ChassisList.class),
            new DemoInfo(R.drawable.info, R.string.receiver_title, R.string.receiver_desc, ReceiverActivity.class)
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cbLaser = view.findViewById(R.id.cb_laser);
        cbLabel = view.findViewById(R.id.cb_label);
        mListView = view.findViewById(R.id.listView);

        SdkDemoHost activity = getActivity() instanceof SdkDemoHost ? (SdkDemoHost) getActivity() : null;
        if (activity != null) {
            if (PeanutConstants.REMOTE_LINK_PROXY.equals(activity.getType())) {
                cbLaser.setChecked(true);
                cbLabel.setChecked(false);
            } else {
                cbLaser.setChecked(false);
                cbLabel.setChecked(true);
            }
        }

        cbLaser.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbLabel.setChecked(false);
                SdkDemoHost host = getActivity() instanceof SdkDemoHost ? (SdkDemoHost) getActivity() : null;
                if (host != null) {
                    host.saveToSP(PeanutConstants.REMOTE_LINK_PROXY);
                    host.reinitSDK(PeanutConstants.REMOTE_LINK_PROXY);
                }
            }
        });

        cbLabel.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbLaser.setChecked(false);
                SdkDemoHost host = getActivity() instanceof SdkDemoHost ? (SdkDemoHost) getActivity() : null;
                if (host != null) {
                    host.saveToSP(PeanutConstants.LOCAL_LINK_PROXY);
                    host.reinitSDK(PeanutConstants.LOCAL_LINK_PROXY);
                }
            }
        });

        mListView.setAdapter(new DemoListAdapter());
        mListView.setOnItemClickListener((parent, v, index, id) -> {
            // Pause patrol (if running) before launching a chassis demo — patrol and MFG_TEST/charger
            // fight each other and cause crashes. The Supermarket host exposes pausePatrol via
            // SdkDemoHost; if we're inside the standalone KeenonApiDemoMain, this is a no-op.
            androidx.fragment.app.FragmentActivity act = getActivity();
            if (act instanceof SdkDemoHost) {
                try {
                    ((SdkDemoHost) act).pausePatrolIfActive();
                } catch (Throwable ignored) {
                }
            }
            Intent intent = new Intent(getActivity(), DEMOS[index].demoClass);
            startActivity(intent);
        });
    }

    private static class DemoInfo {
        final int image;
        final int title;
        final int desc;
        final Class<?> demoClass;

        DemoInfo(int image, int title, int desc, Class<?> demoClass) {
            this.image = image;
            this.title = title;
            this.desc = desc;
            this.demoClass = demoClass;
        }
    }

    private class DemoListAdapter extends BaseAdapter {
        @Override
        public int getCount() { return DEMOS.length; }

        @Override
        public Object getItem(int index) { return DEMOS[index]; }

        @Override
        public long getItemId(int id) { return id; }

        @Override
        public View getView(int index, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.item_demo, parent, false);
            }
            ImageView imageView = convertView.findViewById(R.id.image);
            TextView title = convertView.findViewById(R.id.title);
            TextView desc = convertView.findViewById(R.id.desc);
            imageView.setBackgroundResource(DEMOS[index].image);
            title.setText(DEMOS[index].title);
            desc.setText(DEMOS[index].desc);
            return convertView;
        }
    }
}
