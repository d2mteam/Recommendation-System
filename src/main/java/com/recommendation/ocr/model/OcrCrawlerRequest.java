package com.recommendation.ocr.model;

public record OcrCrawlerRequest(String startUrl, Integer maxDepth, Integer maxFiles) {
}
