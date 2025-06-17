package com.example.chatbot.service;

import com.example.chatbot.entity.KnowledgeBase;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ExcelExportService {
    
    /**
     * 使用BIO方式导出所有知识库数据到Excel
     * @param knowledgeList 知识库数据列表
     * @return Excel文件的字节数组
     */
    byte[] exportToExcelBio(List<KnowledgeBase> knowledgeList);
    
    /**
     * 使用NIO方式导出所有知识库数据到Excel
     * @param knowledgeList 知识库数据列表
     * @return Excel文件的字节数组
     */
    byte[] exportToExcelNio(List<KnowledgeBase> knowledgeList);
    
    /**
     * 使用BIO方式下载Excel文件
     * @param knowledgeList 知识库数据列表
     * @return ResponseEntity包含文件下载信息
     */
    ResponseEntity<byte[]> downloadExcelBio(List<KnowledgeBase> knowledgeList);
    
    /**
     * 使用NIO方式下载Excel文件
     * @param knowledgeList 知识库数据列表
     * @return ResponseEntity包含文件下载信息
     */
    ResponseEntity<byte[]> downloadExcelNio(List<KnowledgeBase> knowledgeList);
} 