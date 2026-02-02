package com.hungpc.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {

    @Size(max = 500, message = "Title must be max 500 characters")
    private String title;

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Size(min = 3, max = 255, message = "Slug must be between 3 and 255 characters")
    private String slug;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 1000, message = "Cover image URL must not exceed 1000 characters")
    private String coverImage;

    private Long categoryId;

    private Long seriesId;

    private List<Long> tagIds;
}
