package com.example.controller;

import com.example.config.UserConfigManager;
import com.example.dto.*;
import com.example.service.RagflowService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ChatController {


    @Autowired
    private RagflowService ragflowService;

    @Autowired
    private UserConfigManager userConfig;

    // 登录接口
    @PostMapping("/login")
    public Response<String> login(@RequestBody LoginRequest request) {

        String token = userConfig.getTokenByUserId(request.getMobile());
        if (token == null || token.isEmpty()) {
            return Response.error(101,"invalid phone NO");
        }
        return Response.success(token);
    }

    @GetMapping("/sessions")
    public Response<List<SessionDTO>> getSessions(@RequestHeader("token") String token, @RequestHeader("appid") String appid) {

        String apiKey=userConfig.getApiKeyByToken(token);
        if (apiKey == null || apiKey.isEmpty())
          return Response.error(101,"invalid token "+token);

        List<SessionDTO> sessions = ragflowService.getSessions(token,appid);
        return Response.success(sessions);
    }

    @PostMapping("/sessions")
    public Response<SessionResponseDTO> createSession(@RequestBody SessionRequest request,
                                                      @RequestHeader("token") String token,
                                                      @RequestHeader("appid") String appid) {
        try {
            String apiKey=userConfig.getApiKeyByToken(token);
            if (apiKey == null || apiKey.isEmpty())
                return Response.error(101,"invalid token "+token);

            SessionResponseDTO sessionResponse = ragflowService.createSession(token,appid,request.getName());
            return Response.success(sessionResponse);
        } catch (Exception e) {
            return Response.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "创建会话失败: " + e.getMessage());
        }
    }

    @PutMapping("/sessions/update")
    public Response<Void> updateSession(
            @RequestBody UpdateSessionRequest request,
            @RequestHeader("token") String token,
            @RequestHeader("appid") String appid) {

        String apiKey = userConfig.getApiKeyByToken(token);
        if (apiKey == null || apiKey.isEmpty()) {
            return Response.error(101, "invalid token " + token);
        }

        if (StringUtils.isBlank(request.getSessionId())) {
            return Response.error(400, "sessionId cannot be null");
        }

        // 调用服务层更新会话名称
        boolean success = ragflowService.updateSessionName(
                token,
                appid,
                request.getSessionId(),
                request.getName()
        );

        return success ?
                Response.success(null) :
                Response.error(500, "更新会话名称失败");
    }

    @DeleteMapping("/sessions")
    public Response<Void> deleteSessions(@RequestBody DeleteRequest request,
                                         @RequestHeader("token") String token,
                                         @RequestHeader("appid") String appid) {
        String apiKey=userConfig.getApiKeyByToken(token);
        if (apiKey == null || apiKey.isEmpty())
            return Response.error(101,"invalid token "+token);

        ragflowService.deleteSessions(token, appid, request.getIds());
        return Response.success(null);
    }

    @GetMapping("/messages")
    public Response<List<MessageResponse>> getMessages(@RequestParam String sessionId,
                                                       @RequestHeader("token") String token,
                                                       @RequestHeader("appid") String appid) {
        String apiKey=userConfig.getApiKeyByToken(token);
        if (apiKey == null || apiKey.isEmpty())
            return Response.error(101,"invalid token "+token);

        List<MessageResponse> messages = ragflowService.getMessages(token,appid, sessionId);
        return Response.success(messages);
    }

    @PostMapping("/messages")
    public Response<MessageResponse> sendMessage(@RequestBody MessageRequest request,
                                                 @RequestHeader("token") String token,
                                                 @RequestHeader("appid") String appid) {
        try {
            String apiKey=userConfig.getApiKeyByToken(token);
            if (apiKey == null || apiKey.isEmpty())
                return Response.error(101,"invalid token "+token);

            MessageResponse response = ragflowService.sendMessage(
                    token,appid,
                    request.getSessionId(),
                    request.getMessage()
            );
            return Response.success(response);
        } catch (Exception e) {
            return Response.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "消息处理失败: " + e.getMessage());
        }
    }

    // SSE流接口保持原样
    @PostMapping(value = "/messages/stream", produces = "text/event-stream")
    public SseEmitter handleStreamRequest(
            @RequestBody MessageRequest request, @RequestHeader("token") String token,
            @RequestHeader("appid") String appid) {

        /*
        String apiKey=chatConfig.getApikeyByToken(token);
        if (apiKey == null || apiKey.isEmpty())
            return Response.error(101,"invalid token "+token);
         */

        SseEmitter emitter = new SseEmitter(180000L);
        try {
            ragflowService.streamMessage(token,
                    appid,
                    request.getSessionId(),
                    request.getMessage(),
                    emitter
            );
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }


}