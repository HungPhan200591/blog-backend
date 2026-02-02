package com.hungpc.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSeriesRequest {
    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 500, message = "Title must be between 2 and 500 characters")
    private String title;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;

    @Size(max = 500, message = "Cover image must be less than 500 characters")
    private String coverImage;

    private String color;
}
