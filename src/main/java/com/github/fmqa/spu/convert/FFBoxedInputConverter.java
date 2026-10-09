package com.github.fmqa.spu.convert;

import com.github.fmqa.spu.api.Streaming;
import com.github.fmqa.spu.io.Loopback;
import com.github.fmqa.spu.media.ffmpeg.ConnectionRejectedException;
import com.github.fmqa.spu.media.ffmpeg.Connectors;
import com.github.fmqa.spu.media.ffmpeg.FFInputable;
import com.github.fmqa.spu.media.ffmpeg.FFPseudoStreamInput;
import com.github.fmqa.spu.media.Fragments;
import com.github.fmqa.spu.media.temporal.Estimator;
import com.github.fmqa.spu.media.temporal.Estimators;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;

/**
 * Converts {@link FFInputable} objects to boxed representations that can be read by FFmpeg directly via their
 * references, without requiring piped I/O.
 * <p></p>
 * Note that the {@link FFInputable} produced by this converter may issue loopback calls, creating requests to the
 * running server application itself.
 */
@Component
public record FFBoxedInputConverter(HttpClient client, FFInputConverter converter, Estimator estimator, @Lazy Loopback lo) implements Converter<URI, FFBoxedInput> {
    URI wav(URI uri) {
        return MvcUriComponentsBuilder
                .fromController(UriComponentsBuilder.fromUri(lo.uri()), Streaming.class)
                .pathSegment("audio.wav")
                .queryParam("source", "{source}")
                .fragment(uri.getFragment())
                .encode()
                .buildAndExpand(uri)
                .toUri();
    }

    FFPseudoStreamInput pseudo(URI uri) {
        return new FFPseudoStreamInput(wav(uri));
    }

    FFInputable probed(URI uri) {
        return Estimators.wrap(pseudo(uri), estimator);
    }

    static boolean hinted(URI uri) {
        return Fragments.delay(uri).isPositive() || !Fragments.ranges(uri).isEmpty();
    }

    @Override
    public FFBoxedInput convert(URI uri) {
        if (hinted(uri)) {
            return new FFBoxedInput(probed(uri));
        }
        var input = converter.convert(uri);
        try {
            input.ffmpeg(Connectors.REJECT);
        } catch (ConnectionRejectedException ignored) {
            input = probed(uri);
        }
        return new FFBoxedInput(input);
    }
}
