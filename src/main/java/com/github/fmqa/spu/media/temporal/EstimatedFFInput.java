package com.github.fmqa.spu.media.temporal;

import com.github.fmqa.spu.media.ffmpeg.Connector;
import com.github.fmqa.spu.media.ffmpeg.FFInputable;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;

record EstimatedFFInput(FFInputable input, Estimator estimator) implements FFInputable {
    @Override
    public URI uri() {
        return input.uri();
    }

    @Override
    public List<String> ffmpeg(Connector connector) {
        return input.ffmpeg(connector);
    }

    @Override
    public FFInputable seek(Duration start) {
        return new EstimatedFFInput(input.seek(start), estimator);
    }

    @Override
    public Duration duration() throws IOException, InterruptedException {
        Duration duration = input.duration();
        if (duration == null) {
            duration = estimator.duration(input.uri());
        }
        return duration;
    }
}
