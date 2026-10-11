package com.github.fmqa.spu.media.pcm;

import java.io.IOException;

/**
 * Thrown when a client error occurs while attempting to retrieve/probe the parameters of an {@code audio/l16}
 * resource.
 */
public class L16ClientException extends IOException {
    L16ClientException(String msg) {
        super(msg);
    }
}
