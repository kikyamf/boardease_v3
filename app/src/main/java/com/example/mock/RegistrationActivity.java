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

    Spinner spinnerRole, spinnerSuffix, spinnerProvince, spinnerMunicipality, spinnerBarangay;

    EditText etFirstName, etLastName, etMiddleName, etBirthDate, etPhone, etAddress, etDetailedAddress, etEmail, etPassword, etGcashNum;
    TextView tvLogin, tvEmailValidation;
    TextView tvFirstNameError, tvLastNameError, tvMiddleNameError, tvBirthDateError, tvBarangayError, tvDetailedAddressError;
    TextView tvGcashNo, tvGcashQR;
    ImageView UploadQr, ivTogglePassword;
    ProgressBar progressBarSection2, progressBarSection3, progressBarSection4, progressBarSection5, progressBarSection6;
    
    // Section 6 fields (from Registration2Activity)
    Spinner spinnerVId;
    ImageView ivUploadF, ivUploadB;
    EditText etIdNumber;
    LinearLayout backIdSection;
    CheckBox cbAgree;
    TextView tvAgreeText;
    private View businessPermitSection;
    private ViewGroup businessPermitContainer;
    private Button btnAddPermit;
    
    // ID and permit data
    private String idFrontPath = null;
    private String idBackPath = null;
    private Bitmap frontBitmap;
    private Bitmap backBitmap;
    private Bitmap qrBitmap;
    private boolean isRegistering = false;
    
    // Business Permit data
    private static class PermitUploadItem {
        View itemView;
        ImageView imageView;
        Button removeButton;
        Bitmap bitmap;
        Uri uri;
        int index;
    }
    private List<PermitUploadItem> permitUploadItems = new ArrayList<>();
    private static final int MAX_PERMITS = 3;
    private int nextPermitIndex = 0;
    private List<ActivityResultLauncher<String>> permitImageLaunchers = new ArrayList<>();
    private PermitUploadItem currentPermitItemForLauncher = null;
    
    // Image launchers for ID
    private ActivityResultLauncher<String> pickFrontImageLauncher;
    private ActivityResultLauncher<String> pickBackImageLauncher;
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
    private com.google.android.material.card.MaterialCardView sectionAdditionalInfo;
    private View sectionBoarderMessage;
    
    // Progress indicator circles
    private TextView progressCircle1;
    private TextView progressCircle2;
    private TextView progressCircle3;
    private TextView progressCircle4;
    private TextView progressCircle5;
    private TextView progressCircle6;
    
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
        spinnerBarangay = findViewById(R.id.spinnerBarangay);

        etBirthDate = findViewById(R.id.etBirthDate);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etBirthDate = findViewById(R.id.etBirthDate);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        etDetailedAddress = findViewById(R.id.etDetailedAddress);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etGcashNum = findViewById(R.id.etGcashNum);

        tvLogin = findViewById(R.id.tvLogin);
        tvEmailValidation = findViewById(R.id.tvEmailValidation);
        
        // Get error TextViews
        tvFirstNameError = findViewById(R.id.tvFirstNameError);
        tvLastNameError = findViewById(R.id.tvLastNameError);
        tvMiddleNameError = findViewById(R.id.tvMiddleNameError);
        tvBirthDateError = findViewById(R.id.tvBirthDateError);
        tvBarangayError = findViewById(R.id.tvBarangayError);
        tvDetailedAddressError = findViewById(R.id.tvDetailedAddressError);
        
        // Get GCash TextViews
        tvGcashNo = findViewById(R.id.tvGcashNo);
        tvGcashQR = findViewById(R.id.tvGcashQR);

        UploadQr = findViewById(R.id.UploadQr);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        
        // Get ProgressBars for section loading
        progressBarSection2 = findViewById(R.id.progressBarSection2);
        progressBarSection3 = findViewById(R.id.progressBarSection3);
        progressBarSection4 = findViewById(R.id.progressBarSection4);
        progressBarSection5 = findViewById(R.id.progressBarSection5);
        progressBarSection6 = findViewById(R.id.progressBarSection6);
        
        // Get section cards
        sectionAccountType = findViewById(R.id.sectionAccountType);
        sectionPersonalInfo = findViewById(R.id.sectionPersonalInfo);
        sectionAddress = findViewById(R.id.sectionAddress);
        sectionLoginCredentials = findViewById(R.id.sectionLoginCredentials);
        sectionPaymentInfo = findViewById(R.id.sectionPaymentInfo);
        sectionAdditionalInfo = findViewById(R.id.sectionAdditionalInfo);
        sectionBoarderMessage = findViewById(R.id.sectionBoarderMessage);
        
        // Initialize Section 6 fields
        spinnerVId = findViewById(R.id.spinnerVId);
        ivUploadF = findViewById(R.id.ivUploadF);
        ivUploadB = findViewById(R.id.ivUploadB);
        etIdNumber = findViewById(R.id.etIdNumber);
        backIdSection = findViewById(R.id.backIdSection);
        cbAgree = findViewById(R.id.cbAgree);
        tvAgreeText = findViewById(R.id.tvAgreeText);
        businessPermitSection = findViewById(R.id.businessPermitSection);
        businessPermitContainer = findViewById(R.id.businessPermitContainer);
        btnAddPermit = findViewById(R.id.btnAddPermit);
        
        // Get progress indicator circles
        progressCircle1 = findViewById(R.id.progressCircle1);
        progressCircle2 = findViewById(R.id.progressCircle2);
        progressCircle3 = findViewById(R.id.progressCircle3);
        progressCircle4 = findViewById(R.id.progressCircle4);
        progressCircle5 = findViewById(R.id.progressCircle5);
        progressCircle6 = findViewById(R.id.progressCircle6);
        
        // Initialize progress indicator (Circle 1 is filled by default)
        updateProgressIndicator();
        
        // Setup back button
        ImageView backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                handleBackNavigation();
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
                View sectionVWrapper = getSectionWrapper(sectionPaymentInfo);
                
                if ("Boarder".equals(selectedRole)) {
                    // Hide Section 5 for Boarders (they'll see the message section instead)
                    if (sectionVWrapper.getVisibility() == View.VISIBLE) {
                        sectionVWrapper.setVisibility(View.GONE);
                    }
                    // Hide business permit section for Boarder
                    if (businessPermitSection != null) {
                        businessPermitSection.setVisibility(View.GONE);
                    }
                    // Hide GCash section completely for Boarders
                    llGcash.setVisibility(android.view.View.GONE);
                } else if ("BH Owner".equals(selectedRole)) {
                    // Show both GCash Number and QR code for BH Owner
                    llGcash.setVisibility(android.view.View.VISIBLE);
                    tvGcashNo.setVisibility(android.view.View.VISIBLE);
                    etGcashNum.setVisibility(android.view.View.VISIBLE);
                    tvGcashQR.setVisibility(android.view.View.VISIBLE);
                    UploadQr.setVisibility(android.view.View.VISIBLE);
                    // Hide boarder message section for BH Owner
                    if (sectionBoarderMessage != null && sectionBoarderMessage.getVisibility() == View.VISIBLE) {
                        sectionBoarderMessage.setVisibility(View.GONE);
                    }
                    // Show business permit section for BH Owner
                    if (businessPermitSection != null) {
                        businessPermitSection.setVisibility(View.VISIBLE);
                        // Create first permit upload item if not already created
                        if (permitUploadItems.isEmpty()) {
                            createPermitUploadItem();
                        }
                    }
                } else {
                    // Hide all GCash fields for "Select --"
                    llGcash.setVisibility(android.view.View.GONE);
                    // Hide business permit section
                    if (businessPermitSection != null) {
                        businessPermitSection.setVisibility(View.GONE);
                    }
                    // Hide both Section 5 and boarder message
                    if (sectionVWrapper.getVisibility() == View.VISIBLE) {
                        sectionVWrapper.setVisibility(View.GONE);
                    }
                    if (sectionBoarderMessage != null && sectionBoarderMessage.getVisibility() == View.VISIBLE) {
                        sectionBoarderMessage.setVisibility(View.GONE);
                    }
                }
                
                // Check if section I is complete and reveal section II
                checkSectionICompletion();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Hide GCash fields by default if nothing selected
                android.widget.LinearLayout llGcash = findViewById(R.id.llGcash);
                llGcash.setVisibility(android.view.View.GONE);
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
        
        // Setup GCash number field with fixed +63 prefix and formatting
        setupGcashNumberField();
        
        // Setup Section 6 fields (Additional Personal Information)
        setupSection6Fields();
        
        // Setup name field validations with input filters and real-time validation
        setupNameFieldValidation(etFirstName, tvFirstNameError, "First Name");
        setupNameFieldValidation(etLastName, tvLastNameError, "Last Name");
        setupNameFieldValidation(etMiddleName, tvMiddleNameError, "Middle Name");
        
        // Setup address field validations
        setupAddressFieldValidation();
        
        // Set the calendar for the birthdate with 18+ restriction
        etBirthDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                int currentYear = calendar.get(Calendar.YEAR);
                int currentMonth = calendar.get(Calendar.MONTH);
                int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
                
                // Calculate maximum allowed year (18 years ago)
                int maxYear = currentYear - 18;
                int maxMonth = currentMonth;
                int maxDay = currentDay;
                
                // Default to 18 years ago
                int defaultYear = maxYear;
                int defaultMonth = maxMonth;
                int defaultDay = maxDay;

                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        RegistrationActivity.this,
                        (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                            // Format: MM/DD/YYYY
                            String date = (selectedMonth + 1) + "/" + selectedDay + "/" + selectedYear;
                            etBirthDate.setText(date);
                            
                            // Validate birth date and show error if needed
                            validateBirthDate(date);
                            
                            // Check section II completion after birth date is set
                            checkSectionIICompletion();
                        },
                        defaultYear, defaultMonth, defaultDay
                );

                // Restrict dates: no future dates and must be at least 18 years old
                Calendar maxDateCalendar = Calendar.getInstance();
                maxDateCalendar.set(maxYear, maxMonth, maxDay);
                datePickerDialog.getDatePicker().setMaxDate(maxDateCalendar.getTimeInMillis());
                
                // Set minimum date to 100 years ago (reasonable limit)
                Calendar minDateCalendar = Calendar.getInstance();
                minDateCalendar.set(currentYear - 100, 0, 1);
                datePickerDialog.getDatePicker().setMinDate(minDateCalendar.getTimeInMillis());

                datePickerDialog.show();
            }
        });
        
        // Setup listeners for section II fields to check completion
        etFirstName.addTextChangedListener(createSectionIICheckListener());
        etLastName.addTextChangedListener(createSectionIICheckListener());
        etPhone.addTextChangedListener(createSectionIICheckListener());
        
        // Setup listeners for section III fields to check completion
        // Note: Barangay is now a spinner, so it has its own listener in initializeAddressPicker()
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
                // Validate all fields including Section 6
                String validationError = validateAllFields();
                if (validationError != null && !validationError.isEmpty()) {
                    showValidationErrorModal(validationError);
                    return;
                }

                // If validation passed, check email validation status
                String email = etEmail.getText().toString().trim();
                Log.d("RegistrationActivity", "=== REGISTER BUTTON CLICKED ===");
                Log.d("RegistrationActivity", "Email: " + email);
                Log.d("RegistrationActivity", "Validation visibility: " + (tvEmailValidation.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                
                if (tvEmailValidation.getVisibility() == View.VISIBLE) {
                    String validationText = tvEmailValidation.getText().toString();
                    Log.d("RegistrationActivity", "Validation text: " + validationText);
                    
                    if (validationText.startsWith("✓")) {
                        // Email is already validated, proceed to registration
                        Log.d("RegistrationActivity", "Email validation PASSED - proceeding to registration");
                        proceedToRegistration();
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
                handleBackNavigation();
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
                            
                            // Proceed to registration after a short delay
                            etEmail.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("RegistrationActivity", "Delayed proceedToRegistration called");
                                    proceedToRegistration();
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
     * Proceeds to registration after email validation and Section 6 validation
     */
    private void proceedToRegistration() {
        // Check if email validation was successful
        if (tvEmailValidation.getVisibility() != View.VISIBLE || 
            !tvEmailValidation.getText().toString().startsWith("✓")) {
            showValidationErrorModal("Please wait for email validation to complete.");
            return;
        }
        
        // Validate Section 6 fields
        String section6Error = validateSection6Fields();
        if (section6Error != null && !section6Error.isEmpty()) {
            showValidationErrorModal(section6Error);
            return;
        }
        
        // All validations passed, proceed with registration
        performRegistration();
    }
    
    /**
     * Validates Section 6 fields (ID, ID images, terms agreement)
     * @return Error message if validation fails, null if valid
     */
    private String validateSection6Fields() {
        String selectedIdType = spinnerVId.getSelectedItem().toString();
        if (selectedIdType == null || selectedIdType.equals("Select --")) {
            return "Please select a valid ID type";
        }
        
        String idNumber = etIdNumber.getText().toString().trim();
        if (idNumber.isEmpty()) {
            return "Please enter your ID number";
        }
        
        if (idFrontPath == null || frontBitmap == null) {
            return "Please upload front ID image";
        }
        
        // Check if back ID is required (Passport doesn't have back side)
        boolean isPassport = selectedIdType.equals("Philippine Passport");
        if (!isPassport && (idBackPath == null || backBitmap == null)) {
            return "Please upload front and back ID images";
        }
        
        if (!cbAgree.isChecked()) {
            return "You must agree to the terms and privacy policy to continue";
        }
        
        // Check role-specific requirements
        String selectedRole = spinnerRole.getSelectedItem().toString();
        boolean isBoarder = "Boarder".equals(selectedRole);
        
        // GCash and QR code are only required for BH Owner (Boarders skip Section 5)
        if (!isBoarder) {
            // QR code is required for BH Owner
            if (selectedQrUri == null || qrBitmap == null) {
                return "GCash QR code is required for BH Owner. Please upload your GCash QR code.";
            }
            
            // Business permits are required for BH Owner (at least one)
            boolean hasPermits = false;
            for (PermitUploadItem item : permitUploadItems) {
                if (item.bitmap != null) {
                    hasPermits = true;
                    break;
                }
            }
            if (!hasPermits) {
                return "At least one business permit is required for BH Owner. Please upload your business permit(s).";
            }
        }
        
        return null; // All validations passed
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
        String barangay = spinnerBarangay.getSelectedItem().toString();
        String detailedAddress = etDetailedAddress.getText().toString().trim();
        
        if (province.equals("Select Province") || province.isEmpty()) {
            return "Please select a Province";
        }
        if (municipality.equals("Select Municipality") || municipality.isEmpty()) {
            return "Please select a Municipality/City";
        }
        if (barangay.equals("Select Barangay") || barangay.isEmpty()) {
            return "Please select a Barangay";
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
        
        // 10. Validate GCash Number (only required for BH Owner, not for Boarder)
        if (!isBoarder) {
            String gcashNum = etGcashNum.getText().toString().trim();
            if (gcashNum.isEmpty()) {
                return "GCash Number is required for BH Owner";
            }
            // Extract digits only (+63 represents 0, so +63 992 531 1409 = 09925311409)
            if (!gcashNum.startsWith("+63")) {
                return "GCash Number must start with +63";
            }
            String gcashDigitsAfterPlus63 = gcashNum.substring(4).replaceAll("[^0-9]", "");
            if (gcashDigitsAfterPlus63.length() != 10) {
                return "GCash Number must have 10 digits after +63 (e.g., +63 992 531 1409)";
            }
            // First digit after +63 should be 9 (Philippine mobile format)
            if (!gcashDigitsAfterPlus63.startsWith("9")) {
                return "GCash Number must start with 9 after +63 (Philippine mobile format)";
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
                if (!currentText.startsWith("+63") || currentText.length() < 4) {
                    // If user tries to delete +63, restore it
                    etPhone.removeTextChangedListener(this);
                    etPhone.setText("+63 ");
                    etPhone.setSelection(etPhone.getText().length());
                    etPhone.addTextChangedListener(this);
                    isFormatting = false;
                    return;
                }
                
                // Extract only numbers after +63 (skip "+63 " = 4 characters)
                // Safety check: ensure text is long enough before substring
                String digitsOnly = currentText.length() >= 4 
                    ? currentText.substring(4).replaceAll("[^0-9]", "")
                    : "";
                
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
     * Sets up GCash number field with fixed +63 prefix and formatting
     * Format: +63 9XX XXX XXXX
     */
    private void setupGcashNumberField() {
        // Set initial value to +63
        etGcashNum.setText("+63 ");
        etGcashNum.setSelection(etGcashNum.getText().length());
        
        etGcashNum.addTextChangedListener(new android.text.TextWatcher() {
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
                if (!currentText.startsWith("+63") || currentText.length() < 4) {
                    // If user tries to delete +63, restore it
                    etGcashNum.removeTextChangedListener(this);
                    etGcashNum.setText("+63 ");
                    etGcashNum.setSelection(etGcashNum.getText().length());
                    etGcashNum.addTextChangedListener(this);
                    isFormatting = false;
                    return;
                }
                
                // Extract only numbers after +63 (skip "+63 " = 4 characters)
                // Safety check: ensure text is long enough before substring
                String digitsOnly = currentText.length() >= 4 
                    ? currentText.substring(4).replaceAll("[^0-9]", "")
                    : "";
                
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
                etGcashNum.removeTextChangedListener(this);
                etGcashNum.setText(formatted.toString());
                
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
                
                etGcashNum.setSelection(Math.min(cursorPosition, formatted.length()));
                
                etGcashNum.addTextChangedListener(this);
                isFormatting = false;
            }
        });
        
        // Prevent selection/deletion of +63 prefix
        etGcashNum.setOnKeyListener((v, keyCode, event) -> {
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
     * Sets up Section 6 fields (ID selection, image uploads, business permits, terms)
     */
    private void setupSection6Fields() {
        // Setup ID type spinner
        String[] idTypes = {
            "Select --",
            "Philippine Passport",
            "Driver's License",
            "PhilID (National ID)",
            "UMID(SSS ID)",
            "GSIS e-card",
            "PhilHealth ID",
            "TIN ID (BIR)",
            "Voter's ID",
            "Postal ID"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            idTypes
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(0xFF000000);
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
        spinnerVId.setAdapter(adapter);
        
        // Setup ID type change listener
        spinnerVId.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedIdType = parent.getItemAtPosition(position).toString();
                // Show/hide back ID section based on ID type
                if (selectedIdType.equals("Philippine Passport")) {
                    backIdSection.setVisibility(View.GONE);
                } else {
                    backIdSection.setVisibility(View.VISIBLE);
                }
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Setup image pickers for ID
        pickFrontImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String selectedIdType = spinnerVId.getSelectedItem().toString();
                    if (selectedIdType == null || selectedIdType.equals("Select --")) {
                        Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    verifyAndSetFrontImage(uri);
                }
            }
        );
        
        pickBackImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String selectedIdType = spinnerVId.getSelectedItem().toString();
                    if (selectedIdType == null || selectedIdType.equals("Select --")) {
                        Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    verifyAndSetBackImage(uri);
                }
            }
        );
        
        // Setup ID image click listeners
        ivUploadF.setOnClickListener(v -> {
            String selectedIdType = spinnerVId.getSelectedItem().toString();
            if (selectedIdType == null || selectedIdType.equals("Select --")) {
                Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, IdCaptureActivity.class);
            intent.putExtra("id_type", "front");
            intent.putExtra("selected_id_type", selectedIdType);
            startActivityForResult(intent, 1001);
        });
        
        ivUploadB.setOnClickListener(v -> {
            String selectedIdType = spinnerVId.getSelectedItem().toString();
            if (selectedIdType == null || selectedIdType.equals("Select --")) {
                Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, IdCaptureActivity.class);
            intent.putExtra("id_type", "back");
            intent.putExtra("selected_id_type", selectedIdType);
            startActivityForResult(intent, 1002);
        });
        
        // Pre-register permit image launchers (must be done during onCreate)
        for (int i = 0; i < MAX_PERMITS; i++) {
            ActivityResultLauncher<String> permitLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && currentPermitItemForLauncher != null) {
                        handlePermitImageSelection(currentPermitItemForLauncher, uri);
                        currentPermitItemForLauncher = null;
                    }
                }
            );
            permitImageLaunchers.add(permitLauncher);
        }
        
        // Setup business permit section (only for BH Owner)
        // Initially hide it, will be shown when role is selected
        if (businessPermitSection != null) {
            businessPermitSection.setVisibility(View.GONE);
        }
        
        // Setup add permit button listener (will be used when BH Owner is selected)
        if (btnAddPermit != null) {
            btnAddPermit.setOnClickListener(v -> {
                if (permitUploadItems.size() < MAX_PERMITS) {
                    createPermitUploadItem();
                    updateAddPermitButtonVisibility();
                } else {
                    Toast.makeText(this, "Maximum of " + MAX_PERMITS + " business permits allowed", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Setup terms and privacy checkbox
        if (tvAgreeText != null) {
            String fullText = "Agree with terms and privacy";
            SpannableString spannableString = new SpannableString(fullText);
            String clickableText = "terms and privacy";
            int startIndex = fullText.indexOf(clickableText);
            int endIndex = startIndex + clickableText.length();
            
            if (startIndex >= 0) {
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        showTermsPrivacyDialog();
                    }
                };
                spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new UnderlineSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                // Set brown color for clickable text
                spannableString.setSpan(new ForegroundColorSpan(0xFFA18167), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            tvAgreeText.setText(spannableString);
            tvAgreeText.setMovementMethod(LinkMovementMethod.getInstance());
        }
        
        // Initialize QR bitmap from selectedQrUri if available
        if (selectedQrUri != null) {
            try {
                ContentResolver resolver = getContentResolver();
                qrBitmap = BitmapFactory.decodeStream(resolver.openInputStream(selectedQrUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        com.google.android.material.card.MaterialCardView cardResultMessage = dialogView.findViewById(R.id.cardResultMessage);
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
                    
                    // Load QR bitmap
                    try {
                        ContentResolver resolver = getContentResolver();
                        qrBitmap = BitmapFactory.decodeStream(resolver.openInputStream(imageUri));
                        Log.d("QR_VALIDATION", "QR bitmap loaded successfully");
                    } catch (Exception e) {
                        Log.e("QR_VALIDATION", "Error loading QR bitmap: " + e.getMessage());
                    }
                    
                    Log.d("QR_VALIDATION", "QR image set successfully");
                    
                    // Show success result
                    tvDialogTitle.setText("Verification Successful");
                    tvResultMessage.setText("✅ Valid GCash QR code detected!");
                    tvResultMessage.setTextColor(ContextCompat.getColor(RegistrationActivity.this, android.R.color.holo_green_dark));
                    cardResultMessage.setVisibility(View.VISIBLE);
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
                    cardResultMessage.setVisibility(View.VISIBLE);
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
        // Initialize barangay spinner with default option
        String[] defaultBarangayArray = {"Select Barangay"};
        ArrayAdapter<String> defaultBarangayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, defaultBarangayArray) {
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
        defaultBarangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangay.setAdapter(defaultBarangayAdapter);
        
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
                    String newMunicipality = parent.getItemAtPosition(position).toString();
                    
                    // If municipality changed, reset barangay
                    if (!newMunicipality.equals(selectedMunicipality)) {
                        clearBarangay();
                        selectedBarangay = "";
                        
                        // Set new municipality and load barangays
                        selectedMunicipality = newMunicipality;
                        loadBarangays(selectedMunicipality);
                    } else {
                        // Same municipality selected, just update
                        selectedMunicipality = newMunicipality;
                    }
                } else {
                    // "Select Municipality" selected
                    selectedMunicipality = "";
                    clearBarangay();
                    selectedBarangay = "";
                }
                updateCompleteAddress();
                // Check section III completion
                checkSectionIIICompletion();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Set up barangay selection listener
        spinnerBarangay.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position > 0) { // Skip "Select Barangay" option
                    selectedBarangay = parent.getItemAtPosition(position).toString();
                } else {
                    selectedBarangay = "";
                }
                updateCompleteAddress();
                // Check section III completion
                checkSectionIIICompletion();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
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
    
    private void loadBarangays(String municipality) {
        try {
            String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/philippine_address_api.php?action=barangays&municipality_name=" + URLEncoder.encode(municipality, "UTF-8");
        
        com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.getBoolean("success")) {
                        org.json.JSONArray barangaysArray = response.getJSONArray("data");
                        
                        if (barangaysArray.length() > 0) {
                            String[] barangayNames = new String[barangaysArray.length() + 1];
                            barangayNames[0] = "Select Barangay";
                            
                            for (int i = 0; i < barangaysArray.length(); i++) {
                                org.json.JSONObject barangay = barangaysArray.getJSONObject(i);
                                barangayNames[i + 1] = barangay.getString("name");
                            }
                            
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                this, android.R.layout.simple_spinner_item, barangayNames) {
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
                            spinnerBarangay.setAdapter(adapter);
                        } else {
                            // No barangays found
                            Log.w("AddressPicker", "No barangays found for municipality: " + municipality);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "No barangays available for this municipality.", Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                } catch (org.json.JSONException e) {
                    Log.e("AddressPicker", "JSON parsing error: " + e.getMessage());
                    e.printStackTrace();
                }
            },
            error -> {
                Log.e("AddressPicker", "Error loading barangays: " + error.getMessage());
                // Show user-friendly message
                if (error.networkResponse != null) {
                    String data = new String(error.networkResponse.data);
                    Log.e("AddressPicker", "Response data: " + data);
                }
                
                // Check if it's due to no data (empty response)
                if (error.getMessage().contains("org.json.JSONException: No value for data")) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "No barangays available for this municipality.", Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
        
        com.android.volley.RequestQueue queue = com.android.volley.toolbox.Volley.newRequestQueue(this);
        queue.add(request);
        } catch (Exception e) {
            Log.e("AddressPicker", "Error encoding municipality name: " + e.getMessage());
            Toast.makeText(this, "Error loading barangays", Toast.LENGTH_SHORT).show();
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
        // Clear barangay spinner
        String[] emptyArray = {"Select Barangay"};
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
        if (spinnerBarangay != null) {
            spinnerBarangay.setAdapter(adapter);
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
     * Reveals a section with a loading animation (1-2 seconds delay)
     * @param sectionWrapper The section wrapper to reveal
     * @param progressBar The progress bar to show during loading
     * @param delayMs Delay in milliseconds (default 1500ms = 1.5 seconds)
     */
    private void revealSectionWithLoading(View sectionWrapper, ProgressBar progressBar, int delayMs) {
        if (sectionWrapper.getVisibility() == View.VISIBLE) {
            return; // Already visible
        }
        
        // Show progress bar
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        // Hide section initially
        sectionWrapper.setVisibility(View.GONE);
        
        // Use Handler to delay section reveal
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // Hide progress bar
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            
            // Show section with animation
            sectionWrapper.setVisibility(View.VISIBLE);
            sectionWrapper.setAlpha(0f);
            sectionWrapper.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setListener(null);
            
            // Update progress indicator
            updateProgressIndicator();
        }, delayMs);
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
            // Section I is complete, reveal Section II with loading animation
            revealSectionWithLoading(sectionIIWrapper, progressBarSection2, 1500);
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
            // Section II is complete, reveal Section III with loading animation
            revealSectionWithLoading(sectionIIIWrapper, progressBarSection3, 1500);
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
        String barangay = spinnerBarangay.getSelectedItem().toString();
        String detailedAddress = etDetailedAddress.getText().toString().trim();
        
        boolean provinceValid = !province.equals("Select Province") && !province.isEmpty();
        boolean municipalityValid = !municipality.equals("Select Municipality") && !municipality.isEmpty();
        boolean barangayValid = !barangay.equals("Select Barangay") && !barangay.isEmpty();
        boolean detailedAddressValid = !detailedAddress.isEmpty();
        
        View sectionIVWrapper = getSectionWrapper(sectionLoginCredentials);
        
        if (provinceValid && municipalityValid && barangayValid && detailedAddressValid) {
            // Section III is complete, reveal Section IV with loading animation
            revealSectionWithLoading(sectionIVWrapper, progressBarSection4, 1500);
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
            if (isBoarder) {
                // For Boarders: Show special message section instead of Section 5, then reveal Section 6
                View sectionVIWrapper = getSectionWrapper(sectionAdditionalInfo);
                
                // Show boarder message section
                if (sectionBoarderMessage != null && sectionBoarderMessage.getVisibility() != View.VISIBLE) {
                    sectionBoarderMessage.setVisibility(View.VISIBLE);
                    sectionBoarderMessage.setAlpha(0f);
                    sectionBoarderMessage.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .setListener(null);
                }
                
                // Hide Section 5 for Boarders
                if (sectionVWrapper.getVisibility() == View.VISIBLE) {
                    sectionVWrapper.setVisibility(View.GONE);
                }
                
                // After delay, reveal Section 6
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (sectionVIWrapper.getVisibility() != View.VISIBLE) {
                        revealSectionWithLoading(sectionVIWrapper, progressBarSection6, 1500);
                    }
                    updateProgressIndicator();
                }, 2000); // 2 second delay before showing Section 6
            } else {
                // For BH Owners: Show Section V with loading animation
                revealSectionWithLoading(sectionVWrapper, progressBarSection5, 1500);
                
                // Hide boarder message section
                if (sectionBoarderMessage != null) {
                    sectionBoarderMessage.setVisibility(View.GONE);
                }
                
                // Also check if Section V is complete to reveal Section VI
                checkSectionVCompletion();
            }
        } else {
            // Section IV is incomplete, hide Section V and boarder message
            if (sectionVWrapper.getVisibility() == View.VISIBLE) {
                sectionVWrapper.setVisibility(View.GONE);
            }
            if (sectionBoarderMessage != null && sectionBoarderMessage.getVisibility() == View.VISIBLE) {
                sectionBoarderMessage.setVisibility(View.GONE);
            }
            updateProgressIndicator();
        }
    }
    
    /**
     * Checks if Section V (Payment Information) completion
     * BH Owners need both GCash number and QR code
     * Boarders skip Section 5 entirely (handled in checkSectionIVCompletion)
     */
    private void checkSectionVCompletion() {
        String selectedRole = spinnerRole.getSelectedItem().toString();
        boolean isBoarder = "Boarder".equals(selectedRole);
        
        // Boarders don't use Section 5, so return early
        if (isBoarder) {
            return;
        }
        
        // For BH Owner, check if GCash number and QR code are provided
        String gcashNum = etGcashNum.getText().toString().trim();
        // Check if it starts with +63 and has 10 digits after it
        boolean gcashNumValid = !gcashNum.isEmpty() && gcashNum.startsWith("+63") && 
                                gcashNum.length() >= 14 && 
                                gcashNum.substring(4).replaceAll("[^0-9]", "").length() >= 10;
        boolean qrCodeValid = selectedQrUri != null;
        
        View sectionVIWrapper = getSectionWrapper(sectionAdditionalInfo);
        
        // Section V is complete, reveal Section VI with loading animation
        if (gcashNumValid && qrCodeValid && sectionVIWrapper.getVisibility() != View.VISIBLE) {
            revealSectionWithLoading(sectionVIWrapper, progressBarSection6, 1500);
        }
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
            if (progressBarSection2 != null) {
                progressBarSection2.setVisibility(View.GONE);
            }
        }
        
        // Hide Section III (Address) if we're starting from section 3 onwards
        if (startSectionNumber <= 3) {
            View sectionIIIWrapper = getSectionWrapper(sectionAddress);
            if (sectionIIIWrapper.getVisibility() == View.VISIBLE) {
                sectionIIIWrapper.setVisibility(View.GONE);
            }
            if (progressBarSection3 != null) {
                progressBarSection3.setVisibility(View.GONE);
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
            if (progressBarSection4 != null) {
                progressBarSection4.setVisibility(View.GONE);
            }
        }
        
        // Hide Section V (Payment Info) if we're starting from section 5 onwards
        if (startSectionNumber <= 5) {
            View sectionVWrapper = getSectionWrapper(sectionPaymentInfo);
            if (progressBarSection5 != null) {
                progressBarSection5.setVisibility(View.GONE);
            }
            if (sectionVWrapper.getVisibility() == View.VISIBLE) {
                sectionVWrapper.setVisibility(View.GONE);
            }
            // Also hide boarder message section
            if (sectionBoarderMessage != null) {
                sectionBoarderMessage.setVisibility(View.GONE);
            }
        }
        
        // Hide Section VI (Additional Info) if we're starting from section 6 onwards
        if (startSectionNumber <= 6) {
            View sectionVIWrapper = getSectionWrapper(sectionAdditionalInfo);
            if (progressBarSection6 != null) {
                progressBarSection6.setVisibility(View.GONE);
            }
            if (sectionVIWrapper != null && sectionVIWrapper.getVisibility() == View.VISIBLE) {
                sectionVIWrapper.setVisibility(View.GONE);
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
            progressCircle4 == null || progressCircle5 == null || progressCircle6 == null) {
            return; // Progress circles not initialized yet
        }
        
        // Circle 1 is always filled (Section 1 is always visible)
        progressCircle1.setBackgroundResource(R.drawable.progress_circle_filled);
        progressCircle1.setTextColor(getResources().getColor(android.R.color.white));
        
        // Check visibility of each section wrapper and fill corresponding circle
        View sectionIIWrapper = getSectionWrapper(sectionPersonalInfo);
        if (sectionIIWrapper != null && sectionIIWrapper.getVisibility() == View.VISIBLE) {
            progressCircle2.setBackgroundResource(R.drawable.progress_circle_filled);
            progressCircle2.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            progressCircle2.setBackgroundResource(R.drawable.progress_circle_hollow);
            progressCircle2.setTextColor(0xFF666666);
        }
        
        View sectionIIIWrapper = getSectionWrapper(sectionAddress);
        if (sectionIIIWrapper != null && sectionIIIWrapper.getVisibility() == View.VISIBLE) {
            progressCircle3.setBackgroundResource(R.drawable.progress_circle_filled);
            progressCircle3.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            progressCircle3.setBackgroundResource(R.drawable.progress_circle_hollow);
            progressCircle3.setTextColor(0xFF666666);
        }
        
        View sectionIVWrapper = getSectionWrapper(sectionLoginCredentials);
        if (sectionIVWrapper != null && sectionIVWrapper.getVisibility() == View.VISIBLE) {
            progressCircle4.setBackgroundResource(R.drawable.progress_circle_filled);
            progressCircle4.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            progressCircle4.setBackgroundResource(R.drawable.progress_circle_hollow);
            progressCircle4.setTextColor(0xFF666666);
        }
        
        View sectionVWrapper = getSectionWrapper(sectionPaymentInfo);
        // Circle 5 is filled if Section 5 is visible OR if boarder message is visible (for Boarders)
        if ((sectionVWrapper != null && sectionVWrapper.getVisibility() == View.VISIBLE) ||
            (sectionBoarderMessage != null && sectionBoarderMessage.getVisibility() == View.VISIBLE)) {
            progressCircle5.setBackgroundResource(R.drawable.progress_circle_filled);
            progressCircle5.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            progressCircle5.setBackgroundResource(R.drawable.progress_circle_hollow);
            progressCircle5.setTextColor(0xFF666666);
        }
        
        View sectionVIWrapper = getSectionWrapper(sectionAdditionalInfo);
        if (sectionVIWrapper != null && sectionVIWrapper.getVisibility() == View.VISIBLE) {
            progressCircle6.setBackgroundResource(R.drawable.progress_circle_filled);
            progressCircle6.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            progressCircle6.setBackgroundResource(R.drawable.progress_circle_hollow);
            progressCircle6.setTextColor(0xFF666666);
        }
    }
    
    /**
     * Sets up validation for name fields (First, Last, Middle)
     * - Capital letters by default
     * - Only letters, spaces, hyphens, apostrophes allowed
     * - Min: 2 chars (Middle name can be optional)
     * - Max: 50 chars
     * - Real-time validation with visual feedback
     */
    private void setupNameFieldValidation(EditText editText, TextView errorTextView, String fieldName) {
        // Add input filter to restrict characters (letters, spaces, hyphens, apostrophes only)
        android.text.InputFilter[] filters = new android.text.InputFilter[] {
            new android.text.InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end,
                                         android.text.Spanned dest, int dstart, int dend) {
                    // Allow letters, spaces, hyphens, and apostrophes
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);
                        if (!Character.isLetter(c) && c != ' ' && c != '-' && c != '\'') {
                            return ""; // Reject the character
                        }
                    }
                    return null; // Accept the input
                }
            },
            new android.text.InputFilter.LengthFilter(50) // Max 50 characters
        };
        editText.setFilters(filters);
        
        // Add TextWatcher for real-time validation
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String text = s.toString().trim();
                String error = validateNameField(text, fieldName, fieldName.equals("Middle Name"));
                showFieldError(editText, errorTextView, error);
                
                // Check section completion if it's First or Last name
                if (fieldName.equals("First Name") || fieldName.equals("Last Name")) {
                    checkSectionIICompletion();
                }
            }
        });
    }
    
    /**
     * Validates a name field and returns error message if invalid
     */
    private String validateNameField(String text, String fieldName, boolean isOptional) {
        if (text.isEmpty()) {
            if (isOptional) {
                return null; // Middle name is optional
            }
            return fieldName + " is required";
        }
        
        if (text.length() < 2) {
            return fieldName + " must be at least 2 characters";
        }
        
        if (text.length() > 50) {
            return fieldName + " must not exceed 50 characters";
        }
        
        // Check for invalid characters (only letters, spaces, hyphens, apostrophes allowed)
        if (!text.matches("^[a-zA-Z\\s\\-']+$")) {
            return fieldName + " can only contain letters, spaces, hyphens, and apostrophes";
        }
        
        return null; // Valid
    }
    
    /**
     * Sets up validation for address fields (Barangay and Detailed Address)
     * - Must start with capital letter
     * - Minimum 3 characters
     * - Real-time validation with visual feedback
     */
    private void setupAddressFieldValidation() {
        // Barangay is now a spinner, so validation is handled in the spinner listener
        // No text watcher needed for spinner
        
        // Detailed Address validation
        etDetailedAddress.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String text = s.toString();
                
                // Capitalize first letter if field is being edited
                if (text.length() > 0 && Character.isLowerCase(text.charAt(0))) {
                    int cursorPosition = etDetailedAddress.getSelectionStart();
                    s.replace(0, 1, String.valueOf(Character.toUpperCase(text.charAt(0))));
                    etDetailedAddress.setSelection(Math.min(cursorPosition, s.length()));
                }
                
                String trimmedText = text.trim();
                String error = validateAddressField(trimmedText, "Detailed Address");
                showFieldError(etDetailedAddress, tvDetailedAddressError, error);
                
                checkSectionIIICompletion();
            }
        });
    }
    
    /**
     * Validates an address field (Barangay or Detailed Address)
     */
    private String validateAddressField(String text, String fieldName) {
        if (text.isEmpty()) {
            return fieldName + " is required";
        }
        
        if (text.length() < 3) {
            return fieldName + " must be at least 3 characters";
        }
        
        // Check if starts with capital letter
        if (!text.isEmpty() && !Character.isUpperCase(text.charAt(0))) {
            return fieldName + " must start with a capital letter";
        }
        
        return null; // Valid
    }
    
    /**
     * Validates birth date and shows error if user is not 18+ years old
     */
    private void validateBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isEmpty()) {
            showFieldError(etBirthDate, tvBirthDateError, "Birth Date is required");
            return;
        }
        
        // Validate date format (MM/DD/YYYY)
        if (!birthDate.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$")) {
            showFieldError(etBirthDate, tvBirthDateError, "Birth Date must be in MM/DD/YYYY format");
            return;
        }
        
        try {
            String[] dateParts = birthDate.split("/");
            int month = Integer.parseInt(dateParts[0]);
            int day = Integer.parseInt(dateParts[1]);
            int year = Integer.parseInt(dateParts[2]);
            
            // Calculate age
            Calendar birthCalendar = Calendar.getInstance();
            birthCalendar.set(year, month - 1, day);
            Calendar now = Calendar.getInstance();
            int age = now.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR);
            if (now.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            
            if (age < 18) {
                showFieldError(etBirthDate, tvBirthDateError, "You must be at least 18 years old to register");
            } else {
                showFieldError(etBirthDate, tvBirthDateError, null); // Clear error
            }
        } catch (Exception e) {
            showFieldError(etBirthDate, tvBirthDateError, "Invalid date format");
        }
    }
    
    /**
     * Shows or hides field error with visual feedback
     * @param editText The EditText field
     * @param errorTextView The TextView to show error message
     * @param errorMessage The error message (null to clear error)
     */
    private void showFieldError(EditText editText, TextView errorTextView, String errorMessage) {
        if (errorTextView == null || editText == null) {
            return;
        }
        
        if (errorMessage != null && !errorMessage.isEmpty()) {
            // Show error
            errorTextView.setText(errorMessage);
            errorTextView.setVisibility(View.VISIBLE);
            editText.setBackgroundResource(R.drawable.edittext_background_error);
        } else {
            // Clear error
            errorTextView.setVisibility(View.GONE);
            editText.setBackgroundResource(R.drawable.edittext_background);
        }
    }
    
    /**
     * Checks if any field in the registration form has been filled
     * @return true if at least one field has been filled, false otherwise
     */
    private boolean hasAnyFieldFilled() {
        try {
            // Check account type
            if (spinnerRole != null && spinnerRole.getSelectedItem() != null) {
                String selectedRole = spinnerRole.getSelectedItem().toString();
                if (!selectedRole.equals("Select --") && !selectedRole.isEmpty()) {
                    return true;
                }
            }
            
            // Check personal information fields
            if (etFirstName != null && !etFirstName.getText().toString().trim().isEmpty()) {
                return true;
            }
            if (etLastName != null && !etLastName.getText().toString().trim().isEmpty()) {
                return true;
            }
            if (etMiddleName != null && !etMiddleName.getText().toString().trim().isEmpty()) {
                return true;
            }
            if (etBirthDate != null && !etBirthDate.getText().toString().trim().isEmpty()) {
                return true;
            }
            if (etPhone != null) {
                String phoneText = etPhone.getText().toString().trim();
                // Phone field starts with "+63 ", so if it's longer, user has entered digits
                if (phoneText.length() > 4) {
                    return true;
                }
            }
            
            // Check address fields
            if (spinnerProvince != null && spinnerProvince.getSelectedItem() != null) {
                String province = spinnerProvince.getSelectedItem().toString();
                if (!province.equals("Select Province") && !province.isEmpty()) {
                    return true;
                }
            }
            if (spinnerMunicipality != null && spinnerMunicipality.getSelectedItem() != null) {
                String municipality = spinnerMunicipality.getSelectedItem().toString();
                if (!municipality.equals("Select Municipality") && !municipality.isEmpty()) {
                    return true;
                }
            }
            if (spinnerBarangay != null && spinnerBarangay.getSelectedItemPosition() > 0) {
                return true;
            }
            if (etDetailedAddress != null && !etDetailedAddress.getText().toString().trim().isEmpty()) {
                return true;
            }
            
            // Check login credentials
            if (etEmail != null && !etEmail.getText().toString().trim().isEmpty()) {
                return true;
            }
            if (etPassword != null && !etPassword.getText().toString().trim().isEmpty()) {
                return true;
            }
            
            // Check payment information
            if (etGcashNum != null) {
                String gcashText = etGcashNum.getText().toString().trim();
                // GCash field starts with "+63 ", so if it's longer, user has entered digits
                if (gcashText.length() > 4) {
                    return true;
                }
            }
            if (selectedQrUri != null) {
                return true;
            }
        } catch (Exception e) {
            // If any error occurs, assume fields might be filled to be safe
            Log.e("RegistrationActivity", "Error checking filled fields: " + e.getMessage());
            return true;
        }
        
        return false;
    }
    
    /**
     * Shows a confirmation dialog before navigating back
     */
    private void showExitConfirmationDialog() {
        // Create custom dialog view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_exit_confirmation, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // Get dialog views
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnContinue = dialogView.findViewById(R.id.btnContinue);
        
        // Cancel button - dismiss dialog
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Continue button - proceed with exit
        btnContinue.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToLogin();
        });
        
        // Show dialog
        dialog.show();
        
        // Style the dialog window
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }
    
    /**
     * Handles back navigation with confirmation if fields are filled
     */
    private void handleBackNavigation() {
        if (hasAnyFieldFilled()) {
            // Show confirmation dialog
            showExitConfirmationDialog();
        } else {
            // No fields filled, proceed directly
            navigateToLogin();
        }
    }
    
    /**
     * Navigates to Login activity
     */
    private void navigateToLogin() {
        Intent intent = new Intent(RegistrationActivity.this, Login.class);
        startActivity(intent);
        finish();
    }
    
    /**
     * Verifies and sets front ID image if approved
     */
    private void verifyAndSetFrontImage(Uri imageUri) {
        String selectedIdType = spinnerVId.getSelectedItem().toString();
        if (selectedIdType == null || selectedIdType.equals("Select --")) {
            Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Verifying front ID document...", Toast.LENGTH_SHORT).show();
        
        IdVerificationHelper.verifyIdDocument(this, imageUri, selectedIdType, new IdVerificationHelper.VerificationCallback() {
            @Override
            public void onVerificationComplete(IdVerificationHelper.VerificationResult result) {
                if (result.isValid) {
                    ivUploadF.setImageURI(imageUri);
                    ivUploadF.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    idFrontPath = imageUri.toString();
                    
                    try {
                        ContentResolver resolver = getContentResolver();
                        frontBitmap = BitmapFactory.decodeStream(resolver.openInputStream(imageUri));
                        if (frontBitmap != null && result.extractedIdNumber != null && !result.extractedIdNumber.isEmpty()) {
                            etIdNumber.setText(result.extractedIdNumber);
                        }
                        Toast.makeText(RegistrationActivity.this, result.reason, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e("RegistrationActivity", "Error loading front image", e);
                        Toast.makeText(RegistrationActivity.this, "Error loading front image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RegistrationActivity.this, result.reason, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onVerificationError(String error) {
                Toast.makeText(RegistrationActivity.this, "Front ID verification failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * Verifies and sets back ID image if approved
     */
    private void verifyAndSetBackImage(Uri imageUri) {
        String selectedIdType = spinnerVId.getSelectedItem().toString();
        if (selectedIdType == null || selectedIdType.equals("Select --")) {
            Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Verifying back ID document...", Toast.LENGTH_SHORT).show();
        
        IdVerificationHelper.verifyIdDocument(this, imageUri, selectedIdType, new IdVerificationHelper.VerificationCallback() {
            @Override
            public void onVerificationComplete(IdVerificationHelper.VerificationResult result) {
                if (result.isValid) {
                    ivUploadB.setImageURI(imageUri);
                    ivUploadB.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    idBackPath = imageUri.toString();
                    
                    try {
                        ContentResolver resolver = getContentResolver();
                        backBitmap = BitmapFactory.decodeStream(resolver.openInputStream(imageUri));
                        Toast.makeText(RegistrationActivity.this, result.reason, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e("RegistrationActivity", "Error loading back image", e);
                        Toast.makeText(RegistrationActivity.this, "Error loading back image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RegistrationActivity.this, result.reason, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onVerificationError(String error) {
                Toast.makeText(RegistrationActivity.this, "Back ID verification failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * Performs the registration by sending data to server
     */
    private void performRegistration() {
        if (isRegistering) {
            Toast.makeText(this, "Registration in progress, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String selectedIdType = spinnerVId.getSelectedItem().toString();
        String idNumber = etIdNumber.getText().toString().trim();
        boolean isAgreed = cbAgree.isChecked();
        String selectedRole = spinnerRole.getSelectedItem().toString();
        boolean isBoarder = "Boarder".equals(selectedRole);
        boolean isPassport = selectedIdType.equals("Philippine Passport");
        
        // Set registering flag
        isRegistering = true;
        btnNext.setEnabled(false);
        btnNext.setText("Registering...");
        
        // Prepare data
        String firstName = etFirstName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String suffix = spinnerSuffix.getSelectedItem().toString();
        String birthDate = etBirthDate.getText().toString().trim();
        String phoneFormatted = etPhone.getText().toString().trim();
        String digitsAfterPlus63 = phoneFormatted.substring(4).replaceAll("[^0-9]", "");
        String phoneNumber = "0" + digitsAfterPlus63;
        String address = etAddress.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String gcashFormatted = etGcashNum.getText().toString().trim();
        String gcashDigitsAfterPlus63 = gcashFormatted.substring(4).replaceAll("[^0-9]", "");
        String gcashNumber = "0" + gcashDigitsAfterPlus63;
        
        String UPLOAD_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/insert_registration.php";
        
        VolleyMultipartRequest request = new VolleyMultipartRequest(Request.Method.POST, UPLOAD_URL,
            response -> {
                String responseString = new String(response.data);
                Log.d("RegistrationActivity", "Raw server response: " + responseString);
                
                try {
                    if (responseString.trim().startsWith("<")) {
                        Log.e("RegistrationActivity", "ERROR: Server returned HTML instead of JSON");
                        Toast.makeText(this, "Server error: Invalid response format", Toast.LENGTH_LONG).show();
                        isRegistering = false;
                        btnNext.setEnabled(true);
                        btnNext.setText("Register");
                        return;
                    }
                    
                    JSONObject obj = new JSONObject(responseString);
                    String message = obj.getString("message");
                    boolean success = obj.getBoolean("success");
                    
                    isRegistering = false;
                    btnNext.setEnabled(true);
                    btnNext.setText("Register");
                    
                    if (success) {
                        int permitsInserted = obj.optInt("permits_inserted", 0);
                        if (permitsInserted > 0) {
                            Log.d("RegistrationActivity", "Business permits saved: " + permitsInserted);
                        }
                        proceedToVerification(obj, message);
                    } else {
                        if (message.contains("Duplicate entry") && message.contains("email")) {
                            Toast.makeText(this, "This email is already registered. Please use a different email or try logging in.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (JSONException e) {
                    Log.e("RegistrationActivity", "JSON parsing error: " + e.getMessage());
                    isRegistering = false;
                    btnNext.setEnabled(true);
                    btnNext.setText("Register");
                    Toast.makeText(this, "Server response error. Please try again.", Toast.LENGTH_LONG).show();
                }
            },
            error -> {
                Log.e("RegistrationActivity", "Network error: " + error.getMessage());
                isRegistering = false;
                btnNext.setEnabled(true);
                btnNext.setText("Register");
                Toast.makeText(this, "Registration failed: " + (error.getMessage() != null ? error.getMessage() : "Network error"), Toast.LENGTH_LONG).show();
            }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = super.getHeaders();
                if (headers == null) {
                    headers = new HashMap<>();
                }
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
            
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("role", selectedRole);
                params.put("firstName", firstName);
                params.put("middleName", middleName);
                params.put("lastName", lastName);
                params.put("suffix", suffix != null ? suffix : "None");
                params.put("birthDate", birthDate);
                params.put("phone", phoneNumber);
                params.put("address", address);
                params.put("email", email);
                params.put("password", password);
                
                if (!isBoarder) {
                    params.put("gcashNum", gcashNumber != null ? gcashNumber : "");
                } else {
                    params.put("gcashNum", "");
                }
                
                params.put("idType", selectedIdType);
                params.put("idNumber", idNumber);
                params.put("isAgreed", String.valueOf(isAgreed));
                return params;
            }
            
            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                
                try {
                    byte[] frontData = AppHelper.getFileDataFromDrawable(getBaseContext(), frontBitmap);
                    params.put("idFrontFile", new DataPart("front.jpg", frontData));
                    
                    if (!isPassport && backBitmap != null) {
                        byte[] backData = AppHelper.getFileDataFromDrawable(getBaseContext(), backBitmap);
                        params.put("idBackFile", new DataPart("back.jpg", backData));
                    }
                    
                    if (!isBoarder && qrBitmap != null) {
                        byte[] qrData = AppHelper.getFileDataFromDrawable(getBaseContext(), qrBitmap);
                        params.put("qrFile", new DataPart("qr.jpg", qrData));
                    }
                    
                    // Include business permits
                    if (!isBoarder && !permitUploadItems.isEmpty()) {
                        int permitIndex = 1;
                        for (PermitUploadItem permitItem : permitUploadItems) {
                            if (permitItem.bitmap != null) {
                                byte[] permitData = AppHelper.getFileDataFromDrawable(getBaseContext(), permitItem.bitmap);
                                if (permitData != null && permitData.length > 0) {
                                    String permitKey = "permitFile" + permitIndex;
                                    params.put(permitKey, new DataPart("permit" + permitIndex + ".jpg", permitData));
                                    permitIndex++;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("RegistrationActivity", "Error creating file data: " + e.getMessage());
                }
                return params;
            }
        };
        
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(30000, 3, com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(request);
    }
    
    /**
     * Proceeds to verification or login after successful registration
     */
    private void proceedToVerification(JSONObject obj, String message) {
        try {
            boolean requiresVerification = obj.optBoolean("requires_verification", false);
            if (requiresVerification) {
                Intent intent = new Intent(RegistrationActivity.this, EmailVerificationActivity.class);
                intent.putExtra("email", etEmail.getText().toString().trim());
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(RegistrationActivity.this, Login.class);
                startActivity(intent);
                finish();
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("RegistrationActivity", "Error in proceedToVerification: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new business permit upload item
     */
    private void createPermitUploadItem() {
        if (permitUploadItems.size() >= MAX_PERMITS) {
            Toast.makeText(this, "Maximum of " + MAX_PERMITS + " business permits allowed", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int permitIndex = nextPermitIndex++;
        PermitUploadItem item = new PermitUploadItem();
        item.index = permitIndex;
        
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        TextView label = new TextView(this);
        label.setText("Business Permit " + (permitUploadItems.size() + 1));
        label.setTextColor(getResources().getColor(android.R.color.black));
        label.setTextSize(14);
        label.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        label.setPadding(0, 0, 0, 8);
        itemLayout.addView(label);
        
        ImageView permitImageView = new ImageView(this);
        permitImageView.setId(View.generateViewId());
        float density = getResources().getDisplayMetrics().density;
        int heightInPixels = (int) (200 * density);
        permitImageView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            heightInPixels
        ));
        permitImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        permitImageView.setAdjustViewBounds(true);
        permitImageView.setBackgroundResource(R.drawable.edittext_background);
        permitImageView.setImageResource(R.drawable.upload);
        permitImageView.setPadding(0, 0, 0, 8);
        itemLayout.addView(permitImageView);
        
        Button removeButton = new Button(this);
        removeButton.setText("Remove");
        removeButton.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        removeButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
        removeButton.setTextColor(getResources().getColor(android.R.color.white));
        removeButton.setVisibility(View.GONE);
        itemLayout.addView(removeButton);
        
        if (permitUploadItems.size() > 0) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) itemLayout.getLayoutParams();
            params.topMargin = 16;
            itemLayout.setLayoutParams(params);
        }
        
        item.itemView = itemLayout;
        item.imageView = permitImageView;
        item.removeButton = removeButton;
        
        int launcherIndex = permitUploadItems.size();
        if (launcherIndex < permitImageLaunchers.size()) {
            ActivityResultLauncher<String> permitLauncher = permitImageLaunchers.get(launcherIndex);
            permitImageView.setOnClickListener(v -> {
                currentPermitItemForLauncher = item;
                permitLauncher.launch("image/*");
            });
        }
        
        removeButton.setOnClickListener(v -> removePermitUploadItem(item));
        
        businessPermitContainer.addView(itemLayout);
        permitUploadItems.add(item);
        updateAddPermitButtonVisibility();
    }
    
    /**
     * Handles when a permit image is selected
     */
    private void handlePermitImageSelection(PermitUploadItem item, Uri imageUri) {
        try {
            ContentResolver resolver = getContentResolver();
            Bitmap bitmap = BitmapFactory.decodeStream(resolver.openInputStream(imageUri));
            
            if (bitmap != null) {
                item.bitmap = bitmap;
                item.uri = imageUri;
                item.imageView.setImageBitmap(bitmap);
                item.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                
                if (permitUploadItems.size() > 1) {
                    for (PermitUploadItem permitItem : permitUploadItems) {
                        permitItem.removeButton.setVisibility(View.VISIBLE);
                    }
                }
                
                updateAddPermitButtonVisibility();
                Toast.makeText(this, "Business permit uploaded successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to load permit image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("RegistrationActivity", "Error loading permit image: " + e.getMessage());
            Toast.makeText(this, "Error loading permit image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Removes a permit upload item
     */
    private void removePermitUploadItem(PermitUploadItem item) {
        try {
            int itemIndex = permitUploadItems.indexOf(item);
            if (itemIndex == -1) return;
            
            businessPermitContainer.removeView(item.itemView);
            permitUploadItems.remove(item);
            
            for (int i = 0; i < permitUploadItems.size(); i++) {
                PermitUploadItem permitItem = permitUploadItems.get(i);
                ViewGroup itemLayout = (ViewGroup) permitItem.itemView;
                TextView label = (TextView) itemLayout.getChildAt(0);
                label.setText("Business Permit " + (i + 1));
                
                ImageView imageView = permitItem.imageView;
                if (i < permitImageLaunchers.size()) {
                    ActivityResultLauncher<String> permitLauncher = permitImageLaunchers.get(i);
                    imageView.setOnClickListener(v -> {
                        currentPermitItemForLauncher = permitItem;
                        permitLauncher.launch("image/*");
                    });
                }
            }
            
            if (permitUploadItems.size() <= 1) {
                for (PermitUploadItem permitItem : permitUploadItems) {
                    permitItem.removeButton.setVisibility(View.GONE);
                }
            }
            
            updateAddPermitButtonVisibility();
        } catch (Exception e) {
            Log.e("RegistrationActivity", "Error removing permit item: " + e.getMessage());
        }
    }
    
    /**
     * Updates the visibility of the add permit button
     */
    private void updateAddPermitButtonVisibility() {
        boolean hasUploadedPermits = false;
        for (PermitUploadItem item : permitUploadItems) {
            if (item.bitmap != null) {
                hasUploadedPermits = true;
                break;
            }
        }
        
        if (hasUploadedPermits && permitUploadItems.size() < MAX_PERMITS) {
            btnAddPermit.setVisibility(View.VISIBLE);
        } else {
            btnAddPermit.setVisibility(View.GONE);
        }
    }
    
    /**
     * Shows the Terms and Privacy dialog
     */
    private void showTermsPrivacyDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_terms_privacy, null);
            builder.setView(dialogView);
            
            ImageButton btnCloseTerms = dialogView.findViewById(R.id.btnCloseTerms);
            Button btnCloseTermsDialog = dialogView.findViewById(R.id.btnCloseTermsDialog);
            
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            
            if (btnCloseTerms != null) {
                btnCloseTerms.setOnClickListener(v -> dialog.dismiss());
            }
            if (btnCloseTermsDialog != null) {
                btnCloseTermsDialog.setOnClickListener(v -> dialog.dismiss());
            }
            
            dialog.show();
        } catch (Exception e) {
            Log.e("RegistrationActivity", "Error showing Terms and Privacy dialog: " + e.getMessage());
            Toast.makeText(this, "Error loading Terms and Privacy", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Handles activity result from ID capture
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            String imagePath = data.getStringExtra("image_path");
            String idNumber = data.getStringExtra("id_number");
            String idType = data.getStringExtra("id_type");
            
            if (requestCode == 1001) { // Front ID
                handleFrontIdResult(imagePath, idNumber);
            } else if (requestCode == 1002) { // Back ID
                handleBackIdResult(imagePath, idNumber);
            }
        }
    }
    
    private void handleFrontIdResult(String imagePath, String idNumber) {
        try {
            String selectedIdType = spinnerVId.getSelectedItem().toString();
            if (selectedIdType == null || selectedIdType.equals("Select --")) {
                Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                Uri imageUri = Uri.fromFile(new java.io.File(imagePath));
                Toast.makeText(this, "Verifying front ID document...", Toast.LENGTH_SHORT).show();
                
                IdVerificationHelper.verifyIdDocument(this, imageUri, selectedIdType, new IdVerificationHelper.VerificationCallback() {
                    @Override
                    public void onVerificationComplete(IdVerificationHelper.VerificationResult result) {
                        if (result.isValid) {
                            frontBitmap = bitmap;
                            idFrontPath = imagePath;
                            ivUploadF.setImageBitmap(bitmap);
                            ivUploadF.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            
                            String finalIdNumber = result.extractedIdNumber != null && !result.extractedIdNumber.isEmpty() 
                                ? result.extractedIdNumber 
                                : (idNumber != null && !idNumber.isEmpty() ? idNumber : "");
                            
                            if (!finalIdNumber.isEmpty()) {
                                etIdNumber.setText(finalIdNumber);
                            }
                            
                            Toast.makeText(RegistrationActivity.this, result.reason, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegistrationActivity.this, result.reason, Toast.LENGTH_LONG).show();
                        }
                    }
                    
                    @Override
                    public void onVerificationError(String error) {
                        Toast.makeText(RegistrationActivity.this, "Front ID verification failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("RegistrationActivity", "Error handling front ID result: " + e.getMessage());
        }
    }
    
    private void handleBackIdResult(String imagePath, String idNumber) {
        try {
            String selectedIdType = spinnerVId.getSelectedItem().toString();
            if (selectedIdType == null || selectedIdType.equals("Select --")) {
                Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                Uri imageUri = Uri.fromFile(new java.io.File(imagePath));
                Toast.makeText(this, "Verifying back ID document...", Toast.LENGTH_SHORT).show();
                
                IdVerificationHelper.verifyIdDocument(this, imageUri, selectedIdType, new IdVerificationHelper.VerificationCallback() {
                    @Override
                    public void onVerificationComplete(IdVerificationHelper.VerificationResult result) {
                        if (result.isValid) {
                            backBitmap = bitmap;
                            idBackPath = imagePath;
                            ivUploadB.setImageBitmap(bitmap);
                            ivUploadB.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            Toast.makeText(RegistrationActivity.this, result.reason, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegistrationActivity.this, result.reason, Toast.LENGTH_LONG).show();
                        }
                    }
                    
                    @Override
                    public void onVerificationError(String error) {
                        Toast.makeText(RegistrationActivity.this, "Back ID verification failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("RegistrationActivity", "Error handling back ID result: " + e.getMessage());
        }
    }
    
    /**
     * Override back button press to show confirmation if fields are filled
     */
    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }
}
