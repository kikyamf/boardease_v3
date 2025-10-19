package com.example.mock;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

    private ImageView ivProfilePic, ivEditProfile;
    private TextView tvOwnerName, tvOwnerEmail, tvSignOut;
    private LinearLayout layoutPayments, layoutNotifications, layoutMessages, layoutAccountSettings, layoutGcashInfo, layoutAboutApp;

    public OwnerProfileFragment() {
        // Required empty public constructor
    }


    public static OwnerProfileFragment newInstance() {
        return new OwnerProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get user ID from login session
        if (getContext() != null) {
            String userIdString = Login.getCurrentUserId(getContext());
            if (userIdString != null && !userIdString.isEmpty()) {
                try {
                    userId = Integer.parseInt(userIdString);
                } catch (NumberFormatException e) {
                    userId = 0; // Default value if parsing fails
                }
            }
        }
    }

    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owner_profile, container, false);

        // Bind Views
        ivProfilePic = view.findViewById(R.id.ivProfilePic);
        ivEditProfile = view.findViewById(R.id.ivEditProfile);
        tvOwnerName = view.findViewById(R.id.tvOwnerName);
        tvOwnerEmail = view.findViewById(R.id.tvOwnerEmail);
        tvSignOut = view.findViewById(R.id.tvSignOut);

        layoutPayments = view.findViewById(R.id.layoutPayments);
        layoutNotifications = view.findViewById(R.id.layoutNotifications);
        layoutMessages = view.findViewById(R.id.layoutMessages);
        layoutAccountSettings = view.findViewById(R.id.layoutAccountSettings);
        layoutGcashInfo = view.findViewById(R.id.layoutGcashInfo);
        layoutAboutApp = view.findViewById(R.id.layoutAboutApp);

        // Load owner profile data
        loadUserData();

        // Click Events
        ivEditProfile.setOnClickListener(v -> openEditProfile());
        ivProfilePic.setOnClickListener(v -> openEditProfile());
        
        layoutPayments.setOnClickListener(v -> Toast.makeText(getContext(), "Open Payments", Toast.LENGTH_SHORT).show());
        layoutNotifications.setOnClickListener(v -> Toast.makeText(getContext(), "Open Notifications", Toast.LENGTH_SHORT).show());
        layoutMessages.setOnClickListener(v -> Toast.makeText(getContext(), "Open Messages", Toast.LENGTH_SHORT).show());
        layoutAccountSettings.setOnClickListener(v -> openAccountSettings());
        layoutGcashInfo.setOnClickListener(v -> openGcashInfo());
        layoutAboutApp.setOnClickListener(v -> Toast.makeText(getContext(), "Open About App", Toast.LENGTH_SHORT).show());

        tvSignOut.setOnClickListener(v -> showSignOutConfirmationDialog());

        return view;
    }
    
    private void openEditProfile() {
        if (userId > 0) {
            Intent intent = new Intent(getContext(), EditOwnerProfileActivity.class);
            intent.putExtra("user_id", userId);
            startActivityForResult(intent, 100); // Use request code 100 for profile edit
        } else {
            Toast.makeText(getContext(), "User ID not available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openAccountSettings() {
        if (userId > 0) {
            Intent intent = new Intent(getContext(), AccountSettingsActivity.class);
            intent.putExtra("user_id", userId);
            startActivityForResult(intent, 200); // Use request code 200 for account settings
        } else {
            Toast.makeText(getContext(), "User ID not available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openGcashInfo() {
        if (userId > 0) {
            Intent intent = new Intent(getContext(), GcashInfoActivity.class);
            intent.putExtra("user_id", userId);
            startActivityForResult(intent, 300); // Use request code 300 for GCash info
        } else {
            Toast.makeText(getContext(), "User ID not available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadUserData() {
        try {
            // Load user data from SharedPreferences
            String userName = Login.getCurrentUserName(getContext());
            String userEmail = Login.getCurrentUserEmail(getContext());
            String userPhone = Login.getCurrentUserPhone(getContext());
            String userAddress = Login.getCurrentUserAddress(getContext());
            String userBirthDate = Login.getCurrentUserBirthDate(getContext());
            String userGcashNumber = Login.getCurrentUserGcashNumber(getContext());
            String userQrCodePath = Login.getCurrentUserQrCodePath(getContext());
            
            if (tvOwnerName != null) {
                if (userName != null && !userName.isEmpty()) {
                    tvOwnerName.setText(userName);
                } else {
                    tvOwnerName.setText("Owner Name"); // Fallback
                }
            }
            if (tvOwnerEmail != null) {
                if (userEmail != null && !userEmail.isEmpty()) {
                    tvOwnerEmail.setText(userEmail);
                } else {
                    tvOwnerEmail.setText("owner@email.com"); // Fallback
                }
            }
            
            // Load profile picture if available
            if (userQrCodePath != null && !userQrCodePath.isEmpty()) {
                // You can load the QR code image here if needed
                // For now, we'll use a default profile picture
                ivProfilePic.setImageResource(R.drawable.btn_profile);
            } else {
                ivProfilePic.setImageResource(R.drawable.btn_profile);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to mock data if there's an error
            if (tvOwnerName != null) {
                tvOwnerName.setText("Owner Name");
            }
            if (tvOwnerEmail != null) {
                tvOwnerEmail.setText("owner@email.com");
            }
            if (ivProfilePic != null) {
                ivProfilePic.setImageResource(R.drawable.btn_profile);
            }
        }
    }
    
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Check if the result is from EditOwnerProfileActivity, AccountSettingsActivity, or GcashInfoActivity
        if ((requestCode == 100 || requestCode == 200 || requestCode == 300) && resultCode == getActivity().RESULT_OK) {
            // Profile, account settings, or GCash info was updated successfully, refresh the data
            loadUserData();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile data when fragment becomes visible
        // This ensures any changes made in other activities are reflected
        loadUserData();
    }
    
    /**
     * Public method to refresh profile data
     * Can be called from parent activity when needed
     */
    public void refreshProfile() {
        loadUserData();
    }
    
    private void showSignOutConfirmationDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Sign Out");
            builder.setMessage("Are you sure you want to sign out?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        // Use the Login.logout() method to properly sign out
                        Login.logout(getContext());
                        Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error signing out", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
