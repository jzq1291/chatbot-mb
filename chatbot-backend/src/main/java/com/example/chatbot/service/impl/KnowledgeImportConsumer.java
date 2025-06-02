package com.example.chatbot.service.impl;

import com.example.chatbot.config.RabbitMQConfig;
import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.service.KnowledgeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeImportConsumer {
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    /**
     * 监听知识导入队列的消息
     * RabbitListener 注解指定要监听的队列名称
     * 当队列中有新消息时，会自动调用这个方法
     */
    @RabbitListener(queues = RabbitMQConfig.KNOWLEDGE_IMPORT_QUEUE)
    public void processKnowledgeImport(Message message) {
        try {
            // 将消息体转换为字节数组
            byte[] body = message.getBody();
            // 使用ObjectMapper将字节数组反序列化为List<KnowledgeBase>
            List<KnowledgeBase> knowledgeList = objectMapper.readValue(body, new TypeReference<>() {
            });
            
            log.info("开始处理批量导入请求，共 {} 条数据", knowledgeList.size());
            
            // 遍历知识列表，逐条保存到数据库
            for (KnowledgeBase knowledge : knowledgeList) {
                try {
                    knowledgeService.addKnowledge(knowledge);
                    log.debug("成功导入知识: {}", knowledge.getTitle());
                } catch (Exception e) {
                    // 单条数据导入失败，记录错误但继续处理其他数据
                    log.error("导入知识失败: {}, 错误: {}", knowledge.getTitle(), e.getMessage());
                }
            }
            log.info("批量导入处理完成");
        } catch (Exception e) {
            // 整个批处理过程发生错误
            log.error("批量导入处理失败", e);
            // 这里可以添加重试逻辑或死信队列处理
            throw new RuntimeException("Failed to process knowledge import", e);
        }
    }
} 