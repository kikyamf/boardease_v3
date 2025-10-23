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

public class InProgressMaintenanceFragment extends Fragment {

    private static final String TAG = "InProgressMaintenance";
    private static final String GET_MAINTENANCE_REQUESTS_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_maintenance_requests.php";

    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;
    private InProgressMaintenanceAdapter adapter;
    private ArrayList<MaintenanceRequest> maintenanceRequests;
    private int userId;
    private ProgressDialog progressDialog;

    public static InProgressMaintenanceFragment newInstance(int userId) {
        InProgressMaintenanceFragment fragment = new InProgressMaintenanceFragment();
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
        View view = inflater.inflate(R.layout.fragment_in_progress_maintenance, container, false);

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
        adapter = new InProgressMaintenanceAdapter(maintenanceRequests, getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadMaintenanceRequests() {
        showProgressDialog("Loading maintenance requests...");

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
                                    
                                    // Only show in-progress requests
                                    if ("In Progress".equals(status)) {
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
                params.put("status", "in_progress");
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









