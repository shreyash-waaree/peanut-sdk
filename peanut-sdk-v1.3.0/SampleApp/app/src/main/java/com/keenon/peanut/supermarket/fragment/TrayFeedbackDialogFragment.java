package com.keenon.peanut.supermarket.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.keenon.peanut.sample.R;

/**
 * Full-screen dialog shown when YOLO detects that an item was picked from the tray.
 *
 * The robot has already spoken and rotated before this dialog opens.
 * The user rates the item (1–5 stars) and taps Submit, or dismisses via the X button.
 */
public class TrayFeedbackDialogFragment extends DialogFragment {

    public static final String TAG = "TrayFeedbackDialog";

    private static final String ARG_PREV_COUNT = "prev_count";
    private static final String ARG_NEW_COUNT  = "new_count";

    public interface Callback {
        void onFeedbackSubmitted(int stars);
        void onFeedbackDismissed();
    }

    private Callback callback;
    private boolean callbackFired = false;

    public static TrayFeedbackDialogFragment newInstance(int previousCount, int newCount) {
        TrayFeedbackDialogFragment f = new TrayFeedbackDialogFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_PREV_COUNT, previousCount);
        b.putInt(ARG_NEW_COUNT, newCount);
        f.setArguments(b);
        return f;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_tray_feedback, container, false);

        View close = root.findViewById(R.id.feedback_close);
        View skip = root.findViewById(R.id.feedback_skip);
        View submit = root.findViewById(R.id.feedback_submit);
        RatingBar ratingBar = root.findViewById(R.id.feedback_rating);

        close.setOnClickListener(v -> dismiss());
        skip.setOnClickListener(v -> dismiss());
        submit.setOnClickListener(v -> {
            int stars = (int) ratingBar.getRating();
            if (stars < 1) {
                Toast.makeText(requireContext(),
                        R.string.feedback_select_star, Toast.LENGTH_SHORT).show();
                return;
            }
            callbackFired = true;
            if (callback != null) callback.onFeedbackSubmitted(stars);
            dismiss();
        });
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null && d.getWindow() != null) {
            d.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            d.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!callbackFired && callback != null) callback.onFeedbackDismissed();
        callbackFired = false;
    }
}
