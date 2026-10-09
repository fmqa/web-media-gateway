package com.github.fmqa.spu.media.temporal;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * Estimates the duration of media resources.
 */
@FunctionalInterface
public interface Estimator {
    /**
     * Estimate the duration of the media resource referred to by the given URI.
     * @param uri the media resource to probe
     * @return the duration of the media resource, or {@code null} if the duration is unknown
     * @throws IOException when an IO error occurs while attempting to determine the duration
     * @throws InterruptedException when the probing task is interrupted
     */
    Duration duration(URI uri) throws IOException, InterruptedException;
}
