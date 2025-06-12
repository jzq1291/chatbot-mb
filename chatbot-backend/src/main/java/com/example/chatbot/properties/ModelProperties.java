package com.example.chatbot.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
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