package com.example.mock;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private int userId = 1; // Hardcoded user_id for testing (Owner)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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
                // You can send this token to your server here
                // For now, we'll just log it
            }

            @Override
            public void onTokenError(Exception error) {
                Log.e("MainActivity", "Failed to get FCM token", error);
            }
        });
    }

}
