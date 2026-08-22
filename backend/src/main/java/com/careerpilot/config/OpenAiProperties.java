package com.careerpilot.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "careerpilot.ai.openai")
public record OpenAiProperties(
        String apiKey,
        URI baseUrl,
        String model
) {

    private static final URI DEFAULT_BASE_URL = URI.create("https://api.openai.com/v1");
    private static final String DEFAULT_MODEL = "gpt-5.6";

    public OpenAiProperties {
        apiKey = apiKey == null ? "" : apiKey;
        baseUrl = baseUrl == null ? DEFAULT_BASE_URL : baseUrl;
        model = model == null || model.isBlank() ? DEFAULT_MODEL : model;
    }
}
