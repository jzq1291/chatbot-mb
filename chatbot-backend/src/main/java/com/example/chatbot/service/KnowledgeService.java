package com.example.chatbot.service;

import com.example.chatbot.dto.PageResponse;
import com.example.chatbot.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeService {
    KnowledgeBase addKnowledge(KnowledgeBase knowledge);
    void deleteKnowledge(Long id);
    PageResponse<KnowledgeBase> searchByKeyword(String keyword, int page, int size);
    PageResponse<KnowledgeBase> findByCategory(String category, int page, int size);
    KnowledgeBase updateKnowledge(Long id, KnowledgeBase knowledge);
} 