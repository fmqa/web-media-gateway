package com.github.fmqa.spu.media;

import com.github.fmqa.spu.units.Durations;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Provides methods for probing resource metadata (such as duration).
 */
public class Probes {
    private static final String DURATION_HEADER = "Content-Duration";

    private Probes() { }

    static Duration content(URI uri) {
        final var builder = new ProcessBuilder(
                "ffprobe",
                "-v","error",
                "-show_entries", "format=duration",
                "-of","default=noprint_wrappers=1:nokey=1",
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
            } catch (IOException ignored) {}
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

    static Optional<Double> parse(String value) {
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static Duration header(HttpClient client, String method, URI uri) throws IOException, InterruptedException {
        final var request = HttpRequest
                .newBuilder(uri)
                .header("Range", "bytes=0-")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();

        final var response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 405 && "HEAD".equals(method)) {
            return header(client, "GET", uri);
        }
        if (response.statusCode() != 200 && response.statusCode() != 206) {
            return null;
        }
        final var value = response.headers().firstValue(DURATION_HEADER).flatMap(Probes::parse).map(Durations::fromSeconds);
        return value.orElse(null);
    }

    static Duration header(HttpClient client, URI uri) throws IOException, InterruptedException {
        return header(client, "HEAD", uri);
    }

    /**
     * Query the duration of the resource referenced by the given (HTTP/HTTPS) URI.
     * @param client HTTP client to query the resource
     * @param uri HTTP/HTTPS URI referencing a resource
     * @return The resource's duration, or {@code null} if the duration can't be determined
     * @throws IOException If an I/O error occurs while querying the resource
     * @throws InterruptedException If the querying process has been interrupted
     */
    public static Duration duration(HttpClient client, URI uri) throws IOException, InterruptedException {
        var value = header(client, uri);
        if (value == null) {
            value = content(uri);
        }
        return value;
    }
}
