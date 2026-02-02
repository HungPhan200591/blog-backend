package com.hungpc.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a post from URL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFromUrlRequest {

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String url;

    /**
     * Optional category ID
     * If null, will be auto-detected or set to default category
     */
    private Long categoryId;

    /**
     * Optional series ID
     * If null, post will not belong to any series
     */
    private Long seriesId;

    /**
     * Optional tags
     * If null or empty, will be auto-extracted from content
     */
    private List<String> tags;
}
