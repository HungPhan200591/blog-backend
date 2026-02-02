package com.hungpc.blog.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for converting between naming conventions
 */
public class NamingUtils {

    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([a-z])([A-Z]+)");

    /**
     * Convert camelCase to snake_case
     * 
     * Examples:
     * - createdAt -> created_at
     * - isPublished -> is_published
     * - visitCount -> visit_count
     */
    public static String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }

        Matcher matcher = CAMEL_CASE_PATTERN.matcher(camelCase);
        return matcher.replaceAll("$1_$2").toLowerCase();
    }

    /**
     * Convert snake_case to camelCase
     * 
     * Examples:
     * - created_at -> createdAt
     * - is_published -> isPublished
     * - visit_count -> visitCount
     */
    public static String snakeToCamel(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }
}
