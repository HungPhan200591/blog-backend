package com.hungpc.blog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;
    private String slug;
    private String title;
    private String description;
    private String coverImage;
    private CategoryInfo category;
    private SeriesInfo series;
    private List<TagInfo> tags;
    private String status;
    private Long visitCount;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private LocalDateTime lastSyncedAt;

    // Additional fields for post detail page
    private String content; // Markdown content
    private Integer readingTime; // Reading time in minutes
    private AuthorInfo author; // Author information

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeriesInfo {
        private Long id;
        private String title;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagInfo {
        private Long id;
        private String name;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfo {
        private String name;
        private String avatar;
    }
}
