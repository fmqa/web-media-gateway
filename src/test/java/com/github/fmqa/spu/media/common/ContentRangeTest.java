package com.github.fmqa.spu.media.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ContentRangeTest {
    @Test
    void testValidParse() {
        final ContentRange range = ContentRange.parse("bytes 0-1/2");
        assertEquals("bytes", range.unit());
        assertEquals(0, range.start());
        assertEquals(1, range.end());
        assertEquals(2, range.total());
        assertEquals(2, range.available());
    }
}
