package com.hungpc.blog.repository;

import com.hungpc.blog.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("SELECT t FROM Tag t WHERE t.id NOT IN (SELECT pt.tagId FROM PostTag pt)")
    List<Tag> findUnusedTags();

    List<Tag> findAllByOrderByNameAsc();
}
