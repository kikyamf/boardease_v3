package com.example.mock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TransactionsLogsActivity extends AppCompatActivity {

    private static final String TAG = "TransactionsLogsActivity";
    
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TransactionsPagerAdapter pagerAdapter;
    private ImageView ivBack;
    private TextView tvTitle;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions_logs);

        // Get user_id from intent or SharedPreferences
        userId = getIntent().getIntExtra("user_id", 0);
        if (userId <= 0) {
            SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", "0");
            try {
                userId = Integer.parseInt(userIdString);
                Log.d(TAG, "Got user_id from SharedPreferences: " + userId);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse user_id from SharedPreferences: " + userIdString);
                userId = 0;
            }
        } else {
            Log.d(TAG, "Got user_id from intent: " + userId);
        }

        // Initialize views
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Set title
        tvTitle.setText("Transactions & Logs");

        // Setup back button
        ivBack.setOnClickListener(v -> finish());

        // Setup ViewPager and TabLayout
        setupViewPager();
    }
    
    public int getUserId() {
        return userId;
    }

    private void setupViewPager() {
        pagerAdapter = new TransactionsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Bookings");
                    break;
                case 1:
                    tab.setText("Payments");
                    break;
                case 2:
                    tab.setText("Rentals");
                    break;
                case 3:
                    tab.setText("Maintenance");
                    break;
            }
        });
        mediator.attach();
        
        // Ensure tab text displays full words without truncation and adjust tabbed pane
        tabLayout.post(() -> {
            try {
                for (int i = 0; i < tabLayout.getTabCount(); i++) {
                    TabLayout.Tab tab = tabLayout.getTabAt(i);
                    if (tab != null && tab.view != null) {
                        // Remove any width constraints from tab view to allow expansion
                        tab.view.setMinimumWidth(0);
                        tab.view.setPadding(
                            (int) (16 * getResources().getDisplayMetrics().density), // 16dp padding start
                            tab.view.getPaddingTop(),
                            (int) (16 * getResources().getDisplayMetrics().density), // 16dp padding end
                            tab.view.getPaddingBottom()
                        );
                        
                        // Find the TextView in the tab view
                        for (int j = 0; j < tab.view.getChildCount(); j++) {
                            View child = tab.view.getChildAt(j);
                            if (child instanceof TextView) {
                                TextView textView = (TextView) child;
                                textView.setSingleLine(true);
                                textView.setMaxLines(1);
                                textView.setEllipsize(null); // No ellipsize - show full text
                                textView.setMaxWidth(Integer.MAX_VALUE); // Allow text to expand fully
                                // Make sure text wraps to content width
                                android.view.ViewGroup.LayoutParams params = textView.getLayoutParams();
                                if (params != null) {
                                    params.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                                    textView.setLayoutParams(params);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting tab text properties: " + e.getMessage());
            }
        });
    }
}


































