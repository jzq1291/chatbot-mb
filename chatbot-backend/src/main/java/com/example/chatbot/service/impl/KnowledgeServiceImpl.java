package com.example.chatbot.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.chatbot.dto.PageResponse;
import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.mapper.KnowledgeBaseMapper;
import com.example.chatbot.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private static final Logger log = LoggerFactory.getLogger(KnowledgeServiceImpl.class);

    @Override
    @Transactional
    public KnowledgeBase addKnowledge(KnowledgeBase knowledge) {
        log.debug("Adding new knowledge base entry: {}", knowledge.getTitle());
        knowledgeBaseMapper.insert(knowledge);
        return knowledge;
    }

    @Override
    @Transactional
    public void deleteKnowledge(Long id) {
        log.debug("Deleting knowledge base entry with id: {}", id);
        if (knowledgeBaseMapper.deleteById(id) == 0) {
            throw new RuntimeException("Knowledge base entry not found");
        }
    }

    @Override
    public PageResponse<KnowledgeBase> searchByKeyword(String keyword, int page, int size) {
        String pattern = "%" + keyword + "%";
        log.debug("Searching knowledge base with pattern: {}", pattern);
        Page<KnowledgeBase> pageResult = knowledgeBaseMapper.searchByKeyword(new Page<>(page, size), pattern);
        log.debug("Found {} results", pageResult.getTotal());
        return new PageResponse<>(
            pageResult.getRecords(),
            page,
            size,
            pageResult.getTotal()
        );
    }

    @Override
    public PageResponse<KnowledgeBase> findByCategory(String category, int page, int size) {
        Page<KnowledgeBase> pageResult = knowledgeBaseMapper.findByCategory(new Page<>(page, size), category);
        return new PageResponse<>(
            pageResult.getRecords(),
            page,
            size,
            pageResult.getTotal()
        );
    }

    @Override
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