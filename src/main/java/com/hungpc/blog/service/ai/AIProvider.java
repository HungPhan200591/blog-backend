package com.hungpc.blog.service.ai;

/**
 * Interface for AI content generation providers
 * Allows easy switching between different AI services (Gemini, OpenAI, Claude,
 * etc.)
 * to avoid rate limits and vendor lock-in
 */
public interface AIProvider {

    /**
     * Generate content using AI
     * 
     * @param prompt The prompt to send to the AI provider
     * @return Generated text response
     * @throws RuntimeException if AI call fails
     */
    String generateContent(String prompt);

    /**
     * Get the name of this AI provider
     * 
     * @return Provider name (e.g., "Gemini", "OpenAI", "Claude")
     */
    String getProviderName();
}
