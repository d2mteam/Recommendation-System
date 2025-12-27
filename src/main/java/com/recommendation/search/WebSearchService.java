package com.recommendation.search;

import com.recommendation.dto.WebSearchResultDto;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebSearchService {

    private final HttpClient httpClient;
    private final String userAgent;

    public WebSearchService(@Value("${websearch.user-agent:RecommendationSystemBot/1.0}") String userAgent) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.userAgent = userAgent;
    }

    public List<WebSearchResultDto> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 20));
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://duckduckgo.com/html/?q=" + encoded;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", userAgent)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            return parseResults(response.body(), safeLimit);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (IOException | IllegalArgumentException ex) {
            return List.of();
        }
    }

    private List<WebSearchResultDto> parseResults(String html, int limit) {
        Document document = Jsoup.parse(html);
        Elements results = document.select("div.result");
        List<WebSearchResultDto> items = new ArrayList<>();
        for (Element result : results) {
            Element link = result.selectFirst("a.result__a");
            if (link == null) {
                continue;
            }
            String title = link.text();
            String url = link.absUrl("href");
            if (url.isBlank()) {
                url = link.attr("href");
            }
            String snippet = Optional.ofNullable(result.selectFirst("a.result__snippet, div.result__snippet"))
                    .map(Element::text)
                    .orElse("");
            items.add(new WebSearchResultDto(title, url, snippet));
            if (items.size() >= limit) {
                break;
            }
        }
        return items;
    }
}
