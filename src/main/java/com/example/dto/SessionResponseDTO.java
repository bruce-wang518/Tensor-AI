package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true) // 添加此注解忽略未知字段
public class SessionResponseDTO {
    // Getters and Setters
    private String chat_id;
    private String create_date;
    private long create_time;
    private String id;
    private List<MessageResponse> messages;
    private String name;
    private String update_date;
    private long update_time;

    public void setChat_id(String chat_id) {
        this.chat_id = chat_id;
    }

    public void setCreate_date(String create_date) {
        this.create_date = create_date;
    }

    public void setCreate_time(long create_time) {
        this.create_time = create_time;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMessages(List<MessageResponse> messages) {
        this.messages = messages;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUpdate_date(String update_date) {
        this.update_date = update_date;
    }

    public void setUpdate_time(long update_time) {
        this.update_time = update_time;
    }
}