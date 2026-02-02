package com.hungpc.blog.service;

import com.hungpc.blog.config.GitConfig;
import com.hungpc.blog.constant.CacheConstants;
import com.hungpc.blog.dto.FileWithMetadata;
import com.hungpc.blog.dto.FrontmatterDTO;
import com.hungpc.blog.dto.internal.TranslationResult;
import com.hungpc.blog.dto.request.CreateFromUrlRequest;
import com.hungpc.blog.dto.response.CreateFromUrlResponse;
import com.hungpc.blog.entity.PostStatus;
import com.hungpc.blog.model.Category;
import com.hungpc.blog.model.Post;
import com.hungpc.blog.model.PostTag;
import com.hungpc.blog.repository.CategoryRepository;
import com.hungpc.blog.repository.PostRepository;
import com.hungpc.blog.repository.PostTagRepository;
import com.hungpc.blog.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Main orchestrator service for creating posts from URLs
 * Coordinates the workflow: scrape → translate → save
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PostFromUrlService {

	private final WebScraperService scraperService;
	private final ImageService imageService;
	private final AITranslationService translationService;
	private final PostRepository postRepository;
	private final CategoryRepository categoryRepository;
	private final TagRepository tagRepository;
	private final PostTagRepository postTagRepository;
	private final GitService gitService;
	private final GitConfig gitConfig;
	private final PexelsService pexelsService;
	private final CategoryTagHelper categoryTagHelper;

	/**
	 * Create a post from URL
	 *
	 * @param request The create from URL request
	 * @return CreateFromUrlResponse with post details
	 */
	@CacheEvict(value = {
			CacheConstants.POST_BY_SLUG,
			CacheConstants.POSTS,
			CacheConstants.RELATED_POSTS,
			CacheConstants.FEATURED_POSTS,
			CacheConstants.LATEST_POSTS,
			CacheConstants.CATEGORIES,
			CacheConstants.TAGS,
			CacheConstants.SERIES
	}, allEntries = true)
	@Transactional
	public CreateFromUrlResponse createPostFromUrl(CreateFromUrlRequest request) {
		String url = request.getUrl();
		log.info("🚀 Starting URL-to-Post workflow for: {}", url);
		long workflowStart = System.currentTimeMillis();

		try {
			// Step 1: Scrape URL to markdown
			log.info("📥 Step 1/5: Scraping URL...");
			long step1Start = System.currentTimeMillis();
			String originalMarkdown = scraperService.scrapeToMarkdown(url);
			long step1Duration = System.currentTimeMillis() - step1Start;
			log.info("✅ Scraping complete: {}ms, {} chars", step1Duration, originalMarkdown.length());

			// // Step 2: Download and process images
			// log.info("🖼️ Step 2/5: Processing images...");
			// long step2Start = System.currentTimeMillis();
			// String markdownWithLocalImages =
			// imageService.processImages(originalMarkdown);
			// long step2Duration = System.currentTimeMillis() - step2Start;
			// log.info("✅ Image processing complete: {}ms", step2Duration);

			// Step 3: Translate to Vietnamese
			log.info("🌐 Step 3/5: Translating to Vietnamese...");
			long step3Start = System.currentTimeMillis();
			// TranslationResult translationResult =
			// translationService.translateToVietnamese(markdownWithLocalImages);
			TranslationResult translationResult = translationService.translateToVietnamese(originalMarkdown, url);
			long step3Duration = System.currentTimeMillis() - step3Start;
			log.info("✅ Translation complete: {}ms", step3Duration);

			FileWithMetadata fileWithMetadata = gitService.parseFileWithMetadata(translationResult.getTranslatedMarkdown());
			String slug = translationResult.getSlug();

			// Step 4: Save to database
			log.info("💾 Step 4/5: Saving to database...");
			long step4Start = System.currentTimeMillis();
			Post post = saveToDatabase(fileWithMetadata, slug);
			long step4Duration = System.currentTimeMillis() - step4Start;
			log.info("✅ Database save complete: {}ms, post_id={}", step4Duration, post.getId());

			// Step 5: Commit to Git
			log.info("📝 Step 5/5: Committing to Git...");
			long step5Start = System.currentTimeMillis();
			String commitHash = commitToGit(fileWithMetadata, slug);
			long step5Duration = System.currentTimeMillis() - step5Start;
			log.info("✅ Git commit complete: {}ms, hash={}", step5Duration,
					commitHash.substring(0, 7));

			long totalDuration = System.currentTimeMillis() - workflowStart;
			log.info("🎉 Workflow complete: {}ms total", totalDuration);

			// Build response using AI-suggested metadata
			return CreateFromUrlResponse.builder()
					.postId(post.getId())
					.title(post.getTitle())
					.slug(post.getSlug())
					.status(post.getIsPublished() ? PostStatus.PUBLISHED : PostStatus.DRAFT)
					.translatedContent(translationResult.getTranslatedMarkdown())
					.originalUrl(url)
					.categoryName(fileWithMetadata.getMetadata().getCategory())
					.seriesName(null) // Series ignored for now
					.tags(fileWithMetadata.getMetadata().getTags())
					.estimatedReadTime(null) // Not tracked anymore
					.createdAt(post.getCreatedAt())
					.gitCommitHash(commitHash)
					.build();

		} catch (Exception e) {
			log.error("❌ Workflow failed for URL: {}", url, e);
			throw new RuntimeException("Failed to create post from URL: " + e.getMessage(), e);
		}
	}

	/**
	 * Save post to database
	 * Handles category lookup/creation, slug generation, and tag associations
	 */
	private Post saveToDatabase(FileWithMetadata fileWithMetadata, String slug) {

		FrontmatterDTO originalFrontmatter = fileWithMetadata.getMetadata();

		// 1. Get or create category from AI suggestion
		String categoryName = originalFrontmatter.getCategory();
		if (categoryName == null || categoryName.isEmpty()) {
			categoryName = "Uncategorized";
		}

		Category category = categoryTagHelper.getOrCreateCategory(categoryName);
		log.debug("Using category: {} (id={})", category.getName(), category.getId());

		// 2. Fetch cover image from Pexels
		log.info("🖼️ Fetching cover image from Pexels...");
		String coverImageUrl = pexelsService.searchCoverImage(originalFrontmatter.getTitle());
		if (coverImageUrl != null) {
			log.info("✅ Cover image found: {}", coverImageUrl);
		} else {
			log.warn("⚠️ No cover image found, post will have no cover");
		}

		// 3. Update frontmatter with cover image URL
		FrontmatterDTO updatedFrontmatter = FrontmatterDTO.builder()
				.title(originalFrontmatter.getTitle())
				.category(originalFrontmatter.getCategory())
				.tags(originalFrontmatter.getTags())
				.description(originalFrontmatter.getDescription())
				.coverImage(coverImageUrl != null ? coverImageUrl : "")
				.publishedAt(originalFrontmatter.getPublishedAt())
				.build();

		// 5. Create Post entity
		Post post = Post.builder()
				.slug(slug)
				.title(updatedFrontmatter.getTitle())
				.description(updatedFrontmatter.getDescription())
				.content(fileWithMetadata.getContent())
				.coverImage(coverImageUrl)
				.categoryId(category.getId())
				.seriesId(null) // Ignore series for now
				.isPublished(true) // TRUE for me
				.visitCount(0L)
				.build();

		post = postRepository.save(post);
		log.info("✅ Saved post to database: id={}, slug={}", post.getId(), post.getSlug());

		// 4. Associate tags from AI suggestion
		List<String> tagNames = originalFrontmatter.getTags();
		if (tagNames != null && !tagNames.isEmpty()) {
			List<Long> tagIds = categoryTagHelper.getOrCreateTags(tagNames);

			// Create post-tag relationships
			for (Long tagId : tagIds) {
				PostTag postTag = PostTag.builder()
						.postId(post.getId())
						.tagId(tagId)
						.build();
				postTagRepository.save(postTag);
			}
			log.info("✅ Associated {} tags with post", tagIds.size());
		}

		return post;
	}

	/**
	 * Commit post to Git repository
	 * Gemini already includes YAML frontmatter in the translated markdown
	 */
	private String commitToGit(FileWithMetadata fileWithMetadata, String slug) {
		try {
			// 1. Write markdown file (already has frontmatter from Gemini)
			String filePath = gitConfig.getContentPath() + slug + ".md";
			gitService.writeFile(filePath, fileWithMetadata.getRawContent());
			log.info("✅ Wrote file with frontmatter: {}", filePath);

			// 2. Commit and push
			String commitMessage = String.format(
					"feat: Add post '%s' from URL.Slug: %s. Created via URL-to-Post feature",
					fileWithMetadata.getMetadata().getTitle(),
					slug);

			String commitHash = gitService.commitAndPush(commitMessage);
			log.info("✅ Committed to Git: {}", commitHash.substring(0, 7));

			return commitHash;

		} catch (Exception e) {
			log.error("❌ Failed to commit to Git", e);
			throw new RuntimeException("Git commit failed: " + e.getMessage(), e);
		}
	}

}
