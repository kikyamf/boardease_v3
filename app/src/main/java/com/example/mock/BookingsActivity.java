package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class BookingsActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private BookingsPagerAdapter pagerAdapter;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        // Get userId from intent
        //
        userId = getIntent().getIntExtra("user_id", 0);

        setupViews();
        setupViewPager();
        setupTabs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Don't auto-refresh on tab switch - only refresh when explicitly requested
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Refresh fragments when returning from BookingDetailsActivity after approve/decline
        if (resultCode == RESULT_OK) {
            refreshFragments();
            
            // Navigate to pending tab if booking was approved
            if (data != null && data.getBooleanExtra("should_navigate_to_pending", false)) {
                // Navigate to pending tab (position 0)
                if (viewPager != null && viewPager.getAdapter() != null && viewPager.getAdapter().getItemCount() > 0) {
                    viewPager.setCurrentItem(0, true); // Navigate to pending tab (position 0)
                }
            }
        }
    }

    private void refreshFragments() {
        // Refresh all fragments by finding them through FragmentManager
        // ViewPager2 creates fragments with tags like "f0", "f1", "f2" etc.
        // Order: 0=Pending, 1=Approved, 2=History
        try {
            Fragment pendingFragment = getSupportFragmentManager().findFragmentByTag("f" + 0);
            Fragment approvedFragment = getSupportFragmentManager().findFragmentByTag("f" + 1);
            Fragment historyFragment = getSupportFragmentManager().findFragmentByTag("f" + 2);
            Fragment terminationFragment = getSupportFragmentManager().findFragmentByTag("f" + 3);
            
            if (pendingFragment instanceof PendingBookingsFragment) {
                ((PendingBookingsFragment) pendingFragment).refreshBookings();
            }
            if (approvedFragment instanceof ApprovedBookingsFragment) {
                ((ApprovedBookingsFragment) approvedFragment).refreshBookings();
            }
            if (historyFragment instanceof BookingHistoryFragment) {
                ((BookingHistoryFragment) historyFragment).refreshBookings();
            }
            if (terminationFragment instanceof TerminationRequestsFragment) {
                ((TerminationRequestsFragment) terminationFragment).refreshBookings();
            }
        } catch (Exception e) {
            // If fragment tags don't work, fragments will refresh on their onResume
            e.printStackTrace();
        }
    }

    private void setupViews() {
        // Setup header
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Bookings");

        // Setup back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new BookingsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // Set offscreen page limit to prevent pre-loading all fragments
        viewPager.setOffscreenPageLimit(1);
        
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
                        if (position == 0 && fragment instanceof PendingBookingsFragment) {
                            // Pending tab
                            ((PendingBookingsFragment) fragment).loadIfNeeded();
                        } else if (position == 1 && fragment instanceof ApprovedBookingsFragment) {
                            // Approved tab
                            ((ApprovedBookingsFragment) fragment).loadIfNeeded();
                        } else if (position == 2 && fragment instanceof BookingHistoryFragment) {
                            // History tab
                            ((BookingHistoryFragment) fragment).loadIfNeeded();
                        } else if (position == 3 && fragment instanceof TerminationRequestsFragment) {
                            // Termination tab
                            ((TerminationRequestsFragment) fragment).loadIfNeeded();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTabs() {
        tabLayout = findViewById(R.id.tabLayout);
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Pending");
                    break;
                case 1:
                    tab.setText("Approved");
                    break;
                case 2:
                    tab.setText("History");
                    break;
                case 3:
                    tab.setText("Termination Request");
                    break;
            }
        }).attach();
    }

    private class BookingsPagerAdapter extends FragmentStateAdapter {

        public BookingsPagerAdapter(@NonNull BookingsActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return PendingBookingsFragment.newInstance(userId);
                case 1:
                    return ApprovedBookingsFragment.newInstance(userId);
                case 2:
                    return BookingHistoryFragment.newInstance(userId);
                case 3:
                    return TerminationRequestsFragment.newInstance(userId);
                default:
                    return PendingBookingsFragment.newInstance(userId);
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}







