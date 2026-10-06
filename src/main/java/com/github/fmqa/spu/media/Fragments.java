package com.github.fmqa.spu.media;

import com.github.fmqa.spu.algo.Range;
import com.github.fmqa.spu.units.Durations;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Utility methods for reading predefined properties encoded in URI fragments.
 */
public class Fragments {
    private Fragments() { }

    private static final Pattern GAIN_PATTERN = Pattern.compile("[#&]?gain=([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BALANCE_PATTERN = Pattern.compile("[#&]?balance=([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELAY_PATTERN = Pattern.compile("[#&]?delay=([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MASK_PATTERN = Pattern.compile("[#&]?mask=(([0-9]+(?:\\.[0-9]+)?)?-([0-9]+(?:\\.[0-9]+)?)?)", Pattern.CASE_INSENSITIVE);

    private static OptionalDouble get(Pattern pattern, String fragment, Function<Double, Double> map) {
        if (fragment == null) return OptionalDouble.empty();
        final var matcher = pattern.matcher(fragment);
        if (matcher.find()) {
            return OptionalDouble.of(map.apply(Double.parseDouble(matcher.group(1))));
        }
        return OptionalDouble.empty();
    }

    /**
     * Returns the value of the {@code #gain=...} URI fragment parameter.
     * @param fragment the URI fragment string
     * @return the gain value if it exists, or an empty optional otherwise
     */
    public static OptionalDouble gain(String fragment) {
        return get(GAIN_PATTERN, fragment, Function.identity());
    }

    /**
     * Returns the value of the {@code #gain=...} URI fragment parameter within the given URI.
     * @param uri the URI object
     * @return the gain value if it exists, or an empty optional otherwise
     */
    public static OptionalDouble gain(URI uri) {
        return gain(uri.getFragment());
    }

    /**
     * Returns the value of the {@code #balance=...} URI fragment parameter.
     * @param fragment the URI fragment string
     * @return the balance value if it , or an empty optional otherwise
     */
    public static OptionalDouble balance(String fragment) {
        return get(BALANCE_PATTERN, fragment, x -> Math.clamp(x, 0, 1));
    }

    /**
     * Returns the value of the {@code #balance=...} URI fragment parameter within the given URI.
     * @param uri the URI object
     * @return the balance value if it , or an empty optional otherwise
     */
    public static OptionalDouble balance(URI uri) {
        return balance(uri.getFragment());
    }

    /**
     * Returns the value of the {@code #delay=...} URI fragment parameter as a {@link Duration}.
     * @param fragment the URI fragment string
     * @return the delay value if it exists, or {@link Duration#ZERO} otherwise
     */
    public static Duration delay(String fragment) {
        return Durations.fromSeconds(get(DELAY_PATTERN, fragment, Function.identity()).orElse(0));
    }

    /**
     * Returns the value of the {@code #delay=...} URI fragment parameter within the given URI as a {@link Duration}..
     * @param uri the URI object
     * @return the delay value if it exists, or {@link Duration#ZERO} otherwise
     */
    public static Duration delay(URI uri) {
        return delay(uri.getFragment());
    }

    /**
     * Returns the values of the {@code #mask=...&mask=...} URI fragment parameters.
     * @param fragment the URI fragment string
     * @return the mask ranges encoded within the fragment
     */
    public static List<Range> ranges(String fragment) {
        if (fragment == null) return Collections.emptyList();
        final var matcher = MASK_PATTERN.matcher(fragment);
        final var ranges = new ArrayList<Range>();
        while (matcher.find()) {
            ranges.add(Range.parse(matcher.group(1)));
        }
        return Range.merge(ranges);
    }

    /**
     * Returns the values of the {@code #mask=...&mask=...} fragment parameters within the given URI.
     * @param uri the URI object
     * @return the mask ranges encoded within the URI's fragment
     */
    public static List<Range> ranges(URI uri) {
        return ranges(uri.getFragment());
    }
}
