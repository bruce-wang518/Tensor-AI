/*
package com.example.dto;

import java.util.List;

public class SessionListResponse {
    private int code;
    private List<SessionDTO> data;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public List<SessionDTO> getData() { return data; }
    public void setData(List<SessionDTO> data) { this.data = data; }
}
*/


package com.example.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionListResponse {
    private int code;
    private List<SessionDTO> data;

    // Getters & Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public List<SessionDTO> getData() { return data; }
    public void setData(List<SessionDTO> data) { this.data = data; }
}