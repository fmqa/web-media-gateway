package com.github.fmqa.spu.media.ffmpeg;

/**
 * Thrown when an FFmpeg connection is refused.
 * @see FFInputable#ffmpeg(Connector)
 */
public class ConnectionRejectedException extends IllegalStateException {
    ConnectionRejectedException(String message) {
        super(message);
    }
}
