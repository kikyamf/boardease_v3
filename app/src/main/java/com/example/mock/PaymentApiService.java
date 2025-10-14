package com.example.mock;

import android.content.Context;
import android.util.Log;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentApiService {
    private static final String TAG = "PaymentApiService";
    private static final String BASE_URL = "http://192.168.101.6/BoardEase2/";
    
    private Context context;
    private RequestQueue requestQueue;

    public PaymentApiService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    // Interface for API callbacks
    public interface PaymentListCallback {
        void onSuccess(List<PaymentData> payments);
        void onError(String error);
    }

    public interface PaymentSummaryCallback {
        void onSuccess(PaymentSummary summary);
        void onError(String error);
    }

    public interface PaymentUpdateCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // Get all payments
    public void getAllPayments(int ownerId, PaymentListCallback callback) {
        String url = BASE_URL + "get_payment_status.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("owner_id", ownerId);
            params.put("status", "all");
        } catch (JSONException e) {
            callback.onError("Error creating request parameters");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            List<PaymentData> payments = parsePaymentList(data.getJSONArray("payments"));
                            callback.onSuccess(payments);
                        } else {
                            callback.onError(response.getString("error"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error parsing response");
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error", error);
                    callback.onError("Network error: " + error.getMessage());
                });

        requestQueue.add(request);
    }

    // Get pending payments
    public void getPendingPayments(int ownerId, PaymentListCallback callback) {
        String url = BASE_URL + "get_payment_status.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("owner_id", ownerId);
            params.put("status", "pending");
        } catch (JSONException e) {
            callback.onError("Error creating request parameters");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            List<PaymentData> payments = parsePaymentList(data.getJSONArray("payments"));
                            callback.onSuccess(payments);
                        } else {
                            callback.onError(response.getString("error"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error parsing response");
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error", error);
                    callback.onError("Network error: " + error.getMessage());
                });

        requestQueue.add(request);
    }

    // Get completed payments
    public void getCompletedPayments(int ownerId, PaymentListCallback callback) {
        String url = BASE_URL + "get_payment_status.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("owner_id", ownerId);
            params.put("status", "paid");
        } catch (JSONException e) {
            callback.onError("Error creating request parameters");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            List<PaymentData> payments = parsePaymentList(data.getJSONArray("payments"));
                            callback.onSuccess(payments);
                        } else {
                            callback.onError(response.getString("error"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error parsing response");
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error", error);
                    callback.onError("Network error: " + error.getMessage());
                });

        requestQueue.add(request);
    }

    // Get overdue payments
    public void getOverduePayments(int ownerId, PaymentListCallback callback) {
        String url = BASE_URL + "get_payment_status.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("owner_id", ownerId);
            params.put("status", "overdue");
        } catch (JSONException e) {
            callback.onError("Error creating request parameters");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            List<PaymentData> payments = parsePaymentList(data.getJSONArray("payments"));
                            callback.onSuccess(payments);
                        } else {
                            callback.onError(response.getString("error"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error parsing response");
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error", error);
                    callback.onError("Network error: " + error.getMessage());
                });

        requestQueue.add(request);
    }

    // Update payment status
    public void updatePaymentStatus(int paymentId, String newStatus, String notes, PaymentUpdateCallback callback) {
        String url = BASE_URL + "update_payment_status.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("payment_id", paymentId);
            params.put("status", newStatus);
            params.put("notes", notes);
        } catch (JSONException e) {
            callback.onError("Error creating request parameters");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            callback.onSuccess(response.getString("message"));
                        } else {
                            callback.onError(response.getString("error"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error parsing response");
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error", error);
                    callback.onError("Network error: " + error.getMessage());
                });

        requestQueue.add(request);
    }

    // Get payment summary
    public void getPaymentSummary(int ownerId, String period, PaymentSummaryCallback callback) {
        String url = BASE_URL + "get_payment_summary.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("owner_id", ownerId);
            params.put("period", period);
        } catch (JSONException e) {
            callback.onError("Error creating request parameters");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            PaymentSummary summary = parsePaymentSummary(response.getJSONObject("data"));
                            callback.onSuccess(summary);
                        } else {
                            callback.onError(response.getString("error"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error parsing response");
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error", error);
                    callback.onError("Network error: " + error.getMessage());
                });

        requestQueue.add(request);
    }

    // Parse payment list from JSON array
    private List<PaymentData> parsePaymentList(JSONArray jsonArray) {
        List<PaymentData> payments = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject paymentJson = jsonArray.getJSONObject(i);
                PaymentData payment = PaymentData.fromJson(paymentJson);
                if (payment != null) {
                    payments.add(payment);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing payment list", e);
        }
        return payments;
    }

    // Parse payment summary from JSON object
    private PaymentSummary parsePaymentSummary(JSONObject json) {
        try {
            return new PaymentSummary(
                json.optInt("total_payments", 0),
                json.optInt("pending_payments", 0),
                json.optInt("paid_payments", 0),
                json.optInt("overdue_payments", 0),
                json.optDouble("total_amount", 0.0),
                json.optDouble("pending_amount", 0.0),
                json.optDouble("paid_amount", 0.0),
                json.optDouble("overdue_amount", 0.0),
                json.optDouble("collection_rate", 0.0)
            );
        } catch (Exception e) {
            Log.e(TAG, "Error parsing payment summary", e);
            return null;
        }
    }

    // Payment Summary model class
    public static class PaymentSummary {
        private int totalPayments;
        private int pendingPayments;
        private int paidPayments;
        private int overduePayments;
        private double totalAmount;
        private double pendingAmount;
        private double paidAmount;
        private double overdueAmount;
        private double collectionRate;

        public PaymentSummary(int totalPayments, int pendingPayments, int paidPayments, int overduePayments,
                             double totalAmount, double pendingAmount, double paidAmount, double overdueAmount,
                             double collectionRate) {
            this.totalPayments = totalPayments;
            this.pendingPayments = pendingPayments;
            this.paidPayments = paidPayments;
            this.overduePayments = overduePayments;
            this.totalAmount = totalAmount;
            this.pendingAmount = pendingAmount;
            this.paidAmount = paidAmount;
            this.overdueAmount = overdueAmount;
            this.collectionRate = collectionRate;
        }

        // Getters
        public int getTotalPayments() { return totalPayments; }
        public int getPendingPayments() { return pendingPayments; }
        public int getPaidPayments() { return paidPayments; }
        public int getOverduePayments() { return overduePayments; }
        public double getTotalAmount() { return totalAmount; }
        public double getPendingAmount() { return pendingAmount; }
        public double getPaidAmount() { return paidAmount; }
        public double getOverdueAmount() { return overdueAmount; }
        public double getCollectionRate() { return collectionRate; }
    }
}
