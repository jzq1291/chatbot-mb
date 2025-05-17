package com.example.chatbot.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ChatResponse {
    private String message;
    private String sessionId;
    private String role;
    private String modelId;
} 