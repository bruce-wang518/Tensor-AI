package com.example.dto;

public class MessageRequest {
    private String sessionId;
    private String message;

    // Getters & Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}