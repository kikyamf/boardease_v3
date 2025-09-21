package com.example.mock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AddingRoomsFragment extends Fragment {

    private LinearLayout containerPrivateRooms, containerBedSpacers;

    public AddingRoomsFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_adding_rooms, container, false);

        // Reference containers
        containerPrivateRooms = view.findViewById(R.id.containerPrivateRooms);
        containerBedSpacers = view.findViewById(R.id.containerBedSpacers);

        // Back button → return to AddingBhFragment
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Private Room +
        ImageButton btnAddPrivateRoom = view.findViewById(R.id.btnAddPrivateRoom);
        btnAddPrivateRoom.setOnClickListener(v -> {
            View privateForm = inflater.inflate(R.layout.item_private_form, containerPrivateRooms, false);

            // Handle delete inside the private form
            ImageButton btnRemovePrivate = privateForm.findViewById(R.id.btnRemoveRoom);
            if (btnRemovePrivate != null) {
                btnRemovePrivate.setOnClickListener(x -> containerPrivateRooms.removeView(privateForm));
            }

            containerPrivateRooms.addView(privateForm);
        });

        // Bed Spacer +
        ImageButton btnAddBedSpacer = view.findViewById(R.id.btnAddBedSpacer);
        btnAddBedSpacer.setOnClickListener(v -> {
            View bedForm = inflater.inflate(R.layout.item_bed_form, containerBedSpacers, false);

            // Handle delete inside the bed form
            ImageButton btnRemoveBed = bedForm.findViewById(R.id.btnRemoveBed);
            if (btnRemoveBed != null) {
                btnRemoveBed.setOnClickListener(x -> containerBedSpacers.removeView(bedForm));
            }

            containerBedSpacers.addView(bedForm);
        });

        // Save All Button (example listener)
        Button btnSaveAll = view.findViewById(R.id.btnSaveAll);
        btnSaveAll.setOnClickListener(v -> {
            // TODO: loop through all forms and save to database
            // Example: containerBedSpacers.getChildCount()
        });

        return view;
    }
}
