package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GcashInfoActivity extends AppCompatActivity {

    private static final String TAG = "GcashInfo";
    private static final String GET_GCASH_INFO_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_gcash_info.php";
    private static final String UPDATE_GCASH_INFO_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/update_gcash_info.php";
    private static final String BOARD_EASE2_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/";

    private int userId;
    private ProgressDialog progressDialog;

    // Views
    private EditText etGcashNumber;
    private Button btnSaveChanges;
    private ImageButton btnSelectQrCode;
    private ImageView btnBack;
    private ImageView ivGcashQrCode;

    // Data
    private String currentGcashNumber;
    private String currentGcashQrPath;
    private Uri selectedQrUri;

    // Image picker launcher
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            verifyAndSetQrImage(uri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gcash_info);

        // Get userId from intent
        userId = getIntent().getIntExtra("user_id", 0);

        initViews();
        setupClickListeners();
        loadGcashInfo();
    }

    private void initViews() {
        // Back button
        btnBack = findViewById(R.id.btnBack);

        // GCash fields
        etGcashNumber = findViewById(R.id.etGcashNumber);
        ivGcashQrCode = findViewById(R.id.ivGcashQrCode);
        btnSelectQrCode = findViewById(R.id.btnSelectQrCode);

        // Update button
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSelectQrCode.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });

        btnSaveChanges.setOnClickListener(v -> saveGcashChanges());
    }

    private void loadGcashInfo() {
        showProgressDialog("Loading GCash information...");

        StringRequest request = new StringRequest(Request.Method.POST, GET_GCASH_INFO_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    hideProgressDialog();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            populateGcashData(jsonResponse);
                        } else {
                            Toast.makeText(GcashInfoActivity.this, "Failed to load GCash info", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing GCash info response", e);
                        Toast.makeText(GcashInfoActivity.this, "Error loading GCash info", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideProgressDialog();
                    Log.e(TAG, "Error loading GCash info", error);
                    Toast.makeText(GcashInfoActivity.this, "Error loading GCash info", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void populateGcashData(JSONObject gcashData) {
        try {
            // Set GCash number
            currentGcashNumber = gcashData.optString("gcash_number", "");
            if (currentGcashNumber == null || currentGcashNumber.isEmpty() || currentGcashNumber.equalsIgnoreCase("null")) {
                currentGcashNumber = "";
            }
            etGcashNumber.setText(currentGcashNumber);

            // Load GCash QR code
            currentGcashQrPath = gcashData.optString("gcash_qr", "");
            if (currentGcashQrPath != null && !currentGcashQrPath.isEmpty() && !currentGcashQrPath.equalsIgnoreCase("null")) {
                loadGcashQrCode(currentGcashQrPath);
            } else {
                Log.d(TAG, "No GCash QR code available");
                ivGcashQrCode.setImageResource(R.drawable.placeholder);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error populating GCash data", e);
        }
    }

    private void loadGcashQrCode(String qrPath) {
        try {
            String fullImageUrl = qrPath;
            
            // If it's not a full URL, construct it
            if (!qrPath.startsWith("http://") && !qrPath.startsWith("https://")) {
                String cleanPath = qrPath.startsWith("/") ? qrPath.substring(1) : qrPath;
                if (cleanPath.startsWith("uploads/")) {
                    fullImageUrl = BOARD_EASE2_URL + cleanPath;
                } else {
                    fullImageUrl = BOARD_EASE2_URL + "uploads/" + cleanPath;
                }
            }
            
            Log.d(TAG, "Loading GCash QR from URL: " + fullImageUrl);
            Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(ivGcashQrCode);
        } catch (Exception e) {
            Log.e(TAG, "Error loading GCash QR code", e);
            ivGcashQrCode.setImageResource(R.drawable.placeholder);
        }
    }

    private void saveGcashChanges() {
        String gcashNumber = etGcashNumber.getText().toString().trim();

        // Validation
        if (gcashNumber.isEmpty()) {
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

        showProgressDialog("Updating GCash information...");

        // Update GCash number and QR code
        if (selectedQrUri != null) {
            updateGcashInfoWithQr(gcashNumber, selectedQrUri);
        } else {
            updateGcashNumber(gcashNumber);
        }
    }
    
    private void updateGcashNumber(String gcashNumber) {
        StringRequest request = new StringRequest(Request.Method.POST, UPDATE_GCASH_INFO_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    hideProgressDialog();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(GcashInfoActivity.this, "GCash information updated successfully", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            String error = jsonResponse.optString("error", "Failed to update GCash information");
                            Toast.makeText(GcashInfoActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing update response", e);
                        Toast.makeText(GcashInfoActivity.this, "Error updating GCash information", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideProgressDialog();
                    Log.e(TAG, "Error updating GCash information", error);
                    Toast.makeText(GcashInfoActivity.this, "Error updating GCash information", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("gcash_number", gcashNumber);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Verifies and sets QR image if approved
     */
    private void verifyAndSetQrImage(Uri imageUri) {
        Log.d(TAG, "=== QR CODE VALIDATION STARTED ===");
        Log.d(TAG, "Image URI: " + imageUri.toString());
        
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
                Log.d(TAG, "=== QR CODE VALIDATION COMPLETED ===");
                Log.d(TAG, "Is Approved: " + isApproved);
                Log.d(TAG, "Reason: " + reason);
                
                // Hide progress bar
                progressBarVerifying.setVisibility(View.GONE);
                
                if (isApproved) {
                    Log.d(TAG, "✅ QR CODE VALIDATION PASSED");
                    // Set the image as selected
                    selectedQrUri = imageUri;
                    ivGcashQrCode.setImageURI(imageUri);
                    ivGcashQrCode.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Log.d(TAG, "QR image set successfully");
                    
                    // Show success result
                    tvDialogTitle.setText("Verification Successful");
                    tvResultMessage.setText("✅ Valid GCash QR code detected!");
                    tvResultMessage.setTextColor(ContextCompat.getColor(GcashInfoActivity.this, android.R.color.holo_green_dark));
                    tvResultMessage.setVisibility(View.VISIBLE);
                    btnCloseDialog.setVisibility(View.VISIBLE);
                } else {
                    Log.d(TAG, "❌ QR CODE VALIDATION FAILED");
                    Log.d(TAG, "Failure reason: " + reason);
                    
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
                    
                    Log.d(TAG, "Showing helpful message to user: " + helpfulMessage);
                    
                    // Show failure result
                    tvDialogTitle.setText("Verification Failed");
                    tvResultMessage.setText("❌ " + helpfulMessage);
                    tvResultMessage.setTextColor(ContextCompat.getColor(GcashInfoActivity.this, android.R.color.holo_red_dark));
                    tvResultMessage.setVisibility(View.VISIBLE);
                    btnCloseDialog.setVisibility(View.VISIBLE);
                }
                
                // Set close button listener
                btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
            }
            
            @Override
            public void onVerificationError(String error) {
                Log.e(TAG, "Verification error: " + error);
                
                // Hide progress bar
                progressBarVerifying.setVisibility(View.GONE);
                
                // Show error result
                tvDialogTitle.setText("Verification Error");
                tvResultMessage.setText("QR code verification failed: " + error);
                tvResultMessage.setTextColor(ContextCompat.getColor(GcashInfoActivity.this, android.R.color.holo_red_dark));
                tvResultMessage.setVisibility(View.VISIBLE);
                btnCloseDialog.setVisibility(View.VISIBLE);
                
                // Set close button listener
                btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
            }
        });
    }

    private void updateGcashInfoWithQr(String gcashNumber, Uri qrUri) {
        try {
            // Convert image to base64
            InputStream inputStream = getContentResolver().openInputStream(qrUri);
            if (inputStream == null) {
                hideProgressDialog();
                Toast.makeText(this, "Error reading QR code image", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                hideProgressDialog();
                Toast.makeText(this, "Invalid image format", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Compress and encode image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            
            inputStream.close();
            
            // Upload with QR code
            StringRequest request = new StringRequest(Request.Method.POST, UPDATE_GCASH_INFO_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        hideProgressDialog();
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                Toast.makeText(GcashInfoActivity.this, "GCash information updated successfully", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                String error = jsonResponse.optString("error", "Failed to update GCash information");
                                Toast.makeText(GcashInfoActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing update response", e);
                            Toast.makeText(GcashInfoActivity.this, "Error updating GCash information", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        Log.e(TAG, "Error updating GCash information", error);
                        Toast.makeText(GcashInfoActivity.this, "Error updating GCash information", Toast.LENGTH_SHORT).show();
                    }
                }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", String.valueOf(userId));
                    params.put("gcash_number", gcashNumber);
                    params.put("gcash_qr", encodedImage);
                    return params;
                }
            };

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
            
        } catch (Exception e) {
            hideProgressDialog();
            Log.e(TAG, "Error processing QR code image", e);
            Toast.makeText(this, "Error processing QR code image", Toast.LENGTH_SHORT).show();
        }
    }
}
















