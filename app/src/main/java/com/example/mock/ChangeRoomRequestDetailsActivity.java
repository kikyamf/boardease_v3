package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChangeRoomRequestDetailsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgProfile;
    private TextView tvBoarderName, tvStatus, tvBhName, tvOldRoom, tvNewRoom, tvReason, tvDetails, tvDate;
    private MaterialButton btnApprove, btnDecline;
    private LinearLayout layoutActions;

    private int requestId;
    private RequestQueue requestQueue;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_room_request_details);

        initializeViews();
        setupClickListeners();
        getIntentData();
        
        requestQueue = Volley.newRequestQueue(this);
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        imgProfile = findViewById(R.id.imgProfile);
        tvBoarderName = findViewById(R.id.tvBoarderName);
        tvStatus = findViewById(R.id.tvStatus);
        tvBhName = findViewById(R.id.tvBhName);
        tvOldRoom = findViewById(R.id.tvOldRoom);
        tvNewRoom = findViewById(R.id.tvNewRoom);
        tvReason = findViewById(R.id.tvReason);
        tvDetails = findViewById(R.id.tvDetails);
        tvDate = findViewById(R.id.tvDate);
        btnApprove = findViewById(R.id.btnApprove);
        btnDecline = findViewById(R.id.btnDecline);
        layoutActions = findViewById(R.id.layoutActions);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnApprove.setOnClickListener(v -> processRequest("Approve"));
        btnDecline.setOnClickListener(v -> processRequest("Decline"));
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            requestId = intent.getIntExtra("request_id", 0);
            tvBoarderName.setText(intent.getStringExtra("boarder_name"));
            tvBhName.setText(intent.getStringExtra("bh_name"));
            tvOldRoom.setText(intent.getStringExtra("old_room_name"));
            tvNewRoom.setText(intent.getStringExtra("new_room_name"));
            tvReason.setText(intent.getStringExtra("reason"));
            tvDetails.setText(intent.getStringExtra("details"));
            tvDate.setText(intent.getStringExtra("created_at"));
            
            String status = intent.getStringExtra("status");
            tvStatus.setText(status);
            
            // Hide actions if already processed
            if (!"Pending".equalsIgnoreCase(status)) {
                layoutActions.setVisibility(View.GONE);
            }
        }
    }

    private void processRequest(String action) {
        showProgressDialog(action + "ing request...");
        String url = "https://boardease.calapebohol.com/process_change_room_request.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    hideProgressDialog();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(this, "Request " + action.toLowerCase() + "d", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Response parsing error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Error processing request", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("change_request_id", String.valueOf(requestId));
                params.put("action", action);
                return params;
            }
        };

        requestQueue.add(stringRequest);
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
