package com.github.fmqa.spu.api;

import com.github.fmqa.spu.av.Bitrate;
import com.github.fmqa.spu.av.M4AStreamBuilder;
import com.github.fmqa.spu.av.MP3StreamBuilder;
import com.github.fmqa.spu.av.MP4StreamBuilder;
import com.github.fmqa.spu.av.Monitorable;
import com.github.fmqa.spu.av.Preset;
import com.github.fmqa.spu.av.Quality;
import com.github.fmqa.spu.av.WavStreamBuilder;
import com.github.fmqa.spu.media.ffmpeg.FFInputable;
import com.github.fmqa.spu.media.Fragments;
import com.github.fmqa.spu.support.Announcer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;

/**
 * HTTP pseudo-streaming API for streaming A/V media in common, web-compatible formats.
 */
@Controller
public record Streaming(
    ObjectFactory<WavStreamBuilder> wavFactory,
    ObjectFactory<MP3StreamBuilder> mp3Factory,
    ObjectFactory<M4AStreamBuilder> m4aFactory,
    ObjectFactory<MP4StreamBuilder> mp4Factory) implements Monitorable
{
    private static final Logger log = LoggerFactory.getLogger(Streaming.class);
    
    @GetMapping(value = {"/audio", "/audio.wav"}, produces = WavStreamBuilder.AUDIO_WAV_TYPE)
    public StreamingResponseBody wav(Announcer announcer, @RequestParam FFInputable source, @RequestParam(required = false) Duration start) throws IOException, InterruptedException {
        final WavStreamBuilder builder = wavFactory.getObject();
        builder.monitor(this);
        builder.input(source, start);
        builder.delay(Fragments.delay(source.uri()));
        builder.delete(Fragments.ranges(source.uri()));
        return builder.build(announcer);
    }

    @GetMapping(value = {"/audio", "/audio.mp3"}, produces = MP3StreamBuilder.AUDIO_MPEG_TYPE)
    public StreamingResponseBody mp3(
            Announcer announcer,
            @RequestParam FFInputable source,
            @RequestParam(required = false) Duration start,
            @RequestParam(required = false) Quality q,
            @RequestParam(required = false) Bitrate b) throws IOException, InterruptedException
    {
        final MP3StreamBuilder builder = mp3Factory.getObject();
        builder.monitor(this);
        builder.input(source, start);
        builder.delay(Fragments.delay(source.uri()));
        builder.delete(Fragments.ranges(source.uri()));
        builder.bitrate(b);
        builder.quality(q);
        return builder.build(announcer);
    }

    @GetMapping(value = {"/audio", "/audio.mp4a"}, produces = M4AStreamBuilder.AUDIO_MP4_TYPE, params = "!selector")
    public StreamingResponseBody mp4a(
            Announcer announcer,
            @RequestParam FFInputable source,
            @RequestParam(required = false) Duration start,
            @RequestParam(required = false) Quality q,
            @RequestParam(required = false) Bitrate b) throws IOException, InterruptedException
    {
        final M4AStreamBuilder builder = m4aFactory.getObject();
        builder.monitor(this);
        builder.input(source, start);
        builder.delay(Fragments.delay(source.uri()));
        builder.delete(Fragments.ranges(source.uri()));
        builder.bitrate(b);
        builder.quality(q);
        return builder.build(announcer);
    }

    @GetMapping(value = {"/audio", "/audio.js", "/audio.mp4a", "/audio.mp4a.js"}, produces = "application/javascript")
    public String mp4ajs(Model model,
                         @RequestParam URI source,
                         @RequestParam(required = false) Quality q,
                         @RequestParam(required = false) Bitrate b,
                         @RequestParam String selector)
    {
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
        model.addAttribute("mediatype", M4AStreamBuilder.AUDIO_MP4.toString());
        return "streaming.js";
    }

    @GetMapping(value = {"/video", "/video.mp4"}, produces = MP4StreamBuilder.VIDEO_MP4_TYPE, params = "!selector")
    public StreamingResponseBody mp4(
            Announcer announcer,
            @RequestParam FFInputable source,
            @RequestParam(required = false) Duration start,
            @RequestParam(required = false) Quality q,
            @RequestParam(required = false) Bitrate b,
            @RequestParam(required = false) Preset preset) throws IOException, InterruptedException
    {
        final MP4StreamBuilder builder = mp4Factory.getObject();
        builder.monitor(this);
        builder.input(source, start);
        builder.delay(Fragments.delay(source.uri()));
        builder.delete(Fragments.ranges(source.uri()));
        builder.bitrate(b);
        builder.quality(q);
        return builder.build(announcer);
    }

    @GetMapping(value = {"/video", "/video.js", "/video.mp4", "/video.mp4.js"}, produces = "application/javascript")
    public String mp4(Model model,
                      @RequestParam URI source,
                      @RequestParam(required = false) Quality q,
                      @RequestParam(required = false) Bitrate b,
                      @RequestParam(required = false) Preset preset,
                      @RequestParam String selector)
    {
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
        model.addAttribute("mediatype", MP4StreamBuilder.VIDEO_MP4.toString());
        model.addAttribute("elementSelector", selector);
        return "streaming.js";
    }

    @Override
    public void started(long pid, List<String> argv) {
        log.info("FFmpeg ({}) started with arguments: {}", pid, String.join(" ", argv));
    }

    @Override
    public void exited(long pid, int code) {
        log.info("FFmpeg ({}) exited with {}", pid, code);
    }
}
