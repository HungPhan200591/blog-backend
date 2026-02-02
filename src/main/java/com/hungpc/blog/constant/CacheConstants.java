package com.hungpc.blog.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Centralize all cache names to prevent hardcoding strings throughout the
 * application.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheConstants {

    // Post related caches
    public static final String POSTS = "posts";
    public static final String POST_BY_SLUG = "postBySlug";
    public static final String RELATED_POSTS = "relatedPosts";
    public static final String FEATURED_POSTS = "featuredPosts";
    public static final String LATEST_POSTS = "latestPosts";

    // Taxonomy caches
    public static final String CATEGORIES = "categories";
    public static final String TAGS = "tags";
    public static final String SERIES = "series";
}
