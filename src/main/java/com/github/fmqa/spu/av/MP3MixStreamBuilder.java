package com.github.fmqa.spu.av;

import java.util.Collections;
import java.util.List;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@Scope(SCOPE_PROTOTYPE)
public class MP3MixStreamBuilder extends AbstractMixStreamBuilder {
    public static final String AUDIO_MPEG_TYPE = "audio/mpeg";
    public static final MediaType AUDIO_MPEG = MediaType.parseMediaType(AUDIO_MPEG_TYPE);
    
    private Quality quality;
    private Bitrate bitrate;
    
    public MP3MixStreamBuilder quality(Quality q) {
        quality = q;
        return this;
    }
    
    public MP3MixStreamBuilder bitrate(Bitrate b) {
        bitrate = b;
        return this;
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
}
