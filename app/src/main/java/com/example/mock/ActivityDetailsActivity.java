package com.example.mock;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ActivityDetailsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvTitle;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ActivityDetailsPagerAdapter pagerAdapter;
    private String activityType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_details);

        // Get activity type from intent
        activityType = getIntent().getStringExtra("activity_type");

        initializeViews();
        setupClickListeners();
        setupViewPager();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Set title based on activity type
        if ("payment_status".equals(activityType)) {
            tvTitle.setText("Payment Status");
        } else if ("boarders_rented".equals(activityType)) {
            tvTitle.setText("Boarders Rented");
        }
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    private void setupViewPager() {
        // Get user_id from intent
        int userId = getIntent().getIntExtra("user_id", 0);
        
        pagerAdapter = new ActivityDetailsPagerAdapter(this, activityType, userId);
        viewPager.setAdapter(pagerAdapter);
        
        // Set offscreen page limit to prevent pre-loading all fragments
        viewPager.setOffscreenPageLimit(1);
        
        // Set up tab layout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if ("payment_status".equals(activityType)) {
                switch (position) {
                    case 0:
                        tab.setText("All Payments");
                        break;
                    case 1:
                        tab.setText("Completed");
                        break;
                    case 2:
                        tab.setText("Pending");
                        break;
                }
            } else if ("boarders_rented".equals(activityType)) {
                switch (position) {
                    case 0:
                        tab.setText("Current");
                        break;
                    case 1:
                        tab.setText("History");
                        break;
                }
            }
        }).attach();
        
        // Listen for page changes to trigger load when tab is clicked
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Trigger load for the selected page when tab is clicked
                // Use post to ensure fragment is fully created
                viewPager.post(() -> {
                    viewPager.postDelayed(() -> loadFragmentIfNeeded(position), 150);
                });
            }
        });
        
        // Load first tab on activity start
        viewPager.post(() -> {
            viewPager.postDelayed(() -> loadFragmentIfNeeded(0), 300);
        });
    }
    
    private void loadFragmentIfNeeded(int position) {
        try {
            // ViewPager2 creates fragments with tags like "f0", "f1", "f2"
            String tag = "f" + position;
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
            
            if (fragment != null && fragment.isAdded()) {
                // Wait for fragment to be ready
                viewPager.post(() -> {
                    try {
                        if ("payment_status".equals(activityType)) {
                            if (position == 0 && fragment instanceof AllPaymentsFragment) {
                                // All Payments tab
                                ((AllPaymentsFragment) fragment).loadIfNeeded();
                            } else if (position == 1 && fragment instanceof CompletedPaymentsFragment) {
                                // Completed tab
                                ((CompletedPaymentsFragment) fragment).loadIfNeeded();
                            } else if (position == 2 && fragment instanceof PendingPaymentsFragment) {
                                // Pending tab
                                ((PendingPaymentsFragment) fragment).loadIfNeeded();
                            }
                        }
                        // Add boarders_rented handling if needed in the future
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ActivityDetailsPagerAdapter extends FragmentStateAdapter {
        private String activityType;
        private int userId;

        public ActivityDetailsPagerAdapter(@NonNull FragmentActivity fragmentActivity, String activityType, int userId) {
            super(fragmentActivity);
            this.activityType = activityType;
            this.userId = userId;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if ("payment_status".equals(activityType)) {
                switch (position) {
                    case 0:
                        return new AllPaymentsFragment();
                    case 1:
                        return new CompletedPaymentsFragment();
                    case 2:
                        return new PendingPaymentsFragment();
                    default:
                        return new AllPaymentsFragment();
                }
            } else if ("boarders_rented".equals(activityType)) {
                switch (position) {
                    case 0:
                        return new CurrentBoardersFragment();
                    case 1:
                        return new BoardersHistoryFragment();
                    default:
                        return new CurrentBoardersFragment();
                }
            }
            return new AllPaymentsFragment();
        }

        @Override
        public int getItemCount() {
            if ("payment_status".equals(activityType)) {
                return 3; // All Payments, Completed, Pending
            } else if ("boarders_rented".equals(activityType)) {
                return 2; // Current, History
            }
            return 1;
        }
    }
}






















