package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class Private extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private);

        // RecyclerView setup
        RecyclerView recyclerView = findViewById(R.id.rvPrivateRooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // TODO: Replace with your adapter + data
        // recyclerView.setAdapter(new PrivateRoomAdapter(myRoomList));

        // Back button functionality
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Explicitly go back to Details activity
                    Intent intent = new Intent(Private.this, Type.class);
                    startActivity(intent);
                    finish(); // para dili mag-stack ang Bed activity
                }
            });
        }
    }
}

