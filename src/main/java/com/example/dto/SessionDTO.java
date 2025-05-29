package com.example.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionDTO {
    private String id;
    private String name;
    private List<MessageDTO> messages;
    private long create_time;
    private long update_time;

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<MessageDTO> getMessages() { return messages; }
    public void setMessages(List<MessageDTO> messages) { this.messages = messages; }

    public long getCreate_time() { return create_time; }
    public void setCreate_time(long create_time) { this.create_time = create_time; }

    public long getUpdate_time() { return update_time; }
    public void setUpdate_time(long update_time) { this.update_time = update_time; }

    }

/*

package com.example.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class SessionDTO {
    private String id;
    private String name;
    private List<Message> messages = new ArrayList<>();

    @JsonSetter("messages")
    public void setMessages(List<Message> messages) {
        this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id != null ? id : "";
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }
}

 */