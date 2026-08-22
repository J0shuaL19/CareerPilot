package com.careerpilot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiConfiguration {

    @Bean("openAiRestClient")
    RestClient openAiRestClient(RestClient.Builder builder, OpenAiProperties properties) {
        return builder
                .baseUrl(properties.baseUrl().toString())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .build();
    }
}
