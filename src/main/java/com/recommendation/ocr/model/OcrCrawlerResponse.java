package com.recommendation.ocr.model;

import java.util.List;

public record OcrCrawlerResponse(String startUrl, int filesProcessed, List<OcrResult> results) {
}
