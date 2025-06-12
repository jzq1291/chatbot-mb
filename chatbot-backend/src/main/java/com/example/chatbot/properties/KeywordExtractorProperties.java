package com.example.chatbot.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "keyword.extractor")
public class KeywordExtractorProperties {
    private int minWordLength;
    private int minKeywordCount;
    private int defaultKeywordCount;
    private List<String> stopWords;
    private List<String> commonPhrases;
} 