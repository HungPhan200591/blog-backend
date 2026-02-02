package com.hungpc.blog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(PostTag.PostTagId.class)
public class PostTag {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Composite Primary Key class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostTagId implements Serializable {
        private Long postId;
        private Long tagId;
    }
}
