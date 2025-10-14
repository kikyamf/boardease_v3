package com.example.mock;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Load HomeFragment by default with user_id
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, OwnerHomeFragment.newInstance(userId))
                    .commit();
        }

        // Handle bottom nav clicks
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = OwnerHomeFragment.newInstance(userId);
            } else if (id == R.id.nav_post) {
                selectedFragment = AddingBhFragment.newInstance(userId);
            } else if (id == R.id.nav_profile) {
                selectedFragment = OwnerProfileFragment.newInstance(userId);
            } else if (id == R.id.nav_activity) {
                selectedFragment = ActivityFragment.newInstance(userId);
            } else if (id == R.id.nav_manage) {
                selectedFragment = ManageFragment.newInstance(userId);
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
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
        String url = "http://192.168.101.6/BoardEase2/register_device_token.php";
        
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

}
