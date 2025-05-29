package com.example.dto;
public class MessageResponse {
    private String role;
    private String content;
    private String reference;

    public MessageResponse() {}

    public MessageResponse(String role, String content) {
        this.role = role;
        this.content = content;
    }

    // Getters & Setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
