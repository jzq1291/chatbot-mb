package com.example.chatbot.controller;

import com.example.chatbot.dto.ChatRequest;
import com.example.chatbot.dto.ChatResponse;
import com.example.chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.getModelId() == null) {
            request.setModelId("qwen3");
        }
        ChatResponse response = chatService.processMessage(request);
        return ResponseEntity.ok(response);
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
