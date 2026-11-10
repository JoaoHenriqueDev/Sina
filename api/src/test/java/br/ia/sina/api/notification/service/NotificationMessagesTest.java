package br.ia.sina.api.notification.service;

import br.ia.sina.api.analysis.model.Source;
import br.ia.sina.api.notification.ScamNotification;
import br.ia.sina.api.notification.email.EmailContent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessagesTest {

    private static final Instant AT = Instant.parse("2026-11-10T15:24:00Z");

    private final ScamNotification scam = new ScamNotification(UUID.randomUUID(), UUID.randomUUID(), Source.SMS,
            "FALSE_BANK", 0.9, "Falso banco pedindo senha.", "Seu cartão foi bloqueado [LINK:banco-x.com]", AT);

    @Test
    void semNomeUsaPessoaProtegidaEHorarioDeBrasilia() {
        EmailContent mail = NotificationMessages.scamEmail(scam, null);
        assertThat(mail.subject()).isEqualTo("Alerta Sina: possível golpe contra a pessoa protegida");
        assertThat(mail.text())
                .contains("recebida pela pessoa protegida pelo SMS em 10/11/2026 às 12:24")
                .contains("Categoria: Falso banco");

        EmailContent inactive = NotificationMessages.inactiveEmail("  ", AT);
        assertThat(inactive.subject()).isEqualTo("Sina: proteção parada no celular da pessoa protegida");
    }

    @Test
    void nomeEEscapadoNoHtmlESemQuebraNoAssunto() {
        EmailContent mail = NotificationMessages.scamEmail(scam, "<script>Zé</script>\r\nBcc: x");
        assertThat(mail.subject()).doesNotContain("\r").doesNotContain("\n");
        assertThat(mail.html()).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    @Test
    void pushOmiteNomeAusenteEUsaStrings() {
        Map<String, String> data = NotificationMessages.scamPush(scam, null);
        assertThat(data).doesNotContainKey("protectedName")
                .containsEntry("scamLabel", "Falso banco")
                .containsEntry("confidence", "0.9")
                .containsEntry("createdAt", "2026-11-10T15:24:00Z");

        Map<String, String> inactive = NotificationMessages.inactivePush(scam.protectedId(), "Dona Rosa", AT);
        assertThat(inactive).containsEntry("type", "PROTECTION_INACTIVE")
                .containsEntry("protectedName", "Dona Rosa")
                .containsEntry("lastSeenAt", "2026-11-10T15:24:00Z");
    }
}
