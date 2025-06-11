package com.example.milvus;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MilvusProperties.class)
@ConditionalOnClass(MilvusServiceClient.class)
@ConditionalOnProperty(prefix = "milvus", name = {"host", "port"})
public class MilvusAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MilvusServiceClient milvusClient(MilvusProperties properties) {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(properties.getHost())
                .withPort(properties.getPort())
                .build();
        return new MilvusServiceClient(connectParam);
    }
} 