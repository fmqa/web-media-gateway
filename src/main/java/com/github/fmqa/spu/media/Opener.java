package com.github.fmqa.spu.media;

import java.io.IOException;
import java.io.InputStream;

/**
 * Supplies an {@link InputStream} associated with a resource.
 */
@FunctionalInterface
public interface Opener {
    /**
     * Supplies an {@link InputStream} that can be used to read resource data.
     * @return An {@link InputStream} for reading resource data
     * @throws IOException If an I/O error occurs while opening the underlying resource
     * @throws InterruptedException If opening the resource was interrupted
     */
    InputStream open() throws IOException, InterruptedException;
}
