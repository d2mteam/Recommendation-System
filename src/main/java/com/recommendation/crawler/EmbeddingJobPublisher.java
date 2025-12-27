package com.recommendation.crawler;

public interface EmbeddingJobPublisher {
    void enqueueEmbedJob(String url, String contentHash);
}
