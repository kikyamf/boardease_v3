package com.example.mock;

import android.Manifest;
import android.content.SharedPreferences;
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
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private int userId;
    
    // SharedPreferences for getting user session
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "UserSession";
    private static final String KEY_USER_ID = "user_id";
    
    // Fragment references
    private Fragment currentFragment;
    
    // Cache fragment instances to avoid recreating them
    private OwnerHomeFragment homeFragment;
    private AddingBhFragment addingBhFragment;
    private OwnerProfileFragment profileFragment;
    private ActivityFragment activityFragment;
    private ManageFragment manageFragment;
    
    // Notification permission launcher for Android 13+
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Redirection check for hybrid soft registration
        showWelcomeDialogIfIncomplete();
        
        // Create notification channel early
        NotificationUtils.createNotificationChannel(this);
        
        // Setup notification permission launcher for Android 13+
        setupNotificationPermissionLauncher();
        
        // Request notification permission if needed (Android 13+)
        requestNotificationPermissionIfNeeded();

        // Get user_id from SharedPreferences (actual logged-in user)
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userIdString = sharedPreferences.getString("user_id", null);
        
        if (userIdString != null) {
            userId = Integer.parseInt(userIdString);
            Log.d("MainActivity", "User ID from session: " + userId);
        } else {
            // Fallback to 1 if no session found (for testing)
            userId = 1;
            Log.d("MainActivity", "No session found, using fallback user ID: " + userId);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize FCM token
        initializeFCMToken();
        
        // Try to restore fragments from FragmentManager first (they persist across configuration changes)
        homeFragment = (OwnerHomeFragment) getSupportFragmentManager().findFragmentByTag("home");
        addingBhFragment = (AddingBhFragment) getSupportFragmentManager().findFragmentByTag("adding_bh");
        profileFragment = (OwnerProfileFragment) getSupportFragmentManager().findFragmentByTag("profile");
        activityFragment = (ActivityFragment) getSupportFragmentManager().findFragmentByTag("activity");
        manageFragment = (ManageFragment) getSupportFragmentManager().findFragmentByTag("manage");
        
        // Create new fragment instances only if they don't exist
        if (homeFragment == null) {
            homeFragment = OwnerHomeFragment.newInstance(userId);
        }
        if (addingBhFragment == null) {
            addingBhFragment = AddingBhFragment.newInstance(userId);
        }
        if (profileFragment == null) {
            profileFragment = OwnerProfileFragment.newInstance(userId);
        }
        if (activityFragment == null) {
            activityFragment = ActivityFragment.newInstance(userId);
        }
        if (manageFragment == null) {
            manageFragment = ManageFragment.newInstance(userId);
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

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
                if (addingBhFragment != null && !addingBhFragment.isAdded()) {
                    transaction.add(R.id.fragment_container, addingBhFragment, "adding_bh");
                }
                if (profileFragment != null && !profileFragment.isAdded()) {
                    transaction.add(R.id.fragment_container, profileFragment, "profile");
                }
                if (activityFragment != null && !activityFragment.isAdded()) {
                    transaction.add(R.id.fragment_container, activityFragment, "activity");
                }
                if (manageFragment != null && !manageFragment.isAdded()) {
                    transaction.add(R.id.fragment_container, manageFragment, "manage");
                }
                
                // Hide all fragments first
                if (homeFragment != null && homeFragment.isAdded()) transaction.hide(homeFragment);
                if (addingBhFragment != null && addingBhFragment.isAdded()) transaction.hide(addingBhFragment);
                if (profileFragment != null && profileFragment.isAdded()) transaction.hide(profileFragment);
                if (activityFragment != null && activityFragment.isAdded()) transaction.hide(activityFragment);
                if (manageFragment != null && manageFragment.isAdded()) transaction.hide(manageFragment);
                
                // Show current fragment
                if (currentFragment != null && currentFragment.isAdded()) {
                    transaction.show(currentFragment);
                }
                
                transaction.commit();
                
                // Update bottom navigation to match current fragment
                int selectedItemId = R.id.nav_home;
                if (currentFragment == homeFragment) selectedItemId = R.id.nav_home;
                else if (currentFragment == addingBhFragment) selectedItemId = R.id.nav_post;
                else if (currentFragment == profileFragment) selectedItemId = R.id.nav_profile;
                else if (currentFragment == activityFragment) selectedItemId = R.id.nav_activity;
                else if (currentFragment == manageFragment) selectedItemId = R.id.nav_manage;
                
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
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
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
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // Handle bottom nav clicks
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String tag = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = homeFragment;
                tag = "home";
            } else if (id == R.id.nav_post) {
                if (Login.checkFeatureAccess(this)) {
                    selectedFragment = addingBhFragment;
                    tag = "adding_bh";
                } else {
                    return false;
                }
            } else if (id == R.id.nav_profile) {
                selectedFragment = profileFragment;
                tag = "profile";
            } else if (id == R.id.nav_activity) {
                if (Login.checkFeatureAccess(this)) {
                    selectedFragment = activityFragment;
                    tag = "activity";
                } else {
                    return false;
                }
            } else if (id == R.id.nav_manage) {
                if (Login.checkFeatureAccess(this)) {
                    selectedFragment = manageFragment;
                    tag = "manage";
                } else {
                    return false;
                }
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment, tag);
                return true;
            }
            return false;
        });
    }
    
    private void loadFragment(Fragment fragment, String tag) {
        if (fragment != null && fragment != currentFragment) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            
            // Hide current fragment if it exists
            if (currentFragment != null) {
                transaction.hide(currentFragment);
            }
            
            // Show selected fragment (or add if first time)
            if (getSupportFragmentManager().findFragmentByTag(tag) != null) {
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

    /**
     * Initialize FCM token for push notifications
     */
    private void initializeFCMToken() {
        FCMTokenManager.getCurrentToken(this, new FCMTokenManager.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                Log.d("MainActivity", "FCM Token received: " + token);
                // Automatically send token to server
                sendTokenToServer(token);
            }

            @Override
            public void onTokenError(Exception error) {
                Log.e("MainActivity", "Failed to get FCM token", error);
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
                Log.d("MainActivity", "Token sent to server successfully: " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        Log.d("MainActivity", "Device token registered: " + jsonResponse.getString("message"));
                    } else {
                        Log.e("MainActivity", "Server error: " + jsonResponse.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.e("MainActivity", "Error parsing server response: " + e.getMessage());
                }
            },
            error -> {
                Log.e("MainActivity", "Error sending token to server: " + error.getMessage());
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
    
    /**
     * Setup notification permission launcher for Android 13+
     */
    private void setupNotificationPermissionLauncher() {
        requestNotificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d("MainActivity", "Notification permission granted");
                } else {
                    Log.w("MainActivity", "Notification permission denied - notifications may not appear in notification center");
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
                Log.d("MainActivity", "Requesting notification permission");
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d("MainActivity", "Notification permission already granted");
            }
        }
    }

    private void showWelcomeDialogIfIncomplete() {
        android.content.SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String status = prefs.getString("user_status", "approved");
        
        if ("profile_incomplete".equals(status)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Welcome to BoardEase!")
                .setMessage("You can now explore the app! To verify your account and unlock all features (like posting boarding houses), please complete your profile.")
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
                     // Optional: refresh logic
                     android.widget.Toast.makeText(this, "Status: Pending Review", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Explore", null)
                .show();
        }
    }
}
