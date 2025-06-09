# Tensor-AI 后端 CORS 配置说明

## 概述

本文档说明 Tensor-AI 后端的 CORS（跨域资源共享）配置，以支持前端应用直接调用后端 API。

## 修改内容

### 1. WebConfig.java 优化

**文件位置**: `src/main/java/com/example/config/WebConfig.java`

**主要改动**:
- 将 CORS 配置从允许所有来源改为只允许特定的前端地址
- 限制允许的路径为 `/api/**`
- 明确指定允许的请求头，包括自定义的 `token` 和 `appid`
- 添加详细的中文注释

**配置详情**:
```java
.allowedOrigins(
    "http://localhost:3000",    // React开发服务器
    "http://127.0.0.1:3000",   // 本地回环地址
    "http://192.168.1.131:3000" // 局域网地址（如需要）
)
.allowedHeaders(
    "Content-Type",
    "Authorization", 
    "token",        // 自定义认证头
    "appid",        // 应用ID头
    "X-Requested-With",
    "Accept",
    "Origin"
)
```

### 2. ChatController.java 清理

**文件位置**: `src/main/java/com/example/controller/ChatController.java`

**主要改动**:
- 移除类级别的 `@CrossOrigin` 注解
- 统一在 `WebConfig` 中管理 CORS 配置
- 添加类级别的中文注释

## 服务器配置

### 端口配置
- **后端端口**: 8080 (在 `application.yaml` 中配置)
- **前端端口**: 3000 (React 开发服务器默认端口)

### RAGFlow API 配置
- **RAGFlow 服务地址**: `http://127.0.0.1:9380`
- **后端作为代理**: Tensor-AI 后端会转发请求到 RAGFlow 服务

## 使用方法

### 1. 启动后端服务

```bash
# 进入 Tensor-AI 目录
cd /home/tensortec/桌面/Tensor-AI

# 编译项目
./mvnw clean package -DskipTests

# 启动服务
java -jar target/ragflow-api-1.0.0.jar
```

### 2. 启动前端应用

```bash
# 进入前端目录
cd /home/tensortec/桌面/ragflow-chat

# 使用直接调用模式启动
./start-direct.sh
```

## API 端点

### 认证相关
- `POST /api/login` - 用户登录

### 会话管理
- `GET /api/sessions` - 获取会话列表
- `POST /api/sessions` - 创建新会话
- `PUT /api/sessions/update` - 更新会话名称
- `DELETE /api/sessions` - 删除会话

### 消息处理
- `GET /api/messages` - 获取消息历史
- `POST /api/messages` - 发送消息（非流式）
- `POST /api/messages/stream` - 发送消息（流式响应）

## 流式响应支持

后端使用 Spring Boot 的 `SseEmitter` 实现流式响应：

```java
@PostMapping(value = "/messages/stream", produces = "text/event-stream")
public SseEmitter handleStreamRequest(
    @RequestBody MessageRequest request, 
    @RequestHeader("token") String token,
    @RequestHeader("appid") String appid) {
    // 流式处理逻辑
}
```

## 安全特性

### CORS 安全配置
1. **限制来源**: 只允许特定的前端地址访问
2. **路径限制**: 只允许访问 `/api/**` 路径
3. **头部控制**: 明确指定允许的请求头
4. **凭证支持**: 允许发送认证信息

### 认证机制
- 使用自定义 `token` 头进行用户认证
- 使用 `appid` 头标识应用类型
- 支持基于配置文件的用户管理

## 故障排除

### 1. CORS 错误
如果前端出现 CORS 错误，检查：
- 后端服务是否正常运行在 8080 端口
- 前端地址是否在允许的来源列表中
- 请求头是否包含必要的 `token` 和 `appid`

### 2. 流式响应问题
如果流式响应不工作：
- 确认请求 Content-Type 为 `application/json`
- 确认响应 Content-Type 为 `text/event-stream`
- 检查网络连接和代理设置

### 3. 认证失败
如果认证失败：
- 检查 `user-config.yaml` 中的用户配置
- 确认 token 是否有效
- 验证 appid 是否正确

## 开发建议

1. **本地开发**: 使用 `http://localhost:3000` 作为前端地址
2. **网络测试**: 可以添加局域网 IP 到允许的来源列表
3. **生产部署**: 根据实际部署环境修改允许的来源地址
4. **日志调试**: 启用 DEBUG 级别日志查看详细信息

## 配置文件位置

- **CORS 配置**: `src/main/java/com/example/config/WebConfig.java`
- **服务器配置**: `src/main/resources/application.yaml`
- **用户配置**: `src/main/resources/user-config.yaml`
- **RAGFlow 配置**: `src/main/java/com/example/config/RagflowConfig.java`

---

**注意**: 修改配置后需要重新编译和启动后端服务才能生效。