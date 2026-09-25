package com.github.fmqa.spu.config;

import java.util.Locale;

/**
 * Thrown by the default announcer if an announcement is made during a HEAD request.
 * @see Announcers
 */
public final class AnnouncementException extends RuntimeException {
    final String content;
    final double seconds;
    AnnouncementException(String content, double seconds) {
        super(String.format(Locale.ROOT, "%s with duration %f", this.content = content, this.seconds = seconds));
    }
}
