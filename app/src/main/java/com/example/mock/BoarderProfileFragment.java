package com.example.mock;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * BoarderProfileFragment - Profile page for boarders
 * Displays user information and provides access to various settings and features
 */
public class BoarderProfileFragment extends Fragment {

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
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private Uri cameraImageUri;
    private File cameraImageFile;

    // Menu Items
    private LinearLayout layoutAccountSettings;
    private LinearLayout layoutNotifications;
    private LinearLayout layoutMessages;
    private LinearLayout layoutPaymentHistory;
    private LinearLayout layoutAboutApp;
    
    // Pull-to-refresh
    private SwipeRefreshLayout swipeRefreshLayout;
    
    // API URL
    private static final String GET_BOARDER_PROFILE_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_owner_profile.php";

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
            layoutNotifications = view.findViewById(R.id.layoutNotifications);
            layoutMessages = view.findViewById(R.id.layoutMessages);
            layoutPaymentHistory = view.findViewById(R.id.layoutPaymentHistory);
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
        // Permission request launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
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
                            // Load the captured image
                            Bitmap bitmap = BitmapFactory.decodeFile(cameraImageFile.getAbsolutePath());
                            if (bitmap != null) {
                                ivProfilePic.setImageBitmap(bitmap);
                                Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
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
                            Bitmap bitmap = getBitmapFromUri(uri);
                            if (bitmap != null) {
                                ivProfilePic.setImageBitmap(bitmap);
                                Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
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

            // Edit profile picture - opens edit profile activity (like owner side)
            if (ivEditProfile != null) {
                ivEditProfile.setOnClickListener(v -> {
                    try {
                        openEditProfile();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            
            // Profile picture click - opens edit profile activity (like owner side)
            if (ivProfilePic != null) {
                ivProfilePic.setOnClickListener(v -> {
                    try {
                        openEditProfile();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Account Settings - opens account settings with email/password change (like owner side)
            if (layoutAccountSettings != null) {
                layoutAccountSettings.setOnClickListener(v -> {
                    try {
                        openAccountSettings();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error opening account settings", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Notifications
            if (layoutNotifications != null) {
                layoutNotifications.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(getActivity(), Notification.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error opening notifications", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Messages
            if (layoutMessages != null) {
                layoutMessages.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(getActivity(), Messages.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error opening messages", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Payment History
            if (layoutPaymentHistory != null) {
                layoutPaymentHistory.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(getActivity(), PaymentHistoryActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error opening payment history", Toast.LENGTH_SHORT).show();
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

    private void openEditProfile() {
        try {
            String userIdString = Login.getCurrentUserId(getContext());
            if (userIdString == null || userIdString.isEmpty()) {
                Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            
            int userId;
            try {
                userId = Integer.parseInt(userIdString);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid user ID", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent(getActivity(), BoarderAccountSettingsActivity.class);
            intent.putExtra("user_id", userId);
            startActivityForResult(intent, 100); // Use request code 100 for profile edit
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error opening edit profile", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openAccountSettings() {
        try {
            String userIdString = Login.getCurrentUserId(getContext());
            if (userIdString == null || userIdString.isEmpty()) {
                Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            
            int userId;
            try {
                userId = Integer.parseInt(userIdString);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid user ID", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent(getActivity(), AccountSettingsActivity.class);
            intent.putExtra("user_id", userId);
            startActivityForResult(intent, 200); // Use request code 200 for account settings
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error opening account settings", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadUserData() {
        // Get user ID
        String userIdString = Login.getCurrentUserId(getContext());
        if (userIdString == null || userIdString.isEmpty()) {
            // Fallback to SharedPreferences data
            loadUserDataFromSharedPreferences();
            return;
        }
        
        int userId;
        try {
            userId = Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            // Fallback to SharedPreferences data
            loadUserDataFromSharedPreferences();
            return;
        }
        
        // Load profile data from server
        StringRequest request = new StringRequest(Request.Method.POST, GET_BOARDER_PROFILE_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // Check if fragment is still attached before processing response
                    if (!isAdded() || getContext() == null) {
                        return;
                    }
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            populateProfileData(jsonResponse);
                        } else {
                            // Fallback to SharedPreferences data
                            loadUserDataFromSharedPreferences();
                        }
                    } catch (JSONException e) {
                        android.util.Log.e("BoarderProfile", "Error parsing profile response", e);
                        // Fallback to SharedPreferences data
                        loadUserDataFromSharedPreferences();
                    } finally {
                        // Stop refresh indicator
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    android.util.Log.e("BoarderProfile", "Error loading profile", error);
                    // Fallback to SharedPreferences data
                    loadUserDataFromSharedPreferences();
                    // Stop refresh indicator
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", userIdString);
                return params;
            }
        };
        
        RequestQueue queue = Volley.newRequestQueue(getContext());
        queue.add(request);
    }
    
    private void populateProfileData(JSONObject profileData) {
        try {
            // Set name
            String firstName = profileData.optString("f_name", "");
            String middleName = profileData.optString("m_name", "");
            String lastName = profileData.optString("l_name", "");
            String suffix = profileData.optString("suffix", "");
            
            String fullName = firstName;
            if (!middleName.isEmpty()) {
                fullName += " " + middleName;
            }
            fullName += " " + lastName;
            if (suffix != null && !suffix.isEmpty() && !suffix.equals("None")) {
                fullName += " " + suffix;
            }
            
            if (isAdded() && tvBoarderName != null) {
                tvBoarderName.setText(fullName.trim());
            }
            
            // Set email
            String email = profileData.optString("email", "");
            if (isAdded() && tvBoarderEmail != null) {
                tvBoarderEmail.setText(email);
            }
            
            // Load profile picture
            String profilePicPath = profileData.optString("profile_picture", "");
            if (!profilePicPath.isEmpty()) {
                String fullImageUrl = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/" + profilePicPath;
                // Check if fragment is still attached before loading image
                if (isAdded() && getContext() != null && ivProfilePic != null) {
                    Glide.with(requireContext())
                        .load(fullImageUrl)
                        .placeholder(R.drawable.btn_profile)
                        .error(R.drawable.btn_profile)
                        .centerCrop()
                        .into(ivProfilePic);
                }
            } else {
                // Set default profile picture
                if (isAdded() && ivProfilePic != null) {
                    ivProfilePic.setImageResource(R.drawable.btn_profile);
                }
            }
            
        } catch (Exception e) {
            android.util.Log.e("BoarderProfile", "Error populating profile data", e);
            // Fallback to SharedPreferences data
            loadUserDataFromSharedPreferences();
        }
    }
    
    private void loadUserDataFromSharedPreferences() {
        try {
            // Load user data from SharedPreferences as fallback
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
            
            // Set default profile picture if not loaded from server
            if (ivProfilePic != null) {
                ivProfilePic.setImageResource(R.drawable.btn_profile);
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
            if (ivProfilePic != null) {
                ivProfilePic.setImageResource(R.drawable.btn_profile);
            }
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
        // Check permissions first
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission);
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
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Check if the result is from BoarderAccountSettingsActivity or AccountSettingsActivity
        if ((requestCode == 100 || requestCode == 200) && resultCode == getActivity().RESULT_OK) {
            // Profile or account settings was updated successfully, refresh the data
            loadUserData();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile data when fragment becomes visible
        // This ensures any changes made in other activities are reflected
        loadUserData();
    }
    
    /**
     * Public method to refresh profile data
     * Can be called from parent activity when needed
     */
    public void refreshProfile() {
        loadUserData();
    }
}
