package com.example.mock;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class AddRoomsActivity extends AppCompatActivity {

    private int bhId;
    private String bhName;
    private int userId = 1; // Default user ID, you can get this from shared preferences

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_rooms);

        // Get data from intent
        bhId = getIntent().getIntExtra("bh_id", -1);
        bhName = getIntent().getStringExtra("bh_name");

        if (bhId == -1) {
            Toast.makeText(this, "Invalid boarding house ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set title
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Add Rooms to " + bhName);

        // Setup back button
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        // Load AddingRoomsFragment with existing boarding house data
        loadAddingRoomsFragment();
    }

    private void loadAddingRoomsFragment() {
        AddingRoomsFragment fragment = new AddingRoomsFragment();
        Bundle bundle = new Bundle();
        
        // Pass existing boarding house data
        bundle.putInt("user_id", userId);
        bundle.putString("bh_name", bhName);
        bundle.putString("bh_address", "Existing Address"); // You might want to fetch this from database
        bundle.putString("bh_description", "Existing Description");
        bundle.putString("bh_rules", "Existing Rules");
        bundle.putString("bh_bathrooms", "1");
        bundle.putString("bh_area", "100");
        bundle.putString("bh_build_year", "2020");
        bundle.putParcelableArrayList("bh_images", new java.util.ArrayList<>());
        bundle.putString("mode", "edit"); // This tells the fragment we're adding rooms to existing BH
        bundle.putInt("bh_id", bhId);
        
        fragment.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}








