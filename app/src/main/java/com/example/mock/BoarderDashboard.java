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

public class BoarderDashboard extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_boarder_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize bottom navigation
        initializeBottomNavigation();
        
        // Load default fragment (Home)
        loadFragment(new BoarderHomeFragment());
    }

    private void initializeBottomNavigation() {
        bottomNavigationView = findViewById(R.id.boarder_bottom_navigation);
        
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new BoarderHomeFragment();
            } else if (itemId == R.id.nav_post) {
                // TODO: Replace with BoarderExploreFragment when created
                selectedFragment = new ExploreFragment(); // Using existing ExploreFragment as placeholder
            } else if (itemId == R.id.nav_manage) {
                // TODO: Create BoarderFavoritesFragment
                selectedFragment = new BoarderHomeFragment(); // Placeholder
            } else if (itemId == R.id.nav_activity) {
                // TODO: Create BoarderBookingsFragment
                selectedFragment = new BoarderHomeFragment(); // Placeholder
            } else if (itemId == R.id.nav_profile) {
                // TODO: Create BoarderProfileFragment
                selectedFragment = new BoarderHomeFragment(); // Placeholder
            }
            
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null && !fragment.equals(currentFragment)) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
            currentFragment = fragment;
        }
    }
}