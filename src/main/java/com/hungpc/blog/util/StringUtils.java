package com.hungpc.blog.util;

import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class for handling nullable and blank string checks
 * Reduces boilerplate code for null/blank validation
 */
public class StringUtils {

    private StringUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Check if string is not null and not blank
     * 
     * @param value String to check
     * @return true if string has content
     */
    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Check if list is not null and not empty
     * 
     * @param list List to check
     * @return true if list has elements
     */
    public static boolean hasElements(List<?> list) {
        return list != null && !list.isEmpty();
    }

    /**
     * Execute consumer if string has text
     * Get value once and reuse in consumer
     * 
     * @param value    String to check
     * @param consumer Consumer to execute with the value
     */
    public static void ifHasText(String value, Consumer<String> consumer) {
        if (hasText(value)) {
            consumer.accept(value);
        }
    }

    /**
     * Execute consumer if list has elements
     * Get value once and reuse in consumer
     * 
     * @param list     List to check
     * @param consumer Consumer to execute with the list
     */
    public static <T> void ifHasElements(List<T> list, Consumer<List<T>> consumer) {
        if (hasElements(list)) {
            consumer.accept(list);
        }
    }
}
