package com.example.chatbot.util;

import com.example.chatbot.config.KeywordExtractorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeywordExtractorTest {

    @Mock
    private KeywordExtractorProperties properties;

    private KeywordExtractor keywordExtractor;

    @BeforeEach
    void setUp() {
        // 设置基本配置
//        when(properties.getMinWordLength()).thenReturn(3);
//        when(properties.getDefaultKeywordCount()).thenReturn(5);
//        when(properties.getCommonPhrases()).thenReturn(Arrays.asList("人工智能", "机器学习"));
//        when(properties.getStopWords()).thenReturn(Arrays.asList("的", "了", "是", "在", "和"));

        keywordExtractor = new KeywordExtractor(properties);
    }

    @Test
    void extractKeywords_ShouldExtractKeywordsFromText() {
        // 准备测试数据
        String text = "人工智能和机器学习是当前最热门的技术领域，深度学习在其中扮演着重要角色。";
        int maxKeywords = 3;

        // 执行测试
        List<String> keywords = keywordExtractor.extractKeywords(text, maxKeywords);

        // 验证结果
        assertNotNull(keywords);
        assertTrue(keywords.size() <= maxKeywords);
        assertTrue(keywords.contains("人工智能"));
        assertTrue(keywords.contains("机器学习"));
    }

    @Test
    void extractKeywords_ShouldHandleEmptyText() {
        // 准备测试数据
        String text = "";
        int maxKeywords = 5;

        // 执行测试
        List<String> keywords = keywordExtractor.extractKeywords(text, maxKeywords);

        // 验证结果
        assertNotNull(keywords);
        assertTrue(keywords.isEmpty());
    }

    @Test
    void extractKeywordsFromArticle_ShouldExtractKeywordsWithPositionWeight() {
        // 准备测试数据
        String article = "人工智能发展\n\n" +
                "人工智能是当前最热门的技术领域。\n\n" +
                "机器学习在其中扮演着重要角色。\n\n" +
                "深度学习是机器学习的重要分支。";
        int maxKeywords = 4;

        // 执行测试
        List<String> keywords = keywordExtractor.extractKeywordsFromArticle(article, maxKeywords);

        // 验证结果
        assertNotNull(keywords);
        assertTrue(keywords.size() <= maxKeywords);
        // 标题中的关键词应该排在前面
        assertTrue(keywords.contains("人工智能"));
        assertTrue(keywords.contains("机器学习"));
    }

    @Test
    void extractKeywordsFromArticle_ShouldHandleEmptyArticle() {
        // 准备测试数据
        String article = "";
        int maxKeywords = 5;

        // 执行测试
        List<String> keywords = keywordExtractor.extractKeywordsFromArticle(article, maxKeywords);

        // 验证结果
        assertNotNull(keywords);
        assertTrue(keywords.isEmpty());
    }

    @Test
    void extractKeywords_ShouldRespectMaxKeywordsLimit() {
        // 准备测试数据
        String text = "人工智能和机器学习是当前最热门的技术领域，深度学习在其中扮演着重要角色，" +
                "自然语言处理也是重要分支，计算机视觉应用广泛，强化学习发展迅速。";
        int maxKeywords = 3;

        // 执行测试
        List<String> keywords = keywordExtractor.extractKeywords(text, maxKeywords);

        // 验证结果
        assertNotNull(keywords);
        assertEquals(maxKeywords, keywords.size());
    }
} 