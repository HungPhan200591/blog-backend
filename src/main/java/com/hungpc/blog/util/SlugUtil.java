package com.hungpc.blog.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+)|(-+$)");

    private SlugUtil() {
    }

    public static String slugify(String input) {
        if (input == null) {
            return null;
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String lowercased = normalized.toLowerCase(Locale.ROOT);
        String dashed = NON_ALPHANUMERIC.matcher(lowercased).replaceAll("-");
        return EDGE_DASHES.matcher(dashed).replaceAll("");
    }
}
