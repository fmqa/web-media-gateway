package com.github.fmqa.spu.media.pcm;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;

/**
 * audio/l16 (RFC2586) format.
 * @param rate sampling rate of the audio data
 * @param channels mumber of channels sampled
 */
public record L16Format(int rate, int channels) {
    private static final MediaType AUDIO_L16 = new MediaType("audio", "l16");
    
    public L16Format {
        if (rate <= 0) {
            throw new IllegalArgumentException("Invalid audio/l16 sampling rate: " + rate);
        }
        if (channels <= 0) {
            throw new IllegalArgumentException("Invalid audio/l16 channel count: " + channels);
        }
    }
    
    /**
     * Creates an l16 audio format from the given {@code audio/l16} media type.
     * @param content media type to parse
     * @return an l16 (RFC2586) audio format
     * @throws IllegalArgumentException if the given media type is not audio/l16 or if any parameters are invalid
     */
    public static L16Format from(MediaType content) {
        if (!AUDIO_L16.isCompatibleWith(content)) {
            throw new IllegalArgumentException("Invalid audio/l16 media: " + content);
        }
        
        int rate = 8000, channels = 1;
        
        String value = content.getParameter("rate");
        if (value != null) {
            rate = Integer.parseInt(value);
        }
        
        value = content.getParameter("channels");
        if (value != null) {
            channels = Integer.parseInt(value);
        }
        
        return new L16Format(rate, channels);
    }
    
    /*
     * Creates an l16 audio format from the given {@code audio/l16} media type string.
     * @param content media type string to parse
     * @return an l16 (RFC2586) audio format
     * @throws IllegalArgumentException if the given media type is not audio/l16 or if any parameters are invalid
     */
    public static L16Format from(String content) {
        return from(MediaType.parseMediaType(content));
    }

    /**
     * Returns the HTTP content type of this audio format.
     * @return the content type string for this format
     */
    public MediaType content() {
        return new MediaType("audio", "l16", Map.of("rate", Integer.toString(rate), "channels", Integer.toString(channels)));
    }
    
    /**
     * Returns the byte length of the given audio duration in this audio format.
     * @param duration the duration to estimate the byte length for
     * @return the number of bytes needed to store the given duration in this audio format
     */
    public long bytes(Duration duration) {
        final var full = duration.toSeconds() * rate;
        final var part = ((double) duration.toNanosPart() / 1.0e9) * (double) rate;
        final var frames = full + (long) part;
        return 2 * channels * frames;
    }
    
    /**
     * Returns the duration of the given audio in bytes, assuming it is sampled in this format.
     * @param bytes the number of audio bytes
     * @return the duration of the audio in this format
     */
    public Duration duration(long bytes) {
        final var bps = 2 * rate * channels;
        final var s = Math.abs(bytes) / (double) bps;
        final var whole = (long) s;
        final var frac = (s - whole) * 1e9;
        return Duration.ofSeconds(whole, (long) frac);
    }
    
    @Override
    public String toString() {
        return content().toString();
    }
}
