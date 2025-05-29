package com.example.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Message {
    private String role;
    private String content;
    private Map<String, Object> metadata = new HashMap<>();

    @JsonAnySetter
    public void setMetadataField(String key, Object value) {
        if (key != null && value != null) {
            metadata.put(key, value);
        }
    }

    // 安全获取reference的方法
    public String getReference() {
        Object refObj = metadata.get("reference");

        if (refObj instanceof List) {
            List<?> refList = (List<?>) refObj;
            return refList.isEmpty() ? null : refList.get(0).toString();
        }

        return refObj != null ? refObj.toString() : null;
    }

    // 保持原有基础方法
    public String getRole() {
        return role != null ? role : "";
    }

    public void setRole(String role) {
        this.role = role != null ? role : "";
    }

    public String getContent() {
        return content != null ? content : "";
    }

    public void setContent(String content) {
        this.content = content != null ? content : "";
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }
}