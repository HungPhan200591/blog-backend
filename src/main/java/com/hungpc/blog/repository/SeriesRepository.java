package com.hungpc.blog.repository;

import com.hungpc.blog.model.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SeriesRepository extends JpaRepository<Series, Long> {
    boolean existsByTitle(String title);
    boolean existsByTitleAndIdNot(String title, Long id);
    List<Series> findAllByOrderByTitleAsc();
}
