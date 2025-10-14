package com.example.mock;

public class Listing {
    private int id;
    private String name;
    private String imagePath;
    private String location;
    private String price;
    private float rating;
    private boolean isFavorite;
    private String description;
    private String amenities;
    private String contactNumber;
    private String ownerName;
    
    public Listing(int id, String name, String imagePath) {
        this.id = id;
        this.name = name;
        this.imagePath = imagePath;
        this.location = "";
        this.price = "";
        this.rating = 0.0f;
        this.isFavorite = false;
        this.description = "";
        this.amenities = "";
        this.contactNumber = "";
        this.ownerName = "";
    }
    
    public Listing(int id, String name, String imagePath, String location, String price, float rating) {
        this.id = id;
        this.name = name;
        this.imagePath = imagePath;
        this.location = location;
        this.price = price;
        this.rating = rating;
        this.isFavorite = false;
        this.description = "";
        this.amenities = "";
        this.contactNumber = "";
        this.ownerName = "";
    }
    
    public Listing(int id, String name, String imagePath, String location, String price, float rating, boolean isFavorite) {
        this.id = id;
        this.name = name;
        this.imagePath = imagePath;
        this.location = location;
        this.price = price;
        this.rating = rating;
        this.isFavorite = isFavorite;
        this.description = "";
        this.amenities = "";
        this.contactNumber = "";
        this.ownerName = "";
    }
    
    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getImagePath() { return imagePath; }
    public String getLocation() { return location; }
    public String getPrice() { return price; }
    public float getRating() { return rating; }
    public boolean isFavorite() { return isFavorite; }
    public String getDescription() { return description; }
    public String getAmenities() { return amenities; }
    public String getContactNumber() { return contactNumber; }
    public String getOwnerName() { return ownerName; }
    
    // Alias methods for compatibility
    public int getBhId() { return id; }
    public String getBhName() { return name; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setLocation(String location) { this.location = location; }
    public void setPrice(String price) { this.price = price; }
    public void setRating(float rating) { this.rating = rating; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setDescription(String description) { this.description = description; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}
