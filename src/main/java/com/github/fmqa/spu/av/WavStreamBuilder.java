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
public class WavStreamBuilder extends AbstractMediaStreamBuilder {
    public static final String AUDIO_WAV_TYPE = "audio/x-wav";
    public static final MediaType AUDIO_WAV = MediaType.parseMediaType(AUDIO_WAV_TYPE);
    
    private final Piper piper;

    public WavStreamBuilder(Piper piper) {
        this.piper = piper;
    }
    
    @Override
    protected void source(List<String> argv, List<String> argn) {
        argv.add("-vn");
        super.ffmpeg(argv);
    }
    
    @Override
    protected MediaType sink(List<String> argv) {
        Collections.addAll(argv, "-f", "wav");
        return AUDIO_WAV;
    }
    
    public StreamingResponseBody build(Announcer announcer) throws IOException, InterruptedException {
        return build(announcer, piper);
    }
}
