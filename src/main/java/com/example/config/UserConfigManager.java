package com.example.config;
import com.example.dto.ApplicationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Configuration
public class UserConfigManager {

    // 从命令行参数指定的配置文件路径
    @Value("${chat.config.path:classpath:user-config.yaml}")
    private String configPath;

    // 映射关系缓存
    private final Map<String, String> userIdToToken = new HashMap<>();
    private final Map<String, String> tokenToApiKey = new HashMap<>();
    private final Map<String, Map<String, String>> tokenToApps = new HashMap<>();

    // 默认配置文件路径
    private static final String DEFAULT_CONFIG_PATH = "classpath:user-config.yaml";

    /**
     * 初始化配置 - 仅从外部文件加载
     */
    @PostConstruct
    public void init() {
        // 确定最终配置文件路径
        String configFilePath = configPath != null && !configPath.isEmpty() ?
                configPath : DEFAULT_CONFIG_PATH;

        System.out.println("Loading user configuration from: " + configFilePath);

        try {
            if (configFilePath.startsWith("classpath:")) {
                // 加载类路径资源
                String resourcePath = configFilePath.substring(10);
                Resource resource = new ClassPathResource(resourcePath);
                loadConfigFromYaml(resource.getInputStream());
            } else {
                // 加载文件系统资源
                loadConfigFromYaml(Files.newInputStream(Paths.get(configFilePath)));
            }

            System.out.printf("Loaded %d users and %d application mappings%n",
                    userIdToToken.size(), tokenToApps.size());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file: " + e.getMessage(), e);
        }
    }

    /**
     * 从YAML输入流加载配置
     */
    private void loadConfigFromYaml(InputStream inputStream) {
        Yaml yaml = new Yaml();
        try {
            // 解析YAML文件为Map列表
            List<Map<String, Object>> configItems = yaml.load(inputStream);

            // 缓存配置数据到内存映射
            for (Map<String, Object> configMap : configItems) {
                String userId = getStringValue(configMap, "userId");
                String token = getStringValue(configMap, "token");
                String apiKey = getStringValue(configMap, "apiKey");

                // 处理applications
                Map<String, String> apps = new HashMap<>();
                Object appsObj = configMap.get("applications");
                if (appsObj instanceof Map) {
                    Map<?, ?> appMap = (Map<?, ?>) appsObj;
                    for (Map.Entry<?, ?> entry : appMap.entrySet()) {
                        apps.put(String.valueOf(entry.getKey()),
                                String.valueOf(entry.getValue()));
                    }
                }

                // 添加到缓存映射
                userIdToToken.put(userId, token);
                tokenToApiKey.put(token, apiKey);
                tokenToApps.put(token, apps);
            }

        } catch (Exception e) {
            throw new RuntimeException("YAML parsing error: " + e.getMessage(), e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Ignore close exception
            }
        }
    }

    /**
     * 安全获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    // ================== 公共API ==================

    /**
     * 根据用户ID获取Token
     */
    public String getTokenByUserId(String userId) {
        return userIdToToken.get(userId);
    }

    /**
     * 根据Token获取API Key
     */
    public String getApiKeyByToken(String token) {
        String apiKey = tokenToApiKey.get(token);
        System.out.println("###getApiKeyByToken：token=" + token + ",ApiKey="+apiKey);
        return apiKey;
    }

    /**
     * 根据Token和应用ID获取Chat ID
     */
    public String getChatIdByToken(String token, String appId) {
        Map<String, String> apps = tokenToApps.get(token);
        return apps != null ? apps.get(appId) : null;
    }

   public List<ApplicationDTO> getUserApplicationsByToken(String token) {
        // 1. 从 tokenToApps 中获取当前 token 对应的应用 Map（应用ID → 应用信息）
        //    若 token 不存在，返回空 Map 避免空指针
        Map<String, String> appsMap = tokenToApps.getOrDefault(token, new HashMap<>());

        // 2. 提取应用 Map 的所有 key（即应用ID），转换为 List<String>
        List<String> appIds = new ArrayList<>(appsMap.keySet());
        // 3. 流处理：将每个应用ID转换为带硬编码 name/type 的 ApplicationDTO
        return appIds.stream()
                .map(appId -> {
                    return new ApplicationDTO(appId,"app name","app type");
                })
                .collect(Collectors.toList()); // 收集为 List<ApplicationDTO>
    }

    // ================== Getter/Setter ==================

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
}