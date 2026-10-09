package com.github.fmqa.spu.media.ffmpeg;

import com.github.fmqa.spu.media.common.Opener;
import java.io.IOException;
import java.io.InputStream;

/**
 * Connects an FFmpeg command line invocation's standard input to a data stream.
 * <p></p>
 * After the FFmpeg process is started, users must call {@link open()} and copy all bytes from the returned
 * {@link InputStream} to the FFmpeg process' standard input ({@link Process#getOutputStream()}).
 * @see Process
 */
public class StandardInputConnector implements Connector {
    private static final String FF_STDIN = "pipe:0";

    private Opener action;

    /**
     * Connects an FFmpeg command line invocation's standard input to a data stream supplied by the given
     * {@link Opener}.
     * @param opener Supplies a resource-related {@link InputStream}
     * @return An implementation-defined string token representing the standard input pipe.
     */
    @Override
    public String connect(Opener opener) {
        if (action != null) {
            throw new IllegalStateException("Standard input already connected, got: " + opener);
        }
        action = opener;
        return FF_STDIN;
    }

    /**
     * Indicates whether this connector is connected to FFmpeg.
     */
    public boolean connected() {
        return action != null;
    }

    /**
     * Returns an {@link InputStream} for reading resource data.
     * <p>
     * Data from this resource must be copied to the FFmpeg process' standard input ({@link Process#getOutputStream()}).
     * @return An {@link InputStream} for reading resource data
     * @throws IOException If an I/O error occurs while opening the underlying resource
     * @throws InterruptedException If opening the resource was interrupted
     */
    public InputStream open() throws IOException, InterruptedException {
        return action == null ? null : action.open();
    }
}
