package com.example.chatbot.task;

import com.example.chatbot.service.impl.RedisService;
import com.example.chatbot.util.KeywordExtractor; // 添加导入语句
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCleanupTask {
    private final RedisService redisService;
    private final KeywordExtractor keywordExtractor; // 注入 KeywordExtractor

    @Scheduled(cron = "0 0 0 * * ?") // 每天凌晨执行
    public void cleanupExpiredData() {
        redisService.removeExpiredHotKnowledge();
        keywordExtractor.cleanupKeywordCache(); // 调用清理方法
    }
}