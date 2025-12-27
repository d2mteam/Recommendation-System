package com.recommendation.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEmbeddingJobPublisher implements EmbeddingJobPublisher {
    private static final Logger logger = LoggerFactory.getLogger(LoggingEmbeddingJobPublisher.class);

    @Override
    public void enqueueEmbedJob(String url, String contentHash) {
        logger.info("Enqueued embed job for url={} contentHash={}", url, contentHash);
    }
}
