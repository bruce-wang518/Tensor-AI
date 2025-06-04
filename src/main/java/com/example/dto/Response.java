package com.example.dto;

public class Response<T> {
    private int code;
    private String message;
    private T data;

    // 成功构造器（无需消息）
    public static <T> Response<T> success(T data) {
        return new Response<>(0, null, data);
    }

    // 错误构造器
    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, message, null);
    }

    // 私有构造器
    private Response(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // Getters
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}