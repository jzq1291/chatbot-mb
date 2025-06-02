package com.example.chatbot.service.impl;

import com.example.chatbot.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DlxMessageHandler {
    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
    public void handleFailedMessage(Message message) {
        log.error("收到死信队列消息: {}", message.toString());
        // 这里可以添加失败消息的处理逻辑
    }
}