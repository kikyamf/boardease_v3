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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
    private static final String GET_GCASH_INFO_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_gcash_info.php";
    private static final String UPDATE_GCASH_INFO_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/update_gcash_info.php";
    private static final String UPLOAD_GCASH_QR_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/upload_gcash_qr.php";
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
                String fullImageUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/" + currentGcashQr;
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
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            
            // Resize and compress the image
            Bitmap resizedBitmap = resizeBitmap(bitmap, 400, 400);
            
            // Convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            showProgressDialog("Uploading GCash QR code...");

            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_GCASH_QR_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        hideProgressDialog();
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                String newQrPath = jsonResponse.optString("gcash_qr_path", "");
                                currentGcashQr = newQrPath;
                                
                                // Update the image view
                                String fullImageUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/" + newQrPath;
                                Glide.with(GcashInfoActivity.this)
                                    .load(fullImageUrl)
                                    .placeholder(R.drawable.placeholder)
                                    .error(R.drawable.placeholder)
                                    .into(ivGcashQr);
                                
                                Toast.makeText(GcashInfoActivity.this, "GCash QR code updated successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                String error = jsonResponse.optString("error", "Failed to upload QR code");
                                Toast.makeText(GcashInfoActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing upload response", e);
                            Toast.makeText(GcashInfoActivity.this, "Error uploading QR code", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        Log.e(TAG, "Error uploading QR code", error);
                        Toast.makeText(GcashInfoActivity.this, "Error uploading QR code", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
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
     * Verifies and uploads GCash QR image if approved
     */
    private void verifyAndUploadGcashQr(Uri imageUri) {
        // Show loading message
        Toast.makeText(this, "Verifying GCash QR image...", Toast.LENGTH_SHORT).show();
        
        // Use QR code specific verification
        ImageVerification.verifyQrCode(this, imageUri, new ImageVerification.VerificationCallback() {
            @Override
            public void onVerificationComplete(boolean isApproved, String reason) {
                if (isApproved) {
                    // Upload the approved image
                    uploadGcashQrImage(imageUri);
                    Toast.makeText(GcashInfoActivity.this, "✅ GCash QR image approved", Toast.LENGTH_SHORT).show();
                } else {
                    // Show rejection reason
                    Toast.makeText(GcashInfoActivity.this, "❌ Image rejected: " + reason, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onVerificationError(String error) {
                Toast.makeText(GcashInfoActivity.this, "Image verification failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
















