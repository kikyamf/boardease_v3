package com.example.mock;

import java.util.List;

public class BoardingHouseDetails {
    private int bhId;
    private String bhName;
    private String bhAddress;
    private String bhDescription;
    private String bhRules;
    private int numberOfBathroom;
    private double area;
    private int buildYear;
    private String status;
    private String bhCreatedAt;
    private List<String> images;
    private List<String> roomCategories;
    private List<RoomDetail> roomDetails;
    private Integer minPrice;
    private Integer maxPrice;
    private OwnerInfo owner;

    public BoardingHouseDetails() {}

    public BoardingHouseDetails(int bhId, String bhName, String bhAddress, String bhDescription, 
                               String bhRules, int numberOfBathroom, double area, int buildYear, 
                               String status, String bhCreatedAt, List<String> images, 
                               List<String> roomCategories, List<RoomDetail> roomDetails, 
                               Integer minPrice, Integer maxPrice, OwnerInfo owner) {
        this.bhId = bhId;
        this.bhName = bhName;
        this.bhAddress = bhAddress;
        this.bhDescription = bhDescription;
        this.bhRules = bhRules;
        this.numberOfBathroom = numberOfBathroom;
        this.area = area;
        this.buildYear = buildYear;
        this.status = status;
        this.bhCreatedAt = bhCreatedAt;
        this.images = images;
        this.roomCategories = roomCategories;
        this.roomDetails = roomDetails;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.owner = owner;
    }

    // Getters and Setters
    public int getBhId() { return bhId; }
    public void setBhId(int bhId) { this.bhId = bhId; }

    public String getBhName() { return bhName; }
    public void setBhName(String bhName) { this.bhName = bhName; }

    public String getBhAddress() { return bhAddress; }
    public void setBhAddress(String bhAddress) { this.bhAddress = bhAddress; }

    public String getBhDescription() { return bhDescription; }
    public void setBhDescription(String bhDescription) { this.bhDescription = bhDescription; }

    public String getBhRules() { return bhRules; }
    public void setBhRules(String bhRules) { this.bhRules = bhRules; }

    public int getNumberOfBathroom() { return numberOfBathroom; }
    public void setNumberOfBathroom(int numberOfBathroom) { this.numberOfBathroom = numberOfBathroom; }

    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }

    public int getBuildYear() { return buildYear; }
    public void setBuildYear(int buildYear) { this.buildYear = buildYear; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBhCreatedAt() { return bhCreatedAt; }
    public void setBhCreatedAt(String bhCreatedAt) { this.bhCreatedAt = bhCreatedAt; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public List<String> getRoomCategories() { return roomCategories; }
    public void setRoomCategories(List<String> roomCategories) { this.roomCategories = roomCategories; }

    public List<RoomDetail> getRoomDetails() { return roomDetails; }
    public void setRoomDetails(List<RoomDetail> roomDetails) { this.roomDetails = roomDetails; }

    public Integer getMinPrice() { return minPrice; }
    public void setMinPrice(Integer minPrice) { this.minPrice = minPrice; }

    public Integer getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Integer maxPrice) { this.maxPrice = maxPrice; }

    public OwnerInfo getOwner() { return owner; }
    public void setOwner(OwnerInfo owner) { this.owner = owner; }

    // Helper methods
    public String getFormattedPriceRange() {
        if (minPrice == null && maxPrice == null) {
            return "Contact for pricing";
        } else if (minPrice != null && maxPrice != null && minPrice.equals(maxPrice)) {
            return "₱" + String.format("%,d", minPrice) + "/month";
        } else if (minPrice != null && maxPrice != null) {
            return "₱" + String.format("%,d", minPrice) + " - ₱" + String.format("%,d", maxPrice) + "/month";
        } else if (minPrice != null) {
            return "From ₱" + String.format("%,d", minPrice) + "/month";
        } else {
            return "Up to ₱" + String.format("%,d", maxPrice) + "/month";
        }
    }

    public String getOwnerFullName() {
        if (owner == null) return "Unknown";
        StringBuilder name = new StringBuilder();
        if (owner.getFirstName() != null) name.append(owner.getFirstName());
        if (owner.getMiddleName() != null && !owner.getMiddleName().isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(owner.getMiddleName());
        }
        if (owner.getLastName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(owner.getLastName());
        }
        return name.length() > 0 ? name.toString() : "Unknown";
    }

    // Inner classes
    public static class RoomDetail {
        private String roomCategory;
        private String roomName;
        private int price;
        private int capacity;
        private String roomDescription;
        private int totalRooms;

        public RoomDetail() {}

        public RoomDetail(String roomCategory, String roomName, int price, int capacity, 
                         String roomDescription, int totalRooms) {
            this.roomCategory = roomCategory;
            this.roomName = roomName;
            this.price = price;
            this.capacity = capacity;
            this.roomDescription = roomDescription;
            this.totalRooms = totalRooms;
        }

        // Getters and Setters
        public String getRoomCategory() { return roomCategory; }
        public void setRoomCategory(String roomCategory) { this.roomCategory = roomCategory; }

        public String getRoomName() { return roomName; }
        public void setRoomName(String roomName) { this.roomName = roomName; }

        public int getPrice() { return price; }
        public void setPrice(int price) { this.price = price; }

        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }

        public String getRoomDescription() { return roomDescription; }
        public void setRoomDescription(String roomDescription) { this.roomDescription = roomDescription; }

        public int getTotalRooms() { return totalRooms; }
        public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }

        public String getFormattedPrice() {
            return "₱" + String.format("%,d", price) + "/month";
        }
    }

    public static class OwnerInfo {
        private String firstName;
        private String middleName;
        private String lastName;
        private String phone;
        private String email;
        private String role;

        public OwnerInfo() {}

        public OwnerInfo(String firstName, String middleName, String lastName, 
                        String phone, String email, String role) {
            this.firstName = firstName;
            this.middleName = middleName;
            this.lastName = lastName;
            this.phone = phone;
            this.email = email;
            this.role = role;
        }

        // Getters and Setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getMiddleName() { return middleName; }
        public void setMiddleName(String middleName) { this.middleName = middleName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
