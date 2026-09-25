package com.github.fmqa.spu.media;

/**
 * Connects an {@link java.io.InputStream} supplied by an {@link Opener} to a reifiable resource identifier
 * understood by FFmpeg.
 */
@FunctionalInterface
public interface Connector {
    /**
     * Connects the {@link java.io.InputStream} supplied by {@link Opener} to FFmpeg using an OS-level primitive.
     * @param opener Supplies a resource-related {@link java.io.InputStream}
     * @return An OS-level identifier that can be used to read resource data
     */
    String connect(Opener opener);
}
