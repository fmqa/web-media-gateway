package com.github.fmqa.spu.algo;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RangeTest {
    @Test
    void parseUnbounded() {
        final Range unbounded = Range.parse("-");
        assertEquals(0, unbounded.start());
        assertEquals(Double.POSITIVE_INFINITY, unbounded.end());
    }

    @Test
    void parseOnlyStartBound() {
        final Range fbound = Range.parse("2-");
        assertEquals(2.0, fbound.start());
        assertEquals(Double.POSITIVE_INFINITY, fbound.end());
    }

    @Test
    void parseOnlyEndBound() {
        final Range lbound = Range.parse("-11");
        assertEquals(0.0, lbound.start());
        assertEquals(11.0, lbound.end());
    }

    @Test
    void parseBounded() {
        final Range bounded = Range.parse("2.1-60.3");
        assertEquals(2.1, bounded.start());
        assertEquals(60.3, bounded.end());
    }

    @Test
    void merging() {
        final Range[] ranges = {
                new Range(5, 12),
                new Range(0, 10),
                new Range(20, 22)
        };
        final List<Range> result = Range.merge(Arrays.asList(ranges));
        assertEquals(List.of(new Range(0, 12), new Range(20, 22)), result);
    }
}