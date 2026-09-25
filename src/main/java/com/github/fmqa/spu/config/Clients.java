package com.github.fmqa.spu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

/**
 * Configuration class for service clients.
 */
@Configuration
public class Clients {
    @Bean
    public HttpClient defaultHttpClient() {
        return HttpClient.newHttpClient();
    }
}
