package com.example.mock;

import android.app.ProgressDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class InProgressMaintenanceFragment extends Fragment {

    private static final String TAG = "InProgressMaintenance";

    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;
    private TextView tvCount;
    private LinearLayout headerLayout;
    private ImageView ivHeaderIcon;
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
        tvCount = view.findViewById(R.id.tvCount);
        headerLayout = view.findViewById(R.id.headerLayout);
        ivHeaderIcon = view.findViewById(R.id.ivHeaderIcon);
        
        // Set header styling (blue for In Progress)
        if (headerLayout != null) {
            headerLayout.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD")); // Light blue
        }
        if (ivHeaderIcon != null) {
            ivHeaderIcon.setColorFilter(android.graphics.Color.parseColor("#2196F3")); // Blue
        }
        if (tvCount != null) {
            tvCount.setBackgroundResource(R.drawable.bg_rounded_blue);
        }
    }

    private void setupRecyclerView() {
        maintenanceRequests = new ArrayList<>();
        adapter = new InProgressMaintenanceAdapter(maintenanceRequests, getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadMaintenanceRequests() {
        showProgressDialog("Loading maintenance requests...");

        MaintenanceApiService apiService = new MaintenanceApiService(getContext());
        apiService.getMaintenanceRequests(userId, "owner", "in_progress", "all", "all", new MaintenanceApiService.MaintenanceApiCallback() {
            @Override
            public void onSuccess(List<MaintenanceRequest> requests) {
                hideProgressDialog();
                maintenanceRequests.clear();
                maintenanceRequests.addAll(requests);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                Log.d(TAG, "Loaded " + requests.size() + " in-progress maintenance requests");
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                Log.e(TAG, "Error loading in-progress maintenance requests: " + error);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        // Update count
        if (tvCount != null) {
            tvCount.setText(String.valueOf(maintenanceRequests.size()));
        }
        
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










