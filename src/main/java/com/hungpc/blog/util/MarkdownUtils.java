package com.hungpc.blog.util;

import lombok.experimental.UtilityClass;

/**
 * Utility class for markdown content processing
 */
@UtilityClass
public class MarkdownUtils {

    /**
     * Remove YAML frontmatter from markdown content
     * Frontmatter is delimited by --- at the beginning and end
     * 
     * @param markdown The markdown content with frontmatter
     * @return The markdown content without frontmatter
     */
    public static String removeFrontmatter(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return markdown;
        }

        // Check if markdown starts with ---
        if (!markdown.trim().startsWith("---")) {
            return markdown; // No frontmatter
        }

        // Find the second --- (end of frontmatter)
        int firstDelimiter = markdown.indexOf("---");
        int secondDelimiter = markdown.indexOf("---", firstDelimiter + 3);

        if (secondDelimiter > 0) {
            // Return content after second ---
            return markdown.substring(secondDelimiter + 3).trim();
        }

        // If no second delimiter found, return original
        return markdown;
    }
}
