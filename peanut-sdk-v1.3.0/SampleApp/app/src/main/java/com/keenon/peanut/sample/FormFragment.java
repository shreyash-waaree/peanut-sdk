package com.keenon.peanut.sample;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
public class FormFragment extends Fragment {

    private static final String TAG = FormFragment.class.getSimpleName();

    private EditText etName;
    private EditText etPhone;
    private EditText etDestination;
    private EditText etMessage;
    private EditText etBackendUrl;
    private Button btnSubmit;
    private TextView tvResult;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName = view.findViewById(R.id.et_name);
        etPhone = view.findViewById(R.id.et_phone);
        etDestination = view.findViewById(R.id.et_destination);
        etMessage = view.findViewById(R.id.et_message);
        etBackendUrl = view.findViewById(R.id.et_backend_url);
        btnSubmit = view.findViewById(R.id.btn_submit);
        tvResult = view.findViewById(R.id.tv_form_result);
        etBackendUrl.setText(BackendConfig.getBaseUrl(requireContext()));

        btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();
        String message = etMessage.getText().toString().trim();
        String backendInput = etBackendUrl.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(destination)) {
            etDestination.setError("Destination is required");
            etDestination.requestFocus();
            return;
        }

        String baseUrl = BackendConfig.normalizeBaseUrl(backendInput);
        if (TextUtils.isEmpty(baseUrl)) {
            etBackendUrl.setError("Backend URL/IP is required");
            etBackendUrl.requestFocus();
            return;
        }
        BackendConfig.setBaseUrl(requireContext(), baseUrl);
        String submitUrl = baseUrl + "/form-submit";

        submitToBackend(name, phone, destination, message, submitUrl);
    }

    private void submitToBackend(String name, String phone, String destination, String message, String submitUrl) {
        btnSubmit.setEnabled(false);
        tvResult.setVisibility(View.VISIBLE);
        tvResult.setText("Submitting to backend...\n" + submitUrl);
        new Thread(() -> {
            int code = -1;
            String response = "";
            try {
                JSONObject body = new JSONObject();
                body.put("name", name);
                body.put("phone", phone);
                body.put("destination", destination);
                body.put("message", message);
                body.put("submittedAt", String.valueOf(System.currentTimeMillis()));

                URL url = new URL(submitUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                }
                code = conn.getResponseCode();
                response = conn.getResponseMessage();
                conn.disconnect();

                final int finalCode = code;
                requireActivity().runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    if (finalCode >= 200 && finalCode < 300) {
                        tvResult.setText("Request submitted!\nRobot heading to: " + destination);
                        Toast.makeText(getActivity(), "Form submitted to backend successfully!", Toast.LENGTH_SHORT).show();
                        etName.setText("");
                        etPhone.setText("");
                        etDestination.setText("");
                        etMessage.setText("");
                    } else {
                        tvResult.setText("Submit failed (" + finalCode + "). Check backend server.");
                        Toast.makeText(getActivity(), "Backend rejected request.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Form submit backend error", e);
                final int finalCode = code;
                final String finalResp = response;
                requireActivity().runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    tvResult.setText("Submit failed. Cannot reach backend.\nURL: " + submitUrl);
                    Toast.makeText(getActivity(),
                            "Network/backend error (" + finalCode + " " + finalResp + ")",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "form-submit").start();
    }

}
