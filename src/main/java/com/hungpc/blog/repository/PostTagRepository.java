package com.hungpc.blog.repository;

import com.hungpc.blog.model.PostTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostTagRepository extends JpaRepository<PostTag, PostTag.PostTagId> {

    /**
     * Find all tag IDs for a post
     */
    @Query("SELECT pt.tagId FROM PostTag pt WHERE pt.postId = :postId")
    List<Long> findTagIdsByPostId(@Param("postId") Long postId);

    /**
     * ✅ BATCH LOADING: Find all tag IDs for multiple posts (1 query instead of N)
     * Returns Map<PostId, List<TagId>>
     */
    @Query("SELECT pt.postId as postId, pt.tagId as tagId FROM PostTag pt WHERE pt.postId IN :postIds")
    List<PostTagProjection> findTagIdsByPostIds(@Param("postIds") List<Long> postIds);

    /**
     * Projection interface for batch loading
     */
    interface PostTagProjection {
        Long getPostId();

        Long getTagId();
    }

    /**
     * Delete all tags for a post
     */
    @Modifying
    @Query("DELETE FROM PostTag pt WHERE pt.postId = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    /**
     * Check if post-tag relationship exists
     */
    boolean existsByPostIdAndTagId(Long postId, Long tagId);

    /**
     * Count actual posts using this tag
     */
    @Query("SELECT COUNT(pt) FROM PostTag pt WHERE pt.tagId = :tagId")
    long countByTagId(@Param("tagId") Long tagId);

    /**
     * Count published posts for each tag
     */
    @Query("SELECT pt.tagId as id, COUNT(pt) as count FROM PostTag pt " +
            "JOIN Post p ON pt.postId = p.id " +
            "WHERE p.isPublished = true GROUP BY pt.tagId")
    List<Object[]> countPublishedPostsByTag();
}
