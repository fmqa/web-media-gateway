package com.github.fmqa.spu.av;

import com.github.fmqa.spu.media.common.Connectors;
import com.github.fmqa.spu.media.ffmpeg.FFInputable;
import com.github.fmqa.spu.support.Announcer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

abstract class AbstractMixStreamBuilder {
    private List<? extends FFInputable> sources = Collections.emptyList();
    private Duration start = Duration.ZERO;
    
    static Duration estimate(FFInputable ff) {
        try {
            return ff.duration();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public void sources(List<? extends FFInputable> inputs, Duration at) {
        sources = inputs == null ? Collections.emptyList() : inputs;
        start = at == null ? Duration.ZERO : at;
    }
    
    public void sources(List<? extends FFInputable> inputs) {
        sources = inputs == null ? Collections.emptyList() : inputs;
        start = Duration.ZERO;
    }
    
    protected void ffmpeg(List<String> argv) {
        Collections.addAll(argv, "ffmpeg", "-nostdin", "-vn");
    }
    
    protected Duration duration() {
        return sources
            .parallelStream()
            .map(AbstractMixStreamBuilder::estimate)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }
    
    void inputs(List<String> argv, FFmMixFilterBuilder mix) {
        for (FFInputable in : sources) {
            mix.add(in.uri());
            in = in.seek(start);
            argv.addAll(in.ffmpeg(Connectors.REJECT));
        }
    }
    
    protected abstract MediaType sink(List<String> argv);
    
    protected MediaType command(List<String> argv) {
        ffmpeg(argv);
        final FFmMixFilterBuilder mix = new FFmMixFilterBuilder();
        inputs(argv, mix);
        Collections.addAll(
            argv,
            "-filter_complex", mix.build(),
            "-map", "[aout]"
        );
        return sink(argv);
    }
    
    protected StreamingResponseBody body(ProcessBuilder builder) {
        return dst -> {
            try (final var process = builder.start(); final var out = process.getInputStream(); final var in = process.getOutputStream()) {
                in.close();
                out.transferTo(dst);
                dst.flush();
            }
        };
    }
    
    public StreamingResponseBody build(Announcer announcer) {
        final List<String> argv = new ArrayList<>();
        final MediaType content = command(argv);
        
        argv.add("pipe:1");
        
        announcer.announce(content, duration());
        
        final ProcessBuilder builder = new ProcessBuilder(argv);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        
        return body(builder);
    }
}
