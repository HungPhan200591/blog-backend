package com.hungpc.blog.service.ai;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * OpenAI AI Provider implementation (GPT-4, GPT-3.5)
 * Uses official OpenAI Java SDK for clean, type-safe API integration
 * 
 * Activate with: ai.provider=openai
 * Required env vars:
 * - openai.api-key
 * - openai.model (default: gpt-4)
 * - openai.api-url (default: https://api.openai.com/v1/chat/completions)
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAIProvider implements AIProvider {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4}")
    private String model;

    @Value("${openai.api-url:https://api.openai.com/}")
    private String apiUrl;

    @Value("${openai.timeout:60}")
    private int timeoutSeconds;

    private OpenAiService service;

    /**
     * Initialize OpenAI service lazily
     */
    private OpenAiService getService() {
        if (service == null) {
            // Extract base URL (remove /v1/chat/completions if present)
            String baseUrl = apiUrl.replaceAll("/v1/chat/completions$", "");
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            log.info("🔧 [OpenAI] Initializing with base URL: {}", baseUrl);
            service = new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
            // Note: OpenAI SDK doesn't support custom base URL in constructor
            // For custom URLs (like local proxy), use AntigravityLocalProxyProvider
        }
        return service;
    }

    @Override
    public String generateContent(String prompt) {
        log.info("🤖 [OpenAI] Calling API with model: {}", model);

        try {
            // Build chat completion request
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new ChatMessage("user", prompt)))
                    .temperature(0.7)
                    .build();

            // Call OpenAI API
            var response = getService().createChatCompletion(request);

            String result = response.getChoices().get(0).getMessage().getContent();

            // Log usage
            if (response.getUsage() != null) {
                log.info("✅ [OpenAI] Response length: {} chars, Tokens: {}",
                        result.length(), response.getUsage().getTotalTokens());
            } else {
                log.info("✅ [OpenAI] Response length: {} chars", result.length());
            }

            return result;

        } catch (Exception e) {
            log.error("❌ [OpenAI] API call failed", e);
            throw new RuntimeException("OpenAI API failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "OpenAI (" + model + ")";
    }
}
