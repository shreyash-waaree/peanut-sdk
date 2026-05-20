package com.keenon.peanut.supermarket.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.supermarket.vision.TrayCountingHelper;

/**
 * Developer "Count2" tab — OpenCV grid / absdiff tray counter (no ML).
 *
 * <p>UI and camera pipeline are implemented by {@link OpenCvTrayCountHelper}.</p>
 */
public class Count2Fragment extends Fragment {

    private static final int REQ_CAMERA = 2203;

    private OpenCvTrayCountHelper openCv;
    private RadioGroup gridGroup;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dev_count2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        openCv = new OpenCvTrayCountHelper(this, "TrayCount2OpenCV", REQ_CAMERA);
        openCv.bindViews(v);
        View bs = v.findViewById(R.id.btn_count_start);
        View bt = v.findViewById(R.id.btn_count_stop);
        if (bs != null) bs.setOnClickListener(x -> openCv.userRequestStart());
        if (bt != null) bt.setOnClickListener(x -> openCv.userStopPreview());

        gridGroup = v.findViewById(R.id.count_grid_group);
        if (gridGroup != null) {
            gridGroup.setOnCheckedChangeListener((group, checkedId) -> {
                TrayCountingHelper.BoxLayout layout = layoutForCheckedId(checkedId);
                if (layout != null) {
                    openCv.setUniformSlotLayout(layout);
                }
            });
        }
    }

    @Nullable
    private static TrayCountingHelper.BoxLayout layoutForCheckedId(int checkedId) {
        if (checkedId == R.id.count_grid_2x2) return TrayCountingHelper.BoxLayout.GRID_2x2;
        if (checkedId == R.id.count_grid_3x3) return TrayCountingHelper.BoxLayout.GRID_3x3;
        if (checkedId == R.id.count_grid_4x4) return TrayCountingHelper.BoxLayout.GRID_4x4;
        if (checkedId == R.id.count_grid_2x3) return TrayCountingHelper.BoxLayout.GRID_2x3;
        return null;
    }

    @Override
    public void onPause() {
        if (openCv != null) openCv.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (openCv != null) {
            openCv.destroy();
            openCv = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (openCv != null) {
            openCv.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
