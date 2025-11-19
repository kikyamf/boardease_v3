package com.example.mock;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendResetLink;
    private TextView tvError, tvSuccess, tvBackToLogin;
    private ImageView btnBack;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;

    private static final String FORGOT_PASSWORD_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/forgot_password.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        // Initialize views
        initializeViews();

        // Initialize Volley RequestQueue with custom timeout
        requestQueue = Volley.newRequestQueue(this);
        
        // Set default timeout to 30 seconds (email sending might take time)
        // This will be applied to all requests in this queue

        // Set click listeners
        setClickListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        tvError = findViewById(R.id.tvError);
        tvSuccess = findViewById(R.id.tvSuccess);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        btnBack = findViewById(R.id.btnBack);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending reset link...");
        progressDialog.setCancelable(false);
    }

    private void setClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSendResetLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPasswordReset();
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestPasswordReset() {
        // Get email
        String email = etEmail.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            showError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Hide previous messages
        hideMessages();

        // Show progress dialog
        progressDialog.show();

        // Create request with extended timeout
        StringRequest stringRequest = new StringRequest(Request.Method.POST, FORGOT_PASSWORD_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        Log.d("ForgotPassword", "Server response: " + response);
                        handleResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Log.e("ForgotPassword", "Volley error: " + error.getMessage());
                        Log.e("ForgotPassword", "Error details: " + error.toString());
                        
                        // Try to get response body for better error message
                        String errorMessage = "Network error. Please try again.";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data);
                                Log.e("ForgotPassword", "Response body: " + responseBody);
                                // Try to parse as JSON
                                JSONObject errorJson = new JSONObject(responseBody);
                                if (errorJson.has("message")) {
                                    errorMessage = errorJson.getString("message");
                                }
                            } catch (Exception e) {
                                Log.e("ForgotPassword", "Error parsing response: " + e.getMessage());
                            }
                        } else if (error.getMessage() != null) {
                            errorMessage = error.getMessage();
                        }
                        
                        showError(errorMessage);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                return params;
            }
            
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("ngrok-skip-browser-warning", "any");
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };
        
        // Set extended timeout (30 seconds) for email sending
        stringRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                30000, // 30 seconds timeout
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        // Add request to queue
        requestQueue.add(stringRequest);
    }

    private void handleResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            boolean success = jsonObject.getBoolean("success");
            String message = jsonObject.getString("message");
            
            // Check if emailExists field is present
            boolean emailExists = jsonObject.has("emailExists") && jsonObject.getBoolean("emailExists");

            if (success) {
                // Success - email exists and reset link sent
                showSuccessDialog("Email Sent", message);
                // Clear email field
                etEmail.setText("");
            } else {
                // Error - email doesn't exist or other error
                if (jsonObject.has("emailExists") && !emailExists) {
                    // Email doesn't exist - show in modal dialog
                    showErrorDialog("Email Not Found", message);
                } else {
                    // Other error - show in text view
                    showError(message);
                }
            }

        } catch (JSONException e) {
            Log.e("ForgotPassword", "JSON parsing error: " + e.getMessage());
            showError("Server response error. Please try again.");
        }
    }

    private void showSuccessDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    // Optionally go back to login
                    finish();
                })
                .setCancelable(false)
                .show();
    }
    
    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
        tvSuccess.setVisibility(View.GONE);
    }

    private void showSuccess(String message) {
        tvSuccess.setText(message);
        tvSuccess.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }

    private void hideMessages() {
        tvError.setVisibility(View.GONE);
        tvSuccess.setVisibility(View.GONE);
    }
}

