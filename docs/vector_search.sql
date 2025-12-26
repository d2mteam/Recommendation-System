-- Top-k vector search with join from page_embedding to page using <-> distance operator.
-- Replace :query_embedding with your vector literal (e.g., '[0.1,0.2,...]').
-- Replace :k with the desired top-k.
SELECT
    p.id,
    p.title,
    p.url,
    pe.embedding <-> :query_embedding AS distance
FROM page_embedding AS pe
JOIN page AS p
    ON p.id = pe.page_id
ORDER BY pe.embedding <-> :query_embedding
LIMIT :k;

-- Validate the planner uses the HNSW index with EXPLAIN ANALYZE.
EXPLAIN ANALYZE
SELECT
    p.id,
    p.title,
    p.url,
    pe.embedding <-> :query_embedding AS distance
FROM page_embedding AS pe
JOIN page AS p
    ON p.id = pe.page_id
ORDER BY pe.embedding <-> :query_embedding
LIMIT :k;

-- Set hnsw.ef_search at runtime for the session.
-- Higher values can improve recall at the cost of latency.
SET hnsw.ef_search = 80;

-- Check latency and top-k results after adjusting ef_search.
EXPLAIN ANALYZE
SELECT
    p.id,
    p.title,
    p.url,
    pe.embedding <-> :query_embedding AS distance
FROM page_embedding AS pe
JOIN page AS p
    ON p.id = pe.page_id
ORDER BY pe.embedding <-> :query_embedding
LIMIT :k;
