package com.example.mock;

import org.json.JSONObject;
import java.io.Serializable;

public class PaymentData implements Serializable {
    private int paymentId;
    private int bookingId;
    private int userId;
    private String boarderName;
    private String room;
    private String rentType;
    private String amountPaid;
    private String totalAmount;
    private String paymentStatus;
    private String rentalStatus;
    private String paymentDate;
    private String dueDate;
    private String paymentMethod;
    private String notes;
    private String createdAt;
    private String updatedAt;

    // Constructor for sample data (backward compatibility)
    public PaymentData(String boarderName, String room, String rentType, String amountPaid, 
                      String totalAmount, String paymentStatus, String rentalStatus, String paymentDate) {
        this.boarderName = boarderName;
        this.room = room;
        this.rentType = rentType;
        this.amountPaid = amountPaid;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.rentalStatus = rentalStatus;
        this.paymentDate = paymentDate;
    }

    // Enhanced constructor for API data
    public PaymentData(int paymentId, int bookingId, int userId, String boarderName, String room, 
                      String rentType, String amountPaid, String totalAmount, String paymentStatus, 
                      String rentalStatus, String paymentDate, String dueDate, String paymentMethod, 
                      String notes, String createdAt, String updatedAt) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.boarderName = boarderName;
        this.room = room;
        this.rentType = rentType;
        this.amountPaid = amountPaid;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.rentalStatus = rentalStatus;
        this.paymentDate = paymentDate;
        this.dueDate = dueDate;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Constructor from JSON
    public static PaymentData fromJson(JSONObject json) {
        try {
            return new PaymentData(
                json.optInt("payment_id", 0),
                json.optInt("booking_id", 0),
                json.optInt("user_id", 0),
                json.optString("boarder_name", ""),
                json.optString("room", ""),
                json.optString("rent_type", ""),
                json.optString("amount_paid", ""),
                json.optString("total_amount", ""),
                json.optString("payment_status", ""),
                json.optString("rental_status", ""),
                json.optString("payment_date", ""),
                json.optString("due_date", ""),
                json.optString("payment_method", ""),
                json.optString("notes", ""),
                json.optString("created_at", ""),
                json.optString("updated_at", "")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Getters
    public int getPaymentId() { return paymentId; }
    public int getBookingId() { return bookingId; }
    public int getUserId() { return userId; }
    public String getBoarderName() { return boarderName; }
    public String getRoom() { return room; }
    public String getRentType() { return rentType; }
    public String getAmountPaid() { return amountPaid; }
    public String getTotalAmount() { return totalAmount; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getRentalStatus() { return rentalStatus; }
    public String getPaymentDate() { return paymentDate; }
    public String getDueDate() { return dueDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getNotes() { return notes; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    // Setters
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setBoarderName(String boarderName) { this.boarderName = boarderName; }
    public void setRoom(String room) { this.room = room; }
    public void setRentType(String rentType) { this.rentType = rentType; }
    public void setAmountPaid(String amountPaid) { this.amountPaid = amountPaid; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setRentalStatus(String rentalStatus) { this.rentalStatus = rentalStatus; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}

















