package com.example.chatbot.service;

import com.example.chatbot.dto.AuthRequest;
import com.example.chatbot.dto.AuthResponse;

public interface AuthService {
    AuthResponse authenticate(AuthRequest request);
    AuthResponse register(AuthRequest request);
    void logout(String authHeader);
} 