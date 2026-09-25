package com.github.fmqa.spu.api;

import com.github.fmqa.spu.convert.FFBoxedInput;
import com.github.fmqa.spu.media.Connectors;
import com.github.fmqa.spu.media.FFInputable;
import com.github.fmqa.spu.media.Fragments;
import com.github.fmqa.spu.support.Announcer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * HTTP pseudo-streaming API for building mixed audio streams consisting of multiple inputs.
 */
@Controller
public class Mixing {
    private static final String AUDIO_WAV_TYPE = "audio/x-wav";
    private static final MediaType AUDIO_WAV = MediaType.parseMediaType(AUDIO_WAV_TYPE);

    static Duration duration(FFInputable ff) {
        try {
            return ff.duration();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    StreamingResponseBody mix(MediaType content, List<String> format, Announcer announcer, FFBoxedInput[] inlets, Duration start) {
        final var durations = Stream.of(inlets).parallel().map(Mixing::duration).toList();
        announcer.announce(content, durations.isEmpty() || durations.stream().anyMatch(Objects::isNull) ? null : Collections.max(durations));
        final var mix = new StringBuilder();
        int index = 0;
        final var filter = new StringBuilder();
        for (int i = 0; i < inlets.length; i++) {
            if (start != null) {
                inlets[i] = inlets[i].seek(start);
            }
            filter.append(String.format(Locale.ROOT, "[%d:a]", index++));
            filter.append("aformat=channel_layouts=mono");
            final var uri = inlets[i].uri();
            final var gain = Fragments.gain(uri);
            final var balance = Fragments.balance(uri).orElse(0.5);
            gain.ifPresent(vol -> filter.append(",volume=").append(vol));
            filter.append(String.format(Locale.ROOT, ",pan=stereo|c0=%.1f*c0|c1=%.1f*c0", balance, 1 - balance));
            final var ch = String.format(Locale.ROOT, "[s%d]", i);
            filter.append(ch).append(';');
            mix.append(ch);
        }
        filter.append(mix).append(String.format(Locale.ROOT, "amix=inputs=%d:normalize=0,alimiter=limit=0.95:asc=1[aout]", inlets.length));
        final var argv = new ArrayList<String>();
        Collections.addAll(argv, "ffmpeg", "-nostdin", "-vn");
        for (final var ff : inlets) argv.addAll(ff.ffmpeg(Connectors.REJECT));
        Collections.addAll(
                argv,
                "-filter_complex", filter.toString(),
                "-map", "[aout]"
        );
        argv.addAll(format);
        argv.add("pipe:1");
        final var builder = new ProcessBuilder(argv);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        return dst -> {
            try (final var process = builder.start(); final var out = process.getInputStream(); final var in = process.getOutputStream()) {
                in.close();
                out.transferTo(dst);
                dst.flush();
            }
        };
    }

    @GetMapping(value = "/stereo.wav", produces = AUDIO_WAV_TYPE)
    public StreamingResponseBody wav(Announcer announcer, @RequestParam FFBoxedInput[] source, @RequestParam(required = false) Duration start) {
        return mix(AUDIO_WAV, List.of("-f", "wav"), announcer, source, start);
    }

    private static final String AUDIO_MPEG_TYPE = "audio/mpeg";
    private static final MediaType AUDIO_MPEG = MediaType.parseMediaType(AUDIO_MPEG_TYPE);

    @GetMapping(value = "/stereo.mp3", produces = AUDIO_MPEG_TYPE)
    public StreamingResponseBody mp3(
            Announcer announcer,
            @RequestParam FFBoxedInput[] source,
            @RequestParam(required = false) Quality q,
            @RequestParam(required = false) Bitrate b,
            @RequestParam(required = false) Duration start) {
        final var argv = new ArrayList<String>();
        Collections.addAll(argv, "-c:a", "libmp3lame");
        if (b != null) {
            Collections.addAll(argv, "-b:a", b.value());
        } else if (q != null) {
            Collections.addAll(argv, "-q:a", String.valueOf(q.value()));
        }
        Collections.addAll(argv, "-f", "mp3");
        return mix(AUDIO_MPEG, argv, announcer, source, start);
    }

    private static final String AUDIO_MP4_TYPE = "audio/mp4; codecs=\"mp4a.40.2\"";
    private static final MediaType AUDIO_MP4 = MediaType.parseMediaType(AUDIO_MP4_TYPE);

    @GetMapping(value = "/stereo.mp4a", produces = AUDIO_MP4_TYPE, params = "!selector")
    public StreamingResponseBody mp4a(
            Announcer announcer,
            @RequestParam FFBoxedInput[] source,
            @RequestParam(required = false) Quality q,
            @RequestParam(required = false) Bitrate b,
            @RequestParam(required = false) Duration start) {
        final var argv = new ArrayList<String>();
        Collections.addAll(argv, "-c:a", "aac");
        if (b != null) {
            Collections.addAll(argv, "-b:a", b.value());
        } else if (q != null) {
            Collections.addAll(argv, "-q:a", String.valueOf(q.value()));
        }
        Collections.addAll(
                argv,
                "-movflags", "empty_moov+default_base_moof+frag_keyframe+skip_sidx",
                "-frag_duration", "100000",
                "-flush_packets", "1",
                "-f", "mp4"
        );
        return mix(AUDIO_MP4, argv, announcer, source, start);
    }

    @GetMapping(value = {"/stereo", "/stereo.js", "/stereo.mp4a", "/stereo.mp4a.js"}, produces = "application/javascript")
    public String mp4ajs(Model model,
                         @RequestParam URI[] source,
                         @RequestParam String selector,
                         @RequestParam(required = false) Quality q,
                         @RequestParam(required = false) Bitrate b) {
        model.addAttribute("streamUrl", MvcUriComponentsBuilder
                .fromController(getClass())
                .pathSegment("stereo.mp4a")
                .queryParam("source", (Object[]) source)
                .queryParamIfPresent("q", Optional.ofNullable(q))
                .queryParamIfPresent("b", Optional.ofNullable(b))
                .encode()
                .build()
                .toUri());
        model.addAttribute("elementSelector", selector);
        model.addAttribute("mediatype", AUDIO_MP4.toString());
        return "streaming.js";
    }
}
