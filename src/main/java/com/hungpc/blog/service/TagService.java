package com.hungpc.blog.service;

import com.hungpc.blog.dto.request.CreateTagRequest;
import com.hungpc.blog.dto.request.UpdateTagRequest;
import com.hungpc.blog.dto.response.TagResponse;
import com.hungpc.blog.exception.BadRequestException;
import com.hungpc.blog.exception.ConflictException;
import com.hungpc.blog.exception.ResourceNotFoundException;
import com.hungpc.blog.model.Tag;
import com.hungpc.blog.repository.TagRepository;
import com.hungpc.blog.repository.PostTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.hungpc.blog.constant.CacheConstants;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;

    @Cacheable(value = CacheConstants.TAGS)
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return getAllTags(null); // Default: alphabetical
    }

    @Cacheable(value = CacheConstants.TAGS, key = "#sort != null ? #sort : 'default'")
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags(String sort) {
        // Fetch all counts in one query
        Map<Long, Integer> countMap = postTagRepository.countPublishedPostsByTag().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()));

        List<TagResponse> tags = tagRepository.findAllByOrderByNameAsc().stream()
                .map(t -> mapToResponse(t, countMap.getOrDefault(t.getId(), 0)))
                .collect(Collectors.toList());

        // Sort by popular (postCount desc) if requested
        if ("popular".equalsIgnoreCase(sort)) {
            tags.sort((a, b) -> Integer.compare(b.getPostCount(), a.getPostCount()));
        }
        // Otherwise already sorted alphabetically by repository query

        return tags;
    }

    @CacheEvict(value = { CacheConstants.TAGS, CacheConstants.POSTS, CacheConstants.POST_BY_SLUG }, allEntries = true)
    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        String name = request.getName().trim();
        if (tagRepository.existsByName(name)) {
            throw new ConflictException("Tag '" + name + "' already exists");
        }

        Tag tag = Tag.builder()
                .name(name)
                .color(request.getColor())
                .postCount(0)
                .build();

        Tag savedTag = tagRepository.save(tag);
        return mapToResponse(savedTag, 0);
    }

    @CacheEvict(value = { CacheConstants.TAGS, CacheConstants.POSTS, CacheConstants.POST_BY_SLUG }, allEntries = true)
    @Transactional
    public TagResponse updateTag(Long id, UpdateTagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));

        String name = request.getName().trim();
        if (tagRepository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException("Tag '" + name + "' already exists");
        }

        tag.setName(name);
        tag.setColor(request.getColor());

        Tag updatedTag = tagRepository.save(tag);
        return mapToResponse(updatedTag, updatedTag.getPostCount());
    }

    @CacheEvict(value = { CacheConstants.TAGS, CacheConstants.POSTS, CacheConstants.POST_BY_SLUG }, allEntries = true)
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));

        // Count actual posts using this tag (including drafts)
        long actualPostCount = postTagRepository.countByTagId(id);

        if (actualPostCount > 0) {
            throw new BadRequestException(
                    "Cannot delete tag '" + tag.getName() + "' because it has " + actualPostCount + " posts");
        }
        tagRepository.delete(tag);
    }

    @CacheEvict(value = { CacheConstants.TAGS, CacheConstants.POSTS, CacheConstants.POST_BY_SLUG }, allEntries = true)
    @Transactional
    public void cleanupUnusedTags() {
        List<Tag> unusedTags = tagRepository.findUnusedTags();

        if (!unusedTags.isEmpty()) {
            tagRepository.deleteAll(unusedTags);
        }
    }

    private TagResponse mapToResponse(Tag tag, Integer dynamicCount) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .postCount(dynamicCount)
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
