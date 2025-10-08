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
    private LinearLayout layoutMyBookings;
    private LinearLayout layoutMyFavorites;
    private LinearLayout layoutPaymentMethods;
    private LinearLayout layoutNotifications;
    private LinearLayout layoutMessages;
    private LinearLayout layoutHelpSupport;
    private LinearLayout layoutAboutApp;

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
            layoutMyBookings = view.findViewById(R.id.layoutMyBookings);
            layoutMyFavorites = view.findViewById(R.id.layoutMyFavorites);
            layoutPaymentMethods = view.findViewById(R.id.layoutPaymentMethods);
            layoutNotifications = view.findViewById(R.id.layoutNotifications);
            layoutMessages = view.findViewById(R.id.layoutMessages);
            layoutHelpSupport = view.findViewById(R.id.layoutHelpSupport);
            layoutAboutApp = view.findViewById(R.id.layoutAboutApp);
            
            // Sign out
            tvSignOut = view.findViewById(R.id.tvSignOut);
            layoutSignOut = view.findViewById(R.id.layoutSignOut);
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
                        // Navigate to account settings fragment
                        if (getActivity() != null) {
                            BoarderAccountSettingsFragment accountSettingsFragment = BoarderAccountSettingsFragment.newInstance();
                            getActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, accountSettingsFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // My Bookings
            if (layoutMyBookings != null) {
                layoutMyBookings.setOnClickListener(v -> {
                    try {
                        // Navigate to bookings fragment
                        if (getActivity() instanceof BoarderDashboard) {
                            BoarderDashboard dashboard = (BoarderDashboard) getActivity();
                            dashboard.switchToTab(R.id.nav_activity);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // My Favorites
            if (layoutMyFavorites != null) {
                layoutMyFavorites.setOnClickListener(v -> {
                    try {
                        // Navigate to favorites fragment
                        if (getActivity() instanceof BoarderDashboard) {
                            BoarderDashboard dashboard = (BoarderDashboard) getActivity();
                            dashboard.switchToTab(R.id.nav_manage);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Payment Methods
            if (layoutPaymentMethods != null) {
                layoutPaymentMethods.setOnClickListener(v -> {
                    try {
                        Toast.makeText(getContext(), "Payment Methods - Coming Soon!", Toast.LENGTH_SHORT).show();
                        // TODO: Navigate to payment methods
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Notifications
            if (layoutNotifications != null) {
                layoutNotifications.setOnClickListener(v -> {
                    try {
                        Toast.makeText(getContext(), "Notifications - Coming Soon!", Toast.LENGTH_SHORT).show();
                        // TODO: Navigate to notifications settings
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Messages
            if (layoutMessages != null) {
                layoutMessages.setOnClickListener(v -> {
                    try {
                        Toast.makeText(getContext(), "Messages - Coming Soon!", Toast.LENGTH_SHORT).show();
                        // TODO: Navigate to messages
                    } catch (Exception e) {
                        e.printStackTrace();
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
            // Load user data from SharedPreferences
            String userName = Login.getCurrentUserName(getContext());
            String userEmail = Login.getCurrentUserEmail(getContext());
            
            if (tvBoarderName != null) {
                if (userName != null && !userName.isEmpty()) {
                    tvBoarderName.setText(userName);
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
}
