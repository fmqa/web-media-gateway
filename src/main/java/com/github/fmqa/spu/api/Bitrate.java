package com.github.fmqa.spu.api;

import java.util.regex.Pattern;

/**
 * FFmpeg-compatible bit rate specifier.
 * @param value Bit rate value e.g. 320k, 320000, 120Ki, etc.
 */
public record Bitrate(String value) {
    private static final Pattern PATTERN = Pattern.compile("^\\d+(\\.\\d+)?([kKMG]i?)?$");

    public Bitrate {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid bitrate: " + value);
        }
    }

    /**
     * Return a {@link Bitrate} instance representing the given value.
     * @param value Bitrate specifier
     * @return The given value as a {@link Bitrate}
     */
    public static Bitrate valueOf(String value) {
        return new Bitrate(value);
    }

    /**
     * Alias for {@link valueOf}
     * @see valueOf
     */
    public static Bitrate of(String value) {
        return valueOf(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
