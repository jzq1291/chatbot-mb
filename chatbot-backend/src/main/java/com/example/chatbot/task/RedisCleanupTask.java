package com.example.chatbot.task;

import com.example.chatbot.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCleanupTask {
    private final RedisService redisService;

    @Scheduled(cron = "0 0 0 * * ?") // 每天凌晨执行
    public void cleanupExpiredData() {
        redisService.removeExpiredHotKnowledge();
    }
} 