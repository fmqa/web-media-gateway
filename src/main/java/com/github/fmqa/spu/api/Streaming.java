package com.github.fmqa.spu.api;

import com.github.fmqa.spu.io.Piper;
import com.github.fmqa.spu.media.FFInputable;
import com.github.fmqa.spu.media.FFRanges;
import com.github.fmqa.spu.media.Fragments;
import com.github.fmqa.spu.media.StandardInputConnector;
import com.github.fmqa.spu.support.Announcer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * HTTP pseudo-streaming API for streaming A/V media in common, web-compatible formats.
 * @param piper used for IPC with FFmpeg processes
 */
@Controller
public record Streaming(Piper piper) {
    private static final String AUDIO_WAV_TYPE = "audio/x-wav";
    private static final MediaType AUDIO_WAV = MediaType.parseMediaType(AUDIO_WAV_TYPE);

    static Duration orZero(Duration duration) {
        return duration == null ? Duration.ZERO : duration;
    }

    static Duration announce(Announcer announcer, MediaType content, FFInputable source, Duration start) throws IOException, InterruptedException {
        final var delay = Fragments.delay(source.uri());
        announcer.announce(content, orZero(source.duration()).plus(delay));
        return delay.minus(start == null ? Duration.ZERO : start);
    }

    StreamingResponseBody process(StandardInputConnector connector, List<String> argv) {
        final var builder = new ProcessBuilder(argv);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        return dst -> {
            try (final var ffmpeg = builder.start()) {
                piper.pipe(connector.open(), ffmpeg.getOutputStream(), ffmpeg.getInputStream(), dst);
            } catch (InterruptedException e) {
                return;
            }
        };
    }

    StreamingResponseBody audio(Announcer announcer, MediaType content, FFInputable source, Duration start, Collection<String> args) throws IOException, InterruptedException {
        final var ranges = FFRanges.toFFArg(Fragments.ranges(source.uri()));
        final var delay = announce(announcer, content, source, start);
        start = delay.negated();
        final var ff = start == null || start.isZero() ? source : source.seek(start);
        final var argv = new ArrayList<String>();
        Collections.addAll(argv, "ffmpeg", "-nostdin", "-vn");
        final var connector = new StandardInputConnector();
        argv.addAll(ff.ffmpeg(connector));
        final var filters = new ArrayList<String>();
        if (delay.isPositive()) {
            filters.add(String.format(Locale.ROOT, "adelay=%dms:all=1", delay.toMillis()));
        }
        if (ranges != null && !ranges.isEmpty()) {
            filters.add(String.format(Locale.ROOT, "volume=0:enable='%s'", ranges));
        }
        if (!filters.isEmpty()) {
            Collections.addAll(argv, "-af", String.join(",", filters));
        }
        argv.addAll(args);
        return process(connector, argv);
    }

    @GetMapping(value = {"/audio", "/audio.wav"}, produces = AUDIO_WAV_TYPE)
    public StreamingResponseBody wav(Announcer announcer, @RequestParam FFInputable source, @RequestParam(required = false) Duration start) throws IOException, InterruptedException {
        return audio(announcer, AUDIO_WAV, source, start, List.of("-f", "wav", "pipe:1"));
    }

    private static final String AUDIO_MPEG_TYPE = "audio/mpeg";
    private static final MediaType AUDIO_MPEG = MediaType.parseMediaType(AUDIO_MPEG_TYPE);

    @GetMapping(value = {"/audio", "/audio.mp3"}, produces = AUDIO_MPEG_TYPE)
    public StreamingResponseBody mp3(
            Announcer announcer,
            @RequestParam FFInputable source,
            @RequestParam(required = false) Duration start,
            @RequestParam(required = false) Quality q,
            @RequestParam(required = false) Bitrate b) throws IOException, InterruptedException {
        final var argv = new ArrayList<String>();
        Collections.addAll(argv, "-c:a", "libmp3lame");
        if (b != null) {
            Collections.addAll(argv, "-b:a", b.value());
        } else if (q != null) {
            Collections.addAll(argv, "-q:a", String.valueOf(q.value()));
        }
        Collections.addAll(argv, "-f", "mp3", "pipe:1");
        return audio(announcer, AUDIO_MPEG, source, start, argv);
    }

    private static final String AUDIO_MP4_TYPE = "audio/mp4; codecs=\"mp4a.40.2\"";
    private static final MediaType AUDIO_MP4 = MediaType.parseMediaType(AUDIO_MP4_TYPE);

    @GetMapping(value = {"/audio", "/audio.mp4a"}, produces = AUDIO_MP4_TYPE, params = "!selector")
    public StreamingResponseBody mp4a(
            Announcer announcer,
            @RequestParam FFInputable source,
            @RequestParam(required = false) Duration start,
            @RequestParam(required = false) Quality q,
            @RequestParam(required = false) Bitrate b) throws IOException, InterruptedException {
        final var argv = new ArrayList<String>();
        Collections.addAll(argv , "-c:a", "aac");
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
                "-f", "mp4", "pipe:1"
        );
        return audio(announcer, AUDIO_MP4, source, start, argv);
    }

    @GetMapping(value = {"/audio", "/audio.js", "/audio.mp4a", "/audio.mp4a.js"}, produces = "application/javascript")
    public String mp4ajs(Model model,
                         @RequestParam URI source,
                         @RequestParam(required = false) Quality q,
                         @RequestParam(required = false) Bitrate b,
                         @RequestParam String selector) {
        model.addAttribute("streamUrl", MvcUriComponentsBuilder
                .fromController(getClass())
                .pathSegment("audio.mp4a")
                .queryParam("source", "{source}")
                .queryParamIfPresent("q", Optional.ofNullable(q))
                .queryParamIfPresent("b", Optional.ofNullable(b))
                .encode()
                .buildAndExpand(source)
                .toUri());
        model.addAttribute("elementSelector", selector);
        model.addAttribute("mediatype", AUDIO_MP4.toString());
        return "streaming.js";
    }

    private static final String VIDEO_MP4_TYPE = "video/mp4; codecs=\"avc1.42E01E, mp4a.40.2\"";
    private static final MediaType VIDEO_MP4 = MediaType.parseMediaType(VIDEO_MP4_TYPE);

    @GetMapping(value = {"/video", "/video.mp4"}, produces = VIDEO_MP4_TYPE, params = "!selector")
    public StreamingResponseBody mp4(
            Announcer announcer,
            @RequestParam FFInputable source,
            @RequestParam(required = false) Duration start,
            @RequestParam(required = false) Quality q,
            @RequestParam(required = false) Bitrate b,
            @RequestParam(required = false) Preset preset) throws IOException, InterruptedException {
        announcer.announce(VIDEO_MP4, source.duration());
        final var ff = start == null ? source : source.seek(start);
        final var argv = new ArrayList<String>();
        Collections.addAll(argv, "ffmpeg", "-nostdin");
        final var connector = new StandardInputConnector();
        argv.addAll(ff.ffmpeg(connector));
        Collections.addAll(argv, "-profile:v", "baseline", "-pix_fmt", "yuv420p", "-c:a", "aac");
        if (b != null) {
            Collections.addAll(argv, "-b:a", b.value());
        } else if (q != null) {
            Collections.addAll(argv, "-q:a", String.valueOf(q.value()));
        }
        if (preset != null) {
            Collections.addAll(argv, "-preset", preset.toString());
        }
        Collections.addAll(
                argv,
                "-movflags", "empty_moov+default_base_moof+frag_keyframe+skip_sidx",
                "-frag_duration", "100000",
                "-flush_packets", "1",
                "-f", "mp4", "pipe:1"
        );
        return process(connector, argv);
    }

    @GetMapping(value = {"/video", "/video.js", "/video.mp4", "/video.mp4.js"}, produces = "application/javascript")
    public String mp4(Model model,
                      @RequestParam URI source,
                      @RequestParam(required = false) Quality q,
                      @RequestParam(required = false) Bitrate b,
                      @RequestParam(required = false) Preset preset,
                      @RequestParam String selector) {
        model.addAttribute("streamUrl", MvcUriComponentsBuilder
                .fromController(getClass())
                .pathSegment("video.mp4")
                .queryParam("source", "{source}")
                .queryParamIfPresent("q", Optional.ofNullable(q))
                .queryParamIfPresent("b", Optional.ofNullable(b))
                .queryParamIfPresent("preset", Optional.ofNullable(preset == null ? null : preset.toString().toLowerCase(Locale.ROOT)))
                .encode()
                .buildAndExpand(source)
                .toUri());
        model.addAttribute("mediatype", VIDEO_MP4.toString());
        model.addAttribute("elementSelector", selector);
        return "streaming.js";
    }
}
