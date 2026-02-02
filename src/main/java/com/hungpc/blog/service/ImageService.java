package com.hungpc.blog.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for downloading and managing images from markdown content
 * Uses WebClient with 10MB buffer for large images
 */
@Service
@Slf4j
public class ImageService {

    @Value("${git.repo.local-path}")
    private String gitRepoPath;

    // Buffer size: 10MB (sufficient for most images)
    // See: docs/technical-debt/streaming-optimization.md for future improvements
    private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10MB

    private final WebClient webClient;

    public ImageService(WebClient.Builder webClientBuilder) {
        // Configure ExchangeStrategies with larger buffer size
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();

        this.webClient = webClientBuilder
                .exchangeStrategies(strategies)
                .build();

        log.info("✅ ImageService initialized with {}MB buffer", MAX_IN_MEMORY_SIZE / 1024 / 1024);
    }

    /**
     * Process images in markdown content
     * Downloads external images and replaces URLs with local paths
     * 
     * @param markdown The markdown content with image URLs
     * @return Markdown with local image paths
     */
    public String processImages(String markdown) {
        log.info("Processing images in markdown");

        // Extract image URLs: ![alt](url)
        Pattern pattern = Pattern.compile("!\\[([^\\]]*)\\]\\(([^\\)]+)\\)");
        Matcher matcher = pattern.matcher(markdown);

        StringBuffer result = new StringBuffer();
        int downloadedCount = 0;
        int skippedCount = 0;

        while (matcher.find()) {
            String alt = matcher.group(1);
            String url = matcher.group(2);

            // Skip if already local path
            if (url.startsWith("/images/") || url.startsWith("images/")) {
                skippedCount++;
                continue;
            }

            // Skip data URLs
            if (url.startsWith("data:")) {
                log.warn("Skipping data URL image");
                skippedCount++;
                continue;
            }

            try {
                // Download and save image
                String filename = downloadAndSave(url);
                String localPath = "/images/" + filename;

                // Replace URL with local path
                matcher.appendReplacement(result, "![" + Matcher.quoteReplacement(alt) + "](" + localPath + ")");
                log.info("Downloaded image: {} → {}", url, localPath);
                downloadedCount++;

            } catch (Exception e) {
                log.warn("Failed to download image: {}", url, e);
                // Keep original URL if download fails
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                skippedCount++;
            }
        }
        matcher.appendTail(result);

        log.info("✅ Image processing complete: {} downloaded, {} skipped", downloadedCount, skippedCount);
        return result.toString();
    }

    /**
     * Download image and save to Git repo
     * 
     * @param url The image URL
     * @return The generated filename
     */
    private String downloadAndSave(String url) throws IOException {
        log.debug("Downloading image from: {}", url);

        try {
            // Download image using WebClient with 10 second timeout
            byte[] imageBytes = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(java.time.Duration.ofSeconds(10)) // ✅ Add timeout
                    .block(); // Block for synchronous behavior

            if (imageBytes == null || imageBytes.length == 0) {
                throw new IOException("Downloaded image is empty");
            }

            // Generate unique filename (MD5 hash of URL)
            String hash = DigestUtils.md5Hex(url);
            String ext = getExtension(url);
            String filename = hash + ext;

            // Save to Git repo: /images/{hash}.png
            Path imagesDir = Paths.get(gitRepoPath, "images");
            Files.createDirectories(imagesDir);

            Path imagePath = imagesDir.resolve(filename);
            Files.write(imagePath, imageBytes);

            log.debug("Saved image: {} ({} bytes)", imagePath, imageBytes.length);
            return filename;

        } catch (Exception e) {
            throw new IOException("Failed to download image from " + url + ": " + e.getMessage(), e);
        }
    }

    /**
     * Extract file extension from URL
     */
    private String getExtension(String url) {
        // Extract extension from URL
        String[] parts = url.split("\\.");
        if (parts.length > 0) {
            String ext = parts[parts.length - 1].split("\\?")[0]; // Remove query params
            ext = ext.toLowerCase();

            // Validate extension
            if (ext.matches("^(png|jpg|jpeg|gif|webp|svg)$")) {
                return "." + ext;
            }
        }

        // Default to .png if extension not found
        return ".png";
    }
}
