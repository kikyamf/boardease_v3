package com.example.mock;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * BoarderProfileFragment - Profile page for boarders
 * Displays user information and provides access to various settings and features
 */
public class BoarderProfileFragment extends Fragment {

    private static final String TAG = "BoarderProfileFragment";
    private static final String BASE_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/";
    private static final String GET_BOARDER_INFO_URL = BASE_URL + "get_boarder_info.php";
    private static final String UPLOAD_PROFILE_PIC_URL = BASE_URL + "upload_profile_picture.php";

    // Views
    private ImageButton btnBack;
    private ImageView ivProfilePic;
    private ImageView ivEditProfile;
    private TextView tvBoarderName;
    private TextView tvBoarderEmail;
    private TextView tvSignOut;
    private LinearLayout layoutSignOut;
    private android.widget.Button btnLogout;
    
    // Image handling
    private ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private Uri cameraImageUri;
    private Uri selectedImageUri;
    private File cameraImageFile;
    private int userId;

    // Menu Items
    private LinearLayout layoutAccountSettings;
    private LinearLayout layoutPaymentMethods;
    private LinearLayout layoutHelpSupport;
    private LinearLayout layoutAboutApp;
    
    // Pull-to-refresh
    private SwipeRefreshLayout swipeRefreshLayout;

    public BoarderProfileFragment() {
        // Required empty public constructor
    }

    public static BoarderProfileFragment newInstance() {
        return new BoarderProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boarder_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        initializeImageHandlers();
        setupClickListeners();
        loadUserData();
    }

    private void initializeViews(View view) {
        try {
            // Header views
            btnBack = view.findViewById(R.id.btnBack);
            
            // Profile views
            ivProfilePic = view.findViewById(R.id.ivProfilePic);
            ivEditProfile = view.findViewById(R.id.ivEditProfile);
            tvBoarderName = view.findViewById(R.id.tvBoarderName);
            tvBoarderEmail = view.findViewById(R.id.tvBoarderEmail);
            
            // Menu items
            layoutAccountSettings = view.findViewById(R.id.layoutAccountSettings);
            layoutPaymentMethods = view.findViewById(R.id.layoutPaymentMethods);
            layoutHelpSupport = view.findViewById(R.id.layoutHelpSupport);
            layoutAboutApp = view.findViewById(R.id.layoutAboutApp);
            
            // Sign out
            tvSignOut = view.findViewById(R.id.tvSignOut);
            layoutSignOut = view.findViewById(R.id.layoutSignOut);
            
            // Pull-to-refresh
            swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
            
            // Set up pull-to-refresh listener
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setOnRefreshListener(() -> {
                    loadUserData();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void initializeImageHandlers() {
        // Multiple permissions request launcher
        requestMultiplePermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Boolean isGranted : permissions.values()) {
                        if (!isGranted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        showImageSourceDialog();
                    } else {
                        Toast.makeText(getContext(), "Permission denied. Cannot access camera or gallery.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        try {
                            // Set the selected image URI from camera
                            selectedImageUri = cameraImageUri;
                            
                            // Load the captured image to preview
                            Bitmap bitmap = BitmapFactory.decodeFile(cameraImageFile.getAbsolutePath());
                            if (bitmap != null) {
                                ivProfilePic.setImageBitmap(bitmap);
                                // Upload the image to database
                                uploadProfilePicture();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error loading image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            // Set the selected image URI from gallery
                            selectedImageUri = uri;
                            
                            // Load the image to preview
                            Bitmap bitmap = getBitmapFromUri(uri);
                            if (bitmap != null) {
                                ivProfilePic.setImageBitmap(bitmap);
                                // Upload the image to database
                                uploadProfilePicture();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error loading image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }
    
    private void setupClickListeners() {
        try {
            // Back button
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    try {
                        // Navigate back or close profile
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Edit profile picture
            if (ivEditProfile != null) {
                ivEditProfile.setOnClickListener(v -> {
                    try {
                        openImageSelector();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            
            // Profile picture click
            if (ivProfilePic != null) {
                ivProfilePic.setOnClickListener(v -> {
                    try {
                        openImageSelector();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Account Settings
            if (layoutAccountSettings != null) {
                layoutAccountSettings.setOnClickListener(v -> {
                    try {
                        // Navigate to account settings activity
                        Intent intent = new Intent(getActivity(), BoarderAccountSettingsActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error opening account settings", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Gcash Information (Payment Methods)
            if (layoutPaymentMethods != null) {
                layoutPaymentMethods.setOnClickListener(v -> {
                    try {
                        // Navigate to GcashInfoActivity
                        String userIdString = Login.getCurrentUserId(getContext());
                        if (userIdString == null || userIdString.isEmpty()) {
                            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Parse user ID to int
                        int userId;
                        try {
                            userId = Integer.parseInt(userIdString);
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Invalid user ID", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        Intent intent = new Intent(getActivity(), GcashInfoActivity.class);
                        intent.putExtra("user_id", userId);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error opening Gcash Information", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Help & Support
            if (layoutHelpSupport != null) {
                layoutHelpSupport.setOnClickListener(v -> {
                    try {
                        Toast.makeText(getContext(), "Help & Support - Coming Soon!", Toast.LENGTH_SHORT).show();
                        // TODO: Navigate to help & support
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // About App
            if (layoutAboutApp != null) {
                layoutAboutApp.setOnClickListener(v -> {
                    try {
                        showAboutAppDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Sign Out - Both text and button trigger logout
            if (layoutSignOut != null) {
                layoutSignOut.setOnClickListener(v -> {
                    try {
                        showSignOutConfirmationDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            
            if (btnLogout != null) {
                btnLogout.setOnClickListener(v -> {
                    try {
                        showSignOutConfirmationDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserData() {
        try {
            // Get user ID
            String userIdString = Login.getCurrentUserId(getContext());
            if (userIdString == null || userIdString.isEmpty()) {
                loadUserDataFromSharedPreferences();
                return;
            }
            
            try {
                userId = Integer.parseInt(userIdString);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid user ID: " + userIdString);
                loadUserDataFromSharedPreferences();
                return;
            }
            
            // Load user data from API to get profile picture
            loadBoarderInfoFromAPI();
            
        } catch (Exception e) {
            e.printStackTrace();
            loadUserDataFromSharedPreferences();
        }
    }
    
    private void loadUserDataFromSharedPreferences() {
        try {
            // Load user data from SharedPreferences
            String userName = Login.getCurrentUserName(getContext());
            String middleName = Login.getCurrentUserMiddleName(getContext());
            String suffix = Login.getCurrentUserSuffix(getContext());
            String userEmail = Login.getCurrentUserEmail(getContext());
            
            // Build full name properly, handling null/empty middle name
            if (tvBoarderName != null) {
                String fullName = buildFullName(userName, middleName, suffix);
                if (fullName != null && !fullName.isEmpty()) {
                    tvBoarderName.setText(fullName);
                } else {
                    tvBoarderName.setText("User Name"); // Fallback
                }
            }
            
            if (tvBoarderEmail != null) {
                if (userEmail != null && !userEmail.isEmpty()) {
                    tvBoarderEmail.setText(userEmail);
                } else {
                    tvBoarderEmail.setText("user@email.com"); // Fallback
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to mock data if there's an error
            if (tvBoarderName != null) {
                tvBoarderName.setText("User Name");
            }
            if (tvBoarderEmail != null) {
                tvBoarderEmail.setText("user@email.com");
            }
        } finally {
            // Stop refresh indicator
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }
    
    private void loadBoarderInfoFromAPI() {
        try {
            StringRequest request = new StringRequest(Request.Method.POST, GET_BOARDER_INFO_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                if (jsonResponse.getBoolean("success")) {
                                    JSONObject boarderData = jsonResponse.getJSONObject("boarder");
                                    
                                    // Load name and email
                                    String firstName = boarderData.optString("first_name", "");
                                    String middleName = boarderData.optString("middle_name", "");
                                    String lastName = boarderData.optString("last_name", "");
                                    String suffix = boarderData.optString("suffix", "");
                                    String email = boarderData.optString("email", "");
                                    
                                    // Build full name
                                    String fullName = firstName;
                                    if (middleName != null && !middleName.isEmpty() && !middleName.equalsIgnoreCase("null")) {
                                        fullName += " " + middleName;
                                    }
                                    fullName += " " + lastName;
                                    if (suffix != null && !suffix.isEmpty() && !suffix.equalsIgnoreCase("null") && !suffix.equalsIgnoreCase("none")) {
                                        fullName += " " + suffix;
                                    }
                                    
                                    if (tvBoarderName != null) {
                                        tvBoarderName.setText(fullName.trim());
                                    }
                                    
                                    if (tvBoarderEmail != null) {
                                        tvBoarderEmail.setText(email);
                                    }
                                    
                                    // Load profile picture
                                    String profilePicture = boarderData.optString("profile_picture", "");
                                    loadProfilePicture(profilePicture);
                                    
                                } else {
                                    loadUserDataFromSharedPreferences();
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing boarder info response", e);
                                loadUserDataFromSharedPreferences();
                            } finally {
                                if (swipeRefreshLayout != null) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Error loading boarder info", error);
                            loadUserDataFromSharedPreferences();
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
            
            RequestQueue queue = Volley.newRequestQueue(getContext());
            queue.add(request);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading boarder info", e);
            loadUserDataFromSharedPreferences();
        }
    }
    
    private void loadProfilePicture(String profilePicturePath) {
        try {
            if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
                String fullImageUrl = BASE_URL + profilePicturePath;
                Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .centerCrop()
                    .circleCrop()
                    .into(ivProfilePic);
            } else {
                // Set default profile picture
                ivProfilePic.setImageResource(R.drawable.btn_profile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading profile picture", e);
            ivProfilePic.setImageResource(R.drawable.btn_profile);
        }
    }
    
    /**
     * Builds the full name from components, properly handling null/empty middle name
     * Removes "null" text from the name display and handles middle name properly
     * @param fullName The full name string (may contain "null" for middle name)
     * @param middleName The middle name separately (to check if it's null/empty)
     * @param suffix The suffix (Jr., Sr., etc.)
     * @return Properly formatted full name without "null" text
     */
    private String buildFullName(String fullName, String middleName, String suffix) {
        try {
            // If fullName is null or empty, return null
            if (fullName == null || fullName.trim().isEmpty()) {
                return null;
            }
            
            // Remove "null" (in any case) from the fullName string if it exists
            String cleanedName = fullName
                .replace(" null ", " ")  // Remove " null " with spaces
                .replace(" null", "")     // Remove " null" at end
                .replace("null ", "")     // Remove "null " at start
                .replace("null", "")      // Remove standalone "null"
                .replace("Null", "")      // Remove "Null"
                .replace("NULL", "");     // Remove "NULL"
            
            // Clean up any double spaces that might result from removing "null"
            cleanedName = cleanedName.replaceAll("\\s+", " ").trim();
            
            // If middleName is provided separately and is null/empty, ensure it's not in the name
            if (middleName == null || middleName.trim().isEmpty() || 
                middleName.equalsIgnoreCase("null") || middleName.equalsIgnoreCase("none")) {
                // Already handled above, but ensure no middle name appears
                cleanedName = cleanedName.replaceAll("\\s+", " ").trim();
            }
            
            // Add suffix if it exists and is not null/empty/none
            if (suffix != null && !suffix.trim().isEmpty() && 
                !suffix.equalsIgnoreCase("null") && !suffix.equalsIgnoreCase("none")) {
                cleanedName = cleanedName + " " + suffix.trim();
            }
            
            return cleanedName.trim();
        } catch (Exception e) {
            e.printStackTrace();
            // Return cleaned version if possible, otherwise original
            if (fullName != null) {
                return fullName.replace("null", "").replace("Null", "").replace("NULL", "").trim();
            }
            return fullName;
        }
    }

    private void showAboutAppDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("About BoardEase");
            builder.setMessage("BoardEase v1.0.0\n\n" +
                    "A comprehensive platform for finding and managing boarding house accommodations.\n\n" +
                    "Features:\n" +
                    "• Browse boarding houses\n" +
                    "• Book accommodations\n" +
                    "• Manage favorites\n" +
                    "• Track bookings\n\n" +
                    "© 2024 BoardEase. All rights reserved.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSignOutConfirmationDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Sign Out");
            builder.setMessage("Are you sure you want to sign out?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        // Use the Login.logout() method to properly sign out
                        Login.logout(getContext());
                        Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error signing out", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void openImageSelector() {
        // Check permissions first - need both CAMERA and storage permissions
        String storagePermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storagePermission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            storagePermission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        
        String cameraPermission = Manifest.permission.CAMERA;
        
        boolean hasStoragePermission = ContextCompat.checkSelfPermission(getContext(), storagePermission) == PackageManager.PERMISSION_GRANTED;
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(getContext(), cameraPermission) == PackageManager.PERMISSION_GRANTED;
        
        if (!hasStoragePermission || !hasCameraPermission) {
            // Request both permissions at once
            java.util.ArrayList<String> permissionsToRequest = new java.util.ArrayList<>();
            if (!hasStoragePermission) {
                permissionsToRequest.add(storagePermission);
            }
            if (!hasCameraPermission) {
                permissionsToRequest.add(cameraPermission);
            }
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            showImageSourceDialog();
        }
    }
    
    private void showImageSourceDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Select Profile Picture");
            builder.setMessage("Choose how you want to set your profile picture");
            
            builder.setPositiveButton("Camera", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openCamera();
                }
            });
            
            builder.setNegativeButton("Gallery", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openGallery();
                }
            });
            
            builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void openCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            
            // Create a file to store the image
            cameraImageFile = new File(getContext().getExternalFilesDir(null), "profile_pic_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", cameraImageFile);
            
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(cameraIntent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error opening camera", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openGallery() {
        try {
            galleryLauncher.launch("image/*");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error opening gallery", Toast.LENGTH_SHORT).show();
        }
    }
    
    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IOException("Cannot open input stream for URI: " + uri);
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (bitmap == null) {
                throw new IOException("Failed to decode bitmap from URI: " + uri);
            }
            
            return bitmap;
        } catch (Exception e) {
            throw new IOException("Error processing image: " + e.getMessage(), e);
        }
    }
    
    private void uploadProfilePicture() {
        if (selectedImageUri == null) {
            Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get user ID
        String userIdString = Login.getCurrentUserId(getContext());
        if (userIdString == null || userIdString.isEmpty()) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            userId = Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid user ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Uploading profile picture...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        try {
            Bitmap originalBitmap;
            
            // Handle different URI types
            if (selectedImageUri.getScheme().equals("file")) {
                // From camera
                originalBitmap = BitmapFactory.decodeFile(cameraImageFile.getAbsolutePath());
            } else {
                // From gallery
                originalBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), selectedImageUri);
            }
            
            if (originalBitmap == null) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Error loading image", Toast.LENGTH_SHORT).show();
                return;
            }
            
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
                Toast.makeText(getContext(), "Image is too large. Please select a smaller image.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_PROFILE_PIC_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            progressDialog.dismiss();
                            Log.d(TAG, "Upload response: " + response);
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                if (jsonResponse.getBoolean("success")) {
                                    String newProfilePicPath = jsonResponse.getString("profile_picture_path");
                                    // Reload profile picture
                                    loadProfilePicture(newProfilePicPath);
                                    Toast.makeText(getContext(), "Profile picture uploaded successfully!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), 
                                        "Failed to upload profile picture: " + jsonResponse.optString("error", "Unknown error"), 
                                        Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                progressDialog.dismiss();
                                Log.e(TAG, "Error parsing upload response", e);
                                Toast.makeText(getContext(), "Error uploading profile picture", Toast.LENGTH_SHORT).show();
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
                            
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
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
            
            RequestQueue queue = Volley.newRequestQueue(getContext());
            queue.add(request);
            
        } catch (IOException e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }
}
