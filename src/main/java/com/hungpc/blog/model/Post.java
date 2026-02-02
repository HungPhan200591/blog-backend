package com.hungpc.blog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "cover_image", length = 1000)
    private String coverImage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "series_id")
    private Long seriesId;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "visit_count", nullable = false)
    @Builder.Default
    private Long visitCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}
