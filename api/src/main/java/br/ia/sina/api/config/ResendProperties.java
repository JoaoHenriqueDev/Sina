package br.ia.sina.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("resend")
public record ResendProperties(
        String apiKey,
        String from,
        @DefaultValue("https://api.resend.com") String baseUrl,
        @DefaultValue("8s") Duration timeout) {
}
