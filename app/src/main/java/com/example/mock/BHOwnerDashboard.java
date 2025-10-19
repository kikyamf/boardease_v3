package com.example.mock;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BHOwnerDashboard extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bh_owner_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize bottom navigation
        initializeBottomNavigation();
        
        // Load default fragment (Home)
        loadFragment(new HomeFragment());
    }

    private void initializeBottomNavigation() {
        try {
            bottomNavigationView = findViewById(R.id.owner_bottom_navigation);
            
            if (bottomNavigationView != null) {
                bottomNavigationView.setOnItemSelectedListener(item -> {
                    try {
                        Fragment selectedFragment = null;
                        
                        int itemId = item.getItemId();
                        if (itemId == R.id.nav_home) {
                            selectedFragment = new HomeFragment();
                        } else if (itemId == R.id.nav_post) {
                            selectedFragment = new AddingBhFragment();
                        } else if (itemId == R.id.nav_manage) {
                            selectedFragment = new ManageFragment();
                        } else if (itemId == R.id.nav_activity) {
                            selectedFragment = new PaymentsFragment();
                        } else if (itemId == R.id.nav_profile) {
                            selectedFragment = new OwnerProfileFragment();
                        }
                        
                        if (selectedFragment != null) {
                            loadFragment(selectedFragment);
                            return true;
                        }
                        
                        return false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null && !fragment.equals(currentFragment)) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
            currentFragment = fragment;
        }
    }

    // Public method to switch to a specific tab
    public void switchToTab(int tabId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(tabId);
        }
    }
}
