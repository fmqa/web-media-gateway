package com.github.fmqa.spu.api;

import com.github.fmqa.spu.io.JitterBuffer;
import com.github.fmqa.spu.media.pcm.L16HTTPResource;
import com.github.fmqa.spu.support.Announcer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Duration;
import java.util.Map;

/**
 * API for non-blocking, jitter-buffered streaming of s16 PCM data.
 * @param jitter jitter buffered IO implementation
 */
@Controller
public record Jitter(JitterBuffer jitter) {
    private static final String AUDIO_L16_TYPE = "audio/l16";

    @GetMapping(value = {"/jitter", "/jitter.l16"}, produces = AUDIO_L16_TYPE)
    public StreamingResponseBody jitter(
            Announcer announcer,
            @RequestParam L16HTTPResource source,
            @RequestParam(required = false) Duration start,
            @RequestParam(required = false) Integer f,
            @RequestParam(required = false) Integer target,
            @RequestParam(required = false) Integer max) {
        final var resource = start == null || start.isZero() ? source : source.seek(start);
        final var parameters = Map.of(
                "rate", Integer.toString(resource.rate()),
                "channels", Integer.toString(resource.channels())
        );
        final var content = new MediaType("audio", "l16", parameters);
        announcer.announce(content, source.duration());
        return dst -> {
            try (final var in = resource.open()) {
                jitter.copy(
                        2 * resource.rate() * resource.channels(),
                        Duration.ofMillis(f == null || f <= 0 ? 20 : f),
                        Duration.ofMillis(target == null || target <= 0 ? 60 : target),
                        Duration.ofMillis(max == null || max <= 0 ? 200 : max),
                        in, dst
                );
            } catch (InterruptedException e) {
                return;
            }
        };
    }
}
