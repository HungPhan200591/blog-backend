package com.hungpc.blog.service;

import com.hungpc.blog.config.GitConfig;
import com.hungpc.blog.constant.CacheConstants;
import com.hungpc.blog.dto.FileWithMetadata;
import com.hungpc.blog.dto.FrontmatterDTO;
import com.hungpc.blog.dto.request.CreatePostRequest;
import com.hungpc.blog.dto.response.PostResponse;
import com.hungpc.blog.dto.response.StatsResponse;
import com.hungpc.blog.exception.BadRequestException;
import com.hungpc.blog.exception.ConflictException;
import com.hungpc.blog.exception.ResourceNotFoundException;
import com.hungpc.blog.model.*;
import com.hungpc.blog.repository.*;
import com.hungpc.blog.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

	private final PostRepository postRepository;
	private final CategoryRepository categoryRepository;
	private final SeriesRepository seriesRepository;
	private final TagRepository tagRepository;
	private final PostTagRepository postTagRepository;
	private final GitService gitService;
	private final GitConfig gitConfig;
	private final AutoFillService autoFillService;
	private final CategoryTagHelper categoryTagHelper;

	/**
	 * Create post from Git repository (Git-First Workflow with Frontmatter Support)
	 * 
	 * Priority:
	 * 1. Request metadata (if provided)
	 * 2. Frontmatter metadata (if exists in file)
	 * 3. AI-generated metadata (fallback)
	 * 
	 * After save, writes metadata back to GitHub frontmatter for sync
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
	public PostResponse createPostFromGit(CreatePostRequest request) {
		log.info("Creating post with slug: {}", request.getSlug());

		// 1. Validate slug uniqueness
		if (postRepository.existsBySlug(request.getSlug())) {
			throw new ConflictException("Slug '" + request.getSlug() + "' already exists");
		}

		// 2. Git pull latest
		gitService.pullLatest();

		// 3. Find file in Git and parse frontmatter
		Optional<FileWithMetadata> fileOpt = gitService.findMarkdownFileWithMetadata(request.getSlug());

		if (fileOpt.isEmpty()) {
			String basePath = gitConfig.getContentPath() + request.getSlug();
			throw new BadRequestException(
					"FILE_NOT_FOUND",
					"File '" + request.getSlug() + ".md' not found in NoteRepo",
					"Expected path: " + basePath + ".md",
					"Please push the markdown file to Git first");
		}

		FileWithMetadata file = fileOpt.get();
		FrontmatterDTO frontmatter = file.getMetadata();
		String content = file.getContent(); // Content WITHOUT frontmatter

		log.info("📄 File loaded. Has frontmatter: {}", frontmatter != null);

		// 4. Resolve metadata with priority: Request > Frontmatter > AI
		String finalTitle = resolveTitle(request, frontmatter, content);
		Long categoryId = resolveCategoryId(request, frontmatter);
		List<Long> tagIds = resolveTagIds(request, frontmatter);
		String description = resolveDescription(request, frontmatter, finalTitle, content);
		String coverImage = resolveCoverImage(request, frontmatter, finalTitle);

		// 5. Validate metadata
		final Long finalCategoryId = categoryId;

		Category category = categoryRepository.findById(finalCategoryId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Category not found with id: "
								+ finalCategoryId));

		// Validate series (optional)
		Long seriesId = null;
		if (request.getSeriesId() != null) {
			Series series = seriesRepository.findById(request.getSeriesId())
					.orElseThrow(
							() -> new ResourceNotFoundException("Series not found with id: "
									+ request.getSeriesId()));
			seriesId = series.getId();
		}

		// Validate tags exist
		if (tagIds != null && !tagIds.isEmpty()) {
			List<Tag> tags = tagRepository.findAllById(tagIds);
			if (tags.size() != tagIds.size()) {
				throw new BadRequestException("One or more tag IDs are invalid");
			}
		}

		// 6. Create post with FK IDs
		Post post = Post.builder()
				.slug(request.getSlug())
				.title(finalTitle)
				.description(description)
				.coverImage(coverImage)
				.content(content) // Content WITHOUT frontmatter
				.categoryId(category.getId())
				.seriesId(seriesId)
				.isPublished(true) // Default to published, true for me
				.visitCount(0L)
				.lastSyncedAt(LocalDateTime.now())
				.build();

		post = postRepository.save(post);

		// 7. Create post-tag relationships
		if (tagIds != null && !tagIds.isEmpty()) {
			Long finalPostId = post.getId();
			List<PostTag> postTags = tagIds.stream()
					.map(tagId -> PostTag.builder()
							.postId(finalPostId)
							.tagId(tagId)
							.build())
					.collect(Collectors.toList());

			postTagRepository.saveAll(postTags);
		}

		// 8. Write metadata back to GitHub frontmatter for sync
		try {
			syncMetadataToGit(request.getSlug(), post, category, tagIds);
		} catch (Exception e) {
			log.error("❌ Failed to sync metadata to Git: {}", e.getMessage(), e);
			// Don't fail the whole operation, just log the error
		}

		log.info("✅ Post created successfully with id: {}", post.getId());
		return toResponse(post);
	}

	/**
	 * Resolve title with priority: Request > Frontmatter > Markdown H1
	 */
	private String resolveTitle(CreatePostRequest request, FrontmatterDTO frontmatter, String content) {
		// Priority 1: Request
		String requestTitle = request.getTitle();
		if (StringUtils.hasText(requestTitle)) {
			log.info("📝 Using title from request: {}", requestTitle);
			return requestTitle;
		}

		// Priority 2: Frontmatter
		if (frontmatter != null) {
			String frontmatterTitle = frontmatter.getTitle();
			if (StringUtils.hasText(frontmatterTitle)) {
				log.info("📝 Using title from frontmatter: {}", frontmatterTitle);
				return frontmatterTitle;
			}
		}

		// Priority 3: Extract from markdown
		String extracted = autoFillService.extractTitleFromMarkdown(content);
		if (StringUtils.hasText(extracted)) {
			log.info("📝 Extracted title from markdown: {}", extracted);
			return extracted;
		}

		throw new BadRequestException("Could not resolve title. Please provide title in request or frontmatter.");
	}

	/**
	 * Resolve category ID with priority: Request > Frontmatter (by name) > AI
	 */
	private Long resolveCategoryId(CreatePostRequest request, FrontmatterDTO frontmatter) {
		// Priority 1: Request
		if (request.getCategoryId() != null) {
			log.info("📂 Using category from request: ID {}", request.getCategoryId());
			return request.getCategoryId();
		}

		// Priority 2: Frontmatter (find/create by name)
		if (frontmatter != null) {
			String categoryName = frontmatter.getCategory();
			if (StringUtils.hasText(categoryName)) {
				Category category = categoryTagHelper.getOrCreateCategory(categoryName);
				log.info("📂 Using category from frontmatter: {} (ID {})", category.getName(), category.getId());
				return category.getId();
			}
		}

		// Priority 3: Will be filled by AI in next step
		return null;
	}

	/**
	 * Resolve tag IDs with priority: Request > Frontmatter (by names) > AI
	 */
	private List<Long> resolveTagIds(CreatePostRequest request, FrontmatterDTO frontmatter) {
		// Priority 1: Request
		List<Long> requestTagIds = request.getTagIds();
		if (StringUtils.hasElements(requestTagIds)) {
			log.info("🏷️ Using tags from request: {} tags", requestTagIds.size());
			return requestTagIds;
		}

		// Priority 2: Frontmatter (find/create by names)
		if (frontmatter != null) {
			List<String> tagNames = frontmatter.getTags();
			if (StringUtils.hasElements(tagNames)) {
				List<Long> tagIds = categoryTagHelper.getOrCreateTags(tagNames);
				log.info("🏷️ Using tags from frontmatter: {} (IDs: {})", tagNames, tagIds);
				return tagIds;
			}
		}

		// Priority 3: Will be filled by AI in next step
		return null;
	}

	/**
	 * Resolve description with priority: Request > Frontmatter > AI
	 */
	private String resolveDescription(CreatePostRequest request, FrontmatterDTO frontmatter, String title,
			String content) {
		// Priority 1: Request
		String requestDesc = request.getDescription();
		if (StringUtils.hasText(requestDesc)) {
			log.info("📄 Using description from request");
			return requestDesc;
		}

		// Priority 2: Frontmatter
		if (frontmatter != null) {
			String frontmatterDesc = frontmatter.getDescription();
			if (StringUtils.hasText(frontmatterDesc)) {
				log.info("📄 Using description from frontmatter");
				return frontmatterDesc;
			}
		}

		// Priority 3: Will be filled by AI in next step
		return null;
	}

	/**
	 * Resolve cover image with priority: Request > Frontmatter > Pexels
	 */
	private String resolveCoverImage(CreatePostRequest request, FrontmatterDTO frontmatter, String title) {
		// Priority 1: Request
		String requestCover = request.getCoverImage();
		if (StringUtils.hasText(requestCover)) {
			log.info("🖼️ Using cover image from request");
			return requestCover;
		}

		// Priority 2: Frontmatter
		if (frontmatter != null) {
			String frontmatterCover = frontmatter.getCoverImage();
			if (StringUtils.hasText(frontmatterCover)) {
				log.info("🖼️ Using cover image from frontmatter");
				return frontmatterCover;
			}
		}

		// Priority 3: Generate via Pexels
		String generated = autoFillService.generateCoverImage(title);
		log.info("🖼️ Generated cover image via Pexels");
		return generated;
	}

	/**
	 * Write metadata back to GitHub frontmatter for sync
	 */
	private void syncMetadataToGit(String slug, Post post, Category category, List<Long> tagIds)
			throws IOException, GitAPIException {
		log.info("🔄 Syncing metadata to Git for slug: {}", slug);

		// Build frontmatter from DB data
		List<String> tagNames = tagIds != null && !tagIds.isEmpty()
				? tagRepository.findAllById(tagIds).stream()
						.map(Tag::getName)
						.collect(Collectors.toList())
				: Collections.emptyList();

		FrontmatterDTO frontmatter = FrontmatterDTO.builder()
				.title(post.getTitle())
				.category(category.getName())
				.tags(tagNames)
				.description(post.getDescription())
				.coverImage(post.getCoverImage() != null ? post.getCoverImage() : "")
				.publishedAt(post.getPublishedAt())
				.build();

		// Read current file
		String basePath = gitConfig.getContentPath() + slug;
		Optional<String> currentContent = gitService.readFile(basePath + ".md");
		if (currentContent.isEmpty()) {
			currentContent = gitService.readFile(basePath + ".mdx");
		}

		if (currentContent.isEmpty()) {
			log.warn("⚠️ File not found for sync: {}", slug);
			return;
		}

		// Replace frontmatter
		String updatedContent = FrontmatterParser.replaceFrontmatter(currentContent.get(), frontmatter);

		// Write back to Git
		String extension = gitService.fileExists(basePath + ".md") ? ".md" : ".mdx";
		gitService.writeFile(basePath + extension, updatedContent);

		// Commit and push
		String commitMessage = String.format("Update frontmatter: %s", post.getTitle());
		gitService.commitAndPush(commitMessage);

		log.info("✅ Metadata synced to Git successfully");
	}

	/**
	 * Overloaded version: Sync metadata to Git (fetch category and tags from DB)
	 */
	private void syncMetadataToGit(Post post) throws IOException, GitAPIException {
		// Fetch category
		Category category = categoryRepository.findById(post.getCategoryId())
				.orElseThrow(
						() -> new ResourceNotFoundException("Category not found with id: " + post.getCategoryId()));

		// Fetch tag IDs
		List<Long> tagIds = postTagRepository.findTagIdsByPostId(post.getId());

		// Call original method
		syncMetadataToGit(post.getSlug(), post, category, tagIds);
	}

	/**
	 * Get all posts with filters
	 * <p>
	 * ✅ OPTIMIZED: Batch loading to prevent N+1 queries
	 * - Before: 1 + (N * 3) queries (category, series, tags per post)
	 * - After: 1 + 4 queries total (batch load all at once)
	 */
	@Cacheable(value = CacheConstants.POSTS, key = "#page + '-' + #size + '-' + #sort + '-' + #search + '-' + #category + '-' + #series + '-' + #status + '-' + #tags")
	public Page<PostResponse> getAllPosts(
			int page,
			int size,
			String sort,
			String search,
			String category,
			String series,
			String status,
			String tags) {
		long startTime = System.currentTimeMillis();

		// Parse sort parameter (e.g., "createdAt,desc")
		String[] sortParts = sort.split(",");
		String sortField = sortParts[0];
		Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
				? Sort.Direction.DESC
				: Sort.Direction.ASC;

		// Convert camelCase to snake_case for native query
		String dbColumnName = NamingUtils.camelToSnake(sortField);

		Pageable pageable = PageRequest.of(page, size, Sort.by(direction, dbColumnName));

		// Parse tags (comma-separated) to String array for PostgreSQL
		String[] tagArray = new String[0];
		int tagCount = 0;
		if (tags != null && !tags.trim().isEmpty()) {
			tagArray = tags.split(",");
			tagCount = tagArray.length;
		}

		// 1️⃣ Load posts (1 query)
		Page<Post> posts = postRepository.findAllWithFilters(search, category, series, status, tags, tagArray,
				tagCount, pageable);

		if (posts.isEmpty()) {
			log.info("✅ Query completed in {}ms (no results)", System.currentTimeMillis() - startTime);
			return Page.empty(pageable);
		}

		// 2️⃣ Batch load all related data (4 queries total instead of N*3)
		List<PostResponse> responses = toResponseBatch(posts.getContent());
		Page<PostResponse> result = new PageImpl<>(responses, pageable, posts.getTotalElements());

		long duration = System.currentTimeMillis() - startTime;
		log.info("✅ Query completed in {}ms (returned {} posts with ~5 total queries)",
				duration, posts.getNumberOfElements());

		return result;
	}

	/**
	 * Get post by ID
	 */
	public PostResponse getPostById(Long id) {
		Post post = postRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
		return toResponse(post);
	}

	/**
	 * Update post metadata (title, description, coverImage, category, series, tags)
	 * Note: slug and content are immutable (content only syncs from Git)
	 */
	@Transactional
	@CacheEvict(value = { CacheConstants.POSTS, CacheConstants.POST_BY_SLUG, CacheConstants.RELATED_POSTS,
			CacheConstants.FEATURED_POSTS, CacheConstants.LATEST_POSTS }, allEntries = true)
	public PostResponse updatePost(Long id, com.hungpc.blog.dto.request.UpdatePostRequest request) {
		log.info("Updating post id: {} with title: {}", id, request.getTitle());

		// 1. Find existing post
		Post post = postRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

		// 2. Validate category exists
		Category category = categoryRepository.findById(request.getCategoryId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Category not found with id: " + request.getCategoryId()));

		// 3. Validate series (optional)
		Long seriesId = null;
		if (request.getSeriesId() != null) {
			Series series = seriesRepository.findById(request.getSeriesId())
					.orElseThrow(() -> new ResourceNotFoundException(
							"Series not found with id: " + request.getSeriesId()));
			seriesId = series.getId();
		}

		// 4. Validate tags exist
		List<Long> tagIds = request.getTagIds() != null ? request.getTagIds() : List.of();
		if (!tagIds.isEmpty()) {
			List<Tag> tags = tagRepository.findAllById(tagIds);
			if (tags.size() != tagIds.size()) {
				throw new BadRequestException("One or more tag IDs are invalid");
			}
		}

		// 5. Update post metadata
		post.setTitle(request.getTitle());
		post.setDescription(request.getDescription());
		post.setCoverImage(request.getCoverImage());
		post.setCategoryId(category.getId());
		post.setSeriesId(seriesId);

		post = postRepository.save(post);

		// 6. Update post-tag relationships
		// Delete existing relationships
		postTagRepository.deleteByPostId(id);

		// Create new relationships
		if (!tagIds.isEmpty()) {
			Long finalPostId = post.getId();
			List<PostTag> postTags = tagIds.stream()
					.map(tagId -> PostTag.builder()
							.postId(finalPostId)
							.tagId(tagId)
							.build())
					.collect(Collectors.toList());

			postTagRepository.saveAll(postTags);
		}

		log.info("✅ Post updated successfully: {}", post.getSlug());

		// 7. Sync metadata back to Git (2-way sync)
		try {
			syncMetadataToGit(post);
		} catch (Exception e) {
			log.error("⚠️ Failed to sync metadata to Git after update: {}", e.getMessage(), e);
			// Don't fail the update if Git sync fails
		}

		return toResponse(post);
	}

	/**
	 * Publish a draft post
	 */
	@Transactional
	@CacheEvict(value = { CacheConstants.POSTS, CacheConstants.POST_BY_SLUG, CacheConstants.RELATED_POSTS,
			CacheConstants.FEATURED_POSTS, CacheConstants.LATEST_POSTS }, allEntries = true)
	public PostResponse publishPost(Long id) {
		log.info("🚀 Publishing post id: {} - Will evict ALL caches", id);
		Post post = postRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

		if (Boolean.TRUE.equals(post.getIsPublished())) {
			throw new BadRequestException("Post is already published");
		}

		post.setIsPublished(true);
		post.setPublishedAt(LocalDateTime.now());
		post = postRepository.save(post);

		log.info("✅ Post published: {} - Caches evicted: posts, postBySlug, relatedPosts, featuredPosts, latestPosts",
				post.getSlug());
		return toResponse(post);
	}

	/**
	 * Unpublish a published post (convert to draft)
	 */
	@Transactional
	@CacheEvict(value = { CacheConstants.POSTS, CacheConstants.POST_BY_SLUG, CacheConstants.RELATED_POSTS,
			CacheConstants.FEATURED_POSTS, CacheConstants.LATEST_POSTS }, allEntries = true)
	public PostResponse unpublishPost(Long id) {
		log.info("🔒 Unpublishing post id: {} - Will evict ALL caches", id);
		Post post = postRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

		if (!Boolean.TRUE.equals(post.getIsPublished())) {
			throw new BadRequestException("Post is already a draft");
		}

		post.setIsPublished(false);
		post.setPublishedAt(null); // Clear published date
		post = postRepository.save(post);

		log.info("✅ Post unpublished: {} - Caches evicted: posts, postBySlug, relatedPosts, featuredPosts, latestPosts",
				post.getSlug());
		return toResponse(post);
	}

	/**
	 * Delete post
	 */
	@Transactional
	@CacheEvict(value = { CacheConstants.POSTS, CacheConstants.POST_BY_SLUG, CacheConstants.RELATED_POSTS,
			CacheConstants.FEATURED_POSTS, CacheConstants.LATEST_POSTS }, allEntries = true)
	public void deletePost(Long id) {
		Post post = postRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

		// Delete post-tag relationships first
		postTagRepository.deleteByPostId(id);

		// Delete post
		postRepository.delete(post);
		log.info("Post deleted: {}", post.getSlug());
	}

	/**
	 * ✅ BATCH LOADING: Convert multiple posts to DTOs with minimal queries
	 * <p>
	 * Performance:
	 * - 1 query: Load all unique categories
	 * - 1 query: Load all unique series
	 * - 1 query: Load all post-tag relationships
	 * - 1 query: Load all unique tags
	 * Total: 4 queries for ANY number of posts (vs N*3 queries before)
	 */
	private List<PostResponse> toResponseBatch(List<Post> posts) {
		if (posts.isEmpty()) {
			return Collections.emptyList();
		}

		// Collect all unique IDs
		Set<Long> categoryIds = posts.stream()
				.map(Post::getCategoryId)
				.collect(Collectors.toSet());

		Set<Long> seriesIds = posts.stream()
				.map(Post::getSeriesId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<Long> postIds = posts.stream()
				.map(Post::getId)
				.toList();

		// 1️⃣ Batch load categories (1 query)
		Map<Long, Category> categoriesMap = categoryRepository.findAllById(categoryIds).stream()
				.collect(Collectors.toMap(Category::getId, c -> c));

		// 2️⃣ Batch load series (1 query)
		Map<Long, Series> seriesMap = seriesIds.isEmpty()
				? Collections.emptyMap()
				: seriesRepository.findAllById(seriesIds).stream()
						.collect(Collectors.toMap(Series::getId, s -> s));

		// 3️⃣ Batch load post-tags relationships (1 query)
		List<PostTagRepository.PostTagProjection> postTagProjections = postTagRepository
				.findTagIdsByPostIds(postIds);

		// Group by postId: Map<PostId, List<TagId>>
		Map<Long, List<Long>> postTagsMap = postTagProjections.stream()
				.collect(Collectors.groupingBy(
						PostTagRepository.PostTagProjection::getPostId,
						Collectors.mapping(PostTagRepository.PostTagProjection::getTagId,
								Collectors.toList())));

		// 4️⃣ Batch load all tags (1 query)
		Set<Long> allTagIds = postTagProjections.stream()
				.map(PostTagRepository.PostTagProjection::getTagId)
				.collect(Collectors.toSet());

		Map<Long, Tag> tagsMap = allTagIds.isEmpty()
				? Collections.emptyMap()
				: tagRepository.findAllById(allTagIds).stream()
						.collect(Collectors.toMap(Tag::getId, t -> t));

		// 5️⃣ Map to DTOs using pre-loaded data (NO additional queries)
		return posts.stream()
				.map(post -> toResponseOptimized(post, categoriesMap, seriesMap, postTagsMap, tagsMap))
				.collect(Collectors.toList());
	}

	/**
	 * ✅ OPTIMIZED: Convert Post to DTO using pre-loaded maps (NO queries)
	 */
	private PostResponse toResponseOptimized(
			Post post,
			Map<Long, Category> categoriesMap,
			Map<Long, Series> seriesMap,
			Map<Long, List<Long>> postTagsMap,
			Map<Long, Tag> tagsMap) {

		// Get category from map (no query)
		PostResponse.CategoryInfo categoryInfo = Optional.ofNullable(categoriesMap.get(post.getCategoryId()))
				.map(c -> PostResponse.CategoryInfo.builder()
						.id(c.getId())
						.name(c.getName())
						.color(c.getColor())
						.build())
				.orElse(null);

		// Get series from map (no query)
		PostResponse.SeriesInfo seriesInfo = null;
		if (post.getSeriesId() != null) {
			seriesInfo = Optional.ofNullable(seriesMap.get(post.getSeriesId()))
					.map(s -> PostResponse.SeriesInfo.builder()
							.id(s.getId())
							.title(s.getTitle())
							.color(s.getColor())
							.build())
					.orElse(null);
		}

		// Get tags from map (no query)
		List<Long> tagIds = postTagsMap.getOrDefault(post.getId(), Collections.emptyList());
		List<PostResponse.TagInfo> tagInfos = tagIds.stream()
				.map(tagsMap::get)
				.filter(Objects::nonNull)
				.map(t -> PostResponse.TagInfo.builder()
						.id(t.getId())
						.name(t.getName())
						.color(t.getColor())
						.build())
				.collect(Collectors.toList());

		return PostResponse.builder()
				.id(post.getId())
				.slug(post.getSlug())
				.title(post.getTitle())
				.description(post.getDescription())
				.coverImage(post.getCoverImage())
				.category(categoryInfo)
				.series(seriesInfo)
				.tags(tagInfos)
				.status(post.getIsPublished() ? "PUBLISHED" : "DRAFT")
				.visitCount(post.getVisitCount())
				.createdAt(post.getCreatedAt())
				.publishedAt(post.getPublishedAt())
				.lastSyncedAt(post.getLastSyncedAt())
				.build();
	}

	/**
	 * ⚠️ LEGACY: Convert Post entity to PostResponse DTO (N+1 queries)
	 * Used only for single post operations (getPostById, createPost, publishPost)
	 */
	private PostResponse toResponse(Post post) {
		// Get category info with color
		PostResponse.CategoryInfo categoryInfo = categoryRepository.findById(post.getCategoryId())
				.map(c -> PostResponse.CategoryInfo.builder()
						.id(c.getId())
						.name(c.getName())
						.color(c.getColor())
						.build())
				.orElse(null);

		// Get series info with color
		PostResponse.SeriesInfo seriesInfo = null;
		if (post.getSeriesId() != null) {
			seriesInfo = seriesRepository.findById(post.getSeriesId())
					.map(s -> PostResponse.SeriesInfo.builder()
							.id(s.getId())
							.title(s.getTitle())
							.color(s.getColor())
							.build())
					.orElse(null);
		}

		// Get tags with color via PostTag junction table
		List<Long> tagIds = postTagRepository.findTagIdsByPostId(post.getId());
		List<PostResponse.TagInfo> tagInfos = tagRepository.findAllById(tagIds).stream()
				.map(t -> PostResponse.TagInfo.builder()
						.id(t.getId())
						.name(t.getName())
						.color(t.getColor())
						.build())
				.collect(Collectors.toList());

		return PostResponse.builder()
				.id(post.getId())
				.slug(post.getSlug())
				.title(post.getTitle())
				.description(post.getDescription())
				.coverImage(post.getCoverImage())
				.category(categoryInfo)
				.series(seriesInfo)
				.tags(tagInfos)
				.status(post.getIsPublished() ? "PUBLISHED" : "DRAFT")
				.visitCount(post.getVisitCount())
				.createdAt(post.getCreatedAt())
				.publishedAt(post.getPublishedAt())
				.lastSyncedAt(post.getLastSyncedAt())
				.build();
	}

	/**
	 * Get dashboard statistics
	 */
	public StatsResponse getStats() {
		long totalPosts = postRepository.count();
		long published = postRepository.countByIsPublished(true);
		long drafts = postRepository.countByIsPublished(false);

		return StatsResponse.builder()
				.totalPosts(totalPosts)
				.published(published)
				.drafts(drafts)
				.build();
	}

	/**
	 * Get post by slug (public API)
	 * Returns full post content including markdown
	 */
	@Cacheable(value = CacheConstants.POST_BY_SLUG, key = "#slug")
	@Transactional(readOnly = true)
	public PostResponse getPostBySlug(String slug) {
		log.info("📖 Getting post by slug: {} (checking cache...)", slug);

		Post post = postRepository.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with slug: " + slug));

		// Only return published posts for public API
		if (!post.getIsPublished()) {
			throw new ResourceNotFoundException("Post not found with slug: " + slug);
		}

		// Strip YAML frontmatter before returning to frontend
		String contentWithoutFrontmatter = MarkdownUtils.removeFrontmatter(post.getContent());

		// Calculate reading time (words / 200 words per minute)
		int wordCount = contentWithoutFrontmatter.split("\\s+").length;
		int readingTime = Math.max(1, (int) Math.ceil(wordCount / 200.0));

		// Build response with full content
		PostResponse response = toResponse(post);

		// Set additional fields for post detail
		response.setContent(contentWithoutFrontmatter);
		response.setReadingTime(readingTime);
		response.setAuthor(PostResponse.AuthorInfo.builder()
				.name("Hung Phan") // TODO: Get from user/author table
				.avatar(null)
				.build());

		return response;
	}

	/**
	 * Get post by slug for admin preview (includes drafts)
	 * Returns full post content including markdown, regardless of published status
	 */
	@Cacheable(value = CacheConstants.POST_BY_SLUG, key = "#slug")
	@Transactional(readOnly = true)
	public PostResponse getPostBySlugForPreview(String slug) {
		log.info("👁️ Admin preview: Getting post by slug: {}", slug);

		Post post = postRepository.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with slug: " + slug));

		// ✅ NO published check - admin can preview drafts

		// Strip YAML frontmatter before returning to frontend
		String contentWithoutFrontmatter = MarkdownUtils.removeFrontmatter(post.getContent());

		// Calculate reading time (words / 200 words per minute)
		int wordCount = contentWithoutFrontmatter.split("\\s+").length;
		int readingTime = Math.max(1, (int) Math.ceil(wordCount / 200.0));

		// Build response with full content
		PostResponse response = toResponse(post);

		// Set additional fields for post detail
		response.setContent(contentWithoutFrontmatter);
		response.setReadingTime(readingTime);
		response.setAuthor(PostResponse.AuthorInfo.builder()
				.name("Hung Phan") // TODO: Get from user/author table
				.avatar(null)
				.build());

		return response;
	}

	/**
	 * Increment visit count for a post (NOT cached)
	 * This method is called separately to ensure visit count increments
	 * even when post content is served from cache
	 */
	@Transactional
	public void incrementVisitCount(String slug) {
		Post post = postRepository.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with slug: " + slug));

		post.setVisitCount(post.getVisitCount() + 1);
		postRepository.save(post);

		log.info("Visit count incremented for post: {} (new count: {})", slug, post.getVisitCount());
	}

	/**
	 * Get related posts by category and tags
	 */
	@Cacheable(value = CacheConstants.RELATED_POSTS, key = "#slug + '-' + #limit")
	@Transactional(readOnly = true)
	public List<PostResponse> getRelatedPosts(String slug, int limit) {
		log.info("🔗 Getting related posts for slug: {}, limit: {} (checking cache...)", slug, limit);

		Post currentPost = postRepository.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Post not found with slug: " + slug));

		// Get posts from same category, excluding current post
		List<Post> relatedPosts = postRepository
				.findTop10ByCategoryIdAndIsPublishedAndIdNotOrderByCreatedAtDesc(
						currentPost.getCategoryId(),
						true,
						currentPost.getId());

		// Limit results
		return relatedPosts.stream()
				.limit(limit)
				.map(this::toResponse)
				.collect(Collectors.toList());
	}
}
