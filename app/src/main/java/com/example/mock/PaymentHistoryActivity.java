package com.example.mock;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentHistoryActivity extends AppCompatActivity {
    
    // Views
    private RecyclerView recyclerViewPayments;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout layoutEmpty;
    private ImageView ivBack;
    private Spinner spinnerFilter;
    
    private BoarderPaymentHistoryAdapter adapter;
    private List<PaymentHistoryItem> paymentList;
    private List<PaymentHistoryItem> filteredPaymentList;
    private RequestQueue requestQueue;
    private ProgressDialog progressDialog;
    private int userId;
    
    private String currentFilter = "all";
    private boolean isFromPullRefresh = false;
    
    private static final String BASE_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/";
    private static final String TAG = "PaymentHistoryActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);
        
        // Get user ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userIdString = sharedPreferences.getString("user_id", "0");
        try {
            userId = Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            userId = 0;
        }
        
        if (userId <= 0) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        setupClickListeners();
        setupSpinners();
        
        // Initialize request queue
        requestQueue = Volley.newRequestQueue(this);
        
        // Initialize payment list
        paymentList = new ArrayList<>();
        filteredPaymentList = new ArrayList<>();
        adapter = new BoarderPaymentHistoryAdapter(filteredPaymentList);
        recyclerViewPayments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPayments.setAdapter(adapter);
        
        // Load payment history
        loadPaymentHistory();
    }
    
    private void initializeViews() {
        recyclerViewPayments = findViewById(R.id.recyclerViewPayments);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        layoutEmpty = findViewById(R.id.tvEmpty);
        ivBack = findViewById(R.id.ivBack);
        spinnerFilter = findViewById(R.id.spinnerFilter);
    }
    
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            isFromPullRefresh = true;
            loadPaymentHistory();
        });
    }
    
    private void setupSpinners() {
        // Filter options: All, Successful, Pending Approval
        String[] filterOptions = {"All Payments", "Successful", "Pending Approval"};
        android.widget.ArrayAdapter<String> filterAdapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, filterOptions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(android.graphics.Color.WHITE);
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(android.graphics.Color.WHITE);
                return view;
            }
        };
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);
        
        // Automatic filtering when spinner selection changes
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newFilter = "all";
                switch (position) {
                    case 0: newFilter = "all"; break;
                    case 1: newFilter = "successful"; break;
                    case 2: newFilter = "pending_approval"; break;
                }
                
                // Only reload if filter changed
                if (!newFilter.equals(currentFilter)) {
                    currentFilter = newFilter;
                    loadPaymentHistory();
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void loadPaymentHistory() {
        // Only show loading dialog if NOT from pull refresh
        if (!isFromPullRefresh) {
            showProgressDialog("Loading payment history...");
        }
        
        String url = BASE_URL + "get_boarder_payment_history.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
            params.put("filter_status", currentFilter);
            params.put("sort_by", "newest"); // Default to newest
        } catch (JSONException e) {
            hideProgressDialog();
            swipeRefreshLayout.setRefreshing(false);
            isFromPullRefresh = false;
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    hideProgressDialog();
                    swipeRefreshLayout.setRefreshing(false);
                    isFromPullRefresh = false;
                    try {
                        if (response.getBoolean("success")) {
                            // Parse payments - server already filtered, just display them
                            JSONArray paymentsArray = response.getJSONArray("payments");
                            parsePaymentHistory(paymentsArray);
                        } else {
                            String error = response.optString("error", "Unknown error");
                            Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                },
                error -> {
                    hideProgressDialog();
                    swipeRefreshLayout.setRefreshing(false);
                    isFromPullRefresh = false;
                    Toast.makeText(this, "Error loading payment history: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };
        
        requestQueue.add(request);
    }
    
    private void parsePaymentHistory(JSONArray paymentsArray) {
        paymentList.clear();
        filteredPaymentList.clear();
        
        try {
            android.util.Log.d(TAG, "Parsing " + paymentsArray.length() + " payments with filter: " + currentFilter);
            
            for (int i = 0; i < paymentsArray.length(); i++) {
                JSONObject paymentObj = paymentsArray.getJSONObject(i);
                
                PaymentHistoryItem item = new PaymentHistoryItem();
                item.setBoardingHouseName(paymentObj.optString("boarding_house_name", ""));
                item.setRoomInfo(paymentObj.optString("room_info", ""));
                item.setRoomName(paymentObj.optString("room_name", ""));
                item.setRoomNumber(paymentObj.optString("room_number", ""));
                item.setMonthlyRate(paymentObj.optString("monthly_rate", ""));
                item.setAmount(paymentObj.optString("amount", "₱0.00"));
                item.setRawAmount(paymentObj.optDouble("raw_amount", 0.0));
                item.setPaymentDate(paymentObj.optString("payment_date", ""));
                item.setFormattedDate(formatDate(paymentObj.optString("payment_date", "")));
                item.setDescription(paymentObj.optString("description", ""));
                item.setPaymentMethod(paymentObj.optString("payment_method", ""));
                item.setBhImageUrl(paymentObj.optString("bh_image_url", ""));
                
                // Get status from payments table (this is the key field for filtering)
                String statusFromTable = paymentObj.optString("status", "Pending");
                item.setStatus(statusFromTable);
                
                // Get enhanced payment status
                String enhancedStatus = paymentObj.optString("payment_status", statusFromTable);
                item.setPaymentStatus(enhancedStatus);
                
                item.setDueDate(paymentObj.optString("due_date", ""));
                item.setDaysLate(paymentObj.optInt("days_late", 0));
                
                // Parse payment breakdown
                if (paymentObj.has("payment_breakdown") && !paymentObj.isNull("payment_breakdown")) {
                    JSONArray breakdownArray = paymentObj.getJSONArray("payment_breakdown");
                    List<PaymentBreakdownItem> breakdowns = new ArrayList<>();
                    for (int j = 0; j < breakdownArray.length(); j++) {
                        JSONObject breakdownObj = breakdownArray.getJSONObject(j);
                        PaymentBreakdownItem breakdown = new PaymentBreakdownItem();
                        breakdown.setBreakdownId(breakdownObj.optInt("breakdown_id", 0));
                        breakdown.setPeriodLabel(breakdownObj.optString("period_label", ""));
                        breakdown.setPeriodStartDate(breakdownObj.optString("period_start_date", ""));
                        breakdown.setPeriodEndDate(breakdownObj.optString("period_end_date", ""));
                        breakdown.setAmount(breakdownObj.optString("amount", "₱0.00"));
                        breakdown.setPaymentDate(breakdownObj.optString("breakdown_date", ""));
                        breakdown.setPaid(breakdownObj.optBoolean("is_paid", false));
                        breakdown.setDueDate(breakdownObj.optString("due_date", ""));
                        breakdown.setPaymentStatus(breakdownObj.optString("payment_status", "Pending"));
                        breakdowns.add(breakdown);
                    }
                    item.setPaymentBreakdown(breakdowns);
                } else {
                    item.setPaymentBreakdown(new ArrayList<>());
                }
                
                paymentList.add(item);
                // Add directly to filtered list since server already filtered
                filteredPaymentList.add(item);
                android.util.Log.d(TAG, "Added payment: status=" + statusFromTable + ", paymentStatus=" + enhancedStatus);
            }
            
            android.util.Log.d(TAG, "Total payments parsed: " + paymentList.size() + ", filtered: " + filteredPaymentList.size());
            
            // Sort by newest first
            Collections.sort(filteredPaymentList, new Comparator<PaymentHistoryItem>() {
                @Override
                public int compare(PaymentHistoryItem a, PaymentHistoryItem b) {
                    String dateA = a.getPaymentDate() != null ? a.getPaymentDate() : "";
                    String dateB = b.getPaymentDate() != null ? b.getPaymentDate() : "";
                    return dateB.compareTo(dateA); // Newest first
                }
            });
            
            adapter.notifyDataSetChanged();
            
            if (filteredPaymentList.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
            }
            
        } catch (JSONException e) {
            e.printStackTrace();
            android.util.Log.e(TAG, "Error parsing payment data", e);
            Toast.makeText(this, "Error parsing payment data", Toast.LENGTH_SHORT).show();
            showEmptyState();
        }
    }
    
    
    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || dateString.equals("null")) {
            return "";
        }
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            
            Date date = inputFormat.parse(dateString);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            // Try alternative format
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                
                Date date = inputFormat.parse(dateString);
                if (date != null) {
                    return outputFormat.format(date);
                }
            } catch (ParseException e2) {
                // Return original string if parsing fails
            }
        }
        
        return dateString;
    }
    
    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        recyclerViewPayments.setVisibility(View.GONE);
    }
    
    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        recyclerViewPayments.setVisibility(View.VISIBLE);
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
    
    public static class PaymentHistoryItem implements java.io.Serializable {
        private String boardingHouseName;
        private String roomInfo;
        private String roomName;
        private String roomNumber;
        private String monthlyRate;
        private String amount;
        private double rawAmount;
        private String paymentDate;
        private String formattedDate;
        private String description;
        private String paymentMethod;
        private String bhImageUrl;
        private String status;
        private String paymentStatus;
        private String dueDate;
        private int daysLate;
        private List<PaymentBreakdownItem> paymentBreakdown;
        
        public PaymentHistoryItem() {
            paymentBreakdown = new ArrayList<>();
        }
        
        // Getters
        public String getBoardingHouseName() { return boardingHouseName; }
        public String getRoomInfo() { return roomInfo; }
        public String getRoomName() { return roomName; }
        public String getRoomNumber() { return roomNumber; }
        public String getMonthlyRate() { return monthlyRate; }
        public String getAmount() { return amount; }
        public double getRawAmount() { return rawAmount; }
        public String getPaymentDate() { return paymentDate; }
        public String getFormattedDate() { return formattedDate; }
        public String getDescription() { return description; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getBhImageUrl() { return bhImageUrl; }
        public String getStatus() { return status; }
        public String getPaymentStatus() { return paymentStatus; }
        public String getDueDate() { return dueDate; }
        public int getDaysLate() { return daysLate; }
        public List<PaymentBreakdownItem> getPaymentBreakdown() { return paymentBreakdown; }
        
        // Setters
        public void setBoardingHouseName(String boardingHouseName) { this.boardingHouseName = boardingHouseName; }
        public void setRoomInfo(String roomInfo) { this.roomInfo = roomInfo; }
        public void setRoomName(String roomName) { this.roomName = roomName; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public void setMonthlyRate(String monthlyRate) { this.monthlyRate = monthlyRate; }
        public void setAmount(String amount) { this.amount = amount; }
        public void setRawAmount(double rawAmount) { this.rawAmount = rawAmount; }
        public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
        public void setFormattedDate(String formattedDate) { this.formattedDate = formattedDate; }
        public void setDescription(String description) { this.description = description; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public void setBhImageUrl(String bhImageUrl) { this.bhImageUrl = bhImageUrl; }
        public void setStatus(String status) { this.status = status; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }
        public void setDaysLate(int daysLate) { this.daysLate = daysLate; }
        public void setPaymentBreakdown(List<PaymentBreakdownItem> paymentBreakdown) { this.paymentBreakdown = paymentBreakdown; }
    }
}
