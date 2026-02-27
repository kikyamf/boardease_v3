package com.example.mock;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;

import android.app.DatePickerDialog;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.example.mock.VolleyMultipartRequest;
import com.example.mock.VolleyMultipartRequest.DataPart;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class RegistrationActivity extends AppCompatActivity {

    private android.widget.AutoCompleteTextView spinnerRole, spinnerSuffix;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private static final String WEB_CLIENT_ID = "663702982127-8dqon7abvp2udgt5j9em59kmdduq37oo.apps.googleusercontent.com"; // TODO: Use actual Web Client ID
    private Button btnGoogleSignUp, btnNext;

    private EditText etFirstName, etLastName, etMiddleName, etBirthDate, etPhone, etEmail, etPassword;
    private TextView tvLogin, tvEmailValidation;
    private ImageView ivPasswordToggle, backButton;
    private boolean isPasswordVisible = false;
    private TextView tvCriteriaLength, tvCriteriaUpper, tvCriteriaLower, tvCriteriaDigit, tvCriteriaSpecial;
    private CheckBox cbAgree;
    private TextView tvAgreeText;
    
    private boolean isPasswordCriteriaMet = false;
    private boolean isRegistering = false;
    private boolean isGoogleRegistration = false; // Flag to track if user is using Google
    private boolean isProgrammaticChange = false; // Flag to prevent listener triggers during auto-fill
    private Runnable validationRunnable;
    
    private TextView progressCircle1, progressCircle2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        initViews();
        setupProgressIndicator();
        setupGoogleSignIn();
        setupAdapters();
        setupClickListeners();
        setupFieldValidations();
    }

    private void initViews() {
        btnNext = findViewById(R.id.btnNext);
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerSuffix = findViewById(R.id.spinnerSuffix);
        
        etBirthDate = findViewById(R.id.etBirthDate);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle);
        backButton = findViewById(R.id.backButton);
        
        tvLogin = findViewById(R.id.tvLogin);
        tvEmailValidation = findViewById(R.id.tvEmailValidation);
        
        tvCriteriaLength = findViewById(R.id.tvCriteriaLength);
        tvCriteriaUpper = findViewById(R.id.tvCriteriaUpper);
        tvCriteriaLower = findViewById(R.id.tvCriteriaLower);
        tvCriteriaDigit = findViewById(R.id.tvCriteriaDigit);
        tvCriteriaSpecial = findViewById(R.id.tvCriteriaSpecial);
        cbAgree = findViewById(R.id.cbAgree);
        tvAgreeText = findViewById(R.id.tvAgreeText);
        progressCircle1 = findViewById(R.id.progressCircle1);
        progressCircle2 = findViewById(R.id.progressCircle2);
    }

    private void setupProgressIndicator() {
        progressCircle1.setBackgroundResource(R.drawable.progress_circle_filled);
        progressCircle1.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        progressCircle2.setBackgroundResource(R.drawable.progress_circle_hollow);
        progressCircle2.setTextColor(0xFF666666);
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignUpResult(task);
                    }
                }
        );
    }

    private void setupAdapters() {
        String[] roles = {"Boarder", "BH Owner"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, roles);
        roleAdapter.setDropDownViewResource(R.layout.dropdown_item);
        spinnerRole.setAdapter(roleAdapter);
        spinnerRole.setOnClickListener(v -> spinnerRole.showDropDown());
        spinnerRole.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                spinnerRole.showDropDown();
            }
        });

        String[] suffixes = {"None", "Jr.", "Sr.", "I", "II", "III", "IV", "V"};
        ArrayAdapter<String> suffixAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, suffixes);
        suffixAdapter.setDropDownViewResource(R.layout.dropdown_item);
        spinnerSuffix.setAdapter(suffixAdapter);
        spinnerSuffix.setOnClickListener(v -> spinnerSuffix.showDropDown());
        spinnerSuffix.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                spinnerSuffix.showDropDown();
            }
        });
    }

    private void setupClickListeners() {
        btnGoogleSignUp.setOnClickListener(v -> signUpWithGoogle());
        tvLogin.setOnClickListener(v -> handleBackNavigation());
        backButton.setOnClickListener(v -> handleBackNavigation());
        
        etBirthDate.setOnClickListener(v -> showDatePicker());
        
        btnNext.setOnClickListener(v -> {
            String error = validateAllFields();
            if (error != null) {
                showValidationErrorModal(error);
            } else {
                validateEmailAndProceed();
            }
        });

        setupTermsAndConditionsLink();
        setupPasswordToggle();
    }

    private void setupTermsAndConditionsLink() {
        String text = "I agree to the Terms and Privacy";
        android.text.SpannableString ss = new android.text.SpannableString(text);
        
        android.text.style.ClickableSpan termsClickableSpan = new android.text.style.ClickableSpan() {
            @Override
            public void onClick(@androidx.annotation.NonNull View widget) {
                showTermsDialog();
            }
            @Override
            public void updateDrawState(@androidx.annotation.NonNull android.text.TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(ContextCompat.getColor(RegistrationActivity.this, R.color.primary));
            }
        };

        // "Terms and Privacy" is from index 15 to 32
        ss.setSpan(termsClickableSpan, 15, 32, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        tvAgreeText.setText(ss);
        tvAgreeText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        tvAgreeText.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    private void showTermsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_terms_privacy, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        View btnCloseTerms = dialogView.findViewById(R.id.btnCloseTerms);
        View btnCloseTermsDialog = dialogView.findViewById(R.id.btnCloseTermsDialog);

        View.OnClickListener closeListener = v -> dialog.dismiss();
        
        if (btnCloseTerms != null) btnCloseTerms.setOnClickListener(closeListener);
        if (btnCloseTermsDialog != null) btnCloseTermsDialog.setOnClickListener(closeListener);

        dialog.show();
    }

    private void setupFieldValidations() {
        setupPasswordValidation();
        setupPhoneNumberField();
        setupNameFieldValidation(etFirstName, "First Name");
        setupNameFieldValidation(etLastName, "Last Name");
        setupNameFieldValidation(etMiddleName, "Middle Name");
        
        etEmail.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String email = s.toString().trim();
                
                // Skip if this is a programmatic change (e.g. from Google fill)
                if (isProgrammaticChange) return;

                // Reset Google flag if user manually edits the email
                if (isGoogleRegistration) {
                    isGoogleRegistration = false;
                    tvEmailValidation.setVisibility(View.GONE);
                }

                if (!email.isEmpty() && email.contains("@")) {
                    etEmail.removeCallbacks(validationRunnable);
                    validationRunnable = () -> validateEmailRealTime(email);
                    etEmail.postDelayed(validationRunnable, 1000);
                } else {
                    tvEmailValidation.setVisibility(View.GONE);
                }
            }
        });
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) - 18;
        DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
            String date = (m + 1) + "/" + d + "/" + y;
            etBirthDate.setText(date);
            validateBirthDate(date);
        }, year, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        
        calendar.set(Calendar.YEAR, year);
        dpd.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        dpd.show();
    }

    private void signUpWithGoogle() {
        // Sign out first to ensure the account picker always appears
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignUpResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String email = account.getEmail();
            
            // Check if this Google account already exists in the system
            checkIfGoogleAccountExists(email, account);
            
        } catch (ApiException e) {
            Log.w("GoogleSignUp", "signInResult:failed code=" + e.getStatusCode());
            showGoogleSignInErrorDialog("Google sign-in failed. Please try again.");
        }
    }
    
    private void checkIfGoogleAccountExists(String email, GoogleSignInAccount account) {
        String url = "https://boardease.calapebohol.com/check_google_email.php";
        
        // Show loading dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Verifying account...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    progressDialog.dismiss();
                    try {
                        org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
                        boolean exists = jsonResponse.getBoolean("exists");
                        
                        if (exists) {
                            // Account already exists - show modal with options
                            showAccountExistsDialog();
                        } else {
                            // New account - populate fields silently
                            populateFieldsFromGoogle(email, account);
                        }
                    } catch (org.json.JSONException e) {
                        Log.e("GoogleSignUp", "JSON parsing error: " + e.getMessage());
                        showGoogleSignInErrorDialog("Failed to verify account. Please try again.");
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Log.e("GoogleSignUp", "Network error: " + error.toString());
                    // Show error and don't proceed
                    showGoogleSignInErrorDialog("Unable to verify account. Please check your internet connection and try again.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                return params;
            }
        };
        
        // Set timeout to 10 seconds
        stringRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            10000,
            1,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }
    
    private void populateFieldsFromGoogle(String email, GoogleSignInAccount account) {
        isProgrammaticChange = true;
        isGoogleRegistration = true;
        
        etEmail.setText(email);
        etFirstName.setText(account.getGivenName());
        etLastName.setText(account.getFamilyName());
        
        isProgrammaticChange = false;

        // Google email is pre-verified, show a prominent badge
        tvEmailValidation.setVisibility(View.VISIBLE);
        tvEmailValidation.setText("✓ Signed in with Google (" + email + ")");
        tvEmailValidation.setTextColor(0xFF2E7D32); // Dark Green
        tvEmailValidation.setBackgroundColor(0xFFE8F5E9); // Light Green Background
        tvEmailValidation.setPadding(20, 10, 20, 10);
        
        checkSectionICompletion();
    }
    
    private void showAccountExistsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Account Already Exists")
            .setMessage("This Google account is already registered in the system.")
            .setPositiveButton("Choose Another Account", (dialog, which) -> {
                // Trigger Google sign-in again to choose different account
                signUpWithGoogle();
            })
            .setNegativeButton("Sign In Instead", (dialog, which) -> {
                // Navigate to Login activity
                Intent intent = new Intent(RegistrationActivity.this, Login.class);
                startActivity(intent);
                finish();
            })
            .setCancelable(false)
            .show();
    }
    
    private void showGoogleSignInErrorDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sign-In Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    /**
     * Validates email address and shows real-time feedback
     */
    private void validateEmailAndProceed() {
        if (isGoogleRegistration) {
            // Prominent logging to verify bypass
            Log.d("RegistrationFlow", "Google User detected. Proceeding to registration directly.");
            performRegistration();
            return;
        }

        String email = etEmail.getText().toString().trim();
        
        // Show validation message
        tvEmailValidation.setVisibility(View.VISIBLE);
        tvEmailValidation.setText("Validating email...");
        tvEmailValidation.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        
        // Make API call to validate email
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://boardease.calapebohol.com/validate_email_robust.php";
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
            response -> {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    boolean success = jsonResponse.getBoolean("success");
                    String message = jsonResponse.getString("message");
                    
                    if (success) {
                        // Email is valid - show success message and proceed
                        tvEmailValidation.setText("✓ " + message);
                        tvEmailValidation.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                        
                        // Proceed to registration after a short delay
                        etEmail.postDelayed(this::proceedToRegistration, 1000);
                    } else {
                        // Email validation failed - show error message
                        tvEmailValidation.setText("✗ " + message);
                        tvEmailValidation.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                    }
                } catch (JSONException e) {
                    tvEmailValidation.setText("✗ Server response error. Please try again.");
                    tvEmailValidation.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                }
            },
            error -> {
                tvEmailValidation.setText("✗ Network error. Please check your connection.");
                tvEmailValidation.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
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
     * Validates email in real-time as user types
     */
    private void validateEmailRealTime(String email) {
        // Show validation message
        tvEmailValidation.setVisibility(View.VISIBLE);
        tvEmailValidation.setText("Validating email...");
        tvEmailValidation.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        
        // Make API call to validate email
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://boardease.calapebohol.com/validate_email_robust.php";
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
            response -> {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    boolean success = jsonResponse.getBoolean("success");
                    String message = jsonResponse.getString("message");
                    
                    if (success) {
                        // Email is valid - show success message
                        tvEmailValidation.setText("✓ " + message);
                        tvEmailValidation.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                        // Check section completion
                        checkSectionIICompletion();
                    } else {
                        // Email validation failed - show error message
                        tvEmailValidation.setText("✗ " + message);
                        tvEmailValidation.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                    }
                } catch (JSONException e) {
                    tvEmailValidation.setText("✗ Server response error. Please try again.");
                    tvEmailValidation.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                }
            },
            error -> {
                tvEmailValidation.setText("✗ Network error. Please check your connection.");
                tvEmailValidation.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
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
     * Proceeds to registration after email validation
     */
    private void proceedToRegistration() {
        // Check if email validation was successful
        if (tvEmailValidation.getVisibility() != View.VISIBLE || 
            !tvEmailValidation.getText().toString().startsWith("✓")) {
            showValidationErrorModal("Please wait for email validation to complete.");
            return;
        }
        
        // All validations passed, proceed with registration
        performRegistration();
    }

    /**
     * Shows validation error in a modal dialog
     */
    private void showValidationErrorModal(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Validation Error");
        builder.setMessage(errorMessage);
        builder.setIcon(R.drawable.ic_alert_white);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(true);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Style the dialog
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(0xFF6200EE);
        }
    }
    
    /**
     * Validates all input fields
     */
    private String validateAllFields() {
        if (spinnerRole.getText().toString().isEmpty() || spinnerRole.getText().toString().equals("Select --")) 
            return "Please select a role";
        
        String firstName = etFirstName.getText().toString().trim();
        if (firstName.isEmpty()) return "First Name is required";
        
        String lastName = etLastName.getText().toString().trim();
        if (lastName.isEmpty()) return "Last Name is required";
        
        String birthDate = etBirthDate.getText().toString().trim();
        if (birthDate.isEmpty()) return "Birth Date is required";
        
        String phone = etPhone.getText().toString().trim();
        if (phone.isEmpty() || phone.equals("+63 ")) return "Phone Number is required";
        
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) return "Email is required";
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Invalid email format";
        
        if (!isPasswordCriteriaMet) return "Password does not meet requirements";
        
        if (!cbAgree.isChecked()) return "You must agree to the terms";
        
        return null; // Valid
    }
    
    /**
     * Sets up phone number field with fixed +63 prefix and formatting
     * Format: +63 9XX XXX YYYY
     */
    private void setupPhoneNumberField() {
        etPhone.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isFormatting = false;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isFormatting) return;
                
                isFormatting = true;
                String currentText = s.toString();
                
                // Extract only numbers
                String digitsOnly = currentText.replaceAll("[^0-9]", "");
                
                // Limit to 10 digits
                if (digitsOnly.length() > 10) {
                    digitsOnly = digitsOnly.substring(0, 10);
                }
                
                if (!currentText.equals(digitsOnly)) {
                    etPhone.setText(digitsOnly);
                    etPhone.setSelection(digitsOnly.length());
                }
                
                isFormatting = false;
                
                // Check section completion
                checkSectionIICompletion();
            }
        });
    }

    /**
     * Checks if Section I (Basic Info) is complete and reveals Section II (Contact & Security)
     */
    private void checkSectionICompletion() {
        // Check if all required fields in Section I are filled
        String selectedRole = spinnerRole.getText().toString();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        
        // Birth date validation: should match MM/DD/YYYY format
        boolean birthDateValid = birthDate.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$");
        boolean roleValid = !selectedRole.equals("Select --") && !selectedRole.isEmpty();
        boolean namesValid = !firstName.isEmpty() && !lastName.isEmpty();
        
        View step2Layout = findViewById(R.id.step2Layout);
        
        if (roleValid && namesValid && birthDateValid) {
            // Section I is complete, reveal Section II
            if (step2Layout.getVisibility() != View.VISIBLE) {
                step2Layout.setVisibility(View.VISIBLE);
                step2Layout.setAlpha(0f);
                step2Layout.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setListener(null);
                btnNext.setText("Register");
                updateProgressIndicator();
            }
        } else {
            // Section I is incomplete, hide Section II
            if (step2Layout.getVisibility() == View.VISIBLE) {
                step2Layout.setVisibility(View.GONE);
                btnNext.setText("Next");
                updateProgressIndicator();
            }
        }
    }

    /**
     * Checks if Section II (Contact & Security) is complete and enables Register button
     */
    private void checkSectionIICompletion() {
        // Check if all required fields in Section II are filled
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        boolean emailValid = !email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        // Phone validation: should have exactly 10 digits and start with 9
        boolean phoneValid = phone.length() == 10 && phone.startsWith("9");
        boolean passwordValid = isPasswordCriteriaMet;
        boolean agreed = cbAgree.isChecked();
        
        if (emailValid && phoneValid && passwordValid && agreed) {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1.0f);
            updateProgressIndicator();
        } else {
            // Only disable if we want to enforce completion before clicking
            // But usually we allow clicking to show validation errors
            // For now, let's keep it enabled but visual indicator could be useful
            // We'll rely on validateAllFields() on click
        }
    }

    private void updateProgressIndicator() {
        if (progressCircle1 == null || progressCircle2 == null) return;

        // Circle 1 is always filled
        progressCircle1.setBackgroundResource(R.drawable.progress_circle_filled);
        progressCircle1.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        
        View step2Layout = findViewById(R.id.step2Layout);
        if (step2Layout != null && step2Layout.getVisibility() == View.VISIBLE) {
            progressCircle2.setBackgroundResource(R.drawable.progress_circle_filled);
            progressCircle2.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        } else {
            progressCircle2.setBackgroundResource(R.drawable.progress_circle_hollow);
            progressCircle2.setTextColor(0xFF666666);
        }
    }
    
    /**
     * Sets up validation for name fields (First, Last, Middle)
     */
    private void setupNameFieldValidation(EditText editText, String fieldName) {
        if (editText == null) return;

        // Add input filter to restrict characters
        android.text.InputFilter[] filters = new android.text.InputFilter[] {
            new android.text.InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end,
                                         android.text.Spanned dest, int dstart, int dend) {
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);
                        if (!Character.isLetter(c) && c != ' ' && c != '-' && c != '\'') {
                            return "";
                        }
                    }
                    return null;
                }
            },
            new android.text.InputFilter.LengthFilter(50)
        };
        editText.setFilters(filters);
        
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String text = s.toString().trim();
                String error = validateNameField(text, fieldName, fieldName.equals("Middle Name"));
                
                // Check section completion
                if (fieldName.equals("First Name") || fieldName.equals("Last Name")) {
                    checkSectionICompletion();
                }
            }
        });
    }
    
    private String validateNameField(String text, String fieldName, boolean isOptional) {
        if (text.isEmpty()) {
            return isOptional ? null : fieldName + " is required";
        }
        if (text.length() < 2) return fieldName + " must be at least 2 characters";
        if (text.length() > 50) return fieldName + " must not exceed 50 characters";
        if (!text.matches("^[a-zA-Z\\s\\-']+$")) return fieldName + " can only contain letters, spaces, hyphens, and apostrophes";
        return null; // Valid
    }

    private void validateBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isEmpty()) {
            return;
        }
        if (!birthDate.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$")) {
            return;
        }
        
        try {
            String[] dateParts = birthDate.split("/");
            int month = Integer.parseInt(dateParts[0]);
            int day = Integer.parseInt(dateParts[1]);
            int year = Integer.parseInt(dateParts[2]);
            
            Calendar birthCalendar = Calendar.getInstance();
            birthCalendar.set(year, month - 1, day);
            Calendar now = Calendar.getInstance();
            int age = now.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR);
            if (now.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            
            if (age >= 18) {
                checkSectionICompletion();
            }
        } catch (Exception e) {
            // Invalid date format
        }
    }

    private void setupPasswordValidation() {
        etPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String password = s.toString();
                
                boolean lengthMet = password.length() >= 8;
                boolean upperMet = !password.equals(password.toLowerCase());
                boolean lowerMet = !password.equals(password.toUpperCase());
                boolean digitMet = password.matches(".*\\d.*");
                boolean specialMet = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
                
                updateCriteriaView(tvCriteriaLength, lengthMet);
                updateCriteriaView(tvCriteriaUpper, upperMet);
                updateCriteriaView(tvCriteriaLower, lowerMet);
                updateCriteriaView(tvCriteriaDigit, digitMet);
                updateCriteriaView(tvCriteriaSpecial, specialMet);
                
                isPasswordCriteriaMet = lengthMet && upperMet && lowerMet && digitMet && specialMet;
                checkSectionIICompletion();
            }
        });
    }
    
    private void updateCriteriaView(TextView view, boolean isMet) {
        if (view == null) return;
        if (isMet) {
            view.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            String text = view.getText().toString();
            if (!text.startsWith("✓ ")) {
                view.setText("✓ " + text.replace("• ", "").replace("✓ ", ""));
            }
        } else {
            view.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            String text = view.getText().toString();
            if (text.startsWith("✓ ")) {
                view.setText("• " + text.replace("✓ ", ""));
            }
        }
    }

    private void performRegistration() {
        if (isRegistering) return;
        isRegistering = true;
        btnNext.setEnabled(false);
        btnNext.setText("Registering...");

        final String isGoogleVal = isGoogleRegistration ? "1" : "0";

        String role = spinnerRole.getText().toString();
        String firstName = etFirstName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        String suffix = spinnerSuffix.getText().toString();
        String birthDate = etBirthDate.getText().toString().trim();
        String phoneInput = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        String UPLOAD_URL = "https://boardease.calapebohol.com/insert_registration_simplified.php";

        StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_URL,
            response -> {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.getBoolean("success")) {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_LONG).show();
                        
                        Intent intent;
                        if (isGoogleRegistration) {
                            intent = new Intent(this, Login.class);
                        } else {
                            intent = new Intent(this, EmailVerificationActivity.class);
                            intent.putExtra("email", email);
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show();
                        isRegistering = false;
                        btnNext.setEnabled(true);
                        btnNext.setText("Register Account");
                    }
                } catch (JSONException e) {
                    Toast.makeText(this, "Response error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isRegistering = false;
                    btnNext.setEnabled(true);
                    btnNext.setText("Register Account");
                }
            },
            error -> {
                isRegistering = false;
                btnNext.setEnabled(true);
                btnNext.setText("Register Account");
                Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("role", role);
                params.put("firstName", firstName);
                params.put("middleName", middleName);
                params.put("lastName", lastName);
                params.put("suffix", suffix);
                params.put("birthDate", birthDate);
                String phoneNumber = etPhone.getText().toString().trim();
                if (!phoneNumber.startsWith("+63 ") && !phoneNumber.isEmpty()) {
                    phoneNumber = "+63 " + phoneNumber;
                }
                params.put("phone", phoneNumber);
                params.put("email", email);
                params.put("password", password);
                params.put("is_google", isGoogleVal);
                return params;
            }
        };
        
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(30000, 3, com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(request);
    }

    private boolean hasAnyFieldFilled() {
        if (etFirstName.getText().length() > 0) return true;
        if (etLastName.getText().length() > 0) return true;
        if (etEmail.getText().length() > 0) return true;
        return false;
    }

    private void handleBackNavigation() {
        if (hasAnyFieldFilled()) {
            showExitConfirmationDialog();
        } else {
            navigateToLogin();
        }
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Exit Registration?")
            .setMessage("You have unsaved changes. Are you sure you want to exit?")
            .setPositiveButton("Exit", (dialog, which) -> navigateToLogin())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegistrationActivity.this, Login.class);
        startActivity(intent);
        finish();
    }

    private void setupPasswordToggle() {
        ivPasswordToggle.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivPasswordToggle.setImageResource(R.drawable.ic_eye);
            } else {
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivPasswordToggle.setImageResource(R.drawable.ic_eye_off);
            }
            etPassword.setSelection(etPassword.getText().length());
        });
    }
}
