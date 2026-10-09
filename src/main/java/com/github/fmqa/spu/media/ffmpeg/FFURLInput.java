package com.github.fmqa.spu.media.ffmpeg;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * Represents a resource which can be processed by FFmpeg.
 * @param uri The URI referencing the resource
 * @param start The (temporal) position to start processing the resource from
 */
public record FFURLInput(URI uri, Duration start) implements FFInputable {
    private URI clean() {
        return UriComponentsBuilder.fromUri(uri).fragment(null).build(true).toUri();
    }

    @Override
    public List<String> ffmpeg(Connector connector) {
        final var uri = clean();
        return start == null || start.isZero()
                ? List.of("-i", uri.toString())
                : List.of("-ss", start.toMillis() + "ms", "-i", uri.toString());
    }

    @Override
    public FFURLInput seek(Duration start) {
        return new FFURLInput(uri, start);
    }
}
