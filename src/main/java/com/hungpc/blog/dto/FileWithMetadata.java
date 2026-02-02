package com.hungpc.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper for markdown file content with parsed frontmatter metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileWithMetadata {

    /**
     * Parsed frontmatter metadata (null if no frontmatter)
     */
    private FrontmatterDTO metadata;

    /**
     * Markdown content WITHOUT frontmatter
     * (frontmatter has been stripped)
     */
    private String content;

    /**
     * Original raw content WITH frontmatter
     * (for debugging or re-parsing)
     */
    private String rawContent;
}
