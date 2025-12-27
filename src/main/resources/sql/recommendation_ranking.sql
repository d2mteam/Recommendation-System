-- Aggregate view counts into a dedicated table for fast ranking.
CREATE TABLE IF NOT EXISTS article_view_stats (
    article_id BIGINT PRIMARY KEY,
    view_count BIGINT NOT NULL DEFAULT 0,
    last_viewed_at TIMESTAMP
);

-- Initial backfill from raw view logs.
INSERT INTO article_view_stats (article_id, view_count, last_viewed_at)
SELECT
    view_logs.article_id,
    COUNT(*) AS view_count,
    MAX(view_logs.viewed_at) AS last_viewed_at
FROM view_logs
GROUP BY view_logs.article_id
ON CONFLICT (article_id)
DO UPDATE SET
    view_count = EXCLUDED.view_count,
    last_viewed_at = EXCLUDED.last_viewed_at;

-- Ranking query with derived age_days and weighted ordering.
WITH ranked_articles AS (
    SELECT
        articles.id,
        COALESCE(view_stats.view_count, 0) AS view_count,
        DATE_PART('day', NOW() - articles.crawled_at) AS age_days,
        COALESCE(articles.similarity_score, 0) AS similarity_score,
        (1.0 / (1.0 + DATE_PART('day', NOW() - articles.crawled_at))) AS freshness_score
    FROM articles
    LEFT JOIN article_view_stats AS view_stats
        ON view_stats.article_id = articles.id
)
SELECT
    id,
    view_count,
    age_days,
    similarity_score
FROM ranked_articles
ORDER BY
    (0.6 * LN(1 + view_count))
    + (0.2 * similarity_score)
    + (0.2 * freshness_score) DESC;
