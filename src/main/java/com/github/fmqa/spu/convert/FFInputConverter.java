package com.github.fmqa.spu.convert;

import com.github.fmqa.spu.media.ffmpeg.FFInputable;
import com.github.fmqa.spu.media.ffmpeg.FFL16Input;
import com.github.fmqa.spu.media.ffmpeg.FFPseudoStreamInput;
import com.github.fmqa.spu.media.ffmpeg.FFURLInput;
import com.github.fmqa.spu.media.temporal.Estimator;
import com.github.fmqa.spu.media.temporal.Estimators;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;

/**
 * Converts {@link URI} objects to {@link FFInputable} objects usable for processing with FFmpeg.
 * @param client The client used to probe/query input URIs.
 */
@Component
public record FFInputConverter(HttpClient client, Estimator estimator) implements Converter<URI, FFInputable> {
    static boolean isPseudoStream(URI uri) {
        final var builder = UriComponentsBuilder.fromUri(uri);
        final var components = builder.build();
        final var query = components.getQueryParams();
        return query.containsKey("start");
    }

    FFInputable probed(FFInputable inputable) {
        return Estimators.wrap(inputable, estimator);
    }

    static boolean isSupported(URI uri) {
        final var scheme = uri.getScheme();
        return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
    }

    @Override
    public FFInputable convert(URI uri) {
        if (!isSupported(uri)) {
            return new FFURLInput(uri);
        }
        if (isPseudoStream(uri)) {
            return probed(new FFPseudoStreamInput(uri));
        }
        FFL16Input l16;
        try {
            l16 = FFL16Input.from(client, uri);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
        if (l16 != null) {
            return l16;
        }
        return probed(new FFURLInput(uri));
    }
}
