package com.example.mock;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RentalsFragment extends Fragment {

    private static final String TAG = "RentalsFragment";
    private static final String API_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_rentals_logs.php";

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private RentalsAdapter adapter;
    private List<RentalLog> rentalLogs;
    private int userId;
    private ProgressDialog progressDialog;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rentals, container, false);
        
        // Get user_id from arguments
        if (getArguments() != null) {
            userId = getArguments().getInt("user_id", 0);
        }
        
        // Fallback to SharedPreferences if userId is still 0
        if (userId <= 0 && getContext() != null) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", "0");
            try {
                userId = Integer.parseInt(userIdString);
            } catch (NumberFormatException e) {
                userId = 0;
            }
        }
        
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        rentalLogs = new ArrayList<>();
        adapter = new RentalsAdapter(rentalLogs, this::onRentalClick);
        recyclerView.setAdapter(adapter);
        
        // Load real rental data
        loadRentalsData();
        
        return view;
    }

    private void loadRentalsData() {
        if (isLoading || userId <= 0) {
            return;
        }

        isLoading = true;
        showProgressDialog("Loading boarders...");

        RequestQueue queue = Volley.newRequestQueue(getContext());
        
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, API_URL, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        hideProgressDialog();
                        isLoading = false;
                        Log.d(TAG, "API Response: " + response.toString());
                        
                        try {
                            if (response.getBoolean("success")) {
                                JSONArray rentalsArray = response.getJSONArray("rentals");
                                rentalLogs.clear();
                                
                                for (int i = 0; i < rentalsArray.length(); i++) {
                                    JSONObject rentalObj = rentalsArray.getJSONObject(i);
                                    
                                    // Format booking date for display
                                    String bookingDate = rentalObj.getString("booking_date");
                                    String formattedDate = formatDateTime(bookingDate);
                                    
                                    RentalLog log = new RentalLog(
                                            rentalObj.getString("boarder_name"),
                                            rentalObj.getString("boarding_house_name"),
                                            formattedDate,
                                            rentalObj.getString("status"),
                                            rentalObj.optString("description", ""),
                                            rentalObj.optString("profile_image_url", "")
                                    );
                                    rentalLogs.add(log);
                                }
                                
                                adapter.notifyDataSetChanged();
                                
                                if (rentalLogs.isEmpty()) {
                                    showEmptyState();
                                } else {
                                    hideEmptyState();
                                }
                            } else {
                                String error = response.optString("error", "Failed to load boarders");
                                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                                showEmptyState();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing response", e);
                            Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        isLoading = false;
                        Log.e(TAG, "Volley Error: " + error.getMessage(), error);
                        Toast.makeText(getContext(), "Error loading boarders", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                });

        queue.add(request);
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null && getContext() != null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false);
        }
        if (progressDialog != null) {
            progressDialog.setMessage(message);
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
    }
    
    private String formatDateTime(String dateTime) {
        // Format from "YYYY-MM-DD HH:MM:SS" to "MMM DD, YYYY HH:MM"
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateTime; // Return original if parsing fails
        }
    }

    private void onRentalClick(RentalLog rental) {
        Toast.makeText(getContext(), "Clicked: " + rental.getPropertyName(), Toast.LENGTH_SHORT).show();
        // Handle rental click - could open details dialog or activity
    }

    // Data class for rental logs
    public static class RentalLog {
        private String tenantName;
        private String propertyName;
        private String timestamp;
        private String status;
        private String description;
        private String profileImageUrl;

        public RentalLog(String tenantName, String propertyName, String timestamp, String status, String description, String profileImageUrl) {
            this.tenantName = tenantName;
            this.propertyName = propertyName;
            this.timestamp = timestamp;
            this.status = status;
            this.description = description;
            this.profileImageUrl = profileImageUrl;
        }

        // Getters
        public String getTenantName() { return tenantName; }
        public String getPropertyName() { return propertyName; }
        public String getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
        public String getProfileImageUrl() { return profileImageUrl; }
    }
}
