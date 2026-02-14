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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class OwnerProfileFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private static final String GET_OWNER_PROFILE_URL = "https://boardease.calapebohol.com/get_owner_profile.php";
    private int userId;

    private ImageView ivProfilePic, ivEditProfile;
    private TextView tvOwnerName, tvOwnerEmail, tvUserStatus, tvSignOut;
    private LinearLayout layoutNotifications, layoutMessages, layoutAccountSettings, layoutGcashInfo, layoutAboutApp;
    private SwipeRefreshLayout swipeRefreshLayout;

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
        ivEditProfile = view.findViewById(R.id.ivEditProfile);
        tvOwnerName = view.findViewById(R.id.tvOwnerName);
        tvOwnerEmail = view.findViewById(R.id.tvOwnerEmail);
        tvUserStatus = view.findViewById(R.id.tvUserStatus);
        tvSignOut = view.findViewById(R.id.tvSignOut);

        layoutNotifications = view.findViewById(R.id.layoutNotifications);
        layoutMessages = view.findViewById(R.id.layoutMessages);
        layoutAccountSettings = view.findViewById(R.id.layoutAccountSettings);
        layoutGcashInfo = view.findViewById(R.id.layoutGcashInfo);
        layoutAboutApp = view.findViewById(R.id.layoutAboutApp);
        
        // Pull-to-refresh
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        // Set up pull-to-refresh listener
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadOwnerProfile();
            });
        }

        // Load owner profile data
        loadOwnerProfile();

        // Click Events
        ivEditProfile.setOnClickListener(v -> checkRestricted(() -> openEditProfile()));
        ivProfilePic.setOnClickListener(v -> checkRestricted(() -> openEditProfile()));
        
        layoutNotifications.setOnClickListener(v -> checkRestricted(() -> openNotifications()));
        layoutMessages.setOnClickListener(v -> checkRestricted(() -> openMessages()));
        layoutAccountSettings.setOnClickListener(v -> checkRestricted(() -> openAccountSettings()));
        layoutGcashInfo.setOnClickListener(v -> checkRestricted(() -> openGcashInfo()));
        layoutAboutApp.setOnClickListener(v -> showAboutAppDialog());

        tvSignOut.setOnClickListener(v -> showSignOutConfirmationDialog());

        return view;
    }

    private void checkRestricted(Runnable onSuccess) {
        if (getContext() == null) return;
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        String status = prefs.getString("user_status", "approved");

        if ("profile_incomplete".equals(status)) {
            new AlertDialog.Builder(getContext())
                .setTitle("Complete Profile Required")
                .setMessage("You need to complete your profile to access this feature.")
                .setPositiveButton("Complete Now", (dialog, which) -> {
                    Intent intent = new Intent(getContext(), UpdateProfileActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
        } else if ("pending_admin_review".equals(status)) {
             new AlertDialog.Builder(getContext())
                .setTitle("Pending Review")
                .setMessage("Your account is currently under review. Some features are restricted.")
                .setPositiveButton("OK", null)
                .show();
        } else {
            onSuccess.run();
        }
    }
    
    private void openEditProfile() {
        Intent intent = new Intent(getContext(), EditOwnerProfileActivity.class);
        intent.putExtra("user_id", userId);
        startActivityForResult(intent, 100); 
    }
    
    private void openAccountSettings() {
        Intent intent = new Intent(getContext(), AccountSettingsActivity.class);
        intent.putExtra("user_id", userId);
        startActivityForResult(intent, 200); 
    }
    
    private void openGcashInfo() {
        Intent intent = new Intent(getContext(), GcashInfoActivity.class);
        intent.putExtra("user_id", userId);
        startActivityForResult(intent, 300); 
    }
    
    private void openNotifications() {
        try {
            Intent intent = new Intent(getContext(), Notification.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("OwnerProfile", "Error opening notifications", e);
            Toast.makeText(getContext(), "Error opening notifications", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openMessages() {
        try {
            Intent intent = new Intent(getContext(), Messages.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("OwnerProfile", "Error opening messages", e);
            Toast.makeText(getContext(), "Error opening messages", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadOwnerProfile() {
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        String email = prefs.getString("user_email", "");

        StringRequest request = new StringRequest(Request.Method.POST, GET_OWNER_PROFILE_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // Check if fragment is still attached before processing response
                    if (!isAdded() || getContext() == null) {
                        return;
                    }
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            populateProfileData(jsonResponse);
                        } else {
                            // Set default data if profile not found
                            if (isAdded() && tvOwnerName != null && tvOwnerEmail != null) {
                                tvOwnerName.setText("Owner " + userId);
                                tvOwnerEmail.setText("owner" + userId + "@example.com");
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("OwnerProfile", "Error parsing profile response", e);
                        // Set default data on error
                        if (isAdded() && tvOwnerName != null && tvOwnerEmail != null) {
                            tvOwnerName.setText("Owner " + userId);
                            tvOwnerEmail.setText("owner" + userId + "@example.com");
                        }
                    } finally {
                        // Stop refresh indicator
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("OwnerProfile", "Error loading profile", error);
                    // Set default data on error only if fragment is still attached
                    if (isAdded() && tvOwnerName != null && tvOwnerEmail != null) {
                        tvOwnerName.setText("Owner " + userId);
                        tvOwnerEmail.setText("owner" + userId + "@example.com");
                    }
                    // Stop refresh indicator
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                if (!email.isEmpty()) {
                    params.put("email", email);
                }
                return params;
            }
        };
        
        RequestQueue queue = Volley.newRequestQueue(getContext());
        queue.add(request);
    }
    
    private void populateProfileData(JSONObject profileData) {
        try {
            // Set name
            String firstName = profileData.optString("f_name", "");
            String lastName = profileData.optString("l_name", "");
            String suffix = profileData.optString("suffix", "");
            String fullName = firstName + " " + lastName;
            if (suffix != null && !suffix.isEmpty() && !suffix.equals("None")) {
                fullName += " " + suffix;
            }
            if (isAdded() && tvOwnerName != null) {
                tvOwnerName.setText(fullName.trim());
            }
            
            // Set email
            String email = profileData.optString("email", "");
            if (isAdded() && tvOwnerEmail != null) {
                tvOwnerEmail.setText(email);
            }
            
            // Load profile picture
            String profilePicPath = profileData.optString("profile_picture", "");
            if (!profilePicPath.isEmpty()) {
                String fullImageUrl = "https://boardease.calapebohol.com/" + profilePicPath;
                // Check if fragment is still attached before loading image
                if (isAdded() && getContext() != null) {
                    Glide.with(requireContext())
                        .load(fullImageUrl)
                        .placeholder(R.drawable.btn_profile)
                        .error(R.drawable.btn_profile)
                        .centerCrop()
                        .into(ivProfilePic);
                }
            } else {
                // Set default profile picture
                if (isAdded() && ivProfilePic != null) {
                    ivProfilePic.setImageResource(R.drawable.btn_profile);
                }
            }
            
            updateStatusBadge(profileData.optString("status", "approved"));
            
        } catch (Exception e) {
            Log.e("OwnerProfile", "Error populating profile data", e);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Check if the result is from EditOwnerProfileActivity, AccountSettingsActivity, or GcashInfoActivity
        if ((requestCode == 100 || requestCode == 200 || requestCode == 300) && resultCode == getActivity().RESULT_OK) {
            // Profile, account settings, or GCash info was updated successfully, refresh the data
            loadOwnerProfile();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile data when fragment becomes visible
        // This ensures any changes made in other activities are reflected
        loadOwnerProfile();
    }
    
    /**
     * Public method to refresh profile data
     * Can be called from parent activity when needed
     */
    public void refreshProfile() {
        loadOwnerProfile();
    }
    
    private void showAboutAppDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("About BoardEase");
            builder.setMessage("BoardEase v1.0.0\n\n" +
                    "A comprehensive platform for finding and managing boarding house accommodations.\n\n" +
                    "Features:\n" +
                    "• Browse boarding houses\n" +
                    "• Book accommodations\n" +
                    "• Manage favorites\n" +
                    "• Track bookings\n\n" +
                    "© 2024 BoardEase. All rights reserved.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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

    private void updateStatusBadge(String status) {
        if (!isAdded() || tvUserStatus == null) return;
        
        tvUserStatus.setVisibility(View.VISIBLE);
        tvUserStatus.setTextColor(android.graphics.Color.WHITE);
        
        if ("pending_admin_review".equals(status)) {
            tvUserStatus.setText("PENDING REVIEW");
            tvUserStatus.setBackgroundResource(R.drawable.status_badge_pending);
        } else if ("rejected".equals(status)) {
            tvUserStatus.setText("REJECTED");
            tvUserStatus.setBackgroundResource(R.drawable.status_badge_rejected);
        } else if ("approved".equals(status)) {
            tvUserStatus.setText("APPROVED");
            tvUserStatus.setBackgroundResource(R.drawable.status_badge_approved);
        } else if ("profile_incomplete".equals(status)) {
            tvUserStatus.setText("PROFILE INCOMPLETE");
            tvUserStatus.setBackgroundResource(R.drawable.status_badge_incomplete);
        } else {
            tvUserStatus.setVisibility(View.GONE);
        }
    }
}
