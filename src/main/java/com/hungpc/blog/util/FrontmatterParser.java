package com.hungpc.blog.util;

import com.hungpc.blog.dto.FrontmatterDTO;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for YAML frontmatter in markdown files.
 * 
 * Frontmatter format:
 * ---
 * title: "Post Title"
 * category: Backend
 * tags:
 * - java
 * - spring-boot
 * description: "Description"
 * coverImage: "https://..."
 * publishedAt: 2024-01-01T10:00:00
 * ---
 * 
 * # Markdown content starts here...
 */
@Slf4j
public class FrontmatterParser {

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "^---\\s*\\n(.*?)\\n---\\s*\\n",
            Pattern.DOTALL);

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    /**
     * Check if content has frontmatter
     */
    public static boolean hasFrontmatter(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        return FRONTMATTER_PATTERN.matcher(content).find();
    }

    /**
     * Parse frontmatter from markdown content
     * 
     * @param content Full markdown content with frontmatter
     * @return FrontmatterDTO or null if no frontmatter found
     */
    public static FrontmatterDTO parse(String content) {
        if (content == null || content.isBlank()) {
            log.warn("Content is null or empty, cannot parse frontmatter");
            return null;
        }

        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            log.warn("No frontmatter found in content");
            return null;
        }

        String yamlContent = matcher.group(1);
        log.debug("Extracted YAML frontmatter: {}", yamlContent);

        try {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(yamlContent);

            if (data == null || data.isEmpty()) {
                log.warn("Frontmatter is empty");
                return null;
            }

            return FrontmatterDTO.builder()
                    .title(getString(data, "title"))
                    .category(getString(data, "category"))
                    .tags(getStringList(data, "tags"))
                    .description(getString(data, "description"))
                    .coverImage(getString(data, "coverImage"))
                    .publishedAt(getDateTime(data, "publishedAt"))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse frontmatter YAML: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Strip frontmatter from content, leaving only markdown body
     * 
     * @param content Full markdown content with frontmatter
     * @return Content without frontmatter
     */
    public static String stripFrontmatter(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        return FRONTMATTER_PATTERN.matcher(content).replaceFirst("");
    }

    /**
     * Build YAML frontmatter string from DTO
     * 
     * @param frontmatter FrontmatterDTO
     * @return YAML string (without --- markers)
     */
    public static String buildYaml(FrontmatterDTO frontmatter) {
        if (frontmatter == null) {
            return "";
        }

        StringBuilder yaml = new StringBuilder();

        // Title (required)
        if (frontmatter.getTitle() != null) {
            yaml.append("title: \"").append(escapeYaml(frontmatter.getTitle())).append("\"\n");
        }

        // Category (required)
        if (frontmatter.getCategory() != null) {
            yaml.append("category: ").append(frontmatter.getCategory()).append("\n");
        }

        // Tags (optional)
        if (frontmatter.getTags() != null && !frontmatter.getTags().isEmpty()) {
            yaml.append("tags:\n");
            for (String tag : frontmatter.getTags()) {
                yaml.append("  - ").append(tag).append("\n");
            }
        }

        // Description (optional)
        if (frontmatter.getDescription() != null) {
            yaml.append("description: \"").append(escapeYaml(frontmatter.getDescription())).append("\"\n");
        }

        // Cover Image (optional)
        String coverImage = frontmatter.getCoverImage();
        if (coverImage != null && !coverImage.isBlank()) {
            yaml.append("coverImage: \"").append(coverImage).append("\"\n");
        } else {
            yaml.append("coverImage: \"\"\n");
        }

        // Published At (optional)
        if (frontmatter.getPublishedAt() != null) {
            yaml.append("publishedAt: ").append(frontmatter.getPublishedAt().toString()).append("\n");
        } else {
            yaml.append("publishedAt: null\n");
        }

        return yaml.toString();
    }

    /**
     * Replace frontmatter in existing content
     * 
     * @param content        Original content with frontmatter
     * @param newFrontmatter New frontmatter DTO
     * @return Updated content
     */
    public static String replaceFrontmatter(String content, FrontmatterDTO newFrontmatter) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String body = stripFrontmatter(content);
        String yaml = buildYaml(newFrontmatter);

        return "---\n" + yaml + "---\n\n" + body;
    }

    // ========== Helper Methods ==========

    private static String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return new ArrayList<>();
        }

        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(item.toString().trim());
                }
            }
            return result;
        }

        // Single value → convert to list
        return List.of(value.toString().trim());
    }

    private static LocalDateTime getDateTime(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null || "null".equalsIgnoreCase(value.toString())) {
            return null;
        }

        String dateStr = value.toString().trim();

        // Try multiple date formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        log.warn("Could not parse date: {}", dateStr);
        return null;
    }

    private static String escapeYaml(String value) {
        if (value == null) {
            return "";
        }
        // Escape double quotes
        return value.replace("\"", "\\\"");
    }
}
