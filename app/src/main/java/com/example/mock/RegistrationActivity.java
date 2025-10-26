package com.example.mock;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class RegistrationActivity extends AppCompatActivity {

    Spinner spinnerRole;

    EditText etFirstName, etLastName, etMiddleName, etBirthDate, etPhone, etAddress, etEmail, etPassword, etGcashNum;
    TextView tvLogin, tvEmailValidation;
    ImageView UploadQr, ivTogglePassword;
    Button btnNext;
    boolean isPasswordVisible = false;

    private Uri selectedQrUri; // store the selected image URI
    private Runnable validationRunnable; // for real-time email validation

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

        etBirthDate = findViewById(R.id.etBirthDate);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etBirthDate = findViewById(R.id.etBirthDate);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etGcashNum = findViewById(R.id.etGcashNum);

        tvLogin = findViewById(R.id.tvLogin);
        tvEmailValidation = findViewById(R.id.tvEmailValidation);

        UploadQr = findViewById(R.id.UploadQr);

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

        // Set adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );
        spinnerRole.setAdapter(adapter);

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
                        },
                        year, month, day
                );

                // Optional: restrict future dates (no selecting birth date in future)
                datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

                datePickerDialog.show();
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

        //Button next for proceeding to the next Activity
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(etFirstName.getText().toString().isEmpty() || etLastName.getText().toString().isEmpty() ||
                        etBirthDate.getText().toString().isEmpty() || etPhone.getText().toString().isEmpty() ||
                        etAddress.getText().toString().isEmpty() || etEmail.getText().toString().isEmpty() ||
                        etPassword.getText().toString().isEmpty() || etGcashNum.getText().toString().isEmpty()) {

                    Toast.makeText(RegistrationActivity.this, "Please input all fields.", Toast.LENGTH_SHORT).show();

                } else if (UploadQr.getDrawable() == null) {
                    Toast.makeText(RegistrationActivity.this, "No image chosen for QR", Toast.LENGTH_SHORT).show();

                } else {
                    // Check if email is already validated
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
                            Toast.makeText(RegistrationActivity.this, "Please fix the email issue before proceeding.", Toast.LENGTH_LONG).show();
                        } else {
                            // Still validating, wait
                            Log.d("RegistrationActivity", "Email validation IN PROGRESS - waiting");
                            Toast.makeText(RegistrationActivity.this, "Please wait for email validation to complete.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // No validation yet, validate email first
                        Log.d("RegistrationActivity", "No validation yet - starting email validation");
                        validateEmailAndProceed();
                    }
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
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/validate_email_robust.php";
        
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
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/validate_email_robust.php";
        
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
            Toast.makeText(this, "Please wait for email validation to complete.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent a = new Intent(RegistrationActivity.this, Registration2Activity.class);

        a.putExtra("role", spinnerRole.getSelectedItem().toString());
        a.putExtra("firstName", etFirstName.getText().toString().trim());
        a.putExtra("middleName", etMiddleName.getText().toString().trim());
        a.putExtra("lastName", etLastName.getText().toString().trim());
        a.putExtra("birthDate", etBirthDate.getText().toString().trim());
        a.putExtra("phone", etPhone.getText().toString().trim());
        a.putExtra("address", etAddress.getText().toString().trim());
        a.putExtra("email", etEmail.getText().toString().trim());
        a.putExtra("password", etPassword.getText().toString().trim());
        a.putExtra("gcashNum", etGcashNum.getText().toString().trim());

        // if you want to send QR URI
        if (selectedQrUri != null) {
            a.putExtra("qrUri", selectedQrUri.toString());
        }

        startActivity(a);
    }

    /**
     * Verifies and sets QR image if approved
     */
    private void verifyAndSetQrImage(Uri imageUri) {
        // Show loading message
        Toast.makeText(this, "Verifying QR image...", Toast.LENGTH_SHORT).show();
        
        // Use QR code specific verification
        ImageVerification.verifyQrCode(this, imageUri, new ImageVerification.VerificationCallback() {
            @Override
            public void onVerificationComplete(boolean isApproved, String reason) {
                if (isApproved) {
                    // Set the image as selected
                    selectedQrUri = imageUri;
                    UploadQr.setImageURI(imageUri); // show the image in ImageView
                    Toast.makeText(RegistrationActivity.this, "✅ QR image approved", Toast.LENGTH_SHORT).show();
                } else {
                    // Show rejection reason
                    Toast.makeText(RegistrationActivity.this, "❌ Image rejected: " + reason, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onVerificationError(String error) {
                Toast.makeText(RegistrationActivity.this, "Image verification failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
