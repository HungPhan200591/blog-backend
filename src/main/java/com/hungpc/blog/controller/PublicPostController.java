package com.hungpc.blog.controller;

import com.hungpc.blog.dto.response.BaseResponse;
import com.hungpc.blog.dto.response.PostResponse;
import com.hungpc.blog.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public API Controller - No authentication required
 * Endpoints for public website (blog readers)
 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:8998", "http://localhost:3000" }) // Allow public access
public class PublicPostController {

    private final PostService postService;

    /**
     * Get published posts (public API)
     * GET
     * /api/v1/posts?page=0&size=10&sort=createdAt,desc&category=...&tags=...&search=...
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<PostResponse>>> getPublishedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String series,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String search) {
        log.info("GET /api/v1/posts - page: {}, size: {}, sort: {}", page, size, sort);

        // Only return PUBLISHED posts for public API
        Page<PostResponse> posts = postService.getAllPosts(
                page,
                size,
                sort,
                search,
                category,
                series,
                "PUBLISHED", // Force PUBLISHED status
                tags);

        return ResponseEntity.ok(BaseResponse.success("Posts retrieved successfully", posts));
    }

    /**
     * Get post by slug (public API)
     * GET /api/v1/posts/{slug}
     */
    @GetMapping("/{slug}")
    public ResponseEntity<BaseResponse<PostResponse>> getPostBySlug(@PathVariable String slug) {
        log.info("GET /api/v1/posts/{} - Getting post detail", slug);

        PostResponse post = postService.getPostBySlug(slug);

        // Increment visit count separately (not cached) to ensure it increments
        // even when post content is served from cache
        postService.incrementVisitCount(slug);

        return ResponseEntity.ok(BaseResponse.success("Post retrieved successfully", post));
    }

    /**
     * Get related posts by slug (public API)
     * GET /api/v1/posts/{slug}/related?limit=3
     */
    @GetMapping("/{slug}/related")
    public ResponseEntity<BaseResponse<java.util.List<PostResponse>>> getRelatedPosts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "3") int limit) {
        log.info("GET /api/v1/posts/{}/related - limit: {}", slug, limit);

        java.util.List<PostResponse> relatedPosts = postService.getRelatedPosts(slug, limit);

        return ResponseEntity.ok(BaseResponse.success("Related posts retrieved successfully", relatedPosts));
    }
}
