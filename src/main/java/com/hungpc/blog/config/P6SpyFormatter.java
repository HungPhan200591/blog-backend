package com.hungpc.blog.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

/**
 * Custom P6Spy log formatter that truncates long bind parameters
 * to prevent cluttering logs with large content (e.g., blog post content)
 */
public class P6SpyFormatter implements MessageFormattingStrategy {

    private static final int MAX_PARAM_LENGTH = 200;
    private static final String TRUNCATED_SUFFIX = "... (truncated)";

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category,
            String prepared, String sql, String url) {

        // Truncate long SQL parameters
        String truncatedSql = truncateLongParams(sql);

        return String.format("%s | took %dms | %s | %s",
                now, elapsed, category, truncatedSql);
    }

    /**
     * Truncate long parameter values in SQL statement
     */
    private String truncateLongParams(String sql) {
        if (sql == null || sql.length() <= MAX_PARAM_LENGTH) {
            return sql;
        }

        // For INSERT/UPDATE statements with long content
        if (sql.contains("insert into") || sql.contains("update ")) {
            // Find the VALUES clause
            int valuesIndex = sql.indexOf("values (");
            if (valuesIndex > 0) {
                String beforeValues = sql.substring(0, valuesIndex + 8); // "values ("
                String afterValues = sql.substring(valuesIndex + 8);

                // Truncate the values part if too long
                if (afterValues.length() > MAX_PARAM_LENGTH) {
                    afterValues = afterValues.substring(0, MAX_PARAM_LENGTH) + TRUNCATED_SUFFIX;
                }

                return beforeValues + afterValues;
            }
        }

        // For other long queries, just truncate
        if (sql.length() > MAX_PARAM_LENGTH * 2) {
            return sql.substring(0, MAX_PARAM_LENGTH * 2) + TRUNCATED_SUFFIX;
        }

        return sql;
    }
}
