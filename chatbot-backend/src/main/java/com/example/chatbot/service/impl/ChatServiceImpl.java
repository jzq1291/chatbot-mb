package com.example.chatbot.service.impl;

import com.example.chatbot.config.ModelProperties;
import com.example.chatbot.dto.ChatRequest;
import com.example.chatbot.dto.ChatResponse;
import com.example.chatbot.entity.ChatMessage;
import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.entity.User;
import com.example.chatbot.mapper.ChatMessageMapper;
import com.example.chatbot.mapper.KnowledgeBaseMapper;
import com.example.chatbot.mapper.UserMapper;
import com.example.chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatClient chatClient;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final ModelProperties modelProperties;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userMapper.findByUsername(username);
    }

    @Override
    @Transactional
    public ChatResponse processMessage(ChatRequest request) {
        User currentUser = getCurrentUser();
        String sessionId = getOrCreateSessionId(request.getSessionId());
        String modelId = request.getModelId() != null ? request.getModelId() : "qwen3";
        
        // 清理用户消息
        String cleanedMessage = cleanMessage(request.getMessage());
        saveUserMessage(cleanedMessage, sessionId, currentUser);
        
        // 搜索相关知识库内容
        List<KnowledgeBase> relevantDocs = knowledgeBaseMapper.retrieveByKeyword(cleanedMessage);
        StringBuilder contextBuilder = new StringBuilder();
        if (!relevantDocs.isEmpty()) {
            contextBuilder.append("相关文档：\n");
            for (KnowledgeBase doc : relevantDocs) {
                contextBuilder.append("标题：").append(doc.getTitle()).append("\n");
                contextBuilder.append("内容：").append(doc.getContent()).append("\n\n");
            }
        }

        // 构建消息上下文
        List<Message> messages = buildMessageContext(sessionId);
        
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

        // 调用AI模型
        String aiResponse = chatClient.prompt()
                .messages(messages)
                .options(options)
                .call()
                .content();

        // 清理AI响应
        assert aiResponse != null;
        String cleanedResponse = cleanAiResponse(aiResponse);

        // 保存AI响应
        saveAssistantMessage(cleanedResponse, sessionId, currentUser);

        return ChatResponse.builder()
                .message(cleanedResponse)
                .sessionId(sessionId)
                .modelId(modelId)
                .build();
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
        // 获取最近的10条消息
        List<ChatMessage> history = chatMessageMapper.findLast10BySessionIdAndUserIdOrderByCreatedAtDesc(sessionId, getCurrentUser().getId());
        // 反转列表以保持时间顺序
        Collections.reverse(history);

        // 构建对话上下文
        List<Message> messages = new ArrayList<>();
        
        // 添加系统提示，包含知识库信息
        String systemPrompt = "你是一个专业的客服助手，请根据以下知识库内容回答用户问题。如果知识库中没有相关信息，请明确告知用户。\n\n";
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
    }

    private String cleanAiResponse(String response) {
        if (response.contains("<think>")) {
            int startIndex = response.indexOf("<think>");
            int endIndex = response.indexOf("</think>");
            if (startIndex != -1 && endIndex != -1) {
                return response.substring(endIndex + 8).trim();
            }
        }
        return response;
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
        chatMessageMapper.deleteBySessionIdAndUserId(sessionId, currentUser.getId());
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

    @Override
    public void processMessageStream(String sessionId, String message, String modelId, SseEmitter emitter) {
        try {
            // 获取或创建会话ID
            String currentSessionId = getOrCreateSessionId(sessionId);
            String currentModelId = modelId != null ? modelId : "qwen3";

            // 保存用户消息
            saveUserMessage(message, currentSessionId, getCurrentUser());

            // 构建消息上下文
            List<Message> messages = buildMessageContext(currentSessionId);

            // 获取模型配置
            ModelProperties.ModelOption modelOptions = modelProperties.getOptions().get(currentModelId);
            if (modelOptions == null) {
                throw new IllegalArgumentException("Invalid model ID: " + currentModelId);
            }

            ChatOptions options = ChatOptions.builder()
                    .model(modelOptions.getModel())
                    .temperature(modelOptions.getTemperature())
                    .topP(modelOptions.getTopP())
                    .topK(modelOptions.getTopK())
                    .build();

            // 发送流式响应
            String response = chatClient.prompt()
                    .messages(messages)
                    .options(options)
                    .call()
                    .content();

            // 将响应分块发送
            // 每次发送一个字符，模拟流式效果
            assert response != null;
            for (char c : response.toCharArray()) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(String.valueOf(c)));
                    Thread.sleep(10); // 添加小延迟使流式效果更明显
                } catch (IOException | InterruptedException e) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(e.getMessage()));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(e.getMessage()));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } finally {
            emitter.complete();
        }
    }
} 