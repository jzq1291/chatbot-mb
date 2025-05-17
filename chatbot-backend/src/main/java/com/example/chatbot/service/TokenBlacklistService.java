package com.example.chatbot.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        // 每天清理过期的 token
        scheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 24, 24, TimeUnit.HOURS);
    }

    @PreDestroy
    public void cleanup() {
        scheduler.shutdown();
    }

    public void addToBlacklist(String token, long expirationTime) {
        blacklist.put(token, expirationTime);
    }

    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    private void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < currentTime);
    }
} 