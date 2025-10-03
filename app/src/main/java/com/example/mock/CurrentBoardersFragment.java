package com.example.mock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CurrentBoardersFragment extends Fragment {

    private TextView tvNoActiveRentals;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_boarders, container, false);

        tvNoActiveRentals = view.findViewById(R.id.tvNoActiveRentals);

        // For now, show no active rentals message
        tvNoActiveRentals.setText("No active rentals.");

        return view;
    }
}





