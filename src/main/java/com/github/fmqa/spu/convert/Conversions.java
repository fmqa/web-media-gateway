package com.github.fmqa.spu.convert;

import com.github.fmqa.spu.media.ffmpeg.FFInputable;
import com.github.fmqa.spu.media.pcm.L16HTTPResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

/**
 * Provides miscellaneous type converters.
 */
@Configuration
public class Conversions {
    @Bean
    public Converter<String, FFInputable> uriToFFInput(URIConverter uri, FFInputConverter ff) {
        return uri.andThen(ff);
    }

    @Bean
    public Converter<String, FFBoxedInput> uriToWrappedFFInput(URIConverter uri, FFBoxedInputConverter ff) {
        return uri.andThen(ff);
    }

    @Bean
    public Converter<String, L16HTTPResource> uriToL16HTTPResource(URIConverter uri, L16HTTPResourceConverter ff) {
        return uri.andThen(ff);
    }
}
