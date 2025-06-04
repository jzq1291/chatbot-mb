package com.example.chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

@Configuration
public class AiConfig {
    @Bean
    public ChatClient chatClient(OllamaChatModel model){
        return ChatClient
                .builder(model)
                .defaultSystem("你的名字是强哥，只输出最终答案，不解释过程，流式输出时也省略思考过程。")
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
