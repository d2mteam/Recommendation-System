-- Hybrid search setup for PostgreSQL + pgvector
-- Assumes an existing table (example: items) with a text column and a vector embedding.
-- Replace table/column names as needed.

-- 1) Add a tsvector column (generated) or create a view
ALTER TABLE items
    ADD COLUMN IF NOT EXISTS tsv tsvector
        GENERATED ALWAYS AS (to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(body, '')))
        STORED;

-- Optional: a view if you prefer not to add a stored column
CREATE OR REPLACE VIEW items_search AS
SELECT
    i.*, 
    to_tsvector('simple', coalesce(i.title, '') || ' ' || coalesce(i.body, '')) AS tsv
FROM items i;

-- 2) Create GIN index for FTS
CREATE INDEX IF NOT EXISTS items_tsv_gin
    ON items
    USING GIN (tsv);

-- 3) Create HNSW index for vector search (cosine distance)
-- Requires pgvector extension and vector_cosine_ops
CREATE INDEX IF NOT EXISTS items_embedding_hnsw
    ON items
    USING HNSW (embedding vector_cosine_ops);

-- 4) Hybrid query: FTS score + cosine similarity
-- :alpha balances FTS vs vector similarity (0..1)
-- :query_text and :query_vector are parameters
WITH
    q AS (
        SELECT
            to_tsquery('simple', :query_text) AS ts_query,
            :query_vector::vector AS query_vector,
            :alpha::float AS alpha
    )
SELECT
    i.id,
    ts_rank_cd(i.tsv, q.ts_query) AS fts_score,
    (1 - (i.embedding <=> q.query_vector)) AS vector_score,
    (q.alpha * ts_rank_cd(i.tsv, q.ts_query)
        + (1 - q.alpha) * (1 - (i.embedding <=> q.query_vector))) AS hybrid_score
FROM items i
CROSS JOIN q
WHERE i.tsv @@ q.ts_query
ORDER BY hybrid_score DESC
LIMIT 20;

-- 5) Verify planner uses GIN + HNSW
EXPLAIN ANALYZE
WITH
    q AS (
        SELECT
            to_tsquery('simple', :query_text) AS ts_query,
            :query_vector::vector AS query_vector,
            :alpha::float AS alpha
    )
SELECT
    i.id,
    (q.alpha * ts_rank_cd(i.tsv, q.ts_query)
        + (1 - q.alpha) * (1 - (i.embedding <=> q.query_vector))) AS hybrid_score
FROM items i
CROSS JOIN q
WHERE i.tsv @@ q.ts_query
ORDER BY hybrid_score DESC
LIMIT 20;
