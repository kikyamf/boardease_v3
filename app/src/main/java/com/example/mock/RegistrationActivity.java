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
import android.widget.EditText;
import android.widget.ImageView;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class RegistrationActivity extends AppCompatActivity {

    Spinner spinnerRole, spinnerSuffix, spinnerProvince, spinnerMunicipality;

    EditText etFirstName, etLastName, etMiddleName, etBirthDate, etPhone, etAddress, etDetailedAddress, etBarangay, etEmail, etPassword, etGcashNum;
    TextView tvLogin, tvEmailValidation;
    ImageView UploadQr, ivTogglePassword;
    Button btnNext;
    boolean isPasswordVisible = false;

    private Uri selectedQrUri; // store the selected image URI
    private Runnable validationRunnable; // for real-time email validation
    
    // Section cards for progressive reveal
    private com.google.android.material.card.MaterialCardView sectionAccountType;
    private com.google.android.material.card.MaterialCardView sectionPersonalInfo;
    private com.google.android.material.card.MaterialCardView sectionAddress;
    private com.google.android.material.card.MaterialCardView sectionLoginCredentials;
    private com.google.android.material.card.MaterialCardView sectionPaymentInfo;
    
    // Progress indicator circles
    private ImageView progressCircle1;
    private ImageView progressCircle2;
    private ImageView progressCircle3;
    private ImageView progressCircle4;
    private ImageView progressCircle5;
    
    // Address picker data
    private String selectedProvince = "";
    private String selectedMunicipality = "";
    private String selectedBarangay = "";
    private String selectedDetailedAddress = "";

    // Launcher to pick image from gallery
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            verifyAndSetQrImage(uri);
                        }
                    });

    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        btnNext = findViewById(R.id.btnNext);

        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerSuffix = findViewById(R.id.spinnerSuffix);
        spinnerProvince = findViewById(R.id.spinnerProvince);
        spinnerMunicipality = findViewById(R.id.spinnerMunicipality);

        etBirthDate = findViewById(R.id.etBirthDate);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etBirthDate = findViewById(R.id.etBirthDate);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        etDetailedAddress = findViewById(R.id.etDetailedAddress);
        etBarangay = findViewById(R.id.etBarangay);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etGcashNum = findViewById(R.id.etGcashNum);

        tvLogin = findViewById(R.id.tvLogin);
        tvEmailValidation = findViewById(R.id.tvEmailValidation);

        UploadQr = findViewById(R.id.UploadQr);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        
        // Get section cards
        sectionAccountType = findViewById(R.id.sectionAccountType);
        sectionPersonalInfo = findViewById(R.id.sectionPersonalInfo);
        sectionAddress = findViewById(R.id.sectionAddress);
        sectionLoginCredentials = findViewById(R.id.sectionLoginCredentials);
        sectionPaymentInfo = findViewById(R.id.sectionPaymentInfo);
        
        // Get progress indicator circles
        progressCircle1 = findViewById(R.id.progressCircle1);
        progressCircle2 = findViewById(R.id.progressCircle2);
        progressCircle3 = findViewById(R.id.progressCircle3);
        progressCircle4 = findViewById(R.id.progressCircle4);
        progressCircle5 = findViewById(R.id.progressCircle5);
        
        // Initialize progress indicator (Circle 1 is filled by default)
        updateProgressIndicator();
        
        // Setup back button
        ImageView backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                // Go back to Login activity
                Intent intent = new Intent(RegistrationActivity.this, Login.class);
                startActivity(intent);
                finish();
            });
        }

        // Add real-time email validation
        etEmail.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String email = s.toString().trim();
                
                // Only validate if email is not empty and contains @
                if (!email.isEmpty() && email.contains("@")) {
                    // Add a small delay to avoid too many API calls
                    etEmail.removeCallbacks(validationRunnable);
                    etEmail.postDelayed(validationRunnable, 1000); // 1 second delay
                } else if (email.isEmpty()) {
                    // Clear validation message if email is empty
                    tvEmailValidation.setVisibility(View.GONE);
                }
                
                // Check section IV completion
                checkSectionIVCompletion();
            }
        });

        // Initialize validation runnable
        validationRunnable = new Runnable() {
            @Override
            public void run() {
                String email = etEmail.getText().toString().trim();
                if (!email.isEmpty() && email.contains("@")) {
                    validateEmailRealTime(email);
                }
            }
        };

        // Create a list of choices
        String[] roles = {"Select --", "Boarder", "BH Owner"};

        // Set adapter with custom styling
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                roles
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(0xFF000000); // Black text color for selected item
                }
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(0xFFFFFFFF); // White text color
                    textView.setTextSize(16);
                    textView.setPadding(16, 16, 16, 16);
                    textView.setBackgroundColor(0xFF2C2C2C); // Dark gray background
                } else {
                    TextView textView = view.findViewById(android.R.id.text1);
                    if (textView != null) {
                        textView.setTextColor(0xFFFFFFFF);
                        textView.setTextSize(16);
                        textView.setPadding(16, 16, 16, 16);
                    }
                    view.setBackgroundColor(0xFF2C2C2C);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        // Setup role spinner listener to show/hide GCash fields and check section completion
        spinnerRole.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedRole = parent.getItemAtPosition(position).toString();
                android.widget.LinearLayout llGcash = findViewById(R.id.llGcash);
                
                if ("Boarder".equals(selectedRole)) {
                    // Hide GCash fields for Boarder
                    llGcash.setVisibility(android.view.View.GONE);
                } else {
                    // Show GCash fields for BH Owner or other roles
                    llGcash.setVisibility(android.view.View.VISIBLE);
                }
                
                // Check if section I is complete and reveal section II
                checkSectionICompletion();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Show GCash fields by default if nothing selected
                android.widget.LinearLayout llGcash = findViewById(R.id.llGcash);
                llGcash.setVisibility(android.view.View.VISIBLE);
            }
        });

        // Setup suffix spinner
        String[] suffixes = {"None", "Jr.", "Sr.", "I", "II", "III", "IV", "V"};

        ArrayAdapter<String> suffixAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                suffixes
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(0xFF000000); // Black text color for selected item
                }
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(0xFFFFFFFF); // White text color
                    textView.setTextSize(16);
                    textView.setPadding(16, 16, 16, 16);
                    textView.setBackgroundColor(0xFF2C2C2C); // Dark gray background
                } else {
                    TextView textView = view.findViewById(android.R.id.text1);
                    if (textView != null) {
                        textView.setTextColor(0xFFFFFFFF);
                        textView.setTextSize(16);
                        textView.setPadding(16, 16, 16, 16);
                    }
                    view.setBackgroundColor(0xFF2C2C2C);
                }
                return view;
            }
        };
        suffixAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSuffix.setAdapter(suffixAdapter);

        // Initialize address picker
        initializeAddressPicker();
        
        // Setup phone number field with fixed +63 prefix and formatting
        setupPhoneNumberField();

        //Set the calendar for the birthdate
        etBirthDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        RegistrationActivity.this,
                        (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                            // Format: MM/DD/YYYY
                            String date = (selectedMonth + 1) + "/" + selectedDay + "/" + selectedYear;
                            etBirthDate.setText(date);
                            // Check section II completion after birth date is set
                            checkSectionIICompletion();
                        },
                        year, month, day
                );

                // Optional: restrict future dates (no selecting birth date in future)
                datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

                datePickerDialog.show();
            }
        });
        
        // Setup listeners for section II fields to check completion
        etFirstName.addTextChangedListener(createSectionIICheckListener());
        etLastName.addTextChangedListener(createSectionIICheckListener());
        etPhone.addTextChangedListener(createSectionIICheckListener());
        
        // Setup listeners for section III fields to check completion
        etBarangay.addTextChangedListener(createSectionIIICheckListener());
        etDetailedAddress.addTextChangedListener(createSectionIIICheckListener());
        
        // Setup listeners for section IV fields to check completion
        etPassword.addTextChangedListener(createSectionIVCheckListener());
        
        // Setup listener for section V fields to check completion
        etGcashNum.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkSectionVCompletion();
            }
        });

        String role = spinnerRole.getSelectedItem().toString();

        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String gcashNum = etGcashNum.getText().toString().trim();

        // When ImageView is clicked → open gallery
        UploadQr.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*"); // open only image files
        });

        // Password toggle click listener
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Hide password
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_eye_closed);
                isPasswordVisible = false;
            } else {
                // Show password
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_eye_open);
                isPasswordVisible = true;
            }
            // Move cursor to end of text
            etPassword.setSelection(etPassword.getText().length());
        });

        //Button next for proceeding to the next Activity
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Validate all fields
                String validationError = validateAllFields();
                if (validationError != null && !validationError.isEmpty()) {
                    showValidationErrorModal(validationError);
                    return;
                }

                // If validation passed, check email validation status
                String email = etEmail.getText().toString().trim();
                Log.d("RegistrationActivity", "=== NEXT BUTTON CLICKED ===");
                Log.d("RegistrationActivity", "Email: " + email);
                Log.d("RegistrationActivity", "Validation visibility: " + (tvEmailValidation.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                
                if (tvEmailValidation.getVisibility() == View.VISIBLE) {
                    String validationText = tvEmailValidation.getText().toString();
                    Log.d("RegistrationActivity", "Validation text: " + validationText);
                    
                    if (validationText.startsWith("✓")) {
                        // Email is already validated, proceed directly
                        Log.d("RegistrationActivity", "Email validation PASSED - proceeding to next activity");
                        proceedToNextActivity();
                    } else if (validationText.startsWith("✗")) {
                        // Email validation failed, show error
                        Log.d("RegistrationActivity", "Email validation FAILED - blocking user");
                        showValidationErrorModal("Please fix the email issue before proceeding. The email address you entered is invalid or already in use.");
                    } else {
                        // Still validating, wait
                        Log.d("RegistrationActivity", "Email validation IN PROGRESS - waiting");
                        showValidationErrorModal("Please wait for email validation to complete.");
                    }
                } else {
                    // No validation yet, validate email first
                    Log.d("RegistrationActivity", "No validation yet - starting email validation");
                    validateEmailAndProceed();
                }
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent a = new Intent(RegistrationActivity.this, Login.class);
                startActivity(a);
            }
        });
    }

    /**
     * Validates email address and shows real-time feedback
     */
    private void validateEmailAndProceed() {
        String email = etEmail.getText().toString().trim();
        Log.d("RegistrationActivity", "=== VALIDATE EMAIL AND PROCEED ===");
        Log.d("RegistrationActivity", "Email to validate: " + email);
        
        // Show validation message
        tvEmailValidation.setVisibility(View.VISIBLE);
        tvEmailValidation.setText("Validating email...");
        tvEmailValidation.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        
        // Make API call to validate email
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/validate_email_robust.php";
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d("RegistrationActivity", "=== EMAIL VALIDATION RESPONSE ===");
                    Log.d("RegistrationActivity", "Raw response: " + response);
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");
                        
                        Log.d("RegistrationActivity", "Parsed success: " + success);
                        Log.d("RegistrationActivity", "Parsed message: " + message);
                        
                        if (success) {
                            // Email is valid - show success message and proceed
                            Log.d("RegistrationActivity", "Email validation SUCCESS - proceeding");
                            tvEmailValidation.setText("✓ " + message);
                            tvEmailValidation.setTextColor(getResources().getColor(android.R.color.black));
                            
                            // Proceed to next activity after a short delay
                            etEmail.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("RegistrationActivity", "Delayed proceedToNextActivity called");
                                    proceedToNextActivity();
                                }
                            }, 1000); // 1 second delay to show the success message
                        } else {
                            // Email validation failed - show error message
                            Log.d("RegistrationActivity", "Email validation FAILED - blocking user");
                            tvEmailValidation.setText("✗ " + message);
                            tvEmailValidation.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        }
                    } catch (JSONException e) {
                        Log.e("RegistrationActivity", "JSON parsing error: " + e.getMessage());
                        Log.e("RegistrationActivity", "Raw response that failed to parse: " + response);
                        tvEmailValidation.setText("✗ Server response error. Please try again.");
                        tvEmailValidation.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("RegistrationActivity", "Volley error: " + error.getMessage());
                    tvEmailValidation.setText("✗ Network error. Please check your connection.");
                    tvEmailValidation.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
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
     * Validates email in real-time as user types
     */
    private void validateEmailRealTime(String email) {
        // Show validation message
        tvEmailValidation.setVisibility(View.VISIBLE);
        tvEmailValidation.setText("Validating email...");
        tvEmailValidation.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        
        // Make API call to validate email
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/validate_email_robust.php";
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");
                        
                        if (success) {
                            // Email is valid - show success message
                            tvEmailValidation.setText("✓ " + message);
                            tvEmailValidation.setTextColor(getResources().getColor(android.R.color.black));
                            // Check section IV completion after email validation
                            checkSectionIVCompletion();
                        } else {
                            // Email validation failed - show error message
                            tvEmailValidation.setText("✗ " + message);
                            tvEmailValidation.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        }
                    } catch (JSONException e) {
                        Log.e("RegistrationActivity", "JSON parsing error: " + e.getMessage());
                        tvEmailValidation.setText("✗ Server response error. Please try again.");
                        tvEmailValidation.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("RegistrationActivity", "Volley error: " + error.getMessage());
                    tvEmailValidation.setText("✗ Network error. Please check your connection.");
                    tvEmailValidation.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
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
     * Proceeds to next activity after email validation
     */
    private void proceedToNextActivity() {
        // Check if email validation was successful
        if (tvEmailValidation.getVisibility() != View.VISIBLE || 
            !tvEmailValidation.getText().toString().startsWith("✓")) {
            showValidationErrorModal("Please wait for email validation to complete.");
            return;
        }
        
                    Intent a = new Intent(RegistrationActivity.this, Registration2Activity.class);

                    String selectedRole = spinnerRole.getSelectedItem().toString();
                    boolean isBoarder = "Boarder".equals(selectedRole);
                    
                    a.putExtra("role", selectedRole);
                    a.putExtra("firstName", etFirstName.getText().toString().trim());
                    a.putExtra("middleName", etMiddleName.getText().toString().trim());
                    a.putExtra("lastName", etLastName.getText().toString().trim());
                    a.putExtra("suffix", spinnerSuffix.getSelectedItem().toString());
                    a.putExtra("birthDate", etBirthDate.getText().toString().trim());
                    // Convert phone format: +63 992 531 1409 -> 09925311409 (start with 0, no +63)
                    String phoneFormatted = etPhone.getText().toString().trim();
                    // Extract digits after +63 (skip "+63 ", get the 10 digits after)
                    String digitsAfterPlus63 = phoneFormatted.substring(4).replaceAll("[^0-9]", "");
                    // Add 0 at the start: 09925311409 (11 digits starting with 0)
                    String phoneNumber = "0" + digitsAfterPlus63;
                    a.putExtra("phone", phoneNumber);
                    a.putExtra("address", etAddress.getText().toString().trim());
                    a.putExtra("email", etEmail.getText().toString().trim());
                    a.putExtra("password", etPassword.getText().toString().trim());
                    
                    // Only add GCash data if not Boarder
                    if (!isBoarder) {
                        a.putExtra("gcashNum", etGcashNum.getText().toString().trim());
                        
                        // if you want to send QR URI
                        if (selectedQrUri != null) {
                            a.putExtra("qrUri", selectedQrUri.toString());
                        }
                    } else {
                        // Boarder doesn't need GCash
                        a.putExtra("gcashNum", "");
                        a.putExtra("qrUri", "");
                    }

                    startActivity(a);
    }

    /**
     * Shows validation error in a modal dialog
     * @param errorMessage The error message to display
     */
    private void showValidationErrorModal(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Validation Error");
        builder.setMessage(errorMessage);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.setCancelable(true);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Style the dialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFF6200EE); // Primary color
    }
    
    /**
     * Validates all input fields based on role and field type
     * @return Error message if validation fails, null if all fields are valid
     */
    private String validateAllFields() {
        String selectedRole = spinnerRole.getSelectedItem().toString();
        boolean isBoarder = "Boarder".equals(selectedRole);
        
        // 1. Validate Role
        if (selectedRole.equals("Select --")) {
            return "Please select a role (Boarder or BH Owner)";
        }
        
        // 2. Validate First Name
        String firstName = etFirstName.getText().toString().trim();
        if (firstName.isEmpty()) {
            return "First Name is required";
        }
        if (firstName.length() < 2 || firstName.length() > 50) {
            return "First Name must be 2-50 characters long";
        }
        if (!firstName.matches("^[a-zA-Z\\s\\-']+$")) {
            return "First Name can only contain letters, spaces, hyphens, and apostrophes";
        }
        
        // 3. Validate Last Name
        String lastName = etLastName.getText().toString().trim();
        if (lastName.isEmpty()) {
            return "Last Name is required";
        }
        if (lastName.length() < 2 || lastName.length() > 50) {
            return "Last Name must be 2-50 characters long";
        }
        if (!lastName.matches("^[a-zA-Z\\s\\-']+$")) {
            return "Last Name can only contain letters, spaces, hyphens, and apostrophes";
        }
        
        // 4. Validate Middle Name (optional but if provided, should be valid)
        String middleName = etMiddleName.getText().toString().trim();
        if (!middleName.isEmpty()) {
            if (middleName.length() < 1 || middleName.length() > 50) {
                return "Middle Name must be 1-50 characters long if provided";
            }
            if (!middleName.matches("^[a-zA-Z\\s\\-']+$")) {
                return "Middle Name can only contain letters, spaces, hyphens, and apostrophes";
            }
        }
        
        // 5. Validate Birth Date
        String birthDate = etBirthDate.getText().toString().trim();
        if (birthDate.isEmpty()) {
            return "Birth Date is required";
        }
        // Validate date format (MM/DD/YYYY)
        if (!birthDate.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$")) {
            return "Birth Date must be in MM/DD/YYYY format";
        }
        // Check if user is at least 18 years old
        try {
            String[] dateParts = birthDate.split("/");
            int month = Integer.parseInt(dateParts[0]);
            int day = Integer.parseInt(dateParts[1]);
            int year = Integer.parseInt(dateParts[2]);
            
            if (month < 1 || month > 12) {
                return "Birth Date: Month must be 1-12";
            }
            if (day < 1 || day > 31) {
                return "Birth Date: Day must be 1-31";
            }
            if (year < 1900 || year > java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) {
                return "Birth Date: Year must be between 1900 and current year";
            }
            
            // Calculate age
            java.util.Calendar birthCalendar = java.util.Calendar.getInstance();
            birthCalendar.set(year, month - 1, day);
            java.util.Calendar now = java.util.Calendar.getInstance();
            int age = now.get(java.util.Calendar.YEAR) - birthCalendar.get(java.util.Calendar.YEAR);
            if (now.get(java.util.Calendar.DAY_OF_YEAR) < birthCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--;
            }
            
            if (age < 18) {
                return "You must be at least 18 years old to register";
            }
            if (age > 120) {
                return "Please enter a valid birth date";
            }
        } catch (Exception e) {
            return "Birth Date: Invalid date format";
        }
        
        // 6. Validate Phone Number
        String phone = etPhone.getText().toString().trim();
        if (phone.isEmpty() || phone.equals("+63")) {
            return "Phone Number is required";
        }
        // Extract digits only (+63 represents 0, so +63 992 531 1409 = 09925311409)
        if (!phone.startsWith("+63")) {
            return "Phone Number must start with +63";
        }
        String digitsAfterPlus63 = phone.substring(4).replaceAll("[^0-9]", "");
        if (digitsAfterPlus63.length() != 10) {
            return "Phone Number must have 10 digits after +63 (e.g., +63 992 531 1409)";
        }
        // First digit after +63 should be 9 (Philippine mobile format)
        if (!digitsAfterPlus63.startsWith("9")) {
            return "Phone Number must start with 9 after +63 (Philippine mobile format)";
        }
        
        // 7. Validate Address
        String province = spinnerProvince.getSelectedItem().toString();
        String municipality = spinnerMunicipality.getSelectedItem().toString();
        String barangay = etBarangay.getText().toString().trim();
        String detailedAddress = etDetailedAddress.getText().toString().trim();
        
        if (province.equals("Select Province") || province.isEmpty()) {
            return "Please select a Province";
        }
        if (municipality.equals("Select Municipality") || municipality.isEmpty()) {
            return "Please select a Municipality/City";
        }
        if (barangay.isEmpty()) {
            return "Barangay is required";
        }
        if (barangay.length() < 2 || barangay.length() > 100) {
            return "Barangay must be 2-100 characters long";
        }
        if (detailedAddress.isEmpty()) {
            return "Detailed Address (Street, House No., etc.) is required";
        }
        if (detailedAddress.length() < 5 || detailedAddress.length() > 200) {
            return "Detailed Address must be 5-200 characters long";
        }
        
        // 8. Validate Email (already has real-time validation, just check if it exists)
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            return "Email is required";
        }
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return "Email format is invalid";
        }
        // Check if email validation was successful
        if (tvEmailValidation.getVisibility() == View.VISIBLE) {
            String validationText = tvEmailValidation.getText().toString();
            if (validationText.startsWith("✗")) {
                return "Email validation failed. Please use a valid email address";
            }
            if (!validationText.startsWith("✓")) {
                return "Please wait for email validation to complete";
            }
        }
        
        // 9. Validate Password
        String password = etPassword.getText().toString();
        if (password.isEmpty()) {
            return "Password is required";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }
        if (password.length() > 50) {
            return "Password must be maximum 50 characters long";
        }
        // Password must contain at least one letter and one number
        if (!password.matches(".*[a-zA-Z].*")) {
            return "Password must contain at least one letter";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Password must contain at least one number";
        }
        
        // 10. Validate GCash Number (only for BH Owner)
        if (!isBoarder) {
            String gcashNum = etGcashNum.getText().toString().trim();
            if (gcashNum.isEmpty()) {
                return "GCash Number is required for BH Owner";
            }
            // GCash format: 09xxxxxxxxx (11 digits starting with 09)
            String gcashDigits = gcashNum.replaceAll("[^0-9]", "");
            if (gcashDigits.length() != 11) {
                return "GCash Number must be 11 digits (09xxxxxxxxx)";
            }
            if (!gcashDigits.matches("^09\\d{9}$")) {
                return "GCash Number must start with 09 (Philippine mobile format)";
            }
            
            // 11. Validate GCash QR Code (only for BH Owner)
            if (UploadQr.getDrawable() == null) {
                return "GCash QR Code image is required for BH Owner";
            }
        }
        
        // All validations passed
        return null;
    }
    
    /**
     * Sets up phone number field with fixed +63 prefix and formatting
     * Format: +63 9XX XXX YYYY
     */
    private void setupPhoneNumberField() {
        // Set initial value to +63
        etPhone.setText("+63 ");
        etPhone.setSelection(etPhone.getText().length());
        
        etPhone.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isFormatting = false;
            private String previousText = "+63 ";
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previousText = s.toString();
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing here
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isFormatting) {
                    return;
                }
                
                isFormatting = true;
                String currentText = s.toString();
                
                // Always ensure +63 is at the start
                if (!currentText.startsWith("+63")) {
                    // If user tries to delete +63, restore it
                    etPhone.removeTextChangedListener(this);
                    etPhone.setText("+63 ");
                    etPhone.setSelection(etPhone.getText().length());
                    etPhone.addTextChangedListener(this);
                    isFormatting = false;
                    return;
                }
                
                // Extract only numbers after +63 (skip "+63 " = 4 characters)
                String digitsOnly = currentText.substring(4).replaceAll("[^0-9]", "");
                
                // Limit to 10 digits (since +63 represents 0, total should be 11 digits: 0 + 10 digits = 11)
                if (digitsOnly.length() > 10) {
                    digitsOnly = digitsOnly.substring(0, 10);
                }
                
                // Format as: +63 9XX XXX XXXX
                // Example: +63 992 531 1409
                StringBuilder formatted = new StringBuilder("+63 ");
                
                if (digitsOnly.length() > 0) {
                    // First 3 digits (e.g., 992)
                    if (digitsOnly.length() <= 3) {
                        formatted.append(digitsOnly);
                    } else {
                        formatted.append(digitsOnly.substring(0, 3));
                        formatted.append(" ");
                        
                        // Next 3 digits (e.g., 531)
                        if (digitsOnly.length() <= 6) {
                            formatted.append(digitsOnly.substring(3));
                        } else {
                            formatted.append(digitsOnly.substring(3, 6));
                            formatted.append(" ");
                            
                            // Last 4 digits (e.g., 1409)
                            if (digitsOnly.length() <= 10) {
                                formatted.append(digitsOnly.substring(6));
                            } else {
                                formatted.append(digitsOnly.substring(6, 10));
                            }
                        }
                    }
                }
                
                // Update the text
                etPhone.removeTextChangedListener(this);
                etPhone.setText(formatted.toString());
                
                // Set cursor position - place it at the end of what was typed
                // Format: +63 XXX XXX XXXX (10 digits)
                int digitCount = digitsOnly.length();
                int cursorPosition;
                
                if (digitCount == 0) {
                    cursorPosition = 4; // After "+63 "
                } else if (digitCount <= 3) {
                    cursorPosition = 4 + digitCount; // After "+63 " + digits
                } else if (digitCount <= 6) {
                    cursorPosition = 5 + digitCount; // After "+63 " + first 3 + space + remaining
                } else {
                    cursorPosition = 6 + digitCount; // After "+63 " + first 3 + space + next 3 + space + remaining
                }
                
                etPhone.setSelection(Math.min(cursorPosition, formatted.length()));
                
                etPhone.addTextChangedListener(this);
                isFormatting = false;
            }
        });
        
        // Prevent selection/deletion of +63 prefix
        etPhone.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL) {
                EditText editText = (EditText) v;
                int cursorPosition = editText.getSelectionStart();
                
                // Prevent deleting +63
                if (cursorPosition <= 4) { // +63 = 4 characters
                    return true; // Consume the event
                }
            }
            return false;
        });
    }
    
    /**
     * Verifies and sets QR image if approved
     */
    private void verifyAndSetQrImage(Uri imageUri) {
        Log.d("QR_VALIDATION", "=== QR CODE VALIDATION STARTED ===");
        Log.d("QR_VALIDATION", "Image URI: " + imageUri.toString());
        
        // Create and show modal dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_qr_verification, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        
        // Get dialog views
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        ImageView ivQrPhoto = dialogView.findViewById(R.id.ivQrPhoto);
        ProgressBar progressBarVerifying = dialogView.findViewById(R.id.progressBarVerifying);
        TextView tvResultMessage = dialogView.findViewById(R.id.tvResultMessage);
        Button btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
        
        // Set the selected photo
        ivQrPhoto.setImageURI(imageUri);
        
        // Show dialog
        dialog.show();
        
        // Use API-based QR code verification
        ImageVerification.verifyQrCode(this, imageUri, new ImageVerification.VerificationCallback() {
            @Override
            public void onVerificationComplete(boolean isApproved, String reason) {
                Log.d("QR_VALIDATION", "=== QR CODE VALIDATION COMPLETED ===");
                Log.d("QR_VALIDATION", "Is Approved: " + isApproved);
                Log.d("QR_VALIDATION", "Reason: " + reason);
                
                // Hide progress bar
                progressBarVerifying.setVisibility(View.GONE);
                
                if (isApproved) {
                    Log.d("QR_VALIDATION", "✅ QR CODE VALIDATION PASSED");
                    // Set the image as selected
                    selectedQrUri = imageUri;
                    UploadQr.setImageURI(imageUri); // show the image in ImageView
                    UploadQr.setScaleType(ImageView.ScaleType.CENTER_CROP); // Ensure proper display
                    Log.d("QR_VALIDATION", "QR image set successfully");
                    
                    // Show success result
                    tvDialogTitle.setText("Verification Successful");
                    tvResultMessage.setText("✅ Valid GCash QR code detected!");
                    tvResultMessage.setTextColor(ContextCompat.getColor(RegistrationActivity.this, android.R.color.holo_green_dark));
                    tvResultMessage.setVisibility(View.VISIBLE);
                    btnCloseDialog.setVisibility(View.VISIBLE);
                    
                    // Check section V completion after QR code is verified
                    checkSectionVCompletion();
                } else {
                    Log.d("QR_VALIDATION", "❌ QR CODE VALIDATION FAILED");
                    Log.d("QR_VALIDATION", "Failure reason: " + reason);
                    
                    // Show rejection reason with more helpful message
                    String helpfulMessage = reason;
                    if (reason.contains("No QR code detected")) {
                        helpfulMessage = "No QR code found in image. Please upload a clear photo of your GCash QR code.";
                    } else if (reason.contains("does not appear to be a GCash QR code")) {
                        helpfulMessage = "This doesn't look like a GCash QR code. Please upload your actual GCash QR code.";
                    } else if (reason.contains("too small")) {
                        helpfulMessage = "Image is too small. Please take a clearer photo of your GCash QR code.";
                    } else if (reason.contains("too large")) {
                        helpfulMessage = "Image is too large. Please compress or resize your GCash QR code image.";
                    }
                    
                    Log.d("QR_VALIDATION", "Showing helpful message to user: " + helpfulMessage);
                    
                    // Show failure result
                    tvDialogTitle.setText("Verification Failed");
                    tvResultMessage.setText("❌ " + helpfulMessage);
                    tvResultMessage.setTextColor(ContextCompat.getColor(RegistrationActivity.this, android.R.color.holo_red_dark));
                    tvResultMessage.setVisibility(View.VISIBLE);
                    btnCloseDialog.setVisibility(View.VISIBLE);
                }
                
                // Set close button listener
                btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
            }
            
            @Override
            public void onVerificationError(String error) {
                Log.e("QR_VALIDATION", "Verification error: " + error);
                
                // Hide progress bar
                progressBarVerifying.setVisibility(View.GONE);
                
                // Show error result
                tvDialogTitle.setText("Verification Error");
                tvResultMessage.setText("QR code verification failed: " + error);
                tvResultMessage.setTextColor(ContextCompat.getColor(RegistrationActivity.this, android.R.color.holo_red_dark));
                tvResultMessage.setVisibility(View.VISIBLE);
                btnCloseDialog.setVisibility(View.VISIBLE);
                
                // Set close button listener
                btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
            }
        });
    }
    
    private void initializeAddressPicker() {
        // Load provinces first
        loadProvinces();
        
        // Set up province selection listener
        spinnerProvince.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position > 0) { // Skip "Select Province" option
                    String newProvince = parent.getItemAtPosition(position).toString();
                    
                    // If province changed, reset municipality and barangay
                    if (!newProvince.equals(selectedProvince)) {
                        // Reset municipality and barangay spinners first
                        clearMunicipalityAndBarangay();
                        selectedMunicipality = "";
                        selectedBarangay = "";
                        
                        // Set new province and load municipalities
                        selectedProvince = newProvince;
                        loadMunicipalities(selectedProvince);
                    } else {
                        // Same province selected, just update
                        selectedProvince = newProvince;
                    }
                } else {
                    // "Select Province" selected
                    selectedProvince = "";
                    clearMunicipalityAndBarangay();
                    selectedMunicipality = "";
                    selectedBarangay = "";
                }
                updateCompleteAddress();
                // Check section III completion
                checkSectionIIICompletion();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Set up municipality selection listener
        spinnerMunicipality.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position > 0) { // Skip "Select Municipality" option
                    selectedMunicipality = parent.getItemAtPosition(position).toString();
                } else {
                    selectedMunicipality = "";
                }
                updateCompleteAddress();
                // Check section III completion
                checkSectionIIICompletion();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Set up barangay input listener
        etBarangay.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                selectedBarangay = s.toString().trim();
                updateCompleteAddress();
            }
        });
        
        // Set up detailed address listener
        etDetailedAddress.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                selectedDetailedAddress = s.toString().trim();
                updateCompleteAddress();
            }
        });
    }
    
    private void loadProvinces() {
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/philippine_address_api.php?action=provinces";
        
        Log.d("AddressPicker", "Loading provinces from: " + url);
        
        com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            response -> {
                try {
                    Log.d("AddressPicker", "Provinces response received: " + response.toString());
                    
                    if (response.getBoolean("success")) {
                        org.json.JSONArray provincesArray = response.getJSONArray("data");
                        Log.d("AddressPicker", "Found " + provincesArray.length() + " provinces");
                        
                        String[] provinceNames = new String[provincesArray.length() + 1];
                        provinceNames[0] = "Select Province";
                        
                        for (int i = 0; i < provincesArray.length(); i++) {
                            org.json.JSONObject province = provincesArray.getJSONObject(i);
                            provinceNames[i + 1] = province.getString("name");
                        }
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            this, android.R.layout.simple_spinner_item, provinceNames) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                                if (textView != null) {
                                    textView.setTextColor(0xFF000000); // Black text color for selected item
                                }
                                return view;
                            }
                            
                            @Override
                            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                View view = super.getDropDownView(position, convertView, parent);
                                if (view instanceof TextView) {
                                    TextView textView = (TextView) view;
                                    textView.setTextColor(0xFFFFFFFF);
                                    textView.setTextSize(16);
                                    textView.setPadding(16, 16, 16, 16);
                                    textView.setBackgroundColor(0xFF2C2C2C);
                                } else {
                                    TextView textView = view.findViewById(android.R.id.text1);
                                    if (textView != null) {
                                        textView.setTextColor(0xFFFFFFFF);
                                        textView.setTextSize(16);
                                        textView.setPadding(16, 16, 16, 16);
                                    }
                                    view.setBackgroundColor(0xFF2C2C2C);
                                }
                                return view;
                            }
                        };
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerProvince.setAdapter(adapter);
                        
                        Log.d("AddressPicker", "Provinces loaded successfully");
                    } else {
                        Log.e("AddressPicker", "API returned success=false");
                        loadProvincesFallback();
                    }
                } catch (org.json.JSONException e) {
                    Log.e("AddressPicker", "JSON parsing error: " + e.getMessage());
                    Log.e("AddressPicker", "Response was: " + response.toString());
                    e.printStackTrace();
                    loadProvincesFallback();
                }
            },
            error -> {
                Log.e("AddressPicker", "Network error loading provinces: " + error.getMessage());
                Log.e("AddressPicker", "Error type: " + error.getClass().getSimpleName());
                if (error.networkResponse != null) {
                    Log.e("AddressPicker", "Response code: " + error.networkResponse.statusCode);
                    Log.e("AddressPicker", "Response data: " + new String(error.networkResponse.data));
                }
                loadProvincesFallback();
            }
        );
        
        com.android.volley.RequestQueue queue = com.android.volley.toolbox.Volley.newRequestQueue(this);
        queue.add(request);
    }
    
    private void loadProvincesFallback() {
        Log.d("AddressPicker", "Using fallback provinces data");
        
        // Fallback provinces data - Major Philippine provinces
        String[] fallbackProvinces = {
            "Select Province",
            "Abra", "Agusan del Norte", "Agusan del Sur", "Aklan", "Albay", "Antique", "Apayao", "Aurora",
            "Basilan", "Bataan", "Batanes", "Batangas", "Benguet", "Biliran", "Bohol", "Bukidnon", "Bulacan",
            "Cagayan", "Camarines Norte", "Camarines Sur", "Camiguin", "Capiz", "Catanduanes", "Cavite", "Cebu",
            "Cotabato", "Davao del Norte", "Davao del Sur", "Davao Oriental", "Davao de Oro", "Davao Occidental",
            "Dinagat Islands", "Eastern Samar", "Guimaras", "Ifugao", "Ilocos Norte", "Ilocos Sur", "Iloilo",
            "Isabela", "Kalinga", "Laguna", "Lanao del Norte", "Lanao del Sur", "La Union", "Leyte", "Maguindanao",
            "Marinduque", "Masbate", "Metro Manila", "Misamis Occidental", "Misamis Oriental", "Mountain Province",
            "Negros Occidental", "Negros Oriental", "Northern Samar", "Nueva Ecija", "Nueva Vizcaya",
            "Occidental Mindoro", "Oriental Mindoro", "Palawan", "Pampanga", "Pangasinan", "Quezon", "Quirino",
            "Rizal", "Romblon", "Samar", "Sarangani", "Siquijor", "Sorsogon", "South Cotabato", "Southern Leyte",
            "Sultan Kudarat", "Sulu", "Surigao del Norte", "Surigao del Sur", "Tarlac", "Tawi-Tawi", "Zambales",
            "Zamboanga del Norte", "Zamboanga del Sur", "Zamboanga Sibugay"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, fallbackProvinces) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(0xFF000000); // Black text color for selected item
                }
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(0xFFFFFFFF);
                    textView.setTextSize(16);
                    textView.setPadding(16, 16, 16, 16);
                    textView.setBackgroundColor(0xFF2C2C2C);
                } else {
                    TextView textView = view.findViewById(android.R.id.text1);
                    if (textView != null) {
                        textView.setTextColor(0xFFFFFFFF);
                        textView.setTextSize(16);
                        textView.setPadding(16, 16, 16, 16);
                    }
                    view.setBackgroundColor(0xFF2C2C2C);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(adapter);
        
        Log.d("AddressPicker", "Fallback provinces loaded successfully");
        Toast.makeText(this, "Using offline provinces data", Toast.LENGTH_SHORT).show();
    }
    
    private void loadMunicipalities(String province) {
        try {
            String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/philippine_address_api.php?action=municipalities&province_name=" + URLEncoder.encode(province, "UTF-8");
        
        com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.getBoolean("success")) {
                        org.json.JSONArray municipalitiesArray = response.getJSONArray("data");
                        
                        if (municipalitiesArray.length() > 0) {
                            String[] municipalityNames = new String[municipalitiesArray.length() + 1];
                            municipalityNames[0] = "Select Municipality";
                            
                            for (int i = 0; i < municipalitiesArray.length(); i++) {
                                org.json.JSONObject municipality = municipalitiesArray.getJSONObject(i);
                                municipalityNames[i + 1] = municipality.getString("name");
                            }
                            
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                this, android.R.layout.simple_spinner_item, municipalityNames) {
                                @Override
                                public View getView(int position, View convertView, ViewGroup parent) {
                                    View view = super.getView(position, convertView, parent);
                                    TextView textView = (TextView) view.findViewById(android.R.id.text1);
                                    if (textView != null) {
                                        textView.setTextColor(0xFF000000); // Black text color for selected item
                                    }
                                    return view;
                                }
                                
                                @Override
                                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                    View view = super.getDropDownView(position, convertView, parent);
                                    if (view instanceof TextView) {
                                        TextView textView = (TextView) view;
                                        textView.setTextColor(0xFFFFFFFF);
                                        textView.setTextSize(16);
                                        textView.setPadding(16, 16, 16, 16);
                                        textView.setBackgroundColor(0xFF2C2C2C);
                                    } else {
                                        TextView textView = view.findViewById(android.R.id.text1);
                                        if (textView != null) {
                                            textView.setTextColor(0xFFFFFFFF);
                                            textView.setTextSize(16);
                                            textView.setPadding(16, 16, 16, 16);
                                        }
                                        view.setBackgroundColor(0xFF2C2C2C);
                                    }
                                    return view;
                                }
                            };
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerMunicipality.setAdapter(adapter);
                        } else {
                            // No municipalities found
                            Log.w("AddressPicker", "No municipalities found for province: " + province);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "No municipalities available for this province. Please type your complete address manually in the detailed address field below.", Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                } catch (org.json.JSONException e) {
                    Log.e("AddressPicker", "JSON parsing error: " + e.getMessage());
                    e.printStackTrace();
                }
            },
            error -> {
                Log.e("AddressPicker", "Error loading municipalities: " + error.getMessage());
                // Show user-friendly message
                if (error.networkResponse != null) {
                    String data = new String(error.networkResponse.data);
                    Log.e("AddressPicker", "Response data: " + data);
                }
                
                // Check if it's due to no data (empty response)
                if (error.getMessage().contains("org.json.JSONException: No value for data")) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "No municipalities available for this province. Please type your address manually below.", Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
        
        com.android.volley.RequestQueue queue = com.android.volley.toolbox.Volley.newRequestQueue(this);
        queue.add(request);
        } catch (Exception e) {
            Log.e("AddressPicker", "Error encoding province name: " + e.getMessage());
            Toast.makeText(this, "Error loading municipalities", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void clearMunicipalityAndBarangay() {
        String[] emptyArray = {"Select Municipality"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, emptyArray) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(0xFF000000); // Black text color for selected item
                }
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(0xFFFFFFFF);
                    textView.setTextSize(16);
                    textView.setPadding(16, 16, 16, 16);
                    textView.setBackgroundColor(0xFF2C2C2C);
                } else {
                    TextView textView = view.findViewById(android.R.id.text1);
                    if (textView != null) {
                        textView.setTextColor(0xFFFFFFFF);
                        textView.setTextSize(16);
                        textView.setPadding(16, 16, 16, 16);
                    }
                    view.setBackgroundColor(0xFF2C2C2C);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMunicipality.setAdapter(adapter);
        clearBarangay();
    }
    
    private void clearBarangay() {
        // Clear barangay EditText
        if (etBarangay != null) {
            etBarangay.setText("");
        }
        selectedBarangay = "";
    }
    
    private void updateCompleteAddress() {
        StringBuilder completeAddress = new StringBuilder();
        
        if (!selectedDetailedAddress.isEmpty()) {
            completeAddress.append(selectedDetailedAddress);
        }
        
        if (!selectedBarangay.isEmpty()) {
            if (completeAddress.length() > 0) completeAddress.append(", ");
            completeAddress.append(selectedBarangay);
        }
        
        if (!selectedMunicipality.isEmpty()) {
            if (completeAddress.length() > 0) completeAddress.append(", ");
            completeAddress.append(selectedMunicipality);
        }
        
        if (!selectedProvince.isEmpty()) {
            if (completeAddress.length() > 0) completeAddress.append(", ");
            completeAddress.append(selectedProvince);
        }
        
        etAddress.setText(completeAddress.toString());
    }
    
    /**
     * Creates a text watcher listener that checks section II completion
     */
    private android.text.TextWatcher createSectionIICheckListener() {
        return new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkSectionIICompletion();
            }
        };
    }
    
    /**
     * Creates a text watcher listener that checks section III completion
     */
    private android.text.TextWatcher createSectionIIICheckListener() {
        return new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkSectionIIICompletion();
            }
        };
    }
    
    /**
     * Creates a text watcher listener that checks section IV completion
     */
    private android.text.TextWatcher createSectionIVCheckListener() {
        return new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkSectionIVCompletion();
            }
        };
    }
    
    /**
     * Gets the parent FrameLayout wrapper for a CardView to control visibility
     */
    private View getSectionWrapper(com.google.android.material.card.MaterialCardView cardView) {
        if (cardView != null && cardView.getParent() instanceof ViewGroup) {
            return (View) cardView.getParent();
        }
        return cardView;
    }
    
    /**
     * Checks if Section I (Account Type) is complete and reveals Section II
     * Also hides Section II and all subsequent sections if Section I becomes incomplete
     */
    private void checkSectionICompletion() {
        String selectedRole = spinnerRole.getSelectedItem().toString();
        View sectionIWrapper = getSectionWrapper(sectionAccountType);
        View sectionIIWrapper = getSectionWrapper(sectionPersonalInfo);
        
        if (!selectedRole.equals("Select --") && !selectedRole.isEmpty()) {
            // Section I is complete, reveal Section II
            if (sectionIIWrapper.getVisibility() != View.VISIBLE) {
                sectionIIWrapper.setVisibility(View.VISIBLE);
                updateProgressIndicator();
            }
        } else {
            // Section I is incomplete, hide Section II and all subsequent sections
            hideAllSubsequentSections(2);
            updateProgressIndicator();
        }
    }
    
    /**
     * Checks if Section II (Personal Information) is complete and reveals Section III
     * Also hides Section III and all subsequent sections if Section II becomes incomplete
     */
    private void checkSectionIICompletion() {
        // Check if all required fields in Section II are filled
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        
        // Phone validation: should have at least +63 and 10 digits after it
        boolean phoneValid = phone.length() >= 14 && phone.startsWith("+63") && 
                            phone.substring(4).replaceAll("[^0-9]", "").length() >= 10;
        
        // Birth date validation: should match MM/DD/YYYY format
        boolean birthDateValid = birthDate.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$");
        
        View sectionIIIWrapper = getSectionWrapper(sectionAddress);
        
        if (!firstName.isEmpty() && !lastName.isEmpty() && birthDateValid && phoneValid) {
            // Section II is complete, reveal Section III
            if (sectionIIIWrapper.getVisibility() != View.VISIBLE) {
                sectionIIIWrapper.setVisibility(View.VISIBLE);
                updateProgressIndicator();
            }
        } else {
            // Section II is incomplete, hide Section III and all subsequent sections
            hideAllSubsequentSections(3);
            updateProgressIndicator();
        }
    }
    
    /**
     * Checks if Section III (Permanent Address) is complete and reveals Section IV
     * Also hides Section IV and all subsequent sections if Section III becomes incomplete
     */
    private void checkSectionIIICompletion() {
        // Check if all required fields in Section III are filled
        String province = spinnerProvince.getSelectedItem().toString();
        String municipality = spinnerMunicipality.getSelectedItem().toString();
        String barangay = etBarangay.getText().toString().trim();
        String detailedAddress = etDetailedAddress.getText().toString().trim();
        
        boolean provinceValid = !province.equals("Select Province") && !province.isEmpty();
        boolean municipalityValid = !municipality.equals("Select Municipality") && !municipality.isEmpty();
        boolean barangayValid = !barangay.isEmpty();
        boolean detailedAddressValid = !detailedAddress.isEmpty();
        
        View sectionIVWrapper = getSectionWrapper(sectionLoginCredentials);
        
        if (provinceValid && municipalityValid && barangayValid && detailedAddressValid) {
            // Section III is complete, reveal Section IV
            if (sectionIVWrapper.getVisibility() != View.VISIBLE) {
                sectionIVWrapper.setVisibility(View.VISIBLE);
                updateProgressIndicator();
            }
        } else {
            // Section III is incomplete, hide Section IV and all subsequent sections
            hideAllSubsequentSections(4);
            updateProgressIndicator();
        }
    }
    
    /**
     * Checks if Section IV (Login Credentials) is complete and reveals Section V
     */
    private void checkSectionIVCompletion() {
        // Check if all required fields in Section IV are filled
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        // Basic email format validation
        boolean emailValid = !email.isEmpty() && email.contains("@") && email.contains(".");
        
        // Password validation: at least 8 characters
        boolean passwordValid = password.length() >= 8;
        
        // For email validation status - if validation has been triggered and visible, it must pass
        // If validation hasn't been triggered yet, we just need valid format
        boolean emailValidationPassed = true;
        if (tvEmailValidation.getVisibility() == View.VISIBLE) {
            String validationText = tvEmailValidation.getText().toString();
            // If validation message is visible, check if it's successful (starts with ✓)
            // If it shows an error (starts with ✗), validation failed
            if (validationText.startsWith("✗")) {
                emailValidationPassed = false;
            } else if (validationText.startsWith("✓")) {
                emailValidationPassed = true;
            } else {
                // Still validating, don't reveal next section yet
                emailValidationPassed = false;
            }
        }
        
        View sectionVWrapper = getSectionWrapper(sectionPaymentInfo);
        String selectedRole = spinnerRole.getSelectedItem().toString();
        boolean isBoarder = "Boarder".equals(selectedRole);
        
        // Section IV is complete if email format is valid, password is valid, and validation passed (if triggered)
        if (emailValid && passwordValid && emailValidationPassed) {
            // Section IV is complete, reveal Section V
            // Only show Section V if not a Boarder (Boarders don't need payment info)
            if (!isBoarder && sectionVWrapper.getVisibility() != View.VISIBLE) {
                sectionVWrapper.setVisibility(View.VISIBLE);
                updateProgressIndicator();
            } else if (isBoarder) {
                // Boarder doesn't need section 5, but section 4 is complete
                updateProgressIndicator();
            }
        } else {
            // Section IV is incomplete, hide Section V
            if (sectionVWrapper.getVisibility() == View.VISIBLE) {
                sectionVWrapper.setVisibility(View.GONE);
            }
            updateProgressIndicator();
        }
    }
    
    /**
     * Checks if Section V (Payment Information) completion
     * This section is only for BH Owner, so we check accordingly
     */
    private void checkSectionVCompletion() {
        String selectedRole = spinnerRole.getSelectedItem().toString();
        boolean isBoarder = "Boarder".equals(selectedRole);
        
        if (isBoarder) {
            // Boarders don't need payment info, so section is always "complete" once revealed
            return;
        }
        
        // For BH Owner, check if GCash number and QR code are provided
        String gcashNum = etGcashNum.getText().toString().trim();
        boolean gcashNumValid = !gcashNum.isEmpty() && gcashNum.replaceAll("[^0-9]", "").length() == 11;
        boolean qrCodeValid = selectedQrUri != null;
        
        // Section V completion doesn't reveal a new section, but we can use this for final validation
        // This method is called when GCash fields are updated to ensure data is ready for submission
    }
    
    /**
     * Hides all subsequent sections starting from the given section number
     * Used when an earlier section becomes incomplete to cascade hide all dependent sections
     * @param startSectionNumber The section number to start hiding from (2 = Section II, 3 = Section III, etc.)
     *                           All sections from this number onwards will be hidden
     */
    private void hideAllSubsequentSections(int startSectionNumber) {
        // Section numbers: 2 = Section II (Personal Info), 3 = Section III (Address),
        // 4 = Section IV (Login Credentials), 5 = Section V (Payment Info)
        
        // Hide Section II (Personal Info) if we're starting from section 2 onwards
        if (startSectionNumber <= 2) {
            View sectionIIWrapper = getSectionWrapper(sectionPersonalInfo);
            if (sectionIIWrapper.getVisibility() == View.VISIBLE) {
                sectionIIWrapper.setVisibility(View.GONE);
            }
        }
        
        // Hide Section III (Address) if we're starting from section 3 onwards
        if (startSectionNumber <= 3) {
            View sectionIIIWrapper = getSectionWrapper(sectionAddress);
            if (sectionIIIWrapper.getVisibility() == View.VISIBLE) {
                sectionIIIWrapper.setVisibility(View.GONE);
            }
        }
        
        // Hide Section IV (Login Credentials) if we're starting from section 4 onwards
        if (startSectionNumber <= 4) {
            View sectionIVWrapper = getSectionWrapper(sectionLoginCredentials);
            if (sectionIVWrapper.getVisibility() == View.VISIBLE) {
                sectionIVWrapper.setVisibility(View.GONE);
                // Also clear email validation message when hiding login section
                if (tvEmailValidation.getVisibility() == View.VISIBLE) {
                    tvEmailValidation.setVisibility(View.GONE);
                }
            }
        }
        
        // Hide Section V (Payment Info) if we're starting from section 5 onwards
        if (startSectionNumber <= 5) {
            View sectionVWrapper = getSectionWrapper(sectionPaymentInfo);
            if (sectionVWrapper.getVisibility() == View.VISIBLE) {
                sectionVWrapper.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Updates the progress indicator circles based on which sections are visible/completed
     * Circle 1 is always filled (Section 1 is always visible)
     * Subsequent circles are filled when their corresponding section becomes visible
     */
    private void updateProgressIndicator() {
        if (progressCircle1 == null || progressCircle2 == null || progressCircle3 == null ||
            progressCircle4 == null || progressCircle5 == null) {
            return; // Progress circles not initialized yet
        }
        
        // Circle 1 is always filled (Section 1 is always visible)
        progressCircle1.setImageResource(R.drawable.progress_circle_filled);
        
        // Check visibility of each section wrapper and fill corresponding circle
        View sectionIIWrapper = getSectionWrapper(sectionPersonalInfo);
        if (sectionIIWrapper != null && sectionIIWrapper.getVisibility() == View.VISIBLE) {
            progressCircle2.setImageResource(R.drawable.progress_circle_filled);
        } else {
            progressCircle2.setImageResource(R.drawable.progress_circle_hollow);
        }
        
        View sectionIIIWrapper = getSectionWrapper(sectionAddress);
        if (sectionIIIWrapper != null && sectionIIIWrapper.getVisibility() == View.VISIBLE) {
            progressCircle3.setImageResource(R.drawable.progress_circle_filled);
        } else {
            progressCircle3.setImageResource(R.drawable.progress_circle_hollow);
        }
        
        View sectionIVWrapper = getSectionWrapper(sectionLoginCredentials);
        if (sectionIVWrapper != null && sectionIVWrapper.getVisibility() == View.VISIBLE) {
            progressCircle4.setImageResource(R.drawable.progress_circle_filled);
        } else {
            progressCircle4.setImageResource(R.drawable.progress_circle_hollow);
        }
        
        View sectionVWrapper = getSectionWrapper(sectionPaymentInfo);
        if (sectionVWrapper != null && sectionVWrapper.getVisibility() == View.VISIBLE) {
            progressCircle5.setImageResource(R.drawable.progress_circle_filled);
        } else {
            progressCircle5.setImageResource(R.drawable.progress_circle_hollow);
        }
    }
}
