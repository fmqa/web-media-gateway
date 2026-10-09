package com.github.fmqa.spu.media.temporal;

import com.github.fmqa.spu.units.Durations;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

record ContentDurationHeaderEstimator(HttpClient client) implements Estimator {
    private static final String DURATION_HEADER = "Content-Duration";
    
    private static Optional<Double> parse(String value) {
        try {
            return Optional.of(Double.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Duration header(String method, URI uri) throws IOException, InterruptedException {
        final var request = HttpRequest
                .newBuilder(uri)
                .header("Range", "bytes=0-")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();

        final var response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 405 && "HEAD".equals(method)) {
            return header("GET", uri);
        }
        if (response.statusCode() != 200 && response.statusCode() != 206) {
            return null;
        }
        final var value = response.headers().firstValue(DURATION_HEADER).flatMap(ContentDurationHeaderEstimator::parse).map(Durations::fromSeconds);
        return value.orElse(null);
    }

    @Override
    public Duration duration(URI uri) throws IOException, InterruptedException {
        return header("HEAD", uri);
    }
}
