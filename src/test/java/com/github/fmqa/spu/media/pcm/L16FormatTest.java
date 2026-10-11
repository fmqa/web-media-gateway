package com.github.fmqa.spu.media.pcm;

import java.time.Duration;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

public class L16FormatTest {
    @Test
    public void testBytes() {
        final L16Format format = new L16Format(8000, 1);
        assertEquals(16000, format.bytes(Duration.ofSeconds(1)));
    }
    
    @Test
    public void testDuration() {
        final L16Format format = new L16Format(8000, 1);
        assertEquals(Duration.ofSeconds(1), format.duration(16000));
    }
    
    @Test
    public void testInvalidFormat() {
        assertThrows(InvalidL16FormatException.class, () -> new L16Format(8000, -1));
        assertThrows(InvalidL16FormatException.class, () -> new L16Format(8000, 0));
        assertThrows(InvalidL16FormatException.class, () -> new L16Format(0, 1));
        assertThrows(InvalidL16FormatException.class, () -> new L16Format(-1, 1));
    }
    
    @Test
    public void testParseFromDefaultMediaType() {
        final L16Format format = L16Format.from(new MediaType("audio", "l16"));
        assertEquals(8000, format.rate());
        assertEquals(1, format.channels());
    }
    
    @Test
    public void testParseFromMediaType() {
        final L16Format format = L16Format.from(new MediaType("audio", "l16", Map.of("rate", "16000", "channels", "2")));
        assertEquals(16000, format.rate());
        assertEquals(2, format.channels());
    }
    
    @Test
    public void testParseFromUnsupportedMediaType() {
        assertThrows(InvalidL16FormatException.class, () -> L16Format.from(MediaType.APPLICATION_OCTET_STREAM));
    }
    
    @Test
    public void testParseFromInvalidMediaType() {
        assertThrows(InvalidMediaTypeException.class, () -> L16Format.from("aaa"));
    }
}
