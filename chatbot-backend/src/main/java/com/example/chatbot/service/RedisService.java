package com.example.chatbot.service;

import com.example.chatbot.entity.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String HOT_KNOWLEDGE_KEY = "hot_knowledge";
    private static final String KNOWLEDGE_DATA_KEY = "knowledge_data:";
    private static final String KEYWORD_INDEX_KEY = "keyword_index:";
    private static final double HOT_THRESHOLD = 5.0;
    private static final int MAX_KEYWORDS_PER_DOC = 10;
    private static final long DEFAULT_EXPIRATION_DAYS = 7;

    public void saveDocToRedis(KnowledgeBase knowledge) {
        String docId = knowledge.getId().toString();
        
        // 1. 更新热门知识分数
        redisTemplate.opsForZSet().incrementScore(HOT_KNOWLEDGE_KEY, docId, 1);
        
        // 2. 存储完整知识数据，设置过期时间
        redisTemplate.opsForValue().set(KNOWLEDGE_DATA_KEY + docId, knowledge, 
            java.time.Duration.ofDays(DEFAULT_EXPIRATION_DAYS));
        
        // 3. 更新关键词索引
        updateKeywordIndex(knowledge);
    }

    private void updateKeywordIndex(KnowledgeBase knowledge) {
        String docId = knowledge.getId().toString();
        
        // 从标题和内容中提取关键词
        Set<String> keywords = extractKeywords(knowledge.getTitle() + " " + knowledge.getContent());
        
        // 为每个关键词创建索引
        for (String keyword : keywords) {
            String keywordKey = KEYWORD_INDEX_KEY + keyword.toLowerCase();
            redisTemplate.opsForSet().add(keywordKey, docId);
        }
    }

    private Set<String> extractKeywords(String text) {
        // 简单的关键词提取：分词并过滤停用词
        return Arrays.stream(text.toLowerCase().split("\\s+"))
                .filter(word -> word.length() > 2) // 过滤短词
                .limit(MAX_KEYWORDS_PER_DOC)
                .collect(Collectors.toSet());
    }

    public void incrementKnowledgeScore(String knowledgeId) {
        redisTemplate.opsForZSet().incrementScore(HOT_KNOWLEDGE_KEY, knowledgeId, 1);
        // 每次访问时重置过期时间
        redisTemplate.expire(KNOWLEDGE_DATA_KEY + knowledgeId, 
            java.time.Duration.ofDays(DEFAULT_EXPIRATION_DAYS));
    }

    public List<KnowledgeBase> searchKnowledge(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] searchTerms = query.toLowerCase().split("\\s+");
        Set<String> matchedDocIds = new HashSet<>();
        
        // 1. 首先检查关键词索引
        for (String term : searchTerms) {
            String keywordKey = KEYWORD_INDEX_KEY + term;
            Set<Object> docIds = redisTemplate.opsForSet().members(keywordKey);
            if (docIds != null) {
                matchedDocIds.addAll(docIds.stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet()));
            }
        }

        // 2. 如果找到匹配的文档，直接返回
        if (!matchedDocIds.isEmpty()) {
            return matchedDocIds.stream()
                    .map(docId -> (KnowledgeBase) redisTemplate.opsForValue().get(KNOWLEDGE_DATA_KEY + docId))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 3. 如果没有找到匹配的文档，返回热门知识
        return getHotKnowledge();
    }

    public List<KnowledgeBase> getHotKnowledge() {
        Set<ZSetOperations.TypedTuple<Object>> hotItems = redisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_KNOWLEDGE_KEY, 0, -1);
        
        if (hotItems == null) {
            return Collections.emptyList();
        }

        return hotItems.stream()
                .filter(tuple -> {
                    Double score = tuple.getScore();
                    return score != null && score >= HOT_THRESHOLD;
                })
                .map(tuple -> {
                    String id = Objects.requireNonNull(tuple.getValue()).toString();
                    return (KnowledgeBase) redisTemplate.opsForValue().get(KNOWLEDGE_DATA_KEY + id);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void removeExpiredHotKnowledge() {
        // 获取所有热门知识及其分数
        Set<ZSetOperations.TypedTuple<Object>> allItems = redisTemplate.opsForZSet()
                .rangeWithScores(HOT_KNOWLEDGE_KEY, 0, -1);
        
        if (allItems == null) {
            return;
        }

        // 只删除访问次数低于阈值的数据
        allItems.stream()
                .filter(tuple -> {
                    Double score = tuple.getScore();
                    return score == null || score < HOT_THRESHOLD;
                })
                .forEach(tuple -> {
                    String id = Objects.requireNonNull(tuple.getValue()).toString();
                    // 删除知识数据
                    redisTemplate.delete(KNOWLEDGE_DATA_KEY + id);
                    // 从热门知识集合中删除
                    redisTemplate.opsForZSet().remove(HOT_KNOWLEDGE_KEY, id);
                    // 删除相关的关键词索引
                    deleteKeywordIndex(id);
                });
    }

    private void deleteKeywordIndex(String docId) {
        // 获取所有关键词索引键
        Set<String> keywordKeys = redisTemplate.keys(KEYWORD_INDEX_KEY + "*");
        for (String key : keywordKeys) {
            redisTemplate.opsForSet().remove(key, docId);
        }
    }
} 