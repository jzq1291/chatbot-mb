package com.example.chatbot.controller;

import com.example.chatbot.dto.ChatRequest;
import com.example.chatbot.dto.ChatResponse;
import com.example.chatbot.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @PostMapping("/send")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.getModelId() == null) {
            request.setModelId("qwen3");
        }
        ChatResponse response = chatService.processMessage(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/send/reactive", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatReactive(@RequestBody ChatRequest request) {
        if (request.getModelId() == null) {
            request.setModelId("qwen3");
        }
        
        // 用于跟踪是否已经过了think块
        final boolean[] passedThinkBlock = {false};
        
        return chatService.processMessageReactive(request)
                .mapNotNull(response -> {
                    try {
                        String message = response.getMessage();
                        
                        // 如果遇到</think>，标记可以开始发送内容
                        if (!passedThinkBlock[0] && message.contains("</think>")) {
                            passedThinkBlock[0] = true;
                            return null;
                        }
                        
                        // 只有在过了think块后才发送内容
                        if (passedThinkBlock[0]) {
                            // 移除开头的换行符
                            message = message.replaceAll("^\\n+", "");
                            response.setMessage(message);
                            return objectMapper.writeValueAsString(response);
                        }
                        
                        return null;
                    } catch (Exception e) {
                        return "data: {\"error\": \"" + e.getMessage() + "\"}\n\n";
                    }
                })
                .filter(str -> str != null && !str.trim().isEmpty()); // 过滤掉空消息和think块内容
    }

    @GetMapping("/models")
    public ResponseEntity<List<String>> getAvailableModels() {
        return ResponseEntity.ok(List.of("qwen3", "deepseekR1"));
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatResponse>> getHistory(@PathVariable String sessionId) {
        List<ChatResponse> history = chatService.getHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<String>> getAllSessions() {
        List<String> sessions = chatService.getAllSessions();
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
