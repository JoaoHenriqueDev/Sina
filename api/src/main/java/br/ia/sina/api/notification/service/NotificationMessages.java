package br.ia.sina.api.notification.service;

import br.ia.sina.api.analysis.model.Source;
import br.ia.sina.api.notification.ScamNotification;
import br.ia.sina.api.notification.email.EmailContent;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class NotificationMessages {

    public static final String SCAM_ALERT = "SCAM_ALERT";
    public static final String PROTECTION_INACTIVE = "PROTECTION_INACTIVE";

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    private NotificationMessages() {
    }

    public static Map<String, String> scamPush(ScamNotification n, String protectedName) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", SCAM_ALERT);
        data.put("alertId", n.alertId().toString());
        data.put("protectedDeviceId", n.protectedId().toString());
        putIfPresent(data, "protectedName", protectedName);
        data.put("source", n.source().name());
        putIfPresent(data, "scamType", n.scamType());
        data.put("scamLabel", category(n.scamType()));
        data.put("confidence", String.valueOf(n.confidence()));
        data.put("familySummary", n.familySummary());
        data.put("messageExcerpt", n.messageExcerpt());
        data.put("createdAt", n.createdAt().toString());
        return data;
    }

    public static Map<String, String> inactivePush(UUID protectedId, String protectedName, Instant lastSeenAt) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", PROTECTION_INACTIVE);
        data.put("protectedDeviceId", protectedId.toString());
        putIfPresent(data, "protectedName", protectedName);
        data.put("lastSeenAt", lastSeenAt.toString());
        return data;
    }

    public static EmailContent scamEmail(ScamNotification n, String protectedName) {
        Person p = Person.from(protectedName);
        String category = category(n.scamType());
        String channel = n.source() == Source.WHATSAPP ? "WhatsApp" : "SMS";
        String when = DATE_TIME.format(n.createdAt());
        String subject = "Alerta Sina: possível golpe contra " + p.plain();

        String text = """
                O Sina identificou uma mensagem de golpe recebida %s pelo %s em %s.

                Categoria: %s
                Resumo: %s

                Trecho da mensagem (dados pessoais ocultos):
                "%s"

                Converse com %s antes de qualquer pagamento ou envio de código. Não clique em links nem ligue para números da mensagem.
                """.formatted(p.by(), channel, when, category, n.familySummary(), n.messageExcerpt(), p.plain());

        String html = """
                <div style="font-family:Arial,sans-serif;font-size:16px;line-height:1.5;color:#1a1a1a;max-width:560px">
                <h2 style="color:#b3261e;margin:0 0 12px">Possível golpe contra %s</h2>
                <p>O Sina identificou uma mensagem de golpe recebida %s pelo %s em %s.</p>
                <p><strong>Categoria:</strong> %s<br><strong>Resumo:</strong> %s</p>
                <p><strong>Trecho da mensagem</strong> (dados pessoais ocultos):</p>
                <blockquote style="margin:0 0 16px;padding:12px 16px;background:#f4f4f4;border-left:4px solid #b3261e">%s</blockquote>
                <p>Converse com %s antes de qualquer pagamento ou envio de código. Não clique em links nem ligue para números da mensagem.</p>
                </div>
                """.formatted(html(p.plain()), html(p.by()), channel, when, html(category),
                html(n.familySummary()), html(n.messageExcerpt()), html(p.plain()));
        return new EmailContent(subject, html, text);
    }

    public static EmailContent inactiveEmail(String protectedName, Instant lastSeenAt) {
        Person p = Person.from(protectedName);
        String when = DATE_TIME.format(lastSeenAt);
        String subject = "Sina: proteção parada no celular " + p.of();

        String text = """
                O celular %s não envia sinal ao Sina desde %s.

                A proteção pode estar desligada: o aparelho pode estar sem internet, desligado ou sem a permissão de leitura de notificações.

                Confira com %s se o Sina continua ativo.
                """.formatted(p.of(), when, p.plain());

        String html = """
                <div style="font-family:Arial,sans-serif;font-size:16px;line-height:1.5;color:#1a1a1a;max-width:560px">
                <h2 style="color:#8a5a00;margin:0 0 12px">Proteção parada</h2>
                <p>O celular %s não envia sinal ao Sina desde %s.</p>
                <p>A proteção pode estar desligada: o aparelho pode estar sem internet, desligado ou sem a permissão de leitura de notificações.</p>
                <p>Confira com %s se o Sina continua ativo.</p>
                </div>
                """.formatted(html(p.of()), when, html(p.plain()));
        return new EmailContent(subject, html, text);
    }

    static String category(String scamType) {
        if (scamType == null) {
            return "Golpe";
        }
        return switch (scamType) {
            case "FALSE_RELATIVE" -> "Falso parente";
            case "FALSE_BANK" -> "Falso banco";
            case "FALSE_DELIVERY" -> "Falsa entrega";
            case "FALSE_PRIZE_OR_DEBT" -> "Falso prêmio ou dívida";
            case "PHISHING_LINK" -> "Link falso";
            case "VERIFICATION_CODE" -> "Pedido de código de verificação";
            case "FAKE_INVESTMENT" -> "Investimento falso";
            case "FALSE_BENEFIT" -> "Falso benefício";
            case "CLONED_ACCOUNT" -> "Conta clonada";
            default -> "Golpe";
        };
    }

    private static void putIfPresent(Map<String, String> data, String key, String value) {
        if (value != null && !value.isBlank()) {
            data.put(key, value);
        }
    }

    private static String html(String value) {
        return HtmlUtils.htmlEscape(value, "UTF-8");
    }

    private record Person(String name) {

        static Person from(String displayName) {
            String name = displayName == null ? null : displayName.replaceAll("[\\r\\n\\t]+", " ").strip();
            return new Person(name == null || name.isEmpty() ? null : name);
        }

        String plain() {
            return name != null ? name : "a pessoa protegida";
        }

        String by() {
            return name != null ? "por " + name : "pela pessoa protegida";
        }

        String of() {
            return name != null ? "de " + name : "da pessoa protegida";
        }
    }
}
