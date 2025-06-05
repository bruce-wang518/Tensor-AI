package com.example.dto;

public class UpdateSessionRequest {
    private String sessionId; // 会话ID在请求体中
    private String name; // 新的会话名称

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}