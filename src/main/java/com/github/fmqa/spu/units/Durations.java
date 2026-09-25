package com.github.fmqa.spu.units;

import java.time.Duration;

/**
 * Helper procedures for converting from/to {@link Duration}.
 * @see Duration
 */
public class Durations {
    private Durations() { }

    /**
     * Convert the given duration to seconds, including subsecond fractions.
     * @param duration Duration to convert
     * @return The given duration in seconds, incl. subsecond fractions
     */
    public static double toSeconds(Duration duration) {
        return duration.getSeconds() + (duration.getNano() / 1e9);
    }

    /**
     * Convert the given amount of seconds (including subsecond fractions) to a {@link Duration} object.
     * @param seconds Duration (in seconds) to convert
     * @return Converted duration object
     */
    public static Duration fromSeconds(double seconds) {
        final var whole = (long) seconds;
        final var frac = (seconds - whole) * 1e9;
        return Duration.ofSeconds(whole, (long) frac);
    }
}
