package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class EmailVerificationActivity extends AppCompatActivity {
    
    private EditText etCode1, etCode2, etCode3, etCode4, etCode5, etCode6;
    private Button btnVerify, btnResend;
    private TextView tvTimer, tvEmail;
    private CountDownTimer countDownTimer;
    private String email;
    private boolean isVerifying = false;
    private boolean isApprovalCheckerRunning = false;
    private android.os.Handler approvalHandler = new android.os.Handler();
    private Runnable approvalRunnable;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);
        
        // Get email from intent
        email = getIntent().getStringExtra("email");
        if (email == null) {
            Toast.makeText(this, "Email not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        setupClickListeners();
        startTimer();
    }
    
    private void initializeViews() {
        etCode1 = findViewById(R.id.etCode1);
        etCode2 = findViewById(R.id.etCode2);
        etCode3 = findViewById(R.id.etCode3);
        etCode4 = findViewById(R.id.etCode4);
        etCode5 = findViewById(R.id.etCode5);
        etCode6 = findViewById(R.id.etCode6);
        
        btnVerify = findViewById(R.id.btnVerify);
        btnResend = findViewById(R.id.btnResend);
        tvTimer = findViewById(R.id.tvTimer);
        tvEmail = findViewById(R.id.tvEmail);
        
        tvEmail.setText("Verification code sent to: " + email);
        
        // Set up code input listeners for auto-focus
        setupCodeInputListeners();
    }
    
    private void setupClickListeners() {
        btnVerify.setOnClickListener(v -> verifyCode());
        btnResend.setOnClickListener(v -> resendCode());
        
        // Set up back to registration button
        TextView tvBackToRegistration = findViewById(R.id.tvBackToRegistration);
        tvBackToRegistration.setOnClickListener(v -> {
            // Go back to registration screen - allows user to start over with different email
            Intent intent = new Intent(EmailVerificationActivity.this, RegistrationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
    
    private void setupCodeInputListeners() {
        // Set up auto-focus and input handling for code fields
        etCode1.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode2.requestFocus();
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        etCode2.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode3.requestFocus();
                } else if (s.length() == 0) {
                    etCode1.requestFocus();
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        etCode3.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode4.requestFocus();
                } else if (s.length() == 0) {
                    etCode2.requestFocus();
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        etCode4.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode5.requestFocus();
                } else if (s.length() == 0) {
                    etCode3.requestFocus();
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        etCode5.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode6.requestFocus();
                } else if (s.length() == 0) {
                    etCode4.requestFocus();
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        etCode6.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    etCode5.requestFocus();
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    private void clearCodeFields() {
        etCode1.setText("");
        etCode2.setText("");
        etCode3.setText("");
        etCode4.setText("");
        etCode5.setText("");
        etCode6.setText("");
        etCode1.requestFocus();
    }
    
    private void startTimer() {
        // 30 minutes = 1800000 milliseconds
        countDownTimer = new CountDownTimer(1800000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                tvTimer.setText(String.format("Time remaining: %02d:%02d", minutes, seconds));
            }
            
            @Override
            public void onFinish() {
                tvTimer.setText("Verification code expired");
                btnVerify.setEnabled(false);
                btnResend.setEnabled(false);
                Toast.makeText(EmailVerificationActivity.this, 
                    "Verification code has expired. Please register again.", 
                    Toast.LENGTH_LONG).show();
                
                // Navigate back to registration
                Intent intent = new Intent(EmailVerificationActivity.this, RegistrationActivity.class);
                startActivity(intent);
                finish();
            }
        };
        countDownTimer.start();
    }
    
    private void verifyCode() {
        if (isVerifying) {
            Toast.makeText(this, "Verification in progress, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get code from individual fields
        String verificationCode = etCode1.getText().toString() + 
                                 etCode2.getText().toString() + 
                                 etCode3.getText().toString() + 
                                 etCode4.getText().toString() + 
                                 etCode5.getText().toString() + 
                                 etCode6.getText().toString();
        
        if (verificationCode.isEmpty()) {
            Toast.makeText(this, "Please enter verification code", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (verificationCode.length() != 6) {
            Toast.makeText(this, "Verification code must be 6 digits", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isVerifying = true;
        btnVerify.setEnabled(false);
        btnVerify.setText("Verifying...");
        
        // Make API call to verify code
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/email_verification.php"; // Adjust URL as needed
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    isVerifying = false;
                    btnVerify.setEnabled(true);
                    btnVerify.setText("VERIFY");
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");
                        
                        if (success) {
                            // Show admin approval message
                            showAdminApprovalMessage();
                        } else {
                            Toast.makeText(EmailVerificationActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e("EmailVerification", "JSON parsing error: " + e.getMessage());
                        Toast.makeText(EmailVerificationActivity.this, "Server response error. Please try again.", Toast.LENGTH_LONG).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    isVerifying = false;
                    btnVerify.setEnabled(true);
                    btnVerify.setText("VERIFY");
                    
                    Log.e("EmailVerification", "Volley error: " + error.getMessage());
                    Toast.makeText(EmailVerificationActivity.this, "Network error. Please check your connection.", Toast.LENGTH_LONG).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "verify_code");
                params.put("email", email);
                params.put("verificationCode", verificationCode);
                return params;
            }
        };
        
        queue.add(request);
    }
    
    private void resendCode() {
        if (isVerifying) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isVerifying = true;
        btnResend.setEnabled(false);
        btnResend.setText("Sending...");
        
        // Make API call to resend code
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/email_verification.php"; // Adjust URL as needed
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d("EmailVerification", "=== RESEND CODE RESPONSE ===");
                    Log.d("EmailVerification", "Raw response: " + response);
                    
                    isVerifying = false;
                    btnResend.setEnabled(true);
                    btnResend.setText("RESEND CODE");
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");
                        
                        Log.d("EmailVerification", "Resend success: " + success);
                        Log.d("EmailVerification", "Resend message: " + message);
                        
                        Toast.makeText(EmailVerificationActivity.this, message, Toast.LENGTH_LONG).show();
                        
                        if (success) {
                            // Clear code fields and restart timer
                            clearCodeFields();
                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                            }
                            startTimer();
                        }
                    } catch (JSONException e) {
                        Log.e("EmailVerification", "JSON parsing error: " + e.getMessage());
                        Log.e("EmailVerification", "Raw response that failed to parse: " + response);
                        Toast.makeText(EmailVerificationActivity.this, "Server response error. Please try again.", Toast.LENGTH_LONG).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("EmailVerification", "=== RESEND CODE NETWORK ERROR ===");
                    Log.e("EmailVerification", "Volley error: " + error.getMessage());
                    Log.e("EmailVerification", "Error type: " + error.getClass().getSimpleName());
                    Log.e("EmailVerification", "Network response: " + (error.networkResponse != null ? error.networkResponse.toString() : "null"));
                    
                    isVerifying = false;
                    btnResend.setEnabled(true);
                    btnResend.setText("RESEND CODE");
                    
                    String errorMessage = "Network error. Please check your connection.";
                    if (error.networkResponse != null) {
                        errorMessage = "Server error: " + error.networkResponse.statusCode;
                    }
                    
                    Toast.makeText(EmailVerificationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "resend_code");
                params.put("email", email);
                return params;
            }
        };
        
        // Configure request with longer timeout for email sending
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                30000, // 30 seconds timeout
                2, // 2 retries
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        queue.add(request);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        // Stop approval checker
        stopApprovalChecker();
    }
    
    /**
     * Shows admin approval message and checks email for approval
     */
    private void showAdminApprovalMessage() {
        // Hide verification UI and show approval message
        findViewById(R.id.verificationContainer).setVisibility(View.GONE);
        findViewById(R.id.approvalContainer).setVisibility(View.VISIBLE);
        
        // Set up check approval button
        Button btnCheckApproval = findViewById(R.id.btnCheckApproval);
        btnCheckApproval.setOnClickListener(v -> checkApprovalStatus());
        
        // Start checking for approval every 30 seconds
        startApprovalChecker();
    }
    
    /**
     * Checks if account has been approved by admin
     */
    private void checkApprovalStatus() {
        Log.d("EmailVerification", "=== CHECKING APPROVAL STATUS ===");
        Log.d("EmailVerification", "Email: " + email);
        Log.d("EmailVerification", "Automatic checker running: " + isApprovalCheckerRunning);
        
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/check_approval_status.php"; // New endpoint to check approval
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d("EmailVerification", "Approval status response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean isApproved = jsonResponse.getBoolean("approved");
                        String message = jsonResponse.getString("message");
                        
                        Log.d("EmailVerification", "Approval result - Approved: " + isApproved + ", Message: " + message);
                        
                        if (isApproved) {
                            // Account approved, stop checking and navigate to login
                            Log.d("EmailVerification", "Account approved! Stopping automatic checker and navigating to login");
                            stopApprovalChecker();
                            Toast.makeText(EmailVerificationActivity.this, "Account approved! You can now login.", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(EmailVerificationActivity.this, Login.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();
                        } else {
                            // Still pending
                            Toast.makeText(EmailVerificationActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("EmailVerification", "JSON parsing error: " + e.getMessage());
                        Toast.makeText(EmailVerificationActivity.this, "Error checking approval status.", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("EmailVerification", "Error checking approval: " + error.getMessage());
                    Toast.makeText(EmailVerificationActivity.this, "Error checking approval status.", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                return params;
            }
        };
        
        queue.add(request);
    }
    
    /**
     * Starts automatic approval checking
     */
    private void startApprovalChecker() {
        if (isApprovalCheckerRunning) {
            Log.d("EmailVerification", "Approval checker already running, skipping start");
            return; // Already running
        }
        
        isApprovalCheckerRunning = true;
        Log.d("EmailVerification", "Starting automatic approval checker - will check every 30 seconds");
        
        // Create the runnable for automatic checking
        approvalRunnable = new Runnable() {
            @Override
            public void run() {
                if (isApprovalCheckerRunning) {
                    Log.d("EmailVerification", "Automatic approval check triggered");
                    checkApprovalStatus();
                    
                    // Schedule next check if still running
                    if (isApprovalCheckerRunning) {
                        approvalHandler.postDelayed(this, 30000); // 30 seconds
                        Log.d("EmailVerification", "Scheduled next automatic check in 30 seconds");
                    }
                }
            }
        };
        
        // Start the first check after 30 seconds
        approvalHandler.postDelayed(approvalRunnable, 30000);
        Log.d("EmailVerification", "First automatic check scheduled in 30 seconds");
    }
    
    /**
     * Stops automatic approval checking
     */
    private void stopApprovalChecker() {
        if (isApprovalCheckerRunning) {
            Log.d("EmailVerification", "Stopping automatic approval checker");
            isApprovalCheckerRunning = false;
            if (approvalRunnable != null) {
                approvalHandler.removeCallbacks(approvalRunnable);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to registration without verification
        Toast.makeText(this, "Please complete email verification or register again", Toast.LENGTH_SHORT).show();
    }
}
