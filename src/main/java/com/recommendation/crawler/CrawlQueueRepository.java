package com.recommendation.crawler;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlQueueRepository extends JpaRepository<CrawlQueue, Long> {
    Optional<CrawlQueue> findFirstByNextCrawlAtBeforeOrderByNextCrawlAtAsc(Instant now);
}
