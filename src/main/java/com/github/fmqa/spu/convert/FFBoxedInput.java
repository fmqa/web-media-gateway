package com.github.fmqa.spu.convert;

import com.github.fmqa.spu.media.Connector;
import com.github.fmqa.spu.media.FFInputable;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * An {@link FFInputable} object that can be read by FFmpeg directly via its reference, without requiring piped I/O.
 * <p></p>
 * Use {@link FFBoxedInputConverter} to convert a possibly-piped {@link FFInputable} to its boxed/reifiable
 * representation.
 */
public final class FFBoxedInput implements FFInputable {
    private final FFInputable boxed;

    FFBoxedInput(FFInputable input) {
        boxed = input;
    }

    @Override
    public URI uri() {
        return boxed.uri();
    }

    @Override
    public List<String> ffmpeg(Connector connector) {
        return boxed.ffmpeg(connector);
    }

    @Override
    public FFBoxedInput seek(Duration start) {
        return new FFBoxedInput(boxed.seek(start));
    }

    @Override
    public Duration duration() throws IOException, InterruptedException {
        return boxed.duration();
    }
}
