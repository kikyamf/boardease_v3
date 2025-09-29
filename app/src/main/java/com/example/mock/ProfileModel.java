package com.example.mock;

public class ProfileModel {
    private String name;
    private int imageResId; // drawable resource (can switch to URL if using Glide/Picasso)

    public ProfileModel(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }
}

