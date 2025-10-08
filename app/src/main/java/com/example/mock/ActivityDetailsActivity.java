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






















