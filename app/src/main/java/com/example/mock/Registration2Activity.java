package com.example.mock;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // Business Permit Views
    private View businessPermitSection;
    private ViewGroup businessPermitContainer;
    private Button btnAddPermit;

    // File paths for ID images
    private String idFrontPath = null;
    private String idBackPath = null;

    // Get data from first registration screen
    String role, firstName, middleName, lastName, suffix, birthDate, phone, address, email, password, gcashNum, qrPath;

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

    // Launchers for picking images
    private ActivityResultLauncher<String> pickFrontImageLauncher;
    private ActivityResultLauncher<String> pickBackImageLauncher;
    private List<ActivityResultLauncher<String>> permitImageLaunchers = new ArrayList<>();
    private PermitUploadItem currentPermitItemForLauncher = null; // Track which permit item is being edited

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
        
        // Initialize Business Permit Views
        businessPermitSection = findViewById(R.id.businessPermitSection);
        businessPermitContainer = findViewById(R.id.businessPermitContainer);
        btnAddPermit = findViewById(R.id.btnAddPermit);

        // Retrieve data passed from RegistrationActivity
        role = getIntent().getStringExtra("role");
        firstName = getIntent().getStringExtra("firstName");
        middleName = getIntent().getStringExtra("middleName");
        lastName = getIntent().getStringExtra("lastName");
        suffix = getIntent().getStringExtra("suffix");
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
        
        // Setup Business Permit Section (only for BH Owner)
        boolean isBHOwner = role != null && !role.equals("Boarder");
        if (isBHOwner) {
            businessPermitSection.setVisibility(View.VISIBLE);
            
            // Pre-register all permit image launchers (must be done during onCreate)
            for (int i = 0; i < MAX_PERMITS; i++) {
                ActivityResultLauncher<String> permitLauncher = registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {
                            if (uri != null && currentPermitItemForLauncher != null) {
                                handlePermitImageSelection(currentPermitItemForLauncher, uri);
                                currentPermitItemForLauncher = null; // Reset after handling
                            }
                        }
                );
                permitImageLaunchers.add(permitLauncher);
            }
            
            // Create first permit upload item
            createPermitUploadItem();
            // Setup add permit button
            btnAddPermit.setOnClickListener(v -> {
                if (permitUploadItems.size() < MAX_PERMITS) {
                    createPermitUploadItem();
                    updateAddPermitButtonVisibility();
                } else {
                    Toast.makeText(this, "Maximum of " + MAX_PERMITS + " business permits allowed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            businessPermitSection.setVisibility(View.GONE);
        }

        // Prepare image pickers
        pickFrontImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        // Validate ID type selection before verifying
                        String selectedIdType = spinnerVId.getSelectedItem().toString();
                        if (selectedIdType == null || selectedIdType.equals("Select --")) {
                            Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
                            spinnerVId.requestFocus();
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
                        // Validate ID type selection before verifying
                        String selectedIdType = spinnerVId.getSelectedItem().toString();
                        if (selectedIdType == null || selectedIdType.equals("Select --")) {
                            Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
                            spinnerVId.requestFocus();
                            return;
                        }
                        verifyAndSetBackImage(uri);
                    }
                }
        );

        // Open ID capture when clicking the ImageViews
        ivUploadF.setOnClickListener(v -> {
            // Validate ID type selection first
            String selectedIdType = spinnerVId.getSelectedItem().toString();
            if (selectedIdType == null || selectedIdType.equals("Select --")) {
                Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
                spinnerVId.requestFocus();
                return;
            }
            
            Log.d("ID_CAPTURE", "Starting front ID capture");
            Intent intent = new Intent(this, IdCaptureActivity.class);
            intent.putExtra("id_type", "front");
            startActivityForResult(intent, 1001);
        });
        
        ivUploadB.setOnClickListener(v -> {
            // Validate ID type selection first
            String selectedIdType = spinnerVId.getSelectedItem().toString();
            if (selectedIdType == null || selectedIdType.equals("Select --")) {
                Toast.makeText(this, "Please select a valid ID type first", Toast.LENGTH_SHORT).show();
                spinnerVId.requestFocus();
                return;
            }
            
            Log.d("ID_CAPTURE", "Starting back ID capture");
            Intent intent = new Intent(this, IdCaptureActivity.class);
            intent.putExtra("id_type", "back");
            startActivityForResult(intent, 1002);
        });

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
        spinnerVId.setAdapter(adapter);

        // Register button click
        btnReg.setOnClickListener(v -> {
            Log.d("REGISTRATION", "=== REGISTER BUTTON CLICKED ===");
            
            // Prevent multiple clicks
            if (isRegistering) {
                Log.d("REGISTRATION", "Registration already in progress, blocking duplicate click");
                Toast.makeText(this, "Registration in progress, please wait...", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedIdType = spinnerVId.getSelectedItem().toString();
            String idNumber = etIdNumber.getText().toString().trim();
            boolean isAgreed = cbAgree.isChecked();

            Log.d("REGISTRATION", "Form validation - ID Type: " + selectedIdType + ", ID Number: " + idNumber + ", Agreed: " + isAgreed);

            if (selectedIdType.equals("Select --")) {
                Log.d("REGISTRATION", "❌ Validation failed: No ID type selected");
                Toast.makeText(this, "Please select a valid ID type", Toast.LENGTH_SHORT).show();
                return;
            }

            if (idNumber.isEmpty()) {
                Log.d("REGISTRATION", "❌ Validation failed: No ID number entered");
                Toast.makeText(this, "Please enter your ID number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isAgreed) {
                Log.d("REGISTRATION", "❌ Validation failed: Terms not agreed");
                Toast.makeText(this, "You must agree to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            if (idFrontPath == null || idBackPath == null) {
                Log.d("REGISTRATION", "❌ Validation failed: Missing ID images - Front: " + (idFrontPath != null) + ", Back: " + (idBackPath != null));
                Toast.makeText(this, "Please upload front and back ID images", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check role - QR code is only required for BH Owner
            boolean isBoarder = "Boarder".equals(role);
            
            // Check if Bitmaps are null and initialize them if needed
            Log.d("REGISTRATION", "Checking bitmap status - Front: " + (frontBitmap != null) + ", Back: " + (backBitmap != null) + ", QR: " + (qrBitmap != null) + ", Role: " + role + ", isBoarder: " + isBoarder);
            
            if (frontBitmap == null || backBitmap == null) {
                String errorMsg = "Error loading images: ";
                if (frontBitmap == null) errorMsg += "Front image null. ";
                if (backBitmap == null) errorMsg += "Back image null. ";
                Log.d("REGISTRATION", "❌ Bitmap validation failed: " + errorMsg);
                Toast.makeText(this, errorMsg + "Please try uploading again.", Toast.LENGTH_LONG).show();
                return;
            }
            
            // QR code is only required for BH Owner
            if (!isBoarder && qrBitmap == null) {
                Log.d("REGISTRATION", "❌ Bitmap validation failed: QR image null (required for BH Owner)");
                Toast.makeText(this, "GCash QR code is required for BH Owner. Please upload your GCash QR code.", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Business permits are required for BH Owner (at least one)
            if (!isBoarder) {
                boolean hasPermits = false;
                for (PermitUploadItem item : permitUploadItems) {
                    if (item.bitmap != null) {
                        hasPermits = true;
                        break;
                    }
                }
                if (!hasPermits) {
                    Log.d("REGISTRATION", "❌ Validation failed: No business permits uploaded (required for BH Owner)");
                    Toast.makeText(this, "At least one business permit is required for BH Owner. Please upload your business permit(s).", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            
            Log.d("REGISTRATION", "✅ All required bitmaps loaded successfully");
            
            // Log permit status before sending
            if (!isBoarder) {
                int permitCount = 0;
                for (PermitUploadItem item : permitUploadItems) {
                    if (item.bitmap != null) {
                        permitCount++;
                        Log.d("REGISTRATION", "Permit " + permitCount + " ready - bitmap size: " + (item.bitmap.getWidth() + "x" + item.bitmap.getHeight()));
                    }
                }
                Log.d("REGISTRATION", "Total permits ready to send: " + permitCount);
            }

            // Set registering flag and disable button
            isRegistering = true;
            btnReg.setEnabled(false);
            btnReg.setText("Registering...");
            Log.d("REGISTRATION", "Registration process started - button disabled");

            // Debug info - remove toast message
            Log.d("REGISTRATION", "File paths - Front: " + idFrontPath + ", Back: " + idBackPath);
            Log.d("REGISTRATION", "BirthDate being sent: '" + birthDate + "'");

            String UPLOAD_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/insert_registration.php";
            Log.d("REGISTRATION", "Upload URL: " + UPLOAD_URL);
            Log.d("REGISTRATION", "Creating VolleyMultipartRequest...");

            // Inside btnReg.setOnClickListener
            VolleyMultipartRequest request = new VolleyMultipartRequest(Request.Method.POST, UPLOAD_URL,
                    response -> {
                        Log.d("REGISTRATION", "=== REGISTRATION RESPONSE RECEIVED ===");
                        Log.d("REGISTRATION", "Response data length: " + response.data.length);
                        Log.d("REGISTRATION", "Response headers: " + response.headers);
                        
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
                            
                            // Business permits are now saved separately, so we don't check permits_received here
                            
                            // Re-enable button
                            isRegistering = false;
                            btnReg.setEnabled(true);
                            btnReg.setText("REGISTER");
                            
                            if (success) {
                                Log.d("Registration2", "Registration successful");
                                
                                // Get registration ID from response
                                int regId = obj.optInt("reg_id", 0);
                                Log.d("Registration2", "Registration ID: " + regId);
                                
                                // Check if user is BH Owner and has business permits to save
                                boolean isBHOwner = role != null && !role.equals("Boarder");
                                boolean hasPermits = false;
                                for (PermitUploadItem item : permitUploadItems) {
                                    if (item.bitmap != null) {
                                        hasPermits = true;
                                        break;
                                    }
                                }
                                
                                if (isBHOwner && hasPermits && regId > 0) {
                                    // Save business permits separately using direct IP
                                    Log.d("Registration2", "Saving business permits separately for reg_id: " + regId);
                                    saveBusinessPermits(regId, () -> {
                                        // After permits are saved, proceed with verification flow
                                        proceedToVerification(obj, message);
                                    });
                                } else {
                                    // No permits to save, proceed directly to verification
                                    proceedToVerification(obj, message);
                                }
                            } else {
                                Log.d("Registration2", "Registration failed: " + message);
                                // Handle specific error messages
                                if (message.contains("Duplicate entry") && message.contains("email")) {
                                    Toast.makeText(this, "This email is already registered. Please use a different email or try logging in.", Toast.LENGTH_LONG).show();
                                } else if (message.contains("GCash QR code validation failed")) {
                                    // Handle QR code validation errors
                                    String qrErrorMessage = message.replace("GCash QR code validation failed: ", "");
                                    String helpfulMessage = qrErrorMessage;
                                    
                                    if (qrErrorMessage.contains("No QR code detected")) {
                                        helpfulMessage = "No QR code found in your image. Please upload a clear photo of your GCash QR code.";
                                    } else if (qrErrorMessage.contains("does not appear to be a GCash QR code")) {
                                        helpfulMessage = "This doesn't look like a GCash QR code. Please upload your actual GCash QR code.";
                                    } else if (qrErrorMessage.contains("too small")) {
                                        helpfulMessage = "QR code image is too small. Please take a clearer photo.";
                                    } else if (qrErrorMessage.contains("too large")) {
                                        helpfulMessage = "QR code image is too large. Please compress or resize the image.";
                                    } else if (qrErrorMessage.contains("Invalid image format")) {
                                        helpfulMessage = "Invalid image format. Please upload a JPG, PNG, or GIF image.";
                                    }
                                    
                                    Toast.makeText(this, "❌ " + helpfulMessage, Toast.LENGTH_LONG).show();
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
                        
                        String detailedErrorMessage = "Network error. Please check your connection.";
                        
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            String responseData = new String(error.networkResponse.data);
                            Log.e("Registration2", "Response code: " + statusCode);
                            Log.e("Registration2", "Response data: " + responseData);
                            
                            // Provide specific error messages based on status code
                            switch (statusCode) {
                                case 404:
                                    detailedErrorMessage = "Server not found (404). Please check if the server is running.";
                                    break;
                                case 500:
                                    detailedErrorMessage = "Server error (500). Please try again later.";
                                    break;
                                case 403:
                                    detailedErrorMessage = "Access forbidden (403). Please check server permissions.";
                                    break;
                                case 400:
                                    detailedErrorMessage = "Bad request (400). Please check your data.";
                                    break;
                                case 0:
                                    detailedErrorMessage = "No response from server. Check your internet connection.";
                                    break;
                                default:
                                    detailedErrorMessage = "Server error (" + statusCode + "). Response: " + responseData;
                                    break;
                            }
                        } else {
                            // No network response - likely connection issues
                            String errorMsg = error.getMessage();
                            if (errorMsg != null) {
                                if (errorMsg.contains("UnknownHostException")) {
                                    detailedErrorMessage = "Cannot reach server. Check your internet connection.";
                                } else if (errorMsg.contains("ConnectException")) {
                                    detailedErrorMessage = "Connection failed. Server may be down.";
                                } else if (errorMsg.contains("SocketTimeoutException")) {
                                    detailedErrorMessage = "Request timed out. Server is slow or unreachable.";
                                } else if (errorMsg.contains("NoConnectionError")) {
                                    detailedErrorMessage = "No internet connection. Please check your network.";
                                } else {
                                    detailedErrorMessage = "Connection error: " + errorMsg;
                                }
                            } else {
                                detailedErrorMessage = "Unknown network error. Please check your connection.";
                            }
                        }
                        
                        Log.e("Registration2", "Final error message: " + detailedErrorMessage);
                        
                        // Re-enable button on error
                        isRegistering = false;
                        btnReg.setEnabled(true);
                        btnReg.setText("REGISTER");
                        Toast.makeText(this, "Registration failed: " + detailedErrorMessage, Toast.LENGTH_LONG).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = super.getHeaders();
                    if (headers == null) {
                        headers = new HashMap<>();
                    }
                    // Add ngrok skip browser warning header
                    headers.put("ngrok-skip-browser-warning", "true");
                    return headers;
                }
                
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    Log.d("REGISTRATION", "=== BUILDING PARAMETERS ===");
                    Log.d("REGISTRATION", "Role: " + role);
                    Log.d("REGISTRATION", "First Name: " + firstName);
                    Log.d("REGISTRATION", "Last Name: " + lastName);
                    Log.d("REGISTRATION", "Email: " + email);
                    Log.d("REGISTRATION", "Password: " + password);
                    Log.d("REGISTRATION", "Birth Date: " + birthDate);
                    Log.d("REGISTRATION", "ID Type: " + selectedIdType);
                    Log.d("REGISTRATION", "ID Number: " + idNumber);
                    
                    // Check role - GCash is only required for BH Owner
                    boolean isBoarder = "Boarder".equals(role);
                    
                    params.put("role", role);
                    params.put("firstName", firstName);
                    params.put("middleName", middleName);
                    params.put("lastName", lastName);
                    params.put("suffix", suffix != null ? suffix : "None");
                    params.put("birthDate", birthDate);
                    params.put("phone", phone);
                    params.put("address", address);
                    params.put("email", email);
                    params.put("password", password);
                    
                    // Only include GCash number if not Boarder (BH Owner required, Boarder optional)
                    if (!isBoarder) {
                        params.put("gcashNum", gcashNum != null ? gcashNum : "");
                    } else {
                        params.put("gcashNum", ""); // Empty for Boarder
                    }
                    
                    params.put("idType", selectedIdType);
                    params.put("idNumber", idNumber);
                    params.put("isAgreed", String.valueOf(isAgreed));
                    
                    Log.d("REGISTRATION", "=== FINAL PARAMETERS ===");
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        Log.d("REGISTRATION", entry.getKey() + " = " + entry.getValue());
                    }
                    Log.d("REGISTRATION", "Total parameters: " + params.size());
                    
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    Log.d("REGISTRATION", "=== BUILDING FILE DATA ===");
                    Log.d("REGISTRATION", "Role: " + role + ", isBoarder: " + "Boarder".equals(role));
                    
                    try {
                        byte[] frontData = AppHelper.getFileDataFromDrawable(getBaseContext(), frontBitmap);
                        byte[] backData = AppHelper.getFileDataFromDrawable(getBaseContext(), backBitmap);
                        
                        Log.d("REGISTRATION", "Front file data size: " + (frontData != null ? frontData.length : "null"));
                        Log.d("REGISTRATION", "Back file data size: " + (backData != null ? backData.length : "null"));
                        
                        // Only include QR code for BH Owner
                        boolean isBoarder = "Boarder".equals(role);
                        if (!isBoarder && qrBitmap != null) {
                            byte[] qrData = AppHelper.getFileDataFromDrawable(getBaseContext(), qrBitmap);
                            Log.d("REGISTRATION", "QR file data size: " + (qrData != null ? qrData.length : "null"));
                            params.put("qrFile", new DataPart("qr.jpg", qrData));
                            Log.d("REGISTRATION", "QR file data added (BH Owner)");
                        } else {
                            Log.d("REGISTRATION", "QR file data skipped - Role: " + role + ", qrBitmap: " + (qrBitmap != null));
                        }
                        
                        params.put("idFrontFile", new DataPart("front.jpg", frontData));
                        params.put("idBackFile", new DataPart("back.jpg", backData));
                        
                        // Business permits are now saved separately after registration
                        // Removed from initial registration request to avoid conflicts
                        Log.d("REGISTRATION", "Business permits will be saved separately after registration");
                        
                        Log.d("REGISTRATION", "File data added successfully. Total files in request: " + params.size());
                        // Log all file keys for debugging
                        for (String key : params.keySet()) {
                            DataPart dataPart = params.get(key);
                            Log.d("REGISTRATION", "File in request: " + key + " -> " + (dataPart != null ? dataPart.getFileName() + " (" + dataPart.getContent().length + " bytes)" : "null"));
                        }
                    } catch (Exception e) {
                        Log.e("REGISTRATION", "Error creating file data: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    return params;
                }
            };
            // Configure request with longer timeout
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    30000, // 30 seconds timeout
                    3, // 3 retries
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            Log.d("REGISTRATION", "Request timeout set to 30 seconds with 3 retries");
            Log.d("REGISTRATION", "Adding request to Volley queue...");
            
            Volley.newRequestQueue(this).add(request);
            
            Log.d("REGISTRATION", "Request added to queue successfully");

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
                    ivUploadF.setScaleType(ImageView.ScaleType.CENTER_CROP); // Ensure proper display
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
                    ivUploadB.setScaleType(ImageView.ScaleType.CENTER_CROP); // Ensure proper display
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            String imagePath = data.getStringExtra("image_path");
            String idNumber = data.getStringExtra("id_number");
            String idType = data.getStringExtra("id_type");
            
            Log.d("ID_CAPTURE", "Received result - Type: " + idType + ", Path: " + imagePath + ", ID Number: " + idNumber);
            
            if (requestCode == 1001) { // Front ID
                handleFrontIdResult(imagePath, idNumber);
            } else if (requestCode == 1002) { // Back ID
                handleBackIdResult(imagePath, idNumber);
            }
        } else {
            Log.d("ID_CAPTURE", "ID capture cancelled or failed");
            Toast.makeText(this, "ID capture cancelled", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void handleFrontIdResult(String imagePath, String idNumber) {
        try {
            Log.d("ID_CAPTURE", "=== HANDLING FRONT ID RESULT ===");
            Log.d("ID_CAPTURE", "Image path: " + imagePath);
            Log.d("ID_CAPTURE", "ID number received: '" + idNumber + "'");
            Log.d("ID_CAPTURE", "ID number is null: " + (idNumber == null));
            Log.d("ID_CAPTURE", "ID number is empty: " + (idNumber != null && idNumber.isEmpty()));
            
            // Load the captured image
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                frontBitmap = bitmap;
                idFrontPath = imagePath;
                
                // Update UI
                ivUploadF.setImageBitmap(bitmap);
                ivUploadF.setScaleType(ImageView.ScaleType.CENTER_CROP); // Ensure proper display
                
                // Auto-fill ID number if extracted
                if (idNumber != null && !idNumber.isEmpty()) {
                    etIdNumber.setText(idNumber);
                    Log.d("ID_CAPTURE", "✅ Auto-filled ID number: " + idNumber);
                    Toast.makeText(this, "✅ Front ID captured! ID number: " + idNumber, Toast.LENGTH_LONG).show();
                } else {
                    Log.d("ID_CAPTURE", "⚠️ No ID number extracted, user needs to enter manually");
                    Toast.makeText(this, "✅ Front ID captured! Please enter ID number manually", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e("ID_CAPTURE", "❌ Failed to load front ID image");
                Toast.makeText(this, "Failed to load front ID image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("ID_CAPTURE", "❌ Error handling front ID result: " + e.getMessage());
            Toast.makeText(this, "Error processing front ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void handleBackIdResult(String imagePath, String idNumber) {
        try {
            // Load the captured image
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                backBitmap = bitmap;
                idBackPath = imagePath;
                
                // Update UI
                ivUploadB.setImageBitmap(bitmap);
                ivUploadB.setScaleType(ImageView.ScaleType.CENTER_CROP); // Ensure proper display
                
                // Show success message
                if (idNumber != null && !idNumber.isEmpty()) {
                    Log.d("ID_CAPTURE", "Back ID captured with ID number: " + idNumber);
                    Toast.makeText(this, "✅ Back ID captured! ID number: " + idNumber, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "✅ Back ID captured!", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Failed to load back ID image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("ID_CAPTURE", "Error handling back ID result: " + e.getMessage());
            Toast.makeText(this, "Error processing back ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        
        // Create the layout for the permit upload item
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        // Label
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
        
        // ImageView for permit
        ImageView permitImageView = new ImageView(this);
        permitImageView.setId(View.generateViewId());
        // Convert 200dp to pixels
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
        
        // Remove button (only show if more than one permit)
        Button removeButton = new Button(this);
        removeButton.setText("Remove");
        removeButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        removeButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
        removeButton.setTextColor(getResources().getColor(android.R.color.white));
        removeButton.setVisibility(View.GONE); // Initially hidden, shown when multiple permits exist
        itemLayout.addView(removeButton);
        
        // Add margin between items
        if (permitUploadItems.size() > 0) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) itemLayout.getLayoutParams();
            params.topMargin = 16;
            itemLayout.setLayoutParams(params);
        }
        
        // Store references
        item.itemView = itemLayout;
        item.imageView = permitImageView;
        item.removeButton = removeButton;
        
        // Get the launcher for this permit item (use index from list size)
        int launcherIndex = permitUploadItems.size();
        if (launcherIndex < permitImageLaunchers.size()) {
            ActivityResultLauncher<String> permitLauncher = permitImageLaunchers.get(launcherIndex);
            
            // Setup click listener for image view
            permitImageView.setOnClickListener(v -> {
                currentPermitItemForLauncher = item; // Set which item is being edited
                permitLauncher.launch("image/*");
            });
        } else {
            Log.e("PERMIT_UPLOAD", "No launcher available for permit item " + launcherIndex);
            Toast.makeText(this, "Error: Cannot add more permits", Toast.LENGTH_SHORT).show();
        }
        
        // Setup remove button
        removeButton.setOnClickListener(v -> {
            removePermitUploadItem(item);
        });
        
        // Add to container and list
        businessPermitContainer.addView(itemLayout);
        permitUploadItems.add(item);
        
        // Update add button visibility
        updateAddPermitButtonVisibility();
        
        Log.d("PERMIT_UPLOAD", "Created permit upload item " + (permitUploadItems.size()));
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
                
                // Show remove buttons for all items if there are multiple permits
                if (permitUploadItems.size() > 1) {
                    for (PermitUploadItem permitItem : permitUploadItems) {
                        permitItem.removeButton.setVisibility(View.VISIBLE);
                    }
                }
                
                // Show add button if not at max
                updateAddPermitButtonVisibility();
                
                Toast.makeText(this, "Business permit uploaded successfully", Toast.LENGTH_SHORT).show();
                Log.d("PERMIT_UPLOAD", "Permit image loaded successfully for item " + item.index);
            } else {
                Toast.makeText(this, "Failed to load permit image", Toast.LENGTH_SHORT).show();
                Log.e("PERMIT_UPLOAD", "Failed to decode permit image");
            }
        } catch (Exception e) {
            Log.e("PERMIT_UPLOAD", "Error loading permit image: " + e.getMessage());
            Toast.makeText(this, "Error loading permit image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Removes a permit upload item
     */
    private void removePermitUploadItem(PermitUploadItem item) {
        try {
            // Find the index of the item to remove
            int itemIndex = permitUploadItems.indexOf(item);
            if (itemIndex == -1) {
                Log.e("PERMIT_UPLOAD", "Item not found in list");
                return;
            }
            
            // Remove from container
            businessPermitContainer.removeView(item.itemView);
            
            // Remove from list
            permitUploadItems.remove(item);
            
            // Update labels and reassign launchers for remaining items
            for (int i = 0; i < permitUploadItems.size(); i++) {
                PermitUploadItem permitItem = permitUploadItems.get(i);
                ViewGroup itemLayout = (ViewGroup) permitItem.itemView;
                TextView label = (TextView) itemLayout.getChildAt(0);
                label.setText("Business Permit " + (i + 1));
                
                // Reassign launcher to the correct index
                ImageView imageView = permitItem.imageView;
                if (i < permitImageLaunchers.size()) {
                    ActivityResultLauncher<String> permitLauncher = permitImageLaunchers.get(i);
                    imageView.setOnClickListener(v -> {
                        currentPermitItemForLauncher = permitItem;
                        permitLauncher.launch("image/*");
                    });
                }
            }
            
            // Update remove button visibility
            if (permitUploadItems.size() <= 1) {
                for (PermitUploadItem permitItem : permitUploadItems) {
                    permitItem.removeButton.setVisibility(View.GONE);
                }
            }
            
            // Update add button visibility
            updateAddPermitButtonVisibility();
            
            Log.d("PERMIT_UPLOAD", "Removed permit upload item " + item.index + ", remaining: " + permitUploadItems.size());
        } catch (Exception e) {
            Log.e("PERMIT_UPLOAD", "Error removing permit item: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error removing permit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void proceedToVerification(JSONObject obj, String message) {
        try {
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
        } catch (Exception e) {
            Log.e("Registration2", "Error in proceedToVerification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveBusinessPermits(int regId, Runnable onComplete) {
        Log.d("REGISTRATION", "=== SAVING BUSINESS PERMITS SEPARATELY ===");
        Log.d("REGISTRATION", "Registration ID: " + regId);
        
        // Use direct IP for saving business permits
        String PERMITS_URL = "http://192.168.1.6/BoardEase2/save_business_permits.php";
        Log.d("REGISTRATION", "Permits URL: " + PERMITS_URL);
        
        VolleyMultipartRequest request = new VolleyMultipartRequest(Request.Method.POST, PERMITS_URL,
                response -> {
                    Log.d("REGISTRATION", "=== BUSINESS PERMITS RESPONSE RECEIVED ===");
                    String responseString = new String(response.data);
                    Log.d("REGISTRATION", "Permits response: " + responseString);
                    
                    try {
                        if (responseString.trim().startsWith("<")) {
                            Log.e("REGISTRATION", "ERROR: Server returned HTML instead of JSON for permits");
                            Toast.makeText(Registration2Activity.this, "Warning: Business permits may not have been saved. Please contact support.", Toast.LENGTH_LONG).show();
                            if (onComplete != null) onComplete.run();
                            return;
                        }
                        
                        JSONObject obj = new JSONObject(responseString);
                        boolean success = obj.getBoolean("success");
                        String message = obj.optString("message", "");
                        int permitsInserted = obj.optInt("permits_inserted", 0);
                        
                        if (success) {
                            Log.d("REGISTRATION", "Business permits saved successfully: " + permitsInserted);
                            Toast.makeText(Registration2Activity.this, "✅ " + permitsInserted + " business permit(s) saved successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("REGISTRATION", "Failed to save business permits: " + message);
                            Toast.makeText(Registration2Activity.this, "⚠️ Business permits could not be saved: " + message, Toast.LENGTH_LONG).show();
                        }
                        
                        if (onComplete != null) onComplete.run();
                    } catch (JSONException e) {
                        Log.e("REGISTRATION", "JSON parsing error for permits: " + e.getMessage());
                        Toast.makeText(Registration2Activity.this, "Warning: Business permits response error. Please contact support.", Toast.LENGTH_LONG).show();
                        if (onComplete != null) onComplete.run();
                    }
                },
                error -> {
                    Log.e("REGISTRATION", "Network error saving business permits: " + error.getMessage());
                    Toast.makeText(Registration2Activity.this, "⚠️ Could not save business permits. Please contact support.", Toast.LENGTH_LONG).show();
                    if (onComplete != null) onComplete.run();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("reg_id", String.valueOf(regId));
                Log.d("REGISTRATION", "Permits params - reg_id: " + regId);
                return params;
            }
            
            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                
                if (!permitUploadItems.isEmpty()) {
                    int permitIndex = 1;
                    for (PermitUploadItem permitItem : permitUploadItems) {
                        if (permitItem.bitmap != null) {
                            byte[] permitData = AppHelper.getFileDataFromDrawable(getBaseContext(), permitItem.bitmap);
                            if (permitData != null && permitData.length > 0) {
                                String permitKey = "permitFile" + permitIndex;
                                params.put(permitKey, new DataPart("permit" + permitIndex + ".jpg", permitData));
                                Log.d("REGISTRATION", "Added permit " + permitIndex + " to request (size: " + permitData.length + " bytes)");
                                permitIndex++;
                            }
                        }
                    }
                    Log.d("REGISTRATION", "Total permits being sent: " + (permitIndex - 1));
                }
                
                return params;
            }
        };
        
        Volley.newRequestQueue(this).add(request);
    }
    
    /**
     * Updates the visibility of the add permit button
     */
    private void updateAddPermitButtonVisibility() {
        // Show button if there are permits uploaded and not at max
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
}
