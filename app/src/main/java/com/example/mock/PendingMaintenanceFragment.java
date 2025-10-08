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
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class PendingMaintenanceFragment extends Fragment {

    private static final String TAG = "PendingMaintenance";
    private static final String GET_MAINTENANCE_REQUESTS_URL = "http://192.168.101.6/BoardEase2/get_maintenance_requests.php";
    private static final String UPDATE_MAINTENANCE_STATUS_URL = "http://192.168.101.6/BoardEase2/update_maintenance_status.php";

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

        MaintenanceApiService apiService = new MaintenanceApiService(getContext());
        apiService.getMaintenanceRequests(userId, "owner", "pending", "all", "all", new MaintenanceApiService.MaintenanceApiCallback() {
            @Override
            public void onSuccess(List<MaintenanceRequest> requests) {
                hideProgressDialog();
                maintenanceRequests.clear();
                maintenanceRequests.addAll(requests);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                Log.d(TAG, "Loaded " + requests.size() + " pending maintenance requests");
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                Log.e(TAG, "Error loading pending maintenance requests: " + error);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void updateMaintenanceStatus(int requestId, String newStatus) {
        showProgressDialog("Updating status...");

        MaintenanceApiService apiService = new MaintenanceApiService(getContext());
        apiService.updateMaintenanceStatus(requestId, newStatus, "", "", "", "", "", "", userId, new MaintenanceApiService.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                hideProgressDialog();
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                // Reload the list to reflect changes
                loadMaintenanceRequests();
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                Log.e(TAG, "Error updating maintenance status: " + error);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
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













