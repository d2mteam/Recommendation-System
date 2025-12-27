CREATE TABLE IF NOT EXISTS crawl_state (
    id BIGSERIAL PRIMARY KEY,
    url TEXT NOT NULL UNIQUE,
    last_crawled_at TIMESTAMPTZ,
    etag TEXT,
    last_modified TIMESTAMPTZ,
    status TEXT,
    content_hash TEXT
);

CREATE TABLE IF NOT EXISTS crawl_queue (
    id BIGSERIAL PRIMARY KEY,
    url TEXT NOT NULL UNIQUE,
    priority TEXT NOT NULL,
    next_crawl_at TIMESTAMPTZ NOT NULL
);
