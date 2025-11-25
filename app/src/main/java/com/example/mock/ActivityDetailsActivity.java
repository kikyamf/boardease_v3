package com.example.mock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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

    private static final String TAG = "ActivityDetailsActivity";
    private ImageView ivBack;
    private ImageButton btnCalendar;
    private TextView tvTitle;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ActivityDetailsPagerAdapter pagerAdapter;
    private String activityType;
    private int userId;
    
    // Callback interface for payment status updates
    public interface PaymentStatusUpdateCallback {
        void onPaymentStatusUpdated(String newStatus);
    }
    
    private PaymentStatusUpdateCallback paymentStatusCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_details);

        // Get activity type from intent
        activityType = getIntent().getStringExtra("activity_type");
        
        // Get user_id from intent
        userId = getIntent().getIntExtra("user_id", 0);
        
        // Fallback to SharedPreferences if userId is 0 or not set
        if (userId <= 0) {
            SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", null);
            if (userIdString != null) {
                try {
                    userId = Integer.parseInt(userIdString);
                    Log.d(TAG, "Got user_id from SharedPreferences: " + userId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing user_id from SharedPreferences", e);
                    userId = 0;
                }
            }
        }
        
        Log.d(TAG, "Final user_id: " + userId);

        initializeViews();
        setupClickListeners();
        setupViewPager();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        btnCalendar = findViewById(R.id.btnCalendar);
        tvTitle = findViewById(R.id.tvTitle);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Set title based on activity type
        if ("payment_status".equals(activityType)) {
            tvTitle.setText("Payment Status");
            // Show calendar button for payment status
            if (btnCalendar != null) {
                btnCalendar.setVisibility(View.VISIBLE);
            }
        } else if ("boarders_rented".equals(activityType)) {
            tvTitle.setText("Boarders Rented");
            // Hide calendar button for other activities
            if (btnCalendar != null) {
                btnCalendar.setVisibility(View.GONE);
            }
        }
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        if (btnCalendar != null) {
            btnCalendar.setOnClickListener(v -> {
                Intent intent = new Intent(ActivityDetailsActivity.this, PaymentCalendarActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupViewPager() {
        // Configure tab layout based on activity type BEFORE setting adapter
        if ("payment_status".equals(activityType)) {
            // Payment status has 4 tabs - use scrollable mode to prevent text cutoff
            tabLayout.setTabMode(com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE);
            tabLayout.setTabGravity(com.google.android.material.tabs.TabLayout.GRAVITY_CENTER);
        } else if ("boarders_rented".equals(activityType)) {
            // Boarders rented has 2 tabs - use fixed mode for balanced layout
            tabLayout.setTabMode(com.google.android.material.tabs.TabLayout.MODE_FIXED);
            tabLayout.setTabGravity(com.google.android.material.tabs.TabLayout.GRAVITY_FILL);
        }
        
        // userId is already set in onCreate
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
                        tab.setText("Pending");
                        break;
                    case 2:
                        tab.setText("Overdue");
                        break;
                    case 3:
                        tab.setText("Remaining / Partially Paid");
                        break;
                    case 4:
                        tab.setText("Completed / Fully Paid");
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
        
        // Ensure tab text stays on one line and doesn't get cut
        tabLayout.post(() -> {
            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                com.google.android.material.tabs.TabLayout.Tab tab = tabLayout.getTabAt(i);
                if (tab != null && tab.view != null) {
                    // Find the TextView in the tab view
                    for (int j = 0; j < tab.view.getChildCount(); j++) {
                        android.view.View child = tab.view.getChildAt(j);
                        if (child instanceof android.widget.TextView) {
                            android.widget.TextView textView = (android.widget.TextView) child;
                            textView.setSingleLine(true);
                            textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
                            textView.setMaxLines(1);
                        }
                    }
                }
            }
        });
        
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
        
        // Set up payment status update callback
        paymentStatusCallback = newStatus -> {
            handlePaymentStatusUpdate(newStatus);
        };
    }
    
    public PaymentStatusUpdateCallback getPaymentStatusCallback() {
        return paymentStatusCallback;
    }
    
    private void handlePaymentStatusUpdate(String newPaymentStatus) {
        if (!"payment_status".equals(activityType)) {
            return;
        }
        
        // Determine which tab to navigate to based on updated payment status
        // Tab indices: 0=All Payments, 1=Pending, 2=Overdue, 3=Remaining/Partially Paid, 4=Completed/Fully Paid
        int targetTabValue = 0; // Default to "All Payments" tab
        
        if (newPaymentStatus != null) {
            String statusLower = newPaymentStatus.toLowerCase();
            if (statusLower.equals("fully paid") || statusLower.equals("fully_paid")) {
                // If marked as fully paid, go to "Completed / Fully Paid" tab (index 4)
                targetTabValue = 4;
            } else if (statusLower.equals("overdue")) {
                // If marked as overdue, go to "Overdue" tab (index 2)
                targetTabValue = 2;
            } else if (statusLower.equals("partially paid") || statusLower.equals("partially_paid") ||
                      (statusLower.contains("partially") && statusLower.contains("paid")) ||
                      // Legacy support for old "Completed/Partially" status
                      statusLower.equals("completed/partially") || statusLower.equals("completed_partially") || 
                      (statusLower.contains("completed") && statusLower.contains("partially"))) {
                // If marked as partially paid, go to "Remaining / Partially Paid" tab (index 3)
                targetTabValue = 3;
            } else if (statusLower.equals("overdue")) {
                // If marked as overdue, go to "Pending" tab (index 1) where overdue payments are shown
                targetTabValue = 1;
            } else if (statusLower.equals("pending")) {
                // If marked as pending, go to "Pending" tab (index 1)
                targetTabValue = 1;
            } else {
                // Default to "All Payments" tab to see the updated payment
                targetTabValue = 0;
            }
        }
        
        // Make final for lambda
        final int targetTab = targetTabValue;
        
        // Navigate to the appropriate tab
        if (viewPager != null && targetTab < viewPager.getAdapter().getItemCount()) {
            viewPager.setCurrentItem(targetTab, true);
            
            // Refresh all fragments after a short delay to ensure tab is switched
            viewPager.postDelayed(() -> {
                refreshAllFragments();
                // Load the target fragment
                loadFragmentIfNeeded(targetTab);
            }, 300);
        } else {
            // If navigation fails, just refresh all fragments
            refreshAllFragments();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Handle result from PaymentDetailsActivity (fallback if fragments don't handle it)
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            boolean paymentUpdated = data.getBooleanExtra("payment_updated", false);
            if (paymentUpdated && "payment_status".equals(activityType)) {
                String newPaymentStatus = data.getStringExtra("new_payment_status");
                
                // Check if original tab (view_type) was passed back - navigate to original tab if provided
                int originalViewType = data.getIntExtra("view_type", -1);
                if (originalViewType != -1) {
                    // Map view_type to tab index
                    // VIEW_TYPE_ALL = 0 → tab 0 (All Payments)
                    // VIEW_TYPE_FULLY_PAID = 1 → tab 3 (Completed/Fully Paid)
                    // VIEW_TYPE_REMAINING = 2 → tab 2 (Remaining/Partially Paid)
                    // VIEW_TYPE_PENDING = 3 → tab 1 (Pending)
                    int targetTab = 0; // Default to All Payments
                    if (originalViewType == PaymentAdapter.VIEW_TYPE_ALL) {
                        targetTab = 0; // All Payments
                    } else if (originalViewType == PaymentAdapter.VIEW_TYPE_PENDING) {
                        targetTab = 1; // Pending
                    } else if (originalViewType == PaymentAdapter.VIEW_TYPE_OVERDUE) {
                        targetTab = 2; // Overdue
                    } else if (originalViewType == PaymentAdapter.VIEW_TYPE_REMAINING) {
                        targetTab = 3; // Remaining/Partially Paid
                    } else if (originalViewType == PaymentAdapter.VIEW_TYPE_FULLY_PAID) {
                        targetTab = 4; // Completed/Fully Paid
                    }
                    
                    // Navigate to the original tab
                    if (viewPager != null && targetTab < viewPager.getAdapter().getItemCount()) {
                        viewPager.setCurrentItem(targetTab, true);
                    }
                } else if (newPaymentStatus != null && paymentStatusCallback != null) {
                    // If no original tab provided, navigate based on new payment status
                    paymentStatusCallback.onPaymentStatusUpdated(newPaymentStatus);
                }
                
                // Refresh all fragments
                refreshAllFragments();
            }
        }
    }
    
    private void refreshAllFragments() {
        if (!"payment_status".equals(activityType)) {
            return;
        }
        
        // Refresh all payment fragments
        for (int i = 0; i < 5; i++) {
            String tag = "f" + i;
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
            
            if (fragment != null && fragment.isAdded()) {
                if (i == 0 && fragment instanceof AllPaymentsFragment) {
                    ((AllPaymentsFragment) fragment).refreshData();
                } else if (i == 1 && fragment instanceof PendingPaymentsFragment) {
                    ((PendingPaymentsFragment) fragment).refreshData();
                } else if (i == 2 && fragment instanceof OverduePaymentsFragment) {
                    ((OverduePaymentsFragment) fragment).refreshData();
                } else if (i == 3 && fragment instanceof RemainingPaymentsFragment) {
                    ((RemainingPaymentsFragment) fragment).refreshData();
                } else if (i == 4 && fragment instanceof FullyPaidPaymentsFragment) {
                    ((FullyPaidPaymentsFragment) fragment).refreshData();
                }
            }
        }
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
                            } else if (position == 1 && fragment instanceof PendingPaymentsFragment) {
                                // Pending tab
                                ((PendingPaymentsFragment) fragment).loadIfNeeded();
                            } else if (position == 2 && fragment instanceof OverduePaymentsFragment) {
                                // Overdue tab
                                ((OverduePaymentsFragment) fragment).loadIfNeeded();
                            } else if (position == 3 && fragment instanceof RemainingPaymentsFragment) {
                                // Remaining / Partially Paid tab
                                ((RemainingPaymentsFragment) fragment).loadIfNeeded();
                            } else if (position == 4 && fragment instanceof FullyPaidPaymentsFragment) {
                                // Completed / Fully Paid tab
                                ((FullyPaidPaymentsFragment) fragment).loadIfNeeded();
                            }
                        } else if ("boarders_rented".equals(activityType)) {
                            if (position == 0 && fragment instanceof CurrentBoardersFragment) {
                                // Current boarders tab
                                ((CurrentBoardersFragment) fragment).loadIfNeeded();
                            } else if (position == 1 && fragment instanceof BoardersHistoryFragment) {
                                // History tab
                                ((BoardersHistoryFragment) fragment).loadIfNeeded();
                            }
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
            Bundle args = new Bundle();
            args.putInt("user_id", userId);
            
            if ("payment_status".equals(activityType)) {
                switch (position) {
                    case 0:
                        AllPaymentsFragment allPaymentsFragment = new AllPaymentsFragment();
                        allPaymentsFragment.setArguments(args);
                        return allPaymentsFragment;
                    case 1:
                        PendingPaymentsFragment pendingFragment = new PendingPaymentsFragment();
                        pendingFragment.setArguments(args);
                        return pendingFragment;
                    case 2:
                        OverduePaymentsFragment overdueFragment = new OverduePaymentsFragment();
                        overdueFragment.setArguments(args);
                        return overdueFragment;
                    case 3:
                        RemainingPaymentsFragment remainingFragment = new RemainingPaymentsFragment();
                        remainingFragment.setArguments(args);
                        return remainingFragment;
                    case 4:
                        FullyPaidPaymentsFragment fullyPaidFragment = new FullyPaidPaymentsFragment();
                        fullyPaidFragment.setArguments(args);
                        return fullyPaidFragment;
                    default:
                        AllPaymentsFragment defaultFragment = new AllPaymentsFragment();
                        defaultFragment.setArguments(args);
                        return defaultFragment;
                }
            } else if ("boarders_rented".equals(activityType)) {
                switch (position) {
                    case 0:
                        CurrentBoardersFragment currentFragment = CurrentBoardersFragment.newInstance(userId);
                        return currentFragment;
                    case 1:
                        BoardersHistoryFragment historyFragment = BoardersHistoryFragment.newInstance(userId);
                        return historyFragment;
                    default:
                        CurrentBoardersFragment defaultFragment = CurrentBoardersFragment.newInstance(userId);
                        return defaultFragment;
                }
            }
            AllPaymentsFragment defaultFragment = new AllPaymentsFragment();
            defaultFragment.setArguments(args);
            return defaultFragment;
        }

        @Override
        public int getItemCount() {
            if ("payment_status".equals(activityType)) {
                return 5; // All Payments, Pending, Overdue, Remaining / Partially Paid, Completed / Fully Paid
            } else if ("boarders_rented".equals(activityType)) {
                return 2; // Current, History
            }
            return 1;
        }
    }
}






















