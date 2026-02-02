package com.hungpc.blog.service;

import com.hungpc.blog.model.Category;
import com.hungpc.blog.model.Tag;
import com.hungpc.blog.repository.CategoryRepository;
import com.hungpc.blog.repository.TagRepository;
import com.hungpc.blog.util.ColorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper service for category and tag operations
 * Shared across PostService and PostSyncService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryTagHelper {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    /**
     * Get or create category by name
     * 
     * @param categoryName Category name
     * @return Category entity
     */
    public Category getOrCreateCategory(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseGet(() -> {
                    Category newCategory = Category.builder()
                            .name(categoryName)
                            .color(ColorUtils.getRandomColor())
                            .build();
                    Category saved = categoryRepository.save(newCategory);
                    log.info("✅ Created new category: {} (ID: {})", saved.getName(), saved.getId());
                    return saved;
                });
    }

    /**
     * Get or create tags by names
     * 
     * @param tagNames List of tag names
     * @return List of tag IDs
     */
    public List<Long> getOrCreateTags(List<String> tagNames) {
        return tagNames.stream()
                .map(tagName -> tagRepository.findByName(tagName)
                        .orElseGet(() -> {
                            Tag newTag = Tag.builder()
                                    .name(tagName)
                                    .color(ColorUtils.getRandomColor())
                                    .build();
                            Tag saved = tagRepository.save(newTag);
                            log.debug("✅ Created new tag: {} (ID: {})", saved.getName(), saved.getId());
                            return saved;
                        }))
                .map(Tag::getId)
                .collect(Collectors.toList());
    }
}
