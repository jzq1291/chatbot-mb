package com.example.chatbot.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String sessionId;
    private String modelId;
} 