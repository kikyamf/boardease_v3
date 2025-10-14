package com.example.mock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds
    
    // SharedPreferences for checking user session
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "UserSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROLE = "user_role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        ImageView logo = findViewById(R.id.logoImage);

        // Load animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo.startAnimation(fadeIn);

        // Check if user is already logged in
        checkExistingSession();
    }
    
    private void checkExistingSession() {
        // Check if user is already logged in
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        String userRole = sharedPreferences.getString(KEY_USER_ROLE, null);
        
        if (userId != null && userRole != null) {
            // User is already logged in, navigate directly to appropriate dashboard
            new Handler().postDelayed(() -> {
                navigateToDashboard(userRole);
            }, SPLASH_DURATION);
        } else {
            // User is not logged in, proceed to Welcome screen
            new Handler().postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
                finish();
            }, SPLASH_DURATION);
        }
    }
    
    private void navigateToDashboard(String userRole) {
        Intent intent;
        
        if ("Boarder".equals(userRole)) {
            // Navigate to BoarderDashboard
            intent = new Intent(SplashActivity.this, BoarderDashboard.class);
        } else if ("BH Owner".equals(userRole)) {
            // Navigate to MainActivity (Owner Dashboard) but let me just change it for testing
            intent = new Intent(SplashActivity.this, BoarderDashboard.class);
        } else {
            // Unknown role, go to Welcome screen
            intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        }
        
        // Clear the splash activity from the stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}