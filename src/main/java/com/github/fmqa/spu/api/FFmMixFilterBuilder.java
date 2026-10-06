package com.github.fmqa.spu.api;

import com.github.fmqa.spu.media.Fragments;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Helper class for building stereo downmix filters.
 */
class FFmMixFilterBuilder {
    private int index = 0;
    private final StringBuilder builder = new StringBuilder();
    private final List<String> streams = new ArrayList<>();

    private static String format(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    public void add(double gain, double balance) {
        builder.append(format("[%d:a]", index));
        builder.append("aformat=channel_layouts=mono");
        if (gain != 1) {
            builder.append(",volume=").append(gain);
        }
        builder.append(format(",pan=stereo|c0=%.1f*c0|c1=%.1f*c0", balance, 1 - balance));
        final String ch = format("[s%d]", index);
        builder.append(ch).append(";");
        streams.add(ch);
        index++;
    }

    public void add(URI uri) {
        add(Fragments.gain(uri).orElse(1.0), Fragments.balance(uri).orElse(0.5));
    }

    public String build() {
        final StringBuilder result = new StringBuilder(builder);
        streams.forEach(result::append);
        result.append(format("amix=inputs=%d:normalize=0,alimiter=limit=0.95:asc=1[aout]", index));
        return result.toString();
    }
}
