package com.recommendation.crawler;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlStateRepository extends JpaRepository<CrawlState, Long> {
    Optional<CrawlState> findByUrl(String url);
}
