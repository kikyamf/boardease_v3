package com.example.mock;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MaintenanceRequestsActivity extends AppCompatActivity {

    private static final String TAG = "MaintenanceRequests";
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_requests);

        // Get user ID from intent
        userId = getIntent().getIntExtra("user_id", 0);
        Log.d(TAG, "User ID: " + userId);

        initViews();
        setupViewPager();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        btnBack.setOnClickListener(v -> finish());
    }
    
    public void refreshAllFragments() {
        // Refresh all fragments by finding them through FragmentManager
        // ViewPager2 creates fragments with tags like "f0", "f1", "f2", "f3" etc.
        // Order: 0=Pending, 1=In Progress, 2=Completed, 3=Rejected
        try {
            Fragment pendingFragment = getSupportFragmentManager().findFragmentByTag("f" + 0);
            Fragment inProgressFragment = getSupportFragmentManager().findFragmentByTag("f" + 1);
            Fragment completedFragment = getSupportFragmentManager().findFragmentByTag("f" + 2);
            Fragment rejectedFragment = getSupportFragmentManager().findFragmentByTag("f" + 3);
            
            if (pendingFragment instanceof PendingMaintenanceFragment) {
                ((PendingMaintenanceFragment) pendingFragment).refreshMaintenanceRequests();
            }
            if (inProgressFragment instanceof InProgressMaintenanceFragment) {
                ((InProgressMaintenanceFragment) inProgressFragment).refreshMaintenanceRequests();
            }
            if (completedFragment instanceof CompletedMaintenanceFragment) {
                ((CompletedMaintenanceFragment) completedFragment).refreshMaintenanceRequests();
            }
            if (rejectedFragment instanceof RejectedMaintenanceFragment) {
                ((RejectedMaintenanceFragment) rejectedFragment).refreshMaintenanceRequests();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing fragments", e);
        }
    }

    private void setupViewPager() {
        MaintenanceRequestsPagerAdapter adapter = new MaintenanceRequestsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Pending");
                    break;
                case 1:
                    tab.setText("In Progress");
                    break;
                case 2:
                    tab.setText("Completed");
                    break;
                case 3:
                    tab.setText("Rejected");
                    break;
            }
        }).attach();
    }

    private class MaintenanceRequestsPagerAdapter extends FragmentStateAdapter {

        public MaintenanceRequestsPagerAdapter(MaintenanceRequestsActivity activity) {
            super(activity);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return PendingMaintenanceFragment.newInstance(userId);
                case 1:
                    return InProgressMaintenanceFragment.newInstance(userId);
                case 2:
                    return CompletedMaintenanceFragment.newInstance(userId);
                case 3:
                    return RejectedMaintenanceFragment.newInstance(userId);
                default:
                    return PendingMaintenanceFragment.newInstance(userId);
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}








