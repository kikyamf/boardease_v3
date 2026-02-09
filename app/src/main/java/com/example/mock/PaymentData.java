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
    private String paymentProof;
    private String profilePicture;
    private String createdAt;
    private String updatedAt;
    
    private String boardingHouseName;
    private String boardingHouseAddress;

    // Payment progress fields
    private int totalPeriods;
    private int paidPeriods;
    private int unpaidPeriods;
    private int totalMonthsForBooking;
    private int paidMonthsForBooking;
    private Integer remainingMonthsToPay;
    private String totalAmountForBooking;
    private String paidAmountForBooking;
    private String remainingAmountToPay;
    private boolean isFullyPaid;
    private double paymentProgressPercent;

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
                      String notes, String paymentProof, String profilePicture, 
                      String createdAt, String updatedAt, String boardingHouseName, String boardingHouseAddress) {
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
        this.paymentProof = paymentProof;
        this.profilePicture = profilePicture;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.boardingHouseName = boardingHouseName;
        this.boardingHouseAddress = boardingHouseAddress;
    }

    // Constructor from JSON
    public static PaymentData fromJson(JSONObject json) {
        try {
            // Get payment proof URL (prefer receipt_url, fallback to payment_proof)
            String paymentProofUrl = json.optString("receipt_url", "");
            if (paymentProofUrl.isEmpty()) {
                paymentProofUrl = json.optString("payment_proof", "");
            }
            
            PaymentData paymentData = new PaymentData(
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
                paymentProofUrl,
                json.optString("profile_picture", ""),
                json.optString("created_at", ""),
                json.optString("updated_at", ""),
                json.optString("boarding_house_name", ""),
                json.optString("boarding_house_address", "")
            );
            
            // Set payment progress fields
            paymentData.setTotalPeriods(json.optInt("total_periods", 0));
            paymentData.setPaidPeriods(json.optInt("paid_periods", 0));
            paymentData.setUnpaidPeriods(json.optInt("unpaid_periods", 0));
            paymentData.setTotalMonthsForBooking(json.optInt("total_months_for_booking", 0));
            paymentData.setPaidMonthsForBooking(json.optInt("paid_months_for_booking", 0));
            if (!json.isNull("remaining_months_to_pay")) {
                paymentData.setRemainingMonthsToPay(json.optInt("remaining_months_to_pay", 0));
            }
            paymentData.setTotalAmountForBooking(json.optString("total_amount_for_booking", "0.00"));
            paymentData.setPaidAmountForBooking(json.optString("paid_amount_for_booking", "0.00"));
            paymentData.setRemainingAmountToPay(json.optString("remaining_amount_to_pay", "0.00"));
            paymentData.setFullyPaid(json.optBoolean("is_fully_paid", false));
            paymentData.setPaymentProgressPercent(json.optDouble("payment_progress_percent", 0.0));
            
            return paymentData;
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
    public String getPaymentProof() { return paymentProof; }
    public String getProfilePicture() { return profilePicture; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getBoardingHouseName() { return boardingHouseName; }
    public String getBoardingHouseAddress() { return boardingHouseAddress; }
    
    // Payment progress getters
    public int getTotalPeriods() { return totalPeriods; }
    public int getPaidPeriods() { return paidPeriods; }
    public int getUnpaidPeriods() { return unpaidPeriods; }
    public int getTotalMonthsForBooking() { return totalMonthsForBooking; }
    public int getPaidMonthsForBooking() { return paidMonthsForBooking; }
    public Integer getRemainingMonthsToPay() { return remainingMonthsToPay; }
    public String getTotalAmountForBooking() { return totalAmountForBooking; }
    public String getPaidAmountForBooking() { return paidAmountForBooking; }
    public String getRemainingAmountToPay() { return remainingAmountToPay; }
    public boolean isFullyPaid() { return isFullyPaid; }
    public double getPaymentProgressPercent() { return paymentProgressPercent; }

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
    public void setPaymentProof(String paymentProof) { this.paymentProof = paymentProof; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public void setBoardingHouseName(String boardingHouseName) { this.boardingHouseName = boardingHouseName; }
    public void setBoardingHouseAddress(String boardingHouseAddress) { this.boardingHouseAddress = boardingHouseAddress; }
    
    // Payment progress setters
    public void setTotalPeriods(int totalPeriods) { this.totalPeriods = totalPeriods; }
    public void setPaidPeriods(int paidPeriods) { this.paidPeriods = paidPeriods; }
    public void setUnpaidPeriods(int unpaidPeriods) { this.unpaidPeriods = unpaidPeriods; }
    public void setTotalMonthsForBooking(int totalMonthsForBooking) { this.totalMonthsForBooking = totalMonthsForBooking; }
    public void setPaidMonthsForBooking(int paidMonthsForBooking) { this.paidMonthsForBooking = paidMonthsForBooking; }
    public void setRemainingMonthsToPay(Integer remainingMonthsToPay) { this.remainingMonthsToPay = remainingMonthsToPay; }
    public void setTotalAmountForBooking(String totalAmountForBooking) { this.totalAmountForBooking = totalAmountForBooking; }
    public void setPaidAmountForBooking(String paidAmountForBooking) { this.paidAmountForBooking = paidAmountForBooking; }
    public void setRemainingAmountToPay(String remainingAmountToPay) { this.remainingAmountToPay = remainingAmountToPay; }
    public void setFullyPaid(boolean isFullyPaid) { this.isFullyPaid = isFullyPaid; }
    public void setPaymentProgressPercent(double paymentProgressPercent) { this.paymentProgressPercent = paymentProgressPercent; }
}

















