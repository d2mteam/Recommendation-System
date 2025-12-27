package com.recommendation.ocr.service;

import com.recommendation.ocr.model.OcrCrawlerRequest;
import com.recommendation.ocr.model.OcrCrawlerResponse;
import com.recommendation.ocr.model.OcrResult;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.RenderingMode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OcrCrawlerService {

    private final HttpClient httpClient;
    private final String tesseractDataPath;
    private final String tesseractLanguage;
    private final int pdfMaxPages;

    public OcrCrawlerService(
            @Value("${ocr.tesseract.data-path:}") String tesseractDataPath,
            @Value("${ocr.tesseract.language:eng}") String tesseractLanguage,
            @Value("${ocr.pdf.max-pages:5}") int pdfMaxPages) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.tesseractDataPath = tesseractDataPath;
        this.tesseractLanguage = tesseractLanguage;
        this.pdfMaxPages = pdfMaxPages;
    }

    public OcrCrawlerResponse crawlAndOcr(OcrCrawlerRequest request) {
        String startUrl = Optional.ofNullable(request.startUrl())
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("startUrl is required"));
        int maxDepth = Optional.ofNullable(request.maxDepth()).orElse(1);
        int maxFiles = Optional.ofNullable(request.maxFiles()).orElse(10);

        Queue<UrlDepth> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        List<OcrResult> results = new ArrayList<>();

        queue.add(new UrlDepth(startUrl, 0));

        while (!queue.isEmpty() && results.size() < maxFiles) {
            UrlDepth current = queue.poll();
            if (current == null || !visited.add(current.url())) {
                continue;
            }

            HttpResponse<byte[]> response = fetch(current.url());
            if (response == null) {
                results.add(new OcrResult(current.url(), null, null, "Failed to fetch"));
                continue;
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .map(value -> value.split(";")[0].trim())
                    .orElse("");

            byte[] body = response.body();
            if (isHtml(contentType)) {
                if (current.depth() < maxDepth) {
                    enqueueLinks(current.url(), body, current.depth() + 1, queue, visited);
                }
                continue;
            }

            if (isPdf(contentType, current.url())) {
                results.add(processPdf(current.url(), contentType, body));
                continue;
            }

            if (isImage(contentType, current.url())) {
                results.add(processImage(current.url(), contentType, body));
            }
        }

        return new OcrCrawlerResponse(startUrl, results.size(), results);
    }

    private HttpResponse<byte[]> fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException | IllegalArgumentException ex) {
            return null;
        }
    }

    private void enqueueLinks(String baseUrl, byte[] body, int depth, Queue<UrlDepth> queue, Set<String> visited) {
        String html = new String(body);
        Document document = Jsoup.parse(html, baseUrl);
        Elements links = document.select("a[href]");
        for (Element link : links) {
            String href = link.absUrl("href");
            if (href.isBlank() || visited.contains(href)) {
                continue;
            }
            queue.add(new UrlDepth(href, depth));
        }
    }

    private OcrResult processPdf(String url, String contentType, byte[] body) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(body))) {
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = Math.min(pdfMaxPages, document.getNumberOfPages());
            StringBuilder textBuilder = new StringBuilder();
            for (int page = 0; page < totalPages; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 300, RenderingMode.QUALITY);
                textBuilder.append(runOcr(image)).append(System.lineSeparator());
            }
            return new OcrResult(url, contentType, textBuilder.toString().trim(), null);
        } catch (IOException | TesseractException ex) {
            return new OcrResult(url, contentType, null, ex.getMessage());
        }
    }

    private OcrResult processImage(String url, String contentType, byte[] body) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(body));
            if (image == null) {
                return new OcrResult(url, contentType, null, "Unsupported image format");
            }
            String text = runOcr(image);
            return new OcrResult(url, contentType, text, null);
        } catch (IOException | TesseractException ex) {
            return new OcrResult(url, contentType, null, ex.getMessage());
        }
    }

    private String runOcr(BufferedImage image) throws TesseractException {
        Tesseract tesseract = new Tesseract();
        if (tesseractDataPath != null && !tesseractDataPath.isBlank()) {
            tesseract.setDatapath(tesseractDataPath);
        }
        tesseract.setLanguage(tesseractLanguage);
        return tesseract.doOCR(image);
    }

    private boolean isHtml(String contentType) {
        return contentType.contains("text/html");
    }

    private boolean isPdf(String contentType, String url) {
        return contentType.contains("application/pdf") || url.toLowerCase().endsWith(".pdf");
    }

    private boolean isImage(String contentType, String url) {
        String lower = url.toLowerCase();
        if (contentType.startsWith("image/")) {
            return true;
        }
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".tiff")
                || lower.endsWith(".bmp");
    }

    private record UrlDepth(String url, int depth) {
        private UrlDepth {
            Objects.requireNonNull(url, "url");
        }
    }
}
