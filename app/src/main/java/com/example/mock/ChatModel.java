package com.example.mock;

public class ChatModel {
    private String name;
    private String lastMessage;
    private String time;
    private int imageResId; // drawable resource (for now, can be URL later)

    public ChatModel(String name, String lastMessage, String time, int imageResId) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getTime() {
        return time;
    }

    public int getImageResId() {
        return imageResId;
    }
}

