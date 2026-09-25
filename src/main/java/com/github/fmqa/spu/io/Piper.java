package com.github.fmqa.spu.io;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implements piping from source to sink via an intermediate pipe.
 */
@Component
public class Piper {
    private final Drainer d;

    public Piper(Drainer drainer) {
        d = drainer;
    }

    /**
     * Copies all bytes from {@code src} to {@code in}, while simultaneously also copying bytes from {@code out} to
     * {@code dst}.
     * <p></p>
     * {@code in}/{@code out} represent the write/read ends of the intermediate pipe, while {@code src}/{@code dst}
     * represent the source/sink of the pipeline.
     * @param src The source (start) of the pipeline
     * @param in The write end of the intermediate pipe
     * @param out The read end of the intermediate pipe
     * @param dst The sink (end) of the pipeline
     */
    public void pipe(InputStream src, OutputStream in, InputStream out, OutputStream dst) {
        try (dst; out; src) {
            d.drain(src, in);
            out.transferTo(dst);
            dst.flush();
        } catch (IOException ignored) {}
    }
}
