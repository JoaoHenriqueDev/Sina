package br.ia.sina.api.notification.email;

import br.ia.sina.api.config.ResendProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

@Component
public class EmailSender {

    private final ResendProperties properties;
    private final RestClient http;

    public EmailSender(ResendProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
        factory.setReadTimeout(properties.timeout());
        this.http = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .build();
    }

    public boolean enabled() {
        return properties.apiKey() != null && !properties.apiKey().isBlank()
                && properties.from() != null && !properties.from().isBlank();
    }

    public void send(String idempotencyKey, String to, EmailContent content) {
        http.post()
                .uri("/emails")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "from", sender(),
                        "to", List.of(to),
                        "subject", content.subject(),
                        "html", content.html(),
                        "text", content.text()))
                .retrieve()
                .toBodilessEntity();
    }

    private String sender() {
        String from = properties.from().strip();
        return from.contains("<") ? from : "Sina <" + from + ">";
    }
}
