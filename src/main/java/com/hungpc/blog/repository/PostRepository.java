package com.hungpc.blog.repository;

import com.hungpc.blog.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

        Optional<Post> findBySlug(String slug);

        boolean existsBySlug(String slug);

        @Query(value = "SELECT DISTINCT p.id, p.slug, p.title, p.description, p.cover_image, " +
                        "p.content, p.category_id, p.series_id, p.is_published, p.visit_count, " +
                        "p.created_at, p.updated_at, p.published_at, p.last_synced_at " +
                        "FROM posts p " +
                        "LEFT JOIN categories c ON p.category_id = c.id " +
                        "LEFT JOIN series s ON p.series_id = s.id " +
                        "WHERE " +
                        "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
                        +
                        "(:category IS NULL OR LOWER(c.name) = LOWER(:category)) AND " +
                        "(:series IS NULL OR LOWER(s.title) = LOWER(:series)) AND " +
                        "(:status IS NULL OR " +
                        "  (:status = 'PUBLISHED' AND p.is_published = true) OR " +
                        "  (:status = 'DRAFT' AND p.is_published = false)) AND " +
                        "(:tags IS NULL OR p.id IN (" +
                        "  SELECT pt.post_id FROM post_tags pt " +
                        "  JOIN tags t ON pt.tag_id = t.id " +
                        "  WHERE t.name = ANY(CAST(:tagList AS text[])) " +
                        "  GROUP BY pt.post_id " +
                        "  HAVING COUNT(DISTINCT t.name) = :tagCount" +
                        "))", countQuery = "SELECT COUNT(DISTINCT p.id) FROM posts p " +
                                        "LEFT JOIN categories c ON p.category_id = c.id " +
                                        "LEFT JOIN series s ON p.series_id = s.id " +
                                        "WHERE " +
                                        "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
                                        +
                                        "(:category IS NULL OR LOWER(c.name) = LOWER(:category)) AND " +
                                        "(:series IS NULL OR LOWER(s.title) = LOWER(:series)) AND " +
                                        "(:status IS NULL OR " +
                                        "  (:status = 'PUBLISHED' AND p.is_published = true) OR " +
                                        "  (:status = 'DRAFT' AND p.is_published = false)) AND " +
                                        "(:tags IS NULL OR p.id IN (" +
                                        "  SELECT pt.post_id FROM post_tags pt " +
                                        "  JOIN tags t ON pt.tag_id = t.id " +
                                        "  WHERE t.name = ANY(CAST(:tagList AS text[])) " +
                                        "  GROUP BY pt.post_id " +
                                        "  HAVING COUNT(DISTINCT t.name) = :tagCount" +
                                        "))", nativeQuery = true)
        Page<Post> findAllWithFilters(
                        @Param("search") String search,
                        @Param("category") String category,
                        @Param("series") String series,
                        @Param("status") String status,
                        @Param("tags") String tags,
                        @Param("tagList") String[] tagList,
                        @Param("tagCount") int tagCount,
                        Pageable pageable);

        // Find related posts by category, excluding current post
        List<Post> findTop10ByCategoryIdAndIsPublishedAndIdNotOrderByCreatedAtDesc(
                        Long categoryId,
                        boolean isPublished,
                        Long excludeId);

        long countByIsPublished(boolean isPublished);

        long countByCategoryId(Long categoryId);

        @Query("SELECT p.categoryId as id, COUNT(p) as count FROM Post p WHERE p.isPublished = true GROUP BY p.categoryId")
        List<Object[]> countPublishedPostsByCategory();

        @Query("SELECT p.seriesId as id, COUNT(p) as count FROM Post p WHERE p.isPublished = true AND p.seriesId IS NOT NULL GROUP BY p.seriesId")
        List<Object[]> countPublishedPostsBySeries();
}
