package com.example.mock;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.mock.VolleyMultipartRequest;
import com.example.mock.VolleyMultipartRequest.DataPart;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateProfileActivity extends AppCompatActivity {

    private static final String TAG = "UpdateProfileActivity";
    private static final int MAX_PERMITS = 3;

    private AutoCompleteTextView spinnerVId, spinnerProvince, spinnerMunicipality, spinnerBarangay;
    private ImageView ivUploadF, ivUploadB, UploadQr;
    private EditText etIdNumber, etGcashNum, etDetailedAddress;
    private View businessPermitSection, backIdSection;
    private ViewGroup businessPermitContainer;
    private Button btnAddPermit, btnSubmit;
    private ImageView backButton;

    // Stepper Views
    private View step1Layout, step2Layout, step3Layout;
    private TextView progressCircle1, progressCircle2, progressCircle3;
    private View progressLine3;
    private int currentStep = 1;

    private String userId, userRole;
    private boolean isBoarder;
    private Bitmap frontBitmap, backBitmap, qrBitmap;
    private List<PermitUploadItem> permitUploadItems = new ArrayList<>();
    private List<ActivityResultLauncher<String>> permitImageLaunchers = new ArrayList<>();
    private PermitUploadItem currentPermitItemForLauncher = null;

    private String selectedProvince = "", selectedMunicipality = "", selectedBarangay = "";

    // Camera handling
    private Uri cameraImageUri;
    private String currentPhotoType; // "FRONT", "BACK", "QR", "PERMIT"
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private ActivityResultLauncher<String> pickFrontImageLauncher;
    private ActivityResultLauncher<String> pickBackImageLauncher;
    private ActivityResultLauncher<String> pickQrImageLauncher;

    private static class PermitUploadItem {
        View itemView;
        ImageView imageView;
        Button removeButton;
        Bitmap bitmap;
        Uri uri;
        int index;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        // Get user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = prefs.getString("user_id", "");
        userRole = prefs.getString("user_role", "");
        isBoarder = "Boarder".equalsIgnoreCase(userRole);

        // Check for verified status - Link/Access Expiration Logic
        String status = prefs.getString("user_status", "");
        // Block access if already pending review, active, or verified
        if ("pending_admin_review".equals(status) || "active".equals(status) || "verified".equals(status) || "approved".equals(status)) {
            new AlertDialog.Builder(this)
                .setTitle("Profile Verified")
                .setMessage("Your profile has already been submitted or verified. You cannot edit it at this stage.")
                .setPositiveButton("OK", (dialog, which) -> {
                    finish(); // Close activity
                })
                .setCancelable(false)
                .show();
            return; // Stop further initialization
        }

        initViews();
        initializeAddressPicker();
        setupIdTypeSpinner();
        setupImageLaunchers();
        setupClickListeners();
        
        updateStepUI();

        if (!isBoarder) {
            businessPermitSection.setVisibility(View.VISIBLE);

            createPermitUploadItem();
            
            // Show third step circle for owners
            progressLine3.setVisibility(View.VISIBLE);
            progressCircle3.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        spinnerVId = findViewById(R.id.spinnerVId);
        spinnerProvince = findViewById(R.id.spinnerProvince);
        spinnerMunicipality = findViewById(R.id.spinnerMunicipality);
        spinnerBarangay = findViewById(R.id.spinnerBarangay);
        etDetailedAddress = findViewById(R.id.etDetailedAddress);
        ivUploadF = findViewById(R.id.ivUploadF);
        ivUploadB = findViewById(R.id.ivUploadB);
        UploadQr = findViewById(R.id.UploadQr);
        etIdNumber = findViewById(R.id.etIdNumber);
        etGcashNum = findViewById(R.id.etGcashNum);
        businessPermitSection = findViewById(R.id.businessPermitSection);
        backIdSection = findViewById(R.id.backIdSection);
        businessPermitContainer = findViewById(R.id.businessPermitContainer);
        btnAddPermit = findViewById(R.id.btnAddPermit);
        btnSubmit = findViewById(R.id.btnSubmit);
        backButton = findViewById(R.id.backButton);

        // Setup interactions for spinners
        setupSpinnerInteractions(spinnerVId);
        setupSpinnerInteractions(spinnerProvince);
        setupSpinnerInteractions(spinnerMunicipality);
        setupSpinnerInteractions(spinnerBarangay);

        // Stepper Layouts
        step1Layout = findViewById(R.id.step1Layout);
        step2Layout = findViewById(R.id.step2Layout);
        step3Layout = findViewById(R.id.step3Layout);

        // Progress Circles
        progressCircle1 = findViewById(R.id.progressCircle1);
        progressCircle2 = findViewById(R.id.progressCircle2);
        progressCircle3 = findViewById(R.id.progressCircle3);
        progressLine3 = findViewById(R.id.progressLine3);
    }

    private void setupSpinnerInteractions(AutoCompleteTextView spinner) {
        spinner.setOnClickListener(v -> spinner.showDropDown());
        spinner.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                spinner.showDropDown();
            }
        });
    }

    private void updateStepUI() {
        // Step 1 is always visible
        step1Layout.setVisibility(View.VISIBLE);

        // Manage visibility of subsequent steps
        if (currentStep >= 2) {
            step2Layout.setVisibility(View.VISIBLE);
        } else {
            step2Layout.setVisibility(View.GONE);
        }

        if (currentStep >= 3) {
            step3Layout.setVisibility(View.VISIBLE);
        } else {
            step3Layout.setVisibility(View.GONE);
        }

        // Update Circles
        updateCircle(progressCircle1, currentStep >= 1);
        updateCircle(progressCircle2, currentStep >= 2);
        updateCircle(progressCircle3, currentStep >= 3);

        // Update Button Text
        if (isBoarder) {
            btnSubmit.setText(currentStep == 2 ? "SUBMIT FOR VERIFICATION" : "NEXT");
            btnSubmit.setVisibility(currentStep > 2 ? View.GONE : View.VISIBLE); // Hide if done, though logic handles submit
        } else {
            btnSubmit.setText(currentStep == 3 ? "SUBMIT FOR VERIFICATION" : "NEXT");
        }
        
        // Smooth scroll to the newly active step to "guide" the user
        findViewById(R.id.scrollView).post(() -> {
            android.widget.ScrollView scrollView = findViewById(R.id.scrollView);
            if (scrollView != null) {
                if (currentStep == 2) {
                    scrollView.smoothScrollTo(0, step2Layout.getTop());
                } else if (currentStep == 3) {
                    scrollView.smoothScrollTo(0, step3Layout.getTop());
                } else if (currentStep == 1) {
                    scrollView.smoothScrollTo(0, 0); // Scroll to top
                }
            }
        });
    }

    private void updateCircle(TextView circle, boolean active) {
        if (active) {
            circle.setBackground(ContextCompat.getDrawable(this, R.drawable.progress_circle_filled));
            circle.setTextColor(Color.WHITE);
        } else {
            circle.setBackground(ContextCompat.getDrawable(this, R.drawable.progress_circle_hollow));
            circle.setTextColor(Color.parseColor("#666666"));
        }
    }

    private void setupIdTypeSpinner() {
        String[] idTypes = {
                "Philippine Passport", "Driver's License", "PhilID (National ID)",
                "UMID(SSS ID)", "GSIS e-card", "PhilHealth ID", "TIN ID (BIR)", "Voter's ID", "Postal ID"
        };
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, idTypes) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.widget.TextView textView = (android.widget.TextView) super.getView(position, convertView, parent);
                textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_regular));
                return textView;
            }
        };
        spinnerVId.setAdapter(adapter);

        spinnerVId.setOnItemClickListener((parent, view, position, id) -> {
            String selectedIdType = parent.getItemAtPosition(position).toString();
            backIdSection.setVisibility(selectedIdType.equals("Philippine Passport") ? View.GONE : View.VISIBLE);
        });
    }

    private void initializeAddressPicker() {
        // Initialize barangay spinner with default option
        String[] defaultBarangayArray = {"Select Barangay"};
        android.widget.ArrayAdapter<String> defaultBarangayAdapter = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, defaultBarangayArray);
        spinnerBarangay.setAdapter(defaultBarangayAdapter);
        
        // Load provinces first
        loadProvinces();
        
        // Set up province selection listener
        spinnerProvince.setOnItemClickListener((parent, view, position, id) -> {
            String newProvince = parent.getItemAtPosition(position).toString();
            if (!newProvince.equals(selectedProvince)) {
                selectedProvince = newProvince;
                spinnerMunicipality.setText("");
                spinnerBarangay.setText("");
                selectedMunicipality = "";
                selectedBarangay = "";
                loadMunicipalities(selectedProvince);
            }
        });
        
        // Set up municipality selection listener
        spinnerMunicipality.setOnItemClickListener((parent, view, position, id) -> {
            String newMunicipality = parent.getItemAtPosition(position).toString();
            if (!newMunicipality.equals(selectedMunicipality)) {
                selectedMunicipality = newMunicipality;
                spinnerBarangay.setText("");
                selectedBarangay = "";
                loadBarangays(selectedMunicipality);
            }
        });

        // Set up barangay selection listener
        spinnerBarangay.setOnItemClickListener((parent, view, position, id) -> {
            selectedBarangay = parent.getItemAtPosition(position).toString();
        });
    }

    private void loadProvinces() {
        String url = "https://boardease.calapebohol.com/philippine_address_api.php?action=provinces";
        com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
            Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.getBoolean("success")) {
                        JSONArray provincesArray = response.getJSONArray("data");
                        String[] provinceNames = new String[provincesArray.length()];
                        for (int i = 0; i < provincesArray.length(); i++) {
                            provinceNames[i] = provincesArray.getJSONObject(i).getString("name");
                        }
                        
                        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, provinceNames) {
                            @Override
                            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                                android.widget.TextView textView = (android.widget.TextView) super.getView(position, convertView, parent);
                                textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_regular));
                                return textView;
                            }
                        };
                        spinnerProvince.setAdapter(adapter);
                    } else {
                        loadProvincesFallback();
                    }
                } catch (JSONException e) {
                    loadProvincesFallback();
                }
            },
            error -> loadProvincesFallback()
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void loadProvincesFallback() {
        String[] fallbackProvinces = {
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
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, fallbackProvinces) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.widget.TextView textView = (android.widget.TextView) super.getView(position, convertView, parent);
                textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_regular));
                return textView;
            }
        };
        spinnerProvince.setAdapter(adapter);
    }

    private void loadMunicipalities(String province) {
        try {
            String url = "https://boardease.calapebohol.com/philippine_address_api.php?action=municipalities&province_name=" + URLEncoder.encode(province, "UTF-8");
            com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray array = response.getJSONArray("data");
                            String[] names = new String[array.length()];
                            for (int i = 0; i < array.length(); i++) {
                                names[i] = array.getJSONObject(i).getString("name");
                            }
                            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, names) {
                                @Override
                                public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                                    android.widget.TextView textView = (android.widget.TextView) super.getView(position, convertView, parent);
                                    textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_regular));
                                    return textView;
                                }
                            };
                            spinnerMunicipality.setAdapter(adapter);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e(TAG, "Error loading municipalities: " + error.getMessage())
            );
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBarangays(String municipality) {
        try {
            String url = "https://boardease.calapebohol.com/philippine_address_api.php?action=barangays&municipality_name=" + URLEncoder.encode(municipality, "UTF-8");
            com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray array = response.getJSONArray("data");
                            String[] names = new String[array.length()];
                            for (int i = 0; i < array.length(); i++) {
                                names[i] = array.getJSONObject(i).getString("name");
                            }
                            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, names) {
                                @Override
                                public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                                    android.widget.TextView textView = (android.widget.TextView) super.getView(position, convertView, parent);
                                    textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_regular));
                                    return textView;
                                }
                            };
                            spinnerBarangay.setAdapter(adapter);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e(TAG, "Error loading barangays: " + error.getMessage())
            );
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ActivityResultLauncher<Intent> idCaptureLauncher;

    // ... (existing code)

    private void setupImageLaunchers() {
        // ID Capture Launcher
        idCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String imagePath = result.getData().getStringExtra("image_path");
                        String idNumber = result.getData().getStringExtra("id_number");
                        String type = result.getData().getStringExtra("id_type");

                        if (imagePath != null) {
                            Uri uri = Uri.fromFile(new java.io.File(imagePath));
                            if ("front".equals(type)) {
                                // For ID Capture, we already verified it, but we'll run it through the standardized setter
                                // to ensure bitmaps are consistent. We can skip re-verification if redundant,
                                // but for now let's reuse the logic to populate frontBitmap.
                                // Optimization: If IDCapture already verified, maybe just set bitmap?
                                // Let's use the existing method for consistency, it handles bitmap decoding.
                                verifyAndSetFrontImage(uri);
                                
                                if (idNumber != null && !idNumber.isEmpty()) {
                                    etIdNumber.setText(idNumber);
                                    Toast.makeText(this, "ID Number extracted: " + idNumber, Toast.LENGTH_SHORT).show();
                                }
                            } else if ("back".equals(type)) {
                                verifyAndSetBackImage(uri);
                            }
                        }
                    }
                }
        );

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        if ("FRONT".equals(currentPhotoType) || "BACK".equals(currentPhotoType)) {
                             openIdCapture(currentPhotoType.toLowerCase());
                        } else {
                             openCamera();
                        }
                    } else {
                        Toast.makeText(this, "Camera permission needed to take photos", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                        switch (currentPhotoType) {
                            case "QR":
                                verifyAndSetQrImage(cameraImageUri);
                                break;
                            case "PERMIT":
                                if (currentPermitItemForLauncher != null) {
                                    handlePermitImageSelection(currentPermitItemForLauncher, cameraImageUri);
                                    currentPermitItemForLauncher = null;
                                }
                                break;
                            // FRONT and BACK are now handled by IdCaptureActivity
                        }
                    }
                }
        );

        pickFrontImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) verifyAndSetFrontImage(uri);
        });

        pickBackImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) verifyAndSetBackImage(uri);
        });

        pickQrImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) verifyAndSetQrImage(uri);
        });

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
    }

    // ...

    private void setupClickListeners() {
        ivUploadF.setOnClickListener(v -> {
            currentPhotoType = "FRONT";
            requestCameraPermission();
        });
        ivUploadB.setOnClickListener(v -> {
            currentPhotoType = "BACK";
            requestCameraPermission();
        });
        UploadQr.setOnClickListener(v -> {
            currentPhotoType = "QR";
            // GCash QR is Gallery only
            pickQrImageLauncher.launch("image/*");
        });

        btnAddPermit.setOnClickListener(v -> {
            if (permitUploadItems.size() < MAX_PERMITS) {
                createPermitUploadItem();
            } else {
                Toast.makeText(this, "Maximum of " + MAX_PERMITS + " permits allowed", Toast.LENGTH_SHORT).show();
            }
        });

        btnSubmit.setOnClickListener(v -> handleNextStep());
        
        backButton.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                updateStepUI();
            } else {
                onBackPressed();
            }
        });
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
             if ("FRONT".equals(currentPhotoType) || "BACK".equals(currentPhotoType)) {
                 openIdCapture(currentPhotoType.toLowerCase());
             } else {
                 openCamera();
             }
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }
    
    private void showPermitImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                 // Take Photo
                 if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                     openCamera();
                 } else {
                     requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
                 }
            } else if (which == 1) {
                // Choose from Gallery
                if (currentPermitItemForLauncher != null) {
                    permitImageLaunchers.get(currentPermitItemForLauncher.index).launch("image/*");
                }
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }
    
    private void openIdCapture(String side) {
        Intent intent = new Intent(this, IdCaptureActivity.class);
        intent.putExtra("id_type", side); // "front" or "back"
        intent.putExtra("selected_id_type", spinnerVId.getText().toString());
        idCaptureLauncher.launch(intent);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            java.io.File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (java.io.IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                cameraImageUri = androidx.core.content.FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private java.io.File createImageFile() throws java.io.IOException {
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        java.io.File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return java.io.File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void handleNextStep() {
        if (currentStep == 1) {
            if (validateStep1()) {
                currentStep = 2;
                updateStepUI();
            }
        } else if (currentStep == 2) {
            if (validateStep2()) {
                if (isBoarder) {
                    performUpdate();
                } else {
                    currentStep = 3;
                    updateStepUI();
                }
            }
        } else if (currentStep == 3) {
            if (validateStep3()) {
                performUpdate();
            }
        }
    }

    private boolean validateStep1() {
        if (selectedProvince.isEmpty()) {
            Toast.makeText(this, "Please select province", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedMunicipality.isEmpty()) {
            Toast.makeText(this, "Please select municipality", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedBarangay.isEmpty()) {
            Toast.makeText(this, "Please select barangay", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDetailedAddress.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter detailed address", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validateStep2() {
        if (spinnerVId.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select ID type", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etIdNumber.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter ID number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (frontBitmap == null) {
            Toast.makeText(this, "Please upload front ID", Toast.LENGTH_SHORT).show();
            return false;
        }
        String idType = spinnerVId.getText().toString();
        if (!idType.equals("Philippine Passport") && backBitmap == null) {
            Toast.makeText(this, "Please upload back ID", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validateStep3() {
        if (qrBitmap == null) {
            Toast.makeText(this, "Please upload GCash QR", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etGcashNum.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter GCash number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (permitUploadItems.isEmpty() || permitUploadItems.get(0).bitmap == null) {
            Toast.makeText(this, "Please upload at least one business permit", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void verifyAndSetFrontImage(Uri imageUri) {
        String selectedIdType = spinnerVId.getText().toString();
        if (selectedIdType == null || selectedIdType.isEmpty() || selectedIdType.equals("Select --")) {
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
                    
                    try {
                        ContentResolver resolver = getContentResolver();
                        InputStream is = resolver.openInputStream(imageUri);
                        frontBitmap = BitmapFactory.decodeStream(is);
                        
                        // Automatically populate ID number if extracted
                        if (frontBitmap != null && result.extractedIdNumber != null && !result.extractedIdNumber.isEmpty()) {
                            etIdNumber.setText(result.extractedIdNumber);
                            Toast.makeText(UpdateProfileActivity.this, "ID Number Extracted: " + result.extractedIdNumber, Toast.LENGTH_SHORT).show();
                        } else {
                            // Even if ID number isn't found, the ID itself is valid
                            Toast.makeText(UpdateProfileActivity.this, "Valid ID detected.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading front image", e);
                        Toast.makeText(UpdateProfileActivity.this, "Error loading front image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(UpdateProfileActivity.this, result.reason, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onVerificationError(String error) {
                Toast.makeText(UpdateProfileActivity.this, "Front ID verification failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void verifyAndSetBackImage(Uri imageUri) {
        String selectedIdType = spinnerVId.getText().toString();
        if (selectedIdType == null || selectedIdType.isEmpty() || selectedIdType.equals("Select --")) {
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
                    
                    try {
                        ContentResolver resolver = getContentResolver();
                        InputStream is = resolver.openInputStream(imageUri);
                        backBitmap = BitmapFactory.decodeStream(is);
                        Toast.makeText(UpdateProfileActivity.this, "Back ID Validated: " + result.reason, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading back image", e);
                        Toast.makeText(UpdateProfileActivity.this, "Error loading back image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(UpdateProfileActivity.this, result.reason, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onVerificationError(String error) {
                Toast.makeText(UpdateProfileActivity.this, "Back ID verification failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void verifyAndSetQrImage(Uri uri) {
        ImageVerification.verifyQrCode(this, uri, new ImageVerification.VerificationCallback() {
            @Override
            public void onVerificationComplete(boolean isApproved, String reason) {
                if (isApproved) {
                    try {
                        qrBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                        UploadQr.setImageBitmap(qrBitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(UpdateProfileActivity.this, "Invalid QR: " + reason, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onVerificationError(String error) {
                Toast.makeText(UpdateProfileActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createPermitUploadItem() {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView label = new TextView(this);
        label.setText("Business Permit " + (permitUploadItems.size() + 1));
        label.setTextColor(0xFF000000);
        label.setTextSize(14);
        label.setPadding(0, 8, 0, 8);
        label.setTypeface(ResourcesCompat.getFont(this, R.font.poppins_medium));
        itemLayout.addView(label);

        ImageView permitImageView = new ImageView(this);
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
        itemLayout.addView(permitImageView);

        Button removeButton = new Button(this);
        removeButton.setText("Remove");
        removeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF4444));
        removeButton.setTextColor(0xFFFFFFFF);
        removeButton.setTypeface(ResourcesCompat.getFont(this, R.font.poppins_medium));
        removeButton.setVisibility(permitUploadItems.size() > 0 ? View.VISIBLE : View.GONE);
        itemLayout.addView(removeButton);

        if (permitUploadItems.size() > 0) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) itemLayout.getLayoutParams();
            params.topMargin = (int) (16 * density);
            itemLayout.setLayoutParams(params);
        }

        PermitUploadItem item = new PermitUploadItem();
        item.itemView = itemLayout;
        item.imageView = permitImageView;
        item.removeButton = removeButton;
        item.index = permitUploadItems.size();

        item.imageView.setOnClickListener(v -> {
            currentPermitItemForLauncher = item;
            currentPhotoType = "PERMIT";
            showPermitImageSourceDialog();
        });

        removeButton.setOnClickListener(v -> {
            businessPermitContainer.removeView(itemLayout);
            permitUploadItems.remove(item);
            updatePermitLabels();
        });



        businessPermitContainer.addView(itemLayout);
        permitUploadItems.add(item);
    }
    
    private void populateFieldsFromSession() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String address = prefs.getString("user_address", "");
        
        // If address is available, try to parse it (assuming Format: "Detailed, Barangay, Municipality, Province")
        // This is a best-effort pre-fill since the formats might vary
        if (!address.isEmpty()) {
            etDetailedAddress.setText(address);
        }
    }

    private void updatePermitLabels() {
        for (int i = 0; i < permitUploadItems.size(); i++) {
            PermitUploadItem item = permitUploadItems.get(i);
            LinearLayout layout = (LinearLayout) item.itemView;
            TextView label = (TextView) layout.getChildAt(0);
            label.setText("Business Permit " + (i + 1));
            item.index = i;
            item.removeButton.setVisibility(permitUploadItems.size() > 1 ? View.VISIBLE : View.GONE);
        }
    }

    private void handlePermitImageSelection(PermitUploadItem item, Uri uri) {
        ImageVerification.verifyImage(this, uri, new ImageVerification.VerificationCallback() {
            @Override
            public void onVerificationComplete(boolean isApproved, String reason) {
                if (isApproved) {
                    try {
                        item.uri = uri;
                        item.bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                        item.imageView.setImageBitmap(item.bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(UpdateProfileActivity.this, "Invalid Permit: " + reason, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onVerificationError(String error) {
                Toast.makeText(UpdateProfileActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performUpdate() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating profile...");
        progressDialog.show();

        String url = "https://boardease.calapebohol.com/update_profile.php";

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url,
                response -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject obj = new JSONObject(new String(response.data));
                        if (obj.getBoolean("success")) {
                            Toast.makeText(this, "Profile updated! Waiting for admin review.", Toast.LENGTH_LONG).show();
                            // Update local status
                            SharedPreferences.Editor editor = getSharedPreferences("UserSession", MODE_PRIVATE).edit();
                            editor.putString("user_status", "pending_admin_review");
                            editor.apply();
                            
                            // Redirect to login or show status message
                            Intent intent = new Intent(this, Login.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, obj.getString("message"), Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("reg_id", userId);
                params.put("idType", spinnerVId.getText().toString());
                params.put("idNumber", etIdNumber.getText().toString().trim());
                params.put("province", selectedProvince);
                params.put("municipality", selectedMunicipality);
                params.put("barangay", selectedBarangay);
                params.put("detailed_address", etDetailedAddress.getText().toString().trim());
                
                if (!isBoarder) {
                    params.put("gcash_num", etGcashNum.getText().toString().trim());
                }
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                if (frontBitmap != null) {
                    params.put("idFrontFile", new DataPart("front.jpg", AppHelper.getFileDataFromDrawable(UpdateProfileActivity.this, frontBitmap)));
                }
                if (backBitmap != null) {
                    params.put("idBackFile", new DataPart("back.jpg", AppHelper.getFileDataFromDrawable(UpdateProfileActivity.this, backBitmap)));
                }
                if (qrBitmap != null) {
                    params.put("gcash_qr", new DataPart("qr.jpg", AppHelper.getFileDataFromDrawable(UpdateProfileActivity.this, qrBitmap)));
                }
                for (int i = 0; i < permitUploadItems.size(); i++) {
                    PermitUploadItem item = permitUploadItems.get(i);
                    if (item.bitmap != null) {
                        params.put("business_permits[" + i + "]", new DataPart("permit_" + i + ".jpg", AppHelper.getFileDataFromDrawable(UpdateProfileActivity.this, item.bitmap)));
                    }
                }
                return params;
            }
        };

        Volley.newRequestQueue(this).add(multipartRequest);
    }
}
