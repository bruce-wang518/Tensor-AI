package com.example.dto;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDTO {
    private String role;
    private String content;
    private Map<String, Object> metadata;

    public String getReference() {
        if (metadata == null) return "";

        Object ref = metadata.get("reference");
        if (ref instanceof List) {
            return ((List<?>) ref).stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("");
        } else if (ref instanceof Map) {
            Map<?,?> refMap = (Map<?,?>) ref;
            List<?> chunks = (List<?>) refMap.get("chunks");
            if (chunks != null && !chunks.isEmpty()) {
                Map<?,?> firstChunk = (Map<?,?>) chunks.get(0);
                Object docName = firstChunk.get("document_name");
                return docName != null ? docName.toString() : "";
            }
        }
        return ref != null ? ref.toString() : "";
    }

    // Getters & Setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}