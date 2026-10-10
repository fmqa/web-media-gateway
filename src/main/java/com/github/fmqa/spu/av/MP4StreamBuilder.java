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
public class MP4StreamBuilder extends AbstractMediaStreamBuilder {
    public static final String VIDEO_MP4_TYPE = "video/mp4; codecs=\"avc1.42E01E, mp4a.40.2\"";
    public static final MediaType VIDEO_MP4 = MediaType.parseMediaType(VIDEO_MP4_TYPE);

    private final Piper piper;
    private Quality quality;
    private Bitrate bitrate;
    private Preset preset;

    public MP4StreamBuilder(Piper piper) {
        this.piper = piper;
    }

    public MP4StreamBuilder quality(Quality q) {
        quality = q;
        return this;
    }

    public MP4StreamBuilder bitrate(Bitrate b) {
        bitrate = b;
        return this;
    }

    public MP4StreamBuilder preset(Preset p) {
        preset = p;
        return this;
    }

    @Override
    protected MediaType sink(List<String> argv) {
        Collections.addAll(argv, "-profile:v", "baseline", "-pix_fmt", "yuv420p", "-c:a", "aac");
        if (bitrate != null) {
            Collections.addAll(argv, "-b:a", bitrate.value());
        } else if (quality != null) {
            Collections.addAll(argv, "-q:a", String.valueOf(quality.value()));
        }
        if (preset != null) {
            Collections.addAll(argv, "-preset", preset.toString());
        }
        Collections.addAll(
            argv,
            "-movflags", "empty_moov+default_base_moof+frag_keyframe+skip_sidx",
            "-frag_duration", "100000",
            "-flush_packets", "1",
            "-f", "mp4"
        );
        return VIDEO_MP4;
    }

    public StreamingResponseBody build(Announcer announcer) throws IOException, InterruptedException {
        return build(announcer, piper);
    }
}
