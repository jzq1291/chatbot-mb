package com.example.chatbot.service.impl;

import com.example.chatbot.dto.PageResponse;
import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.service.KnowledgeService;
import com.example.chatbot.service.VectorSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public class VectorSearchServiceImplTest {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private VectorSearchService vectorSearchService;

    /**
     * 测试将 PostgreSQL 中的所有知识库数据同步到 Milvus
     * 注意：这是一个集成测试，需要实际的数据库连接
     */
    @Test
    public void testSyncAllKnowledgeToMilvus() {
        // 2. 从 PostgreSQL 中获取所有知识库数据
        // 这里使用分页查询，每次处理 50 条数据
        int pageSize = 50;
        int page = 1;
        int totalProcessed = 0;
        
        while (true) {
            PageResponse<KnowledgeBase> pageResponse = knowledgeService.findAll(page, pageSize);
            List<KnowledgeBase> knowledgeList = pageResponse.getContent();
            
            if (knowledgeList.isEmpty()) {
                break;
            }

            // 3. 将数据索引到 Milvus
            vectorSearchService.indexDocuments(knowledgeList);
            
            totalProcessed += knowledgeList.size();
            System.out.printf("Processed %d records, current page: %d%n", totalProcessed, page);
            
            page++;
        }
        
        System.out.printf("Sync completed. Total records processed: %d%n", totalProcessed);
    }
} 