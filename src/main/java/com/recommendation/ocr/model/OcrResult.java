package com.recommendation.ocr.model;

public record OcrResult(String sourceUrl, String contentType, String text, String error) {
}
