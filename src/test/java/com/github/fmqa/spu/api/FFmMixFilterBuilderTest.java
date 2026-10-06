package com.github.fmqa.spu.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FFmMixFilterBuilderTest {
    @Test
    void normal() {
        final FFmMixFilterBuilder builder = new FFmMixFilterBuilder();
        builder.add(1.0, 0.5);
        assertEquals(
                "[0:a]aformat=channel_layouts=mono,pan=stereo|c0=0.5*c0|c1=0.5*c0[s0];" +
                "[s0]amix=inputs=1:normalize=0,alimiter=limit=0.95:asc=1[aout]",
                builder.build()
        );
    }

    @Test
    void normalWithVolumeAndBalance() {
        final FFmMixFilterBuilder builder = new FFmMixFilterBuilder();
        builder.add(1.1, 0.9);
        assertEquals(
                "[0:a]aformat=channel_layouts=mono,volume=1.1,pan=stereo|c0=0.9*c0|c1=0.1*c0[s0];" +
                        "[s0]amix=inputs=1:normalize=0,alimiter=limit=0.95:asc=1[aout]",
                builder.build()
        );
    }

    @Test
    void multiStreams() {
        final FFmMixFilterBuilder builder = new FFmMixFilterBuilder();
        builder.add(1.1, 0.9);
        builder.add(1.2, 0.7);
        assertEquals(
                "[0:a]aformat=channel_layouts=mono,volume=1.1,pan=stereo|c0=0.9*c0|c1=0.1*c0[s0];" +
                        "[1:a]aformat=channel_layouts=mono,volume=1.2,pan=stereo|c0=0.7*c0|c1=0.3*c0[s1];" +
                        "[s0][s1]amix=inputs=2:normalize=0,alimiter=limit=0.95:asc=1[aout]",
                builder.build()
        );
    }
}