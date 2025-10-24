package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AccountSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AccountSettings";
    private static final String GET_ACCOUNT_INFO_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_account_info.php";
    private static final String UPDATE_ACCOUNT_INFO_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/update_account_info.php";

    private int userId;
    private ProgressDialog progressDialog;

    // Views
    private EditText etEmail, etCurrentPassword, etNewPassword, etConfirmPassword;
    private Button btnSaveChanges;
    private ImageView btnBack;
    private ImageView ivToggleCurrentPassword, ivToggleNewPassword, ivToggleConfirmPassword;

    // Data
    private String currentEmail;
    
    // Password visibility states
    private boolean isCurrentPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        // Get userId from intent
        userId = getIntent().getIntExtra("user_id", 0);

        initViews();
        setupClickListeners();
        loadAccountInfo();
    }

    private void initViews() {
        // Back button
        btnBack = findViewById(R.id.btnBack);

        // Email and password fields
        etEmail = findViewById(R.id.etEmail);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Password toggle icons
        ivToggleCurrentPassword = findViewById(R.id.ivToggleCurrentPassword);
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);

        // Save button
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSaveChanges.setOnClickListener(v -> saveAccountChanges());
        
        // Password toggle listeners
        ivToggleCurrentPassword.setOnClickListener(v -> toggleCurrentPasswordVisibility());
        ivToggleNewPassword.setOnClickListener(v -> toggleNewPasswordVisibility());
        ivToggleConfirmPassword.setOnClickListener(v -> toggleConfirmPasswordVisibility());
    }

    private void loadAccountInfo() {
        showProgressDialog("Loading account information...");

        StringRequest request = new StringRequest(Request.Method.POST, GET_ACCOUNT_INFO_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    hideProgressDialog();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            populateAccountData(jsonResponse);
                        } else {
                            Toast.makeText(AccountSettingsActivity.this, "Failed to load account info", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing account info response", e);
                        Toast.makeText(AccountSettingsActivity.this, "Error loading account info", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideProgressDialog();
                    Log.e(TAG, "Error loading account info", error);
                    Toast.makeText(AccountSettingsActivity.this, "Error loading account info", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void populateAccountData(JSONObject accountData) {
        try {
            // Set email
            currentEmail = accountData.optString("email", "");
            etEmail.setText(currentEmail);

        } catch (Exception e) {
            Log.e(TAG, "Error populating account data", e);
        }
    }

    private void saveAccountChanges() {
        String email = etEmail.getText().toString().trim();
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }

        if (!newPassword.isEmpty()) {
            if (currentPassword.isEmpty()) {
                etCurrentPassword.setError("Current password is required to change password");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }
            if (newPassword.length() < 6) {
                etNewPassword.setError("Password must be at least 6 characters");
                return;
            }
        }

        showProgressDialog("Saving changes...");

        // Debug logging
        Log.d(TAG, "Saving account changes:");
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "Current Password Length: " + currentPassword.length());
        Log.d(TAG, "New Password Length: " + newPassword.length());

        StringRequest request = new StringRequest(Request.Method.POST, UPDATE_ACCOUNT_INFO_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    hideProgressDialog();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(AccountSettingsActivity.this, "Account updated successfully", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            String error = jsonResponse.optString("error", "Failed to update account");
                            Toast.makeText(AccountSettingsActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing update response", e);
                        Toast.makeText(AccountSettingsActivity.this, "Error updating account", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideProgressDialog();
                    Log.e(TAG, "Error updating account", error);
                    Toast.makeText(AccountSettingsActivity.this, "Error updating account", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("email", email);
                params.put("current_password", currentPassword);
                params.put("new_password", newPassword);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }


    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    
    // Password visibility toggle methods
    private void toggleCurrentPasswordVisibility() {
        try {
            if (isCurrentPasswordVisible) {
                // Hide password - show dots
                etCurrentPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivToggleCurrentPassword.setImageResource(R.drawable.ic_password_hidden);
                isCurrentPasswordVisible = false;
            } else {
                // Show password - show text
                etCurrentPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivToggleCurrentPassword.setImageResource(R.drawable.ic_password_visible);
                isCurrentPasswordVisible = true;
            }
            etCurrentPassword.setSelection(etCurrentPassword.getText().length());
        } catch (Exception e) {
            Log.e(TAG, "Error toggling current password visibility", e);
        }
    }
    
    private void toggleNewPasswordVisibility() {
        try {
            if (isNewPasswordVisible) {
                // Hide password - show dots
                etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivToggleNewPassword.setImageResource(R.drawable.ic_password_hidden);
                isNewPasswordVisible = false;
            } else {
                // Show password - show text
                etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivToggleNewPassword.setImageResource(R.drawable.ic_password_visible);
                isNewPasswordVisible = true;
            }
            etNewPassword.setSelection(etNewPassword.getText().length());
        } catch (Exception e) {
            Log.e(TAG, "Error toggling new password visibility", e);
        }
    }
    
    private void toggleConfirmPasswordVisibility() {
        try {
            if (isConfirmPasswordVisible) {
                // Hide password - show dots
                etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_password_hidden);
                isConfirmPasswordVisible = false;
            } else {
                // Show password - show text
                etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_password_visible);
                isConfirmPasswordVisible = true;
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        } catch (Exception e) {
            Log.e(TAG, "Error toggling confirm password visibility", e);
        }
    }
}










