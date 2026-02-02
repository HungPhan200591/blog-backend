package com.hungpc.blog.service;

import com.hungpc.blog.model.Category;
import com.hungpc.blog.model.Tag;
import com.hungpc.blog.repository.CategoryRepository;
import com.hungpc.blog.repository.TagRepository;
import com.hungpc.blog.service.ai.AIProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for auto-filling missing post metadata using AI
 * Uses pluggable AIProvider interface (Gemini, OpenAI, Claude, etc.)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AutoFillService {

    private final AIProvider aiProvider;
    private final PexelsService pexelsService;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    /**
     * Extract title from markdown content (first # heading)
     */
    public String extractTitleFromMarkdown(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return null;
        }

        // Match first # heading
        Pattern pattern = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(markdown);

        if (matcher.find()) {
            String title = matcher.group(1).trim();
            log.debug("Extracted title from markdown: {}", title);
            return title;
        }

        log.warn("No title found in markdown");
        return null;
    }

    /**
     * Auto-fill metadata using AI
     * Returns: [category, tags, description]
     */
    public MetadataResult generateMetadata(String title, String content) {
        log.info("🤖 Generating metadata with AI...");

        try {
            // Use AI to generate category, tags, description
            String prompt = buildMetadataPrompt(title, content);
            log.info("🤖 Using AI Provider: {}", aiProvider.getProviderName());
            String aiResponse = aiProvider.generateContent(prompt);

            // Extract metadata from AI response
            String category = extractCategory(aiResponse);
            List<String> tags = extractTags(aiResponse);
            String description = extractDescription(aiResponse);

            log.info("✅ AI metadata generated: category={}, tags={}, description length={}",
                    category, tags.size(), description != null ? description.length() : 0);

            return new MetadataResult(category, tags, description);

        } catch (Exception e) {
            log.error("❌ Failed to generate metadata with AI", e);
            // Return defaults on error
            return new MetadataResult("Uncategorized", List.of(), null);
        }
    }

    /**
     * Generate cover image URL using Pexels
     */
    public String generateCoverImage(String title) {
        log.info("🖼️ Generating cover image...");
        String coverUrl = pexelsService.searchCoverImage(title);

        if (coverUrl != null) {
            log.info("✅ Cover image generated: {}", coverUrl);
        } else {
            log.warn("⚠️ Failed to generate cover image");
        }

        return coverUrl;
    }

    private String buildMetadataPrompt(String title, String content) {
        // Fetch existing categories and tags (exclude Uncategorized)
        String existingCategories = categoryRepository.findAll().stream()
                .filter(c -> !"Uncategorized".equalsIgnoreCase(c.getName()))
                .map(Category::getName)
                .collect(java.util.stream.Collectors.joining(", "));

        String existingTags = tagRepository.findAll().stream()
                .map(Tag::getName)
                .collect(java.util.stream.Collectors.joining(", "));

        // Truncate content to avoid token limits
        String truncatedContent = content.length() > 1000
                ? content.substring(0, 1000) + "..."
                : content;

        return String.format(
                """
                        Analyze this Vietnamese blog post and generate metadata.

                        Title: %s

                        Content:
                        %s

                        Generate:
                        1. Category: Choose the MOST appropriate existing category OR CREATE A NEW ONE if none fit well.
                           Existing categories: %s

                           ⚠️ CRITICAL NAMING RULES:
                           - **MUST use Title Case format**: "System Design", "Backend", "Frontend" (NOT "system-design", "backend")
                           - **NEVER use "Uncategorized"** - this is FORBIDDEN!
                           - **If existing category matches, use EXACT name** (e.g., if "System Design" exists, use "System Design", NOT "system-design")
                           - **If no existing category fits, CREATE NEW in Title Case** (e.g., "Machine Learning", "Cloud Computing")
                           - Category should be broad topic area (e.g., "Backend", "Frontend", "DevOps", "AI", "System Design")
                           - Be specific but not too narrow (e.g., "Spring Boot" → use "Backend", "React" → use "Frontend")

                        2. Tags: Choose 3-5 relevant tags from existing OR suggest new ones if needed.
                           Existing tags: %s

                           ⚠️ CRITICAL NAMING RULES:
                           - **MUST use kebab-case format**: "spring-boot", "system-design", "rest-api" (NOT "Spring Boot", "System Design")
                           - **If existing tag matches, use EXACT name** (e.g., if "spring-boot" exists, use "spring-boot", NOT "Spring Boot")
                           - **If creating new tag, use kebab-case** (e.g., "machine-learning", "cloud-computing")
                           - Tags should be specific topics/technologies (e.g., "java", "spring-boot", "rest-api", "microservices")

                        3. Description: Write 1-2 SHORT sentences in Vietnamese (under 200 chars).
                           Should summarize the main topic and value.
                           MUST end with complete sentence (., !, or ?).

                        Format your response EXACTLY as:
                        <!-- CATEGORY: [category] -->
                        <!-- TAGS: [tag1, tag2, tag3] -->
                        <!-- DESCRIPTION: [description] -->
                        """,
                title, truncatedContent, existingCategories, existingTags);
    }

    private String extractCategory(String aiResponse) {
        Pattern pattern = Pattern.compile("<!--\\s*CATEGORY:\\s*(.+?)\\s*-->", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Uncategorized";
    }

    private List<String> extractTags(String aiResponse) {
        Pattern pattern = Pattern.compile("<!--\\s*TAGS:\\s*(.+?)\\s*-->", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            String tagsStr = matcher.group(1).trim();
            return List.of(tagsStr.split("\\s*,\\s*"));
        }
        return List.of();
    }

    private String extractDescription(String aiResponse) {
        Pattern pattern = Pattern.compile("<!--\\s*DESCRIPTION:\\s*(.+?)\\s*-->", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Result object for metadata generation
     */
    public record MetadataResult(
            String category,
            List<String> tags,
            String description) {
    }
}
