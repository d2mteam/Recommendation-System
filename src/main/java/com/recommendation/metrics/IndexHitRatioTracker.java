package com.recommendation.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class IndexHitRatioTracker {

    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();

    public IndexHitRatioTracker(MeterRegistry meterRegistry) {
        Gauge.builder("index.hit.ratio", this, IndexHitRatioTracker::getHitRatio)
                .description("Ratio of index hits to total lookups")
                .register(meterRegistry);
    }

    public void recordHit() {
        hitCount.incrementAndGet();
    }

    public void recordMiss() {
        missCount.incrementAndGet();
    }

    public double getHitRatio() {
        long hits = hitCount.get();
        long misses = missCount.get();
        long total = hits + misses;
        if (total == 0) {
            return 0.0;
        }
        return (double) hits / total;
    }
}
