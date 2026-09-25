package com.github.fmqa.spu.media;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * Wraps an {@link FFInputable}, adding the capability to determine the underlying resource's duration by probing it.
 * @param client The HTTP client to use for probing the resource
 * @param input The wrapped resource
 * @see Probes#duration(HttpClient, URI)
 */
public record FFProbedURLInput(HttpClient client, FFInputable input) implements FFInputable {
    @Override
    public URI uri() {
        return input.uri();
    }

    @Override
    public List<String> ffmpeg(Connector connector) {
        return input.ffmpeg(connector);
    }

    @Override
    public FFProbedURLInput seek(Duration start) {
        return new FFProbedURLInput(client, input.seek(start));
    }

    @Override
    public Duration duration() throws IOException, InterruptedException {
        return Probes.duration(client, input.uri());
    }
}
