package br.ia.sina.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("openai")
public record OpenAiProperties(
        String apiKey,
        String model,
        @DefaultValue("https://api.openai.com/v1") String baseUrl,
        @DefaultValue("8s") Duration timeout) {
}
