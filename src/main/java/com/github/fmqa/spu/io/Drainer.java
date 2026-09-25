package com.github.fmqa.spu.io;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Component
public class Drainer {
    /**
     * Copies all bytes from {@code src} to {@code dst}.
     * <p></p>
     * Note that this method is annotated with {@link Async}, causing it to return immediately.
     * The copying happens in the background util either stream EOFs. Both streams will be closed regardless of
     * success/failure.
     * @param src The source stream to copy from
     * @param dst The sink/destination stream to copy to
     */
    @Async
    public void drain(InputStream src, OutputStream dst) {
        try (src; dst) {
            if (src == null || dst == null) return;
            src.transferTo(dst);
            dst.flush();
        } catch (IOException ignored) {}
    }
}
