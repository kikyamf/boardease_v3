package com.example.mock;

import java.util.ArrayList;

public class Listing {
    private int bhId;
    private String bhName;
    private String bhAddress;
    private String bhDescription;
    private String bhRules;
    private String bhBathrooms;
    private String bhArea;
    private String bhBuildYear;
    private String imagePath;
    private ArrayList<String> imagePaths;
    private Integer minPrice;
    private Integer maxPrice;
    private double averageRating;
    // Owner contact information
    private String ownerName;
    private String ownerPhone;
    private String ownerEmail;

    public Listing(int bhId, String bhName, String imagePath) {
        this.bhId = bhId;
        this.bhName = bhName;
        this.imagePath = imagePath;
        this.imagePaths = new ArrayList<>();
    }

    public Listing(int bhId, String bhName, String bhAddress, String bhDescription, 
                   String bhRules, String bhBathrooms, String bhArea, String bhBuildYear, 
                   String imagePath, ArrayList<String> imagePaths) {
        this.bhId = bhId;
        this.bhName = bhName;
        this.bhAddress = bhAddress;
        this.bhDescription = bhDescription;
        this.bhRules = bhRules;
        this.bhBathrooms = bhBathrooms;
        this.bhArea = bhArea;
        this.bhBuildYear = bhBuildYear;
        this.imagePath = imagePath;
        this.imagePaths = imagePaths != null ? imagePaths : new ArrayList<>();
    }

    public Listing(int bhId, String bhName, String bhAddress, String bhDescription, 
                   String bhRules, String bhBathrooms, String bhArea, String bhBuildYear, 
                   String imagePath, ArrayList<String> imagePaths, Integer minPrice, Integer maxPrice) {
        this(bhId, bhName, bhAddress, bhDescription, bhRules, bhBathrooms, bhArea, bhBuildYear, 
             imagePath, imagePaths, minPrice, maxPrice, 0.0);
    }

    public Listing(int bhId, String bhName, String bhAddress, String bhDescription, 
                   String bhRules, String bhBathrooms, String bhArea, String bhBuildYear, 
                   String imagePath, ArrayList<String> imagePaths, Integer minPrice, Integer maxPrice, double averageRating) {
        this.bhId = bhId;
        this.bhName = bhName;
        this.bhAddress = bhAddress;
        this.bhDescription = bhDescription;
        this.bhRules = bhRules;
        this.bhBathrooms = bhBathrooms;
        this.bhArea = bhArea;
        this.bhBuildYear = bhBuildYear;
        this.imagePath = imagePath;
        this.imagePaths = imagePaths != null ? imagePaths : new ArrayList<>();
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.averageRating = averageRating;
    }

    public int getBhId() {
        return bhId;
    }

    public String getBhName() {
        return bhName;
    }

    public String getBhAddress() {
        return bhAddress;
    }

    public String getBhDescription() {
        return bhDescription;
    }

    public String getBhRules() {
        return bhRules;
    }

    public String getBhBathrooms() {
        return bhBathrooms;
    }

    public String getBhArea() {
        return bhArea;
    }

    public String getBhBuildYear() {
        return bhBuildYear;
    }

    public String getImagePath() {
        return imagePath;
    }

    public ArrayList<String> getImagePaths() {
        return imagePaths;
    }

    public Integer getMinPrice() {
        return minPrice;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public void setOwnerPhone(String ownerPhone) {
        this.ownerPhone = ownerPhone;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getFormattedPrice() {
        if (minPrice == null) {
            // Generate sample price based on boarding house ID for testing
            int samplePrice = 2000 + (bhId % 5) * 500; // Generate prices between 2000-4000
            return "₱" + String.format("%,d", samplePrice) + "/month";
        }
        
        if (maxPrice == null || minPrice.equals(maxPrice)) {
            return "₱" + String.format("%,d", minPrice) + "/month";
        } else {
            return "₱" + String.format("%,d", minPrice) + " - ₱" + String.format("%,d", maxPrice) + "/month";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Listing listing = (Listing) o;
        return bhId == listing.bhId &&
                Double.compare(listing.averageRating, averageRating) == 0 &&
                java.util.Objects.equals(bhName, listing.bhName) &&
                java.util.Objects.equals(bhAddress, listing.bhAddress) &&
                java.util.Objects.equals(bhDescription, listing.bhDescription) &&
                java.util.Objects.equals(imagePath, listing.imagePath) &&
                java.util.Objects.equals(minPrice, listing.minPrice) &&
                java.util.Objects.equals(maxPrice, listing.maxPrice);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(bhId, bhName, bhAddress, bhDescription, imagePath, minPrice, maxPrice, averageRating);
    }
}
