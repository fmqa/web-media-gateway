package com.github.fmqa.spu.media.temporal;

import com.github.fmqa.spu.units.Durations;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Estimates the duration of media resources via ffprobe(1).
 * @param timeout duration to wait for the ffprobe process
 */
public record FFEstimator(Duration timeout) implements Estimator {
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
                if (!process.waitFor(timeout) || process.exitValue() != 0) {
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
            } catch (IOException _) {
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
}
