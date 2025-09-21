package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class Bed extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bed);

        RecyclerView recyclerView = findViewById(R.id.rvBedSpacers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // TODO: Replace with your adapter + data
        // recyclerView.setAdapter(new BedSpacerAdapter(myBedList));

        // Back button (if exists in your XML)
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Explicitly go back to Details activity
                    Intent intent = new Intent(Bed.this, Type.class);
                    startActivity(intent);
                    finish(); // para dili mag-stack ang Bed activity
                }
            });
        }
    }
}

