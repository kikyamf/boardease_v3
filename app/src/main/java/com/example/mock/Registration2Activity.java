package com.example.mock;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Registration2Activity extends AppCompatActivity {

    Spinner spinnerVId;
    ImageView ivUploadF, ivUploadB;
    EditText etIdNumber;
    CheckBox cbAgree;
    Button btnReg;
    Bitmap qrBitmap;
    Bitmap frontBitmap;
    Bitmap backBitmap;
    TextView tvLogin;
    private boolean isRegistering = false; // Flag to prevent multiple registrations


    // File paths for ID images
    private String idFrontPath = null;
    private String idBackPath = null;

    // Get data from first registration screen
    String role, firstName, middleName, lastName, birthDate, phone, address, email, password, gcashNum, qrPath;

    // Launchers for picking images
    private ActivityResultLauncher<String> pickFrontImageLauncher;
    private ActivityResultLauncher<String> pickBackImageLauncher;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration2);

        // Initialize Views
        spinnerVId = findViewById(R.id.spinnerVId);
        ivUploadF = findViewById(R.id.ivUploadF);
        ivUploadB = findViewById(R.id.ivUploadB);
        etIdNumber = findViewById(R.id.etidNumber);
        cbAgree = findViewById(R.id.cbAgree);
        btnReg = findViewById(R.id.btnReg);

        tvLogin = findViewById(R.id.tvLogin);

        // Retrieve data passed from RegistrationActivity
        role = getIntent().getStringExtra("role");
        firstName = getIntent().getStringExtra("firstName");
        middleName = getIntent().getStringExtra("middleName");
        lastName = getIntent().getStringExtra("lastName");
        birthDate = getIntent().getStringExtra("birthDate");
        phone = getIntent().getStringExtra("phone");
        address = getIntent().getStringExtra("address");
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");
        gcashNum = getIntent().getStringExtra("gcashNum");
        qrPath = getIntent().getStringExtra("qrUri");
        
        // Initialize QR bitmap from qrPath
        if (qrPath != null && !qrPath.isEmpty()) {
            try {
                Uri qrUri = Uri.parse(qrPath);
                ContentResolver resolver = getContentResolver();
                qrBitmap = BitmapFactory.decodeStream(resolver.openInputStream(qrUri));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading QR image", Toast.LENGTH_SHORT).show();
            }
        }

        // Prepare image pickers
        pickFrontImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        verifyAndSetFrontImage(uri);
                    }
                }
        );

        pickBackImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        verifyAndSetBackImage(uri);
                    }
                }
        );

        // Open gallery when clicking the ImageViews
        ivUploadF.setOnClickListener(v -> pickFrontImageLauncher.launch("image/*"));
        ivUploadB.setOnClickListener(v -> pickBackImageLauncher.launch("image/*"));

        // Spinner choices
        String[] roles = {
                "Select --",
                "Philippine Passport",
                "Driver's License",
                "PhilID (National ID)",
                "UMID",
                "SSS ID",
                "GSIS e-card",
                "PhilHealth ID",
                "TIN ID (BIR)",
                "Voter's ID",
                "Postal ID"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );
        spinnerVId.setAdapter(adapter);

        // Register button click
        btnReg.setOnClickListener(v -> {
            // Prevent multiple clicks
            if (isRegistering) {
                Toast.makeText(this, "Registration in progress, please wait...", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedIdType = spinnerVId.getSelectedItem().toString();
            String idNumber = etIdNumber.getText().toString().trim();
            boolean isAgreed = cbAgree.isChecked();

            if (selectedIdType.equals("Select --")) {
                Toast.makeText(this, "Please select a valid ID type", Toast.LENGTH_SHORT).show();
                return;
            }

            if (idNumber.isEmpty()) {
                Toast.makeText(this, "Please enter your ID number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isAgreed) {
                Toast.makeText(this, "You must agree to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            if (idFrontPath == null || idBackPath == null) {
                Toast.makeText(this, "Please upload front and back ID images", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if Bitmaps are null and initialize them if needed
            if (frontBitmap == null || backBitmap == null || qrBitmap == null) {
                String errorMsg = "Error loading images: ";
                if (frontBitmap == null) errorMsg += "Front image null. ";
                if (backBitmap == null) errorMsg += "Back image null. ";
                if (qrBitmap == null) errorMsg += "QR image null. ";
                Toast.makeText(this, errorMsg + "Please try uploading again.", Toast.LENGTH_LONG).show();
                return;
            }

            // Set registering flag and disable button
            isRegistering = true;
            btnReg.setEnabled(false);
            btnReg.setText("Registering...");

            // Debug info - remove toast message
            Log.d("Registration2", "Front: " + idFrontPath + ", Back: " + idBackPath);
            Log.d("Registration2", "BirthDate being sent: '" + birthDate + "'");

            String UPLOAD_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/insert_registration.php";

            // Inside btnReg.setOnClickListener
            VolleyMultipartRequest request = new VolleyMultipartRequest(Request.Method.POST, UPLOAD_URL,
                    response -> {
                        Log.d("Registration2", "=== REGISTRATION RESPONSE RECEIVED ===");
                        Log.d("Registration2", "Response data length: " + response.data.length);
                        Log.d("Registration2", "Response headers: " + response.headers);
                        
                        String responseString = new String(response.data);
                        Log.d("Registration2", "Raw server response: " + responseString);
                        Log.d("Registration2", "Response string length: " + responseString.length());
                        
                        try {
                            // Check if response starts with HTML (error)
                            if (responseString.trim().startsWith("<")) {
                                Log.e("Registration2", "ERROR: Server returned HTML instead of JSON");
                                Log.e("Registration2", "HTML response: " + responseString);
                                Toast.makeText(this, "Server error: Invalid response format", Toast.LENGTH_LONG).show();
                                return;
                            }
                            
                            Log.d("Registration2", "Attempting to parse JSON...");
                            JSONObject obj = new JSONObject(responseString);
                            String message = obj.getString("message");
                            boolean success = obj.getBoolean("success");
                            
                            Log.d("Registration2", "JSON parsing successful!");
                            Log.d("Registration2", "Success: " + success);
                            Log.d("Registration2", "Message: " + message);
                            Log.d("Registration2", "Has requires_verification: " + obj.has("requires_verification"));
                            
                            // Re-enable button
                            isRegistering = false;
                            btnReg.setEnabled(true);
                            btnReg.setText("REGISTER");
                            
                            if (success) {
                                Log.d("Registration2", "Registration successful");
                                
                                // Check if verification is required
                                boolean requiresVerification = obj.optBoolean("requires_verification", false);
                                Log.d("Registration2", "Requires verification: " + requiresVerification);
                                
                                if (requiresVerification) {
                                    // Navigate to email verification
                                    Log.d("Registration2", "Navigating to EmailVerificationActivity");
                                    Intent intent = new Intent(Registration2Activity.this, EmailVerificationActivity.class);
                                    intent.putExtra("email", email);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Navigate to login (old flow)
                                    Log.d("Registration2", "Navigating to Login");
                                    Intent intent = new Intent(Registration2Activity.this, Login.class);
                                    startActivity(intent);
                                    finish();
                                }
                                
                                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                            } else {
                                Log.d("Registration2", "Registration failed: " + message);
                                // Handle specific error messages
                                if (message.contains("Duplicate entry") && message.contains("email")) {
                                    Toast.makeText(this, "This email is already registered. Please use a different email or try logging in.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("Registration2", "=== JSON PARSING ERROR ===");
                            Log.e("Registration2", "JSONException: " + e.getMessage());
                            Log.e("Registration2", "Raw response that failed to parse: " + responseString);
                            Log.e("Registration2", "Response length: " + responseString.length());
                            Log.e("Registration2", "First 200 chars: " + responseString.substring(0, Math.min(200, responseString.length())));
                            
                            // Re-enable button on error
                            isRegistering = false;
                            btnReg.setEnabled(true);
                            btnReg.setText("REGISTER");
                            Toast.makeText(this, "Server response error. Please try again.", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e("Registration2", "=== UNEXPECTED ERROR ===");
                            Log.e("Registration2", "Exception: " + e.getMessage());
                            Log.e("Registration2", "Exception type: " + e.getClass().getSimpleName());
                            e.printStackTrace();
                            
                            // Re-enable button on error
                            isRegistering = false;
                            btnReg.setEnabled(true);
                            btnReg.setText("REGISTER");
                            Toast.makeText(this, "Unexpected error occurred. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    },
                    error -> {
                        Log.e("Registration2", "=== NETWORK ERROR ===");
                        Log.e("Registration2", "VolleyError: " + error.getMessage());
                        Log.e("Registration2", "Error type: " + error.getClass().getSimpleName());
                        Log.e("Registration2", "Network response: " + (error.networkResponse != null ? error.networkResponse.toString() : "null"));
                        if (error.networkResponse != null) {
                            Log.e("Registration2", "Response code: " + error.networkResponse.statusCode);
                            Log.e("Registration2", "Response data: " + new String(error.networkResponse.data));
                        }
                        
                        // Re-enable button on error
                        isRegistering = false;
                        btnReg.setEnabled(true);
                        btnReg.setText("REGISTER");
                        String errorMessage = error.getMessage();
                        if (errorMessage == null || errorMessage.isEmpty()) {
                            errorMessage = "Network error. Please check your connection.";
                        }
                        Toast.makeText(this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("role", role);
                    params.put("firstName", firstName);
                    params.put("middleName", middleName);
                    params.put("lastName", lastName);
                    params.put("birthDate", birthDate);
                    params.put("phone", phone);
                    params.put("address", address);
                    params.put("email", email);
                    params.put("password", password);
                    params.put("gcashNum", gcashNum);
                    params.put("idType", selectedIdType);
                    params.put("idNumber", idNumber);
                    params.put("isAgreed", String.valueOf(isAgreed));
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    params.put("qrFile", new DataPart("qr.jpg", AppHelper.getFileDataFromDrawable(getBaseContext(), qrBitmap)));
                    params.put("idFrontFile", new DataPart("front.jpg", AppHelper.getFileDataFromDrawable(getBaseContext(), frontBitmap)));
                    params.put("idBackFile", new DataPart("back.jpg", AppHelper.getFileDataFromDrawable(getBaseContext(), backBitmap)));
                    return params;
                }
            };
            // Configure request with longer timeout
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    30000, // 30 seconds timeout
                    3, // 3 retries
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            Volley.newRequestQueue(this).add(request);

        });

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
        });

        // Insets handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Verifies and sets front ID image if approved
     */
    private void verifyAndSetFrontImage(Uri imageUri) {
        // Show loading message
        Toast.makeText(this, "Verifying front ID document...", Toast.LENGTH_SHORT).show();
        
        // Use ID document specific verification
        ImageVerification.verifyIdDocument(this, imageUri, new ImageVerification.VerificationCallback() {
            @Override
            public void onVerificationComplete(boolean isApproved, String reason) {
                if (isApproved) {
                    // Set the image as selected
                    ivUploadF.setImageURI(imageUri); // show image
                    idFrontPath = imageUri.toString(); // save URI path
                    
                    // Convert URI to Bitmap
                    try {
                        Log.d("Registration2", "Front URI: " + imageUri.toString());
                        ContentResolver resolver = getContentResolver();
                        frontBitmap = BitmapFactory.decodeStream(resolver.openInputStream(imageUri));
                        if (frontBitmap == null) {
                            Log.e("Registration2", "Failed to decode front image from URI");
                            Toast.makeText(Registration2Activity.this, "Failed to load front image", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("Registration2", "Front image loaded successfully, size: " + frontBitmap.getWidth() + "x" + frontBitmap.getHeight());
                            Toast.makeText(Registration2Activity.this, "✅ Front ID document verified", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("Registration2", "Error loading front image", e);
                        Toast.makeText(Registration2Activity.this, "Error loading front image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Show rejection reason
                    Toast.makeText(Registration2Activity.this, "❌ Front ID document rejected: " + reason, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onVerificationError(String error) {
                Toast.makeText(Registration2Activity.this, "Front ID document verification failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Verifies and sets back ID image if approved
     */
    private void verifyAndSetBackImage(Uri imageUri) {
        // Show loading message
        Toast.makeText(this, "Verifying back ID document...", Toast.LENGTH_SHORT).show();
        
        // Use ID document specific verification
        ImageVerification.verifyIdDocument(this, imageUri, new ImageVerification.VerificationCallback() {
            @Override
            public void onVerificationComplete(boolean isApproved, String reason) {
                if (isApproved) {
                    // Set the image as selected
                    ivUploadB.setImageURI(imageUri); // show image
                    idBackPath = imageUri.toString(); // save URI path
                    
                    // Convert URI to Bitmap
                    try {
                        Log.d("Registration2", "Back URI: " + imageUri.toString());
                        ContentResolver resolver = getContentResolver();
                        backBitmap = BitmapFactory.decodeStream(resolver.openInputStream(imageUri));
                        if (backBitmap == null) {
                            Log.e("Registration2", "Failed to decode back image from URI");
                            Toast.makeText(Registration2Activity.this, "Failed to load back image", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("Registration2", "Back image loaded successfully, size: " + backBitmap.getWidth() + "x" + backBitmap.getHeight());
                            Toast.makeText(Registration2Activity.this, "✅ Back ID document verified", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("Registration2", "Error loading back image", e);
                        Toast.makeText(Registration2Activity.this, "Error loading back image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Show rejection reason
                    Toast.makeText(Registration2Activity.this, "❌ Back ID document rejected: " + reason, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onVerificationError(String error) {
                Toast.makeText(Registration2Activity.this, "Back ID document verification failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
