package com.github.fmqa.spu.media;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Represents a media resource that supports navigation via HTTP pseudo-streaming parameters (?start=...).
 * @param uri The URI referencing the resource
 */
public record FFPseudoStreamInput(URI uri) implements FFInputable {
    private URI clean() {
        return UriComponentsBuilder.fromUri(uri).fragment(null).build(true).toUri();
    }

    @Override
    public List<String> ffmpeg(Connector connector) {
        final var uri = clean();
        return List.of("-i", uri.toString());
    }

    @Override
    public FFPseudoStreamInput seek(Duration start) {
        final var next = UriComponentsBuilder
                .fromUri(uri)
                .replaceQueryParam("start", start == null || start.isZero() ? Collections.emptyList() : Collections.singletonList(start))
                .build(true);
        return new FFPseudoStreamInput(next.toUri());
    }
}
