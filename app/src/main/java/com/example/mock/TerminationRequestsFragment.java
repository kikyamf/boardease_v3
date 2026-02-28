package com.example.mock;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TerminationRequestsFragment extends Fragment {

    private static final String TAG = "TerminationRequests";
    private static final String ARG_USER_ID = "user_id";
    private RecyclerView recyclerView;
    private TerminationRequestsAdapter adapter;
    private List<TerminationRequestData> terminationRequests;
    private RequestQueue requestQueue;
    private int ownerId;
    private TextView tvCount;
    private TextView emptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private boolean isInitialLoad = true;

    // Real-time polling
    private final android.os.Handler pollingHandler = new android.os.Handler();
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            loadRequests(false); // silent poll
            pollingHandler.postDelayed(this, 5000);
        }
    };

    private void startPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
        pollingHandler.postDelayed(pollingRunnable, 5000);
    }

    private void stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    public static TerminationRequestsFragment newInstance(int ownerId) {
        TerminationRequestsFragment fragment = new TerminationRequestsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, ownerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_termination_requests, container, false);

        if (getArguments() != null) {
            ownerId = getArguments().getInt(ARG_USER_ID);
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tvCount = view.findViewById(R.id.tvCount);
        emptyState = view.findViewById(R.id.emptyState);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);

        requestQueue = Volley.newRequestQueue(getContext());
        terminationRequests = new ArrayList<>();

        swipeRefreshLayout.setOnRefreshListener(() -> loadRequests(true));

        loadRequests(true); // initial load shows spinner

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
        hideProgressDialog();
    }

    public void loadIfNeeded() {
        if (terminationRequests.isEmpty()) {
            loadRequests(false);
        }
    }

    public void refreshBookings() {
        loadRequests(false);
    }

    private void loadRequests(boolean isRefresh) {
        if (isRefresh) {
            if (isInitialLoad) {
                isInitialLoad = false;
                showProgressDialog("Loading requests...");
            } else {
                swipeRefreshLayout.setRefreshing(true);
            }
        }
        // If polling (isRefresh=false): completely silent, no UI indicator
        String url = "https://boardease.calapebohol.com/get_termination_requests.php?owner_id=" + ownerId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    hideProgressDialog();
                    swipeRefreshLayout.setRefreshing(false);
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray array = response.getJSONObject("data").getJSONArray("termination_requests");
                            terminationRequests.clear();
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                terminationRequests.add(new TerminationRequestData(
                                        obj.getInt("termination_id"),
                                        obj.getInt("booking_id"),
                                        obj.getInt("boarder_id"),
                                        obj.getString("reason"),
                                        obj.getString("details"),
                                        obj.getString("status"),
                                        obj.getString("created_at"),
                                        obj.getString("f_name"),
                                        obj.getString("l_name"),
                                        obj.getString("bh_name"),
                                        obj.getString("room_number"),
                                        obj.getString("room_type")
                                ));
                            }
                            updateUI();
                        } else {
                            Toast.makeText(getContext(), response.getString("error"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    hideProgressDialog();
                    swipeRefreshLayout.setRefreshing(false);
                    String errorMessage = "Error loading requests";
                    if (error.networkResponse != null) {
                        errorMessage += " (Status: " + error.networkResponse.statusCode + ")";
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            android.util.Log.e(TAG, "Load Requests Error Data: " + responseBody);
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error parsing error data", e);
                        }
                    } else if (error.getMessage() != null) {
                        errorMessage += ": " + error.getMessage();
                    }
                    android.util.Log.e(TAG, errorMessage, error);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void updateUI() {
        if (adapter == null) {
            adapter = new TerminationRequestsAdapter(terminationRequests, new TerminationRequestsAdapter.OnActionListener() {
                @Override
                public void onApprove(TerminationRequestData request) {
                    approveRequest(request);
                }

                @Override
                public void onDecline(TerminationRequestData request) {
                    declineRequest(request);
                }
            });
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        tvCount.setText(String.valueOf(terminationRequests.size()));
        emptyState.setVisibility(terminationRequests.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void approveRequest(TerminationRequestData request) {
        showProgressDialog("Approving termination...");
        String url = "https://boardease.calapebohol.com/approve_termination.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    hideProgressDialog();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(getContext(), "Termination approved", Toast.LENGTH_SHORT).show();
                            loadRequests(false);
                        } else {
                            Toast.makeText(getContext(), jsonResponse.getString("error"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    hideProgressDialog();
                    String errorMessage = "Error approving termination";
                    if (error.networkResponse != null) {
                        errorMessage += " (Status: " + error.networkResponse.statusCode + ")";
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            android.util.Log.e(TAG, "Approve Termination Error Data: " + responseBody);
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error parsing error data", e);
                        }
                    } else if (error.getMessage() != null) {
                        errorMessage += ": " + error.getMessage();
                    }
                    android.util.Log.e(TAG, errorMessage, error);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("termination_id", String.valueOf(request.getTerminationId()));
                params.put("owner_id", String.valueOf(ownerId));
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }

    private void declineRequest(TerminationRequestData request) {
        showProgressDialog("Declining termination...");
        String url = "https://boardease.calapebohol.com/decline_termination.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    hideProgressDialog();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(getContext(), "Termination declined", Toast.LENGTH_SHORT).show();
                            loadRequests(false);
                        } else {
                            Toast.makeText(getContext(), jsonResponse.getString("error"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    hideProgressDialog();
                    String errorMessage = "Error declining termination";
                    if (error.networkResponse != null) {
                        errorMessage += " (Status: " + error.networkResponse.statusCode + ")";
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            android.util.Log.e(TAG, "Decline Termination Error Data: " + responseBody);
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error parsing error data", e);
                        }
                    } else if (error.getMessage() != null) {
                        errorMessage += ": " + error.getMessage();
                    }
                    android.util.Log.e(TAG, errorMessage, error);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("termination_id", String.valueOf(request.getTerminationId()));
                params.put("owner_id", String.valueOf(ownerId));
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }

    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
