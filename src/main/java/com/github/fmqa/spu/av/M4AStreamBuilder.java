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
public class M4AStreamBuilder extends AbstractMediaStreamBuilder {
    public static final String AUDIO_MP4_TYPE = "audio/mp4; codecs=\"mp4a.40.2\"";
    public static final MediaType AUDIO_MP4 = MediaType.parseMediaType(AUDIO_MP4_TYPE);

    private final Piper piper;
    private Quality quality;
    private Bitrate bitrate;

    public M4AStreamBuilder(Piper piper) {
        this.piper = piper;
    }
    
    public M4AStreamBuilder quality(Quality q) {
        quality = q;
        return this;
    }
    
    public M4AStreamBuilder bitrate(Bitrate b) {
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
        Collections.addAll(argv , "-c:a", "aac");
        if (bitrate != null) {
            Collections.addAll(argv, "-b:a", bitrate.value());
        } else if (quality != null) {
            Collections.addAll(argv, "-q:a", String.valueOf(quality.value()));
        }
        Collections.addAll(
            argv,
            "-movflags", "empty_moov+default_base_moof+frag_keyframe+skip_sidx",
            "-frag_duration", "100000",
            "-flush_packets", "1",
            "-f", "mp4"
        );
        return AUDIO_MP4;
    }

    public StreamingResponseBody build(Announcer announcer) throws IOException, InterruptedException {
        return build(announcer, piper);
    }
}
