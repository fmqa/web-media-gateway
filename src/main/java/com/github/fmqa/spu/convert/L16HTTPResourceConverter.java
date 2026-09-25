package com.github.fmqa.spu.convert;

import com.github.fmqa.spu.media.pcm.L16HTTPResource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;

/**
 * Converts a given URI to a {@link L16HTTPResource}, if the referenced resource is of the proper type.
 * @param client HTTP client to use for probing/fetching the resource
 */
@Component
public record L16HTTPResourceConverter(HttpClient client) implements Converter<URI, L16HTTPResource> {
    @Override
    public L16HTTPResource convert(URI source) {
        final L16HTTPResource result;
        try {
            result = L16HTTPResource.from(client, source);
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
        if (result == null) {
            throw new IllegalArgumentException("Could not convert " + source + " to audio/l16");
        }
        return result;
    }
}
