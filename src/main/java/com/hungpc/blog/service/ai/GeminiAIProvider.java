package com.hungpc.blog.service.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Gemini AI Provider implementation
 * Uses Google's official GenAI SDK
 * 
 * Activate with: ai.provider=gemini (default)
 */
@Service
@Slf4j
@Primary // Default provider
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiAIProvider implements AIProvider {

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    private final Client client;
    private final Environment env;

    public GeminiAIProvider(
            @Value("${gemini.api-key}") String apiKey,
            Environment env) {
        this.client = Client.builder().apiKey(apiKey).build();
        this.env = env;
        log.info("✅ Gemini AI Provider initialized with model: {}", model);
    }

    @Override
    public String generateContent(String prompt) {
        log.info("🤖 [Gemini] Calling API with prompt length: {} chars", prompt.length());

        try {
            // Read token limit flag at runtime (allows hot reload via env var change)
            boolean enableTokenLimit = env.getProperty("gemini.enable-token-limit", Boolean.class, false);

            GenerateContentConfig config = null;
            if (enableTokenLimit) {
                int maxTokens = env.getProperty("gemini.max-output-tokens", Integer.class, 5000);
                config = GenerateContentConfig.builder()
                        .maxOutputTokens(maxTokens)
                        .temperature(0.7f)
                        .build();
                log.info("🔧 [Gemini] Token limit enabled: {} tokens", maxTokens);
            }

            // Call Gemini API
            GenerateContentResponse response = client.models.generateContent(
                    model,
                    prompt,
                    config);

            String result = response.text();
            log.info("✅ [Gemini] Response length: {} chars", result.length());
            return result;

        } catch (Exception e) {
            log.error("❌ [Gemini] API call failed", e);
            throw new RuntimeException("Gemini API failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "Gemini";
    }
}
