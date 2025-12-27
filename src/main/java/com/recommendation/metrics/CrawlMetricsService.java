package com.recommendation.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CrawlMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlMetricsService.class);

    private final Timer crawlLatency;
    private final Counter crawlSuccessCount;
    private final Counter crawlFailureCount;

    public CrawlMetricsService(MeterRegistry meterRegistry) {
        this.crawlLatency = Timer.builder("crawl.latency")
                .description("Latency per crawl execution")
                .register(meterRegistry);
        this.crawlSuccessCount = Counter.builder("crawl.urls.success")
                .description("Total number of successfully crawled URLs")
                .register(meterRegistry);
        this.crawlFailureCount = Counter.builder("crawl.urls.failure")
                .description("Total number of failed crawled URLs")
                .register(meterRegistry);
    }

    public void recordCrawlResult(Duration latency, int successCount, int failureCount) {
        if (latency != null) {
            crawlLatency.record(latency);
        }

        if (successCount > 0) {
            crawlSuccessCount.increment(successCount);
        }

        if (failureCount > 0) {
            crawlFailureCount.increment(failureCount);
        }

        logger.info(
                "Crawl completed in {} ms (success URLs: {}, failed URLs: {})",
                latency != null ? latency.toMillis() : 0,
                successCount,
                failureCount
        );
    }
}
