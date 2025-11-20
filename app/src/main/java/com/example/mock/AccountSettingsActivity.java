package com.example.mock;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

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

public class AccountSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AccountSettings";
    private static final String GET_ACCOUNT_INFO_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_account_info.php";
    private static final String CHANGE_EMAIL_VERIFICATION_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/change_email_verification.php";
    private static final String UPDATE_PASSWORD_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/update_account_info.php";

    private int userId;
    private ProgressDialog progressDialog;

    // Email Change Views
    private TextView tvCurrentEmail;
    private EditText etEmailCurrentPassword, etNewEmail, etConfirmNewEmail;
    private ImageView ivToggleEmailCurrentPassword;
    private Button btnChangeEmail;

    // Password Change Views
    private EditText etPasswordCurrentPassword, etNewPassword, etConfirmPassword;
    private ImageView ivTogglePasswordCurrentPassword, ivToggleNewPassword, ivToggleConfirmPassword;
    private Button btnChangePassword;

    // Back button
    private ImageView btnBack;

    // Data
    private String currentEmail;
    private String pendingNewEmail; // Store new email for OTP verification
    
    // Password visibility states
    private boolean isEmailCurrentPasswordVisible = false;
    private boolean isPasswordCurrentPasswordVisible = false;
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

        // Email change views
        tvCurrentEmail = findViewById(R.id.tvCurrentEmail);
        etEmailCurrentPassword = findViewById(R.id.etEmailCurrentPassword);
        etNewEmail = findViewById(R.id.etNewEmail);
        etConfirmNewEmail = findViewById(R.id.etConfirmNewEmail);
        ivToggleEmailCurrentPassword = findViewById(R.id.ivToggleEmailCurrentPassword);
        btnChangeEmail = findViewById(R.id.btnChangeEmail);

        // Password change views
        etPasswordCurrentPassword = findViewById(R.id.etPasswordCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivTogglePasswordCurrentPassword = findViewById(R.id.ivTogglePasswordCurrentPassword);
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            setResult(RESULT_OK); // Set result to refresh profile data
            finish();
        });

        // Email change button
        btnChangeEmail.setOnClickListener(v -> handleEmailChange());

        // Password change button
        btnChangePassword.setOnClickListener(v -> handlePasswordChange());
        
        // Password toggle listeners
        ivToggleEmailCurrentPassword.setOnClickListener(v -> toggleEmailCurrentPasswordVisibility());
        ivTogglePasswordCurrentPassword.setOnClickListener(v -> togglePasswordCurrentPasswordVisibility());
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
            currentEmail = accountData.optString("email", "");
            tvCurrentEmail.setText(currentEmail);
        } catch (Exception e) {
            Log.e(TAG, "Error populating account data", e);
        }
    }

    // ========== EMAIL CHANGE FLOW ==========
    private void handleEmailChange() {
        String currentPassword = etEmailCurrentPassword.getText().toString().trim();
        String newEmail = etNewEmail.getText().toString().trim();
        String confirmNewEmail = etConfirmNewEmail.getText().toString().trim();

        // Validation
        if (currentPassword.isEmpty()) {
            etEmailCurrentPassword.setError("Current password is required");
            return;
        }

        if (newEmail.isEmpty()) {
            etNewEmail.setError("New email is required");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etNewEmail.setError("Invalid email format");
            return;
        }

        if (!newEmail.equals(confirmNewEmail)) {
            etConfirmNewEmail.setError("Emails do not match");
            return;
        }

        if (newEmail.equals(currentEmail)) {
            etNewEmail.setError("New email must be different from current email");
            return;
        }

        // Step 1: Send verification code
        sendEmailVerificationCode(currentPassword, newEmail);
    }

    private void sendEmailVerificationCode(String currentPassword, String newEmail) {
        // Disable button to prevent duplicate clicks
        btnChangeEmail.setEnabled(false);
        showProgressDialog("Sending verification code...");

        StringRequest request = new StringRequest(Request.Method.POST, CHANGE_EMAIL_VERIFICATION_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    hideProgressDialog();
                    btnChangeEmail.setEnabled(true);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            pendingNewEmail = newEmail;
                            showOTPVerificationDialog();
                        } else {
                            String error = jsonResponse.optString("error", "Failed to send verification code");
                            Toast.makeText(AccountSettingsActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing verification response", e);
                        Toast.makeText(AccountSettingsActivity.this, "Error sending verification code", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideProgressDialog();
                    btnChangeEmail.setEnabled(true);
                    Log.e(TAG, "Error sending verification code", error);
                    
                    // Check if it's a timeout error but email might have been sent
                    if (error instanceof com.android.volley.TimeoutError) {
                        // Email might have been sent successfully, show dialog anyway
                        pendingNewEmail = newEmail;
                        showOTPVerificationDialog();
                        Toast.makeText(AccountSettingsActivity.this, "Please check your email for the verification code", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(AccountSettingsActivity.this, "Error sending verification code", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                // Normalize email - lowercase and remove all whitespace (same as backend)
                String normalizedEmail = newEmail.trim().toLowerCase().replaceAll("\\s+", "");
                
                params.put("action", "send_verification_code");
                params.put("user_id", String.valueOf(userId));
                params.put("current_password", currentPassword);
                params.put("new_email", normalizedEmail);
                
                Log.d(TAG, "Sending verification code request - User ID: " + userId + ", Email: " + normalizedEmail + " (original: " + newEmail + ")");
                return params;
            }
        };

        // Set longer timeout (30 seconds)
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            30000, // 30 seconds timeout
            0, // no retries
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void showOTPVerificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_email_verification, null);
        builder.setView(dialogView);

        EditText etCode1 = dialogView.findViewById(R.id.etCode1);
        EditText etCode2 = dialogView.findViewById(R.id.etCode2);
        EditText etCode3 = dialogView.findViewById(R.id.etCode3);
        EditText etCode4 = dialogView.findViewById(R.id.etCode4);
        EditText etCode5 = dialogView.findViewById(R.id.etCode5);
        EditText etCode6 = dialogView.findViewById(R.id.etCode6);
        Button btnVerify = dialogView.findViewById(R.id.btnVerifyEmail);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelVerification);
        TextView tvMessage = dialogView.findViewById(R.id.tvVerificationMessage);

        tvMessage.setText("Enter the verification code sent to " + pendingNewEmail);

        EditText[] codeFields = {etCode1, etCode2, etCode3, etCode4, etCode5, etCode6};

        // Auto-focus next field
        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            codeFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < codeFields.length - 1) {
                        codeFields[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        btnVerify.setOnClickListener(v -> {
            StringBuilder code = new StringBuilder();
            for (EditText field : codeFields) {
                String digit = field.getText().toString().trim();
                // Only append if it's a single digit (0-9)
                if (digit.length() == 1 && Character.isDigit(digit.charAt(0))) {
                    code.append(digit);
                } else if (!digit.isEmpty()) {
                    // If field has multiple characters, take only the first digit
                    if (Character.isDigit(digit.charAt(0))) {
                        code.append(digit.charAt(0));
                    }
                }
            }

            String verificationCode = code.toString();
            Log.d(TAG, "Verification code entered: " + verificationCode + " (length: " + verificationCode.length() + ")");
            
            // Ensure code is exactly 6 digits, pad with zeros if needed
            if (verificationCode.length() < 6) {
                // Pad with leading zeros
                while (verificationCode.length() < 6) {
                    verificationCode = "0" + verificationCode;
                }
                Log.d(TAG, "Padded verification code: " + verificationCode);
            } else if (verificationCode.length() > 6) {
                // Take only first 6 digits
                verificationCode = verificationCode.substring(0, 6);
                Log.d(TAG, "Truncated verification code: " + verificationCode);
            }
            
            if (verificationCode.length() == 6) {
                verifyEmailCode(verificationCode, dialog);
            } else {
                Toast.makeText(this, "Please enter the complete verification code", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        etCode1.requestFocus();
    }

    private void verifyEmailCode(String code, AlertDialog dialog) {
        showProgressDialog("Verifying code...");

        StringRequest request = new StringRequest(Request.Method.POST, CHANGE_EMAIL_VERIFICATION_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    hideProgressDialog();
                    try {
                        // Log the raw response for debugging
                        Log.d(TAG, "Verification response (raw): " + response);
                        Log.d(TAG, "Verification response length: " + response.length());
                        
                        // Trim and clean the response
                        String cleanResponse = response.trim();
                        
                        // Check if response contains success indicators first (before parsing)
                        boolean hasSuccessIndicator = cleanResponse.contains("\"success\":true") || 
                                                     cleanResponse.contains("\"success\": true") ||
                                                     cleanResponse.contains("Email changed successfully");
                        
                        if (hasSuccessIndicator) {
                            Log.d(TAG, "Found success indicator in response");
                            dialog.dismiss();
                            showEmailChangeSuccessDialog();
                            return;
                        }
                        
                        // Try to extract JSON object(s) from response
                        // Find first complete JSON object
                        int jsonStart = cleanResponse.indexOf("{");
                        if (jsonStart < 0) {
                            // No JSON found, check if it's a success message
                            if (hasSuccessIndicator) {
                                dialog.dismiss();
                                showEmailChangeSuccessDialog();
                                return;
                            }
                            throw new JSONException("No JSON object found in response");
                        }
                        
                        // Find the matching closing brace
                        int braceCount = 0;
                        int jsonEnd = jsonStart;
                        for (int i = jsonStart; i < cleanResponse.length(); i++) {
                            char c = cleanResponse.charAt(i);
                            if (c == '{') braceCount++;
                            if (c == '}') braceCount--;
                            if (braceCount == 0) {
                                jsonEnd = i + 1;
                                break;
                            }
                        }
                        
                        String jsonString = cleanResponse.substring(jsonStart, jsonEnd);
                        Log.d(TAG, "Extracted JSON: " + jsonString);
                        
                        JSONObject jsonResponse = new JSONObject(jsonString);
                        boolean success = jsonResponse.optBoolean("success", false);
                        
                        Log.d(TAG, "Parsed success value: " + success);
                        
                        if (success) {
                            dialog.dismiss();
                            showEmailChangeSuccessDialog();
                        } else {
                            String error = jsonResponse.optString("error", "Invalid verification code");
                            Log.e(TAG, "Verification failed: " + error);
                            
                            // Check if email was actually updated by reloading account info
                            // Sometimes the response might be wrong but email was updated
                            checkIfEmailWasUpdated(dialog, pendingNewEmail);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing verification response", e);
                        Log.e(TAG, "Response that failed to parse: " + response);
                        
                        // Final fallback: if response contains any success indicator, treat as success
                        if (response.contains("Email changed successfully") || 
                            response.contains("\"success\":true") || 
                            response.contains("\"success\": true")) {
                            Log.d(TAG, "Fallback: Treating as success based on content");
                            dialog.dismiss();
                            showEmailChangeSuccessDialog();
                        } else {
                            // If email was actually updated (we can't verify here), 
                            // but response parsing failed, show generic error
                            Toast.makeText(AccountSettingsActivity.this, 
                                "Verification completed. Please check if your email was updated.", 
                                Toast.LENGTH_LONG).show();
                            // Still dismiss dialog and reload to check
                            dialog.dismiss();
                            loadAccountInfo();
                        }
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideProgressDialog();
                    Log.e(TAG, "Error verifying code", error);
                    Toast.makeText(AccountSettingsActivity.this, "Error verifying code", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                String trimmedCode = code.trim();
                // Normalize email - lowercase and remove all whitespace (same as backend)
                String normalizedEmail = pendingNewEmail.trim().toLowerCase().replaceAll("\\s+", "");
                
                // Remove any non-numeric characters from code
                String cleanCode = trimmedCode.replaceAll("[^0-9]", "");
                // Ensure it's 6 digits
                while (cleanCode.length() < 6) {
                    cleanCode = "0" + cleanCode;
                }
                if (cleanCode.length() > 6) {
                    cleanCode = cleanCode.substring(0, 6);
                }
                
                params.put("action", "verify_code");
                params.put("user_id", String.valueOf(userId));
                params.put("new_email", normalizedEmail);
                params.put("verification_code", cleanCode);
                
                Log.d(TAG, "Sending verification request - User ID: " + userId + ", Email: " + normalizedEmail + " (original: " + pendingNewEmail + "), Code: " + cleanCode + " (original: " + code + ", length: " + cleanCode.length() + ")");
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void checkIfEmailWasUpdated(AlertDialog otpDialog, String expectedNewEmail) {
        // Reload account info to check if email was actually updated
        StringRequest request = new StringRequest(Request.Method.POST, GET_ACCOUNT_INFO_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            String currentEmail = jsonResponse.optString("email", "").trim().toLowerCase();
                            String expectedEmail = expectedNewEmail.trim().toLowerCase();
                            
                            Log.d(TAG, "Checking email update - Current: " + currentEmail + ", Expected: " + expectedEmail);
                            
                            if (currentEmail.equals(expectedEmail)) {
                                // Email was actually updated! Show success
                                Log.d(TAG, "Email was updated successfully despite error response");
                                otpDialog.dismiss();
                                showEmailChangeSuccessDialog();
                            } else {
                                // Email wasn't updated, show the error
                                Toast.makeText(AccountSettingsActivity.this, "Invalid verification code", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(AccountSettingsActivity.this, "Invalid verification code", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error checking email update", e);
                        Toast.makeText(AccountSettingsActivity.this, "Invalid verification code", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Error checking email update", error);
                    Toast.makeText(AccountSettingsActivity.this, "Invalid verification code", Toast.LENGTH_SHORT).show();
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

    private void showEmailChangeSuccessDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Email Changed Successfully")
            .setMessage("Your email address has been successfully changed. You will receive a notification confirming this change.")
            .setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();
                // Clear all email change fields
                etEmailCurrentPassword.setText("");
                etNewEmail.setText("");
                etConfirmNewEmail.setText("");
                // Reload account info to show the new email
                loadAccountInfo();
            })
            .setCancelable(false)
            .show();
    }

    private void showPasswordChangeSuccessDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Password Changed Successfully")
            .setMessage("Your password has been successfully changed. You will receive a notification confirming this change.")
            .setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();
                // Clear all password change fields
                etPasswordCurrentPassword.setText("");
                etNewPassword.setText("");
                etConfirmPassword.setText("");
            })
            .setCancelable(false)
            .show();
    }

    // ========== PASSWORD CHANGE FLOW ==========
    private void handlePasswordChange() {
        String currentPassword = etPasswordCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (currentPassword.isEmpty()) {
            etPasswordCurrentPassword.setError("Current password is required");
            return;
        }

        if (newPassword.isEmpty()) {
            etNewPassword.setError("New password is required");
                return;
            }

            if (newPassword.length() < 6) {
                etNewPassword.setError("Password must be at least 6 characters");
                return;
            }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        updatePassword(currentPassword, newPassword);
    }

    private void updatePassword(String currentPassword, String newPassword) {
        showProgressDialog("Changing password...");

        StringRequest request = new StringRequest(Request.Method.POST, UPDATE_PASSWORD_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    hideProgressDialog();
                    try {
                        // Log the raw response for debugging
                        Log.d(TAG, "Password change response (raw): " + response);
                        Log.d(TAG, "Password change response length: " + response.length());
                        
                        // Trim and clean the response
                        String cleanResponse = response.trim();
                        
                        // Check if response contains error indicators first
                        boolean hasErrorIndicator = cleanResponse.contains("\"error\":\"Current password is incorrect\"") ||
                                                   cleanResponse.contains("\"error\": \"Current password is incorrect\"") ||
                                                   (cleanResponse.contains("\"success\":false") && cleanResponse.contains("Current password is incorrect"));
                        
                        if (hasErrorIndicator) {
                            Log.e(TAG, "Found error indicator in password change response");
                            // Check if password was actually updated by checking for success message too
                            boolean alsoHasSuccess = cleanResponse.contains("Password changed successfully") ||
                                                    cleanResponse.contains("\"success\":true");
                            
                            if (alsoHasSuccess) {
                                Log.w(TAG, "Response contains both error and success - password may have been updated");
                                // Still show success since password was updated
                                showPasswordChangeSuccessDialog();
                                return;
                            } else {
                                // Only error, show error
                                String error = "Current password is incorrect";
                                Toast.makeText(AccountSettingsActivity.this, error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        
                        // Check if response contains success indicators
                        boolean hasSuccessIndicator = cleanResponse.contains("\"success\":true") || 
                                                     cleanResponse.contains("\"success\": true") ||
                                                     cleanResponse.contains("Password changed successfully");
                        
                        if (hasSuccessIndicator) {
                            Log.d(TAG, "Found success indicator in password change response");
                            showPasswordChangeSuccessDialog();
                            return;
                        }
                        
                        // Try to extract JSON object from response
                        int jsonStart = cleanResponse.indexOf("{");
                        if (jsonStart < 0) {
                            // No JSON found, check if it's a success message
                            if (hasSuccessIndicator) {
                                showPasswordChangeSuccessDialog();
                                return;
                            }
                            throw new JSONException("No JSON object found in response");
                        }
                        
                        // Find the matching closing brace
                        int braceCount = 0;
                        int jsonEnd = jsonStart;
                        for (int i = jsonStart; i < cleanResponse.length(); i++) {
                            char c = cleanResponse.charAt(i);
                            if (c == '{') braceCount++;
                            if (c == '}') braceCount--;
                            if (braceCount == 0) {
                                jsonEnd = i + 1;
                                break;
                            }
                        }
                        
                        String jsonString = cleanResponse.substring(jsonStart, jsonEnd);
                        Log.d(TAG, "Extracted JSON: " + jsonString);
                        
                        JSONObject jsonResponse = new JSONObject(jsonString);
                        boolean success = jsonResponse.optBoolean("success", false);
                        
                        Log.d(TAG, "Parsed success value: " + success);
                        
                        if (success) {
                            showPasswordChangeSuccessDialog();
                        } else {
                            String error = jsonResponse.optString("error", "Failed to change password");
                            Log.e(TAG, "Password change failed: " + error);
                            Toast.makeText(AccountSettingsActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing password change response", e);
                        Log.e(TAG, "Response that failed to parse: " + response);
                        
                        // Final fallback: if response contains any success indicator, treat as success
                        if (response.contains("Password changed successfully") || 
                            response.contains("\"success\":true") || 
                            response.contains("\"success\": true")) {
                            Log.d(TAG, "Fallback: Treating password change as success based on content");
                            showPasswordChangeSuccessDialog();
                        } else {
                            Toast.makeText(AccountSettingsActivity.this, 
                                "Password change completed. Please verify if your password was updated.", 
                                Toast.LENGTH_LONG).show();
                        }
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideProgressDialog();
                    Log.e(TAG, "Error changing password", error);
                    Toast.makeText(AccountSettingsActivity.this, "Error changing password", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("current_password", currentPassword);
                params.put("new_password", newPassword);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    // ========== HELPER METHODS ==========
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
    private void toggleEmailCurrentPasswordVisibility() {
        try {
            if (isEmailCurrentPasswordVisible) {
                etEmailCurrentPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivToggleEmailCurrentPassword.setImageResource(R.drawable.ic_password_hidden);
                isEmailCurrentPasswordVisible = false;
            } else {
                etEmailCurrentPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivToggleEmailCurrentPassword.setImageResource(R.drawable.ic_password_visible);
                isEmailCurrentPasswordVisible = true;
            }
            etEmailCurrentPassword.setSelection(etEmailCurrentPassword.getText().length());
        } catch (Exception e) {
            Log.e(TAG, "Error toggling email current password visibility", e);
        }
    }

    private void togglePasswordCurrentPasswordVisibility() {
        try {
            if (isPasswordCurrentPasswordVisible) {
                etPasswordCurrentPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivTogglePasswordCurrentPassword.setImageResource(R.drawable.ic_password_hidden);
                isPasswordCurrentPasswordVisible = false;
            } else {
                etPasswordCurrentPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivTogglePasswordCurrentPassword.setImageResource(R.drawable.ic_password_visible);
                isPasswordCurrentPasswordVisible = true;
            }
            etPasswordCurrentPassword.setSelection(etPasswordCurrentPassword.getText().length());
        } catch (Exception e) {
            Log.e(TAG, "Error toggling password current password visibility", e);
        }
    }
    
    private void toggleNewPasswordVisibility() {
        try {
            if (isNewPasswordVisible) {
                etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivToggleNewPassword.setImageResource(R.drawable.ic_password_hidden);
                isNewPasswordVisible = false;
            } else {
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
                etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_password_hidden);
                isConfirmPasswordVisible = false;
            } else {
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











