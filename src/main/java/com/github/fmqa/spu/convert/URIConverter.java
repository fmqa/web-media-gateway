package com.github.fmqa.spu.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Converts string parameters to {@link URI} objects.
 */
@Component
public class URIConverter implements Converter<String, URI> {
    @Override
    public URI convert(String source) {
        return URI.create(source);
    }
}
