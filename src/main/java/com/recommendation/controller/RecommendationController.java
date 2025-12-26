package com.recommendation.controller;

import com.recommendation.dto.RecommendationDto;
import com.recommendation.dto.SearchResultDto;
import com.recommendation.service.RecommendationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendationController {
    private static final Logger log = LoggerFactory.getLogger(RecommendationController.class);
    private static final int DEFAULT_LIMIT = 10;

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/search")
    public List<SearchResultDto> search(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        long start = System.nanoTime();
        try {
            return recommendationService.search(query, normalizeLimit(limit));
        } finally {
            logLatency("/search", start, "query", query);
        }
    }

    @GetMapping("/recommend/page/{id}")
    public List<RecommendationDto> recommendForPage(
            @PathVariable("id") long id,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        long start = System.nanoTime();
        try {
            return recommendationService.recommendForPage(id, normalizeLimit(limit));
        } finally {
            logLatency("/recommend/page/" + id, start, "id", id);
        }
    }

    @GetMapping("/recommend/user/{id}")
    public List<RecommendationDto> recommendForUser(
            @PathVariable("id") long id,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        long start = System.nanoTime();
        try {
            return recommendationService.recommendForUser(id, normalizeLimit(limit));
        } finally {
            logLatency("/recommend/user/" + id, start, "id", id);
        }
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return limit;
    }

    private void logLatency(String path, long start, String key, Object value) {
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        log.info("endpoint={} {}={} latencyMs={}", path, key, value, String.format("%.2f", elapsedMs));
    }
}
