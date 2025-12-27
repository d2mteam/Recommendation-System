package com.recommendation.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SearchMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(SearchMetricsService.class);

    private final Timer searchLatency;

    public SearchMetricsService(MeterRegistry meterRegistry) {
        this.searchLatency = Timer.builder("search.latency")
                .description("Latency per search execution")
                .register(meterRegistry);
    }

    public void recordSearch(Duration latency, String queryPlanSample) {
        if (latency != null) {
            searchLatency.record(latency);
        }

        if (queryPlanSample == null || queryPlanSample.isBlank()) {
            logger.info("Search completed in {} ms", latency != null ? latency.toMillis() : 0);
            return;
        }

        logger.info(
                "Search completed in {} ms with query plan sample: {}",
                latency != null ? latency.toMillis() : 0,
                queryPlanSample
        );
    }
}
