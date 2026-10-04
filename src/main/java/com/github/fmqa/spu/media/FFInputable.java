package com.github.fmqa.spu.media;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * A local or remote media resource that can be used as an FFmpeg input.
 */
public interface FFInputable {
    /**
     * Returns the URI of this media resource.
     * @return The URI of this media resource.
     */
    URI uri();

    /**
     * Generates the required FFmpeg command line arguments to process this resource as an input resource.
     * @param connector Connects the resource bitstream to FFmpeg
     * @return Argument list specifying this resource as an FFmpeg input
     */
    List<String> ffmpeg(Connector connector);

    /**
     * Produces a resource containing the same data as this one, but starting from the specified position.
     * @param start Position the returned resource should begin at
     * @return A resource containing the same data, but navigated to the specified position
     */
    FFInputable seek(Duration start);

    /**
     * Returns the duration of this media resource.
     * @return The duration of this resource, or {@code null} if the duration is unknown
     * @throws IOException If an I/O error happens while requesting the duration
     * @throws InterruptedException If the request for the duration is interrupted
     */
    default Duration duration() throws IOException, InterruptedException { return null; }
}
