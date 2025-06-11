// 在 com.example.dto 包下新增
package com.example.dto;

public class ApplicationDTO {
    private String id;      // 应用ID
    private String name;    // 应用名称
    private String type;    // 应用类型（如：审批、报销等）

    // 全参构造、getter和setter
    public ApplicationDTO(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    // getter和setter方法
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}