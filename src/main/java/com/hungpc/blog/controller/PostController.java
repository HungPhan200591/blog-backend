package com.hungpc.blog.controller;

import com.hungpc.blog.dto.request.CreatePostRequest;
import com.hungpc.blog.dto.request.CreateFromUrlRequest;
import com.hungpc.blog.dto.request.UpdatePostRequest;
import com.hungpc.blog.dto.response.BaseResponse;
import com.hungpc.blog.dto.response.PostResponse;
import com.hungpc.blog.dto.response.CreateFromUrlResponse;
import com.hungpc.blog.service.PostService;
import com.hungpc.blog.service.PostFromUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    private final PostFromUrlService postFromUrlService;

    /**
     * Get all posts with filters
     * GET
     * /api/v1/admin/posts?page=0&size=10&sort=createdAt,desc&search=...&category=...&series=...&status=...&tags=...
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<PostResponse>>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String series,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tags) {
        log.info("GET /api/v1/admin/posts - page: {}, size: {}, sort: {}, tags: {}", page, size, sort, tags);
        Page<PostResponse> posts = postService.getAllPosts(page, size, sort, search, category, series, status, tags);
        return ResponseEntity.ok(BaseResponse.success(posts));
    }

    /**
     * Create new post (Git-first workflow)
     * POST /api/v1/admin/posts
     */
    @PostMapping
    public ResponseEntity<BaseResponse<PostResponse>> createPost(@RequestBody @Valid CreatePostRequest request) {
        log.info("POST /api/v1/admin/posts - slug: {}", request.getSlug());
        PostResponse post = postService.createPostFromGit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success("Post created successfully", post));
    }

    /**
     * Get post by ID
     * GET /api/v1/admin/posts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<PostResponse>> getPost(@PathVariable Long id) {
        log.info("GET /api/v1/admin/posts/{}", id);
        PostResponse post = postService.getPostById(id);
        return ResponseEntity.ok(BaseResponse.success(post));
    }

    /**
     * Update post metadata
     * PUT /api/v1/admin/posts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<PostResponse>> updatePost(
            @PathVariable Long id,
            @RequestBody @Valid UpdatePostRequest request) {
        log.info("PUT /api/v1/admin/posts/{} - title: {}", id, request.getTitle());
        PostResponse post = postService.updatePost(id, request);
        return ResponseEntity.ok(BaseResponse.success("Post updated successfully", post));
    }

    /**
     * Publish a draft post
     * POST /api/v1/admin/posts/{id}/publish
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<BaseResponse<PostResponse>> publishPost(@PathVariable Long id) {
        log.info("POST /api/v1/admin/posts/{}/publish", id);
        PostResponse post = postService.publishPost(id);
        return ResponseEntity.ok(BaseResponse.success("Post published successfully", post));
    }

    /**
     * Unpublish a published post (convert to draft)
     * POST /api/v1/admin/posts/{id}/unpublish
     */
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<BaseResponse<PostResponse>> unpublishPost(@PathVariable Long id) {
        log.info("POST /api/v1/admin/posts/{}/unpublish", id);
        PostResponse post = postService.unpublishPost(id);
        return ResponseEntity.ok(BaseResponse.success("Post unpublished successfully", post));
    }

    /**
     * Delete post
     * DELETE /api/v1/admin/posts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<String>> deletePost(@PathVariable Long id) {
        log.info("DELETE /api/v1/admin/posts/{}", id);
        postService.deletePost(id);
        return ResponseEntity.ok(BaseResponse.success("Post deleted successfully", null));
    }

    /**
     * Preview post by slug (admin only - includes drafts)
     * GET /api/v1/admin/posts/preview/{slug}
     */
    @GetMapping("/preview/{slug}")
    public ResponseEntity<BaseResponse<PostResponse>> previewPost(@PathVariable String slug) {
        log.info("👁️ GET /api/v1/admin/posts/preview/{} - Admin preview", slug);
        PostResponse post = postService.getPostBySlugForPreview(slug);
        return ResponseEntity.ok(BaseResponse.success("Post preview retrieved successfully", post));
    }

    /**
     * Create a post from URL
     * POST /api/v1/admin/posts/from-url
     * 
     * @param request The create from URL request
     * @return BaseResponse with CreateFromUrlResponse
     */
    @PostMapping("/from-url")
    public ResponseEntity<BaseResponse<CreateFromUrlResponse>> createFromUrl(
            @Valid @RequestBody CreateFromUrlRequest request) {
        log.info("📥 Received create-from-URL request: {}", request.getUrl());

        try {
            CreateFromUrlResponse response = postFromUrlService.createPostFromUrl(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(BaseResponse.success(
                            "Post created successfully from URL",
                            response));

        } catch (IllegalArgumentException e) {
            log.warn("❌ Invalid request: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(BaseResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("❌ Failed to create post from URL", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to create post: " + e.getMessage()));
        }
    }

}
