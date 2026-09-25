package com.github.fmqa.spu.api;

/**
 * Quality scale (0..9), where 0 is best/highest quality, and 9 is lowest quality.
 * @param value Quality value
 */
public record Quality(int value) {
    public Quality {
        value = Math.clamp(value, 0, 9);
    }

    /**
     * Return a {@link Quality} instance representing the given value.
     * @param value Quality value
     * @return The given value as a {@link Quality}
     */
    public static Quality valueOf(String value) {
        return new Quality(Integer.parseInt(value));
    }

    /**
     * Alias for {@link valueOf}
     * @see valueOf
     */
    public static Quality of(String value) {
        return valueOf(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
