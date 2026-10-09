package com.github.fmqa.spu.config;

import com.github.fmqa.spu.media.temporal.Estimator;
import com.github.fmqa.spu.media.temporal.Estimators;
import java.net.http.HttpClient;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Estimation {
    @Bean
    public Estimator estimator(HttpClient client) {
        return Estimators.combine(List.of(Estimators.header(client), Estimators.FFPROBE));
    }
}
