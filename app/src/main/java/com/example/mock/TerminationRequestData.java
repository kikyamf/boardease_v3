package com.example.mock;

public class TerminationRequestData {
    private int terminationId;
    private int bookingId;
    private int boarderId;
    private String reason;
    private String details;
    private String status;
    private String createdAt;
    private String firstName;
    private String lastName;
    private String boardingHouseName;
    private String roomNumber;
    private String roomType;

    public TerminationRequestData(int terminationId, int bookingId, int boarderId, String reason, String details, 
                                String status, String createdAt, String firstName, String lastName, 
                                String boardingHouseName, String roomNumber, String roomType) {
        this.terminationId = terminationId;
        this.bookingId = bookingId;
        this.boarderId = boarderId;
        this.reason = reason;
        this.details = details;
        this.status = status;
        this.createdAt = createdAt;
        this.firstName = firstName;
        this.lastName = lastName;
        this.boardingHouseName = boardingHouseName;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
    }

    public int getTerminationId() { return terminationId; }
    public int getBookingId() { return bookingId; }
    public int getBoarderId() { return boarderId; }
    public String getReason() { return reason; }
    public String getDetails() { return details; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getBoarderName() { return firstName + " " + lastName; }
    public String getBoardingHouseName() { return boardingHouseName; }
    public String getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
}
