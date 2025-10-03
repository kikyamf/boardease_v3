package com.example.mock;

public class BoarderData {
    private int boarderId;
    private String boarderName;
    private String boarderEmail;
    private String boarderPhone;
    private String boardingHouseName;
    private String roomNumber;
    private String rentType;
    private String startDate;
    private String endDate;
    private String status;
    private String profilePicture;

    public BoarderData() {
        // Default constructor
    }

    public BoarderData(int boarderId, String boarderName, String boarderEmail, String boarderPhone,
                      String boardingHouseName, String roomNumber, String rentType,
                      String startDate, String endDate, String status, String profilePicture) {
        this.boarderId = boarderId;
        this.boarderName = boarderName;
        this.boarderEmail = boarderEmail;
        this.boarderPhone = boarderPhone;
        this.boardingHouseName = boardingHouseName;
        this.roomNumber = roomNumber;
        this.rentType = rentType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.profilePicture = profilePicture;
    }

    // Getters and Setters
    public int getBoarderId() {
        return boarderId;
    }

    public void setBoarderId(int boarderId) {
        this.boarderId = boarderId;
    }

    public String getBoarderName() {
        return boarderName;
    }

    public void setBoarderName(String boarderName) {
        this.boarderName = boarderName;
    }

    public String getBoarderEmail() {
        return boarderEmail;
    }

    public void setBoarderEmail(String boarderEmail) {
        this.boarderEmail = boarderEmail;
    }

    public String getBoarderPhone() {
        return boarderPhone;
    }

    public void setBoarderPhone(String boarderPhone) {
        this.boarderPhone = boarderPhone;
    }

    public String getBoardingHouseName() {
        return boardingHouseName;
    }

    public void setBoardingHouseName(String boardingHouseName) {
        this.boardingHouseName = boardingHouseName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRentType() {
        return rentType;
    }

    public void setRentType(String rentType) {
        this.rentType = rentType;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}




