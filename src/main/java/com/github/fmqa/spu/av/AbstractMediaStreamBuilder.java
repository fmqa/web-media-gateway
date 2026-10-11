package com.github.fmqa.spu.av;

import com.github.fmqa.spu.algo.Range;
import com.github.fmqa.spu.io.Piper;
import com.github.fmqa.spu.media.common.Connector;
import com.github.fmqa.spu.media.ffmpeg.FFInputable;
import com.github.fmqa.spu.media.ffmpeg.FFRanges;
import com.github.fmqa.spu.media.ffmpeg.FFStandardInputConnector;
import com.github.fmqa.spu.support.Announcer;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

abstract class AbstractMediaStreamBuilder {
    private FFInputable source;
    private Duration delay = Duration.ZERO;
    private Duration start = Duration.ZERO;
    private List<Range> ranges = Collections.emptyList();
    private Monitorable monitor = Monitorables.IGNORE;
    
    public void input(FFInputable input) {
        source = input;
        start = Duration.ZERO;
    }
    
    public void input(FFInputable input, Duration at) {
        source = input;
        start = at == null ? Duration.ZERO : at;
    }
    
    public void delay(Duration by) {
        delay = by == null ? Duration.ZERO : by;
    }
    
    public void delete(List<Range> which) {
        ranges = which == null ? Collections.emptyList() : which;
    }
    
    protected void ffmpeg(List<String> argv) {
        Collections.addAll(argv, "ffmpeg", "-nostdin");
    }
    
    protected void source(List<String> argv, List<String> argn) {
        argv.addAll(argn);
    }
    
    protected void source(List<String> argv, Connector connector) {
        final Duration from = start.minus(delay);
        source(argv, from.isPositive() ? source.seek(from).ffmpeg(connector) : source.ffmpeg(connector));
    }
    
    protected void delay(List<String> filters) {
        final Duration by = delay.minus(start);
        if (by.isPositive()) {
            filters.add(String.format(Locale.ROOT, "adelay=%dms:all=1", by.toMillis()));
        }
    }
    
    protected void ranges(List<String> filters) {
        if (!ranges.isEmpty()) {
            filters.add(String.format(Locale.ROOT, "volume=0:enable='%s'", FFRanges.toFFArg(ranges)));
        }
    }
    
    protected void filters(List<String> argv) {
        final List<String> filters = new ArrayList<>();
        delay(filters);
        ranges(filters);
        if (!filters.isEmpty()) {
            Collections.addAll(argv, "-af", String.join(",", filters));
        }
    }
    
    protected abstract MediaType sink(List<String> argv);
    
    protected MediaType command(List<String> argv, Connector connector) {
        ffmpeg(argv);
        source(argv, connector);
        filters(argv);
        return sink(argv);
    }
    
    public void monitor(Monitorable monitorable) {
        monitor = monitorable == null ? Monitorables.IGNORE : monitorable;
    }
    
    protected void started(long pid, List<String> args) {
        monitor.started(pid, args);
    }
    
    protected void exited(long pid, int code) {
        monitor.exited(pid, code);
    }
    
    private void exited(Process process) {
        exited(process.pid(), process.exitValue());
    }
    
    protected StreamingResponseBody body(ProcessBuilder builder, FFStandardInputConnector connector, Piper piper) {
        return dst -> {
            try (final var ffmpeg = builder.start()) {
                ffmpeg.onExit().thenAccept(this::exited);
                started(ffmpeg.pid(), builder.command());
                piper.pipe(connector.open(), ffmpeg.getOutputStream(), ffmpeg.getInputStream(), dst);
            } catch (InterruptedException _) {
            }
        };
    }
    
    protected StreamingResponseBody build(Announcer announcer, Piper piper) throws IOException, InterruptedException {
        final FFStandardInputConnector connector = new FFStandardInputConnector();
        final List<String> argv = new ArrayList<>();
        final MediaType content = command(argv, connector);
        
        argv.add("pipe:1");
        
        Duration duration = source.duration();
        if (duration != null) {
            duration = duration.plus(delay);
        }
        
        announcer.announce(content, duration);
        
        final ProcessBuilder builder = new ProcessBuilder(argv);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        
        return body(builder, connector, piper);
    }
}
