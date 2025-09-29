package com.example.mock;

public class MessageModel {
    private String message;
    private boolean isReceiver; // true = received, false = sent

    public MessageModel(String message, boolean isReceiver) {
        this.message = message;
        this.isReceiver = isReceiver;
    }

    public String getMessage() {
        return message;
    }

    public boolean isReceiver() {
        return isReceiver;
    }
}
