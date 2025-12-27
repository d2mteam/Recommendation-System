package com.recommendation.embedding;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "embedding.job")
public record EmbeddingJobProperties(
        boolean enabled,
        int batchSize,
        String pageTable,
        String pageIdColumn,
        String pageContentColumn,
        String embeddingTable,
        String embeddingPageIdColumn,
        String embeddingVectorColumn,
        int expectedDimension,
        EmbeddingSource source,
        String offlineScriptPath,
        Duration offlineTimeout
) {
    public enum EmbeddingSource {
        POSTGRESML,
        OFFLINE
    }

    public EmbeddingJobProperties {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("embedding.job.batch-size must be > 0");
        }
        if (expectedDimension < 0) {
            throw new IllegalArgumentException("embedding.job.expected-dimension must be >= 0");
        }
        if (offlineTimeout == null) {
            offlineTimeout = Duration.ofSeconds(30);
        }
    }
}
