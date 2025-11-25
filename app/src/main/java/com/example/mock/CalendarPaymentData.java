package com.example.mock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CalendarPaymentData {
    private String date;
    private String dominantStatus;
    private int paymentCount;
    private double totalAmount;
    private int pendingCount;
    private int partiallyPaidCount;
    private int overdueCount;
    private int paidCount;
    private List<PaymentItem> payments;

    public CalendarPaymentData(String date, String dominantStatus, int paymentCount, double totalAmount,
                              int pendingCount, int partiallyPaidCount, int overdueCount, int paidCount,
                              List<PaymentItem> payments) {
        this.date = date;
        this.dominantStatus = dominantStatus;
        this.paymentCount = paymentCount;
        this.totalAmount = totalAmount;
        this.pendingCount = pendingCount;
        this.partiallyPaidCount = partiallyPaidCount;
        this.overdueCount = overdueCount;
        this.paidCount = paidCount;
        this.payments = payments;
    }

    public static CalendarPaymentData fromJson(JSONObject json) {
        try {
            String date = json.getString("date");
            String dominantStatus = json.getString("dominant_status");
            int paymentCount = json.getInt("payment_count");
            double totalAmount = json.getDouble("total_amount");
            
            JSONObject statusCounts = json.getJSONObject("status_counts");
            int pendingCount = statusCounts.optInt("Pending", 0);
            int partiallyPaidCount = statusCounts.optInt("Partially Paid", 0);
            int overdueCount = statusCounts.optInt("Overdue", 0);
            int paidCount = statusCounts.optInt("Paid", 0);
            
            List<PaymentItem> payments = new ArrayList<>();
            if (json.has("payments")) {
                JSONArray paymentsArray = json.getJSONArray("payments");
                for (int i = 0; i < paymentsArray.length(); i++) {
                    JSONObject paymentJson = paymentsArray.getJSONObject(i);
                    PaymentItem item = PaymentItem.fromJson(paymentJson);
                    if (item != null) {
                        payments.add(item);
                    }
                }
            }
            
            return new CalendarPaymentData(date, dominantStatus, paymentCount, totalAmount,
                    pendingCount, partiallyPaidCount, overdueCount, paidCount, payments);
        } catch (JSONException e) {
            android.util.Log.e("CalendarPaymentData", "Error parsing JSON", e);
            return null;
        }
    }

    // Getters
    public String getDate() { return date; }
    public String getDominantStatus() { return dominantStatus; }
    public int getPaymentCount() { return paymentCount; }
    public double getTotalAmount() { return totalAmount; }
    public int getPendingCount() { return pendingCount; }
    public int getPartiallyPaidCount() { return partiallyPaidCount; }
    public int getOverdueCount() { return overdueCount; }
    public int getPaidCount() { return paidCount; }
    public List<PaymentItem> getPayments() { return payments; }

    // Payment item class
    public static class PaymentItem {
        private Integer breakdownId;
        private Integer paymentId;
        private int bookingId;
        private int boarderUserId;
        private String boarderName;
        private String roomNumber;
        private String bhName;
        private double amount;
        private String status;
        private String calendarStatus;
        private String dueDate;

        public PaymentItem(Integer breakdownId, Integer paymentId, int bookingId, int boarderUserId, String boarderName,
                          String roomNumber, String bhName, double amount, String status, String calendarStatus, String dueDate) {
            this.breakdownId = breakdownId;
            this.paymentId = paymentId;
            this.bookingId = bookingId;
            this.boarderUserId = boarderUserId;
            this.boarderName = boarderName;
            this.roomNumber = roomNumber;
            this.bhName = bhName;
            this.amount = amount;
            this.status = status;
            this.calendarStatus = calendarStatus;
            this.dueDate = dueDate;
        }

        public static PaymentItem fromJson(JSONObject json) {
            try {
                Integer breakdownId = json.has("breakdown_id") && !json.isNull("breakdown_id") 
                    ? json.getInt("breakdown_id") : null;
                Integer paymentId = json.has("payment_id") && !json.isNull("payment_id") 
                    ? json.getInt("payment_id") : null;
                int bookingId = json.getInt("booking_id");
                int boarderUserId = json.optInt("boarder_user_id", 0);
                String boarderName = json.optString("boarder_name", "Unknown");
                String roomNumber = json.optString("room_number", "N/A");
                String bhName = json.optString("bh_name", "Unknown");
                double amount = json.getDouble("amount");
                String status = json.optString("status", "Pending");
                String calendarStatus = json.optString("calendar_status", "Pending");
                String dueDate = json.optString("due_date", "");
                
                return new PaymentItem(breakdownId, paymentId, bookingId, boarderUserId, boarderName, roomNumber, 
                    bhName, amount, status, calendarStatus, dueDate);
            } catch (JSONException e) {
                android.util.Log.e("PaymentItem", "Error parsing JSON", e);
                return null;
            }
        }

        // Getters
        public Integer getBreakdownId() { return breakdownId; }
        public Integer getPaymentId() { return paymentId; }
        public int getBookingId() { return bookingId; }
        public int getBoarderUserId() { return boarderUserId; }
        public String getBoarderName() { return boarderName; }
        public String getRoomNumber() { return roomNumber; }
        public String getBhName() { return bhName; }
        public double getAmount() { return amount; }
        public String getStatus() { return status; }
        public String getCalendarStatus() { return calendarStatus; }
        public String getDueDate() { return dueDate; }
    }
}

