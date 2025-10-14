package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.telecom.Call;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class Type extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_type);

        // Reference buttons
        Button btnViewPrivateRooms = findViewById(R.id.btnViewPrivateRooms);
        Button btnViewBedSpacers = findViewById(R.id.btnViewBedSpacers);

        // Open ActivityPrivate
        btnViewPrivateRooms.setOnClickListener(v -> {
            Intent intent = new Intent(Type.this, Private.class);
            startActivity(intent);
        });

        // Open ActivityBed
        btnViewBedSpacers.setOnClickListener(v -> {
            Intent intent = new Intent(Type.this, Bed.class);
            startActivity(intent);
        });

        // Back button (if exists in your XML)
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Explicitly go back to Details activity
                    Intent intent = new Intent(Type.this, Call.Details.class);
                    startActivity(intent);
                    finish(); // para dili mag-stack ang Bed activity
                }
            });
        }

    }
}
