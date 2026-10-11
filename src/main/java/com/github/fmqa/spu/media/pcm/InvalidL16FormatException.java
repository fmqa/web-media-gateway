package com.github.fmqa.spu.media.pcm;

/**
 * Thrown when attempting to construct an invalid {@code audio/l16} (RFC2586) format.
 */
public final class InvalidL16FormatException extends IllegalArgumentException {
    InvalidL16FormatException(String msg) {
        super(msg);
    }

    InvalidL16FormatException(Throwable cause) {
        super(cause);
    }
}
