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
        userId = getIntent().getIntExtra("user_id", 0);

        setupViews();
        setupViewPager();
        setupTabs();
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
    }

    private void setupTabs() {
        tabLayout = findViewById(R.id.tabLayout);
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Approved");
                    break;
                case 1:
                    tab.setText("Pending");
                    break;
                case 2:
                    tab.setText("History");
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
                    return ApprovedBookingsFragment.newInstance(userId);
                case 1:
                    return PendingBookingsFragment.newInstance(userId);
                case 2:
                    return BookingHistoryFragment.newInstance(userId);
                default:
                    return ApprovedBookingsFragment.newInstance(userId);
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}