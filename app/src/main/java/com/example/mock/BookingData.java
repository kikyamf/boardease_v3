package com.example.mock;

public class BookingData {
    private int bookingId;
    private String boarderName;
    private String email;
    private String phoneNumber;
    private String roomName;
    private String startDate;
    private String endDate;
    private String amount;
    private String rentType;
    private String status;
    private String boardingHouseName;
    private String boardingHouseAddress;
    private String bookingDate;
    private String paymentStatus;
    private String notes;
    private String profileImage;
    private int boarderId;
    private int roomId;
    private int boardingHouseId;

    // Original constructor for backward compatibility
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
    
    // Enhanced constructor with all fields
    public BookingData(int bookingId, String boarderName, String email, String phoneNumber, 
                      String roomName, String startDate, String endDate, String amount, 
                      String rentType, String status, String boardingHouseName, 
                      String boardingHouseAddress, String bookingDate, String paymentStatus, 
                      String notes, String profileImage, int boarderId, int roomId, int boardingHouseId) {
        this.bookingId = bookingId;
        this.boarderName = boarderName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.roomName = roomName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.amount = amount;
        this.rentType = rentType;
        this.status = status;
        this.boardingHouseName = boardingHouseName;
        this.boardingHouseAddress = boardingHouseAddress;
        this.bookingDate = bookingDate;
        this.paymentStatus = paymentStatus;
        this.notes = notes;
        this.profileImage = profileImage;
        this.boarderId = boarderId;
        this.roomId = roomId;
        this.boardingHouseId = boardingHouseId;
    }

    // Getters
    public int getBookingId() { return bookingId; }
    public String getBoarderName() { return boarderName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getRoomName() { return roomName; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getAmount() { return amount; }
    public String getRentType() { return rentType; }
    public String getStatus() { return status; }
    public String getBoardingHouseName() { return boardingHouseName; }
    public String getBoardingHouseAddress() { return boardingHouseAddress; }
    public String getBookingDate() { return bookingDate; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getNotes() { return notes; }
    public String getProfileImage() { return profileImage; }
    public int getBoarderId() { return boarderId; }
    public int getRoomId() { return roomId; }
    public int getBoardingHouseId() { return boardingHouseId; }

    // Setters
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public void setBoarderName(String boarderName) { this.boarderName = boarderName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setAmount(String amount) { this.amount = amount; }
    public void setRentType(String rentType) { this.rentType = rentType; }
    public void setStatus(String status) { this.status = status; }
    public void setBoardingHouseName(String boardingHouseName) { this.boardingHouseName = boardingHouseName; }
    public void setBoardingHouseAddress(String boardingHouseAddress) { this.boardingHouseAddress = boardingHouseAddress; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public void setBoarderId(int boarderId) { this.boarderId = boarderId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public void setBoardingHouseId(int boardingHouseId) { this.boardingHouseId = boardingHouseId; }
}
















