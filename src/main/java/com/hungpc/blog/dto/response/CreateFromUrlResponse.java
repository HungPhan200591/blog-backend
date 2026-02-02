package com.hungpc.blog.dto.response;

import com.hungpc.blog.entity.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for post created from URL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFromUrlResponse {

    /**
     * Generated post ID
     */
    private Long postId;

    /**
     * Translated title (Vietnamese)
     */
    private String title;

    /**
     * Generated slug
     */
    private String slug;

    /**
     * Post status (always DRAFT for URL-created posts)
     */
    private PostStatus status;

    /**
     * Translated content (Vietnamese markdown)
     */
    private String translatedContent;

    /**
     * Original source URL
     */
    private String originalUrl;

    /**
     * Category name
     */
    private String categoryName;

    /**
     * Series name (if applicable)
     */
    private String seriesName;

    /**
     * Tags
     */
    private List<String> tags;

    /**
     * Estimated read time (minutes)
     */
    private Integer estimatedReadTime;

    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Git commit hash (if pushed to Git)
     */
    private String gitCommitHash;
}
