package com.example.chatbot.config;

import com.example.chatbot.service.impl.VectorSearchServiceImpl;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorSearchConfig {
    
    private final VectorSearchServiceImpl vectorSearchService;
    
    public VectorSearchConfig(VectorSearchServiceImpl vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }
    
    @PostConstruct
    public void init() {
        vectorSearchService.init();
    }
} 