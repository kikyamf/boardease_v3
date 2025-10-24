package com.example.mock;

import android.app.DatePickerDialog;
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
import android.widget.DatePicker;
import android.widget.EditText;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditOwnerProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditOwnerProfile";
    private static final String GET_OWNER_PROFILE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_owner_profile.php";
    private static final String UPDATE_OWNER_PROFILE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/update_owner_profile.php";
    private static final String UPLOAD_PROFILE_PIC_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/upload_profile_picture.php";
    
    private static final int PICK_IMAGE_REQUEST = 1;
    
    private ImageView ivProfilePic, ivBack;
    private EditText etFirstName, etMiddleName, etLastName, etPhoneNumber, etAddress;
    private Button btnBirthdate, btnSaveChanges;
    
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private int userId;
    private String currentProfilePicPath = "";
    private Uri selectedImageUri;
    private boolean imageChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_owner_profile);
        
        // Get user ID from intent
        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        setupClickListeners();
        loadOwnerProfile();
    }
    
    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        ivProfilePic = findViewById(R.id.ivProfilePic);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        btnBirthdate = findViewById(R.id.btnBirthdate);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etAddress = findViewById(R.id.etAddress);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }
    
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> {
            setResult(RESULT_CANCELED); // Set result when back is pressed
            finish();
        });
        
        ivProfilePic.setOnClickListener(v -> openImagePicker());
        
        // Find the edit profile overlay if it exists
        ImageView ivEditProfile = findViewById(R.id.ivEditProfile);
        if (ivEditProfile != null) {
            ivEditProfile.setOnClickListener(v -> openImagePicker());
        }
        
        btnBirthdate.setOnClickListener(v -> showDatePicker());
        
        btnSaveChanges.setOnClickListener(v -> saveProfileChanges());
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateBirthdateButton();
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    
    private void updateBirthdateButton() {
        String dateString = dateFormat.format(calendar.getTime());
        btnBirthdate.setText(dateString);
    }
    
    private void loadOwnerProfile() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading profile...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        StringRequest request = new StringRequest(Request.Method.POST, GET_OWNER_PROFILE_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    progressDialog.dismiss();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            populateForm(jsonResponse);
                        } else {
                            Toast.makeText(EditOwnerProfileActivity.this, 
                                "Failed to load profile: " + jsonResponse.getString("error"), 
                                Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing profile response", e);
                        Toast.makeText(EditOwnerProfileActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error loading profile", error);
                    Toast.makeText(EditOwnerProfileActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
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
    
    private void populateForm(JSONObject profileData) {
        try {
            // Populate text fields
            etFirstName.setText(profileData.optString("f_name", ""));
            etMiddleName.setText(profileData.optString("m_name", ""));
            etLastName.setText(profileData.optString("l_name", ""));
            etPhoneNumber.setText(profileData.optString("phone_number", ""));
            etAddress.setText(profileData.optString("p_address", ""));
            
            // Set birthdate
            String birthdate = profileData.optString("birthdate", "");
            if (!birthdate.isEmpty()) {
                btnBirthdate.setText(birthdate);
                try {
                    calendar.setTime(dateFormat.parse(birthdate));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing birthdate", e);
                }
            }
            
            // Load profile picture
            String profilePicPath = profileData.optString("profile_picture", "");
            if (!profilePicPath.isEmpty()) {
                currentProfilePicPath = profilePicPath;
                String fullImageUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/" + profilePicPath;
                Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .centerCrop()
                    .into(ivProfilePic);
            } else {
                // Set default profile picture
                ivProfilePic.setImageResource(R.drawable.btn_profile);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error populating form", e);
        }
    }
    
    private void saveProfileChanges() {
        if (!validateForm()) {
            return;
        }
        
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving changes...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // First upload profile picture if changed
        if (imageChanged && selectedImageUri != null) {
            uploadProfilePicture(progressDialog);
        } else {
            updateProfileData(progressDialog, currentProfilePicPath);
        }
    }
    
    private void uploadProfilePicture(ProgressDialog progressDialog) {
        try {
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            
            // Resize image to reduce file size
            int maxSize = 800; // Maximum width or height
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            
            if (width > maxSize || height > maxSize) {
                float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
                int newWidth = Math.round(width * ratio);
                int newHeight = Math.round(height * ratio);
                
                originalBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
                Log.d(TAG, "Resized image from " + width + "x" + height + " to " + newWidth + "x" + newHeight);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // Reduced quality to 70%
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            
            Log.d(TAG, "Original image size: " + imageBytes.length + " bytes");
            Log.d(TAG, "Encoded image length: " + encodedImage.length());
            
            // Check if image is too large (limit to 1MB)
            if (imageBytes.length > 1024 * 1024) {
                progressDialog.dismiss();
                Toast.makeText(this, "Image is too large. Please select a smaller image.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_PROFILE_PIC_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Upload response: " + response);
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                String newProfilePicPath = jsonResponse.getString("profile_picture_path");
                                updateProfileData(progressDialog, newProfilePicPath);
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(EditOwnerProfileActivity.this, 
                                    "Failed to upload profile picture: " + jsonResponse.getString("error"), 
                                    Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            progressDialog.dismiss();
                            Log.e(TAG, "Error parsing upload response", e);
                            Toast.makeText(EditOwnerProfileActivity.this, "Error uploading profile picture", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error uploading profile picture", error);
                        
                        String errorMessage = "Error uploading profile picture";
                        if (error.networkResponse != null) {
                            errorMessage += " (HTTP " + error.networkResponse.statusCode + ")";
                        }
                        
                        Toast.makeText(EditOwnerProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", String.valueOf(userId));
                    params.put("profile_picture", encodedImage);
                    return params;
                }
                
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/x-www-form-urlencoded");
                    return headers;
                }
            };
            
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
            
        } catch (IOException e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateProfileData(ProgressDialog progressDialog, String profilePicPath) {
        StringRequest request = new StringRequest(Request.Method.POST, UPDATE_OWNER_PROFILE_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    progressDialog.dismiss();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(EditOwnerProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK); // Set result to indicate successful update
                            finish();
                        } else {
                            Toast.makeText(EditOwnerProfileActivity.this, 
                                "Failed to update profile: " + jsonResponse.getString("error"), 
                                Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing update response", e);
                        Toast.makeText(EditOwnerProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error updating profile", error);
                    Toast.makeText(EditOwnerProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("f_name", etFirstName.getText().toString().trim());
                params.put("m_name", etMiddleName.getText().toString().trim());
                params.put("l_name", etLastName.getText().toString().trim());
                params.put("birthdate", btnBirthdate.getText().toString());
                params.put("phone_number", etPhoneNumber.getText().toString().trim());
                params.put("p_address", etAddress.getText().toString().trim());
                params.put("profile_picture", profilePicPath);
                return params;
            }
        };
        
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
    
    private boolean validateForm() {
        if (etFirstName.getText().toString().trim().isEmpty()) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return false;
        }
        
        if (etLastName.getText().toString().trim().isEmpty()) {
            etLastName.setError("Last name is required");
            etLastName.requestFocus();
            return false;
        }
        
        if (btnBirthdate.getText().toString().equals("Select Birthdate")) {
            Toast.makeText(this, "Please select your birthdate", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (etPhoneNumber.getText().toString().trim().isEmpty()) {
            etPhoneNumber.setError("Phone number is required");
            etPhoneNumber.requestFocus();
            return false;
        }
        
        if (etAddress.getText().toString().trim().isEmpty()) {
            etAddress.setError("Address is required");
            etAddress.requestFocus();
            return false;
        }
        
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                imageChanged = true;
                // Use Glide to load the selected image with proper circular clipping
                Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(ivProfilePic);
            }
        }
    }
}










