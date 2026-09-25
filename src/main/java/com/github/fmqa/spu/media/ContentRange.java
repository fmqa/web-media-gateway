package com.github.fmqa.spu.media;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Modern Java representation and parser for the HTTP Content-Range header (RFC 9110).
 * Uses negative values when a range bound or instance length is unknown/unspecified.
 */
public record ContentRange(
        String unit,
        long start,
        long end,
        long total
) {
    public static final String BYTES = "bytes";

    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "^(?<unit>\\w+)\\s+(?:(?<start>\\d+)-(?<end>\\d+)|\\*)/(?:(?<total>\\d+)|\\*)$"
    );

    public ContentRange {
        Objects.requireNonNull(unit, "unit must not be null");
        if (unit.isBlank()) {
            throw new IllegalArgumentException("unit must not be blank");
        }
        if ((start < 0) != (end < 0)) {
            throw new IllegalArgumentException("Start and end positions must both be specified or unspecified");
        }
        if (start >= 0 && start > end) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Start position (%d) cannot be greater than end position (%d)", start, end));
        }
    }

    public static ContentRange parse(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            throw new IllegalArgumentException("Header value must not be null or blank");
        }

        final var matcher = HEADER_PATTERN.matcher(headerValue.strip());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Content-Range format: " + headerValue);
        }

        final var unit = matcher.group("unit");
        final var start = parseLongOrDefault(matcher.group("start"));
        final var end = parseLongOrDefault(matcher.group("end"));
        final var total = parseLongOrDefault(matcher.group("total"));

        return new ContentRange(unit, start, end, total);
    }

    private static long parseLongOrDefault(String value) {
        return (value != null) ? Long.parseLong(value) : -1;
    }

    public long span() {
        return start >= 0 && end >= 0 ? (end - start + 1) : -1;
    }

    public long available() {
        if (total >= 0) return total;
        return span();
    }
}