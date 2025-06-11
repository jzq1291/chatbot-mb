package com.example.chatbot.service;

import com.example.chatbot.entity.KnowledgeBase;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

public interface RedisService {
    /**
     * 获取RedisTemplate实例
     */
    RedisTemplate<String, Object> getRedisTemplate();

    /**
     * 将知识库文档保存到Redis
     */
    void saveDocToRedis(KnowledgeBase knowledge);

    /**
     * 增加知识库文档的访问分数
     */
    void incrementKnowledgeScore(String knowledgeId);

    /**
     * 根据关键词搜索知识库文档
     */
    List<KnowledgeBase> searchKnowledge(List<String> keywords);

    /**
     * 获取热门知识库文档
     */
    List<KnowledgeBase> getHotKnowledge(List<String> searchTerms);

    /**
     * 清理过期的热门知识
     */
    void removeExpiredHotKnowledge();

    /**
     * 从Redis中删除知识库文档及其相关数据
     * @param id 知识库文档ID
     */
    void deleteKnowledge(Long id);
} 