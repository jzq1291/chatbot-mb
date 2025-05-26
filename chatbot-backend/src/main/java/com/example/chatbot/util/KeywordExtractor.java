package com.example.chatbot.util;

import com.example.chatbot.config.KeywordExtractorProperties;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.seg.Dijkstra.DijkstraSegment;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeywordExtractor {
    private final KeywordExtractorProperties properties;
    private final Segment segment;

    public KeywordExtractor(KeywordExtractorProperties properties) {
        this.properties = properties;
        this.segment = new DijkstraSegment();
        initializeDictionary();
    }

    private void initializeDictionary() {
        // 添加常见词组到自定义词典
        properties.getCommonPhrases().forEach(phrase -> 
            CustomDictionary.add(phrase, "nz 1024")); // nz表示专有名词，1024是词频
    }

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

        // 1. 分词
        List<Term> terms = segment.seg(text);
        
        // 2. 处理分词结果
        List<String> phrases = processTerms(terms);

        // 3. 提取关键词
        return extractKeywordsFromPhrases(phrases, maxKeywords);
    }

    private List<String> processTerms(List<Term> terms) {
        List<String> phrases = new ArrayList<>();
        StringBuilder currentPhrase = new StringBuilder();
        
        for (int i = 0; i < terms.size(); i++) {
            Term term = terms.get(i);
            String word = term.word;

            if (isStopWord(word)) {
                addCurrentPhrase(phrases, currentPhrase);
                continue;
            }

            if (isValidNature(term.nature)) {
                processValidTerm(terms, i, phrases, currentPhrase);
            }
        }

        addCurrentPhrase(phrases, currentPhrase);
        return phrases;
    }

    private boolean isStopWord(String word) {
        return properties.getStopWords().contains(word);
    }

    private boolean isValidNature(Nature nature) {
        return nature == Nature.n || // 名词
               nature == Nature.v || // 动词
               nature == Nature.a || // 形容词
               nature == Nature.i || // 成语
               nature == Nature.j || // 简称
               nature == Nature.l || // 习用语
               nature == Nature.nz;  // 其他专名
    }

    private void processValidTerm(List<Term> terms, int currentIndex, List<String> phrases, StringBuilder currentPhrase) {
        Term term = terms.get(currentIndex);
        String word = term.word;

        if (word.length() >= properties.getMinWordLength()) {
            addCurrentPhrase(phrases, currentPhrase);
            phrases.add(word);
        } else {
            tryCombineWithNextTerm(terms, currentIndex, currentPhrase);
        }
    }

    private void tryCombineWithNextTerm(List<Term> terms, int currentIndex, StringBuilder currentPhrase) {
        if (currentIndex < terms.size() - 1) {
            Term nextTerm = terms.get(currentIndex + 1);
            String combined = terms.get(currentIndex).word + nextTerm.word;
            if (combined.length() >= properties.getMinWordLength()) {
                currentPhrase.append(combined);
            } else {
                currentPhrase.append(terms.get(currentIndex).word);
            }
        } else {
            currentPhrase.append(terms.get(currentIndex).word);
        }
    }

    private void addCurrentPhrase(List<String> phrases, StringBuilder currentPhrase) {
        if (!currentPhrase.isEmpty()) {
            phrases.add(currentPhrase.toString());
            currentPhrase.setLength(0);
        }
    }

    private List<String> extractKeywordsFromPhrases(List<String> phrases, int maxKeywords) {
        // 使用TextRank算法提取关键词
        List<String> keywords = HanLP.extractKeyword(String.join(" ", phrases), maxKeywords);
        
        // 如果提取的关键词太少，补充TF-IDF结果
        if (keywords.size() < properties.getMinKeywordCount()) {
            List<String> tfidfKeywords = HanLP.extractKeyword(String.join(" ", phrases), maxKeywords);
            keywords.addAll(tfidfKeywords.stream()
                .filter(k -> !keywords.contains(k) && k.length() >= properties.getMinWordLength())
                .limit(maxKeywords - keywords.size())
                .toList());
        }

        return keywords;
    }

    /**
     * 使用默认参数提取关键词
     * @param text 输入文本
     * @return 关键词列表
     */
    public List<String> extractKeywords(String text) {
        return extractKeywords(text, properties.getDefaultKeywordCount());
    }
} 