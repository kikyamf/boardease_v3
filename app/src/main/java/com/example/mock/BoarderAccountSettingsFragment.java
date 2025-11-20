package com.example.mock;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * BoarderAccountSettingsFragment - Account settings and profile management
 * Allows users to edit personal information and change password
 */
public class BoarderAccountSettingsFragment extends Fragment {

    // Views
    private ImageButton btnBack;
    private ProgressBar progressBar;
    
    // Collapsible Personal Information Section
    private View llPersonalInfoHeader;
    private View llPersonalInfoFields;
    private ImageView ivExpandCollapsePersonal;
    private boolean isPersonalInfoSectionExpanded = false;
    
    // Collapsible Privacy Section
    private View llPrivacyHeader;
    private View llPasswordFields;
    private ImageView ivExpandCollapse;
    private boolean isPasswordSectionExpanded = false;
    
    // Collapsible Gcash Section
    private View llGcashHeader;
    private View llGcashFields;
    private ImageView ivExpandCollapseGcash;
    private TextInputEditText etGcashNumber;
    private MaterialButton btnChangeGcash;
    private boolean isGcashSectionExpanded = false;

    // Personal Information Fields
    private TextInputEditText etFirstName;
    private TextInputEditText etMiddleName;
    private TextInputEditText etLastName;
    private TextInputEditText etSuffix;
    private TextInputEditText etEmail;
    private TextInputEditText etContactNumber;
    private TextInputEditText etBirthdate;
    private TextInputEditText etAddress;
    private MaterialButton btnSaveChanges;

    // Password Change Fields (using EditText to match layout)
    private android.widget.EditText etCurrentPassword;
    private android.widget.EditText etNewPassword;
    private android.widget.EditText etConfirmPassword;
    private MaterialButton btnUpdatePassword;
    
    // Password visibility states
    private boolean isCurrentPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    // SharedPreferences for storing user data
    private SharedPreferences userPrefs;
    private static final String PREFS_NAME = "boarder_user_prefs";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_MIDDLE_NAME = "middle_name";
    private static final String KEY_LAST_NAME = "last_name";
    private static final String KEY_SUFFIX = "suffix";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_CONTACT = "contact_number";
    private static final String KEY_BIRTHDATE = "birthdate";
    private static final String KEY_ADDRESS = "address";
    
    // API URL
    // ============================================================================
    // LOCAL DEVELOPMENT (Testing on local network with local database)
    // ============================================================================
    // Use this when testing with your local database on this PC
    // Current detected IP: 192.168.137.1 (your PC's local IP)
    // If this doesn't work, check your IP with: ipconfig (Windows)
    // Common options:
    //   - http://192.168.137.1/boardease_v3/get_boarder_info.php (current IP)
    //   - http://192.168.1.3/boardease_v3/get_boarder_info.php (common local IP)
    //   - http://localhost/boardease_v3/get_boarder_info.php (if using emulator)
    //   - http://10.0.2.2/boardease_v3/get_boarder_info.php (Android emulator localhost)
    private static final String API_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_boarder_info.php";
    
    // ============================================================================
    // PRODUCTION (For final deployment with ngrok)
    // ============================================================================
    // Uncomment the line below and comment out the local URL above when deploying
    // private static final String API_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_boarder_info.php";
    // ============================================================================
    
    private static final String TAG = "BoarderAccountSettings";

    public BoarderAccountSettingsFragment() {
        // Required empty public constructor
    }

    public static BoarderAccountSettingsFragment newInstance() {
        return new BoarderAccountSettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boarder_account_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        initializeSharedPreferences();
        setupClickListeners();
        loadUserData();
    }

    private void initializeViews(View view) {
        try {
            Log.d("BoarderAccountSettings", "initializeViews called"); // Debug log
            // Header views
            btnBack = view.findViewById(R.id.btnBack);
            progressBar = view.findViewById(R.id.progressBar);

            // Personal Information fields
            etFirstName = view.findViewById(R.id.etFirstName);
            etMiddleName = view.findViewById(R.id.etMiddleName);
            etLastName = view.findViewById(R.id.etLastName);
            etSuffix = view.findViewById(R.id.etSuffix);
            etEmail = view.findViewById(R.id.etEmail);
            etContactNumber = view.findViewById(R.id.etContactNumber);
            etBirthdate = view.findViewById(R.id.etBirthdate);
            etAddress = view.findViewById(R.id.etAddress);
            btnSaveChanges = view.findViewById(R.id.btnSaveChanges);

            // Password change fields
            etCurrentPassword = view.findViewById(R.id.etCurrentPassword);
            etNewPassword = view.findViewById(R.id.etNewPassword);
            etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
            btnUpdatePassword = view.findViewById(R.id.btnUpdatePassword);
            
            // Collapsible Personal Information Section
            llPersonalInfoHeader = view.findViewById(R.id.llPersonalInfoHeader);
            llPersonalInfoFields = view.findViewById(R.id.llPersonalInfoFields);
            ivExpandCollapsePersonal = view.findViewById(R.id.ivExpandCollapsePersonal);
            
            // Collapsible Privacy Section
            llPrivacyHeader = view.findViewById(R.id.llPrivacyHeader);
            llPasswordFields = view.findViewById(R.id.llPasswordFields);
            ivExpandCollapse = view.findViewById(R.id.ivExpandCollapse);
            
            // Collapsible Gcash Section
            llGcashHeader = view.findViewById(R.id.llGcashHeader);
            llGcashFields = view.findViewById(R.id.llGcashFields);
            ivExpandCollapseGcash = view.findViewById(R.id.ivExpandCollapseGcash);
            etGcashNumber = view.findViewById(R.id.etGcashNumber);
            btnChangeGcash = view.findViewById(R.id.btnChangeGcash);
            
            // Debug: Log Gcash views
            Log.d("BoarderAccountSettings", "Gcash views initialization:");
            Log.d("BoarderAccountSettings", "  - llGcashHeader: " + (llGcashHeader != null ? "FOUND" : "NULL"));
            Log.d("BoarderAccountSettings", "  - llGcashFields: " + (llGcashFields != null ? "FOUND" : "NULL"));
            Log.d("BoarderAccountSettings", "  - ivExpandCollapseGcash: " + (ivExpandCollapseGcash != null ? "FOUND" : "NULL"));
            Log.d("BoarderAccountSettings", "  - etGcashNumber: " + (etGcashNumber != null ? "FOUND" : "NULL"));
            Log.d("BoarderAccountSettings", "  - btnChangeGcash: " + (btnChangeGcash != null ? "FOUND" : "NULL"));
            
            // Initialize all sections as collapsed
            initializeSectionsCollapsed();
            
            Log.d("BoarderAccountSettings", "llPersonalInfoHeader found: " + (llPersonalInfoHeader != null));
            Log.d("BoarderAccountSettings", "llPersonalInfoFields found: " + (llPersonalInfoFields != null));
            Log.d("BoarderAccountSettings", "llPrivacyHeader found: " + (llPrivacyHeader != null));
            Log.d("BoarderAccountSettings", "llPasswordFields found: " + (llPasswordFields != null));
            Log.d("BoarderAccountSettings", "ivExpandCollapse found: " + (ivExpandCollapse != null));
            Log.d("BoarderAccountSettings", "llGcashHeader found: " + (llGcashHeader != null));
            Log.d("BoarderAccountSettings", "llGcashFields found: " + (llGcashFields != null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeSharedPreferences() {
        try {
            if (getContext() != null) {
                userPrefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeSectionsCollapsed() {
        try {
            // Set Personal Information section as collapsed
            if (llPersonalInfoFields != null) {
                llPersonalInfoFields.setVisibility(View.GONE);
            }
            if (ivExpandCollapsePersonal != null) {
                ivExpandCollapsePersonal.setRotation(0f);
            }
            
            // Set Gcash section as collapsed
            if (llGcashFields != null) {
                llGcashFields.setVisibility(View.GONE);
            }
            if (ivExpandCollapseGcash != null) {
                ivExpandCollapseGcash.setRotation(0f);
            }
            
            // Set Privacy section as collapsed (already has visibility="gone" in XML, but ensure it's set)
            if (llPasswordFields != null) {
                llPasswordFields.setVisibility(View.GONE);
            }
            if (ivExpandCollapse != null) {
                ivExpandCollapse.setRotation(0f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupClickListeners() {
        try {
            Log.d("BoarderAccountSettings", "setupClickListeners called"); // Debug log
            // Back button
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    try {
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            
            // Personal Information Section Collapsible
            if (llPersonalInfoHeader != null) {
                llPersonalInfoHeader.setOnClickListener(v -> {
                    try {
                        Log.d("BoarderAccountSettings", "Personal Info header clicked!");
                        togglePersonalInfoSection();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            
            // Privacy Section Collapsible
            if (llPrivacyHeader != null) {
                llPrivacyHeader.setOnClickListener(v -> {
                    try {
                        Log.d("BoarderAccountSettings", "Privacy header clicked!");
                        togglePasswordSection();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Birthdate picker
            if (etBirthdate != null) {
                etBirthdate.setOnClickListener(v -> {
                    try {
                        showDatePickerDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Save Changes button
            if (btnSaveChanges != null) {
                btnSaveChanges.setOnClickListener(v -> {
                    try {
                        savePersonalInformation();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Update Password button
            if (btnUpdatePassword != null) {
                btnUpdatePassword.setOnClickListener(v -> {
                    try {
                        updatePassword();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            
            // Gcash Section Collapsible
            if (llGcashHeader != null) {
                Log.d("BoarderAccountSettings", "Setting up Gcash header click listener - View found: " + llGcashHeader.toString());
                
                // Remove any existing click listeners
                llGcashHeader.setOnClickListener(null);
                
                // Set up new click listener
                llGcashHeader.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("BoarderAccountSettings", "*** Gcash header onClick called! ***");
                        try {
                            toggleGcashSection();
                        } catch (Exception e) {
                            Log.e("BoarderAccountSettings", "Error in Gcash header click: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
                
                // Also make sure it's clickable and focusable
                llGcashHeader.setClickable(true);
                llGcashHeader.setFocusable(true);
                llGcashHeader.setFocusableInTouchMode(true);
                
                // Test if view is clickable
                boolean isClickable = llGcashHeader.isClickable();
                boolean isFocusable = llGcashHeader.isFocusable();
                Log.d("BoarderAccountSettings", "Gcash header - Clickable: " + isClickable + ", Focusable: " + isFocusable);
                Log.d("BoarderAccountSettings", "Gcash header click listener set successfully");
            } else {
                Log.e("BoarderAccountSettings", "*** llGcashHeader is NULL! Cannot set click listener ***");
            }
            
            // Change Gcash button
            if (btnChangeGcash != null) {
                btnChangeGcash.setOnClickListener(v -> {
                    try {
                        updateGcashNumber();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserData() {
        // Show loading indicator
        setLoading(true);
        
        // Get current user ID
        String userId = Login.getCurrentUserId(getContext());
        
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID not found");
            setLoading(false);
            Toast.makeText(getContext(), "User ID not found. Please login again.", Toast.LENGTH_LONG).show();
            // Set fallback values
            setFallbackValues();
            return;
        }
        
        // Create request queue
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        
        // Build URL with user_id parameter for GET request
        String urlWithParams = API_URL + "?user_id=" + userId;
        
        // Create string request with GET method (same as other working endpoints)
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlWithParams,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d(TAG, "API Response: " + response);
                            
                            // Check if response is null or empty
                            if (response == null || response.trim().isEmpty()) {
                                Log.e(TAG, "Received null or empty response");
                                showError("Server returned empty response");
                                return;
                            }
                            
                            // Check if response is HTML (ngrok warning page)
                            if (response.trim().startsWith("<!DOCTYPE html>") || 
                                (response.contains("ngrok") && response.contains("<html"))) {
                                Log.e(TAG, "Received ngrok warning page instead of JSON");
                                showError("Ngrok warning! Please try again.");
                                return;
                            }
                            
                            // Parse JSON response
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            
                            if (success) {
                                JSONObject data = jsonResponse.getJSONObject("data");
                                
                                // Extract all fields from JSON
                                String firstName = data.optString("first_name", "");
                                String middleName = data.optString("middle_name", "");
                                String lastName = data.optString("last_name", "");
                                String suffix = data.optString("suffix", "");
                                String email = data.optString("email", "");
                                String contact = data.optString("contact", "");
                                String birthdate = data.optString("birthdate", "");
                                String address = data.optString("address", "");
                                
                                // Handle suffix - if null or "null", display "none"
                                if (suffix == null || suffix.isEmpty() || suffix.equalsIgnoreCase("null")) {
                                    suffix = "none";
                                }
                                 
                                // Update UI with fetched data
                                if (etFirstName != null) etFirstName.setText(firstName);
                                if (etMiddleName != null) etMiddleName.setText(middleName);
                                if (etLastName != null) etLastName.setText(lastName);
                                if (etSuffix != null) etSuffix.setText(suffix);
                                if (etEmail != null) etEmail.setText(email);
                                if (etContactNumber != null) etContactNumber.setText(contact);
                                if (etBirthdate != null) etBirthdate.setText(birthdate);
                                if (etAddress != null) etAddress.setText(address);
                                
                                // Load Gcash number if available
                                String gcashNumber = data.optString("gcash_number", "");
                                if (gcashNumber == null || gcashNumber.isEmpty() || gcashNumber.equalsIgnoreCase("null")) {
                                    // Try alternative field name
                                    gcashNumber = data.optString("gcash_num", "");
                                }
                                if (etGcashNumber != null) {
                                    if (gcashNumber != null && !gcashNumber.isEmpty() && !gcashNumber.equalsIgnoreCase("null")) {
                                        etGcashNumber.setText(gcashNumber);
                                        Log.d(TAG, "GCash number loaded: " + gcashNumber);
                                    } else {
                                        etGcashNumber.setText("");
                                        Log.d(TAG, "No GCash number found in response");
                                    }
                                }
                                
                                Log.d(TAG, "Successfully loaded boarder profile data");
                            } else {
                                String error = jsonResponse.optString("error", "Unknown error occurred");
                                Log.e(TAG, "API Error: " + error);
                                showError("Failed to load profile: " + error);
                                setFallbackValues();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            Log.e(TAG, "Response that failed to parse: " + response);
                            showError("Error parsing server response: " + e.getMessage());
                            setFallbackValues();
                        } catch (Exception e) {
                            Log.e(TAG, "Unexpected error: " + e.getMessage());
                            showError("Unexpected error: " + e.getMessage());
                            setFallbackValues();
                        } finally {
                            setLoading(false);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error: " + error.getMessage());
                        setLoading(false);
                        String errorMessage = "Network error: ";
                        if (error.getMessage() != null) {
                            errorMessage += error.getMessage();
                        } else if (error.networkResponse != null) {
                            errorMessage += "HTTP " + error.networkResponse.statusCode;
                        } else {
                            errorMessage += "Unknown network error";
                        }
                        showError(errorMessage);
                        setFallbackValues();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("ngrok-skip-browser-warning", "any");
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        
        // Add request to queue
        requestQueue.add(stringRequest);
    }
    
    private void setFallbackValues() {
        try {
            // Set fallback values from SharedPreferences if available
            String fullName = Login.getCurrentUserName(getContext());
            String middleName = Login.getCurrentUserMiddleName(getContext());
            String suffix = Login.getCurrentUserSuffix(getContext());
            String email = Login.getCurrentUserEmail(getContext());
            String contact = Login.getCurrentUserPhone(getContext());
            String birthdate = Login.getCurrentUserBirthDate(getContext());
            String address = Login.getCurrentUserAddress(getContext());

            // Extract first and last name from the full name
            String firstName = "";
            String lastName = "";
            
            if (fullName != null && !fullName.isEmpty()) {
                String[] nameParts = fullName.split(" ");
                if (nameParts.length >= 2) {
                    firstName = nameParts[0];
                    lastName = nameParts[nameParts.length - 1];
                } else if (nameParts.length == 1) {
                    firstName = nameParts[0];
                }
            }

            // Set default values if data is null or empty
            if (firstName == null || firstName.isEmpty()) firstName = "";
            if (middleName == null) middleName = "";
            if (lastName == null || lastName.isEmpty()) lastName = "";
            // Handle suffix - if null or "none", display "none"
            if (suffix == null || suffix.isEmpty() || suffix.equalsIgnoreCase("null")) {
                suffix = "none";
            }
            if (email == null) email = "";
            if (contact == null) contact = "";
            if (birthdate == null) birthdate = "";
            if (address == null) address = "";

            if (etFirstName != null) etFirstName.setText(firstName);
            if (etMiddleName != null) etMiddleName.setText(middleName);
            if (etLastName != null) etLastName.setText(lastName);
            if (etSuffix != null) etSuffix.setText(suffix);
            if (etEmail != null) etEmail.setText(email);
            if (etContactNumber != null) etContactNumber.setText(contact);
            if (etBirthdate != null) etBirthdate.setText(birthdate);
            if (etAddress != null) etAddress.setText(address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void showDatePickerDialog() {
        try {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        try {
                            String formattedDate = String.format("%04d-%02d-%02d", 
                                    selectedYear, selectedMonth + 1, selectedDay);
                            if (etBirthdate != null) {
                                etBirthdate.setText(formattedDate);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, year, month, day);

            // Set maximum date to today
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePersonalInformation() {
        try {
            // Validate input fields
            if (!validatePersonalInformation()) {
                return;
            }

            // Show loading
            setLoading(true);

            // Get input values
            String firstName = etFirstName.getText().toString().trim();
            String middleName = etMiddleName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String suffix = etSuffix.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String contact = etContactNumber.getText().toString().trim();
            String birthdate = etBirthdate.getText().toString().trim();
            String address = etAddress.getText().toString().trim();

            // Save to SharedPreferences (in real app, save to Firebase)
            if (userPrefs != null) {
                SharedPreferences.Editor editor = userPrefs.edit();
                editor.putString(KEY_FIRST_NAME, firstName);
                editor.putString(KEY_MIDDLE_NAME, middleName);
                editor.putString(KEY_LAST_NAME, lastName);
                editor.putString(KEY_SUFFIX, suffix);
                editor.putString(KEY_EMAIL, email);
                editor.putString(KEY_CONTACT, contact);
                editor.putString(KEY_BIRTHDATE, birthdate);
                editor.putString(KEY_ADDRESS, address);
                editor.apply();
            }

            // Simulate network delay
            new android.os.Handler().postDelayed(() -> {
                try {
                    setLoading(false);
                    Toast.makeText(getContext(), "Personal information updated successfully!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 1500);

        } catch (Exception e) {
            e.printStackTrace();
            setLoading(false);
            Toast.makeText(getContext(), "Error updating information", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validatePersonalInformation() {
        try {
            // Check if fields are empty
            if (TextUtils.isEmpty(etFirstName.getText().toString().trim())) {
                etFirstName.setError("First name is required");
                etFirstName.requestFocus();
                return false;
            }

            if (TextUtils.isEmpty(etLastName.getText().toString().trim())) {
                etLastName.setError("Last name is required");
                etLastName.requestFocus();
                return false;
            }

            if (TextUtils.isEmpty(etEmail.getText().toString().trim())) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return false;
            }

            // Validate email format
            String email = etEmail.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Please enter a valid email address");
                etEmail.requestFocus();
                return false;
            }

            if (TextUtils.isEmpty(etContactNumber.getText().toString().trim())) {
                etContactNumber.setError("Contact number is required");
                etContactNumber.requestFocus();
                return false;
            }

            if (TextUtils.isEmpty(etBirthdate.getText().toString().trim())) {
                etBirthdate.setError("Birthdate is required");
                etBirthdate.requestFocus();
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updatePassword() {
        try {
            // Validate password fields
            if (!validatePasswordFields()) {
                return;
            }

            // Show loading
            setLoading(true);

            // Get password values
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();

            // Simulate current password verification (in real app, verify with Firebase Auth)
            new android.os.Handler().postDelayed(() -> {
                try {
                    // Simulate password verification
                    if (verifyCurrentPassword(currentPassword)) {
                        // Password is correct, proceed with update
                        performPasswordUpdate(newPassword);
                    } else {
                        // Current password is incorrect
                        setLoading(false);
                        etCurrentPassword.setError("Current password is incorrect");
                        etCurrentPassword.requestFocus();
                        Toast.makeText(getContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    setLoading(false);
                    Toast.makeText(getContext(), "Error verifying password", Toast.LENGTH_SHORT).show();
                }
            }, 1500);

        } catch (Exception e) {
            e.printStackTrace();
            setLoading(false);
            Toast.makeText(getContext(), "Error updating password", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean verifyCurrentPassword(String currentPassword) {
        // In a real app, this would verify against Firebase Auth or your backend
        // For demo purposes, we'll simulate with a stored password
        String storedPassword = userPrefs.getString("stored_password", "Demo123");
        return currentPassword.equals(storedPassword);
    }

    private void performPasswordUpdate(String newPassword) {
        try {
            // Simulate password update (in real app, use Firebase Auth)
            new android.os.Handler().postDelayed(() -> {
                try {
                    setLoading(false);
                    
                    // Store new password (in real app, update Firebase Auth)
                    if (userPrefs != null) {
                        SharedPreferences.Editor editor = userPrefs.edit();
                        editor.putString("stored_password", newPassword);
                        editor.apply();
                    }
                    
                    // Clear password fields
                    if (etCurrentPassword != null) etCurrentPassword.setText("");
                    if (etNewPassword != null) etNewPassword.setText("");
                    if (etConfirmPassword != null) etConfirmPassword.setText("");
                    
                    Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 1000);
        } catch (Exception e) {
            e.printStackTrace();
            setLoading(false);
            Toast.makeText(getContext(), "Error updating password", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validatePasswordFields() {
        try {
            // Check if fields are empty
            if (TextUtils.isEmpty(etCurrentPassword.getText().toString().trim())) {
                etCurrentPassword.setError("Current password is required");
                etCurrentPassword.requestFocus();
                return false;
            }

            if (TextUtils.isEmpty(etNewPassword.getText().toString().trim())) {
                etNewPassword.setError("New password is required");
                etNewPassword.requestFocus();
                return false;
            }

            // Enhanced password validation
            String newPassword = etNewPassword.getText().toString().trim();
            String currentPassword = etCurrentPassword.getText().toString().trim();
            
            // Check if new password is same as current password
            if (newPassword.equals(currentPassword)) {
                etNewPassword.setError("New password must be different from current password");
                etNewPassword.requestFocus();
                return false;
            }

            // Check password length
            if (newPassword.length() < 8) {
                etNewPassword.setError("Password must be at least 8 characters");
                etNewPassword.requestFocus();
                return false;
            }

            // Check for strong password requirements
            if (!isStrongPassword(newPassword)) {
                etNewPassword.setError("Password must contain at least one uppercase letter, one lowercase letter, and one number");
                etNewPassword.requestFocus();
                return false;
            }

            if (TextUtils.isEmpty(etConfirmPassword.getText().toString().trim())) {
                etConfirmPassword.setError("Please confirm your new password");
                etConfirmPassword.requestFocus();
                return false;
            }

            // Check if passwords match
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            if (!newPassword.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                etConfirmPassword.requestFocus();
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isStrongPassword(String password) {
        // Check for at least one uppercase letter, one lowercase letter, and one number
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*[0-9].*");
        
        return hasUppercase && hasLowercase && hasNumber;
    }

    private void setLoading(boolean isLoading) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            
            if (btnSaveChanges != null) {
                btnSaveChanges.setEnabled(!isLoading);
            }
            
            if (btnUpdatePassword != null) {
                btnUpdatePassword.setEnabled(!isLoading);
            }
            
            if (btnChangeGcash != null) {
                btnChangeGcash.setEnabled(!isLoading);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleCurrentPasswordVisibility() {
        try {
            if (isCurrentPasswordVisible) {
                etCurrentPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                isCurrentPasswordVisible = false;
            } else {
                etCurrentPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                isCurrentPasswordVisible = true;
            }
            etCurrentPassword.setSelection(etCurrentPassword.getText().length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleNewPasswordVisibility() {
        try {
            if (isNewPasswordVisible) {
                etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                isNewPasswordVisible = false;
            } else {
                etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                isNewPasswordVisible = true;
            }
            etNewPassword.setSelection(etNewPassword.getText().length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleConfirmPasswordVisibility() {
        try {
            if (isConfirmPasswordVisible) {
                etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                isConfirmPasswordVisible = false;
            } else {
                etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                isConfirmPasswordVisible = true;
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupPrivacyHeaderClick() {
        try {
            Log.d("BoarderAccountSettings", "setupPrivacyHeaderClick called");
            if (llPrivacyHeader != null) {
                llPrivacyHeader.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Log.d("BoarderAccountSettings", "Privacy header clicked!");
                            Toast.makeText(getContext(), "Privacy header clicked!", Toast.LENGTH_SHORT).show();
                            
                            // Check if views are initialized
                            if (llPasswordFields == null || ivExpandCollapse == null) {
                                Log.d("BoarderAccountSettings", "Views not initialized yet");
                                return;
                            }
                            
                            togglePasswordSection();
                        } catch (Exception e) {
                            Log.d("BoarderAccountSettings", "Error in privacy header click: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
                Log.d("BoarderAccountSettings", "Privacy header click listener set successfully");
            } else {
                Log.d("BoarderAccountSettings", "llPrivacyHeader is null in setupPrivacyHeaderClick");
            }
        } catch (Exception e) {
            Log.d("BoarderAccountSettings", "Error in setupPrivacyHeaderClick: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void togglePersonalInfoSection() {
        try {
            Log.d("BoarderAccountSettings", "togglePersonalInfoSection called, current state: " + isPersonalInfoSectionExpanded);
            
            if (llPersonalInfoFields == null) {
                Log.d("BoarderAccountSettings", "llPersonalInfoFields is null");
                return;
            }
            if (ivExpandCollapsePersonal == null) {
                Log.d("BoarderAccountSettings", "ivExpandCollapsePersonal is null");
                return;
            }
            
            if (isPersonalInfoSectionExpanded) {
                // Collapse the section
                llPersonalInfoFields.setVisibility(View.GONE);
                ivExpandCollapsePersonal.setRotation(0f); // Point down
                isPersonalInfoSectionExpanded = false;
                Log.d("BoarderAccountSettings", "Personal Info section collapsed");
            } else {
                // Expand the section
                llPersonalInfoFields.setVisibility(View.VISIBLE);
                ivExpandCollapsePersonal.setRotation(180f); // Point up
                isPersonalInfoSectionExpanded = true;
                Log.d("BoarderAccountSettings", "Personal Info section expanded");
            }
        } catch (Exception e) {
            Log.d("BoarderAccountSettings", "Error in togglePersonalInfoSection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void togglePasswordSection() {
        try {
            Log.d("BoarderAccountSettings", "togglePasswordSection called, current state: " + isPasswordSectionExpanded); // Debug log
            
            // Double check views are not null
            if (llPasswordFields == null) {
                Log.d("BoarderAccountSettings", "llPasswordFields is null");
                return;
            }
            if (ivExpandCollapse == null) {
                Log.d("BoarderAccountSettings", "ivExpandCollapse is null");
                return;
            }
            
            if (isPasswordSectionExpanded) {
                // Collapse the section
                llPasswordFields.setVisibility(View.GONE);
                ivExpandCollapse.setRotation(0f); // Point down
                isPasswordSectionExpanded = false;
                Log.d("BoarderAccountSettings", "Section collapsed"); // Debug log
            } else {
                // Expand the section
                llPasswordFields.setVisibility(View.VISIBLE);
                ivExpandCollapse.setRotation(180f); // Point up
                isPasswordSectionExpanded = true;
                Log.d("BoarderAccountSettings", "Section expanded"); // Debug log
            }
        } catch (Exception e) {
            Log.d("BoarderAccountSettings", "Error in togglePasswordSection: " + e.getMessage()); // Debug log
            e.printStackTrace();
        }
    }
    
    private void toggleGcashSection() {
        try {
            Log.d("BoarderAccountSettings", "toggleGcashSection called, current state: " + isGcashSectionExpanded);
            
            if (llGcashFields == null) {
                Log.d("BoarderAccountSettings", "llGcashFields is null");
                return;
            }
            if (ivExpandCollapseGcash == null) {
                Log.d("BoarderAccountSettings", "ivExpandCollapseGcash is null");
                return;
            }
            
            if (isGcashSectionExpanded) {
                // Collapse the section
                llGcashFields.setVisibility(View.GONE);
                ivExpandCollapseGcash.setRotation(0f); // Point down
                isGcashSectionExpanded = false;
                Log.d("BoarderAccountSettings", "Gcash section collapsed");
            } else {
                // Expand the section
                llGcashFields.setVisibility(View.VISIBLE);
                ivExpandCollapseGcash.setRotation(180f); // Point up
                isGcashSectionExpanded = true;
                Log.d("BoarderAccountSettings", "Gcash section expanded");
            }
        } catch (Exception e) {
            Log.d("BoarderAccountSettings", "Error in toggleGcashSection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateGcashNumber() {
        try {
            // Validate Gcash number
            if (etGcashNumber == null) {
                Toast.makeText(getContext(), "Error: Gcash field not found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String gcashNumber = etGcashNumber.getText().toString().trim();
            
            // Basic validation
            if (TextUtils.isEmpty(gcashNumber)) {
                etGcashNumber.setError("GCash number is required");
                etGcashNumber.requestFocus();
                return;
            }
            
            // Validate phone number format (should be 11 digits for Philippines)
            if (!gcashNumber.matches("^09\\d{9}$")) {
                etGcashNumber.setError("Please enter a valid GCash number (09XXXXXXXXX)");
                etGcashNumber.requestFocus();
                return;
            }
            
            // Show loading
            setLoading(true);
            
            // Get user ID
            String userId = Login.getCurrentUserId(getContext());
            if (userId == null || userId.isEmpty()) {
                setLoading(false);
                Toast.makeText(getContext(), "User ID not found. Please login again.", Toast.LENGTH_LONG).show();
                return;
            }
            
            // TODO: Implement API call to update Gcash number
            // For now, simulate update
            new android.os.Handler().postDelayed(() -> {
                try {
                    setLoading(false);
                    Toast.makeText(getContext(), "GCash number updated successfully!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 1500);
            
        } catch (Exception e) {
            e.printStackTrace();
            setLoading(false);
            Toast.makeText(getContext(), "Error updating GCash number", Toast.LENGTH_SHORT).show();
        }
    }
}
