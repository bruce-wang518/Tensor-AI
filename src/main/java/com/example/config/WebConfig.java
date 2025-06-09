package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类 - 配置CORS跨域访问
 * 允许前端应用直接调用后端API
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置CORS跨域映射
     * 允许前端应用（localhost:3000）直接访问后端API
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 只允许API路径
                .allowedOrigins(
                        "http://localhost:3000", // React开发服务器
                        "http://127.0.0.1:3000", // 本地回环地址
                        "http://192.168.1.131:3000" // 局域网地址（如需要）
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的HTTP方法
                .allowedHeaders(
                        "Content-Type",
                        "Authorization",
                        "token", // 自定义认证头
                        "appid", // 应用ID头
                        "X-Requested-With",
                        "Accept",
                        "Origin")
                .allowCredentials(true) // 允许发送凭证（cookies等）
                .maxAge(3600); // 预检请求缓存时间（1小时）
    }
}