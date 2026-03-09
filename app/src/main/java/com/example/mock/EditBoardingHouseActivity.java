package com.example.mock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.util.Log;
import android.view.ViewGroup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditBoardingHouseActivity extends AppCompatActivity {

    private EditText etBhName, etBhAddress, etBhDescription, etBhRules, etArea, etBuildYear;
    private TextView tvBathroomsCount;
    private ImageButton btnIncrementBathrooms, btnDecrementBathrooms;
    private EditText etDetailedAddress;
    private Spinner spinnerProvince, spinnerMunicipality, spinnerBarangay;
    private ViewPager2 viewPagerImages;
    private ImageView ivPlaceholder;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddImages;

    private static final String TAG = "EditBoardingHouse";
    
    // Address picker data
    private String selectedProvince = "";
    private String selectedMunicipality = "";
    private String selectedBarangay = "";
    private String selectedDetailedAddress = "";
    private String pendingAddressToParse = ""; 

    private ArrayList<Uri> imageUris = new ArrayList<>();
    private ArrayList<Uri> originalImageUris = new ArrayList<>(); // Track original images
    private ArrayList<String> removedImagePaths = new ArrayList<>(); // Track removed image paths for database deletion
    private HashMap<Uri, String> uriToPathMap = new HashMap<>(); // Map URI to image path for deletion tracking
    private ImageAdapter imageAdapter;

    private int bhId; // 📌 unique ID for this listing
    
    // Loading dialogs
    private ProgressDialog loadingDialog;
    private ProgressDialog imageUploadDialog;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_boarding_house);

        // Bind views
        etBhName = findViewById(R.id.etTitle);
        etBhAddress = findViewById(R.id.etAddress);
        etBhDescription = findViewById(R.id.etDescription);
        etBhRules = findViewById(R.id.etRules);
        tvBathroomsCount = findViewById(R.id.tvBathroomsCount);
        btnIncrementBathrooms = findViewById(R.id.btnIncrementBathrooms);
        btnDecrementBathrooms = findViewById(R.id.btnDecrementBathrooms);
        
        setupBathroomsStepper();
        
        // Address views
        spinnerProvince = findViewById(R.id.spinnerProvince);
        spinnerMunicipality = findViewById(R.id.spinnerMunicipality);
        spinnerBarangay = findViewById(R.id.spinnerBarangay);
        etDetailedAddress = findViewById(R.id.etDetailedAddress);
        
        viewPagerImages = findViewById(R.id.viewPagerImages);
        ivPlaceholder = findViewById(R.id.ivPlaceholder);
        fabAddImages = findViewById(R.id.fabAddImages);

        // Initialize address picker
        initializeAddressPicker();

        // Get data from intent
        Intent intent = getIntent();
        bhId = intent.getIntExtra("bh_id", -1);
        
        // Set basic data from intent first
        etBhName.setText(intent.getStringExtra("bh_name"));
        // Don't add images here - will be loaded from API to avoid duplication
        
        // Try to fetch complete data from server
        // Show loading while fetching data
        showLoadingDialog("Loading boarding house details...");
        fetchBoardingHouseDetails();


        // Setup adapter
        imageAdapter = new ImageAdapter(this, imageUris, position -> {
            System.out.println("DEBUG: Removing image at position: " + position);
            System.out.println("DEBUG: Before removal - imageUris size: " + imageUris.size());
            System.out.println("DEBUG: Before removal - originalImageUris size: " + originalImageUris.size());
            
            imageUris.remove(position);
            
            // Create a new adapter instance to force ViewPager2 refresh
            imageAdapter = new ImageAdapter(this, imageUris, this::removeImage);
            viewPagerImages.setAdapter(imageAdapter);
            
            // Update visibility
            updateImageViewsVisibility();
            
            System.out.println("DEBUG: After removal - imageUris size: " + imageUris.size());
        });
        viewPagerImages.setAdapter(imageAdapter);

        updateImageViewsVisibility();

        // Placeholder click = pick new images
        ivPlaceholder.setOnClickListener(v -> pickImages());
        
        // FAB click = pick new images
        fabAddImages.setOnClickListener(v -> pickImages());

        // Save Changes button
        findViewById(R.id.btnSaveChanges).setOnClickListener(v -> saveChanges());
        
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            // Check if there are unsaved changes
            if (hasUnsavedChanges()) {
                // Show confirmation dialog
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Are you sure you want to go back?")
                    .setPositiveButton("Discard", (dialog, which) -> finish())
                    .setNegativeButton("Cancel", null)
                    .setIcon(R.drawable.ic_alert_white)
                    .show();
            } else {
                finish();
            }
        });
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, 2001);
    }
    
    // Check if there are unsaved changes
    private boolean hasUnsavedChanges() {
        // Check if any text fields have been modified
        String currentName = etBhName.getText().toString().trim();
        String currentAddress = etBhAddress.getText().toString().trim();
        String currentDescription = etBhDescription.getText().toString().trim();
        String currentRules = etBhRules.getText().toString().trim();
        String currentBathrooms = tvBathroomsCount.getText().toString().trim();
        String currentArea = etArea.getText().toString().trim();
        String currentBuildYear = etBuildYear.getText().toString().trim();
        
        // Get original values from intent
        String originalName = getIntent().getStringExtra("bh_name");
        String originalAddress = getIntent().getStringExtra("bh_address");
        String originalDescription = getIntent().getStringExtra("bh_description");
        String originalRules = getIntent().getStringExtra("bh_rules");
        String originalBathrooms = getIntent().getStringExtra("number_of_bathroom");
        String originalArea = getIntent().getStringExtra("area");
        String originalBuildYear = getIntent().getStringExtra("build_year");
        
        // Check if any field has changed
        boolean textChanged = !currentName.equals(originalName != null ? originalName : "") ||
                            !currentAddress.equals(originalAddress != null ? originalAddress : "") ||
                            !currentDescription.equals(originalDescription != null ? originalDescription : "") ||
                            !currentRules.equals(originalRules != null ? originalRules : "") ||
                            !currentBathrooms.equals(originalBathrooms != null ? originalBathrooms : "") ||
                            !currentArea.equals(originalArea != null ? originalArea : "") ||
                            !currentBuildYear.equals(originalBuildYear != null ? originalBuildYear : "");
        
        // Check if images were modified
        boolean imagesChanged = imagesWereModified();
        
        return textChanged || imagesChanged;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2001 && resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    if (!imageUris.contains(uri)) {
                        verifyAndAddImage(uri);
                    }
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if (!imageUris.contains(uri)) {
                    verifyAndAddImage(uri);
                }
            }
        }
    }

    private void saveChanges() {
        String name = etBhName.getText().toString().trim();
        String address = etBhAddress.getText().toString().trim();
        
        if (TextUtils.isEmpty(name)) {
            etBhName.setError("Listing name is required");
            etBhName.requestFocus();
            return;
        }

        if (spinnerProvince.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, "Please select a province", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spinnerMunicipality.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, "Please select a municipality", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spinnerBarangay.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, "Please select a barangay", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(etDetailedAddress.getText().toString().trim())) {
            etDetailedAddress.setError("Detailed address is required");
            etDetailedAddress.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Address cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading dialog with progress message
        showLoadingDialog("Saving changes...");
        
        // Update boarding house details on server
        updateBoardingHouseOnServer();
    }

    private void updateBoardingHouseOnServer() {
        String url = "https://boardease.calapebohol.com/update_boarding_houses.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    hideLoadingDialog();
                    
                        if (response.contains("success")) {
                            // Success - check if images were modified
                            if (imagesWereModified()) {
                                // First delete removed images from database
                                if (!removedImagePaths.isEmpty()) {
                                    deleteRemovedImagesFromDatabase();
                                } else {
                                    // No images to delete, proceed with uploads
                                    ArrayList<Uri> newImages = getNewImages();
                                    if (!newImages.isEmpty()) {
                                        showImageUploadDialog();
                                        uploadNewImagesSequentially(newImages, 0);
                                    } else {
                                        Toast.makeText(this, "Boarding house updated successfully!", Toast.LENGTH_SHORT).show();
                                        finishWithSuccess();
                                    }
                                }
                            } else {
                                // No image changes
                                Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show();
                                finishWithSuccess();
                            }
                        } else {
                            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                        }
                },
                error -> {
                    hideLoadingDialog();
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bh_id", String.valueOf(bhId));
                params.put("bh_name", etBhName.getText().toString().trim());
                params.put("bh_address", etBhAddress.getText().toString().trim());
                params.put("bh_description", etBhDescription.getText().toString().trim());
                params.put("bh_rules", etBhRules.getText().toString().trim());
                params.put("number_of_bathroom", tvBathroomsCount.getText().toString().trim());
                params.put("area", etArea.getText().toString().trim());
                params.put("build_year", etBuildYear.getText().toString().trim());
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
    
    // Custom request class to handle POST data properly
    private static class CustomStringRequest extends com.android.volley.Request<String> {
        private final com.android.volley.Response.Listener<String> mListener;
        private final Map<String, String> mParams;
        
        public CustomStringRequest(int method, String url, Map<String, String> params, 
                                 com.android.volley.Response.Listener<String> listener, 
                                 com.android.volley.Response.ErrorListener errorListener) {
            super(method, url, errorListener);
            mListener = listener;
            mParams = params;
        }
        
        @Override
        protected Map<String, String> getParams() {
            return mParams;
        }
        
        @Override
        protected com.android.volley.Response<String> parseNetworkResponse(com.android.volley.NetworkResponse response) {
            String parsed;
            try {
                parsed = new String(response.data, "UTF-8");
            } catch (Exception e) {
                parsed = new String(response.data);
            }
            return com.android.volley.Response.success(parsed, com.android.volley.toolbox.HttpHeaderParser.parseCacheHeaders(response));
        }
        
        @Override
        protected void deliverResponse(String response) {
            mListener.onResponse(response);
        }
    }

    private void fetchBoardingHouseDetails() {
        String url = "https://boardease.calapebohol.com/get_boarding_houses.php";
        
        // Clear existing images to avoid duplication
        imageUris.clear();
        originalImageUris.clear();
        uriToPathMap.clear();
        
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        System.out.println("API Response: " + response);
                        
                        if (response == null || response.trim().isEmpty()) {
                            Toast.makeText(this, "Empty response from server", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        JSONObject obj = new JSONObject(response);
                        
                        // Check for error
                        if (obj.has("error")) {
                            Toast.makeText(this, "Server Error: " + obj.getString("error"), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Populate fields
                        etBhName.setText(obj.optString("bh_name", ""));
                        String fullAddress = obj.optString("bh_address", "");
                        etBhAddress.setText(fullAddress);
                        etBhDescription.setText(obj.optString("bh_description", ""));
                        etBhRules.setText(obj.optString("bh_rules", ""));
                        // Robust parsing for numeric fields
                        String bathroomCount = obj.optString("number_of_bathroom", "1");
                        tvBathroomsCount.setText(bathroomCount);
                        
                        // Use obj.get().toString() to avoid issues with numeric types in JSON
                        String areaVal = obj.has("area") ? obj.get("area").toString() : "";
                        String yearVal = obj.has("build_year") ? obj.get("build_year").toString() : "";
                        
                        etArea.setText(areaVal);
                        etBuildYear.setText(yearVal);
                        
                        Log.d(TAG, "Fetched Details - Area: " + areaVal + ", Year: " + yearVal + ", Bathrooms: " + bathroomCount);
                        
                        // Parse address to populate spinners
                        if (!fullAddress.isEmpty()) {
                            parseAndPopulateAddress(fullAddress);
                        }

                        // Handle images
                        if (obj.has("images") && !obj.isNull("images")) {
                            JSONArray imagesArray = obj.getJSONArray("images");
                            for (int i = 0; i < imagesArray.length(); i++) {
                                String imagePath = imagesArray.getString(i);
                                if (imagePath != null && !imagePath.isEmpty()) {
                                    try {
                                        Uri uri = Uri.parse(imagePath);
                                        imageUris.add(uri);
                                        originalImageUris.add(uri); // Track as original image
                                        
                                        // Store the relative path (without domain and leading slash) for database deletion
                                        String relativePath = imagePath;
                                        if (imagePath.contains("/uploads/")) {
                                            relativePath = imagePath.substring(imagePath.indexOf("/uploads/") + 1); // +1 to remove leading slash
                                        }
                                        uriToPathMap.put(uri, relativePath); // Map URI to relative path for deletion tracking
                                        
                                        System.out.println("DEBUG: Full path: " + imagePath);
                                        System.out.println("DEBUG: Relative path: " + relativePath);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                        // Update UI - create new adapter to ensure proper refresh
                        imageAdapter = new ImageAdapter(this, imageUris, this::removeImage);
                        viewPagerImages.setAdapter(imageAdapter);
                        updateImageViewsVisibility();

                        // Hide loading dialog
                        hideLoadingDialog();

                    } catch (Exception e) {
                        e.printStackTrace();
                        hideLoadingDialog();
                        Toast.makeText(this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    hideLoadingDialog();
                    Toast.makeText(this, "Network Error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", "1"); // Hardcoded for now - same as MainActivity
                params.put("bh_id", String.valueOf(bhId));
                return params;
            }
        };

        queue.add(request);
    }

    // Helper method to get only new images (not in original list)
    private ArrayList<Uri> getNewImages() {
        ArrayList<Uri> newImages = new ArrayList<>();
        for (Uri uri : imageUris) {
            if (!originalImageUris.contains(uri)) {
                newImages.add(uri);
            }
        }
        return newImages;
    }
    
    // Helper method to check if images were modified (added or removed)
    private boolean imagesWereModified() {
        // Check if any new images were added
        ArrayList<Uri> newImages = getNewImages();
        if (!newImages.isEmpty()) {
            System.out.println("DEBUG: New images detected: " + newImages.size());
            return true;
        }
        
        // Check if any original images were removed
        removedImagePaths.clear(); // Clear previous removals
        boolean hasRemovals = false;
        
        System.out.println("DEBUG: Checking for removed images...");
        System.out.println("DEBUG: Original images count: " + originalImageUris.size());
        System.out.println("DEBUG: Current images count: " + imageUris.size());
        System.out.println("DEBUG: URI to Path Map size: " + uriToPathMap.size());
        
        // Debug: Print all original URIs
        for (int i = 0; i < originalImageUris.size(); i++) {
            Uri originalUri = originalImageUris.get(i);
            System.out.println("DEBUG: Original URI " + i + ": " + originalUri.toString());
            String path = uriToPathMap.get(originalUri);
            System.out.println("DEBUG: Mapped path: " + path);
        }
        
        // Debug: Print all current URIs
        for (int i = 0; i < imageUris.size(); i++) {
            Uri currentUri = imageUris.get(i);
            System.out.println("DEBUG: Current URI " + i + ": " + currentUri.toString());
        }
        
        for (Uri originalUri : originalImageUris) {
            boolean isInCurrentList = imageUris.contains(originalUri);
            System.out.println("DEBUG: Checking URI: " + originalUri.toString());
            System.out.println("DEBUG: Is in current list? " + isInCurrentList);
            
            if (!isInCurrentList) {
                // This image was removed, add its path to removal list
                String imagePath = uriToPathMap.get(originalUri);
                System.out.println("DEBUG: Found removed URI: " + originalUri.toString());
                System.out.println("DEBUG: Path for removed URI: " + imagePath);
                
                if (imagePath != null) {
                    removedImagePaths.add(imagePath);
                    System.out.println("DEBUG: Added to removal list: " + imagePath);
                    hasRemovals = true;
                } else {
                    System.out.println("DEBUG: WARNING - No path found for removed URI!");
                }
            }
        }
        
        if (hasRemovals) {
            System.out.println("DEBUG: Total removed images: " + removedImagePaths.size());
            for (String path : removedImagePaths) {
                System.out.println("DEBUG: Will delete: " + path);
            }
        } else {
            System.out.println("DEBUG: No images were removed");
        }
        
        return hasRemovals;
    }
    
    private void deleteRemovedImagesFromDatabase() {
        String url = "https://boardease.calapebohol.com/delete_bh_images.php";
        
        System.out.println("DEBUG: Starting database deletion...");
        System.out.println("DEBUG: BH ID: " + bhId);
        System.out.println("DEBUG: Images to delete: " + removedImagePaths.size());
        
        // Create JSON array of image paths to delete
        JSONArray imagePathsArray = new JSONArray();
        for (String imagePath : removedImagePaths) {
            imagePathsArray.put(imagePath);
            System.out.println("DEBUG: Adding to JSON: " + imagePath);
        }
        
        System.out.println("DEBUG: JSON array: " + imagePathsArray.toString());
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        System.out.println("Delete images response: " + response);
                        System.out.println("Response length: " + response.length());
                        
                        // Check if response is HTML (PHP error)
                        if (response.trim().startsWith("<")) {
                            System.out.println("ERROR: Server returned HTML instead of JSON");
                            System.out.println("HTML Response: " + response);
                            Toast.makeText(this, "Server returned HTML. Check PHP errors.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        // Check if response is empty
                        if (response.trim().isEmpty()) {
                            System.out.println("ERROR: Empty response from server");
                            Toast.makeText(this, "Empty response from server", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        JSONObject obj = new JSONObject(response);
                        
                        if (obj.has("success")) {
                            System.out.println("DEBUG: PHP deletion successful!");
                            String message = obj.optString("message", "Images deleted");
                            int deletedCount = obj.optInt("deleted_count", 0);
                            System.out.println("DEBUG: PHP message: " + message);
                            System.out.println("DEBUG: PHP deleted count: " + deletedCount);
                            
                            // Show success message with deletion count
                            if (deletedCount > 0) {
                                Toast.makeText(this, "Successfully deleted " + deletedCount + " image(s)!", Toast.LENGTH_SHORT).show();
                            }
                            
                            // Images deleted successfully, now handle new image uploads
                            ArrayList<Uri> newImages = getNewImages();
                            if (!newImages.isEmpty()) {
                                showImageUploadDialog();
                                uploadNewImagesSequentially(newImages, 0);
                            } else {
                                Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show();
                                finishWithSuccess();
                            }
                        } else {
                            String error = obj.optString("error", "Failed to delete images");
                            System.out.println("DEBUG: PHP deletion failed: " + error);
                            Toast.makeText(this, "Error deleting images: " + error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("ERROR: Exception parsing delete response: " + e.getMessage());
                        System.out.println("Raw response: " + response);
                        Toast.makeText(this, "Error parsing delete response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Error deleting images: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bh_id", String.valueOf(bhId));
                params.put("image_paths", imagePathsArray.toString());
                return params;
            }
        };
        
        Volley.newRequestQueue(this).add(request);
    }
    
    private void uploadNewImagesSequentially(ArrayList<Uri> newImages, int index) {
        if (index >= newImages.size()) {
            // All new images uploaded
            hideImageUploadDialog();
            Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show();
            finishWithSuccess();
            return;
        }
        
        // Update progress immediately when starting upload
        updateImageUploadProgress(index + 1, newImages.size());
        
        // Start upload immediately on UI thread for faster response
        runOnUiThread(() -> uploadSingleImage(newImages.get(index), index));
    }

    private void uploadImagesSequentially(int index) {

    
        if (index >= imageUris.size()) {
            // All images uploaded
            hideImageUploadDialog();
            Toast.makeText(this, "Boarding house and images updated successfully!", Toast.LENGTH_SHORT).show();
            finishWithSuccess();
            return;
        }

        if (imageUris.isEmpty()) {
            hideImageUploadDialog();
            Toast.makeText(this, "Boarding house updated successfully!", Toast.LENGTH_SHORT).show();
            finishWithSuccess();
            return;
        }

        // Update progress immediately
        updateImageUploadProgress(index + 1, imageUris.size());
        
        // Start upload immediately on UI thread for faster response
        Uri imageUri = imageUris.get(index);
        runOnUiThread(() -> uploadSingleImage(imageUri, index));
    }

    private void uploadSingleImage(Uri imageUri, int index) {
        String url = "https://boardease.calapebohol.com/upload_bh_image.php";
        
        // Optimized upload - removed debug logs for faster performance
        
        try {
            // Check if URI is valid
            if (imageUri == null) {
                Toast.makeText(this, "Image URI is null", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Open input stream with optimized error handling
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "Could not read image file", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Convert URI to byte array with optimized buffer size
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; // Larger buffer for faster reading
            int length;
            
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            
            byte[] imageData = byteArrayOutputStream.toByteArray();
            inputStream.close();
            byteArrayOutputStream.close();
            
            if (imageData.length == 0) {
                Toast.makeText(this, "Image file is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Convert to base64 with NO_WRAP for better performance
            String base64Image = android.util.Base64.encodeToString(imageData, android.util.Base64.NO_WRAP);
            
            // Create optimized request
            StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        
                        if (obj.has("success")) {
                            // Upload next image immediately
                            ArrayList<Uri> newImages = getNewImages();
                            if (!newImages.isEmpty()) {
                                uploadNewImagesSequentially(newImages, index + 1);
                            } else {
                                uploadImagesSequentially(index + 1);
                            }
                        } else {
                            String error = obj.optString("error", "Image upload failed");
                            Toast.makeText(this, "Image upload failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing image upload response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Image upload error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("bh_id", String.valueOf(bhId));
                    params.put("image_base64", base64Image);
                    return params;
                }
            };
            
            // Set timeout using RetryPolicy
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                8000, // 8 second timeout
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            // Use optimized request queue
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: Exception in uploadSingleImage: " + e.getMessage());
            Toast.makeText(this, "Error reading image file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void finishWithSuccess() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("bh_id", bhId);
        resultIntent.putExtra("updated", true);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    // Loading dialog methods
    private void showLoadingDialog(String message) {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setCancelable(false);
        }
        loadingDialog.setMessage(message);
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void showImageUploadDialog() {
        if (imageUploadDialog == null) {
            imageUploadDialog = new ProgressDialog(this);
            imageUploadDialog.setCancelable(false);
            imageUploadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        }
        imageUploadDialog.setMessage("Uploading new images...");
        imageUploadDialog.setMax(100);
        imageUploadDialog.setProgress(0);
        imageUploadDialog.show();
    }

    private void hideImageUploadDialog() {
        if (imageUploadDialog != null && imageUploadDialog.isShowing()) {
            imageUploadDialog.dismiss();
        }
    }

    private void updateImageUploadProgress(int current, int total) {
        if (imageUploadDialog != null && imageUploadDialog.isShowing()) {
            int progress = (current * 100) / total;
            imageUploadDialog.setProgress(progress);
            imageUploadDialog.setMessage("Uploading image " + current + " of " + total + " (" + progress + "%)");
        }
    }
    
    // Helper method to update image views visibility
    private void updateImageViewsVisibility() {
        if (imageUris.isEmpty()) {
            // No images - show placeholder, hide ViewPager and FAB
            ivPlaceholder.setVisibility(ImageView.VISIBLE);
            viewPagerImages.setVisibility(ViewPager2.GONE);
            fabAddImages.setVisibility(View.GONE);
        } else {
            // Has images - hide placeholder, show ViewPager and FAB
            ivPlaceholder.setVisibility(ImageView.GONE);
            viewPagerImages.setVisibility(ViewPager2.VISIBLE);
            fabAddImages.setVisibility(View.VISIBLE);
        }
    }
    
    // Method to handle image removal
    private void removeImage(int position) {
        System.out.println("DEBUG: Removing image at position: " + position);
        System.out.println("DEBUG: Before removal - imageUris size: " + imageUris.size());
        System.out.println("DEBUG: Before removal - originalImageUris size: " + originalImageUris.size());
        
        // Get the URI that's being removed for debugging
        if (position < imageUris.size()) {
            Uri removedUri = imageUris.get(position);
            System.out.println("DEBUG: Removing URI: " + removedUri.toString());
        }
        
        // Remove the image from the list
        imageUris.remove(position);
        
        System.out.println("DEBUG: After removal - imageUris size: " + imageUris.size());
        
        // Create a new adapter instance to force ViewPager2 refresh
        imageAdapter = new ImageAdapter(this, imageUris, this::removeImage);
        viewPagerImages.setAdapter(imageAdapter);
        
        // Update visibility
        updateImageViewsVisibility();
    }

    @Override
    public void onBackPressed() {
        // Check if there are unsaved changes
        if (hasUnsavedChanges()) {
            // Show confirmation dialog
            new android.app.AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Are you sure you want to go back?")
                .setPositiveButton("Discard", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_alert_white)
                .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideLoadingDialog();
        hideImageUploadDialog();
    }

    private void initializeAddressPicker() {
        // Initialize barangay spinner with default option
        String[] defaultBarangayArray = {"Select Barangay"};
        ArrayAdapter<String> defaultBarangayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, defaultBarangayArray) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                android.widget.TextView textView = (android.widget.TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(0xFF000000); // Black text color for selected item
                }
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof android.widget.TextView) {
                    android.widget.TextView textView = (android.widget.TextView) view;
                    textView.setTextColor(0xFFFFFFFF);
                    textView.setTextSize(16);
                    textView.setPadding(16, 16, 16, 16);
                    textView.setBackgroundColor(0xFF2C2C2C);
                } else {
                    android.widget.TextView textView = view.findViewById(android.R.id.text1);
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
        defaultBarangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangay.setAdapter(defaultBarangayAdapter);
        
        loadProvinces();
        
        spinnerProvince.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String newProvince = parent.getItemAtPosition(position).toString();
                    if (!newProvince.equals(selectedProvince)) {
                        clearMunicipalityAndBarangay();
                        selectedMunicipality = "";
                        selectedBarangay = "";
                        selectedProvince = newProvince;
                        loadMunicipalities(selectedProvince);
                    } else {
                        selectedProvince = newProvince;
                    }
                } else {
                    selectedProvince = "";
                    clearMunicipalityAndBarangay();
                    selectedMunicipality = "";
                    selectedBarangay = "";
                }
                updateCompleteAddress();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        spinnerMunicipality.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String newMunicipality = parent.getItemAtPosition(position).toString();
                    
                    // If municipality changed, reset barangay
                    if (!newMunicipality.equals(selectedMunicipality)) {
                        clearBarangay();
                        selectedBarangay = "";
                        
                        // Set new municipality and load barangays
                        selectedMunicipality = newMunicipality;
                        loadBarangays(selectedMunicipality);
                    } else {
                        // Same municipality selected, just update
                        selectedMunicipality = newMunicipality;
                    }
                } else {
                    // "Select Municipality" selected
                    selectedMunicipality = "";
                    clearBarangay();
                    selectedBarangay = "";
                }
                updateCompleteAddress();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Set up barangay selection listener
        spinnerBarangay.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select Barangay" option
                    selectedBarangay = parent.getItemAtPosition(position).toString();
                } else {
                    selectedBarangay = "";
                }
                updateCompleteAddress();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        etDetailedAddress.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                selectedDetailedAddress = s.toString().trim();
                updateCompleteAddress();
            }
        });
    }

    private void loadProvinces() {
        String url = "https://boardease.calapebohol.com/philippine_address_api.php?action=provinces";
        
        com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
            Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.getBoolean("success")) {
                        org.json.JSONArray provincesArray = response.getJSONArray("data");
                        
                        String[] provinceNames = new String[provincesArray.length() + 1];
                        provinceNames[0] = "Select Province";
                        
                        for (int i = 0; i < provincesArray.length(); i++) {
                            org.json.JSONObject province = provincesArray.getJSONObject(i);
                            provinceNames[i + 1] = province.getString("name");
                        }
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, provinceNames) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                android.widget.TextView textView = (android.widget.TextView) view;
                                textView.setTextColor(0xFF000000);
                                textView.setTextSize(16);
                                return view;
                            }

                            @Override
                            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                View view = super.getDropDownView(position, convertView, parent);
                                if (view instanceof android.widget.TextView) {
                                    android.widget.TextView textView = (android.widget.TextView) view;
                                    textView.setTextColor(0xFFFFFFFF);
                                    textView.setTextSize(16);
                                    textView.setPadding(16, 16, 16, 16);
                                    textView.setBackgroundColor(0xFF2C2C2C);
                                } else {
                                    android.widget.TextView textView = view.findViewById(android.R.id.text1);
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
                        spinnerProvince.setAdapter(adapter);
                        
                        if (!pendingAddressToParse.isEmpty()) {
                            parseAndPopulateAddress(pendingAddressToParse);
                            pendingAddressToParse = "";
                        }
                    } else {
                        loadProvincesFallback();
                    }
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    loadProvincesFallback();
                }
            },
            error -> {
                Log.e(TAG, "Network error loading provinces: " + error.getMessage());
                loadProvincesFallback();
            }
        );
        
        Volley.newRequestQueue(this).add(request);
    }
    
    private void loadProvincesFallback() {
        String[] fallbackProvinces = {
            "Select Province", "Abra", "Agusan del Norte", "Agusan del Sur", "Aklan", "Albay", "Antique", "Apayao", "Aurora",
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
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fallbackProvinces) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                android.widget.TextView textView = (android.widget.TextView) view;
                textView.setTextColor(0xFF000000);
                textView.setTextSize(16);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof android.widget.TextView) {
                    android.widget.TextView textView = (android.widget.TextView) view;
                    textView.setTextColor(0xFFFFFFFF);
                    textView.setTextSize(16);
                    textView.setPadding(16, 16, 16, 16);
                    textView.setBackgroundColor(0xFF2C2C2C);
                } else {
                    android.widget.TextView textView = view.findViewById(android.R.id.text1);
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
        spinnerProvince.setAdapter(adapter);
        
        if (!pendingAddressToParse.isEmpty()) {
            parseAndPopulateAddress(pendingAddressToParse);
            pendingAddressToParse = "";
        }
    }
    
    private void loadMunicipalities(String province) {
        loadMunicipalitiesWithCallback(province, null);
    }
    
    private void loadMunicipalitiesWithCallback(String province, Runnable callback) {
        try {
            String url = "https://boardease.calapebohol.com/philippine_address_api.php?action=municipalities&province_name=" + 
                java.net.URLEncoder.encode(province, "UTF-8");
            
            com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            org.json.JSONArray municipalitiesArray = response.getJSONArray("data");
                            
                            if (municipalitiesArray.length() > 0) {
                                String[] municipalityNames = new String[municipalitiesArray.length() + 1];
                                municipalityNames[0] = "Select Municipality";
                                
                                for (int i = 0; i < municipalitiesArray.length(); i++) {
                                    org.json.JSONObject municipality = municipalitiesArray.getJSONObject(i);
                                    municipalityNames[i + 1] = municipality.getString("name");
                                }
                                
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, municipalityNames) {
                                    @Override
                                    public View getView(int position, View convertView, ViewGroup parent) {
                                        View view = super.getView(position, convertView, parent);
                                        android.widget.TextView textView = (android.widget.TextView) view;
                                        textView.setTextColor(0xFF000000);
                                        textView.setTextSize(16);
                                        return view;
                                    }

                                    @Override
                                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                        View view = super.getDropDownView(position, convertView, parent);
                                        if (view instanceof android.widget.TextView) {
                                            android.widget.TextView textView = (android.widget.TextView) view;
                                            textView.setTextColor(0xFFFFFFFF);
                                            textView.setTextSize(16);
                                            textView.setPadding(16, 16, 16, 16);
                                            textView.setBackgroundColor(0xFF2C2C2C);
                                        } else {
                                            android.widget.TextView textView = view.findViewById(android.R.id.text1);
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
                                spinnerMunicipality.setAdapter(adapter);
                                
                                if (callback != null) {
                                    runOnUiThread(callback);
                                }
                            }
                        }
                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Error loading municipalities: " + error.getMessage());
                }
            );
            
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            Log.e(TAG, "Error encoding province name: " + e.getMessage());
        }
    }
    
    private void loadBarangays(String municipality) {
        loadBarangaysWithCallback(municipality, null);
    }
    
    private void loadBarangaysWithCallback(String municipality, Runnable callback) {
        try {
            String url = "https://boardease.calapebohol.com/philippine_address_api.php?action=barangays&municipality_name=" + 
                java.net.URLEncoder.encode(municipality, "UTF-8");
            
            com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            org.json.JSONArray barangaysArray = response.getJSONArray("data");
                            
                            if (barangaysArray.length() > 0) {
                                String[] barangayNames = new String[barangaysArray.length() + 1];
                                barangayNames[0] = "Select Barangay";
                                
                                for (int i = 0; i < barangaysArray.length(); i++) {
                                    org.json.JSONObject barangay = barangaysArray.getJSONObject(i);
                                    barangayNames[i + 1] = barangay.getString("name");
                                }
                                
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, barangayNames) {
                                    @Override
                                    public View getView(int position, View convertView, ViewGroup parent) {
                                        View view = super.getView(position, convertView, parent);
                                        android.widget.TextView textView = (android.widget.TextView) view;
                                        textView.setTextColor(0xFF000000);
                                        textView.setTextSize(16);
                                        return view;
                                    }

                                    @Override
                                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                        View view = super.getDropDownView(position, convertView, parent);
                                        if (view instanceof android.widget.TextView) {
                                            android.widget.TextView textView = (android.widget.TextView) view;
                                            textView.setTextColor(0xFFFFFFFF);
                                            textView.setTextSize(16);
                                            textView.setPadding(16, 16, 16, 16);
                                            textView.setBackgroundColor(0xFF2C2C2C);
                                        } else {
                                            android.widget.TextView textView = view.findViewById(android.R.id.text1);
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
                                spinnerBarangay.setAdapter(adapter);
                                
                                if (callback != null) {
                                    runOnUiThread(callback);
                                }
                            }
                        }
                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Error loading barangays: " + error.getMessage());
                }
            );
            
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            Log.e(TAG, "Error encoding municipality name: " + e.getMessage());
        }
    }
    
    private void clearMunicipalityAndBarangay() {
        String[] emptyArray = {"Select Municipality"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, emptyArray) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                android.widget.TextView textView = (android.widget.TextView) view;
                textView.setTextColor(0xFF000000);
                textView.setTextSize(16);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof android.widget.TextView) {
                    android.widget.TextView textView = (android.widget.TextView) view;
                    textView.setTextColor(0xFFFFFFFF);
                    textView.setTextSize(16);
                    textView.setPadding(16, 16, 16, 16);
                    textView.setBackgroundColor(0xFF2C2C2C);
                } else {
                    android.widget.TextView textView = view.findViewById(android.R.id.text1);
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
        spinnerMunicipality.setAdapter(adapter);
        clearBarangay();
    }
    
    private void clearBarangay() {
        String[] emptyArray = {"Select Barangay"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, emptyArray) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                android.widget.TextView textView = (android.widget.TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(0xFF000000);
                }
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof android.widget.TextView) {
                    android.widget.TextView textView = (android.widget.TextView) view;
                    textView.setTextColor(0xFFFFFFFF);
                    textView.setTextSize(16);
                    textView.setPadding(16, 16, 16, 16);
                    textView.setBackgroundColor(0xFF2C2C2C);
                } else {
                    android.widget.TextView textView = view.findViewById(android.R.id.text1);
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
        if (spinnerBarangay != null) {
            spinnerBarangay.setAdapter(adapter);
        }
        selectedBarangay = "";
    }
    
    private void updateCompleteAddress() {
        StringBuilder completeAddress = new StringBuilder();
        
        if (!selectedDetailedAddress.isEmpty()) {
            completeAddress.append(selectedDetailedAddress);
        }
        
        if (!selectedBarangay.isEmpty()) {
            if (completeAddress.length() > 0) completeAddress.append(", ");
            completeAddress.append(selectedBarangay);
        }
        
        if (!selectedMunicipality.isEmpty()) {
            if (completeAddress.length() > 0) completeAddress.append(", ");
            completeAddress.append(selectedMunicipality);
        }
        
        if (!selectedProvince.isEmpty()) {
            if (completeAddress.length() > 0) completeAddress.append(", ");
            completeAddress.append(selectedProvince);
        }
        
        etBhAddress.setText(completeAddress.toString());
    }
    
    private void parseAndPopulateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return;
        }
        
        android.widget.SpinnerAdapter adapter = spinnerProvince.getAdapter();
        if (!(adapter instanceof ArrayAdapter) || adapter == null || adapter.getCount() == 0) {
            pendingAddressToParse = address;
            return;
        }

        @SuppressWarnings("unchecked")
        ArrayAdapter<String> provinceAdapter = (ArrayAdapter<String>) adapter;
        
        String[] parts = address.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        
        String foundProvince = null;
        int provinceIndex = -1;
        
        for (int i = parts.length - 1; i >= 0; i--) {
            String potentialProvince = findProvinceInAddress(parts[i], provinceAdapter);
            if (potentialProvince != null) {
                foundProvince = potentialProvince;
                provinceIndex = i;
                break;
            }
        }
        
        if (foundProvince != null && provinceIndex >= 0) {
            selectedProvince = foundProvince;
            
            if (provinceIndex == parts.length - 1) {
                if (parts.length >= 4) {
                    selectedDetailedAddress = parts[0];
                    selectedBarangay = parts[1];
                    selectedMunicipality = parts[2];
                } else if (parts.length == 3) {
                    selectedBarangay = parts[0];
                    selectedMunicipality = parts[1];
                } else if (parts.length == 2) {
                    selectedMunicipality = parts[0];
                }
            } else {
                StringBuilder detailedAddr = new StringBuilder();
                for (int i = 0; i < provinceIndex; i++) {
                    if (i > 0) detailedAddr.append(", ");
                    detailedAddr.append(parts[i]);
                }
                
                if (provinceIndex >= 3) {
                    selectedDetailedAddress = parts[0];
                    selectedBarangay = parts[1];
                    selectedMunicipality = parts[2];
                } else if (provinceIndex == 2) {
                    selectedBarangay = parts[0];
                    selectedMunicipality = parts[1];
                } else if (provinceIndex == 1) {
                    selectedMunicipality = parts[0];
                } else {
                    selectedDetailedAddress = detailedAddr.toString();
                }
            }
        } else {
            if (parts.length >= 4) {
                selectedDetailedAddress = parts[0];
                selectedBarangay = parts[1];
                selectedMunicipality = parts[2];
                selectedProvince = parts[3];
            } else if (parts.length == 3) {
                selectedBarangay = parts[0];
                selectedMunicipality = parts[1];
                selectedProvince = parts[2];
            } else if (parts.length == 2) {
                selectedMunicipality = parts[0];
                selectedProvince = parts[1];
            } else {
                String potentialProvince = findProvinceInAddress(parts[0], provinceAdapter);
                if (potentialProvince != null) {
                    selectedProvince = potentialProvince;
                } else {
                    selectedDetailedAddress = parts[0];
                }
            }
        }
        
        if (!selectedDetailedAddress.isEmpty()) {
            etDetailedAddress.setText(selectedDetailedAddress);
        }
        
        if (!selectedProvince.isEmpty()) {
            setProvinceSelection(selectedProvince);
        }
    }
    
    private String findProvinceInAddress(String text, ArrayAdapter<String> provinceAdapter) {
        if (provinceAdapter == null || text == null || text.trim().isEmpty()) {
            return null;
        }
        String cleanText = text.trim();
        for (int i = 1; i < provinceAdapter.getCount(); i++) {
            String province = provinceAdapter.getItem(i);
            if (province != null) {
                if (province.equalsIgnoreCase(cleanText) || cleanText.contains(province) || province.contains(cleanText)) {
                    return province;
                }
            }
        }
        return null;
    }
    
    private void setProvinceSelection(String provinceName) {
        android.widget.SpinnerAdapter spinnerAdapter = spinnerProvince.getAdapter();
        if (spinnerAdapter instanceof ArrayAdapter) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerAdapter;
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).equals(provinceName)) {
                    spinnerProvince.setSelection(i, false);
                    selectedProvince = provinceName;
                    loadMunicipalitiesWithCallback(selectedProvince, () -> setMunicipalitySelection(selectedMunicipality));
                    break;
                }
            }
        }
    }
    
    private void setMunicipalitySelection(String municipalityName) {
        android.widget.SpinnerAdapter spinnerAdapter = spinnerMunicipality.getAdapter();
        if (spinnerAdapter instanceof ArrayAdapter && !municipalityName.isEmpty()) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerAdapter;
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).equals(municipalityName)) {
                    spinnerMunicipality.setSelection(i, false);
                    selectedMunicipality = municipalityName;
                    loadBarangaysWithCallback(selectedMunicipality, () -> setBarangaySelection(selectedBarangay));
                    break;
                }
            }
        }
    }
    
    private void setBarangaySelection(String barangayName) {
        android.widget.SpinnerAdapter spinnerAdapter = spinnerBarangay.getAdapter();
        if (spinnerAdapter instanceof ArrayAdapter && !barangayName.isEmpty()) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerAdapter;
            for (int i = 0; i < adapter.getCount(); i++) {
                String item = adapter.getItem(i);
                if (item != null && item.equalsIgnoreCase(barangayName)) {
                    spinnerBarangay.setSelection(i, false);
                    selectedBarangay = item;
                    break;
                }
            }
        }
    }

    private void setupBathroomsStepper() {
        btnIncrementBathrooms.setOnClickListener(v -> {
            try {
                int current = Integer.parseInt(tvBathroomsCount.getText().toString());
                if (current < 99) {
                    tvBathroomsCount.setText(String.valueOf(current + 1));
                }
            } catch (NumberFormatException e) {
                tvBathroomsCount.setText("1");
            }
        });
        
        btnDecrementBathrooms.setOnClickListener(v -> {
            try {
                int current = Integer.parseInt(tvBathroomsCount.getText().toString());
                if (current > 1) {
                    tvBathroomsCount.setText(String.valueOf(current - 1));
                }
            } catch (NumberFormatException e) {
                tvBathroomsCount.setText("1");
            }
        });
    }

    /**
     * Verifies and adds image to the list if approved
     */
    private void verifyAndAddImage(Uri imageUri) {
        // Show loading message
        Toast.makeText(this, "Verifying image...", Toast.LENGTH_SHORT).show();
        
        // Use smart verification (API if available, local if not)
        ImageVerification.verifyImage(this, imageUri, new ImageVerification.VerificationCallback() {
            @Override
            public void onVerificationComplete(boolean isApproved, String reason) {
                if (isApproved) {
                    // Add image to list
                    if (!imageUris.contains(imageUri)) {
                        imageUris.add(imageUri);
                        // Create new adapter to ensure proper refresh
                        imageAdapter = new ImageAdapter(EditBoardingHouseActivity.this, imageUris, EditBoardingHouseActivity.this::removeImage);
                        viewPagerImages.setAdapter(imageAdapter);
                        updateImageViewsVisibility();
                        Toast.makeText(EditBoardingHouseActivity.this, "✅ Image approved and added", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Show rejection reason
                    Toast.makeText(EditBoardingHouseActivity.this, "❌ Image rejected: " + reason, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onVerificationError(String error) {
                Toast.makeText(EditBoardingHouseActivity.this, "Image verification failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

}
