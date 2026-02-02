package com.hungpc.blog.service;

import com.hungpc.blog.dto.FileWithMetadata;
import com.hungpc.blog.dto.FrontmatterDTO;
import com.hungpc.blog.dto.response.SyncAllResult;
import com.hungpc.blog.dto.response.SyncResult;
import com.hungpc.blog.exception.MarkdownFileNotFoundException;
import com.hungpc.blog.exception.ResourceNotFoundException;
import com.hungpc.blog.model.Category;
import com.hungpc.blog.model.Post;
import com.hungpc.blog.model.PostTag;
import com.hungpc.blog.config.GitConfig;
import com.hungpc.blog.repository.PostRepository;
import com.hungpc.blog.repository.PostTagRepository;
import com.hungpc.blog.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import com.hungpc.blog.constant.CacheConstants;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for syncing posts from Git repository
 * Syncs both content AND metadata from frontmatter
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostSyncService {

    private final GitService gitService;
    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final GitConfig gitConfig;
    private final CategoryTagHelper categoryTagHelper;

    /**
     * Sync single post from Git repository
     * Updates BOTH content AND metadata from frontmatter
     * 
     * @param postId ID of post to sync
     * @return Sync result with details
     */
    @CacheEvict(value = { CacheConstants.POST_BY_SLUG, CacheConstants.POSTS, CacheConstants.RELATED_POSTS,
            CacheConstants.FEATURED_POSTS, CacheConstants.LATEST_POSTS }, allEntries = true)
    @Transactional
    public SyncResult syncPost(Long postId) {
        log.info("🔄 Syncing post with ID: {}", postId);

        try {
            // 1. Find post
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

            // 2. Pull latest changes
            gitService.pullLatest();

            // 3. Find markdown file and parse frontmatter
            Optional<FileWithMetadata> fileOpt = gitService.findMarkdownFileWithMetadata(post.getSlug());

            if (fileOpt.isEmpty()) {
                String expectedPath = String.format("%s%s.md", gitConfig.getContentPath(), post.getSlug());
                throw new MarkdownFileNotFoundException(post.getSlug(), expectedPath);
            }

            FileWithMetadata file = fileOpt.get();
            FrontmatterDTO frontmatter = file.getMetadata();
            String content = file.getContent(); // Content WITHOUT frontmatter

            log.info("📄 File loaded. Has frontmatter: {}", frontmatter != null);

            // 4. Update content (always)
            post.setContent(content);
            post.setLastSyncedAt(LocalDateTime.now());

            // 5. Update metadata from frontmatter (if exists)
            if (frontmatter != null) {
                // Update title
                StringUtils.ifHasText(frontmatter.getTitle(), title -> {
                    post.setTitle(title);
                    log.info("📝 Updated title from frontmatter: {}", title);
                });

                // Update category (find/create by name)
                StringUtils.ifHasText(frontmatter.getCategory(), categoryName -> {
                    Category category = categoryTagHelper.getOrCreateCategory(categoryName);
                    post.setCategoryId(category.getId());
                    log.info("📂 Updated category from frontmatter: {} (ID {})", category.getName(), category.getId());
                });

                // Update tags (find/create by names)
                StringUtils.ifHasElements(frontmatter.getTags(), tagNames -> {
                    List<Long> tagIds = categoryTagHelper.getOrCreateTags(tagNames);

                    // Delete existing post-tag relationships
                    postTagRepository.deleteByPostId(post.getId());

                    // Create new relationships
                    List<PostTag> postTags = tagIds.stream()
                            .map(tagId -> PostTag.builder()
                                    .postId(post.getId())
                                    .tagId(tagId)
                                    .build())
                            .collect(Collectors.toList());

                    postTagRepository.saveAll(postTags);
                    log.info("🏷️ Updated tags from frontmatter: {} (IDs: {})", tagNames, tagIds);
                });

                // Update description
                StringUtils.ifHasText(frontmatter.getDescription(), description -> {
                    post.setDescription(description);
                    log.info("📄 Updated description from frontmatter");
                });

                // Update cover image
                StringUtils.ifHasText(frontmatter.getCoverImage(), coverImage -> {
                    post.setCoverImage(coverImage);
                    log.info("🖼️ Updated cover image from frontmatter");
                });

                // Update published date (if exists)
                if (frontmatter.getPublishedAt() != null) {
                    post.setPublishedAt(frontmatter.getPublishedAt());
                    log.info("📅 Updated publishedAt from frontmatter: {}", frontmatter.getPublishedAt());
                }
            } else {
                log.warn("⚠️ No frontmatter found. Only content was synced.");
            }

            // 6. Save
            postRepository.save(post);

            log.info("✅ Successfully synced post: {} ({} bytes)", post.getSlug(), content.length());

            return SyncResult.builder()
                    .postId(post.getId())
                    .slug(post.getSlug())
                    .success(true)
                    .syncedAt(LocalDateTime.now())
                    .contentLength(content.length())
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to sync post with ID: {}", postId, e);
            return SyncResult.builder()
                    .postId(postId)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 
     * /**
     * Sync all posts from Git repository (batch operation)
     * Updates BOTH content AND metadata from frontmatter
     * 
     * @return Summary of sync operation
     */
    @CacheEvict(value = { CacheConstants.POST_BY_SLUG, CacheConstants.POSTS, CacheConstants.RELATED_POSTS,
            CacheConstants.FEATURED_POSTS, CacheConstants.LATEST_POSTS }, allEntries = true)
    @Transactional
    public SyncAllResult syncAllPosts() {
        log.info("🔄 Starting batch sync for all posts");

        // Pull once for all posts
        gitService.pullLatest();

        List<Post> allPosts = postRepository.findAll();

        int synced = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Post post : allPosts) {
            try {
                // Find markdown file and parse frontmatter
                Optional<FileWithMetadata> fileOpt = gitService.findMarkdownFileWithMetadata(post.getSlug());

                if (fileOpt.isPresent()) {
                    FileWithMetadata file = fileOpt.get();
                    FrontmatterDTO frontmatter = file.getMetadata();
                    String content = file.getContent();

                    // Update content (always)
                    post.setContent(content);
                    post.setLastSyncedAt(LocalDateTime.now());

                    // Update metadata from frontmatter (if exists)
                    if (frontmatter != null) {
                        // Update title
                        StringUtils.ifHasText(frontmatter.getTitle(), post::setTitle);

                        // Update category (find/create by name)
                        StringUtils.ifHasText(frontmatter.getCategory(), categoryName -> {
                            Category category = categoryTagHelper.getOrCreateCategory(categoryName);
                            post.setCategoryId(category.getId());
                        });

                        // Update tags (find/create by names)
                        StringUtils.ifHasElements(frontmatter.getTags(), tagNames -> {
                            List<Long> tagIds = categoryTagHelper.getOrCreateTags(tagNames);

                            // Delete existing post-tag relationships
                            postTagRepository.deleteByPostId(post.getId());

                            // Create new relationships
                            List<PostTag> postTags = tagIds.stream()
                                    .map(tagId -> PostTag.builder()
                                            .postId(post.getId())
                                            .tagId(tagId)
                                            .build())
                                    .collect(Collectors.toList());

                            postTagRepository.saveAll(postTags);
                        });

                        // Update description
                        StringUtils.ifHasText(frontmatter.getDescription(), post::setDescription);

                        // Update cover image
                        StringUtils.ifHasText(frontmatter.getCoverImage(), post::setCoverImage);

                        // Update published date
                        if (frontmatter.getPublishedAt() != null) {
                            post.setPublishedAt(frontmatter.getPublishedAt());
                        }
                    }

                    postRepository.save(post);

                    synced++;
                    log.debug("✅ Synced post: {} (has frontmatter: {})", post.getSlug(), frontmatter != null);
                } else {
                    log.warn("⚠️ File not found for post: {}", post.getSlug());
                    skipped++;
                }

            } catch (Exception e) {
                log.error("❌ Failed to sync post: {}", post.getSlug(), e);
                errors.add(String.format("%s: %s", post.getSlug(), e.getMessage()));
                skipped++;
            }
        }

        log.info("✅ Batch sync completed. Synced: {}, Skipped: {}, Errors: {}",
                synced, skipped, errors.size());

        return SyncAllResult.builder()
                .totalScanned(allPosts.size())
                .synced(synced)
                .skipped(skipped)
                .errors(errors)
                .syncedAt(LocalDateTime.now())
                .build();
    }
}
