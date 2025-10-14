package com.example.mock;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

/**
 * BoarderAccountSettingsFragment - Account settings and profile management
 * Allows users to edit personal information and change password
 */
public class BoarderAccountSettingsFragment extends Fragment {

    // Views
    private ImageButton btnBack;
    private ProgressBar progressBar;

    // Personal Information Fields
    private TextInputEditText etFirstName;
    private TextInputEditText etMiddleName;
    private TextInputEditText etLastName;
    private TextInputEditText etEmail;
    private TextInputEditText etContactNumber;
    private TextInputEditText etBirthdate;
    private TextInputEditText etAddress;
    private MaterialButton btnSaveChanges;

    // Password Change Fields
    private TextInputEditText etCurrentPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnUpdatePassword;

    // SharedPreferences for storing user data
    private SharedPreferences userPrefs;
    private static final String PREFS_NAME = "boarder_user_prefs";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_MIDDLE_NAME = "middle_name";
    private static final String KEY_LAST_NAME = "last_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_CONTACT = "contact_number";
    private static final String KEY_BIRTHDATE = "birthdate";
    private static final String KEY_ADDRESS = "address";

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
            // Header views
            btnBack = view.findViewById(R.id.btnBack);
            progressBar = view.findViewById(R.id.progressBar);

            // Personal Information fields
            etFirstName = view.findViewById(R.id.etFirstName);
            etMiddleName = view.findViewById(R.id.etMiddleName);
            etLastName = view.findViewById(R.id.etLastName);
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

    private void setupClickListeners() {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserData() {
        try {
            // Load user data from SharedPreferences
            if (userPrefs != null) {
                String firstName = userPrefs.getString(KEY_FIRST_NAME, "John");
                String middleName = userPrefs.getString(KEY_MIDDLE_NAME, "Michael");
                String lastName = userPrefs.getString(KEY_LAST_NAME, "Doe");
                String email = userPrefs.getString(KEY_EMAIL, "john.doe@email.com");
                String contact = userPrefs.getString(KEY_CONTACT, "09123456789");
                String birthdate = userPrefs.getString(KEY_BIRTHDATE, "1995-01-01");
                String address = userPrefs.getString(KEY_ADDRESS, "Tagbilaran City");

                if (etFirstName != null) etFirstName.setText(firstName);
                if (etMiddleName != null) etMiddleName.setText(middleName);
                if (etLastName != null) etLastName.setText(lastName);
                if (etEmail != null) etEmail.setText(email);
                if (etContactNumber != null) etContactNumber.setText(contact);
                if (etBirthdate != null) etBirthdate.setText(birthdate);
                if (etAddress != null) etAddress.setText(address);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
