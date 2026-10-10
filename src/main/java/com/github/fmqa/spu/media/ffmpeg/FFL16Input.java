package com.github.fmqa.spu.media.ffmpeg;

import com.github.fmqa.spu.media.common.Connector;
import com.github.fmqa.spu.media.pcm.L16HTTPResource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * Wraps an {@link L16HTTPResource} as an {@link FFInputable}.
 * @param resource audio/l16 remote resource to be wrapped
 */
public record FFL16Input(L16HTTPResource resource) implements FFInputable {
    public static FFL16Input from(HttpClient client, URI uri) throws IOException, InterruptedException {
        final var l16http = L16HTTPResource.from(client, uri);
        return l16http == null ? null : new FFL16Input(l16http);
    }

    @Override
    public URI uri() {
        return resource.uri();
    }

    @Override
    public List<String> ffmpeg(Connector connector) {
        return List.of(
                "-f", "s16be",
                "-ar", Integer.toString(resource.format().rate()),
                "-ac", Integer.toString(resource.format().channels()),
                "-i", connector.connect(resource::open)
        );
    }

    @Override
    public FFL16Input seek(Duration start) {
        return new FFL16Input(resource.seek(start));
    }

    @Override
    public Duration duration() throws IOException, InterruptedException {
        return resource.duration();
    }
}
