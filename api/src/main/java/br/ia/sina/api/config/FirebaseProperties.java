package br.ia.sina.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("firebase")
public record FirebaseProperties(String credentialsPath) {
}
