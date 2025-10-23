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
    private static final String GET_OWNER_PROFILE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_owner_profile.php";
    private int userId;

    private ImageView ivProfilePic, ivEditProfile;
    private TextView tvOwnerName, tvOwnerEmail, tvSignOut;
    private LinearLayout layoutPayments, layoutNotifications, layoutMessages, layoutAccountSettings, layoutGcashInfo, layoutAboutApp;

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
        tvSignOut = view.findViewById(R.id.tvSignOut);

        layoutPayments = view.findViewById(R.id.layoutPayments);
        layoutNotifications = view.findViewById(R.id.layoutNotifications);
        layoutMessages = view.findViewById(R.id.layoutMessages);
        layoutAccountSettings = view.findViewById(R.id.layoutAccountSettings);
        layoutGcashInfo = view.findViewById(R.id.layoutGcashInfo);
        layoutAboutApp = view.findViewById(R.id.layoutAboutApp);

        // Load owner profile data
        loadOwnerProfile();

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
        Intent intent = new Intent(getContext(), EditOwnerProfileActivity.class);
        intent.putExtra("user_id", userId);
        startActivityForResult(intent, 100); // Use request code 100 for profile edit
    }
    
    private void openAccountSettings() {
        Intent intent = new Intent(getContext(), AccountSettingsActivity.class);
        intent.putExtra("user_id", userId);
        startActivityForResult(intent, 200); // Use request code 200 for account settings
    }
    
    private void openGcashInfo() {
        Intent intent = new Intent(getContext(), GcashInfoActivity.class);
        intent.putExtra("user_id", userId);
        startActivityForResult(intent, 300); // Use request code 300 for GCash info
    }
    
    private void loadOwnerProfile() {
        StringRequest request = new StringRequest(Request.Method.POST, GET_OWNER_PROFILE_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            populateProfileData(jsonResponse);
                        } else {
                            // Set default data if profile not found
                            tvOwnerName.setText("Owner " + userId);
                            tvOwnerEmail.setText("owner" + userId + "@example.com");
                        }
                    } catch (JSONException e) {
                        Log.e("OwnerProfile", "Error parsing profile response", e);
                        // Set default data on error
                        tvOwnerName.setText("Owner " + userId);
                        tvOwnerEmail.setText("owner" + userId + "@example.com");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("OwnerProfile", "Error loading profile", error);
                    // Set default data on error
                    tvOwnerName.setText("Owner " + userId);
                    tvOwnerEmail.setText("owner" + userId + "@example.com");
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
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
            String fullName = firstName + " " + lastName;
            tvOwnerName.setText(fullName.trim());
            
            // Set email
            String email = profileData.optString("email", "");
            tvOwnerEmail.setText(email);
            
            // Load profile picture
            String profilePicPath = profileData.optString("profile_picture", "");
            if (!profilePicPath.isEmpty()) {
                String fullImageUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/" + profilePicPath;
                Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .centerCrop()
                    .into(ivProfilePic);
            } else {
                // Set default profile picture
                ivProfilePic.setImageResource(R.drawable.btn_profile);
            }
            
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
