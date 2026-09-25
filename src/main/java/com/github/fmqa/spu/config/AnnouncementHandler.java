package com.github.fmqa.spu.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles {@link AnnouncementException}s thrown in controllers.
 * <p></p>
 * This handles any {@link AnnouncementException} by returning an HTTP 200 OK
 * response with the announced media content type in the Content-Type header, and the duration
 * of the media in the Content-Duration header.
 */
@RestControllerAdvice
public class AnnouncementHandler {
    @ExceptionHandler(AnnouncementException.class)
    public ResponseEntity<?> handleAnnouncement(AnnouncementException ex) {
        var response = ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, ex.content);
        if (ex.seconds >= 0 && Double.isFinite(ex.seconds)) {
            response = response.header(Announcers.DURATION_HEADER, Double.toString(ex.seconds));
        }
        return response.build();
    }
}
