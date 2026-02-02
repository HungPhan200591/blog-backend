package com.hungpc.blog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesResponse {
    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private String color;
    private Integer postCount;
    private LocalDateTime createdAt;
}
