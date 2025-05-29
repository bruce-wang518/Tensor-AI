package com.example.controller;

import com.example.dto.*;
import com.example.service.RagflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {
    @Autowired
    private RagflowService ragflowService;

    @GetMapping("/sessions")
    public List<SessionDTO> getSessions(@RequestHeader("tab") String tab) {
        System.out.println("########tab=" + tab);
        return ragflowService.getSessions(tab);
    }

    @PostMapping("/sessions")
    public void createSession(@RequestBody SessionRequest request,
                              @RequestHeader("tab") String tab) {
        ragflowService.createSession(tab, request.getName());
    }

    @DeleteMapping("/sessions")
    public void deleteSessions(@RequestBody DeleteRequest request,
                               @RequestHeader("tab") String tab) {
        ragflowService.deleteSessions(tab, request.getIds());
    }

    @GetMapping("/messages")
    public List<MessageResponse> getMessages(@RequestParam String sessionId,
                                             @RequestHeader("tab") String tab) {
        return ragflowService.getMessages(tab, sessionId);
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> sendMessage(@RequestBody MessageRequest request,
                                                       @RequestHeader("tab") String tab) {
        try {
            MessageResponse response = ragflowService.sendMessage(
                    tab,
                    request.getSessionId(),
                    request.getMessage()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("system", "消息处理失败: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/messages/stream", produces = "text/event-stream")
    public SseEmitter handleStreamRequest(
            @RequestBody MessageRequest request,
            @RequestHeader("tab") String tab) {

        SseEmitter emitter = new SseEmitter(180000L); // 避免使用下划线数字字面量
        try {
            ragflowService.streamMessage(
                    tab,
                    request.getSessionId(),
                    request.getMessage(),
                    emitter
            );
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    class SessionRequest {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    class DeleteRequest {
        private List<String> ids;

        public List<String> getIds() {
            return ids;
        }

        public void setIds(List<String> ids) {
            this.ids = ids;
        }
    }
}