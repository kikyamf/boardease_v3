package com.example.mock;

import java.io.Serializable;

public class PaymentBreakdownItem implements Serializable {
    private int breakdownId;
    private int bookingId;
    private Integer paymentId;
    private String periodType;
    private int periodNumber;
    private String periodLabel;
    private String periodStartDate;
    private String periodEndDate;
    private String amount;
    private boolean isSelected;
    private boolean isPaid;
    private String dueDate;
    private String paymentStatus;
    private String paymentDate;
    
    public PaymentBreakdownItem() {
    }
    
    public PaymentBreakdownItem(int breakdownId, int bookingId, Integer paymentId, String periodType,
                               int periodNumber, String periodLabel, String periodStartDate,
                               String periodEndDate, String amount, boolean isSelected, boolean isPaid,
                               String dueDate, String paymentStatus, String paymentDate) {
        this.breakdownId = breakdownId;
        this.bookingId = bookingId;
        this.paymentId = paymentId;
        this.periodType = periodType;
        this.periodNumber = periodNumber;
        this.periodLabel = periodLabel;
        this.periodStartDate = periodStartDate;
        this.periodEndDate = periodEndDate;
        this.amount = amount;
        this.isSelected = isSelected;
        this.isPaid = isPaid;
        this.dueDate = dueDate;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
    }
    
    // Getters
    public int getBreakdownId() { return breakdownId; }
    public int getBookingId() { return bookingId; }
    public Integer getPaymentId() { return paymentId; }
    public String getPeriodType() { return periodType; }
    public int getPeriodNumber() { return periodNumber; }
    public String getPeriodLabel() { return periodLabel; }
    public String getPeriodStartDate() { return periodStartDate; }
    public String getPeriodEndDate() { return periodEndDate; }
    public String getAmount() { return amount; }
    public boolean isSelected() { return isSelected; }
    public boolean isPaid() { return isPaid; }
    public String getDueDate() { return dueDate; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getPaymentDate() { return paymentDate; }
    
    // Setters
    public void setBreakdownId(int breakdownId) { this.breakdownId = breakdownId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public void setPaymentId(Integer paymentId) { this.paymentId = paymentId; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
    public void setPeriodNumber(int periodNumber) { this.periodNumber = periodNumber; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }
    public void setPeriodStartDate(String periodStartDate) { this.periodStartDate = periodStartDate; }
    public void setPeriodEndDate(String periodEndDate) { this.periodEndDate = periodEndDate; }
    public void setAmount(String amount) { this.amount = amount; }
    public void setSelected(boolean selected) { isSelected = selected; }
    public void setPaid(boolean paid) { isPaid = paid; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
}




