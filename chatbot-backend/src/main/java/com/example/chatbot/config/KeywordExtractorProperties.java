package com.example.chatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "keyword.extractor")
public class KeywordExtractorProperties {
    private int minWordLength = 3;
    private int minKeywordCount = 3;
    private int defaultKeywordCount = 5;
    private List<String> stopWords;
    private List<String> commonPhrases;
} 