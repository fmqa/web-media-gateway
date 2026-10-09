package com.github.fmqa.spu.media.temporal;

import com.github.fmqa.spu.media.ffmpeg.FFInputable;
import com.github.fmqa.spu.units.Durations;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Define predefined strategies for estimating the duration of media resources.
 */
public enum Estimators implements Estimator {
    /**
     * Estimates the duration of media resources via ffprobe(1).
     */
    FFPROBE {
        @Override
        public Duration duration(URI uri) throws IOException, InterruptedException {
            final var builder = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                "-i", uri.toString()
            );
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            final Path output;
            try {
                output = Files.createTempFile("spu", ".duration");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            final String content;
            try {
                builder.redirectOutput(output.toFile());
                try (final var process = builder.start()) {
                    if (!process.waitFor(5, TimeUnit.SECONDS) || process.exitValue() != 0) {
                        return null;
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (InterruptedException e) {
                    return null;
                }
                try {
                    content = Files.readString(output);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } finally {
                try {
                    Files.deleteIfExists(output);
                } catch (IOException ignored) {
                }
            }
            final var trimmed = content.trim();
            final double seconds;
            try {
                seconds = Double.parseDouble(trimmed);
            } catch (NumberFormatException e) {
                return null;
            }
            return Durations.fromSeconds(seconds);
        }
    };
    
    /**
     * Combines multiple estimation strategies.
     * <p></p>
     * The resulting compositie estimator tries the given estimator sequentially, with the estimated result being the
     * first non-null one.
     * @param estimators estimation strategies
     * @return an estimator combining the given estimation strategies
     */
    public static Estimator combine(Iterable<? extends Estimator> estimators) {
        return new CombinedEstimator(estimators);
    }
    
    /**
     * Returns an estimator that uses the given HTTP client to query a given media resource's duration using HTTP
     * header metadata.
     * <p></p>
     * This attempts to parse a {@code Content-Duration} header if available.
     * @param client the HTTP client to use for the query
     * @return an estimator that performs duration queries by reading HTTP response headers
     */
    public static Estimator header(HttpClient client) {
        return new ContentDurationHeaderEstimator(client);
    }
    
    /**
     * Decorates an {@link FFInputable} with the given estimator, which will be used as a fallback in case
     * {@link FFInputable#duration()} returns {@code null}.
     * @param input the input to wrap
     * @param estimator the estimator to use a fallback for duration estimator
     * @return a media object which may fallsback onto the given duration estimator
     */
    public static FFInputable wrap(FFInputable input, Estimator estimator) {
        return new EstimatedFFInput(input, estimator);
    }
}
