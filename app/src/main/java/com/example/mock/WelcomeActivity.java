package com.example.mock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mock.adapters.CarouselAdapter;

import me.relex.circleindicator.CircleIndicator3;

public class WelcomeActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button nextButton;
//    private CircleIndicator3 indicator; // Instance variable

    private int[] images = {
            R.drawable.carousel3,
            R.drawable.login_bg_bh,
            R.drawable.carousel2,
            R.drawable.carousel1,
            R.drawable.carousel3
    };
    private String[] titles = {
            "Welcome to BoardEase!",
            "What is BoardEase?",
            "Easy to Get Started",
            "Empowering Features",
            "Ready to Dive In?"
    };
    private String[] descriptions = {
            "Your ultimate companion for finding and managing your perfect boarding house.",
            "Streamlining the boarding house experience for owners and boarders alike.",
            "Register, verify your email, and start searching or managing in minutes.",
            "Real-time messaging, secure payments, and smart management at your fingertips.",
            "Join our growing community and experience housing simplified."
    };

    // SharedPreferences for checking user session
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "UserSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROLE = "user_role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Check if user is already logged in
        checkExistingSession();

        viewPager = findViewById(R.id.viewPager);
        nextButton = findViewById(R.id.nextButton);

        CarouselAdapter adapter = new CarouselAdapter(images, titles, descriptions);
        viewPager.setAdapter(adapter);

        // Link indicator with ViewPager2
        CircleIndicator3 indicator = findViewById(R.id.indicator);
        indicator.setViewPager(viewPager);

        // Add interactive feedback to button
        addButtonClickAnimation(nextButton);

        nextButton.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < images.length - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                startActivity(new Intent(WelcomeActivity.this, Login.class));
                finish();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == images.length - 1) {
                    nextButton.setText("Get Started");
                } else {
                    nextButton.setText("Next");
                }
            }
        });
    }

    private void addButtonClickAnimation(android.view.View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }
    
    private void checkExistingSession() {
        // Check if user is already logged in
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        String userRole = sharedPreferences.getString(KEY_USER_ROLE, null);
        
        if (userId != null && userRole != null) {
            // User is already logged in, navigate directly to appropriate dashboard
            navigateToDashboard(userRole);
        }
    }
    
    private void navigateToDashboard(String userRole) {
        Intent intent;
        
        if ("Boarder".equals(userRole)) {
            // Navigate to BoarderDashboard
            intent = new Intent(WelcomeActivity.this, BoarderDashboard.class);
        } else if ("BH Owner".equals(userRole)) {
            // Navigate to MainActivity (Owner Dashboard)
            intent = new Intent(WelcomeActivity.this, MainActivity.class);
        } else {
            // Unknown role, stay on welcome screen
            return;
        }
        
        // Clear the welcome activity from the stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
