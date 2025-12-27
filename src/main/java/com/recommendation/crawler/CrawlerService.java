package com.recommendation.crawler;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class CrawlerService {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);

    private final CrawlQueueRepository crawlQueueRepository;
    private final CrawlStateRepository crawlStateRepository;
    private final EmbeddingJobPublisher embeddingJobPublisher;
    private final RestClient restClient;
    private final Clock clock;

    public CrawlerService(
            CrawlQueueRepository crawlQueueRepository,
            CrawlStateRepository crawlStateRepository,
            EmbeddingJobPublisher embeddingJobPublisher,
            RestClient restClient,
            Clock clock
    ) {
        this.crawlQueueRepository = crawlQueueRepository;
        this.crawlStateRepository = crawlStateRepository;
        this.embeddingJobPublisher = embeddingJobPublisher;
        this.restClient = restClient;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${crawler.schedule.delay:60000}")
    @Transactional
    public void crawlNextDue() {
        Optional<CrawlQueue> next = crawlQueueRepository
                .findFirstByNextCrawlAtBeforeOrderByNextCrawlAtAsc(Instant.now(clock));
        if (next.isEmpty()) {
            return;
        }
        CrawlQueue queue = next.get();
        processQueue(queue);
    }

    private void processQueue(CrawlQueue queue) {
        CrawlState state = crawlStateRepository.findByUrl(queue.getUrl())
                .orElseGet(() -> new CrawlState(queue.getUrl()));

        try {
            HttpHeaders conditionalHeaders = buildConditionalHeaders(state);
            ResponseEntity<Void> headResponse = null;
            if (!conditionalHeaders.isEmpty()) {
                headResponse = restClient.method(HttpMethod.HEAD)
                        .uri(queue.getUrl())
                        .headers(headers -> headers.addAll(conditionalHeaders))
                        .retrieve()
                        .toBodilessEntity();
            }

            if (headResponse != null && headResponse.getStatusCode().value() == 304) {
                updateNotModified(state);
                crawlStateRepository.save(state);
                scheduleNext(queue);
                return;
            }

            ResponseEntity<String> getResponse = restClient.get()
                    .uri(queue.getUrl())
                    .headers(headers -> headers.addAll(conditionalHeaders))
                    .retrieve()
                    .toEntity(String.class);

            if (getResponse.getStatusCode().value() == 304) {
                updateNotModified(state);
                crawlStateRepository.save(state);
                scheduleNext(queue);
                return;
            }

            if (!getResponse.getStatusCode().is2xxSuccessful()) {
                updateError(state, "Unexpected status: " + getResponse.getStatusCode());
                crawlStateRepository.save(state);
                scheduleNext(queue);
                return;
            }

            String body = getResponse.getBody();
            if (body == null) {
                updateError(state, "Empty response body");
                crawlStateRepository.save(state);
                scheduleNext(queue);
                return;
            }

            updateStateFromResponse(state, getResponse.getHeaders(), body);
            crawlStateRepository.save(state);
            embeddingJobPublisher.enqueueEmbedJob(queue.getUrl(), state.getContentHash());
            scheduleNext(queue);
        } catch (Exception ex) {
            logger.warn("Failed to crawl {}", queue.getUrl(), ex);
            updateError(state, ex.getMessage());
            crawlStateRepository.save(state);
            scheduleNext(queue);
        }
    }

    private HttpHeaders buildConditionalHeaders(CrawlState state) {
        HttpHeaders headers = new HttpHeaders();
        if (state.getEtag() != null && !state.getEtag().isBlank()) {
            headers.setIfNoneMatch(state.getEtag());
        }
        if (state.getLastModified() != null) {
            headers.setIfModifiedSince(state.getLastModified().toEpochMilli());
        }
        return headers;
    }

    private void updateNotModified(CrawlState state) {
        state.setLastCrawledAt(Instant.now(clock));
        state.setStatus(CrawlStatus.NOT_MODIFIED);
    }

    private void updateError(CrawlState state, String message) {
        state.setLastCrawledAt(Instant.now(clock));
        state.setStatus(CrawlStatus.ERROR);
        logger.warn("Crawl error for {}: {}", state.getUrl(), message);
    }

    private void updateStateFromResponse(CrawlState state, HttpHeaders headers, String body) {
        state.setLastCrawledAt(Instant.now(clock));
        state.setStatus(CrawlStatus.SUCCESS);
        if (headers.getETag() != null) {
            state.setEtag(headers.getETag());
        }
        if (headers.getLastModified() > 0) {
            state.setLastModified(Instant.ofEpochMilli(headers.getLastModified()));
        }
        state.setContentHash(hashContent(body));
    }

    private String hashContent(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Missing SHA-256 support", ex);
        }
    }

    private void scheduleNext(CrawlQueue queue) {
        Duration interval = pickInterval(queue.getPriority());
        queue.setNextCrawlAt(Instant.now(clock).plus(interval));
        crawlQueueRepository.save(queue);
    }

    private Duration pickInterval(CrawlPriority priority) {
        long minSeconds = priority.getMinInterval().toSeconds();
        long maxSeconds = priority.getMaxInterval().toSeconds();
        if (maxSeconds <= minSeconds) {
            return priority.getMinInterval();
        }
        long selected = ThreadLocalRandom.current().nextLong(minSeconds, maxSeconds + 1);
        return Duration.ofSeconds(selected);
    }
}
