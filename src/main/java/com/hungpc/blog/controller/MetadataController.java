package com.hungpc.blog.controller;

import com.hungpc.blog.dto.request.*;
import com.hungpc.blog.dto.response.BaseResponse;
import com.hungpc.blog.dto.response.CategoryResponse;
import com.hungpc.blog.dto.response.SeriesResponse;
import com.hungpc.blog.dto.response.TagResponse;
import com.hungpc.blog.service.CategoryService;
import com.hungpc.blog.service.SeriesService;
import com.hungpc.blog.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class MetadataController {

    private final CategoryService categoryService;
    private final SeriesService seriesService;
    private final TagService tagService;

    // --- Categories ---
    @GetMapping("/categories")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(BaseResponse.success(categoryService.getAllCategories()));
    }

    @PostMapping("/categories")
    public ResponseEntity<BaseResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return new ResponseEntity<>(BaseResponse.success("Category created successfully", categoryService.createCategory(request)), HttpStatus.CREATED);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(BaseResponse.success("Category deleted successfully", null));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<BaseResponse<CategoryResponse>> updateCategory(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Category updated successfully", categoryService.updateCategory(id, request)));
    }

    // --- Series ---
    @GetMapping("/series")
    public ResponseEntity<BaseResponse<List<SeriesResponse>>> getSeries() {
        return ResponseEntity.ok(BaseResponse.success(seriesService.getAllSeries()));
    }

    @PostMapping("/series")
    public ResponseEntity<BaseResponse<SeriesResponse>> createSeries(@Valid @RequestBody CreateSeriesRequest request) {
        return new ResponseEntity<>(BaseResponse.success("Series created successfully", seriesService.createSeries(request)), HttpStatus.CREATED);
    }

    @DeleteMapping("/series/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteSeries(@PathVariable Long id) {
        seriesService.deleteSeries(id);
        return ResponseEntity.ok(BaseResponse.success("Series deleted successfully", null));
    }

    @PutMapping("/series/{id}")
    public ResponseEntity<BaseResponse<SeriesResponse>> updateSeries(@PathVariable Long id, @Valid @RequestBody UpdateSeriesRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Series updated successfully", seriesService.updateSeries(id, request)));
    }

    // --- Tags ---
    @GetMapping("/tags")
    public ResponseEntity<BaseResponse<List<TagResponse>>> getTags() {
        return ResponseEntity.ok(BaseResponse.success(tagService.getAllTags()));
    }

    @PostMapping("/tags")
    public ResponseEntity<BaseResponse<TagResponse>> createTag(@Valid @RequestBody CreateTagRequest request) {
        return new ResponseEntity<>(BaseResponse.success("Tag created successfully", tagService.createTag(request)), HttpStatus.CREATED);
    }

    @DeleteMapping("/tags/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(BaseResponse.success("Tag deleted successfully", null));
    }
    
    @PutMapping("/tags/{id}")
    public ResponseEntity<BaseResponse<TagResponse>> updateTag(@PathVariable Long id, @Valid @RequestBody UpdateTagRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Tag updated successfully", tagService.updateTag(id, request)));
    }
}
