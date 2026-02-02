package com.hungpc.blog.service.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Antigravity Local Proxy AI Provider
 * Uses local Gemini-compatible proxy (http://127.0.0.1:8045)
 * 
 * Benefits:
 * - Unlimited rate limit (no 15 RPM restriction)
 * - Gemini-compatible interface (uses Google GenAI SDK)
 * - Local development friendly
 * - Can use Gemini models through local proxy
 * 
 * Activate with: ai.provider=antigravity-local-proxy
 * 
 * Python equivalent:
 * 
 * <pre>
 * import google.generativeai as genai
 * 
 * genai.configure(
 *     api_key="sk-25d1af550d014c1c9dffd6260dffbfb3",
 *     transport='rest',
 *     client_options={'api_endpoint': 'http://127.0.0.1:8045'}
 * )
 * 
 * model = genai.GenerativeModel('gemini-3-flash')
 * response = model.generate_content("Hello")
 * print(response.text)
 * </pre>
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "ai.provider", havingValue = "antigravity-local-proxy")
public class AntigravityLocalProxyProvider implements AIProvider {

    @Value("${antigravity.proxy.api-endpoint}")
    private String apiEndpoint;

    @Value("${antigravity.proxy.api-key}")
    private String apiKey;

    @Value("${antigravity.proxy.model:gemini-3-flash}")
    private String model;

    @Value("${antigravity.proxy.enable-token-limit:false}")
    private boolean enableTokenLimit;

    @Value("${antigravity.proxy.max-output-tokens:5000}")
    private int maxOutputTokens;

    private Client client;

    /**
     * Initialize Gemini client with custom endpoint (local proxy)
     * Note: Google GenAI SDK supports custom endpoints via baseUrl
     */
    private Client getClient() {
        if (client == null) {
            log.info("🔧 [Antigravity Local Proxy] Initializing Gemini client");
            log.info("🔧 [Antigravity Local Proxy] API Endpoint: {}", apiEndpoint);
            log.info("� [Antigravity Local Proxy] Model: {}", model);

            // Initialize Google GenAI client with custom endpoint
            // Note: The SDK might not support custom endpoints directly
            // This is a placeholder - you may need to use HTTP client directly
            client = Client.builder()
                    .apiKey(apiKey)
                    .httpOptions(HttpOptions.builder()
                            .baseUrl(apiEndpoint)  // Proxy như Antigravity
                            .build())
                    .build();

            log.warn("⚠️ Google GenAI SDK limitation: Custom endpoint support may be limited");
            log.warn("💡 If this doesn't work, we'll need to use HTTP client directly");
        }
        return client;
    }

    @Override
    public String generateContent(String prompt) {
        log.info("🤖 [Antigravity Local Proxy] Using local Gemini proxy at: {}", apiEndpoint);
        log.info("🤖 [Antigravity Local Proxy] Model: {}", model);

        try {
            // Build config (same as GeminiAIProvider)
            GenerateContentConfig config = null;
            if (enableTokenLimit) {
                config = GenerateContentConfig.builder()
                        .maxOutputTokens(maxOutputTokens)
                        .temperature(0.7f)
                        .build();
                log.info("🔧 [Antigravity Local Proxy] Token limit enabled: {} tokens", maxOutputTokens);
            }

            // Call Gemini API (will use local proxy endpoint)
            GenerateContentResponse response = getClient().models.generateContent(
                    model,
                    prompt,
                    config);

            String result = response.text();
            log.info("✅ [Antigravity Local Proxy] Response length: {} chars", result.length());
            return result;

        } catch (Exception e) {
            log.error("❌ [Antigravity Local Proxy] API call failed", e);
            log.error("⚠️ Make sure the proxy server is running on {}", apiEndpoint);
            log.error("💡 Start proxy: gemini-openai-proxy or check your proxy server");
            throw new RuntimeException("Antigravity Local Proxy failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "Antigravity Local Proxy (Gemini via " + apiEndpoint + ")";
    }
}
