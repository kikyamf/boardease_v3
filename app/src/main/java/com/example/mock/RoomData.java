package com.example.mock;

public class RoomData {
    private String roomName;
    private int price;
    private int capacity;
    private String roomCategory;
    private String roomDescription;
    private int totalRooms;

    public RoomData(String roomName, int price, int capacity, String roomCategory, String roomDescription, int totalRooms) {
        this.roomName = roomName;
        this.price = price;
        this.capacity = capacity;
        this.roomCategory = roomCategory;
        this.roomDescription = roomDescription;
        this.totalRooms = totalRooms;
    }

    // Getters
    public String getRoomName() {
        return roomName;
    }

    public int getPrice() {
        return price;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getRoomCategory() {
        return roomCategory;
    }

    public String getRoomDescription() {
        return roomDescription;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public String getFormattedPrice() {
        return "₱" + String.format("%,d", price) + "/month";
    }

    public String getCapacityText() {
        return capacity == 1 ? "1 person" : capacity + " people";
    }
}
