package com.example.mock;

public class PaymentData {
    private String boarderName;
    private String room;
    private String rentType;
    private String amountPaid;
    private String totalAmount;
    private String paymentStatus;
    private String rentalStatus;
    private String paymentDate;

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

    // Getters
    public String getBoarderName() { return boarderName; }
    public String getRoom() { return room; }
    public String getRentType() { return rentType; }
    public String getAmountPaid() { return amountPaid; }
    public String getTotalAmount() { return totalAmount; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getRentalStatus() { return rentalStatus; }
    public String getPaymentDate() { return paymentDate; }

    // Setters
    public void setBoarderName(String boarderName) { this.boarderName = boarderName; }
    public void setRoom(String room) { this.room = room; }
    public void setRentType(String rentType) { this.rentType = rentType; }
    public void setAmountPaid(String amountPaid) { this.amountPaid = amountPaid; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setRentalStatus(String rentalStatus) { this.rentalStatus = rentalStatus; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
}





