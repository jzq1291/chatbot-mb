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
import java.util.stream.Collectors;

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
        
        // 2. 处理分词结果，组合有意义的词组
        List<String> phrases = processTerms(terms);

        // 3. 提取关键词并过滤短词
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

        // 如果当前词长度足够，直接添加
        if (word.length() >= properties.getMinWordLength()) {
            addCurrentPhrase(phrases, currentPhrase);
            phrases.add(word);
        } else {
            // 尝试与下一个词组合
            tryCombineWithNextTerm(terms, currentIndex, currentPhrase);
        }
    }

    private void tryCombineWithNextTerm(List<Term> terms, int currentIndex, StringBuilder currentPhrase) {
        if (currentIndex < terms.size() - 1) {
            Term nextTerm = terms.get(currentIndex + 1);
            if (isValidNature(nextTerm.nature)) {
                String combined = terms.get(currentIndex).word + nextTerm.word;
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
        String text = String.join(" ", phrases);
        
        // 使用TextRank算法提取关键词
        List<String> textRankKeywords = HanLP.extractKeyword(text, maxKeywords);
        
        // 如果提取的关键词太少，补充TF-IDF结果
        if (textRankKeywords.size() < properties.getMinKeywordCount()) {
            // 使用TF-IDF算法提取关键词
            List<String> tfidfKeywords = HanLP.extractKeyword(text, maxKeywords, true);
            textRankKeywords.addAll(tfidfKeywords.stream()
                .filter(k -> !textRankKeywords.contains(k))
                .limit(maxKeywords - textRankKeywords.size())
                .toList());
        }

        // 过滤掉长度小于minWordLength的关键词
        return textRankKeywords.stream()
            .filter(k -> k.length() >= properties.getMinWordLength())
            .collect(Collectors.toList());
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