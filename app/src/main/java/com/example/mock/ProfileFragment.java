package com.example.mock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private ImageView ivProfilePic, ivEditPic, btnEditFirstName;
    private TextView tvUserName, tvActiveBH, tvViewInfo, tvFirstName;
    private LinearLayout layoutFullInfo;
    private Button btnUpdateProfile, btnHistory;

    // For picking profile image
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        ivProfilePic = view.findViewById(R.id.ivProfilePic);
        ivEditPic = view.findViewById(R.id.ivEditPic);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvActiveBH = view.findViewById(R.id.tvActiveBH);
        tvViewInfo = view.findViewById(R.id.tvViewInfo);
        layoutFullInfo = view.findViewById(R.id.layoutFullInfo);
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile);
        btnHistory = view.findViewById(R.id.btnHistory);

        // Example field
        tvFirstName = view.findViewById(R.id.tvFirstName);
        btnEditFirstName = view.findViewById(R.id.btnEditFirstName);

        // Sample data
        tvUserName.setText("John Doe");
        tvActiveBH.setText("Active Boarding House: Sunshine Dorm");
        tvFirstName.setText("First Name: John");

        // Register ActivityResultLauncher for picking image
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        ivProfilePic.setImageURI(selectedImage);
                    }
                }
        );

        // Edit profile picture
        ivEditPic.setOnClickListener(v -> openImagePicker());

        // Toggle View Info section
        tvViewInfo.setOnClickListener(v -> {
            if (layoutFullInfo.getVisibility() == View.GONE) {
                layoutFullInfo.setVisibility(View.VISIBLE);
                tvViewInfo.setText("Hide Information");
            } else {
                layoutFullInfo.setVisibility(View.GONE);
                tvViewInfo.setText("View Full Information");
            }
        });

        // Handle Edit First Name
        btnEditFirstName.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit First Name clicked", Toast.LENGTH_SHORT).show();
            // TODO: Switch TextView → EditText and allow editing
        });

        // Handle Update Button
        btnUpdateProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Profile Updated!", Toast.LENGTH_SHORT).show();
            // TODO: Save updated profile info to DB / API
        });

        // History Button
        btnHistory.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Open Transaction History", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to HistoryFragment
        });

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }
}
