package com.github.fmqa.spu.media.pcm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import static java.net.http.HttpResponse.BodyHandlers;

/**
 * An I/O wrapper for {@link L16Resource}, providing the capability to read audio/l16 resource data.
 * @param client The client to use for reading the resource
 * @param resource The wrapped audio/l16 resource
 */
public record L16HTTPResource(HttpClient client, L16Resource resource) {
    public static L16HTTPResource from(HttpClient client, URI uri) throws IOException, InterruptedException {
        final var l16 = L16Resource.from(client, uri);
        return l16 == null ? null : new L16HTTPResource(client, l16);
    }

    /**
     * Returns the URI referencing the audio/l16 resource.
     */
    public URI uri() {
        return resource.request().uri();
    }

    /**
     * Returns the length of the resource in bytes.
     * @see L16Resource#bytes()
     */
    public long bytes() {
        return resource.bytes();
    }

    /**
     * Returns the audio format.
     * @see L16Resource#format()
     */
    public L16Format format() {
        return resource.format();
    }

    /**
     * Returns a new (sub)resource starting at the given (temporal) position.
     * @param start The position to start the returned subresource from
     * @return A new resource starting from the given start position
     * @see L16Resource#seek(Duration)
     */
    public L16HTTPResource seek(Duration start) {
        return new L16HTTPResource(client, resource.seek(start));
    }

    /**
     * Returns a new  (sub)resource starting at the given byte offset.
     * @param start The byte offset to start the returned subresource from
     * @return A new resource starting from the given byte offset
     * @see L16Resource#seek(long)
     */
    public L16HTTPResource seek(long start) {
        return new L16HTTPResource(client, resource.seek(start));
    }

    /**
     * Return the resource's duration.
     * @return The duration of the resource, or {@code null} if the duration can't be determined
     * @see L16Resource#duration()
     */
    public Duration duration() {
        return resource.duration();
    }

    /**
     * Opens this resource, returning an {@link InputStream} that can be used to read audio data.
     * @return An input stream for reading audio/l16 data
     * @throws IOException If an I/O error occurs while opening the resource
     * @throws InterruptedException If the opening process was interrupted
     */
    public InputStream open() throws IOException, InterruptedException {
        final var response = client.send(resource.request(), BodyHandlers.ofInputStream());
        if (response.statusCode() != 200 && response.statusCode() != 206) {
            return null;
        }
        return response.body();
    }
}
