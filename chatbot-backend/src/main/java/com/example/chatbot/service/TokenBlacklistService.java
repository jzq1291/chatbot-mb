package com.example.chatbot.service;

public interface TokenBlacklistService {
    void addToBlacklist(String token, long expirationTime);
    boolean isBlacklisted(String token);
} 