package com.github.fmqa.spu.av;

import com.github.fmqa.spu.io.Piper;
import com.github.fmqa.spu.support.Announcer;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Component
@Scope(SCOPE_PROTOTYPE)
public class MP3StreamBuilder extends AbstractMediaStreamBuilder {
    public static final String AUDIO_MPEG_TYPE = "audio/mpeg";
    public static final MediaType AUDIO_MPEG = MediaType.parseMediaType(AUDIO_MPEG_TYPE);
    
    private final Piper piper;
    private Quality quality;
    private Bitrate bitrate;
    
    public MP3StreamBuilder(Piper piper) {
        this.piper = piper;
    }
    
    public MP3StreamBuilder quality(Quality q) {
        quality = q;
        return this;
    }
    
    public MP3StreamBuilder bitrate(Bitrate b) {
        bitrate = b;
        return this;
    }
    
    @Override
    protected void source(List<String> argv, List<String> argn) {
        argv.add("-vn");
        super.ffmpeg(argv);
    }

    @Override
    protected MediaType sink(List<String> argv) {
        Collections.addAll(argv, "-c:a", "libmp3lame");
        if (bitrate != null) {
            Collections.addAll(argv, "-b:a", bitrate.value());
        } else if (quality != null) {
            Collections.addAll(argv, "-q:a", String.valueOf(quality.value()));
        }
        Collections.addAll(argv, "-f", "mp3");
        return AUDIO_MPEG;
    }
    
    public StreamingResponseBody build(Announcer announcer) throws IOException, InterruptedException {
        return build(announcer, piper);
    }
}
