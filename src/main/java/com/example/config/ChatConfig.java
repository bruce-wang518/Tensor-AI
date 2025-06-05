package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "ragflow.chat")
public class ChatConfig {
    private String idProcess; // 自动绑定id.process
    private String idProduct; // 绑定id.product
    private String idData;    // 绑定id.data

    // 系统属性配置键
    private static final String CONFIG_PATH_PROPERTY = "chat.config.path";
    private static final String DEFAULT_CONFIG_PATH = "classpath:user-config.csv";
    // 配置映射缓存
    private final Map<String, String> phoneToTokenMap = new HashMap<>();
    private final Map<String, String> tokenToApiKeyMap = new HashMap<>();

    // 默认构造函数
    public ChatConfig() {
        // 将在 @PostConstruct 中加载配置
    }

    // 初始化配置
    @PostConstruct
    public void init() {
        // 获取配置路径（命令行参数优先）
        String configPath = getConfigPath();
        System.out.println("Loading chat configuration from: " + configPath);

        // 加载配置
        loadConfig(configPath);
        // 日志输出加载状态
        System.out.printf("Loaded %d mobile mappings and %d token mappings%n",
                phoneToTokenMap.size(), tokenToApiKeyMap.size());
    }

    // 获取配置文件路径（优先级：命令行参数 > 系统属性 > 默认路径）
    private String getConfigPath() {
        // 1. 检查系统属性
        String pathProp = System.getProperty(CONFIG_PATH_PROPERTY);
        if (pathProp != null && !pathProp.trim().isEmpty()) {
            return pathProp;
        }
        // 2. 默认配置路径
        return DEFAULT_CONFIG_PATH;
    }

    // 加载配置文件
    private void loadConfig(String configPath) {
        try {
            // 处理 classpath 资源
            InputStream inputStream = configPath.startsWith("classpath:")
                    ? new ClassPathResource(configPath.substring(10)).getInputStream()
                    : Files.newInputStream(Paths.get(configPath));

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                // 跳过标题行
                reader.readLine();

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.split(",", -1);
                    if (parts.length < 3) {
                        System.err.println("忽略无效配置行: " + line);
                        continue;
                    }

                    String mobile = parts[0].trim();
                    String token = parts[1].trim();
                    String apiKey = parts[2].trim();

                    // 存储映射关系
                    phoneToTokenMap.put(mobile, token);
                    tokenToApiKeyMap.put(token, apiKey);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("聊天配置加载失败: " + e.getMessage(), e);
        }
    }

    public String getTokenByPhone(String phone) {
        // 优先使用配置文件中的值
        String token = phoneToTokenMap.get(phone.toLowerCase());
        if (token != null) return token;

        return "";
    }

    public String getApikeyByToken(String token) {
        // 优先使用配置文件中的值
        String apiKey = tokenToApiKeyMap.get(token);
        if (apiKey != null) return apiKey;

        return "";
    }

    public String getChatId(String appid) {
        switch(appid.toLowerCase()) {
            case "process": return idProcess != null ? idProcess : "cdaacb8c41f111f09f3913c504f0ae96";//131:zjts:浙江腾视规章制度查询  ，ac4df552048b11f0b44b0242ac120006(local)
            case "product": return idProduct != null ? idProduct : "be2bfde241c011f0bff0c1a468e0fb80";//131:zjts:大模型专业知识查询
            case "data":    return idData != null ? idData : "af2068f441ef11f0abbcb714da4bbcd2";//131:zjts:大模型专业知识查询
            default:        return idProcess != null ? idProcess : "534f4e5839f411f08edf0242ac140006";
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