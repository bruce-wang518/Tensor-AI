package com.example.service;

import com.example.config.UserConfigManager;
import com.example.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import javax.net.ssl.SSLContext;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@Service
public class RagflowService {

    @Value("${ragflow.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    private final UserConfigManager userConfig;

    @Autowired
    public RagflowService(UserConfigManager userConfig) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this.userConfig = userConfig;

        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial((chain, authType) -> true)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();

        this.restTemplate = new RestTemplate(
                new HttpComponentsClientHttpRequestFactory(httpClient)
        );
    }

    public List<SessionDTO> getSessions(String token, String appid) {
        try {
            String chatId = userConfig.getChatIdByToken(token,appid);
            String url = String.format("%s/api/v1/chats/%s/sessions", baseUrl, chatId);
            System.out.println("url="+url);

            String apiKey=userConfig.getApiKeyByToken(token);
            System.out.println("**********token="+token);

            System.out.println("**********apiKey="+apiKey);

            // 设置与Postman完全一致的请求头String token,
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("User-Agent", "PostmanRuntime/7.44.0");
            headers.setAccept(Collections.singletonList(MediaType.ALL));
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Connection", "keep-alive");

            ResponseEntity<SessionListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    SessionListResponse.class
            );
            System.out.println("**********getsession response"+response.getBody());

            return Optional.ofNullable(response.getBody())
                    .map(SessionListResponse::getData)
                    .orElse(Collections.emptyList());

        } catch (HttpClientErrorException e) {
            System.err.println("请求失败！响应内容：\n" + e.getResponseBodyAsString());
            return Collections.emptyList();
        }
    }

    public SessionResponseDTO createSession(String token, String appid, String name) {
        String chatId = userConfig.getChatIdByToken(token,appid);
        String url = String.format("%s/api/v1/chats/%s/sessions", baseUrl, chatId);
        String apiKey=userConfig.getApiKeyByToken(token);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, String> body = Collections.singletonMap("name", name);

        // 直接使用 SessionResponseDTO 类型获取 Ragflow API 的完整响应
        Map<String, Object> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();

        // 检查响应状态码
        if (response == null || !"0".equals(response.get("code").toString())) {
            String errorMsg = response != null ?
                    response.getOrDefault("message", "创建会话失败").toString() :
                    "无响应";
            throw new RuntimeException("创建会话失败: " + errorMsg);
        }

        // 获取 data 部分并转换为 SessionResponseDTO
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(response.get("data"), SessionResponseDTO.class);
    }
    // 更新：现在sessionId从请求体获取
    public boolean updateSessionName(
            String token,
            String appid,
            String sessionId,
            String newName) {

        try {
            // 获取对应应用的聊天助手ID
            String chatId = userConfig.getChatIdByToken(token,appid);

            // Ragflow API URL格式 (chat_id在路径中，session_id在请求体中)
            String url = String.format(
                    "%s/api/v1/chats/%s/sessions/%s",
                    baseUrl, chatId, sessionId
            );

            String apiKey = userConfig.getApiKeyByToken(token);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 构建请求体 - 只包含name字段
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("name", newName);

            // 发送PUT请求
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK &&
                    response.getBody() != null) {

                Map<String, Object> body = response.getBody();
                Integer code = (Integer) body.get("code");
                return code != null && code == 0;
            }
            return false;

        } catch (HttpClientErrorException e) {
            System.err.println("会话更新失败：" + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("会话更新异常：" + e.getMessage());
            return false;
        }
    }

    public void deleteSessions(String token, String appid,List<String> ids) {
        String chatId = userConfig.getChatIdByToken(token,appid);
        String url = String.format("%s/api/v1/chats/%s/sessions", baseUrl, chatId);
        String apiKey=userConfig.getApiKeyByToken(token);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = Collections.singletonMap("ids", ids);

        restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(body, headers), Void.class);
    }
    public List<MessageResponse> getMessages(String token, String appid,String sessionId) {
        try {
            // 调用会话列表接口并过滤特定session
            String chatId = userConfig.getChatIdByToken(token,appid);
            String url = String.format("%s/api/v1/chats/%s/sessions?id=%s",
                    baseUrl, chatId, sessionId);

            String apiKey=userConfig.getApiKeyByToken(token);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);

            ResponseEntity<SessionListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    SessionListResponse.class
            );

            // 过滤并提取消息
            return Optional.ofNullable(response.getBody())
                    .map(SessionListResponse::getData)
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .findFirst()
                    .map(SessionDTO::getMessages)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(DtoConverter::convertMessage)
                    .collect(Collectors.toList());

        } catch (HttpClientErrorException e) {
            System.err.println("消息获取失败：" + e.getResponseBodyAsString());
            return Collections.emptyList();
        }
    }

public MessageResponse sendMessage(String token, String appid, String sessionId, String message) {
    String chatId = userConfig.getChatIdByToken(token,appid);
    String url = String.format("%s/api/v1/chats/%s/completions", baseUrl, chatId);

    String apiKey=userConfig.getApiKeyByToken(token);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + apiKey);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("question", message);
    requestBody.put("session_id", sessionId);
    requestBody.put("stream", false);

    try {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                new HttpEntity<>(requestBody, headers),
                Map.class
        );

        System.out.println("API响应体: " + response.getBody());

        return parseRagflowResponse(response.getBody());

    } catch (HttpClientErrorException e) {
        //log.error("API调用失败: {}", e.getResponseBodyAsString());
        return new MessageResponse("system", "服务暂时不可用");
    }
}

    // 在RagflowService中修改解析方法
    private MessageResponse parseRagflowResponse(Map<String, Object> response) {
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setRole("assistant");

        if (response.containsKey("data")) {

            Map<String, Object> data = (Map<String, Object>) response.get("data");

            // 解析回答内容
            if (data.containsKey("answer")) {
                messageResponse.setContent(data.get("answer").toString());
            }

            // 解析参考文档链接（核心修改部分）
            if (data.containsKey("reference")) {
                Object ref = data.get("reference");
                messageResponse.setReference(buildReferenceLinks(ref));
            }
        }
        return messageResponse;
    }

    // 生成符合前端要求的字符串
    private String buildReferenceLinks(Object ref) {
        List<String> linkPairs = new ArrayList<>();

        if (ref instanceof Map) {
            Map<?, ?> refMap = (Map<?, ?>) ref;
            Object docAggsObj = refMap.get("doc_aggs");

            if (docAggsObj instanceof List) {
                for (Object item : (List<?>) docAggsObj) {
                    if (item instanceof Map) {
                        Map<?, ?> agg = (Map<?, ?>) item;
                        String docId = getString(agg, "doc_id");
                        String docName = getString(agg, "doc_name");

                        // 生成标准化链接对
                        String pair = buildDocumentPair(docId, docName);
                        if (StringUtils.isNotBlank(pair)) {
                            linkPairs.add(pair);
                        }
                    }
                }
            }
        }

        return String.join(",", linkPairs); // 最终格式：title1|url1,title2|url2
    }

    // 核心链接生成方法
    private String buildDocumentPair(String docId, String docName) {
        if (StringUtils.isBlank(docId)) return "";

        // 1. 提取纯文件名（去除路径）
        String fileName = extractFileName(docName);

        // 2. 清理特殊字符作为标题
        String title = StringUtils.isNotBlank(fileName) ?
                fileName.replace(",", " ")  // 替换逗号
                        .replace("|", "-")   // 替换竖线
                : "未命名文档";

        // 3. 提取文件扩展名
        String ext = extractFileExtension(fileName);

        // 4. 构建固定prefix的URL
        String url = String.format("%s/document/%s?ext=%s&prefix=document",
                baseUrl,  // 需配置前端地址，如：http://127.0.0.1
                encodeURIComponent(docId),
                encodeURIComponent(ext));

        return String.format("%s|%s", title, url);
    }

    // 辅助方法：提取文件名（兼容Windows/Unix路径）
    private String extractFileName(String fullPath) {
        if (StringUtils.isBlank(fullPath)) return "";

        int lastSlashIndex = Math.max(
                fullPath.lastIndexOf('/'),
                fullPath.lastIndexOf('\\')
        );
        return (lastSlashIndex != -1) ?
                fullPath.substring(lastSlashIndex + 1) :
                fullPath;
    }

    // 辅助方法：提取文件扩展名
    private String extractFileExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) return "pdf";

        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) ?
                fileName.substring(lastDotIndex + 1) :
                "pdf";
    }

    // 辅助方法：安全获取字符串
    private String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return (value != null) ? value.toString() : "";
    }

    // 辅助方法：URL编码

    private String encodeURIComponent(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            // 实际不会触发此异常，因UTF-8是所有Java平台必须支持的编码
            return s;
        }
    }
/// 流式消息接口///////////////////////////////////////////////////////////////////////////
    public void streamMessage(String token, String appid, String sessionId, String message, SseEmitter emitter) {
        // 设置SSE超时时间（3分钟）
        emitter.onTimeout(() -> {
            System.out.println("SSE连接超时 sessionId:" + sessionId);
            emitter.complete();
        });
        emitter.onError(e -> System.err.println("SSE连接异常:" + e.getMessage()));

        String chatId = userConfig.getChatIdByToken(token,appid);
        String url = String.format("%s/api/v1/chats/%s/completions", baseUrl, chatId);
        String apiKey=userConfig.getApiKeyByToken(token);
        System.out.println("!!!!!!chatId="+chatId);
        System.out.println("!!!!!!apiKey="+apiKey);

        WebClient client = buildStreamWebClient();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("question", message);
        requestBody.put("session_id", sessionId);
        requestBody.put("stream", true);

        client.post()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                        chunk -> processStreamData(chunk, emitter),
                        error -> handleStreamError(error, emitter),
                        () -> completeStream(emitter)
                );
    }

    private void processStreamData(String chunk, SseEmitter emitter) {
        try {
            // 直接透传原始数据块
            System.out.println(chunk);
            emitter.send(SseEmitter.event()
                    .data(chunk)
                    .id(UUID.randomUUID().toString())
                    .reconnectTime(3000)
            );
        } catch (Exception e) {
            sendError(emitter, "Stream error: " + e.getClass().getSimpleName());
        }
    }

    private WebClient buildStreamWebClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB缓冲区
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofMinutes(3)) // 3分钟超时
                                .secure(ssl -> ssl.sslContext(
                                        SslContextBuilder.forClient()
                                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                ))
                ))
                .build();
    }


    private void handleStreamError(Throwable error, SseEmitter emitter) {
        String errorMsg = String.format("Stream error [%s]: %s",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                error.getMessage()
        );

        try {
            String errorEvent = String.format("event: error\ndata: %s\n\n",
                    new ObjectMapper().writeValueAsString(
                            Collections.singletonMap("error", errorMsg)
                    )
            );
            emitter.send(errorEvent);
        } catch (Exception e) {
            System.err.println("发送错误事件失败: " + e.getMessage());
        } finally {
            emitter.completeWithError(error);
        }
    }

    private void completeStream(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("complete"));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .data("{\"error\":\"" + message + "\"}")
                    .name("error")
            );
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
    /// ///////////////////////////////////////////////////////////////////

}