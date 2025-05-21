package com.example.chatbot.service;

import com.example.chatbot.entity.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String HOT_KNOWLEDGE_KEY = "hot_knowledge";
    private static final String KNOWLEDGE_DATA_KEY = "knowledge_data:";
    private static final int MAX_HOT_ITEMS = 20;

    public void incrementKnowledgeScore(KnowledgeBase knowledge) {
        // 更新热门知识分数
        redisTemplate.opsForZSet().incrementScore(HOT_KNOWLEDGE_KEY, knowledge.getId().toString(), 1);
        // 存储完整知识数据
        redisTemplate.opsForValue().set(KNOWLEDGE_DATA_KEY + knowledge.getId(), knowledge);
    }

    public List<KnowledgeBase> getHotKnowledge() {
        Set<ZSetOperations.TypedTuple<Object>> hotItems = redisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_KNOWLEDGE_KEY, 0, MAX_HOT_ITEMS - 1);
        
        if (hotItems == null) {
            return List.of();
        }

        return hotItems.stream()
                .map(tuple -> {
                    String id = tuple.getValue().toString();
                    return (KnowledgeBase) redisTemplate.opsForValue().get(KNOWLEDGE_DATA_KEY + id);
                })
                .filter(knowledge -> knowledge != null)
                .collect(Collectors.toList());
    }

    public void removeExpiredHotKnowledge() {
        // 每天凌晨执行，保留最近24小时的数据
        redisTemplate.delete(HOT_KNOWLEDGE_KEY);
        // 删除所有知识数据
        Set<String> keys = redisTemplate.keys(KNOWLEDGE_DATA_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
} 