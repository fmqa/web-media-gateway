package com.github.fmqa.spu.av;

import java.util.Collections;
import java.util.List;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@Scope(SCOPE_PROTOTYPE)
public class WavMixStreamBuilder extends AbstractMixStreamBuilder {
    public static final String AUDIO_WAV_TYPE = "audio/x-wav";
    public static final MediaType AUDIO_WAV = MediaType.parseMediaType(AUDIO_WAV_TYPE);
    
    @Override
    protected MediaType sink(List<String> argv) {
        Collections.addAll(argv, "-f", "wav");
        return AUDIO_WAV;
    }
}
