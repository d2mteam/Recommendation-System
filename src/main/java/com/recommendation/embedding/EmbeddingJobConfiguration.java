package com.recommendation.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmbeddingJobProperties.class)
public class EmbeddingJobConfiguration {

    @Bean
    @ConditionalOnProperty(name = "embedding.job.source", havingValue = "OFFLINE")
    public EmbeddingClient offlineEmbeddingClient(EmbeddingJobProperties properties, ObjectMapper objectMapper) {
        return new OfflineScriptEmbeddingClient(properties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "embedding.job.source", havingValue = "POSTGRESML", matchIfMissing = true)
    public EmbeddingClient postgresMlEmbeddingClient(EmbeddingModel embeddingModel) {
        return new PostgresMlEmbeddingClient(embeddingModel);
    }
}
