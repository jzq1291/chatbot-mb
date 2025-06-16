package com.example.chatbot.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "chatbot.model")
public class ModelProperties {
    private Map<String, ModelOption> options;

    @Data
    public static class ModelOption {
        private String model;  // 模型名称和版本
        private Double temperature;  // 温度参数，控制输出的随机性。范围[0,1]，默认0.7
        private Double topP;  // 核采样参数，控制输出词的概率分布。范围[0,1]，默认0.95
        private Integer topK;  // 控制每次生成时考虑的最高概率词的数量。范围[1,∞)，默认5
        private Integer maxTokens;  // 生成文本的最大长度。范围[1,∞)，默认2048
        private Double presencePenalty;  // 存在惩罚，控制模型避免重复内容的程度。范围[-2,2]，默认0.0
        private Double frequencyPenalty;  // 频率惩罚，控制模型避免重复使用相同词的程度。范围[-2,2]，默认0.0
        private List<String> stop;  // 停止序列，当生成文本包含这些序列时停止生成
    }
} 