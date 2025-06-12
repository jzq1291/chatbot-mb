package com.example.chatbot.service.impl;

import com.example.chatbot.properties.ModelProperties;
import com.example.chatbot.dto.ChatRequest;
import com.example.chatbot.dto.ChatResponse;
import com.example.chatbot.entity.ChatMessage;
import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.entity.User;
import com.example.chatbot.mapper.ChatMessageMapper;
import com.example.chatbot.mapper.KnowledgeBaseMapper;
import com.example.chatbot.mapper.UserMapper;
import com.example.chatbot.service.ChatService;
import com.example.chatbot.service.KnowledgeService;
import com.example.chatbot.service.RedisDistributedLock;
import com.example.chatbot.service.RedisService;
import com.example.chatbot.util.KeywordExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {
    private final ChatClient chatClient;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final ModelProperties modelProperties;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final RedisService redisService;
    private final KeywordExtractor keywordExtractor;
    private final KnowledgeService knowledgeService;
    private final Scheduler elasticScheduler;
    private final RedisDistributedLock distributedLock;
    private static final int MIN_REQUIRED_RESULTS = 3;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userMapper.findByUsername(username);
    }

    // 提取公共的消息处理逻辑
    private ProcessMessageResult processMessageCommon(ChatRequest request) {
        String sessionId = getOrCreateSessionId(request.getSessionId());
        User currentUser = getCurrentUser();
        String modelId = request.getModelId() != null ? request.getModelId() : "qwen3";
        
        // 清理用户消息
        String cleanedMessage = cleanMessage(request.getMessage());

        // 提取关键词并搜索相关文档
        List<String> keywords = keywordExtractor.extractKeywords(cleanedMessage, 3);
        List<KnowledgeBase> relevantDocs = searchRelevantDocuments(cleanedMessage, keywords);

        // 构建上下文
        StringBuilder contextBuilder = buildContextFromDocs(relevantDocs);
        
        // 构建消息上下文
        List<Message> messages = buildMessageContext(sessionId);

        //保存用户消息
        saveUserMessage(cleanedMessage, sessionId, currentUser);
        
        // 如果有相关文档，添加到用户消息中
        if (!contextBuilder.isEmpty()) {
            String enhancedMessage = cleanedMessage + "\n\n" + contextBuilder;
            messages.add(new UserMessage(enhancedMessage));
        } else {
            messages.add(new UserMessage(cleanedMessage));
        }

        // 获取模型配置
        ModelProperties.ModelOption modelOptions = modelProperties.getOptions().get(modelId);
        if (modelOptions == null) {
            throw new IllegalArgumentException("Invalid model ID: " + modelId);
        }

        ChatOptions options = ChatOptions.builder()
                .model(modelOptions.getModel())
                .temperature(modelOptions.getTemperature())
                .topP(modelOptions.getTopP())
                .topK(modelOptions.getTopK())
                .build();

        return new ProcessMessageResult(messages, options, sessionId, modelId, currentUser);
    }

    // 搜索结果处理
    private List<KnowledgeBase> searchRelevantDocuments(String cleanedMessage, List<String> keywords) {
        List<KnowledgeBase> combinedResults = new ArrayList<>();
        
        // 1. 首先从Redis搜索相关文档
        List<KnowledgeBase> redisResults = redisService.searchKnowledge(keywords);
        if (!redisResults.isEmpty()) {
            combinedResults.addAll(redisResults);
        }
        
        // 2. 如果Redis结果数量小于要求的最小数量，进行向量搜索
        if (combinedResults.size() < MIN_REQUIRED_RESULTS) {
            try {
                // 2.1 使用完整的cleanedMessage进行向量搜索
                List<KnowledgeBase> vectorResults = knowledgeService.searchSimilar(cleanedMessage, MIN_REQUIRED_RESULTS);
                if (vectorResults != null && !vectorResults.isEmpty()) {
                    // 合并结果，去重
                    for (KnowledgeBase kb : vectorResults) {
                        if (!combinedResults.contains(kb)) {
                            combinedResults.add(kb);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Vector search failed, falling back to keyword search only", e);
            }
        }
        
        // 3. 如果结果数量仍然小于要求的最小数量，进行关键词搜索
        if (combinedResults.size() < MIN_REQUIRED_RESULTS) {
            List<KnowledgeBase> keywordResults = searchKnowledgeFromDB(keywords);
            // 合并结果，去重
            for (KnowledgeBase kb : keywordResults) {
                if (!combinedResults.contains(kb)) {
                    combinedResults.add(kb);
                }
            }
        }
        
        // 4. 更新Redis缓存，使用分布式锁保护
        for (KnowledgeBase doc : combinedResults) {
            String lockKey = "knowledge:save:" + doc.getId();
            String lockValue = distributedLock.tryLock(lockKey, 5, TimeUnit.SECONDS);
            try {
                if (lockValue != null) {
                    // 统一使用saveDocToRedis，它会处理新文档保存和已有文档的更新
                    redisService.saveDocToRedis(doc);
                }
            } finally {
                if (lockValue != null) {
                    distributedLock.unlock(lockKey, lockValue);
                }
            }
        }
        
        return combinedResults;
    }

    // 构建文档上下文
    private StringBuilder buildContextFromDocs(List<KnowledgeBase> relevantDocs) {
        StringBuilder contextBuilder = new StringBuilder();
        if (!relevantDocs.isEmpty()) {
            contextBuilder.append("结合以下知识库内容回答：\n");
            for (KnowledgeBase doc : relevantDocs) {
                contextBuilder.append("标题：").append(doc.getTitle()).append("\n");
                contextBuilder.append("内容：").append(doc.getContent()).append("\n\n");
            }
        }
        return contextBuilder;
    }

    @Override
    @Transactional
    public ChatResponse processMessage(ChatRequest request) {
        ProcessMessageResult result = processMessageCommon(request);

        // 调用AI模型
        String aiResponse = chatClient.prompt()
                .messages(result.messages())
                .options(result.options())
                .call()
                .content();

        // 清理AI响应
        assert aiResponse != null;
        String cleanedResponse = cleanAiResponse(aiResponse);

        // 保存AI响应
        saveAssistantMessage(cleanedResponse, result.sessionId(), result.currentUser());

        return ChatResponse.builder()
                .message(cleanedResponse)
                .sessionId(result.sessionId())
                .modelId(result.modelId())
                .build();
    }

    @Override
    @Transactional
    public Flux<ChatResponse> processMessageReactive(ChatRequest request) {
        return Mono.fromCallable(() -> processMessageCommon(request))
            .subscribeOn(elasticScheduler)  // Move blocking operation to elastic thread pool
            .flatMapMany(result -> {
                StringBuilder fullResponse = new StringBuilder();
                return chatClient.prompt()
                        .messages(result.messages())
                        .options(result.options())
                        .stream()
                        .content()
                        .mapNotNull(chunk -> {
                            if (!chunk.isEmpty()) {
                                fullResponse.append(chunk);
                                return buildChatResponse(chunk, result.sessionId(), result.modelId());
                            }
                            return null;
                        })
                        .doOnComplete(() -> {
                            // Move blocking save operation to elastic thread pool
                            Mono.fromRunnable(() -> 
                                saveAssistantMessage(cleanAiResponse(fullResponse.toString()), result.sessionId(), result.currentUser())
                            ).subscribeOn(elasticScheduler).subscribe();
                        })
                        .doOnError(error -> {
                            log.error("Error in streaming response: {}", error.getMessage());
                        });
            });
    }

    // 记录处理结果的数据类
    private record ProcessMessageResult(
        List<Message> messages,
        ChatOptions options,
        String sessionId,
        String modelId,
        User currentUser
    ) {}

    private ChatResponse buildChatResponse(String message, String sessionId, String modelId) {
        return ChatResponse.builder()
                .message(message)
                .sessionId(sessionId)
                .modelId(modelId)
                .sequence(UUID.randomUUID().toString())
                .build();
    }

    private List<KnowledgeBase> searchKnowledgeFromDB(List<String> keywords) {
        if(keywords == null || keywords.isEmpty()){
            return Collections.emptyList();
        }else{
            return knowledgeBaseMapper.retrieveByKeywords(keywords);
        }
    }

    private String getOrCreateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }

    private void saveUserMessage(String content, String sessionId, User user) {
        ChatMessage userMessage = new ChatMessage();
        userMessage.setContent(content);
        userMessage.setRole("user");
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(user.getId());
        chatMessageMapper.insert(userMessage);
    }

    private void saveAssistantMessage(String content, String sessionId, User user) {
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setContent(content);
        assistantMessage.setRole("assistant");
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setUserId(user.getId());
        chatMessageMapper.insert(assistantMessage);
    }

    private List<Message> buildMessageContext(String sessionId) {
        return Mono.fromCallable(() -> {
            // 获取最近的10条消息
            List<ChatMessage> history = chatMessageMapper.findLast10BySessionIdAndUserIdOrderByCreatedAtDesc(sessionId, getCurrentUser().getId());
            // 反转列表以保持时间顺序
            Collections.reverse(history);

            // 构建对话上下文
            List<Message> messages = new ArrayList<>();
            
            String systemPrompt = """
                你是一个智能助手，名字叫强哥。请严格遵守以下规则：
                1.**输出要求**：所有回答（包括流式输出）必须直接给出最终答案，完全省略思考过程、推理步骤或解释性文字。
                2.**知识库优先级**：当用户提供本地知识库内容（通过UserMessage传递）时，必须优先分析知识库内容并结合自身知识库给出回答。
                3.**角色一致性**：回答时需以对话的形式回答，保持友好的语气，避免出现分析知识库等描述。
            """;
            messages.add(new SystemMessage(systemPrompt));
            // 添加历史消息
            for (ChatMessage msg : history) {
                if ("user".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else {
                    messages.add(new AssistantMessage(msg.getContent()));
                }
            }

            return messages;
        }).subscribeOn(elasticScheduler).block();  // Execute blocking DB operation on elastic thread pool
    }

    private String cleanAiResponse(String response) {
        if (response == null) {
            return "";
        }
        
        // 1. 移除think块及其之前的所有内容
        String cleaned = response.replaceAll("(?s).*?</think>\\s*", "");
        
        // 2. 移除其他HTML标签
        cleaned = cleaned.replaceAll("<[^>]+>", "");
        
        // 3. 清理空白字符
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }

    @Override
    public List<ChatResponse> getHistory(String sessionId) {
        User currentUser = getCurrentUser();
        List<ChatMessage> messages = chatMessageMapper.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, currentUser.getId());
        return messages.stream()
                .map(msg -> ChatResponse.builder()
                        .message(msg.getContent())
                        .sessionId(msg.getSessionId())
                        .role(msg.getRole())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllSessions() {
        User currentUser = getCurrentUser();
        return chatMessageMapper.findDistinctSessionIdByUserId(currentUser.getId());
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        User currentUser = getCurrentUser();
        String lockKey = "chat:delete:" + sessionId;
        String lockValue = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
        try {
            if (lockValue != null) {
                chatMessageMapper.deleteBySessionIdAndUserId(sessionId, currentUser.getId());
            }
        } finally {
            if (lockValue != null) {
                distributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    private String cleanMessage(String message) {
        if (message == null) {
            return "";
        }
        // 去除前后的空白字符和特殊转义符
        return message.trim()
                .replaceAll("^[\\n\\t\\r]+|[\\n\\t\\r]+$", "") // 去除前后的换行、制表符
                .replaceAll("\\s+", " "); // 将中间的多个空白字符替换为单个空格
    }
} 