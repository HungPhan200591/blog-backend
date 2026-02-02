package com.hungpc.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Service for fetching cover images from Pexels API
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PexelsService {

    @Value("${pexels.api-key}")
    private String pexelsApiKey;

    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";

    private final WebClient.Builder webClientBuilder;
    private final Random random = new Random();

    /**
     * Build search query from title (same logic as frontend)
     */
    private String buildCoverSearchQuery(String rawTitle) {
        // Clean and extract keywords
        String cleaned = rawTitle
                .toLowerCase()
                .replaceAll("[^\\w\\s-]", " ")
                .trim();

        String[] words = cleaned.split("\\s+");
        StringBuilder queryBuilder = new StringBuilder();

        int count = 0;
        for (String word : words) {
            if (word.length() > 2 && count < 6) {
                if (count > 0)
                    queryBuilder.append(" ");
                queryBuilder.append(word);
                count++;
            }
        }

        // Topic pool for better image results
        String[] topicPool = {
                "technology", "software", "programming", "coding",
                "computer", "science", "space", "cosmos",
                "abstract", "geometry"
        };

        String randomTopic = topicPool[random.nextInt(topicPool.length)];

        String finalQuery = queryBuilder.toString().trim();
        if (finalQuery.isEmpty()) {
            return randomTopic;
        }

        return finalQuery + " " + randomTopic;
    }

    /**
     * Search for cover image on Pexels
     * Returns the first high-quality image URL
     * 
     * @param title Post title
     * @return Image URL or null if not found
     */
    public String searchCoverImage(String title) {
        try {
            String searchQuery = buildCoverSearchQuery(title);
            log.info("🔍 Searching Pexels for: {}", searchQuery);

            WebClient webClient = webClientBuilder
                    .baseUrl(PEXELS_API_URL)
                    .defaultHeader("Authorization", pexelsApiKey)
                    .build();

            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("query", searchQuery)
                            .queryParam("per_page", 1) // Only need 1 image
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("photos")) {
                log.warn("⚠️ No photos found in Pexels response");
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> photos = (List<Map<String, Object>>) response.get("photos");

            if (photos == null || photos.isEmpty()) {
                log.warn("⚠️ No photos found for query: {}", searchQuery);
                return null;
            }

            // Get first photo's large2x URL (best quality)
            Map<String, Object> photo = photos.get(0);
            @SuppressWarnings("unchecked")
            Map<String, String> src = (Map<String, String>) photo.get("src");

            if (src == null) {
                log.warn("⚠️ No src found in photo");
                return null;
            }

            // Priority: large2x > large > original
            String imageUrl = src.getOrDefault("large2x",
                    src.getOrDefault("large",
                            src.get("original")));

            if (imageUrl != null) {
                log.info("✅ Found cover image: {}", imageUrl);
                String photographer = (String) photo.get("photographer");
                if (photographer != null) {
                    log.debug("📸 Photo by: {}", photographer);
                }
            }

            return imageUrl;

        } catch (Exception e) {
            log.error("❌ Failed to search Pexels: {}", e.getMessage(), e);
            return null;
        }
    }
}
