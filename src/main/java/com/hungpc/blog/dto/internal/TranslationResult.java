package com.hungpc.blog.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for translation results
 * Used to pass data between AITranslationService and PostFromUrlService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResult {

    /**
     * Original English markdown
     */
    private String originalMarkdown;

    /**
     * Translated Vietnamese markdown (with YAML frontmatter from Gemini)
     */
    private String translatedMarkdown;

    /**
     * Extracted slug from HTML comment (then removed from content)
     * Format: <!-- SLUG: url-friendly-slug -->
     */
    private String slug;
}
