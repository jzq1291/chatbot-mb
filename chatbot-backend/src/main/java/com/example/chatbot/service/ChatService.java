package com.example.chatbot.service;

import com.example.chatbot.dto.ChatRequest;
import com.example.chatbot.dto.ChatResponse;

import java.util.List;

public interface ChatService {
    ChatResponse processMessage(ChatRequest request);
    List<ChatResponse> getHistory(String sessionId);
    List<String> getAllSessions();
    void deleteSession(String sessionId);
}