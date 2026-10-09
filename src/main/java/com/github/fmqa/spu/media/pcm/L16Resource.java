package com.github.fmqa.spu.media.pcm;

import com.github.fmqa.spu.media.common.ContentRange;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Locale;

import static java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpResponse.BodyHandlers;

/**
 * RFC2586 (audio/l16) audio resource.
 * @param request Request used to fetch the audio resource
 * @param rate The sampling rate of the audio signal
 * @param channels The number of audio channels
 * @param bytes The resource's length in bytes, or {@code -1} if undefined/indeterminate
 */
public record L16Resource(HttpRequest request, int rate, int channels, long bytes) {
    private static final MediaType AUDIO_L16 = new MediaType("audio", "l16");

    private static int rateOf(MediaType arg) {
        final var rate = arg.getParameter("rate");
        return rate == null ? 8000 : Integer.parseInt(rate);
    }

    private static int channelsOf(MediaType arg) {
        final var rate = arg.getParameter("channels");
        return rate == null ? 1 : Integer.parseInt(rate);
    }

    private static HttpRequest get(URI uri, long start) {
        return HttpRequest
                .newBuilder(uri)
                .header("Range", String.format(Locale.ROOT,"bytes=%d-", start))
                .GET()
                .build();
    }

    private static HttpRequest get(URI uri) {
        return get(uri, 0);
    }

    private static L16Resource method(HttpClient client, String method, URI uri) throws IOException, InterruptedException {
        final var request = HttpRequest
                .newBuilder(uri)
                .header("Range", "bytes=0-")
                .method(method, BodyPublishers.noBody())
                .build();

        final var response = client.send(request, BodyHandlers.discarding());
        if (response.statusCode() == 405 && "HEAD".equals(method)) {
            return method(client, "GET", uri);
        }
        if (response.statusCode() != 200 && response.statusCode() != 206) {
            return null;
        }

        final var contentType = response.headers().firstValue("Content-Type").map(MediaType::parseMediaType).orElse(null);
        if (contentType == null || !AUDIO_L16.isCompatibleWith(contentType)) {
            return null;
        }

        var length = response
                .headers()
                .firstValueAsLong("Content-Length")
                .orElse(-1);
        if (length < 0) {
            length = response
                    .headers()
                    .firstValue("Content-Range")
                    .map(ContentRange::parse)
                    .filter(r -> ContentRange.BYTES.equals(r.unit()))
                    .map(ContentRange::available)
                    .orElse(-1L);
        }

        return new L16Resource(get(uri), rateOf(contentType), channelsOf(contentType), length);
    }

    /**
     * Constructs an audio/l16 resource from the given URI, content type, and byte length specification.
     * @param uri The URI referencing this resource
     * @param contentType The audio/l16 content type of the resource
     * @param bytes The size of the resource in bytes, or {@code -1} if unknown
     * @return An audio/l16 resource with the given parameters, or {@code null} if {@code contentType} is not compatible with
     * audio/l16
     */
    public static L16Resource from(URI uri, MediaType contentType, long bytes) {
        if (contentType == null || !AUDIO_L16.isCompatibleWith(contentType)) {
            return null;
        }
        return new L16Resource(get(uri), rateOf(contentType), channelsOf(contentType), bytes);
    }

    /**
     * Constructs an audio/l16 resource from the given HTTP/HTTPS URI.
     * <p></p>
     * The resource's parameters (rate, channels, length in bytes) are determined by performing a HEAD request against
     * the URI. If the HEAD method is not supported by the URI, a GET request will be performed as a fallback.
     * @param client The client to use for issuing the probing request
     * @param uri The URI referencing the audio/l16 resource
     * @return An audio/l16 resource backed by the given URI, or {@code null} if the resource referenced by the given
     * URI is not an audio/l16 resource
     * @throws IOException If an I/O error occurs while probing resource parameters
     * @throws InterruptedException If the probing process has been interrupted
     */
    public static L16Resource from(HttpClient client, URI uri) throws IOException, InterruptedException {
        return method(client, "HEAD", uri);
    }

    /**
     * Returns a new audio/l16 (sub)resource starting at the given (temporal) position.
     * @param start The position to start the returned subresource from
     * @return A new audio/l16 resource starting from the given start position
     */
    public L16Resource seek(Duration start) {
        final var full = start.toSeconds() * rate;
        final var part = ((double) start.toNanosPart() / 1.0e9) * (double) rate;
        final var frames = full + (long) part;
        final var offset = 2 * channels * frames;
        return seek(offset);
    }

    /**
     * Returns a new audio/l16 (sub)resource starting at the given byte offset.
     * @param start The byte offset to start the returned subresource from
     * @return A new audio/l16 resource starting from the given byte offset
     */
    public L16Resource seek(long start) {
        return new L16Resource(get(request.uri(), start), rate, channels, bytes);
    }

    /**
     * Return the audio/l16 resource's duration.
     * @return The duration of the resource, or {@code null} if the duration can't be determined
     */
    public Duration duration() {
        if (bytes < 0) {
            return null;
        }
        final var bps = 2 * rate * channels;
        final var s = bytes / (double) bps;
        final var whole = (long) s;
        final var frac = (s - whole) * 1e9;
        return Duration.ofSeconds(whole, (long) frac);
    }
}
