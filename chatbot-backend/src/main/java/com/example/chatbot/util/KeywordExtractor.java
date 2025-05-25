package com.example.chatbot.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.corpus.tag.Nature;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class KeywordExtractor {
    private static final int DEFAULT_KEYWORD_COUNT = 5;
    private static final int MIN_KEYWORD_COUNT = 3;
    private static final int MIN_WORD_LENGTH = 2;

    /**
     * 提取文本中的关键词
     * @param text 输入文本
     * @param maxKeywords 最大关键词数量
     * @return 关键词列表
     */
    public List<String> extractKeywords(String text, int maxKeywords) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        // 1. 分词并词性标注
        List<Term> terms = HanLP.segment(text);
        
        // 2. 过滤词性，只保留名词、动词、形容词等有意义的词性
        List<String> filteredTerms = terms.stream()
            .filter(term -> {
                Nature nature = term.nature;
                // 保留名词、动词、形容词、成语等
                return nature == Nature.n || // 名词
                       nature == Nature.v || // 动词
                       nature == Nature.a || // 形容词
                       nature == Nature.i || // 成语
                       nature == Nature.j || // 简称
                       nature == Nature.l || // 习用语
                       nature == Nature.nz;  // 其他专名
            })
            .filter(term -> term.word.length() >= MIN_WORD_LENGTH) // 过滤掉短词
            .map(term -> term.word)
            .collect(Collectors.toList());

        // 3. 使用TextRank算法提取关键词
        List<String> keywords = HanLP.extractKeyword(String.join(" ", filteredTerms), maxKeywords);
        
        // 4. 如果提取的关键词太少，补充TF-IDF结果
        if (keywords.size() < MIN_KEYWORD_COUNT) {
            List<String> tfidfKeywords = HanLP.extractKeyword(text, maxKeywords);
            keywords.addAll(tfidfKeywords.stream()
                .filter(k -> !keywords.contains(k))
                .limit(maxKeywords - keywords.size())
                .collect(Collectors.toList()));
        }

        return keywords;
    }

    /**
     * 使用默认参数提取关键词
     * @param text 输入文本
     * @return 关键词列表
     */
    public List<String> extractKeywords(String text) {
        return extractKeywords(text, DEFAULT_KEYWORD_COUNT);
    }
} 