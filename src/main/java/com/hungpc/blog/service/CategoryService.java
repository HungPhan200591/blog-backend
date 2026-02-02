package com.hungpc.blog.service;

import com.hungpc.blog.dto.request.CreateCategoryRequest;
import com.hungpc.blog.dto.request.UpdateCategoryRequest;
import com.hungpc.blog.dto.response.CategoryResponse;
import com.hungpc.blog.exception.BadRequestException;
import com.hungpc.blog.exception.ConflictException;
import com.hungpc.blog.exception.ResourceNotFoundException;
import com.hungpc.blog.model.Category;
import com.hungpc.blog.repository.CategoryRepository;
import com.hungpc.blog.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.hungpc.blog.constant.CacheConstants;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    @Cacheable(value = CacheConstants.CATEGORIES)
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        // Fetch all counts in one query
        java.util.Map<Long, Integer> countMap = postRepository.countPublishedPostsByCategory().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()));

        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> mapToResponse(c, countMap.getOrDefault(c.getId(), 0)))
                .collect(Collectors.toList());
    }

    @CacheEvict(value = { CacheConstants.CATEGORIES, CacheConstants.POSTS,
            CacheConstants.POST_BY_SLUG }, allEntries = true)
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByName(name)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }

        Category category = Category.builder()
                .name(name)
                .color(request.getColor())
                .postCount(0)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory, 0);
    }

    @CacheEvict(value = { CacheConstants.CATEGORIES, CacheConstants.POSTS,
            CacheConstants.POST_BY_SLUG }, allEntries = true)
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        String name = request.getName().trim();
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }

        category.setName(name);
        category.setColor(request.getColor());

        Category updatedCategory = categoryRepository.save(category);
        return mapToResponse(updatedCategory, updatedCategory.getPostCount());
    }

    @CacheEvict(value = { CacheConstants.CATEGORIES, CacheConstants.POSTS,
            CacheConstants.POST_BY_SLUG }, allEntries = true)
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Count actual posts using this category (including drafts)
        long actualPostCount = postRepository.countByCategoryId(id);

        if (actualPostCount > 0) {
            throw new BadRequestException(
                    "Cannot delete category '" + category.getName() + "' because it has " + actualPostCount + " posts");
        }
        categoryRepository.delete(category);
    }

    private CategoryResponse mapToResponse(Category category, Integer dynamicCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .postCount(dynamicCount)
                .createdAt(category.getCreatedAt())
                .build();
    }
}
