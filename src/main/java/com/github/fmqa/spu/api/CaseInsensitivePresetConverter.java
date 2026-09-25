package com.github.fmqa.spu.api;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Case-insensitive converter for {@link Preset} enumeration values.
 */
@Component
public class CaseInsensitivePresetConverter implements Converter<String, Preset> {
    @Override
    public Preset convert(String source) {
        return Preset.valueOf(source.toUpperCase(Locale.ROOT));
    }
}
