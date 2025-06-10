package com.example.chatbot.service.impl;

import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.util.KeywordExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final KeywordExtractor keywordExtractor;
    private static final String HOT_KNOWLEDGE_KEY = "hot_knowledge";
    private static final String KNOWLEDGE_DATA_KEY = "knowledge_data:";
    private static final String KEYWORD_INDEX_KEY = "keyword_index:";
    private static final double HOT_THRESHOLD = 5.0;     // 访问超过5次被列为热知识，不自动清理
    private static final int MAX_KEYWORDS_PER_DOC = 5;   // 每篇文章提取5个关键词
    private static final long DEFAULT_EXPIRATION_DAYS = 7;
    private static final int MAX_HOT_KNOWLEDGE_NUMBER = 50;  // 热知识最大数量

    public void saveDocToRedis(KnowledgeBase knowledge) {
        String docId = knowledge.getId().toString();
        
        // 1. 检查并维护热知识集合大小
        Long currentSize = redisTemplate.opsForZSet().size(HOT_KNOWLEDGE_KEY);
        if (currentSize != null && currentSize >= MAX_HOT_KNOWLEDGE_NUMBER) {
            // 如果达到最大数量，移除分数最低的条目
            Set<ZSetOperations.TypedTuple<Object>> lowestScoreItems = redisTemplate.opsForZSet()
                    .rangeWithScores(HOT_KNOWLEDGE_KEY, 0, 0);
            if (lowestScoreItems != null && !lowestScoreItems.isEmpty()) {
                ZSetOperations.TypedTuple<Object> lowestItem = lowestScoreItems.iterator().next();
                if (lowestItem != null && lowestItem.getValue() != null) {
                    String lowestId = lowestItem.getValue().toString();
                    // 只从热知识集合中移除分数最低的条目
                    redisTemplate.opsForZSet().remove(HOT_KNOWLEDGE_KEY, lowestId);
                }
            }
        }
        
        // 2. 更新热门知识分数
        redisTemplate.opsForZSet().incrementScore(HOT_KNOWLEDGE_KEY, docId, 1);
        
        // 3. 存储完整知识数据，设置过期时间
        redisTemplate.opsForValue().set(KNOWLEDGE_DATA_KEY + docId, knowledge, 
            java.time.Duration.ofDays(DEFAULT_EXPIRATION_DAYS));
        
        // 4. 更新关键词索引
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
        return new HashSet<>(keywordExtractor.extractKeywordsFromArticle(text, MAX_KEYWORDS_PER_DOC));
    }

    public void incrementKnowledgeScore(String knowledgeId) {
        redisTemplate.opsForZSet().incrementScore(HOT_KNOWLEDGE_KEY, knowledgeId, 1);
        // 每次访问时重置过期时间
        redisTemplate.expire(KNOWLEDGE_DATA_KEY + knowledgeId, 
            java.time.Duration.ofDays(DEFAULT_EXPIRATION_DAYS));
    }

    public List<KnowledgeBase> searchKnowledge(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> matchedDocIds = new HashSet<>();
        
        // 1. 首先检查关键词索引
        for (String keyword : keywords) {
            String keywordKey = KEYWORD_INDEX_KEY + keyword.toLowerCase();
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

        // 3. 如果没有找到匹配的文档，返回热门知识，并传入搜索词
        return getHotKnowledge(keywords);
    }

    public List<KnowledgeBase> getHotKnowledge(List<String> searchTerms) {
        Set<ZSetOperations.TypedTuple<Object>> hotItems = redisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_KNOWLEDGE_KEY, 0, -1);
        
        if (hotItems == null) {
            return Collections.emptyList();
        }

        return hotItems.stream()
                .map(tuple -> {
                    String id = Objects.requireNonNull(tuple.getValue()).toString();
                    return (KnowledgeBase) redisTemplate.opsForValue().get(KNOWLEDGE_DATA_KEY + id);
                })
                .filter(Objects::nonNull)
                .filter(knowledge -> {
                    if (searchTerms == null || searchTerms.isEmpty()) {
                        return true;
                    }
                    String title = knowledge.getTitle().toLowerCase();
                    String content = knowledge.getContent().toLowerCase();
                    String category = knowledge.getCategory().toLowerCase();
                    
                    // 检查是否包含任意一个搜索词
                    return searchTerms.stream()
                            .anyMatch(term -> title.contains(term.toLowerCase()) || 
                                           content.contains(term.toLowerCase()) || 
                                           category.contains(term.toLowerCase()));
                })
                .collect(Collectors.toList());
    }

    public void removeExpiredHotKnowledge() {
        // 获取所有热门知识及其分数
        Set<ZSetOperations.TypedTuple<Object>> allItems = redisTemplate.opsForZSet()
                .rangeWithScores(HOT_KNOWLEDGE_KEY, 0, -1);
        
        if (allItems == null || allItems.isEmpty()) {
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
                    // 使用事务确保原子性
                    redisTemplate.execute(new SessionCallback<List<Object>>() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public <K, V> List<Object> execute(@NonNull RedisOperations<K, V> operations) throws DataAccessException {
                            operations.multi();
                            try {
                                // 删除知识数据
                                operations.delete((K)(KNOWLEDGE_DATA_KEY + id));
                                // 从热门知识集合中删除
                                operations.opsForZSet().remove((K)HOT_KNOWLEDGE_KEY, id);
                                // 删除相关的关键词索引
                                deleteKeywordIndex(id);
                                return operations.exec();
                            } catch (Exception e) {
                                operations.discard();
                                throw e;
                            }
                        }
                    });
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