package com.keenon.peanut.supermarket.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.supermarket.SupermarketActivity;
import com.keenon.peanut.supermarket.backend.PromoSubmitter;
import com.keenon.peanut.supermarket.model.FeedbackPayload;
import com.keenon.peanut.supermarket.util.RobotDeviceId;
import com.keenon.peanut.supermarket.util.TtsHelper;

public class FeedbackFragment extends Fragment {

  private RatingBar ratingBar;
  private RadioGroup rgWouldBuy;
  private EditText etComments;
  private Button btnSubmit;
  private ProgressBar progressBar;
  private LinearLayout thankPanel;

  private PromoSubmitter promoSubmitter;
  private final TtsHelper ttsHelper = new TtsHelper();

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_feedback, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ratingBar = view.findViewById(R.id.rating_bar);
    rgWouldBuy = view.findViewById(R.id.rg_would_buy);
    etComments = view.findViewById(R.id.et_feedback_comments);
    btnSubmit = view.findViewById(R.id.btn_submit_feedback);
    progressBar = view.findViewById(R.id.progress_feedback);
    thankPanel = view.findViewById(R.id.feedback_thank_panel);
    btnSubmit.setOnClickListener(v -> onSubmit());
  }

  @Override
  public void onResume() {
    super.onResume();
    promoSubmitter = new PromoSubmitter(requireContext());
    ttsHelper.initialize(requireContext());
  }

  @Override
  public void onDestroyView() {
    ttsHelper.shutdown();
    super.onDestroyView();
  }

  private void onSubmit() {
    int stars = (int) ratingBar.getRating();
    if (stars < 1 || stars > 5) {
      Toast.makeText(requireContext(), "Please select a rating", Toast.LENGTH_SHORT).show();
      return;
    }
    String wouldBuy = "";
    int id = rgWouldBuy.getCheckedRadioButtonId();
    if (id == R.id.rb_yes) wouldBuy = "Yes";
    else if (id == R.id.rb_maybe) wouldBuy = "Maybe";
    else if (id == R.id.rb_no) wouldBuy = "No";

    String product = "";
    if (getActivity() instanceof SupermarketActivity) {
      product = ((SupermarketActivity) getActivity()).getProductConfig().getProductName();
    }

    String comments = etComments.getText().toString().trim();
    FeedbackPayload payload =
        new FeedbackPayload(
            product,
            stars,
            wouldBuy,
            comments,
            System.currentTimeMillis(),
            RobotDeviceId.get(requireContext()));

    btnSubmit.setEnabled(false);
    progressBar.setVisibility(View.VISIBLE);

    promoSubmitter.submitFeedback(
        payload,
        () ->
            requireActivity()
                .runOnUiThread(
                    () -> {
                      progressBar.setVisibility(View.GONE);
                      btnSubmit.setEnabled(true);
                      thankPanel.setVisibility(View.VISIBLE);
                      ttsHelper.speak("Thanks for your feedback!");
                      if (getActivity() instanceof SupermarketActivity) {
                        ((SupermarketActivity) getActivity()).enterConfirmationMode();
                      }
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
                      thankPanel.setVisibility(View.VISIBLE);
                      ttsHelper.speak("Thanks for your feedback!");
                      if (getActivity() instanceof SupermarketActivity) {
                        ((SupermarketActivity) getActivity()).enterConfirmationMode();
                      }
                    }));
  }
}
