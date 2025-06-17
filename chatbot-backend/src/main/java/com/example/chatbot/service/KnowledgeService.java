package com.example.chatbot.service;

import com.example.chatbot.dto.PageResponse;
import com.example.chatbot.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeService {
    PageResponse<KnowledgeBase> findAll(int page, int size);
    PageResponse<KnowledgeBase> search(String keyword, int page, int size);
    List<KnowledgeBase> searchSimilar(String query, int topK);
    PageResponse<KnowledgeBase> findByCategory(String category, int page, int size);
    KnowledgeBase findById(Long id);
    KnowledgeBase addKnowledge(KnowledgeBase knowledge);
    KnowledgeBase updateKnowledge(Long id, KnowledgeBase knowledge);
    void deleteKnowledge(Long id);
    void batchImport(List<KnowledgeBase> knowledgeList);
    
    /**
     * 获取所有知识库数据（不分页）
     * @return 所有知识库数据列表
     */
    List<KnowledgeBase> findAllData();
} 