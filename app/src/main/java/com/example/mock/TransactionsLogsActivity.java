package com.example.mock;

import android.os.Bundle;
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

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TransactionsPagerAdapter pagerAdapter;
    private ImageView ivBack;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions_logs);

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

    private void setupViewPager() {
        pagerAdapter = new TransactionsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Reservations");
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
        }).attach();
    }
}
































