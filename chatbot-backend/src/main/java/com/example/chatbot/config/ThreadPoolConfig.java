package com.example.chatbot.config;

import com.example.chatbot.properties.ThreadPoolProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class ThreadPoolConfig {
    private final ThreadPoolProperties threadPoolProperties;
    
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        ThreadPoolProperties.PoolConfig config = threadPoolProperties.getAsync();
        executor.setCorePoolSize(config.getCoreSize());
        executor.setMaxPoolSize(config.getMaxSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setThreadNamePrefix(config.getThreadNamePrefix());
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
    
    @Bean
    public Scheduler elasticScheduler() {
        return Schedulers.fromExecutor(taskExecutor());
    }
} 