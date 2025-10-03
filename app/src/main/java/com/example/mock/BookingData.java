package com.example.mock;

public class BookingData {
    private String boarderName;
    private String email;
    private String phoneNumber;
    private String roomName;
    private String startDate;
    private String endDate;
    private String amount;
    private String rentType;
    private String status;

    public BookingData(String boarderName, String email, String phoneNumber, String roomName, 
                      String startDate, String endDate, String amount, String rentType, String status) {
        this.boarderName = boarderName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.roomName = roomName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.amount = amount;
        this.rentType = rentType;
        this.status = status;
    }

    // Getters
    public String getBoarderName() { return boarderName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getRoomName() { return roomName; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getAmount() { return amount; }
    public String getRentType() { return rentType; }
    public String getStatus() { return status; }

    // Setters
    public void setBoarderName(String boarderName) { this.boarderName = boarderName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setAmount(String amount) { this.amount = amount; }
    public void setRentType(String rentType) { this.rentType = rentType; }
    public void setStatus(String status) { this.status = status; }
}





