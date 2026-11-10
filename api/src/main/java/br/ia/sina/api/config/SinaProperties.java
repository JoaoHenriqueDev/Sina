package br.ia.sina.api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("sina")
public record SinaProperties(@NotBlank @Size(min = 32) String deviceTokenPepper) {
}
