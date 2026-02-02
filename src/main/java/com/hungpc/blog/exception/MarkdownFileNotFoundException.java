package com.hungpc.blog.exception;

import lombok.Getter;

/**
 * Exception thrown when markdown file is not found in repository
 */
@Getter
public class MarkdownFileNotFoundException extends RuntimeException {

    private final String slug;
    private final String expectedPath;

    public MarkdownFileNotFoundException(String slug, String expectedPath) {
        super(String.format("Markdown file not found for slug '%s' at path '%s'", slug, expectedPath));
        this.slug = slug;
        this.expectedPath = expectedPath;
    }
}
