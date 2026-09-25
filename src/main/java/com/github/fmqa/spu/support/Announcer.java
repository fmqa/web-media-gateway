package com.github.fmqa.spu.support;

import com.github.fmqa.spu.units.Durations;
import org.springframework.http.MediaType;

import java.time.Duration;

/**
 * Identifies a media resource to consumers.
 */
@FunctionalInterface
public interface Announcer {
    /**
     * Identifies a media resource.
     * @param content The content type of the media resource
     * @param seconds The duration of the media resource in seconds - may be NaN or +/-Infinity if the resource
     *                has an unknown duration
     */
    void announce(String content, double seconds);

    /**
     * Identifies a media resource.
     * @param content The content type of the media resource
     * @param duration The duration of the media resource, or {@code null} if the resource has unknown duration
     */
    default void announce(String content, Duration duration) {
        announce(content, duration == null ? Double.NEGATIVE_INFINITY : Durations.toSeconds(duration));
    }

    /**
     * Identifies a media resource.
     * @param content The content type of the media resource
     * @param seconds The duration of the media resource in seconds - may be NaN or +/-Infinity if the resource
     *                has an unknown duration
     */
    default void announce(MediaType content, double seconds) {
        announce(content.toString(), seconds);
    }

    /**
     * Identifies a media resource.
     * @param content The content type of the media resource
     * @param duration The duration of the media resource, or {@code null} if the resource has unknown duration
     */
    default void announce(MediaType content, Duration duration) {
        announce(content.toString(), duration);
    }
}
