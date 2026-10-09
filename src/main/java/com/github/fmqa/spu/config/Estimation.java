package com.github.fmqa.spu.config;

import com.github.fmqa.spu.media.temporal.Estimator;
import com.github.fmqa.spu.media.temporal.Estimators;
import java.net.http.HttpClient;
import java.time.Duration;

import com.github.fmqa.spu.media.temporal.FFEstimator;
import com.github.fmqa.spu.media.temporal.HeaderEstimator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Estimation {
    @Bean
    public HeaderEstimator header(HttpClient client) {
        return new HeaderEstimator(client);
    }

    @Bean
    public FFEstimator ffprobe() {
        return new FFEstimator(Duration.ofSeconds(5));
    }

    @Bean
    public Estimator estimator(HeaderEstimator header, FFEstimator ffprobe) {
        return Estimators.combine(header, ffprobe);
    }
}
