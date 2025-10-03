package com.example.mock;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoardersListActivity extends AppCompatActivity {

    private static final String TAG = "BoardersList";
    private static final String GET_BOARDERS_URL = "http://192.168.254.121/BoardEase2/get_owner_boarders.php";

    private int userId;
    private ProgressDialog progressDialog;
    private boolean isLoading = false;

    // Views
    private ImageButton btnBack;
    private TextView tvTitle;
    private LinearLayout tvEmptyState;
    private RecyclerView recyclerView;

    // Data
    private List<BoarderData> boardersList;
    private BoardersListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarders_list);

        // Get userId from intent
        userId = getIntent().getIntExtra("user_id", 0);
        Log.d(TAG, "Received user_id from intent: " + userId);

        initViews();
        setupClickListeners();
        loadBoarders();
    }

    private void initViews() {
        // Back button
        btnBack = findViewById(R.id.btnBack);

        // Title
        tvTitle = findViewById(R.id.tvTitle);

        // RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Empty state
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // Initialize data
        boardersList = new ArrayList<>();
        adapter = new BoardersListAdapter(boardersList);
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadBoarders() {
        // Prevent multiple simultaneous requests
        if (isLoading) {
            Log.d(TAG, "Already loading boarders, skipping request");
            return;
        }
        
        isLoading = true;
        showProgressDialog("Loading boarders...");

        StringRequest request = new StringRequest(Request.Method.POST, GET_BOARDERS_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    isLoading = false;
                    hideProgressDialog();
                    Log.d(TAG, "Server Response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray boardersArray = jsonResponse.getJSONArray("boarders");
                            boardersList.clear();
                            
                            // Check if there are any boarders
                            if (boardersArray.length() > 0) {
                                for (int i = 0; i < boardersArray.length(); i++) {
                                    JSONObject boarderObj = boardersArray.getJSONObject(i);
                                    BoarderData boarder = new BoarderData(
                                        boarderObj.optInt("boarder_id", 0),
                                        boarderObj.optString("boarder_name", ""),
                                        boarderObj.optString("boarder_email", ""),
                                        boarderObj.optString("boarder_phone", ""),
                                        boarderObj.optString("boarding_house_name", ""),
                                        boarderObj.optString("room_number", ""),
                                        boarderObj.optString("rent_type", ""),
                                        boarderObj.optString("start_date", ""),
                                        boarderObj.optString("end_date", ""),
                                        boarderObj.optString("status", ""),
                                        boarderObj.optString("profile_picture", "")
                                    );
                                    boardersList.add(boarder);
                                }
                            }
                            
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                            
                        } else {
                            String error = jsonResponse.optString("error", "Failed to load boarders");
                            Log.e(TAG, "Server Error: " + error);
                            Log.e(TAG, "Full Response: " + response);
                            Toast.makeText(BoardersListActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing boarders response", e);
                        Toast.makeText(BoardersListActivity.this, "Error loading boarders", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    isLoading = false;
                    hideProgressDialog();
                    Log.e(TAG, "Volley Error: " + error.getMessage(), error);
                    Log.e(TAG, "Error Type: " + error.getClass().getSimpleName());
                    
                    String errorMessage = "Error loading boarders";
                    if (error.networkResponse != null) {
                        errorMessage += " (HTTP " + error.networkResponse.statusCode + ")";
                        Log.e(TAG, "HTTP Status: " + error.networkResponse.statusCode);
                        Log.e(TAG, "Response Data: " + new String(error.networkResponse.data));
                    }
                    
                    Toast.makeText(BoardersListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    
                    // Show empty state on error
                    updateEmptyState();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                Log.d(TAG, "Sending user_id: " + userId);
                Log.d(TAG, "Params: " + params.toString());
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void updateEmptyState() {
        if (boardersList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
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
