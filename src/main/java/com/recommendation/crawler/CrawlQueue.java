package com.recommendation.crawler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "crawl_queue")
public class CrawlQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrawlPriority priority;

    @Column(name = "next_crawl_at", nullable = false)
    private Instant nextCrawlAt;

    protected CrawlQueue() {
    }

    public CrawlQueue(String url, CrawlPriority priority, Instant nextCrawlAt) {
        this.url = url;
        this.priority = priority;
        this.nextCrawlAt = nextCrawlAt;
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public CrawlPriority getPriority() {
        return priority;
    }

    public void setPriority(CrawlPriority priority) {
        this.priority = priority;
    }

    public Instant getNextCrawlAt() {
        return nextCrawlAt;
    }

    public void setNextCrawlAt(Instant nextCrawlAt) {
        this.nextCrawlAt = nextCrawlAt;
    }
}
