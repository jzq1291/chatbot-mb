package com.example.chatbot.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {
    private PoolConfig async;
    private PoolConfig mvc;

    @Data
    public static class PoolConfig {
        private int coreSize;
        private int maxSize;
        private int queueCapacity;
        private String threadNamePrefix;
        private int keepAliveSeconds;
        private Integer timeoutMilliseconds; // 仅MVC配置需要
    }
} 