package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ragflow.chat")
public class ChatConfig {
    private String idProcess; // 自动绑定id.process
    private String idProduct; // 绑定id.product
    private String idData;    // 绑定id.data

    public String getChatId(String tab) {
        switch(tab.toLowerCase()) {
            case "process": return "ac4df552048b11f0b44b0242ac120006";//idProcess; ac4df552048b11f0b44b0242ac120006
            case "product": return "34e07a0e142511f08b7a0242ac120007";//idProduct; 34e07a0e142511f08b7a0242ac120007
            case "data":    return "e3c93b4c3ad011f0837e0242ac120003";//idData;e3c93b4c3ad011f0837e0242ac120003
            default:       return "ac4df552048b11f0b44b0242ac120006"; // 默认返回process
        }
    }
    // Getter/Setter
    public String getIdProcess() { return idProcess; }
    public void setIdProcess(String idProcess) { this.idProcess = idProcess; }

    public String getIdProduct() { return idProduct; }
    public void setIdProduct(String idProduct) { this.idProduct = idProduct; }

    public String getIdData() { return idData; }
    public void setIdData(String idData) { this.idData = idData; }
}