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

    public String getFormattedPrice() {
        if (minPrice == null) {
            return "Contact for pricing";
        }
        
        if (maxPrice == null || minPrice.equals(maxPrice)) {
            return "₱" + String.format("%,d", minPrice) + "/month";
        } else {
            return "₱" + String.format("%,d", minPrice) + " - ₱" + String.format("%,d", maxPrice) + "/month";
        }
    }
}
