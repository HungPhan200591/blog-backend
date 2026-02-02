package com.hungpc.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 500, message = "Title must be between 5 and 500 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 1000, message = "Cover image URL must not exceed 1000 characters")
    private String coverImage;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private Long seriesId;

    private List<Long> tagIds;
}
