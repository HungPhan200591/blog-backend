-- Performance Optimization: Add indexes for All Posts filtering

-- Index for search on title and slug
CREATE INDEX IF NOT EXISTS idx_posts_title_lower ON posts (LOWER(title));
CREATE INDEX IF NOT EXISTS idx_posts_slug_lower ON posts (LOWER(slug));

-- Index for category filter
CREATE INDEX IF NOT EXISTS idx_categories_name_lower ON categories (LOWER(name));

-- Index for series filter  
CREATE INDEX IF NOT EXISTS idx_series_title_lower ON series (LOWER(title));

-- Index for tags filter
CREATE INDEX IF NOT EXISTS idx_tags_name_lower ON tags (LOWER(name));

-- Composite index for post_tags (for tags AND logic)
CREATE INDEX IF NOT EXISTS idx_post_tags_post_tag ON post_tags (post_id, tag_id);

-- Index for published status filter
CREATE INDEX IF NOT EXISTS idx_posts_is_published ON posts (is_published);

-- Index for sorting by created_at (most common sort)
CREATE INDEX IF NOT EXISTS idx_posts_created_at_desc ON posts (created_at DESC);

-- Analyze tables to update statistics
ANALYZE posts;
ANALYZE categories;
ANALYZE series;
ANALYZE tags;
ANALYZE post_tags;
