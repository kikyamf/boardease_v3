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

public class PaymentsFragment extends Fragment {

    private static final String TAG = "PaymentsFragment";
    private static final String API_URL = "https://boardease.calapebohol.com/get_payments_logs.php";

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private PaymentsAdapter adapter;
    private List<PaymentLog> paymentLogs;
    private int userId;
    private ProgressDialog progressDialog;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payments, container, false);
        
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
        
        paymentLogs = new ArrayList<>();
        adapter = new PaymentsAdapter(paymentLogs, this::onPaymentClick);
        recyclerView.setAdapter(adapter);
        
        // Load real payment data
        loadPaymentsData();
        
        return view;
    }

    private void loadPaymentsData() {
        if (isLoading || userId <= 0) {
            return;
        }

        isLoading = true;
        showProgressDialog("Loading payments...");

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
                                JSONArray paymentsArray = response.getJSONArray("payments");
                                paymentLogs.clear();
                                
                                for (int i = 0; i < paymentsArray.length(); i++) {
                                    JSONObject paymentObj = paymentsArray.getJSONObject(i);
                                    
                                    // Format payment date for display
                                    String paymentDate = paymentObj.getString("payment_date");
                                    String formattedDate = formatDateTime(paymentDate);
                                    
                                    PaymentLog log = new PaymentLog(
                                            paymentObj.getString("boarder_name"),
                                            paymentObj.getString("boarding_house_name"),
                                            paymentObj.getString("amount"),
                                            formattedDate,
                                            paymentObj.getString("status"),
                                            paymentObj.optString("description", ""),
                                            paymentObj.optString("bh_image_url", "")
                                    );
                                    paymentLogs.add(log);
                                }
                                
                                adapter.notifyDataSetChanged();
                                
                                if (paymentLogs.isEmpty()) {
                                    showEmptyState();
                                } else {
                                    hideEmptyState();
                                }
                            } else {
                                String error = response.optString("error", "Failed to load payments");
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
                        Toast.makeText(getContext(), "Error loading payments", Toast.LENGTH_SHORT).show();
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
        // Format from "YYYY-MM-DD HH:MM:SS" to "MMM DD, YYYY hh:mm a" (e.g., "Jan 15, 2025 02:00 PM")
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateTime; // Return original if parsing fails
        }
    }

    private void onPaymentClick(PaymentLog payment) {
        // Handle payment click - could open details dialog or activity
    }

    // Data class for payment logs
    public static class PaymentLog {
        private String tenantName;
        private String propertyName;
        private String amount;
        private String timestamp;
        private String status;
        private String description;
        private String bhImageUrl;

        public PaymentLog(String tenantName, String propertyName, String amount, String timestamp, String status, String description, String bhImageUrl) {
            this.tenantName = tenantName;
            this.propertyName = propertyName;
            this.amount = amount;
            this.timestamp = timestamp;
            this.status = status;
            this.description = description;
            this.bhImageUrl = bhImageUrl;
        }

        // Getters
        public String getTenantName() { return tenantName; }
        public String getPropertyName() { return propertyName; }
        public String getAmount() { return amount; }
        public String getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
        public String getBhImageUrl() { return bhImageUrl; }
    }
}


































