package com.example.mock;


import android.annotation.SuppressLint;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OwnerProfileFragment extends Fragment {


    private static final String ARG_USER_ID = "user_id";
    private int userId;

    private ImageView ivProfilePic;
    private TextView tvOwnerName, tvOwnerEmail, tvSignOut;
    private LinearLayout layoutPayments, layoutNotifications, layoutMessages, layoutSettings, layoutAboutApp;

    public OwnerProfileFragment() {
        // Required empty public constructor
    }


    public static OwnerProfileFragment newInstance(int userId) {
        OwnerProfileFragment fragment = new OwnerProfileFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID);
        }
    }

    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owner_profile, container, false);

        // Bind Views
        ivProfilePic = view.findViewById(R.id.ivProfilePic);
        tvOwnerName = view.findViewById(R.id.tvOwnerName);
        tvOwnerEmail = view.findViewById(R.id.tvOwnerEmail);
        tvSignOut = view.findViewById(R.id.tvSignOut);

        layoutPayments = view.findViewById(R.id.layoutPayments);
        layoutNotifications = view.findViewById(R.id.layoutNotifications);
        layoutMessages = view.findViewById(R.id.layoutMessages);
        layoutSettings = view.findViewById(R.id.layoutSettings);
        layoutAboutApp = view.findViewById(R.id.layoutAboutApp);

        // Example: set user data
        tvOwnerName.setText("Owner " + userId);
        tvOwnerEmail.setText("owner" + userId + "@example.com");

        // Click Events
        layoutPayments.setOnClickListener(v -> Toast.makeText(getContext(), "Open Payments", Toast.LENGTH_SHORT).show());
        layoutNotifications.setOnClickListener(v -> Toast.makeText(getContext(), "Open Notifications", Toast.LENGTH_SHORT).show());
        layoutMessages.setOnClickListener(v -> Toast.makeText(getContext(), "Open Messages", Toast.LENGTH_SHORT).show());
        layoutSettings.setOnClickListener(v -> Toast.makeText(getContext(), "Open Settings", Toast.LENGTH_SHORT).show());
        layoutAboutApp.setOnClickListener(v -> Toast.makeText(getContext(), "Open About App", Toast.LENGTH_SHORT).show());

        tvSignOut.setOnClickListener(v -> Toast.makeText(getContext(), "Signing Out...", Toast.LENGTH_SHORT).show());

        return view;

    }
}
