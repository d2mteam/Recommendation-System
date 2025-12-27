package com.recommendation.ocr.controller;

import com.recommendation.ocr.model.OcrCrawlerRequest;
import com.recommendation.ocr.model.OcrCrawlerResponse;
import com.recommendation.ocr.service.OcrCrawlerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ocr")
public class OcrCrawlerController {

    private final OcrCrawlerService crawlerService;

    public OcrCrawlerController(OcrCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/crawl")
    public ResponseEntity<OcrCrawlerResponse> crawl(@RequestBody OcrCrawlerRequest request) {
        return ResponseEntity.ok(crawlerService.crawlAndOcr(request));
    }
}
