package com.example.chatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    // 定义队列名称常量
    public static final String KNOWLEDGE_IMPORT_QUEUE = "knowledge.import.queue";
    // 定义交换机名称常量
    public static final String KNOWLEDGE_IMPORT_EXCHANGE = "knowledge.import.exchange";
    // 定义路由键常量
    public static final String KNOWLEDGE_IMPORT_ROUTING_KEY = "knowledge.import.routing.key";
    public static final String DLX_EXCHANGE = "dlx.exchange";
    public static final String DLX_QUEUE = "dlx.queue";
    public static final String DLX_ROUTING_KEY = "dlx.routing.key";

    @Bean
    public Queue knowledgeImportQueue(@Value("${spring.rabbitmq.queue.max-length}") int maxLength) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 86400000); // 24小时TTL,消息过期时间,超时进入死信队列
        args.put("x-max-length", maxLength); // 队列最大长度，如果超过这个长度，最早的消息将被删除,进入死信队列
        args.put("x-dead-letter-exchange", DLX_EXCHANGE); // 死信交换机
        args.put("x-dead-letter-routing-key", DLX_ROUTING_KEY); // 死信路由键
        return new Queue(KNOWLEDGE_IMPORT_QUEUE, true, false, false, args);
    }

    /**
     * 创建直连交换机
     * 直连交换机根据精确的路由键将消息路由到队列
     */
    @Bean
    public DirectExchange knowledgeImportExchange() {
        return new DirectExchange(KNOWLEDGE_IMPORT_EXCHANGE);
    }

    /**
     * 将队列绑定到交换机
     * 使用指定的路由键将队列和交换机绑定在一起
     */
    @Bean
    public Binding knowledgeImportBinding(Queue knowledgeImportQueue, DirectExchange knowledgeImportExchange) {
        return BindingBuilder
                .bind(knowledgeImportQueue)
                .to(knowledgeImportExchange)
                .with(KNOWLEDGE_IMPORT_ROUTING_KEY);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue dlxQueue() {
        Map<String, Object> args = new HashMap<>();
        return new Queue(DLX_QUEUE, true, false, false, args);
    }
    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlxQueue)
                .to(dlxExchange)
                .with(DLX_ROUTING_KEY);
    }

    /**
     * 配置ObjectMapper
     * 用于JSON序列化和反序列化
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    /**
     * 配置RabbitTemplate
     * 设置消息转换器为JSON格式，这样可以直接发送和接收Java对象
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        Jackson2JsonMessageConverter messageConverter = new Jackson2JsonMessageConverter(objectMapper);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}