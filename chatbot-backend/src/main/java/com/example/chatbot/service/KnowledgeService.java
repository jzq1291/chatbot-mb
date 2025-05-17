package com.example.chatbot.service;

import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeService {
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    @Transactional
    public KnowledgeBase addKnowledge(KnowledgeBase knowledge) {
        log.debug("Adding new knowledge base entry: {}", knowledge.getTitle());
        knowledgeBaseMapper.insert(knowledge);
        return knowledge;
    }

    @Transactional
    public void deleteKnowledge(Long id) {
        log.debug("Deleting knowledge base entry with id: {}", id);
        if (knowledgeBaseMapper.deleteById(id) == 0) {
            throw new RuntimeException("Knowledge base entry not found");
        }
    }

    public List<KnowledgeBase> searchByKeyword(String keyword) {
        String pattern = "%" + keyword + "%";
        log.debug("Searching knowledge base with pattern: {}", pattern);
        List<KnowledgeBase> results = knowledgeBaseMapper.searchByKeyword(pattern);
        log.debug("Found {} results", results.size());
        if (!results.isEmpty()) {
            log.debug("First result title: {}", results.get(0).getTitle());
        }
        return results;
    }

    public List<KnowledgeBase> findByCategory(String category) {
        return knowledgeBaseMapper.findByCategory(category);
    }

    @Transactional
    public KnowledgeBase updateKnowledge(Long id, KnowledgeBase knowledge) {
        KnowledgeBase existingKnowledge = knowledgeBaseMapper.selectById(id);
        if (existingKnowledge == null) {
            throw new RuntimeException("Knowledge base entry not found");
        }
        knowledge.setId(id);
        knowledgeBaseMapper.updateById(knowledge);
        return knowledge;
    }
} 