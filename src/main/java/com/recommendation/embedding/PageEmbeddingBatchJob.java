package com.recommendation.embedding;

import java.time.Duration;
import java.util.List;
import java.util.StringJoiner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PageEmbeddingBatchJob implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingJobProperties properties;

    public PageEmbeddingBatchJob(JdbcTemplate jdbcTemplate, EmbeddingClient embeddingClient, EmbeddingJobProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embeddingClient;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            log.info("Embedding job disabled (embedding.job.enabled=false).");
            return;
        }
        long totalProcessed = 0;
        long overallStart = System.nanoTime();
        while (true) {
            List<PageRecord> batch = loadMissingEmbeddings(properties.batchSize());
            if (batch.isEmpty()) {
                break;
            }
            long batchStart = System.nanoTime();
            int processed = processBatch(batch);
            totalProcessed += processed;
            Duration batchDuration = Duration.ofNanos(System.nanoTime() - batchStart);
            log.info("Embedding batch processed {} records in {} ms.", processed, batchDuration.toMillis());
        }
        Duration overallDuration = Duration.ofNanos(System.nanoTime() - overallStart);
        log.info("Embedding job finished. Total records: {}. Total latency: {} ms.", totalProcessed, overallDuration.toMillis());
    }

    private List<PageRecord> loadMissingEmbeddings(int batchSize) {
        String sql = """
                SELECT p.%s AS page_id, p.%s AS content
                FROM %s p
                LEFT JOIN %s e
                  ON p.%s = e.%s
                WHERE e.%s IS NULL
                ORDER BY p.%s
                LIMIT ?
                """.formatted(
                properties.pageIdColumn(),
                properties.pageContentColumn(),
                properties.pageTable(),
                properties.embeddingTable(),
                properties.pageIdColumn(),
                properties.embeddingPageIdColumn(),
                properties.embeddingPageIdColumn(),
                properties.pageIdColumn()
        );
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new PageRecord(rs.getLong("page_id"), rs.getString("content")),
                batchSize
        );
    }

    private int processBatch(List<PageRecord> records) {
        String upsertSql = """
                INSERT INTO %s (%s, %s)
                VALUES (?, ?::vector)
                ON CONFLICT (%s)
                DO UPDATE SET %s = EXCLUDED.%s
                """.formatted(
                properties.embeddingTable(),
                properties.embeddingPageIdColumn(),
                properties.embeddingVectorColumn(),
                properties.embeddingPageIdColumn(),
                properties.embeddingVectorColumn(),
                properties.embeddingVectorColumn()
        );
        int expectedDimension = properties.expectedDimension();
        int processed = 0;
        for (PageRecord record : records) {
            List<Double> vector = embeddingClient.embed(record.content());
            if (expectedDimension > 0 && vector.size() != expectedDimension) {
                log.warn("Embedding dimension mismatch for page {}. Expected {}, got {}.",
                        record.pageId(), expectedDimension, vector.size());
            }
            String vectorLiteral = toVectorLiteral(vector);
            processed += jdbcTemplate.update(upsertSql, record.pageId(), vectorLiteral);
        }
        return processed;
    }

    private String toVectorLiteral(List<Double> vector) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (Double value : vector) {
            joiner.add(String.valueOf(value));
        }
        return joiner.toString();
    }

    private record PageRecord(long pageId, String content) {
    }
}
