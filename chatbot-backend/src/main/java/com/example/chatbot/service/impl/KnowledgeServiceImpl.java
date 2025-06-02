package com.example.chatbot.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.chatbot.config.RabbitMQConfig;
import com.example.chatbot.dto.PageResponse;
import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.mapper.KnowledgeBaseMapper;
import com.example.chatbot.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final RabbitTemplate rabbitTemplate;
    @Value("${spring.rabbitmq.queue.batch-size:10}")
    private int BATCH_SIZE;

    @Override
    public PageResponse<KnowledgeBase> findAll(int page, int size) {
        Page<KnowledgeBase> pageResult = knowledgeBaseMapper.selectPage(new Page<>(page, size), null);
        return new PageResponse<>(
            pageResult.getRecords(),
            page,
            size,
            pageResult.getTotal()
        );
    }

    @Override
    public PageResponse<KnowledgeBase> search(String keyword, int page, int size) {
        String pattern = "%" + keyword + "%";
        Page<KnowledgeBase> pageResult = knowledgeBaseMapper.searchByKeyword(new Page<>(page, size), pattern);
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
    public KnowledgeBase findById(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    @Override
    @Transactional
    public KnowledgeBase addKnowledge(KnowledgeBase knowledge) {
        log.debug("Adding new knowledge base entry: {}", knowledge.getTitle());
        knowledgeBaseMapper.insert(knowledge);
        return knowledge;
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

    @Override
    @Transactional
    public void deleteKnowledge(Long id) {
        log.debug("Deleting knowledge base entry with id: {}", id);
        if (knowledgeBaseMapper.deleteById(id) == 0) {
            throw new RuntimeException("Knowledge base entry not found");
        }
    }

    @Override
    @Transactional
    public void batchImport(List<KnowledgeBase> knowledgeList) {
        // 将大列表拆分为每10条一组
        for (int i = 0; i < knowledgeList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, knowledgeList.size());
            List<KnowledgeBase> batch = knowledgeList.subList(i, end);
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.KNOWLEDGE_IMPORT_EXCHANGE,
                RabbitMQConfig.KNOWLEDGE_IMPORT_ROUTING_KEY,
                batch
            );
            log.info("已将 {} 条知识数据(批次 {})发送到消息队列", batch.size(), (i/BATCH_SIZE)+1);
        }
    }
}