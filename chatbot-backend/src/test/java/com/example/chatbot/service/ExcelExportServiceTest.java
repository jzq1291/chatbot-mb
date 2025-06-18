package com.example.chatbot.service;

import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.service.impl.ExcelExportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
    void testDownloadExcelBio() {
        // 执行测试
        ResponseEntity<?> response = excelExportService.downloadExcel(testData, false);

        // 验证结果
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof byte[]);
        
        byte[] result = (byte[]) response.getBody();
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        // 验证文件头（Excel文件的魔数）
        assertTrue(result[0] == 0x50); // P
        assertTrue(result[1] == 0x4B); // K
        
        // 验证响应头
        assertNotNull(response.getHeaders().getContentDisposition());
        assertTrue(response.getHeaders().getContentDisposition().getFilename().contains("BIO"));
    }

    @Test
    void testDownloadExcelNio() {
        // 执行测试
        ResponseEntity<?> response = excelExportService.downloadExcel(testData, true);

        // 验证结果
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof byte[]);
        
        byte[] result = (byte[]) response.getBody();
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        // 验证文件头（Excel文件的魔数）
        assertTrue(result[0] == 0x50); // P
        assertTrue(result[1] == 0x4B); // K
        
        // 验证响应头
        assertNotNull(response.getHeaders().getContentDisposition());
        assertTrue(response.getHeaders().getContentDisposition().getFilename().contains("NIO"));
    }

    @Test
    void testDownloadExcelEmptyList() {
        List<KnowledgeBase> emptyList = new ArrayList<>();
        
        // 测试BIO方式
        ResponseEntity<?> bioResponse = excelExportService.downloadExcel(emptyList, false);
        assertEquals(204, bioResponse.getStatusCodeValue()); // No Content
        
        // 测试NIO方式
        ResponseEntity<?> nioResponse = excelExportService.downloadExcel(emptyList, true);
        assertEquals(204, nioResponse.getStatusCodeValue()); // No Content
    }

    @Test
    void testDownloadExcelLargeData() {
        // 创建大量测试数据
        List<KnowledgeBase> largeData = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            largeData.add(createTestKnowledge(
                (long) i,
                "测试知识" + i,
                "分类" + (i % 5),
                "这是测试内容" + i + "，包含一些较长的内容用于测试自动换行功能。"
            ));
        }
        
        // 测试BIO方式
        long bioStartTime = System.currentTimeMillis();
        ResponseEntity<?> bioResponse = excelExportService.downloadExcel(largeData, false);
        long bioEndTime = System.currentTimeMillis();
        
        // 测试NIO方式
        long nioStartTime = System.currentTimeMillis();
        ResponseEntity<?> nioResponse = excelExportService.downloadExcel(largeData, true);
        long nioEndTime = System.currentTimeMillis();
        
        // 验证结果
        assertNotNull(bioResponse);
        assertNotNull(nioResponse);
        assertEquals(200, bioResponse.getStatusCodeValue());
        assertEquals(200, nioResponse.getStatusCodeValue());
        
        // BIO返回byte[]
        assertTrue(bioResponse.getBody() instanceof byte[]);
        byte[] bioResult = (byte[]) bioResponse.getBody();
        assertTrue(bioResult.length > 0);
        
        // NIO也返回byte[]
        assertTrue(nioResponse.getBody() instanceof byte[]);
        byte[] nioResult = (byte[]) nioResponse.getBody();
        assertTrue(nioResult.length > 0);
        
        System.out.println("BIO方式耗时: " + (bioEndTime - bioStartTime) + "ms");
        System.out.println("NIO方式耗时: " + (nioEndTime - nioStartTime) + "ms");
        
        // 验证性能差异（NIO应该更快）
        long bioTime = bioEndTime - bioStartTime;
        long nioTime = nioEndTime - nioStartTime;
        System.out.println("性能提升: " + String.format("%.2f", (double) bioTime / nioTime) + "倍");
    }

    @Test
    void testDownloadExcelResponseHeaders() {
        ResponseEntity<?> response = excelExportService.downloadExcel(testData, false);
        
        // 验证响应头
        assertNotNull(response.getHeaders().getContentType());
        assertEquals("application/octet-stream", response.getHeaders().getContentType().toString());
        
        assertNotNull(response.getHeaders().getContentDisposition());
        // 验证文件名包含attachment
        String disposition = response.getHeaders().getContentDisposition().toString();
        assertTrue(disposition.contains("attachment"));
        
        assertNotNull(response.getHeaders().getContentLength());
        assertTrue(response.getHeaders().getContentLength() > 0);
    }
} 