package com.hungpc.blog.service;

import com.hungpc.blog.dto.MarkdownContent;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for parsing markdown files with frontmatter
 */
@Service
@Slf4j
public class MarkdownService {

    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    private final Yaml yamlParser;

    public MarkdownService() {
        // Initialize Flexmark parser with extensions
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create()));

        this.markdownParser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
        this.yamlParser = new Yaml();
    }

    /**
     * Parse markdown file with frontmatter
     * 
     * Expected format:
     * ---
     * title: Post Title
     * tags: [java, spring]
     * ---
     * # Content here
     * 
     * @param rawContent Raw markdown content with frontmatter
     * @return Parsed MarkdownContent with separated frontmatter and content
     */
    public MarkdownContent parse(String rawContent) {
        Map<String, Object> frontmatter = new HashMap<>();
        String content = rawContent;

        // Check for frontmatter (starts with ---)
        if (rawContent.startsWith("---")) {
            int endIndex = rawContent.indexOf("---", 3);

            if (endIndex > 0) {
                // Extract frontmatter YAML
                String frontmatterYaml = rawContent.substring(3, endIndex).trim();

                try {
                    Map<String, Object> parsed = yamlParser.load(frontmatterYaml);
                    if (parsed != null) {
                        frontmatter = parsed;
                        log.debug("Parsed frontmatter with {} keys", frontmatter.size());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse frontmatter YAML", e);
                }

                // Extract content (after second ---)
                content = rawContent.substring(endIndex + 3).trim();
            }
        }

        return MarkdownContent.builder()
                .content(content)
                .frontmatter(frontmatter)
                .build();
    }

    /**
     * Convert markdown to HTML (optional, for preview)
     * 
     * @param markdown Markdown content
     * @return HTML string
     */
    public String toHtml(String markdown) {
        Node document = markdownParser.parse(markdown);
        return htmlRenderer.render(document);
    }
}
