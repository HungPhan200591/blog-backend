package com.hungpc.blog.util;

import com.hungpc.blog.constant.ColorConstants;

import java.util.Random;

/**
 * Utility class for color-related operations
 */
public class ColorUtils {

    private static final Random RANDOM = new Random();

    /**
     * Get a random color from the predefined palette
     * 
     * @return Hex color string (e.g., "#3b82f6")
     */
    public static String getRandomColor() {
        return ColorConstants.PREDEFINED_COLORS[RANDOM.nextInt(ColorConstants.PREDEFINED_COLORS.length)];
    }

    private ColorUtils() {
        // Prevent instantiation
    }
}
