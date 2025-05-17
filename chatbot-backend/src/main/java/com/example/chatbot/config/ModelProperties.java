package com.example.chatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "chatbot.model")
public class ModelProperties {
    private Map<String, ModelOption> options;

    @Data
    public static class ModelOption {
        private String model;
        private Double temperature;
        private Double topP;
        private Integer topK;
    }
} 