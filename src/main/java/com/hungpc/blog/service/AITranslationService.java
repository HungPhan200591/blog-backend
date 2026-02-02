package com.hungpc.blog.service;

import com.hungpc.blog.dto.FrontmatterDTO;
import com.hungpc.blog.dto.internal.TranslationResult;
import com.hungpc.blog.model.Category;
import com.hungpc.blog.model.Tag;
import com.hungpc.blog.repository.CategoryRepository;
import com.hungpc.blog.repository.PostRepository;
import com.hungpc.blog.repository.TagRepository;
import com.hungpc.blog.service.ai.AIProvider;
import com.hungpc.blog.util.FrontmatterParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for AI-powered translation
 * Translates English markdown content to Vietnamese while preserving formatting
 * Uses pluggable AIProvider interface (Gemini, OpenAI, Claude, etc.)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AITranslationService {

    private final AIProvider aiProvider;
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    /**
     * Translate English markdown to Vietnamese
     * 
     * @param englishMarkdown The English markdown content
     * @param originalUrl     The original URL of the article
     * @return TranslationResult with translated content and metadata
     */
    public TranslationResult translateToVietnamese(String englishMarkdown, String originalUrl) {
        log.info("Starting translation: {} chars", englishMarkdown.length());
        long startTime = System.currentTimeMillis();

        try {
            // 1. Build translation prompt (with original URL for AI to add link)
            String prompt = buildTranslationPrompt(englishMarkdown, originalUrl);

            // 2. Call AI Provider (Gemini, OpenAI, Claude, etc.)
            log.info("🤖 Using AI Provider: {}", aiProvider.getProviderName());
            String translatedMarkdown = aiProvider.generateContent(prompt);

            // 3. Extract slug from HTML comment
            String slug = extractSlugFromMarkdown(translatedMarkdown);
            if (slug == null || slug.isEmpty()) {
                log.warn("No slug found in AI response, generating from English title");
                slug = generateSlug(extractTitle(englishMarkdown));
            }

            // 4. Ensure slug is unique
            slug = ensureUniqueSlug(slug);

            // 5. Remove slug comment from content
            String cleanedMarkdown = removeSlugComment(translatedMarkdown);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Translation complete: {}ms", duration);

            return TranslationResult.builder()
                    .originalMarkdown(englishMarkdown)
                    .translatedMarkdown(cleanedMarkdown)
                    .slug(slug)
                    .build();

        } catch (Exception e) {
            log.error("❌ Translation failed", e);
            throw new RuntimeException("Failed to translate content: " + e.getMessage(), e);
        }
    }

    /**
     * Build optimized translation prompt for Gemini with category/tag context
     */
    private String buildTranslationPrompt(String englishMarkdown, String originalUrl) {
        // Fetch existing categories and tags for context (exclude Uncategorized)
        List<Category> categories = categoryRepository.findAll();
        List<Tag> tags = tagRepository.findAll();

        String categoryContext = categories.stream()
                .filter(c -> !"Uncategorized".equalsIgnoreCase(c.getName()))
                .map(Category::getName)
                .collect(Collectors.joining(", "));
        categoryContext = categoryContext.isEmpty() ? "No existing categories" : categoryContext;

        String tagContext = tags.isEmpty() ? "No existing tags"
                : tags.stream().map(Tag::getName).collect(Collectors.joining(", "));

        return """
                Translate the following English article to Vietnamese.

                CRITICAL OUTPUT FORMAT:
                You MUST return the translated article with this EXACT structure:

                1. First line: HTML comment with slug
                2. Then: YAML frontmatter block
                3. Then: Translated content

                Example:
                <!-- SLUG: url-friendly-slug -->
                ---
                title: "Vietnamese Title"
                category: "Category Name"
                tags:
                  - tag1
                  - tag2
                description: "Vietnamese description"
                coverImage: ""
                publishedAt: null
                ---

                # Vietnamese Title
                (English Title)

                > **Bài viết gốc**: %s

                [Rest of translated content...]

                SLUG COMMENT RULES:
                1. **MUST be first line** of output
                2. **Format**: <!-- SLUG: url-friendly-slug -->
                3. **Generated from ENGLISH title** (not Vietnamese)
                4. **Format**: lowercase, hyphen-separated, no special characters
                5. **Example**: "How to Build REST API" → <!-- SLUG: how-to-build-rest-api -->

                FRONTMATTER RULES:
                1. **title**: Vietnamese title
                   Example: "Nghiện phản bác: Căn bệnh nan y của Internet"

                2. **ORIGINAL ARTICLE LINK**:
                - **MUST add original article link** right after (English Title)
                - Format/Example: `> **Bài viết gốc**: https://original-url.com`

                3. **category**: Choose from existing OR create new appropriate category
                   Existing categories: %s

                   ⚠️ CRITICAL NAMING RULES:
                   - **MUST use Title Case format**: "System Design", "Backend", "Frontend" (NOT "system-design", "backend")
                   - **NEVER use "Uncategorized"** - this is FORBIDDEN!
                   - **If existing category matches, use EXACT name** (e.g., if "System Design" exists, use "System Design", NOT "system-design")
                   - **If no existing category fits, CREATE NEW in Title Case** (e.g., "Machine Learning", "Cloud Computing")
                   - Category should be broad topic area (e.g., "Backend", "Frontend", "DevOps", "AI", "System Design")
                   - Be specific but not too narrow (e.g., "Spring Boot" → use "Backend")

                4. **tags**: 2-5 relevant tags (choose from existing OR create new)
                   Existing tags: %s

                   ⚠️ CRITICAL NAMING RULES:
                   - **MUST use kebab-case format**: "spring-boot", "system-design", "rest-api" (NOT "Spring Boot", "System Design")
                   - **If existing tag matches, use EXACT name** (e.g., if "spring-boot" exists, use "spring-boot", NOT "Spring Boot")
                   - **If creating new tag, use kebab-case** (e.g., "machine-learning", "cloud-computing")
                   - Tags should be specific topics/technologies
                   - Use YAML array format with hyphens

                5. **description**: 1-2 SHORT sentences in Vietnamese summarizing the article
                   - Keep it VERY concise and engaging
                   - Max 250 characters
                   - MUST end with complete sentence (., !, or ?)

                6. **coverImage**: Leave as empty string ""

                7. **publishedAt**: Leave as null

                TRANSLATION RULES:
                1. **Preserve ALL markdown formatting** (headers, code blocks, links, images, lists, tables)
                2. **Keep technical terms in English** (API, REST, JSON, HTTP, database, etc.)
                3. **Keep code blocks COMPLETELY UNCHANGED** (do not translate code, comments, or variable names)
                4. **Keep URLs unchanged** (links, image URLs)
                5. **Maintain natural Vietnamese flow** (not word-by-word translation)
                6. **Keep image alt text in English** (for SEO)

                FORMATTING EXAMPLES:
                - `# Vietnamese Title` followed by `(English Title)` on next line
                - ` ```java ... ``` ` → ` ```java ... ``` ` (keep code blocks unchanged)
                - `[Link](url)` → `[Liên kết](url)` (translate text, keep URL)
                - `![alt](url)` → `![alt](url)` (keep image syntax unchanged)

                TRANSLATION STYLE:
                - Use professional, technical Vietnamese
                - Maintain original paragraph structure
                - Keep the same tone and voice
                - Preserve emphasis (bold, italic)

                Article to translate:

                %s
                """
                .formatted(categoryContext, tagContext, originalUrl, englishMarkdown);
    }

    /**
     * Extract slug from AI-generated markdown comment
     * Format: <!-- SLUG: your-slug-here -->
     */
    private String extractSlugFromMarkdown(String markdown) {
        Pattern pattern = Pattern.compile("<!--\\s*SLUG:\\s*([a-z0-9-]+)\\s*-->", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(markdown);

        if (matcher.find()) {
            String slug = matcher.group(1).trim().toLowerCase();
            log.debug("Extracted slug from AI response: {}", slug);
            return slug;
        }

        return null;
    }

    /**
     * Ensure slug is unique by checking database and appending counter if needed
     */
    private String ensureUniqueSlug(String baseSlug) {
        String slug = baseSlug;
        int counter = 1;
        while (postRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
            log.debug("Slug exists, trying: {}", slug);
        }
        return slug;
    }

    /**
     * Remove slug HTML comment from markdown content
     * Format: <!-- SLUG: your-slug-here -->
     */
    private String removeSlugComment(String markdown) {
        return markdown.replaceFirst("(?m)^\\s*<!-- SLUG:\\s*[a-z0-9-]+\\s*-->\\s*\\n?", "");
    }

    /**
     * Add original article link after title
     */
    private String addOriginalLink(String markdown, String originalUrl) {
        // Find first H1 and add link after it
        Pattern pattern = Pattern.compile("(^#\\s+.+$\\n(?:\\(.+\\)\\n)?)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(markdown);

        if (matcher.find()) {
            String titleSection = matcher.group(1);
            String linkLine = "\nLink bài viết gốc: " + originalUrl + "\n";
            return matcher.replaceFirst(titleSection + linkLine);
        }

        return markdown;
    }

    /**
     * Extract title from markdown (first H1)
     */
    private String extractTitle(String markdown) {
        Pattern pattern = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(markdown);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Fallback: use first line
        String[] lines = markdown.split("\\n");
        return lines.length > 0 ? lines[0].trim() : "Untitled";
    }

    /**
     * Generate slug from title
     * Converts Vietnamese title to URL-friendly slug
     */
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special chars
                .trim()
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-"); // Remove duplicate hyphens
    }
}
