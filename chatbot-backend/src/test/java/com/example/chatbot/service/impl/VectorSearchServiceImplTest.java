package com.example.chatbot.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class VectorSearchServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VectorSearchServiceImpl vectorSearchService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGenerateEmbedding() throws Exception {
        // 模拟 HTTP 响应
        String mockResponse = "{\"embedding\": [0.1, 0.2, 0.3]}";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(mockResponse);
        when(restTemplate.postForEntity(eq("http://localhost:8001/embed"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // 模拟 ObjectMapper 解析结果
        Map<String, Object> mockResult = Map.of("embedding", List.of(0.1, 0.2, 0.3));
        when(objectMapper.readValue(mockResponse, Map.class)).thenReturn(mockResult);

        // 调用方法
        List<Float> embedding = vectorSearchService.generateEmbedding("test text");

        // 验证结果
        assertEquals(3, embedding.size());
        assertEquals(0.1f, embedding.get(0));
        assertEquals(0.2f, embedding.get(1));
        assertEquals(0.3f, embedding.get(2));
    }
} 