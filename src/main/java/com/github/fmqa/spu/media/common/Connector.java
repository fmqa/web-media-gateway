package com.github.fmqa.spu.media.common;

/**
 * Connects an {@link java.io.InputStream} supplied by an {@link Opener} to a reifiable resource descriptor, such as
 * a named pipe or a named file handle.
 */
@FunctionalInterface
public interface Connector {
    /**
     * Returns a string descriptor for the {@link java.io.InputStream} supplied by {@link Opener}.
     * @param opener Supplies a resource-related {@link java.io.InputStream}
     * @return An OS-level string descriptor that can be used to read resource data
     */
    String connect(Opener opener);
}
