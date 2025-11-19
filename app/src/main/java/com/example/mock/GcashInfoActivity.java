package com.example.mock;

import static android.content.Intent.getIntent;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GcashInfoActivity extends AppCompatActivity {

    private static final String TAG = "GcashInfo";
    private static final String GET_GCASH_INFO_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_gcash_info.php";
    private static final String UPDATE_GCASH_INFO_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/update_gcash_info.php";
    private static final String UPLOAD_GCASH_QR_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/upload_gcash_qr.php";
    private static final int PICK_GCASH_QR_REQUEST = 300;

    private int userId;
    private ProgressDialog progressDialog;

    // Views
    private EditText etGcashNumber;
    private ImageView ivGcashQr;
    private Button btnSaveChanges;
    private ImageView btnBack, btnUpdateGcashQr;

    // Data
    private String currentGcashNumber, currentGcashQr;
    private Uri pendingQrUri; // Store pending QR code URI after verification (not uploaded yet)

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
        ivGcashQr = findViewById(R.id.ivGcashQr);
        btnUpdateGcashQr = findViewById(R.id.btnUpdateGcashQr);

        // Save button
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSaveChanges.setOnClickListener(v -> saveGcashChanges());

        btnUpdateGcashQr.setOnClickListener(v -> selectGcashQrImage());
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
            etGcashNumber.setText(currentGcashNumber);

            // Set GCash QR
            currentGcashQr = gcashData.optString("gcash_qr", "");
            if (!currentGcashQr.isEmpty()) {
                String fullImageUrl = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/" + currentGcashQr;
                Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(ivGcashQr);
            } else {
                ivGcashQr.setImageResource(R.drawable.placeholder);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error populating GCash data", e);
        }
    }

    private void saveGcashChanges() {
        String gcashNumber = etGcashNumber.getText().toString().trim();

        // Validation
        if (gcashNumber.isEmpty()) {
            etGcashNumber.setError("GCash number is required");
            return;
        }

        showProgressDialog("Saving GCash information...");

        // First upload QR code if there's a pending one
        if (pendingQrUri != null) {
            uploadGcashQrImage(pendingQrUri, new QRUploadCallback() {
                @Override
                public void onUploadComplete(boolean success) {
                    // After QR upload (if any), update GCash number
                    // Continue with update even if QR upload failed (but log it)
                    if (!success) {
                        Log.w(TAG, "QR code upload failed, but continuing with GCash number update");
                    }
                    updateGcashNumber(gcashNumber);
                }
            });
        } else {
            // No QR code to upload, just update GCash number
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
    
    // Interface for QR upload callback
    private interface QRUploadCallback {
        void onUploadComplete(boolean success);
    }

    private void selectGcashQrImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_GCASH_QR_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_GCASH_QR_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                verifyAndUploadGcashQr(imageUri);
            }
        }
    }

    private void uploadGcashQrImage(Uri imageUri) {
        uploadGcashQrImage(imageUri, null);
    }
    
    private void uploadGcashQrImage(Uri imageUri, QRUploadCallback callback) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            
            // Resize and compress the image
            Bitmap resizedBitmap = resizeBitmap(bitmap, 400, 400);
            
            // Convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            // Progress dialog is already showing from saveGcashChanges

            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_GCASH_QR_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                String newQrPath = jsonResponse.optString("gcash_qr_path", "");
                                currentGcashQr = newQrPath;
                                pendingQrUri = null; // Clear pending QR after successful upload
                                
                                // Update the image view
                                String fullImageUrl = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/" + newQrPath;
                                Glide.with(GcashInfoActivity.this)
                                    .load(fullImageUrl)
                                    .placeholder(R.drawable.placeholder)
                                    .error(R.drawable.placeholder)
                                    .into(ivGcashQr);
                                
                                Log.d(TAG, "QR code uploaded successfully");
                                if (callback != null) {
                                    callback.onUploadComplete(true);
                                }
                            } else {
                                String error = jsonResponse.optString("error", "Failed to upload QR code");
                                Log.e(TAG, "QR upload failed: " + error);
                                if (callback != null) {
                                    callback.onUploadComplete(false);
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing upload response", e);
                            if (callback != null) {
                                callback.onUploadComplete(false);
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error uploading QR code", error);
                        if (callback != null) {
                            callback.onUploadComplete(false);
                        }
                    }
                }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", String.valueOf(userId));
                    params.put("gcash_qr", base64Image);
                    return params;
                }
            };

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);

        } catch (IOException e) {
            Log.e(TAG, "Error processing image", e);
            hideProgressDialog();
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onUploadComplete(false);
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap original, int maxWidth, int maxHeight) {
        int width = original.getWidth();
        int height = original.getHeight();

        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(ratio * width);
        int newHeight = Math.round(ratio * height);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
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
     * Verifies GCash QR image and displays it if approved (does not upload yet)
     */
    private void verifyAndUploadGcashQr(Uri imageUri) {
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
        android.widget.ProgressBar progressBarVerifying = dialogView.findViewById(R.id.progressBarVerifying);
        TextView tvResultMessage = dialogView.findViewById(R.id.tvResultMessage);
        Button btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
        
        // Set the selected photo
        ivQrPhoto.setImageURI(imageUri);
        
        // Show dialog
        dialog.show();
        
        // Use QR code specific verification
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
                    
                    // Store the verified QR code URI (pending upload)
                    pendingQrUri = imageUri;
                    
                    // Display the image in the ImageView
                    ivGcashQr.setImageURI(imageUri);
                    
                    // Show success result
                    tvDialogTitle.setText("Verification Successful");
                    tvResultMessage.setText("✅ Valid GCash QR code detected! Click 'Save Changes' to update.");
                    tvResultMessage.setTextColor(ContextCompat.getColor(GcashInfoActivity.this, android.R.color.holo_green_dark));
                    tvResultMessage.setVisibility(View.VISIBLE);
                    btnCloseDialog.setVisibility(View.VISIBLE);
                    
                    // Set close button listener
                    btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
                } else {
                    Log.d(TAG, "❌ QR CODE VALIDATION FAILED");
                    Log.d(TAG, "Failure reason: " + reason);
                    
                    // Clear pending QR if verification failed
                    pendingQrUri = null;
                    
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
                    
                    // Set close button listener
                    btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
                }
            }
            
            @Override
            public void onVerificationError(String error) {
                Log.e(TAG, "Verification error: " + error);
                
                // Clear pending QR on error
                pendingQrUri = null;
                
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
}
















