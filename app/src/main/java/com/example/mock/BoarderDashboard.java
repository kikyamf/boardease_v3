package com.example.mock;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BoarderDashboard extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment currentFragment;
    private int userId;
    
    // Cache fragment instances to avoid recreating them
    private BoarderHomeFragment homeFragment;
    private ExploreFragment exploreFragment;
    private BoarderFavoriteFragment favoriteFragment;
    private BoarderBookingFragment bookingFragment;
    private BoarderProfileFragment profileFragment;
    
    // Notification permission launcher for Android 13+
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_boarder_dashboard);
        
        // Redirection check for hybrid soft registration
        showWelcomeDialogIfIncomplete();
        
        // Create notification channel early
        NotificationUtils.createNotificationChannel(this);
        
        // Setup notification permission launcher for Android 13+
        setupNotificationPermissionLauncher();
        
        // Request notification permission if needed (Android 13+)
        // requestNotificationPermissionIfNeeded(); // Moved to BoarderHomeFragment for sequential flow
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get user ID from SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userIdString = prefs.getString("user_id", "0");
        try {
            userId = Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            userId = 0;
        }
        
        // Initialize FCM token
        initializeFCMToken();
        
        // Try to restore fragments from FragmentManager first (they persist across configuration changes)
        homeFragment = (BoarderHomeFragment) getSupportFragmentManager().findFragmentByTag("home");
        exploreFragment = (ExploreFragment) getSupportFragmentManager().findFragmentByTag("explore");
        favoriteFragment = (BoarderFavoriteFragment) getSupportFragmentManager().findFragmentByTag("favorite");
        bookingFragment = (BoarderBookingFragment) getSupportFragmentManager().findFragmentByTag("booking");
        profileFragment = (BoarderProfileFragment) getSupportFragmentManager().findFragmentByTag("profile");
        
        // Create new fragment instances only if they don't exist
        if (homeFragment == null) {
            boolean isNewLogin = getIntent().getBooleanExtra("IS_NEW_LOGIN", false);
            homeFragment = BoarderHomeFragment.newInstance(isNewLogin);
        }
        if (exploreFragment == null) {
            exploreFragment = ExploreFragment.newInstance(userId);
        }
        if (favoriteFragment == null) {
            favoriteFragment = BoarderFavoriteFragment.newInstance();
        }
        if (bookingFragment == null) {
            bookingFragment = BoarderBookingFragment.newInstance();
        }
        if (profileFragment == null) {
            profileFragment = BoarderProfileFragment.newInstance();
        }

        // Initialize bottom navigation
        initializeBottomNavigation();
        
        // Restore current fragment or load default (Home)
        if (savedInstanceState != null) {
            // Try to restore the current fragment
            String currentTag = savedInstanceState.getString("currentFragmentTag", "home");
            Fragment restoredFragment = getSupportFragmentManager().findFragmentByTag(currentTag);
            if (restoredFragment != null) {
                currentFragment = restoredFragment;
                // Make sure all fragments are added but hidden, then show current
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                if (homeFragment != null && !homeFragment.isAdded()) {
                    transaction.add(R.id.fragment_container, homeFragment, "home");
                }
                if (exploreFragment != null && !exploreFragment.isAdded()) {
                    transaction.add(R.id.fragment_container, exploreFragment, "explore");
                }
                if (favoriteFragment != null && !favoriteFragment.isAdded()) {
                    transaction.add(R.id.fragment_container, favoriteFragment, "favorite");
                }
                if (bookingFragment != null && !bookingFragment.isAdded()) {
                    transaction.add(R.id.fragment_container, bookingFragment, "booking");
                }
                if (profileFragment != null && !profileFragment.isAdded()) {
                    transaction.add(R.id.fragment_container, profileFragment, "profile");
                }
                
                // Hide all fragments first
                if (homeFragment != null && homeFragment.isAdded()) transaction.hide(homeFragment);
                if (exploreFragment != null && exploreFragment.isAdded()) transaction.hide(exploreFragment);
                if (favoriteFragment != null && favoriteFragment.isAdded()) transaction.hide(favoriteFragment);
                if (bookingFragment != null && bookingFragment.isAdded()) transaction.hide(bookingFragment);
                if (profileFragment != null && profileFragment.isAdded()) transaction.hide(profileFragment);
                
                // Show current fragment
                if (currentFragment != null && currentFragment.isAdded()) {
                    transaction.show(currentFragment);
                }
                
                transaction.commit();
                
                // Update bottom navigation to match current fragment
                int selectedItemId = R.id.nav_home;
                if (currentFragment == homeFragment) selectedItemId = R.id.nav_home;
                else if (currentFragment == exploreFragment) selectedItemId = R.id.nav_post;
                else if (currentFragment == favoriteFragment) selectedItemId = R.id.nav_manage;
                else if (currentFragment == bookingFragment) selectedItemId = R.id.nav_activity;
                else if (currentFragment == profileFragment) selectedItemId = R.id.nav_profile;
                
                if (bottomNavigationView != null) {
                    bottomNavigationView.setSelectedItemId(selectedItemId);
                }
            } else {
                // Fallback: load default fragment
                if (homeFragment != null) {
                    loadFragment(homeFragment, "home");
                }
            }
        } else {
            // First time: load default fragment (Home)
            if (homeFragment != null) {
                loadFragment(homeFragment, "home");
            }
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current fragment tag
        if (currentFragment != null && currentFragment.getTag() != null) {
            outState.putString("currentFragmentTag", currentFragment.getTag());
        }
    }

    private void initializeBottomNavigation() {
        try {
            bottomNavigationView = findViewById(R.id.boarder_bottom_navigation);
            
            if (bottomNavigationView != null) {
                bottomNavigationView.setOnItemSelectedListener(item -> {
                    try {
                        Fragment selectedFragment = null;
                        String tag = null;
                        
                        int itemId = item.getItemId();
                        if (itemId == R.id.nav_home) {
                            selectedFragment = homeFragment;
                            tag = "home";
                        } else if (itemId == R.id.nav_post) {
                            selectedFragment = exploreFragment;
                            tag = "explore";
                        } else if (itemId == R.id.nav_manage) {
                            selectedFragment = favoriteFragment;
                            tag = "favorite";
                        } else if (itemId == R.id.nav_activity) {
                            if (Login.checkFeatureAccess(this)) {
                                selectedFragment = bookingFragment;
                                tag = "booking";
                            } else {
                                return false;
                            }
                        } else if (itemId == R.id.nav_profile) {
                            selectedFragment = profileFragment;
                            tag = "profile";
                        }
                        
                        if (selectedFragment != null) {
                            loadFragment(selectedFragment, tag);
                            return true;
                        }
                        
                        return false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFragment(Fragment fragment, String tag) {
        if (fragment != null && fragment != currentFragment) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            
            // Hide current fragment if it exists
            if (currentFragment != null) {
                transaction.hide(currentFragment);
            }
            
            // Show selected fragment (or add if first time)
            if (fragment.isAdded()) {
                transaction.show(fragment);
            } else {
                transaction.add(R.id.fragment_container, fragment, tag);
            }
            
            transaction.commit();
            currentFragment = fragment;
        } else if (fragment == currentFragment && fragment != null && fragment.isAdded()) {
            // Fragment is already visible, just make sure it's shown
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.show(fragment);
            transaction.commit();
        }
    }

    // Public method to switch to a specific tab
    public void switchToTab(int tabId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(tabId);
        }
    }
    
    /**
     * Setup notification permission launcher for Android 13+
     */
    private void setupNotificationPermissionLauncher() {
        requestNotificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d("BoarderDashboard", "Notification permission granted");
                } else {
                    Log.w("BoarderDashboard", "Notification permission denied - notifications may not appear in notification center");
                }
            }
        );
    }
    
    /**
     * Request notification permission if needed (Android 13+)
     */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, request it
                Log.d("BoarderDashboard", "Requesting notification permission");
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d("BoarderDashboard", "Notification permission already granted");
            }
        }
    }
    
    /**
     * Initialize FCM token for push notifications
     */
    private void initializeFCMToken() {
        FCMTokenManager.getCurrentToken(this, new FCMTokenManager.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                Log.d("BoarderDashboard", "FCM Token received: " + token);
                // Automatically send token to server
                sendTokenToServer(token);
            }

            @Override
            public void onTokenError(Exception error) {
                Log.e("BoarderDashboard", "Failed to get FCM token", error);
            }
        });
    }
    
    /**
     * Send FCM token to server
     */
    private void sendTokenToServer(String token) {
        // Server URL - update this to match your server path
        String url = "https://boardease.calapebohol.com/register_device_token.php";
        
        // Create request using Volley
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
            response -> {
                Log.d("BoarderDashboard", "Token sent to server successfully: " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        Log.d("BoarderDashboard", "Device token registered: " + jsonResponse.getString("message"));
                    } else {
                        Log.e("BoarderDashboard", "Server error: " + jsonResponse.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.e("BoarderDashboard", "Error parsing server response: " + e.getMessage());
                }
            },
            error -> {
                Log.e("BoarderDashboard", "Error sending token to server: " + error.getMessage());
            }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId)); // Use actual logged-in user ID
                params.put("device_token", token);
                params.put("device_type", "android");
                params.put("app_version", "1.0.0");
                return params;
            }
        };
        
        // Add request to queue
        Volley.newRequestQueue(this).add(stringRequest);
    }

    // Polling for status updates
    private android.os.Handler statusHandler = new android.os.Handler();
    private Runnable statusRunnable = new Runnable() {
        @Override
        public void run() {
            checkUserStatus();
            statusHandler.postDelayed(this, 5000); // Poll every 5 seconds
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Start polling
        statusHandler.post(statusRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop polling to save battery
        statusHandler.removeCallbacks(statusRunnable);
    }

    private void checkUserStatus() {
        if (userId == 0) return;

        String url = "https://boardease.calapebohol.com/get_user_status.php?user_id=" + userId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("success")) {
                            String serverStatus = jsonObject.getString("status");
                            
                            // Get local status
                            android.content.SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                            String localStatus = prefs.getString("user_status", "");

                            // Check for transition to approved
                            if (!serverStatus.equals(localStatus)) {
                                prefs.edit().putString("user_status", serverStatus).apply();
                                if ("approved".equals(serverStatus) && ("pending_admin_review".equals(localStatus) || "profile_incomplete".equals(localStatus) || "email_unverified".equals(localStatus))) {
                                    // Status changed to approved!
                                    showApprovalDialog();
                                } else {
                                    // Just update storage for other changes, maybe refresh silently if needed
                                    // If moving from valid to invalid, maybe force logout? But for now focused on approval.
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Fail silently for polling
                });
        
        Volley.newRequestQueue(this).add(request);
    }

    private void showApprovalDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Account Approved!")
            .setMessage("Your account has been fully verified and approved by the admin. You now have full access to all features.")
            .setPositiveButton("OK", (dialog, which) -> {
                // Refresh activity to update UI and go to Home
                android.content.Intent intent = new android.content.Intent(this, BoarderDashboard.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            })
            .setCancelable(false)
            .show();
    }

    private void showWelcomeDialogIfIncomplete() {
        android.content.SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String status = prefs.getString("user_status", "approved");
        
        if ("profile_incomplete".equals(status)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Welcome to BoardEase!")
                .setMessage("You can now explore the app/dashboard! To verify your account and unlock all features (like booking rooms), please complete your profile.")
                .setPositiveButton("Complete Profile", (dialog, which) -> {
                    android.content.Intent intent = new android.content.Intent(this, UpdateProfileActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Explore First", null)
                .setCancelable(false)
                .show();
        } else if ("pending_admin_review".equals(status)) {
             new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Profile Under Review")
                .setMessage("Your profile is currently being reviewed by the admin. You can explore the app, but some features may be restricted until approved.")
                .setPositiveButton("Check Status", (dialog, which) -> {
                     android.widget.Toast.makeText(this, "Status: Pending Review", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Explore", null)
                .show();
        }
    }
}