package com.keenon.peanut.supermarket.fragment;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.keenon.peanut.sample.BackendConfig;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.supermarket.SupermarketActivity;
import com.keenon.peanut.supermarket.backend.PromoSubmitter;
import com.keenon.peanut.supermarket.manager.PatrolNavManager;
import com.keenon.peanut.supermarket.model.InterestPayload;
import com.keenon.peanut.supermarket.util.RobotDeviceId;
import com.keenon.peanut.supermarket.util.TtsHelper;
import com.keenon.peanut.supermarket.util.ValidationUtil;

public class InterestFormFragment extends Fragment {

  private static final long IDLE_TIMEOUT_MS = 60_000;

  private EditText etBackendUrl;
  private EditText etName;
  private EditText etMobile;
  private EditText etEmail;
  private AutoCompleteTextView actContactTime;
  private EditText etQuantity;
  private EditText etComments;
  private MaterialButton btnSubmit;
  private ProgressBar progressBar;

  private static final String[] CONTACT_TIME_OPTIONS =
      new String[] {"Morning", "Afternoon", "Evening", "Any"};

  private PromoSubmitter promoSubmitter;
  private final TtsHelper ttsHelper = new TtsHelper();
  private final Handler idleHandler = new Handler(Looper.getMainLooper());
  private final Runnable clearFormRunnable = this::clearForm;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_interest_form, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    etBackendUrl = view.findViewById(R.id.et_backend_url);
    etName = view.findViewById(R.id.et_name);
    etMobile = view.findViewById(R.id.et_mobile);
    etEmail = view.findViewById(R.id.et_email);
    actContactTime = view.findViewById(R.id.act_contact_time);
    etQuantity = view.findViewById(R.id.et_quantity);
    etComments = view.findViewById(R.id.et_comments);
    btnSubmit = view.findViewById(R.id.btn_submit_interest);
    progressBar = view.findViewById(R.id.progress_interest);

    ArrayAdapter<String> timeAdapter =
        new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            CONTACT_TIME_OPTIONS);
    actContactTime.setAdapter(timeAdapter);
    actContactTime.setThreshold(0);
    setContactTimeText(CONTACT_TIME_OPTIONS[0]);

    etBackendUrl.setText(BackendConfig.getBaseUrl(requireContext()));

    View.OnTouchListener touch =
        (v, event) -> {
          if (event.getAction() == MotionEvent.ACTION_DOWN) {
            resetIdleTimer();
          }
          return false;
        };
    etBackendUrl.setOnTouchListener(touch);
    etName.setOnTouchListener(touch);
    etMobile.setOnTouchListener(touch);
    etEmail.setOnTouchListener(touch);
    etComments.setOnTouchListener(touch);
    actContactTime.setOnTouchListener(touch);
    etQuantity.setOnTouchListener(touch);

    btnSubmit.setOnClickListener(v -> onSubmitClick());
  }

  @Override
  public void onResume() {
    super.onResume();
    promoSubmitter = new PromoSubmitter(requireContext());
    ttsHelper.initialize(requireContext());
    startIdleTimer();
  }

  @Override
  public void onPause() {
    cancelIdleTimer();
    super.onPause();
  }

  @Override
  public void onDestroyView() {
    ttsHelper.shutdown();
    super.onDestroyView();
  }

  private void resetIdleTimer() {
    cancelIdleTimer();
    startIdleTimer();
    if (getActivity() instanceof SupermarketActivity) {
      ((SupermarketActivity) getActivity()).resetIdleTimer();
    }
  }

  private void startIdleTimer() {
    idleHandler.postDelayed(clearFormRunnable, IDLE_TIMEOUT_MS);
  }

  private void cancelIdleTimer() {
    idleHandler.removeCallbacks(clearFormRunnable);
  }

  private void clearForm() {
    etName.setText("");
    etMobile.setText("");
    etEmail.setText("");
    setContactTimeText(CONTACT_TIME_OPTIONS[0]);
    etQuantity.setText("1");
    etComments.setText("");
  }

  private void onSubmitClick() {
    String urlInput = etBackendUrl.getText().toString().trim();
    if (!urlInput.isEmpty()) {
      BackendConfig.setBaseUrl(requireContext(), urlInput);
    }
    if (!validateForm()) return;
    btnSubmit.setEnabled(false);
    progressBar.setVisibility(View.VISIBLE);

    String product = "";
    PatrolNavManager nav = null;
    if (getActivity() instanceof SupermarketActivity) {
      SupermarketActivity act = (SupermarketActivity) getActivity();
      product = act.getProductConfig().getProductName();
      nav = act.getPatrolNavManager();
    }
    String location = nav != null ? nav.getCurrentWaypointLabel() : "";

    int quantity = parseQuantity();
    InterestPayload payload =
        new InterestPayload(
            product,
            etName.getText().toString().trim(),
            etMobile.getText().toString().trim(),
            etEmail.getText().toString().trim(),
            actContactTime.getText() != null ? actContactTime.getText().toString().trim() : "",
            quantity,
            etComments.getText().toString().trim(),
            System.currentTimeMillis(),
            RobotDeviceId.get(requireContext()),
            location);

    promoSubmitter.submitInterest(
        payload,
        () ->
            requireActivity()
                .runOnUiThread(
                    () -> {
                      progressBar.setVisibility(View.GONE);
                      btnSubmit.setEnabled(true);
                      Toast.makeText(requireContext(), "Submitted successfully!", Toast.LENGTH_SHORT)
                          .show();
                      ttsHelper.speak("Thank you! Our team will reach out to you.");
                      if (getActivity() instanceof SupermarketActivity) {
                        ((SupermarketActivity) getActivity()).enterConfirmationMode();
                      }
                      clearForm();
                    }),
        msg ->
            requireActivity()
                .runOnUiThread(
                    () -> {
                      progressBar.setVisibility(View.GONE);
                      btnSubmit.setEnabled(true);
                      Toast.makeText(
                              requireContext(),
                              "Saved offline. Will retry shortly.",
                              Toast.LENGTH_LONG)
                          .show();
                      ttsHelper.speak("Thank you! Our team will reach out to you.");
                      if (getActivity() instanceof SupermarketActivity) {
                        ((SupermarketActivity) getActivity()).enterConfirmationMode();
                      }
                      clearForm();
                    }));
  }

  private boolean validateForm() {
    if (etName.getText().toString().trim().isEmpty()) {
      etName.setError("Name is required");
      return false;
    }
    if (!ValidationUtil.isValidMobile(etMobile.getText())) {
      etMobile.setError("Enter 10-digit number");
      return false;
    }
    if (!ValidationUtil.isValidEmail(etEmail.getText())) {
      etEmail.setError("Invalid email");
      return false;
    }
    int q = parseQuantity();
    if (q < 1 || q > 99) {
      etQuantity.setError("Enter 1–99");
      return false;
    }
    return true;
  }

  /**
   * Two-arg setText(CharSequence, boolean) exists from API 26; minSdk is 19 so we branch on API level.
   */
  private void setContactTimeText(String value) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      actContactTime.setText(value, false);
    } else {
      actContactTime.setText(value);
    }
  }

  /** Parses quantity; returns -1 if empty or invalid. */
  private int parseQuantity() {
    String s = etQuantity.getText().toString().trim();
    if (s.isEmpty()) {
      return -1;
    }
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}
