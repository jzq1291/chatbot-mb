package com.example.chatbot.util;

import com.example.chatbot.config.KeywordExtractorProperties;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.seg.Viterbi.ViterbiSegment;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeywordExtractor {
    private static final Logger logger = LoggerFactory.getLogger(KeywordExtractor.class);
    private final KeywordExtractorProperties properties;
    private final Segment segment;
    private final Map<String, CacheEntry> keywordCache;
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_EXPIRY_MS = 3600000; // 1 hour

    private static class CacheEntry {
        private final List<String> keywords;
        private final long timestamp;

        public CacheEntry(List<String> keywords) {
            this.keywords = keywords;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }

    private static boolean dictionaryInitialized = false;
    private static final Object lock = new Object();

    public KeywordExtractor(KeywordExtractorProperties properties) {
        this.properties = properties;
        this.segment = new ViterbiSegment();
        this.keywordCache = Collections.synchronizedMap(new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > MAX_CACHE_SIZE || eldest.getValue().isExpired();
            }
        });
        initializeDictionary();
    }

    private void initializeDictionary() {
        synchronized (lock) {
            if (!dictionaryInitialized) {
                try {
                    // 添加常用短语
                    properties.getCommonPhrases().forEach(phrase -> 
                        CustomDictionary.add(phrase, "nz 1024"));
                    
                    // 添加停止词，设置较低的词频
                    properties.getStopWords().forEach(stopWord -> 
                        CustomDictionary.add(stopWord, "x 1"));
                    
                    dictionaryInitialized = true;
                    logger.info("Dictionary initialized successfully");
                } catch (Exception e) {
                    logger.error("Failed to initialize dictionary", e);
                }
            }
        }
    }

    /**
     * 提取文本中的关键词
     * @param text 输入文本
     * @param maxKeywords 最大关键词数量
     * @return 关键词列表
     */
    public List<String> extractKeywords(String text, int maxKeywords) {
        String cacheKey = text + maxKeywords;
        CacheEntry cacheEntry = keywordCache.get(cacheKey);
        
        if (cacheEntry != null && !cacheEntry.isExpired()) {
            return cacheEntry.keywords;
        }
    
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }
    
        // 1. 分词
        List<Term> terms = segment.seg(text);
        
        // 2. 处理分词结果，组合有意义的词组
        List<String> phrases = processTerms(terms);
    
        // 3. 提取关键词并过滤短词
        List<String> keywords = extractKeywordsFromPhrases(phrases, maxKeywords);
        keywordCache.put(cacheKey, new CacheEntry(keywords));
        return keywords;
    }

    private List<String> processTerms(List<Term> terms) {
        List<String> phrases = new ArrayList<>();
        StringBuilder currentPhrase = new StringBuilder();
        
        for (int i = 0; i < terms.size(); i++) {
            Term term = terms.get(i);
            String word = term.word;
    
            if (isStopWord(word)) {
                addCurrentPhrase(phrases, currentPhrase);
                continue;
            }
    
            if (isValidNature(term.nature)) {
                processValidTerm(terms, i, phrases, currentPhrase);
            }
        }
    
        addCurrentPhrase(phrases, currentPhrase);
        return phrases;
    }

    private boolean isStopWord(String word) {
        return CustomDictionary.contains(word) && 
               CustomDictionary.get(word).toString().contains("x");
    }

    private boolean isValidNature(Nature nature) {
        return nature == Nature.n || // 名词
               nature == Nature.v || // 动词
               nature == Nature.a || // 形容词
               nature == Nature.i || // 成语
               nature == Nature.j || // 简称
               nature == Nature.l || // 习用语
               nature == Nature.nz;  // 其他专名
    }

    private void processValidTerm(List<Term> terms, int currentIndex, List<String> phrases, StringBuilder currentPhrase) {
        Term term = terms.get(currentIndex);
        String word = term.word;
    
        // 优先检查是否在自定义词典中
        if(CustomDictionary.contains(word)) {
            addCurrentPhrase(phrases, currentPhrase);
            phrases.add(word);
            return;
        }
    
        // 如果当前词长度足够，直接添加
        if (word.length() >= properties.getMinWordLength()) {
            addCurrentPhrase(phrases, currentPhrase);
            phrases.add(word);
        } else {
            // 尝试与下一个词组合
            tryCombineWithNextTerm(terms, currentIndex, currentPhrase);
        }
    }

    private void tryCombineWithNextTerm(List<Term> terms, int currentIndex, StringBuilder currentPhrase) {
        if (currentIndex < terms.size() - 1) {
            Term nextTerm = terms.get(currentIndex + 1);
            if (isValidNature(nextTerm.nature)) {
                String combined = terms.get(currentIndex).word + nextTerm.word;
                currentPhrase.append(combined);
            } else {
                currentPhrase.append(terms.get(currentIndex).word);
            }
        } else {
            currentPhrase.append(terms.get(currentIndex).word);
        }
    }

    private void addCurrentPhrase(List<String> phrases, StringBuilder currentPhrase) {
        if (!currentPhrase.isEmpty()) {
            phrases.add(currentPhrase.toString());
            currentPhrase.setLength(0);
        }
    }

    private List<String> extractKeywordsFromPhrases(List<String> phrases, int maxKeywords) {
        try {
            String text = String.join(" ", phrases);
            int minWordLength = properties.getMinWordLength();
        
            // 使用TextRank算法提取关键词

            return HanLP.extractKeyword(text, maxKeywords).stream()
                .filter(k -> k.length() >= minWordLength)
                .limit(maxKeywords)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error extracting keywords", e);
            return new ArrayList<>();
        }
    }

    /**
     * 使用默认参数提取关键词
     * @param text 输入文本
     * @return 关键词列表
     */
    public List<String> extractKeywords(String text) {
        return extractKeywords(text, properties.getDefaultKeywordCount());
    }

    public List<String> extractKeywordsFromArticle(String article, int maxKeywords) {
        String[] paragraphs = article.split("\n\n"); // 按段落分割
        Map<String, Double> keywordWeights = new HashMap<>();
        
        // 处理标题（第一段）
        if (paragraphs.length > 0) {
            List<String> titleKeywords = extractKeywords(paragraphs[0], maxKeywords);
            for (String keyword : titleKeywords) {
                keywordWeights.put(keyword, 2.0); // 标题权重为2
            }
        }
        
        // 处理正文
        for (int i = 1; i < paragraphs.length; i++) {
            List<String> bodyKeywords = extractKeywords(paragraphs[i], maxKeywords);
            for (String keyword : bodyKeywords) {
                keywordWeights.merge(keyword, 1.0, Double::sum); // 正文权重为1
            }
        }
        
        // 按权重排序并返回前N个关键词
        return keywordWeights.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(maxKeywords)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
}