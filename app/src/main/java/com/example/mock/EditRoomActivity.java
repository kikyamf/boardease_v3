package com.example.mock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditRoomActivity extends AppCompatActivity {

    private static final int IMAGE_PICK_CODE = 1001;
    private static final String UPDATE_ROOM_URL = "http://192.168.101.6/BoardEase2/update_room.php";
    private static final String UPLOAD_ROOM_IMAGE_URL = "http://192.168.101.6/BoardEase2/upload_room_image.php";

    // Room data
    private int roomId;
    private int bhId;
    private String category;
    private String originalTitle;
    private String originalDescription;
    private String originalPrice;
    private String originalCapacity;
    private String originalTotalRooms;

    // UI elements
    private EditText etTitle, etDescription, etPrice, etCapacity, etTotalRooms;
    private ViewPager2 viewPagerImages;
    private ImageView ivPlaceholder;
    private Button btnSaveChanges, btnAddImages;
    private ImageView ivBack;
    private TextView tvTitle;

    // Image handling
    private ArrayList<Uri> imageUris = new ArrayList<>();
    private ArrayList<Uri> originalImageUris = new ArrayList<>();
    private ArrayList<String> removedImagePaths = new ArrayList<>();
    private HashMap<Uri, String> uriToPathMap = new HashMap<>();
    private ImageAdapter imageAdapter;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddImages;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_room);

        // Get room data from intent
        getRoomDataFromIntent();

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();

        // Populate fields with existing data
        populateFields();
    }

    private void getRoomDataFromIntent() {
        Intent intent = getIntent();
        roomId = intent.getIntExtra("room_id", -1);
        bhId = intent.getIntExtra("bh_id", -1);
        category = intent.getStringExtra("category");
        originalTitle = intent.getStringExtra("title");
        originalDescription = intent.getStringExtra("description");
        originalPrice = intent.getStringExtra("price");
        originalCapacity = intent.getStringExtra("capacity");
        originalTotalRooms = intent.getStringExtra("total_rooms");
        
        // Debug logging
        System.out.println("DEBUG: EditRoomActivity received data:");
        System.out.println("DEBUG: roomId = " + roomId);
        System.out.println("DEBUG: bhId = " + bhId);
        System.out.println("DEBUG: category = " + category);
        System.out.println("DEBUG: title = " + originalTitle);
        System.out.println("DEBUG: description = " + originalDescription);
        System.out.println("DEBUG: price = " + originalPrice);
        System.out.println("DEBUG: capacity = " + originalCapacity);
        System.out.println("DEBUG: total_rooms = " + originalTotalRooms);
    }

    private void initializeViews() {
        // Navigation
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);

        // Form fields
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etCapacity = findViewById(R.id.etCapacity);
        etTotalRooms = findViewById(R.id.etTotalRooms);

        // Images
        viewPagerImages = findViewById(R.id.viewPagerImages);
        ivPlaceholder = findViewById(R.id.ivPlaceholder);
        fabAddImages = findViewById(R.id.fabAddImages);

        // Buttons
        btnSaveChanges = findViewById(R.id.btnSaveChanges);

        // Set title
        tvTitle.setText("Edit " + category);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        fabAddImages.setOnClickListener(v -> pickImages());
        ivPlaceholder.setOnClickListener(v -> pickImages());
        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void populateFields() {
        etTitle.setText(originalTitle);
        etDescription.setText(originalDescription);
        etPrice.setText(originalPrice);
        etCapacity.setText(originalCapacity);
        etTotalRooms.setText(originalTotalRooms);

        // Initialize image adapter
        imageAdapter = new ImageAdapter(this, imageUris, this::removeImage);
        viewPagerImages.setAdapter(imageAdapter);
        
        // Fetch existing room images
        fetchRoomImages();
    }

    private void fetchRoomImages() {
        // Clear existing data
        imageUris.clear();
        originalImageUris.clear();
        uriToPathMap.clear();

        // Fetch room images from server
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, "http://192.168.101.6/BoardEase2/get_rooms.php",
                response -> {
                    try {
                        System.out.println("DEBUG: Room images response: " + response);
                        JSONObject obj = new JSONObject(response);
                        if (obj.getBoolean("success")) {
                            // Find the specific room and get its images
                            org.json.JSONArray rooms = obj.getJSONArray("rooms");
                            for (int i = 0; i < rooms.length(); i++) {
                                org.json.JSONObject room = rooms.getJSONObject(i);
                                if (room.getInt("bhr_id") == roomId) {
                                    // Found our room, get its images
                                    org.json.JSONArray images = room.getJSONArray("images");
                                    for (int j = 0; j < images.length(); j++) {
                                        String imagePath = images.getString(j);
                                        // Convert relative path to full URL
                                        String fullImageUrl = "http://192.168.101.6/BoardEase2/" + imagePath;
                                        Uri imageUri = Uri.parse(fullImageUrl);
                                        
                                        imageUris.add(imageUri);
                                        originalImageUris.add(imageUri);
                                        uriToPathMap.put(imageUri, imagePath);
                                    }
                                    break;
                                }
                            }
                            
                            // Update UI
                            imageAdapter = new ImageAdapter(this, imageUris, this::removeImage);
                            viewPagerImages.setAdapter(imageAdapter);
                            updateImageViewsVisibility();
                        } else {
                            System.out.println("DEBUG: Failed to fetch room images: " + obj.optString("error"));
                            updateImageViewsVisibility();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("DEBUG: Error parsing room images response: " + e.getMessage());
                        updateImageViewsVisibility();
                    }
                },
                error -> {
                    System.out.println("DEBUG: Network error fetching room images: " + error.getMessage());
                    updateImageViewsVisibility();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bh_id", String.valueOf(bhId));
                return params;
            }
        };
        queue.add(request);
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        if (!imageUris.contains(uri)) {
                            imageUris.add(uri);
                        }
                    }
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    if (!imageUris.contains(uri)) {
                        imageUris.add(uri);
                    }
                }

                // Recreate adapter to refresh ViewPager
                imageAdapter = new ImageAdapter(this, imageUris, this::removeImage);
                viewPagerImages.setAdapter(imageAdapter);
                updateImageViewsVisibility();

                Toast.makeText(this, "Image(s) added", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to add images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void removeImage(int position) {
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            // Recreate adapter to refresh ViewPager
            imageAdapter = new ImageAdapter(this, imageUris, this::removeImage);
            viewPagerImages.setAdapter(imageAdapter);
            updateImageViewsVisibility();
        }
    }

    private void updateImageViewsVisibility() {
        if (imageUris.isEmpty()) {
            ivPlaceholder.setVisibility(View.VISIBLE);
            viewPagerImages.setVisibility(View.GONE);
            fabAddImages.setVisibility(View.GONE);
        } else {
            ivPlaceholder.setVisibility(View.GONE);
            viewPagerImages.setVisibility(View.VISIBLE);
            fabAddImages.setVisibility(View.VISIBLE);
        }
    }

    private void saveChanges() {
        // Validate required fields
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String capacity = etCapacity.getText().toString().trim();
        String totalRooms = etTotalRooms.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(price) || 
            TextUtils.isEmpty(capacity) || TextUtils.isEmpty(totalRooms)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        showSaveConfirmationDialog(title, description, price, capacity, totalRooms);
    }

    private void showSaveConfirmationDialog(String title, String description, String price, String capacity, String totalRooms) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Changes");
        
        String message = "Are you sure you want to save these changes?\n\n" +
                        "Title: " + title + "\n" +
                        "Price: ₱" + price + "\n" +
                        "Capacity: " + capacity + " person(s)\n" +
                        "Total Units: " + totalRooms + "\n" +
                        "New Images: " + imageUris.size() + " image(s)";
        
        builder.setMessage(message);
        
        builder.setPositiveButton("Save", (dialog, which) -> {
            dialog.dismiss();
            updateRoomOnServer(title, description, price, capacity, totalRooms);
        });
        
        builder.setNegativeButton("Review", (dialog, which) -> {
            dialog.dismiss();
            // Stay on current screen for review
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateRoomOnServer(String title, String description, String price, String capacity, String totalRooms) {
        // Show loading dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving room changes...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, UPDATE_ROOM_URL,
                response -> {
                    try {
                        System.out.println("DEBUG: Update room response: " + response);
                        System.out.println("DEBUG: Response length: " + response.length());
                        
                        // Check if response is HTML (server error)
                        if (response.trim().startsWith("<")) {
                            Toast.makeText(this, "Server returned HTML. Check PHP errors.", Toast.LENGTH_LONG).show();
                            System.out.println("HTML Response: " + response);
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                            return;
                        }
                        
                        JSONObject obj = new JSONObject(response);
                        if (obj.has("success")) {
                            System.out.println("DEBUG: Room updated successfully");
                            
                            // Update progress dialog
                            if (progressDialog != null) {
                                progressDialog.setMessage("Uploading images...");
                            }
                            
                            // Upload new images if any
                            if (!imageUris.isEmpty()) {
                                uploadImagesSequentially(0);
                            } else {
                                completeUpdateProcess();
                            }
                        } else {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(this, "Error: " + obj.optString("error"), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                        System.out.println("DEBUG: Exception details: " + e.getMessage());
                        Toast.makeText(this, "Failed parsing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bhr_id", String.valueOf(roomId));
                params.put("bh_id", String.valueOf(bhId));
                params.put("category", category);
                params.put("title", title);
                params.put("room_description", description);
                params.put("price", price);
                params.put("capacity", capacity);
                params.put("total_rooms", totalRooms);
                return params;
            }
        };
        
        queue.add(request);
    }

    private void uploadImagesSequentially(int index) {
        if (index >= imageUris.size()) {
            completeUpdateProcess();
            return;
        }

        Uri uri = imageUris.get(index);
        System.out.println("DEBUG: Uploading room image " + (index + 1) + "/" + imageUris.size() + ": " + uri.toString());
        
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                System.out.println("ERROR: Could not open input stream for URI: " + uri.toString());
                uploadImagesSequentially(index + 1);
                return;
            }
            
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            String encoded = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);
            inputStream.close();

            if (encoded.isEmpty()) {
                System.out.println("ERROR: Encoded image is empty for URI: " + uri.toString());
                uploadImagesSequentially(index + 1);
                return;
            }

            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_ROOM_IMAGE_URL,
                    response -> {
                        System.out.println("DEBUG: Room image upload response: " + response);
                        uploadImagesSequentially(index + 1);
                    },
                    error -> {
                        System.out.println("ERROR: Room image upload failed: " + error.getMessage());
                        uploadImagesSequentially(index + 1);
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("bhr_id", String.valueOf(roomId));
                    params.put("image_base64", encoded);
                    return params;
                }
            };
            
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(8000, 1, 1.0f));
            queue.add(request);
            
        } catch (Exception e) {
            System.out.println("ERROR: Exception uploading room image: " + e.getMessage());
            e.printStackTrace();
            uploadImagesSequentially(index + 1);
        }
    }

    private void completeUpdateProcess() {
        // Dismiss progress dialog
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        
        // Show success message
        Toast.makeText(this, "Room updated successfully!", Toast.LENGTH_LONG).show();
        
        // Return to previous activity with success result
        Intent resultIntent = new Intent();
        resultIntent.putExtra("updated", true);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}








