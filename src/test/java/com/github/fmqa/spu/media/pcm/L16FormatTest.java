package com.github.fmqa.spu.media.pcm;

import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

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
}
