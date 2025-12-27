package com.recommendation.search;

import com.recommendation.dto.WebSearchResultDto;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web-search")
public class WebSearchController {
    private static final Logger log = LoggerFactory.getLogger(WebSearchController.class);
    private static final int DEFAULT_LIMIT = 10;

    private final WebSearchService webSearchService;

    public WebSearchController(WebSearchService webSearchService) {
        this.webSearchService = webSearchService;
    }

    @GetMapping
    public List<WebSearchResultDto> search(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        long start = System.nanoTime();
        try {
            return webSearchService.search(query, normalizeLimit(limit));
        } finally {
            logLatency("/api/web-search", start, "query", query);
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
