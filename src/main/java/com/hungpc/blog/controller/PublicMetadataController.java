package com.hungpc.blog.controller;

import com.hungpc.blog.dto.response.BaseResponse;
import com.hungpc.blog.dto.response.CategoryResponse;
import com.hungpc.blog.dto.response.SeriesResponse;
import com.hungpc.blog.dto.response.TagResponse;
import com.hungpc.blog.service.CategoryService;
import com.hungpc.blog.service.SeriesService;
import com.hungpc.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public Metadata API Controller - No authentication required
 * Endpoints for public website to fetch categories, series, tags
 */
@RestController
@RequestMapping("/api/v1/meta")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:8998", "http://localhost:3000" }) // Allow public access
public class PublicMetadataController {

    private final CategoryService categoryService;
    private final SeriesService seriesService;
    private final TagService tagService;

    /**
     * Get all categories (public API)
     * GET /api/v1/meta/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getCategories() {
        log.info("GET /api/v1/meta/categories");
        return ResponseEntity.ok(BaseResponse.success(categoryService.getAllCategories()));
    }

    /**
     * Get all series (public API)
     * GET /api/v1/meta/series
     */
    @GetMapping("/series")
    public ResponseEntity<BaseResponse<List<SeriesResponse>>> getSeries() {
        log.info("GET /api/v1/meta/series");
        return ResponseEntity.ok(BaseResponse.success(seriesService.getAllSeries()));
    }

    /**
     * Get all tags (public API)
     * GET /api/v1/meta/tags?sort=popular
     * GET /api/v1/meta/tags?sort=alphabetical
     */
    @GetMapping("/tags")
    public ResponseEntity<BaseResponse<List<TagResponse>>> getTags(
            @RequestParam(required = false) String sort) {
        log.info("GET /api/v1/meta/tags - sort: {}", sort);
        return ResponseEntity.ok(BaseResponse.success(tagService.getAllTags(sort)));
    }
}
