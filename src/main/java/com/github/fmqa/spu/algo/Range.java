package com.github.fmqa.spu.algo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A closed interval {@code (start; end)}.
 * @param start begin of the interval (must be a finite number greater than, or equal to, zero)
 * @param end end of the interval (must be greater than {@code start}, and may also be {@link Double#POSITIVE_INFINITY})
 */
public record Range(double start, double end) {
    private static final Pattern PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)?-([0-9]+(?:\\.[0-9]+)?)?");

    public Range {
        if (!Double.isFinite(start) || Double.isNaN(end) || start < 0 || start > end) {
            throw new IllegalArgumentException("Invalid range bounds: " + start + ", " + end);
        }
    }

    /**
     * Parses the given value as a range.
     * <p></p>
     * If the start bound is unspecified, it is interpreted as 0.
     * If the end bound is unspecified, it is interpreted as {@link Double#POSITIVE_INFINITY}.
     * @param value a range specification e.g. 0-1, -1, 0-, or -
     * @return a range object representing the given value
     */
    public static Range parse(String value) {
        Objects.requireNonNull(value, "Range value must not be null");
        final var matcher = PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid range: " + value);
        }
        final String a = matcher.group(1), b = matcher.group(2);
        return new Range(a == null || a.isEmpty() ? 0 : Double.parseDouble(a), b == null || b.isEmpty() ? Double.POSITIVE_INFINITY : Double.parseDouble(b));
    }

    /**
     * Merges overlapping ranges.
     * <p></p>
     * Note that this may mutate the given list by sorting it according to {@code start}.
     * @param ranges list of possibly-overlapping ranges
     * @return result list of non-overlapping ranges, after ranges with overlapping ranges are merged
     */
    public static List<Range> merge(List<Range> ranges) {
        ranges.sort(Comparator.comparingDouble(Range::start));
        final List<Range> merged = new ArrayList<>(ranges.size());
        for (final Range current : ranges) {
            if (merged.isEmpty() || merged.getLast().end < current.start) {
                merged.add(current);
            } else {
                final Range last = merged.removeLast();
                merged.add(new Range(last.start, Math.max(last.end, current.end)));
            }
        }
        return merged;
    }

    @Override
    public String toString() {
        String result = start + "-";
        if (!Double.isInfinite(end)) {
            result += end;
        }
        return result;
    }
}
