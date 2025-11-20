package com.example.mock;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GcashInfoActivity extends AppCompatActivity {

    private static final String TAG = "GcashInfo";
    private static final String GET_GCASH_INFO_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_gcash_info.php";
    private static final String UPDATE_GCASH_INFO_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/update_gcash_info.php";

    private int userId;
    private ProgressDialog progressDialog;

    // Views
    private EditText etGcashNumber;
    private Button btnSaveChanges;
    private ImageView btnBack;

    // Data
    private String currentGcashNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gcash_info);

        // Get userId from intent
        userId = getIntent().getIntExtra("user_id", 0);

        initViews();
        setupClickListeners();
        loadGcashInfo();
    }

    private void initViews() {
        // Back button
        btnBack = findViewById(R.id.btnBack);

        // GCash fields
        etGcashNumber = findViewById(R.id.etGcashNumber);

        // Update button
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSaveChanges.setOnClickListener(v -> saveGcashChanges());
    }

    private void loadGcashInfo() {
        showProgressDialog("Loading GCash information...");

        StringRequest request = new StringRequest(Request.Method.POST, GET_GCASH_INFO_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    hideProgressDialog();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            populateGcashData(jsonResponse);
                        } else {
                            Toast.makeText(GcashInfoActivity.this, "Failed to load GCash info", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing GCash info response", e);
                        Toast.makeText(GcashInfoActivity.this, "Error loading GCash info", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideProgressDialog();
                    Log.e(TAG, "Error loading GCash info", error);
                    Toast.makeText(GcashInfoActivity.this, "Error loading GCash info", Toast.LENGTH_SHORT).show();
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

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void populateGcashData(JSONObject gcashData) {
        try {
            // Set GCash number
            currentGcashNumber = gcashData.optString("gcash_number", "");
            if (currentGcashNumber == null || currentGcashNumber.isEmpty() || currentGcashNumber.equalsIgnoreCase("null")) {
                currentGcashNumber = "";
            }
            etGcashNumber.setText(currentGcashNumber);

        } catch (Exception e) {
            Log.e(TAG, "Error populating GCash data", e);
        }
    }

    private void saveGcashChanges() {
        String gcashNumber = etGcashNumber.getText().toString().trim();

        // Validation
        if (gcashNumber.isEmpty()) {
            etGcashNumber.setError("GCash number is required");
            etGcashNumber.requestFocus();
            return;
        }

        // Validate phone number format (should be 11 digits for Philippines)
        if (!gcashNumber.matches("^09\\d{9}$")) {
            etGcashNumber.setError("Please enter a valid GCash number (09XXXXXXXXX)");
            etGcashNumber.requestFocus();
            return;
        }

        showProgressDialog("Updating GCash information...");

        // Update GCash number
        updateGcashNumber(gcashNumber);
    }
    
    private void updateGcashNumber(String gcashNumber) {
        StringRequest request = new StringRequest(Request.Method.POST, UPDATE_GCASH_INFO_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    hideProgressDialog();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(GcashInfoActivity.this, "GCash information updated successfully", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            String error = jsonResponse.optString("error", "Failed to update GCash information");
                            Toast.makeText(GcashInfoActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing update response", e);
                        Toast.makeText(GcashInfoActivity.this, "Error updating GCash information", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideProgressDialog();
                    Log.e(TAG, "Error updating GCash information", error);
                    Toast.makeText(GcashInfoActivity.this, "Error updating GCash information", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("gcash_number", gcashNumber);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
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
















