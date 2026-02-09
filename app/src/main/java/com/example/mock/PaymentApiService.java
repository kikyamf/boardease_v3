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
    private static final String BASE_URL = "https://boardease.calapebohol.com/";
    
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
        void onSuccess(String message, String newStatus);
        void onError(String error);
    }

    public interface PaymentCalendarCallback {
        void onSuccess(List<CalendarPaymentData> calendarData);
        void onError(String error);
    }

    public interface OverdueUpdateCallback {
        void onSuccess(String message, int totalUpdated, int notificationsSent, int notificationsSkipped);
        void onError(String error);
    }

    public interface PaymentReminderCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // Get all payments
    public void getAllPayments(int ownerId, PaymentListCallback callback) {
        String url = BASE_URL + "get_payment_status.php";
        
        Log.d(TAG, "getAllPayments - Requesting payments for ownerId: " + ownerId);
        
        JSONObject params = new JSONObject();
        try {
            params.put("owner_id", ownerId);
            params.put("status", "all");
            Log.d(TAG, "getAllPayments - Request params: " + params.toString());
        } catch (JSONException e) {
            Log.e(TAG, "getAllPayments - Error creating request parameters", e);
            callback.onError("Error creating request parameters");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        Log.d(TAG, "getAllPayments - Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray paymentsArray = data.getJSONArray("payments");
                            Log.d(TAG, "getAllPayments - Found " + paymentsArray.length() + " payments in response");
                            
                            // Check debug info if available
                            if (response.has("debug")) {
                                try {
                                    JSONObject debug = response.getJSONObject("debug");
                                    Log.d(TAG, "getAllPayments - Debug info: requested_owner_id=" + 
                                          debug.optInt("requested_owner_id", 0) + 
                                          ", simple_query_count=" + debug.optInt("simple_query_count", 0) +
                                          ", main_query_count=" + debug.optInt("main_query_count", 0));
                                } catch (JSONException e) {
                                    Log.e(TAG, "getAllPayments - Error parsing debug info", e);
                                }
                            }
                            
                            List<PaymentData> payments = parsePaymentList(paymentsArray);
                            Log.d(TAG, "getAllPayments - Parsed " + payments.size() + " payments");
                            callback.onSuccess(payments);
                        } else {
                            String errorMsg = response.optString("error", "Unknown error");
                            Log.e(TAG, "getAllPayments - Server returned error: " + errorMsg);
                            callback.onError(errorMsg);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "getAllPayments - Error parsing response", e);
                        Log.e(TAG, "getAllPayments - Response was: " + response.toString());
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "getAllPayments - Volley error", error);
                    String errorMsg = "Network error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error");
                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "getAllPayments - Error response body: " + responseBody);
                            errorMsg = "Server error (" + error.networkResponse.statusCode + "): " + responseBody;
                        } catch (Exception e) {
                            Log.e(TAG, "getAllPayments - Error parsing error response", e);
                        }
                    }
                    callback.onError(errorMsg);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };

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
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };

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
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    // Get fully paid bookings (all periods paid)
    public void getFullyPaidPayments(int ownerId, PaymentListCallback callback) {
        String url = BASE_URL + "get_payment_status.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("owner_id", ownerId);
            params.put("status", "fully_paid");
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
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    // Get remaining payments (bookings with unpaid periods)
    public void getRemainingPayments(int ownerId, PaymentListCallback callback) {
        String url = BASE_URL + "get_payment_status.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("owner_id", ownerId);
            params.put("status", "remaining");
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
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };

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
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };

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
            Log.d(TAG, "updatePaymentStatus - Request params: " + params.toString());
        } catch (JSONException e) {
            Log.e(TAG, "updatePaymentStatus - Error creating request parameters", e);
            callback.onError("Error creating request parameters");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        Log.d(TAG, "updatePaymentStatus - Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            String message = response.optString("message", "Payment status updated successfully");
                            // Get the new payment status from response
                            String updatedStatus = null;
                            if (response.has("status")) {
                                updatedStatus = response.getString("status");
                            } else if (response.has("data")) {
                                JSONObject data = response.getJSONObject("data");
                                if (data.has("new_status")) {
                                    updatedStatus = data.getString("new_status");
                                }
                            }
                            callback.onSuccess(message, updatedStatus);
                        } else {
                            String errorMsg = response.optString("error", "Unknown error occurred");
                            Log.e(TAG, "updatePaymentStatus - Server returned error: " + errorMsg);
                            callback.onError(errorMsg);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "updatePaymentStatus - Error parsing response", e);
                        callback.onError("Error parsing response");
                    }
                },
                error -> {
                    Log.e(TAG, "updatePaymentStatus - Volley error", error);
                    String errorMsg = "Network error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error");
                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "updatePaymentStatus - Error response body: " + responseBody);
                            errorMsg = "Server error (" + error.networkResponse.statusCode + "): " + responseBody;
                        } catch (Exception e) {
                            Log.e(TAG, "updatePaymentStatus - Error parsing error response", e);
                        }
                    }
                    callback.onError(errorMsg);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    // Get payment breakdown for a booking
    public void getPaymentBreakdown(int bookingId, PaymentBreakdownCallback callback) {
        String url = BASE_URL + "get_payment_breakdown.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("booking_id", bookingId);
        } catch (JSONException e) {
            callback.onError("Error creating request parameters");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            List<PaymentBreakdownItem> breakdowns = parseBreakdownList(data.getJSONArray("breakdowns"));
                            callback.onSuccess(breakdowns);
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
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    // Interface for payment breakdown callback
    public interface PaymentBreakdownCallback {
        void onSuccess(List<PaymentBreakdownItem> breakdowns);
        void onError(String error);
    }

    // Parse breakdown list from JSON array
    private List<PaymentBreakdownItem> parseBreakdownList(JSONArray jsonArray) {
        List<PaymentBreakdownItem> breakdowns = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject breakdownJson = jsonArray.getJSONObject(i);
                PaymentBreakdownItem item = new PaymentBreakdownItem(
                    breakdownJson.optInt("breakdown_id", 0),
                    breakdownJson.optInt("booking_id", 0),
                    breakdownJson.isNull("payment_id") ? null : breakdownJson.optInt("payment_id", 0),
                    breakdownJson.optString("period_type", ""),
                    breakdownJson.optInt("period_number", 0),
                    breakdownJson.optString("period_label", ""),
                    breakdownJson.optString("period_start_date", ""),
                    breakdownJson.optString("period_end_date", ""),
                    breakdownJson.optString("amount", "0.00"),
                    breakdownJson.optBoolean("is_selected", false),
                    breakdownJson.optBoolean("is_paid", false),
                    breakdownJson.optString("due_date", ""),
                    breakdownJson.optString("payment_status", "Pending"),
                    breakdownJson.optString("payment_date", ""),
                    breakdownJson.optString("payment_proof", "")
                );
                breakdowns.add(item);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing breakdown list", e);
        }
        return breakdowns;
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

    // Get payments for calendar view
    public void getPaymentsCalendar(int ownerId, int month, int year, PaymentCalendarCallback callback) {
        String url = BASE_URL + "get_payments_calendar.php";
        
        Log.d(TAG, "getPaymentsCalendar - Requesting calendar data for ownerId: " + ownerId + ", month: " + month + ", year: " + year);
        
        JSONObject params = new JSONObject();
        try {
            params.put("owner_id", ownerId);
            params.put("month", month);
            params.put("year", year);
        } catch (JSONException e) {
            Log.e(TAG, "getPaymentsCalendar - Error creating request parameters", e);
            callback.onError("Error creating request parameters");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        Log.d(TAG, "getPaymentsCalendar - Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            JSONArray calendarArray = response.getJSONArray("data");
                            List<CalendarPaymentData> calendarData = parseCalendarData(calendarArray);
                            Log.d(TAG, "getPaymentsCalendar - Parsed " + calendarData.size() + " calendar entries");
                            callback.onSuccess(calendarData);
                        } else {
                            String errorMsg = response.optString("error", "Unknown error");
                            Log.e(TAG, "getPaymentsCalendar - Server returned error: " + errorMsg);
                            callback.onError(errorMsg);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "getPaymentsCalendar - Error parsing response", e);
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "getPaymentsCalendar - Volley error", error);
                    callback.onError("Network error: " + error.getMessage());
                });

        requestQueue.add(request);
    }

    // Parse calendar data from JSON array
    private List<CalendarPaymentData> parseCalendarData(JSONArray jsonArray) {
        List<CalendarPaymentData> calendarData = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject dateJson = jsonArray.getJSONObject(i);
                CalendarPaymentData data = CalendarPaymentData.fromJson(dateJson);
                if (data != null) {
                    calendarData.add(data);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing calendar data", e);
        }
        return calendarData;
    }

    // Parse payment list from JSON array
    private List<PaymentData> parsePaymentList(JSONArray jsonArray) {
        List<PaymentData> payments = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject paymentJson = jsonArray.getJSONObject(i);
                
                // Log payment proof data for debugging
                if (paymentJson.has("payment_proof")) {
                    Log.d(TAG, "Payment ID " + paymentJson.optInt("payment_id", 0) + 
                          ", payment_proof: " + paymentJson.optString("payment_proof", "null"));
                }
                
                PaymentData payment = PaymentData.fromJson(paymentJson);
                if (payment != null) {
                    payments.add(payment);
                    Log.d(TAG, "Parsed payment ID " + payment.getPaymentId() + 
                          " - Payment Proof: " + payment.getPaymentProof());
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

    // Auto-mark overdue payments
    public void autoMarkOverdue(OverdueUpdateCallback callback) {
        String url = BASE_URL + "auto_mark_overdue.php";
        
        Log.d(TAG, "autoMarkOverdue - Requesting overdue update");
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "autoMarkOverdue - Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            String message = response.optString("message", "Overdue payments updated");
                            int totalUpdated = response.optInt("total_updated", 0);
                            int notificationsSent = response.optInt("notifications_sent", 0);
                            int notificationsSkipped = response.optInt("notifications_skipped", 0);
                            
                            Log.d(TAG, "autoMarkOverdue - Parsed: notificationsSent=" + notificationsSent + ", notificationsSkipped=" + notificationsSkipped);
                            
                            callback.onSuccess(message, totalUpdated, notificationsSent, notificationsSkipped);
                        } else {
                            String errorMsg = response.optString("error", "Unknown error");
                            Log.e(TAG, "autoMarkOverdue - Server returned error: " + errorMsg);
                            callback.onError(errorMsg);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "autoMarkOverdue - Error parsing response", e);
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "autoMarkOverdue - Volley error", error);
                    String errorMsg = "Network error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error");
                    callback.onError(errorMsg);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    // Send payment reminder
    public void sendPaymentReminder(int paymentId, int boarderUserId, PaymentReminderCallback callback) {
        String url = BASE_URL + "send_payment_reminder.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("payment_id", paymentId);
            params.put("boarder_user_id", boarderUserId);
        } catch (JSONException e) {
            callback.onError("Error creating request parameters");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            String message = response.optString("message", "Payment reminder sent successfully");
                            callback.onSuccess(message);
                        } else {
                            callback.onError(response.optString("error", "Unknown error"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error parsing response");
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error", error);
                    String errorMessage = "Network error";
                    if (error instanceof com.android.volley.TimeoutError) {
                        errorMessage = "Request timeout. Please check your connection and try again.";
                    } else if (error.getMessage() != null) {
                        errorMessage = "Network error: " + error.getMessage();
                    }
                    callback.onError(errorMessage);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };

        // Set timeout to 30 seconds
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                30000, // 30 seconds timeout
                1, // Max retries
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }
}
