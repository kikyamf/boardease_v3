package com.example.mock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditBoardingHouseActivity extends AppCompatActivity {

    private EditText etBhName, etBhAddress, etBhDescription, etBhRules, etBathrooms, etArea, etBuildYear;
    private ViewPager2 viewPagerImages;
    private ImageView ivPlaceholder;

    private ArrayList<Uri> imageUris = new ArrayList<>();
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
        etBathrooms = findViewById(R.id.etBathrooms);
        etArea = findViewById(R.id.etArea);
        etBuildYear = findViewById(R.id.etBuildYear);
        viewPagerImages = findViewById(R.id.viewPagerImages);
        ivPlaceholder = findViewById(R.id.ivPlaceholder);

        // Get data from intent
        Intent intent = getIntent();
        bhId = intent.getIntExtra("bh_id", -1);

        // Set basic data from intent first
        etBhName.setText(intent.getStringExtra("bh_name"));
        String imagePath = intent.getStringExtra("image_path");
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                Uri uri = Uri.parse(imagePath);
                imageUris.add(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Try to fetch complete data from server
        // Show loading while fetching data
        showLoadingDialog("Loading boarding house details...");
        fetchBoardingHouseDetails();


        // Setup adapter
        imageAdapter = new ImageAdapter(this, imageUris, position -> {
            imageUris.remove(position);
            imageAdapter.notifyDataSetChanged();
            if (imageUris.isEmpty()) {
                ivPlaceholder.setVisibility(ImageView.VISIBLE);
                viewPagerImages.setVisibility(ViewPager2.GONE);
            }
        });
        viewPagerImages.setAdapter(imageAdapter);

        if (!imageUris.isEmpty()) {
            ivPlaceholder.setVisibility(ImageView.GONE);
            viewPagerImages.setVisibility(ViewPager2.VISIBLE);
        }

        // Placeholder click = pick new images
        ivPlaceholder.setOnClickListener(v -> pickImages());

        // Save Changes button
        findViewById(R.id.btnSaveChanges).setOnClickListener(v -> saveChanges());
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, 2001);
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
                    if (!imageUris.contains(uri)) imageUris.add(uri);
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if (!imageUris.contains(uri)) imageUris.add(uri);
            }
            imageAdapter.notifyDataSetChanged();
            ivPlaceholder.setVisibility(ImageView.GONE);
            viewPagerImages.setVisibility(ViewPager2.VISIBLE);
        }
    }

    private void saveChanges() {
        String name = etBhName.getText().toString().trim();
        String address = etBhAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading dialog
        showLoadingDialog("Updating boarding house...");

        // Update boarding house details on server
        updateBoardingHouseOnServer();
    }

    private void updateBoardingHouseOnServer() {
        String url = "http://192.168.101.6/BoardEase2/update_boarding_houses.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    hideLoadingDialog();

                    if (response.contains("success")) {
                        // Success - handle images or finish
                        if (!imageUris.isEmpty()) {
                            showImageUploadDialog();
                            uploadImagesSequentially(0);
                        } else {
                            Toast.makeText(this, "Boarding house updated successfully!", Toast.LENGTH_SHORT).show();
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
                params.put("number_of_bathroom", etBathrooms.getText().toString().trim());
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
        String url = "http://192.168.101.6/BoardEase2/get_boarding_houses.php";

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
                        etBhAddress.setText(obj.optString("bh_address", ""));
                        etBhDescription.setText(obj.optString("bh_description", ""));
                        etBhRules.setText(obj.optString("bh_rules", ""));
                        etBathrooms.setText(obj.optString("number_of_bathroom", ""));
                        etArea.setText(obj.optString("area", ""));
                        etBuildYear.setText(obj.optString("build_year", ""));

                        // Handle images
                        if (obj.has("images") && !obj.isNull("images")) {
                            JSONArray imagesArray = obj.getJSONArray("images");
                            for (int i = 0; i < imagesArray.length(); i++) {
                                String imagePath = imagesArray.getString(i);
                                if (imagePath != null && !imagePath.isEmpty()) {
                                    try {
                                        Uri uri = Uri.parse(imagePath);
                                        imageUris.add(uri);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                        // Update UI
                        imageAdapter.notifyDataSetChanged();
                        if (!imageUris.isEmpty()) {
                            ivPlaceholder.setVisibility(ImageView.GONE);
                            viewPagerImages.setVisibility(ViewPager2.VISIBLE);
                        }

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

        // Update progress
        updateImageUploadProgress(index + 1, imageUris.size());

        Uri imageUri = imageUris.get(index);
        uploadSingleImage(imageUri, index);
    }

    private void uploadSingleImage(Uri imageUri, int index) {
        String url = "http://192.168.101.6/BoardEase2/upload_bh_image.php";

        System.out.println("=== IMAGE UPLOAD DEBUG START ===");
        System.out.println("Image URI: " + imageUri.toString());
        System.out.println("Image index: " + index);
        System.out.println("Total images: " + imageUris.size());

        try {
            // Check if URI is valid
            if (imageUri == null) {
                System.out.println("ERROR: Image URI is null");
                Toast.makeText(this, "Image URI is null", Toast.LENGTH_SHORT).show();
                return;
            }

            // Try to open input stream
            InputStream inputStream = null;
            try {
                inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    System.out.println("ERROR: Could not open input stream for URI: " + imageUri);
                    Toast.makeText(this, "Could not read image file", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (Exception e) {
                System.out.println("ERROR: Exception opening input stream: " + e.getMessage());
                Toast.makeText(this, "Error opening image file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert URI to byte array
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            int totalBytes = 0;

            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
                totalBytes += length;
            }

            byte[] imageData = byteArrayOutputStream.toByteArray();
            inputStream.close();
            byteArrayOutputStream.close();

            System.out.println("Image data size: " + imageData.length + " bytes");
            System.out.println("Total bytes read: " + totalBytes);

            if (imageData.length == 0) {
                System.out.println("ERROR: Image data is empty");
                Toast.makeText(this, "Image file is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert to base64
            String base64Image = android.util.Base64.encodeToString(imageData, android.util.Base64.DEFAULT);
            System.out.println("Base64 length: " + base64Image.length());
            System.out.println("=== IMAGE UPLOAD DEBUG END ===");

            // Create string request with base64 data
            StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        System.out.println("Image upload response: " + response);
                        JSONObject obj = new JSONObject(response);

                        if (obj.has("success")) {
                            // Upload next image
                            uploadImagesSequentially(index + 1);
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

            Volley.newRequestQueue(this).add(request);

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
        imageUploadDialog.setMessage("Uploading images...");
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
            imageUploadDialog.setMessage("Uploading images... " + current + "/" + total);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideLoadingDialog();
        hideImageUploadDialog();
    }

}
