package com.example.mock;

import android.app.ProgressDialog;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PendingMaintenanceFragment extends Fragment {

    private static final String TAG = "PendingMaintenance";
    private static final String GET_MAINTENANCE_REQUESTS_URL = "http://192.168.254.121/BoardEase2/get_maintenance_requests.php";
    private static final String UPDATE_MAINTENANCE_STATUS_URL = "http://192.168.254.121/BoardEase2/update_maintenance_status.php";

    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;
    private PendingMaintenanceAdapter adapter;
    private ArrayList<MaintenanceRequest> maintenanceRequests;
    private int userId;
    private ProgressDialog progressDialog;

    public static PendingMaintenanceFragment newInstance(int userId) {
        PendingMaintenanceFragment fragment = new PendingMaintenanceFragment();
        Bundle args = new Bundle();
        args.putInt("user_id", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt("user_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pending_maintenance, container, false);

        initViews(view);
        setupRecyclerView();
        loadMaintenanceRequests();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyLayout = view.findViewById(R.id.emptyLayout);
    }

    private void setupRecyclerView() {
        maintenanceRequests = new ArrayList<>();
        adapter = new PendingMaintenanceAdapter(maintenanceRequests, getContext(), new PendingMaintenanceAdapter.OnStatusUpdateListener() {
            @Override
            public void onStatusUpdate(int requestId, String newStatus) {
                updateMaintenanceStatus(requestId, newStatus);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadMaintenanceRequests() {
        showProgressDialog("Loading pending maintenance requests...");

        StringRequest request = new StringRequest(Request.Method.POST, GET_MAINTENANCE_REQUESTS_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        hideProgressDialog();
                        Log.d(TAG, "Server Response: " + response);
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                JSONArray requestsArray = jsonResponse.getJSONArray("maintenance_requests");
                                maintenanceRequests.clear();

                                for (int i = 0; i < requestsArray.length(); i++) {
                                    JSONObject requestObj = requestsArray.getJSONObject(i);
                                    String status = requestObj.getString("status");
                                    
                                    // Only show pending requests
                                    if ("Pending".equals(status)) {
                                        MaintenanceRequest maintenanceRequest = new MaintenanceRequest(
                                                requestObj.getInt("request_id"),
                                                requestObj.getString("boarder_name"),
                                                requestObj.getString("boarding_house_name"),
                                                requestObj.getString("room_number"),
                                                requestObj.getString("maintenance_type"),
                                                requestObj.getString("description"),
                                                requestObj.getString("request_date"),
                                                requestObj.getString("status"),
                                                requestObj.getString("priority")
                                        );
                                        maintenanceRequests.add(maintenanceRequest);
                                    }
                                }

                                adapter.notifyDataSetChanged();
                                updateEmptyState();
                            } else {
                                Toast.makeText(getContext(), "Failed to load maintenance requests", Toast.LENGTH_SHORT).show();
                                updateEmptyState();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                            updateEmptyState();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        Log.e(TAG, "Volley Error: " + error.getMessage());
                        Toast.makeText(getContext(), "Error loading maintenance requests", Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("status", "pending");
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(getContext());
        queue.add(request);
    }

    private void updateMaintenanceStatus(int requestId, String newStatus) {
        showProgressDialog("Updating status...");

        StringRequest request = new StringRequest(Request.Method.POST, UPDATE_MAINTENANCE_STATUS_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        hideProgressDialog();
                        Log.d(TAG, "Update Response: " + response);
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                Toast.makeText(getContext(), "Status updated successfully", Toast.LENGTH_SHORT).show();
                                // Reload the list to reflect changes
                                loadMaintenanceRequests();
                            } else {
                                Toast.makeText(getContext(), "Failed to update status", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            Toast.makeText(getContext(), "Error updating status", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        Log.e(TAG, "Volley Error: " + error.getMessage());
                        Toast.makeText(getContext(), "Error updating status", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("request_id", String.valueOf(requestId));
                params.put("status", newStatus);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(getContext());
        queue.add(request);
    }

    private void updateEmptyState() {
        if (maintenanceRequests.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}



