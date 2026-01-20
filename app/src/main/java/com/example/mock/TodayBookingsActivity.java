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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TodayBookingsActivity extends AppCompatActivity {

    private static final String TAG = "TodayBookingsActivity";
    private static final String API_URL = "https://boardease.calapebohol.com/get_today_bookings.php";

    private int userId;
    private ProgressDialog progressDialog;
    private boolean isLoading = false;

    // Views
    private ImageButton btnBack;
    private TextView tvTitle;
    private LinearLayout tvEmptyState;
    private RecyclerView recyclerView;

    // Data
    private List<TodayBooking> bookingsList;
    private TodayBookingsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today_bookings);

        // Get userId from intent
        userId = getIntent().getIntExtra("user_id", 0);
        Log.d(TAG, "Received user_id from intent: " + userId);

        initViews();
        setupClickListeners();
        loadTodayBookings();
    }

    private void initViews() {
        // Back button
        btnBack = findViewById(R.id.btnBack);

        // Title
        tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Today's Bookings");

        // RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Empty state
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // Initialize data
        bookingsList = new ArrayList<>();
        adapter = new TodayBookingsAdapter(bookingsList);
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadTodayBookings() {
        if (isLoading) {
            return;
        }

        if (userId <= 0) {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        isLoading = true;
        showProgressDialog("Loading today's bookings...");

        // Create request with POST method
        RequestQueue queue = Volley.newRequestQueue(this);
        
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
                                JSONArray bookingsArray = response.getJSONArray("bookings");
                                bookingsList.clear();
                                
                                for (int i = 0; i < bookingsArray.length(); i++) {
                                    JSONObject bookingObj = bookingsArray.getJSONObject(i);
                                    TodayBooking booking = new TodayBooking(
                                            bookingObj.getInt("booking_id"),
                                            bookingObj.getString("boarder_name"),
                                            bookingObj.getString("boarding_house_name"),
                                            bookingObj.getString("room_name"),
                                            bookingObj.getString("start_date"),
                                            bookingObj.getString("end_date"),
                                            bookingObj.getString("amount"),
                                            bookingObj.getString("booking_date"),
                                            bookingObj.getString("profile_image")
                                    );
                                    bookingsList.add(booking);
                                }
                                
                                adapter.notifyDataSetChanged();
                                
                                if (bookingsList.isEmpty()) {
                                    showEmptyState();
                                } else {
                                    hideEmptyState();
                                }
                            } else {
                                String error = response.optString("error", "Failed to load bookings");
                                Toast.makeText(TodayBookingsActivity.this, error, Toast.LENGTH_SHORT).show();
                                showEmptyState();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing response", e);
                            Toast.makeText(TodayBookingsActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(TodayBookingsActivity.this, "Error loading bookings", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                });

        queue.add(request);
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
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

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
    }

    // Data model class
    public static class TodayBooking {
        private int bookingId;
        private String boarderName;
        private String boardingHouseName;
        private String roomName;
        private String startDate;
        private String endDate;
        private String amount;
        private String bookingDate;
        private String profileImage;

        public TodayBooking(int bookingId, String boarderName, String boardingHouseName, 
                           String roomName, String startDate, String endDate, String amount,
                           String bookingDate, String profileImage) {
            this.bookingId = bookingId;
            this.boarderName = boarderName;
            this.boardingHouseName = boardingHouseName;
            this.roomName = roomName;
            this.startDate = startDate;
            this.endDate = endDate;
            this.amount = amount;
            this.bookingDate = bookingDate;
            this.profileImage = profileImage;
        }

        // Getters
        public int getBookingId() { return bookingId; }
        public String getBoarderName() { return boarderName; }
        public String getBoardingHouseName() { return boardingHouseName; }
        public String getRoomName() { return roomName; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getAmount() { return amount; }
        public String getBookingDate() { return bookingDate; }
        public String getProfileImage() { return profileImage; }
    }
}

