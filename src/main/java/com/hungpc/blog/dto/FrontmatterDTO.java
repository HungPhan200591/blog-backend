package com.hungpc.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for YAML frontmatter metadata from markdown files.
 * 
 * Example frontmatter:
 * ---
 * title: "Post Title"
 * category: Backend
 * tags:
 * - java
 * - spring-boot
 * description: "SEO description."
 * coverImage: ""
 * publishedAt: null
 * ---
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrontmatterDTO {

    /**
     * Post title (required)
     */
    private String title;

    /**
     * Category name (required)
     * Will be used to find/create category
     */
    private String category;

    /**
     * List of tag names (optional)
     * Will be used to find/create tags
     */
    private List<String> tags;

    /**
     * SEO description (optional)
     */
    private String description;

    /**
     * Cover image URL (optional)
     * Will be generated via Pexels if empty
     */
    private String coverImage;

    /**
     * Published date (optional)
     * null = draft, non-null = published
     */
    private LocalDateTime publishedAt;
}
