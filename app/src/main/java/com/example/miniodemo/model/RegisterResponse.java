package com.example.miniodemo.model;

public class RegisterResponse {
    private String message;
    private int userId;

    public RegisterResponse() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
    