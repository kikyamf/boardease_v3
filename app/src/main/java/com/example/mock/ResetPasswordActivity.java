package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private Button btnResetPassword;
    private ImageButton  btnToggleNewPassword, btnToggleConfirmPassword;

    private ImageView btnBack;
    private TextView tvError, tvSuccess;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private String resetToken;

    private static final String UPDATE_PASSWORD_URL = "https://boardease.calapebohol.com/update_password.php";
    private static final String VALIDATE_TOKEN_URL = "https://boardease.calapebohol.com/validate_reset_token.php";
    private static final String TAG = "ResetPassword";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);

        // Initialize views
        initializeViews();

        // Initialize Volley RequestQueue FIRST (before validateToken)
        requestQueue = Volley.newRequestQueue(this);

        // Get token from intent (deep link or regular intent)
        resetToken = getTokenFromIntent();
        
        if (resetToken == null || resetToken.isEmpty()) {
            showError("Invalid or missing reset token. Please request a new password reset link.");
            btnResetPassword.setEnabled(false);
        } else {
            // Validate token when activity opens
            validateToken();
        }

        // Set click listeners
        setClickListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnBack = findViewById(R.id.btnBack);
        btnToggleNewPassword = findViewById(R.id.btnToggleNewPassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);
        tvError = findViewById(R.id.tvError);
        tvSuccess = findViewById(R.id.tvSuccess);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Resetting password...");
        progressDialog.setCancelable(false);
    }

    /**
     * Extracts token from either regular intent extra or deep link intent
     */
    private String getTokenFromIntent() {
        Intent intent = getIntent();
        
        // First try to get token from regular intent extra
        String tokenFromExtra = intent.getStringExtra("token");
        if (tokenFromExtra != null) {
            Log.d(TAG, "Token from regular intent extra: " + tokenFromExtra);
            return tokenFromExtra;
        }
        
        // If not found, try to get from deep link intent
        Uri data = intent.getData();
        if (data != null) {
            Log.d(TAG, "Deep link data: " + data.toString());
            
            // Handle both custom scheme and https scheme
            if ("boardease".equals(data.getScheme()) && "reset-password".equals(data.getHost())) {
                String tokenFromQuery = data.getQueryParameter("token");
                if (tokenFromQuery != null) {
                    Log.d(TAG, "Token from custom scheme deep link: " + tokenFromQuery);
                    return tokenFromQuery;
                }
            } else if ("https".equals(data.getScheme()) && "boardease.app".equals(data.getHost()) && 
                     data.getPath() != null && data.getPath().startsWith("/reset-password")) {
                String tokenFromQuery = data.getQueryParameter("token");
                if (tokenFromQuery != null) {
                    Log.d(TAG, "Token from https deep link: " + tokenFromQuery);
                    return tokenFromQuery;
                }
            }
        }
        
        Log.w(TAG, "No token found in intent");
        return null;
    }

    private void setClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });

        btnToggleNewPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNewPasswordVisibility();
            }
        });

        btnToggleConfirmPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleConfirmPasswordVisibility();
            }
        });
    }

    private void toggleNewPasswordVisibility() {
        if (isNewPasswordVisible) {
            etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnToggleNewPassword.setImageResource(R.drawable.ic_password_hidden);
            isNewPasswordVisible = false;
        } else {
            etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnToggleNewPassword.setImageResource(R.drawable.ic_password_visible);
            isNewPasswordVisible = true;
        }
        etNewPassword.setSelection(etNewPassword.getText().length());
    }

    private void toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_password_hidden);
            isConfirmPasswordVisible = false;
        } else {
            etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_password_visible);
            isConfirmPasswordVisible = true;
        }
        etConfirmPassword.setSelection(etConfirmPassword.getText().length());
    }

    private void resetPassword() {
        // Get input values
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(newPassword)) {
            showError("New password is required");
            etNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 8) {
            showError("Password must be at least 8 characters long");
            etNewPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            showError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (resetToken == null || resetToken.isEmpty()) {
            showError("Invalid reset token. Please request a new password reset link.");
            return;
        }

        // Hide previous messages
        hideMessages();

        // Show progress dialog
        progressDialog.show();

        // Create request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, UPDATE_PASSWORD_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        Log.d(TAG, "Server response: " + response);
                        handleResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Volley error: " + error.getMessage());
                        Log.e(TAG, "Error details: " + error.toString());
                        
                        String errorMessage = "Network error. Please try again.";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data);
                                Log.e(TAG, "Response body: " + responseBody);
                                JSONObject errorJson = new JSONObject(responseBody);
                                if (errorJson.has("message")) {
                                    errorMessage = errorJson.getString("message");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing response: " + e.getMessage());
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
                params.put("token", resetToken);
                params.put("password", newPassword);
                params.put("confirm_password", confirmPassword);
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

        // Add request to queue
        requestQueue.add(stringRequest);
    }

    private void handleResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            boolean success = jsonObject.getBoolean("success");
            String message = jsonObject.getString("message");

            if (success) {
                // Success - show modal dialog with OK button
                showSuccessDialog(message);
                // Clear password fields
                etNewPassword.setText("");
                etConfirmPassword.setText("");
                // Disable button
                btnResetPassword.setEnabled(false);
            } else {
                // Error
                showError(message);
            }

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage());
            showError("Server response error. Please try again.");
        }
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

    /**
     * Shows a modal dialog for success message with OK button
     * When OK is clicked, navigates to Login activity
     */
    private void showSuccessDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Password Reset Successful")
            .setMessage(message)
            .setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();
                // Navigate to login activity
                Intent intent = new Intent(ResetPasswordActivity.this, Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setCancelable(false)
            .setIcon(R.drawable.ic_check_white)
            .show();
    }

    private void hideMessages() {
        tvError.setVisibility(View.GONE);
        tvSuccess.setVisibility(View.GONE);
    }

    /**
     * Validates the reset token when activity opens
     */
    private void validateToken() {
        if (resetToken == null || resetToken.isEmpty()) {
            return;
        }

        // Show loading state
        btnResetPassword.setEnabled(false);
        hideMessages();

        // Create validation request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, VALIDATE_TOKEN_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Token validation response: " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            boolean success = jsonObject.getBoolean("success");
                            boolean valid = jsonObject.getBoolean("valid");
                            String message = jsonObject.getString("message");

                            if (success && valid) {
                                // Token is valid - enable button
                                btnResetPassword.setEnabled(true);
                                hideMessages();
                            } else {
                                // Token is invalid/used/expired
                                showError(message);
                                btnResetPassword.setEnabled(false);
                                
                                // Navigate to login after 3 seconds
                                new android.os.Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent intent = new Intent(ResetPasswordActivity.this, Login.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                }, 3000);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            // If validation fails, still allow user to try (server will validate again)
                            btnResetPassword.setEnabled(true);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Token validation error: " + error.getMessage());
                        // If validation request fails, still allow user to try (server will validate)
                        btnResetPassword.setEnabled(true);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("token", resetToken);
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

        // Add request to queue
        requestQueue.add(stringRequest);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        
        // Handle new deep link intents
        String newToken = getTokenFromIntent();
        if (newToken != null && !newToken.equals(resetToken)) {
            Log.d(TAG, "New token from deep link: " + newToken);
            resetToken = newToken;
            // Validate the new token
            validateToken();
        }
    }
}

