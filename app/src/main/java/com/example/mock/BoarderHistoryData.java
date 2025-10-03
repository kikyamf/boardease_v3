package com.example.mock;

public class BoarderHistoryData {
    private String boarderName;
    private String roomName;
    private String startDate;
    private String endDate;
    private String status;

    public BoarderHistoryData(String boarderName, String roomName, String startDate, String endDate, String status) {
        this.boarderName = boarderName;
        this.roomName = roomName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    // Getters
    public String getBoarderName() { return boarderName; }
    public String getRoomName() { return roomName; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getStatus() { return status; }

    // Setters
    public void setBoarderName(String boarderName) { this.boarderName = boarderName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setStatus(String status) { this.status = status; }
}





