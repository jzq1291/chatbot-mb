package com.example.chatbot.service;

import com.example.chatbot.entity.KnowledgeBase;
import java.util.List;

public interface VectorSearchService {
    /**
     * 搜索相似文档
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 相似文档列表
     */
    List<KnowledgeBase> searchSimilar(String query, int topK);
    
    /**
     * 将文档转换为向量并存储
     * @param knowledge 知识库文档
     */
    void indexDocument(KnowledgeBase knowledge);
    
    /**
     * 批量索引文档
     * @param knowledgeList 知识库文档列表
     */
    void indexDocuments(List<KnowledgeBase> knowledgeList);
    
    /**
     * 删除文档索引
     * @param id 文档ID
     */
    void deleteDocument(Long id);
    
    /**
     * 更新文档索引
     * @param knowledge 知识库文档
     */
    void updateDocument(KnowledgeBase knowledge);
} 