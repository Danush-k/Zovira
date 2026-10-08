-- Full-text and fuzzy search support.
--
-- Titles and keywords use the 'simple' configuration so model names and numbers ("iPhone 16",
-- "WH-1000XM5") are indexed verbatim; descriptions use English stemming. Trigram indexes power
-- typo-tolerant matching and autocomplete.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE products
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(search_keywords, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(short_description, '')), 'C')
        ) STORED;

CREATE INDEX ix_products_search_vector ON products USING GIN (search_vector);
CREATE INDEX ix_products_title_trgm ON products USING GIN (lower(title) gin_trgm_ops);
CREATE INDEX ix_brands_name_trgm ON brands USING GIN (lower(name) gin_trgm_ops);
CREATE INDEX ix_categories_name_trgm ON categories USING GIN (lower(name) gin_trgm_ops);
CREATE INDEX ix_search_history_query_trgm ON search_history USING GIN (lower(query) gin_trgm_ops);
