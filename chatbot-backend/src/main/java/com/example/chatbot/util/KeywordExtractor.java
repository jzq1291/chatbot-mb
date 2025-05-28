package com.example.chatbot.util;

import com.example.chatbot.config.KeywordExtractorProperties;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.seg.Dijkstra.DijkstraSegment;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KeywordExtractor {
    private final KeywordExtractorProperties properties;
    private final Segment segment;
    private final Map<String, CacheEntry> keywordCache;
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_EXPIRY_MS = 3600000; // 1 hour
    private static boolean dictionaryInitialized = false;
    private static final Object lock = new Object();

    public KeywordExtractor(KeywordExtractorProperties properties) {
        this.properties = properties;
        this.segment = new DijkstraSegment();
        this.keywordCache = new ConcurrentHashMap<>(MAX_CACHE_SIZE);
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
                    log.info("Dictionary initialized successfully");
                } catch (Exception e) {
                    log.error("Failed to initialize dictionary", e);
                }
            }
        }
    }

    /**
     * 提取文本中的关键词
     *
     * @param text        输入文本
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
        
        for (int i = 0; i < terms.size(); i++) {
            Term term = terms.get(i);
            String word = term.word;
            Nature nature = term.nature;
    
            // 跳过停止词和标点符号
            if (isStopWord(word) || nature == Nature.w) {
                continue;
            }
    
            // 处理专有名词和自定义词典中的词
            if (nature == Nature.nz || nature == Nature.gi || CustomDictionary.contains(word)) {
                phrases.add(word);
                continue;
            }
    
            // 尝试与下一个词组合
            if (i < terms.size() - 1) {
                Term nextTerm = terms.get(i + 1);
                if (!isStopWord(nextTerm.word) && nextTerm.nature != Nature.w) {
                    // 检查当前词和下一个词是否可以组合
                    if (canCombine(nature, nextTerm.nature)) {
                        String combined = word + nextTerm.word;
                        phrases.add(combined);
                        i++; // 跳过下一个词
                        continue;
                    }
                }
            }
            
            // 如果不需要组合或无法组合，则单独添加当前词
            if (isValidNature(nature)) {
                phrases.add(word);
            }
        }
        
        return phrases;
    }

    private boolean isStopWord(String word) {
        return CustomDictionary.contains(word) &&
                CustomDictionary.get(word).toString().contains("x");
    }

    private boolean canCombine(Nature currentNature, Nature nextNature) {
        // 名词 + 名词
        if ((currentNature == Nature.n || currentNature == Nature.ng) && 
            (nextNature == Nature.n || nextNature == Nature.ng)) {
            return true;
        }
        // 名词 + 动词
        if ((currentNature == Nature.n || currentNature == Nature.ng) && 
            nextNature == Nature.v) {
            return true;
        }
        // 动词 + 名词
        if (currentNature == Nature.v && 
            (nextNature == Nature.n || nextNature == Nature.ng)) {
            return true;
        }
        // 形容词 + 名词
        if (currentNature == Nature.a && 
            (nextNature == Nature.n || nextNature == Nature.ng)) {
            return true;
        }
        // 名词 + 形容词
        return (currentNature == Nature.n || currentNature == Nature.ng) &&
                nextNature == Nature.a;
    }

    private boolean isValidNature(Nature nature) {
        return nature == Nature.n || // 名词
               nature == Nature.nz || // 专有名词
               nature == Nature.ng || // 名语素
               nature == Nature.nr || // 人名
               nature == Nature.v ||  // 动词
               nature == Nature.a ||  // 形容词
               nature == Nature.gi;   // 技术名词
    }

    private List<String> extractKeywordsFromPhrases(List<String> phrases, int maxKeywords) {
        try {
            // 将处理后的词组重新组合成文本，用空格分隔
            String text = String.join(" ", phrases);
            
            // 使用HanLP的TextRank算法对原始词组进行排序
            List<String> rankedPhrases = HanLP.extractKeyword(text, maxKeywords * 3);
            
            // 从原始词组中筛选出在rankedPhrases中出现的关键词
            return phrases.stream()
                .filter(phrase -> phrase.length() >= 2) // 过滤掉单字词
                .filter(phrase -> {
                    // 检查词组是否在rankedPhrases中或其子串中
                    return rankedPhrases.stream()
                        .anyMatch(ranked -> ranked.contains(phrase) || phrase.contains(ranked));
                })
                .sorted((a, b) -> {
                    // 首先按长度降序排序
                    int lengthCompare = Integer.compare(b.length(), a.length());
                    if (lengthCompare != 0) {
                        return lengthCompare;
                    }
                    // 长度相同时，按在rankedPhrases中的位置排序
                    int aIndex = rankedPhrases.indexOf(a);
                    int bIndex = rankedPhrases.indexOf(b);
                    if (aIndex != -1 && bIndex != -1) {
                        return Integer.compare(aIndex, bIndex);
                    }
                    // 如果不在rankedPhrases中，按在文本中的位置排序
                    return Integer.compare(text.indexOf(a), text.indexOf(b));
                })
                .filter(phrase -> phrase.length() > properties.getMinWordLength())
                .limit(maxKeywords)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error extracting keywords", e);
            return new ArrayList<>();
        }
    }

    /**
     * 使用默认参数提取关键词
     *
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
                keywordWeights.merge(keyword, 5.0, Double::sum); // 标题权重为5
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

    // 缓存条目类，用于存储关键词和过期时间
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

    /**
     * 清理过期的缓存条目并确保缓存不超过最大大小
     */
    public void cleanupKeywordCache() {
        // 移除过期数据
        keywordCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // 确保缓存不超过最大大小
        while (keywordCache.size() > MAX_CACHE_SIZE) {
            java.util.Iterator<java.util.Map.Entry<String, CacheEntry>> iterator = keywordCache.entrySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }
}