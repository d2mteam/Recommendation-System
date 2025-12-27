package com.recommendation.crawler;

import java.time.Duration;

public enum CrawlPriority {
    PRIMARY(Duration.ofHours(1), Duration.ofHours(6)),
    SECONDARY(Duration.ofDays(1), Duration.ofDays(7));

    private final Duration minInterval;
    private final Duration maxInterval;

    CrawlPriority(Duration minInterval, Duration maxInterval) {
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
    }

    public Duration getMinInterval() {
        return minInterval;
    }

    public Duration getMaxInterval() {
        return maxInterval;
    }
}
