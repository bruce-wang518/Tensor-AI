package com.example.controller;

import com.example.dto.*;
import com.example.service.RagflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {


    @Autowired
    private RagflowService ragflowService;

    // 登录接口
    @PostMapping("/login")
    public Response<String> login(@RequestBody LoginRequest request) {
        return Response.success("token_123456");
    }

    @GetMapping("/sessions")
    public Response<List<SessionDTO>> getSessions(@RequestHeader("token") String token,
                                                  @RequestHeader("appid") String appid) {
        System.out.println("########appid=" + appid);
        List<SessionDTO> sessions = ragflowService.getSessions(appid);
        return Response.success(sessions);
    }

    @PostMapping("/sessions")
    public Response<SessionResponseDTO> createSession(@RequestBody SessionRequest request,
                                                      @RequestHeader("token") String token,
                                                      @RequestHeader("appid") String appid) {
        try {
            SessionResponseDTO sessionResponse = ragflowService.createSession(appid, request.getName());
            return Response.success(sessionResponse);
        } catch (Exception e) {
            return Response.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "创建会话失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/sessions")
    public Response<Void> deleteSessions(@RequestBody DeleteRequest request,
                                         @RequestHeader("token") String token,
                                         @RequestHeader("appid") String appid) {
        ragflowService.deleteSessions(appid, request.getIds());
        return Response.success(null);
    }

    @GetMapping("/messages")
    public Response<List<MessageResponse>> getMessages(@RequestParam String sessionId,
                                                       @RequestHeader("token") String token,
                                                       @RequestHeader("appid") String appid) {
        List<MessageResponse> messages = ragflowService.getMessages(appid, sessionId);
        return Response.success(messages);
    }

    @PostMapping("/messages")
    public Response<MessageResponse> sendMessage(@RequestBody MessageRequest request,
                                                 @RequestHeader("token") String token,
                                                 @RequestHeader("appid") String appid) {
        try {
            MessageResponse response = ragflowService.sendMessage(
                    appid,
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

        SseEmitter emitter = new SseEmitter(180000L);
        try {
            ragflowService.streamMessage(
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