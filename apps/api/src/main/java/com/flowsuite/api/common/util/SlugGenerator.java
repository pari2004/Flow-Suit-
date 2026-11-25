package com.flowsuite.api.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/** Turns a display name into a URL-safe slug, with a uniqueness-aware helper. */
public final class SlugGenerator {

    private SlugGenerator() {
    }

    /** Lowercase, ASCII, hyphen-separated base slug (no uniqueness guarantee). */
    public static String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isEmpty() ? "org" : slug;
    }

    /**
     * Returns {@link #toSlug(String)}, appending a numeric suffix until {@code isTaken}
     * reports the candidate is free. Prevents unbounded collisions by falling back to a
     * random suffix after a few tries.
     */
    public static String toUniqueSlug(String input, Predicate<String> isTaken) {
        String base = toSlug(input);
        if (!isTaken.test(base)) {
            return base;
        }
        for (int i = 2; i <= 50; i++) {
            String candidate = base + "-" + i;
            if (!isTaken.test(candidate)) {
                return candidate;
            }
        }
        String random = base + "-" + Long.toString(System.nanoTime(), 36);
        return random;
    }
}
