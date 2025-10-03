package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class RoomViewActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private RoomPagerAdapter pagerAdapter;
    private ImageView ivBack;
    private TextView tvTitle;
    private ImageButton btnAddRooms;
    
    private int bhId;
    private String bhName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_view);

        // Get boarding house data from intent
        bhId = getIntent().getIntExtra("bh_id", -1);
        bhName = getIntent().getStringExtra("bh_name");

        // Initialize views
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        btnAddRooms = findViewById(R.id.btnAddRooms);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Set title with boarding house name
        tvTitle.setText(bhName + " - Rooms");

        // Setup back button
        ivBack.setOnClickListener(v -> finish());
        
        // Setup add rooms button
        btnAddRooms.setOnClickListener(v -> {
            // Navigate to AddRoomsActivity
            Intent intent = new Intent(this, AddRoomsActivity.class);
            intent.putExtra("bh_id", bhId);
            intent.putExtra("bh_name", bhName);
            startActivity(intent);
        });

        // Setup ViewPager and TabLayout
        setupViewPager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from AddRoomsActivity
        if (pagerAdapter != null) {
            pagerAdapter.refreshData();
        }
    }

    private void setupViewPager() {
        pagerAdapter = new RoomPagerAdapter(this, bhId);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Private Rooms");
                    break;
                case 1:
                    tab.setText("Bed Spacers");
                    break;
            }
        }).attach();
    }
}
