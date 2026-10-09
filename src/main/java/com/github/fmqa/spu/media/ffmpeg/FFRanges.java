package com.github.fmqa.spu.media.ffmpeg;

import com.github.fmqa.spu.algo.Range;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Helper methods for converting time range specifications to FFmpeg CLI arguments.
 */
public class FFRanges {
    private FFRanges() {}

    public static String toFFArg(double start, double end) {
        if (Double.isInfinite(end)) {
            return String.format(Locale.ROOT, "gte(t,%.3f)", start);
        } else {
            return String.format(Locale.ROOT, "between(t,%.3f,%.3f)", start, end);
        }
    }

    public static String toFFArg(Range range) {
        return toFFArg(range.start(), range.end());
    }

    public static String toFFArg(List<Range> ranges) {
        return ranges.stream().map(FFRanges::toFFArg).collect(Collectors.joining("+"));
    }
}
