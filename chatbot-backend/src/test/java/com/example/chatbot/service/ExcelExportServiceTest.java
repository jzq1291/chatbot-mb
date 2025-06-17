package com.example.chatbot.service;

import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.service.impl.ExcelExportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {

    @InjectMocks
    private ExcelExportServiceImpl excelExportService;

    private List<KnowledgeBase> testData;

    @BeforeEach
    void setUp() {
        testData = Arrays.asList(
            createTestKnowledge(1L, "测试知识1", "技术", "这是测试内容1"),
            createTestKnowledge(2L, "测试知识2", "管理", "这是测试内容2")
        );
    }

    private KnowledgeBase createTestKnowledge(Long id, String title, String category, String content) {
        KnowledgeBase knowledge = new KnowledgeBase();
        knowledge.setId(id);
        knowledge.setTitle(title);
        knowledge.setCategory(category);
        knowledge.setContent(content);
        knowledge.setCreatedAt(LocalDateTime.now());
        knowledge.setUpdatedAt(LocalDateTime.now());
        return knowledge;
    }

    @Test
    void testExportToExcelBio() {
        // 执行测试
        byte[] result = excelExportService.exportToExcelBio(testData);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        // 验证文件头（Excel文件的魔数）
        assertTrue(result[0] == 0x50); // P
        assertTrue(result[1] == 0x4B); // K
    }

    @Test
    void testExportToExcelNio() {
        // 执行测试
        byte[] result = excelExportService.exportToExcelNio(testData);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        // 验证文件头（Excel文件的魔数）
        assertTrue(result[0] == 0x50); // P
        assertTrue(result[1] == 0x4B); // K
    }

    @Test
    void testExportEmptyList() {
        List<KnowledgeBase> emptyList = new ArrayList<>();
        
        // 测试BIO方式
        byte[] bioResult = excelExportService.exportToExcelBio(emptyList);
        assertNotNull(bioResult);
        assertTrue(bioResult.length > 0);
        
        // 测试NIO方式
        byte[] nioResult = excelExportService.exportToExcelNio(emptyList);
        assertNotNull(nioResult);
        assertTrue(nioResult.length > 0);
    }

    @Test
    void testExportLargeData() {
        // 创建大量测试数据
        List<KnowledgeBase> largeData = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeData.add(createTestKnowledge(
                (long) i,
                "测试知识" + i,
                "分类" + (i % 5),
                "这是测试内容" + i + "，包含一些较长的内容用于测试自动换行功能。"
            ));
        }
        
        // 测试BIO方式
        long bioStartTime = System.currentTimeMillis();
        byte[] bioResult = excelExportService.exportToExcelBio(largeData);
        long bioEndTime = System.currentTimeMillis();
        
        // 测试NIO方式
        long nioStartTime = System.currentTimeMillis();
        byte[] nioResult = excelExportService.exportToExcelNio(largeData);
        long nioEndTime = System.currentTimeMillis();
        
        // 验证结果
        assertNotNull(bioResult);
        assertNotNull(nioResult);
        assertTrue(bioResult.length > 0);
        assertTrue(nioResult.length > 0);
        
        System.out.println("BIO方式耗时: " + (bioEndTime - bioStartTime) + "ms");
        System.out.println("NIO方式耗时: " + (nioEndTime - nioStartTime) + "ms");
    }
} 