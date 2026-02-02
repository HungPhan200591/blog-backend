package com.hungpc.blog.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents parsed markdown content with frontmatter
 */
@Data
@Builder
public class MarkdownContent {

    /**
     * Raw markdown content (without frontmatter)
     */
    private String content;

    /**
     * YAML frontmatter as key-value map
     */
    private Map<String, Object> frontmatter;

    /**
     * Get frontmatter value as String
     */
    public String getFrontmatterString(String key) {
        if (frontmatter == null) {
            return null;
        }
        Object value = frontmatter.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Get frontmatter value as List
     */
    @SuppressWarnings("unchecked")
    public List<String> getFrontmatterList(String key) {
        if (frontmatter == null) {
            return Collections.emptyList();
        }
        Object value = frontmatter.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return Collections.emptyList();
    }
}
