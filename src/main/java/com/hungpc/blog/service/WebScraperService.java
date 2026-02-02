package com.hungpc.blog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Service for web scraping using Jina Reader API
 * Converts any URL to clean markdown format
 * Uses WebClient with 10MB buffer for large blog posts
 */
@Service
@Slf4j
public class WebScraperService {

    private static final String JINA_READER_URL = "https://r.jina.ai/";

    // Buffer size: 10MB (sufficient for 99% of blog posts)
    // See: docs/technical-debt/streaming-optimization.md for future improvements
    private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10MB

    private final WebClient webClient;

    public WebScraperService(WebClient.Builder webClientBuilder) {
        // Configure ExchangeStrategies with larger buffer size
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();

        this.webClient = webClientBuilder
                .baseUrl(JINA_READER_URL)
                .exchangeStrategies(strategies)
                .build();

        log.info("✅ WebScraperService initialized with {}MB buffer", MAX_IN_MEMORY_SIZE / 1024 / 1024);
    }

    /**
     * Scrape URL and convert to markdown using Jina Reader API
     * 
     * @param url The URL to scrape
     * @return Clean markdown content
     */
    public String scrapeToMarkdown(String url) {
        log.info("Scraping URL: {}", url);

        try {
            // Jina Reader API: https://r.jina.ai/{url}
            String markdown = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // Block for synchronous behavior

            if (markdown == null || markdown.isBlank()) {
                throw new RuntimeException("Jina Reader returned empty content");
            }

            log.info("✅ Scraped {} chars from {}", markdown.length(), url);
            return markdown;

        } catch (Exception e) {
            log.error("❌ Failed to scrape URL: {}", url, e);
            throw new RuntimeException("Failed to scrape URL: " + e.getMessage(), e);
        }
    }
}
