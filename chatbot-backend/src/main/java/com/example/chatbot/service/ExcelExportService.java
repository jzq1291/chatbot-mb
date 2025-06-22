package com.example.chatbot.service;

import com.example.chatbot.entity.KnowledgeBase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

public interface ExcelExportService {
    
    /**
     * 下载Excel文件
     * @param knowledgeList 知识库数据列表
     * @param useNio 是否使用NIO方式（true=NIO，false=BIO）
     * @return ResponseEntity<byte[]> 包含文件下载信息
     */
    ResponseEntity<byte[]> downloadExcel(List<KnowledgeBase> knowledgeList, boolean useNio);

    /**
     * 下载CSV文件（流式输出）
     */
    ResponseEntity<StreamingResponseBody> downloadCsv(List<KnowledgeBase> knowledgeList);

    /**
     * 流式NIO下载Excel文件
     * @param knowledgeList 知识库数据列表
     * @return ResponseEntity<StreamingResponseBody> 流式响应
     */
    ResponseEntity<StreamingResponseBody> downloadExcelStreamingNio(List<KnowledgeBase> knowledgeList);
} 