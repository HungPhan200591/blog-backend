package com.hungpc.blog.service;

import com.hungpc.blog.dto.request.CreateSeriesRequest;
import com.hungpc.blog.dto.request.UpdateSeriesRequest;
import com.hungpc.blog.dto.response.SeriesResponse;
import com.hungpc.blog.exception.ConflictException;
import com.hungpc.blog.exception.ResourceNotFoundException;
import com.hungpc.blog.model.Series;
import com.hungpc.blog.repository.SeriesRepository;
import com.hungpc.blog.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.hungpc.blog.constant.CacheConstants;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final PostRepository postRepository;
    private final JdbcTemplate jdbcTemplate;

    @Cacheable(value = CacheConstants.SERIES)
    @Transactional(readOnly = true)
    public List<SeriesResponse> getAllSeries() {
        // Fetch all counts in one query
        java.util.Map<Long, Integer> countMap = postRepository.countPublishedPostsBySeries().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()));

        return seriesRepository.findAllByOrderByTitleAsc().stream()
                .map(s -> mapToResponse(s, countMap.getOrDefault(s.getId(), 0)))
                .collect(Collectors.toList());
    }

    @CacheEvict(value = { CacheConstants.SERIES, CacheConstants.POSTS, CacheConstants.POST_BY_SLUG }, allEntries = true)
    @Transactional
    public SeriesResponse createSeries(CreateSeriesRequest request) {
        String title = request.getTitle().trim();
        if (seriesRepository.existsByTitle(title)) {
            throw new ConflictException("Series '" + title + "' already exists");
        }

        Series series = Series.builder()
                .title(title)
                .description(request.getDescription())
                .coverImage(request.getCoverImage())
                .color(request.getColor())
                .postCount(0)
                .build();

        Series savedSeries = seriesRepository.save(series);
        return mapToResponse(savedSeries, 0);
    }

    @CacheEvict(value = { CacheConstants.SERIES, CacheConstants.POSTS, CacheConstants.POST_BY_SLUG }, allEntries = true)
    @Transactional
    public SeriesResponse updateSeries(Long id, UpdateSeriesRequest request) {
        Series series = seriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Series", "id", id));

        String title = request.getTitle().trim();
        if (seriesRepository.existsByTitleAndIdNot(title, id)) {
            throw new ConflictException("Series '" + title + "' already exists");
        }

        series.setTitle(title);
        series.setDescription(request.getDescription());
        series.setCoverImage(request.getCoverImage());
        series.setColor(request.getColor());

        Series updatedSeries = seriesRepository.save(series);
        return mapToResponse(updatedSeries, updatedSeries.getPostCount());
    }

    @CacheEvict(value = { CacheConstants.SERIES, CacheConstants.POSTS, CacheConstants.POST_BY_SLUG }, allEntries = true)
    @Transactional
    public void deleteSeries(Long id) {
        Series series = seriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Series", "id", id));
        jdbcTemplate.update("UPDATE posts SET series_id = NULL WHERE series_id = ?", id);
        seriesRepository.delete(series);
    }

    private SeriesResponse mapToResponse(Series series, Integer dynamicCount) {
        return SeriesResponse.builder()
                .id(series.getId())
                .title(series.getTitle())
                .description(series.getDescription())
                .coverImage(series.getCoverImage())
                .color(series.getColor())
                .postCount(dynamicCount)
                .createdAt(series.getCreatedAt())
                .build();
    }
}
