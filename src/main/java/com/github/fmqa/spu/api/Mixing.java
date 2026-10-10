package com.github.fmqa.spu.api;

import com.github.fmqa.spu.av.Bitrate;
import com.github.fmqa.spu.av.M4AMixStreamBuilder;
import com.github.fmqa.spu.av.MP3MixStreamBuilder;
import com.github.fmqa.spu.av.Monitorable;
import com.github.fmqa.spu.av.Quality;
import com.github.fmqa.spu.av.WavMixStreamBuilder;
import com.github.fmqa.spu.convert.FFBoxedInput;
import com.github.fmqa.spu.support.Announcer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;

/**
 * HTTP pseudo-streaming API for building mixed audio streams consisting of multiple inputs.
 */
@Controller
public record Mixing(
    ObjectFactory<WavMixStreamBuilder> wavFactory,
    ObjectFactory<MP3MixStreamBuilder> mp3Factory,
    ObjectFactory<M4AMixStreamBuilder> m4aFactory) implements Monitorable
{
    private static final Logger log = LoggerFactory.getLogger(Mixing.class);
    
    @GetMapping(value = "/stereo.wav", produces = WavMixStreamBuilder.AUDIO_WAV_TYPE)
    public StreamingResponseBody wav(Announcer announcer, @RequestParam List<FFBoxedInput> source, @RequestParam(required = false) Duration start) {
        final WavMixStreamBuilder builder = wavFactory.getObject();
        builder.monitor(this);
        builder.sources(source, start);
        return builder.build(announcer);
    }

    @GetMapping(value = "/stereo.mp3", produces = MP3MixStreamBuilder.AUDIO_MPEG_TYPE)
    public StreamingResponseBody mp3(
            Announcer announcer,
            @RequestParam List<FFBoxedInput> source,
            @RequestParam(required = false) Quality q,
            @RequestParam(required = false) Bitrate b,
            @RequestParam(required = false) Duration start) 
    {
        final MP3MixStreamBuilder builder = mp3Factory.getObject();
        builder.monitor(this);
        builder.sources(source, start);
        builder.quality(q);
        builder.bitrate(b);
        return builder.build(announcer);
    }

    @GetMapping(value = "/stereo.mp4a", produces = M4AMixStreamBuilder.AUDIO_MP4_TYPE, params = "!selector")
    public StreamingResponseBody mp4a(
            Announcer announcer,
            @RequestParam List<FFBoxedInput> source,
            @RequestParam(required = false) Quality q,
            @RequestParam(required = false) Bitrate b,
            @RequestParam(required = false) Duration start)
    {
        final M4AMixStreamBuilder builder = m4aFactory.getObject();
        builder.monitor(this);
        builder.sources(source, start);
        builder.quality(q);
        builder.bitrate(b);
        return builder.build(announcer);
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
        model.addAttribute("mediatype", M4AMixStreamBuilder.AUDIO_MP4.toString());
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
