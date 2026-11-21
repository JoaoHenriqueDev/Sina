package br.ia.sina.api.device.controller;

import br.ia.sina.api.device.service.EmailVerificationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/** Fica fora de /v1 (sem token de dispositivo). O GET só mostra o botão, para robôs que abrem links não confirmarem sozinhos. */
@RestController
@RequestMapping(value = "/email/confirmar", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
public class EmailVerificationController {

    private final EmailVerificationService service;

    public EmailVerificationController(EmailVerificationService service) {
        this.service = service;
    }

    @GetMapping
    String form(@RequestParam(defaultValue = "") String token) {
        return page("Confirmar e-mail", """
                <p>Toque no botão para receber os avisos de golpe do Sina neste e-mail.</p>
                <form method="post"><input type="hidden" name="token" value="%s">
                <button type="submit">Confirmar e-mail</button></form>
                """.formatted(HtmlUtils.htmlEscape(token, "UTF-8")));
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String confirm(@RequestParam(defaultValue = "") String token) {
        return service.confirm(token)
                ? page("E-mail confirmado", "<p>Pronto. Você vai receber os avisos do Sina neste e-mail.</p>")
                : page("Link inválido", "<p>Este link não vale mais. Se o e-mail já foi confirmado, não precisa fazer nada.</p>");
    }

    private static String page(String title, String body) {
        return """
                <!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>%s · Sina</title><style>body{font-family:Arial,sans-serif;max-width:480px;margin:48px auto;padding:0 16px;line-height:1.5;color:#1a1a1a}
                button{font-size:17px;padding:12px 20px;border:0;border-radius:10px;background:#1a1a1a;color:#fff}</style></head>
                <body><h1>%s</h1>%s</body></html>
                """.formatted(title, title, body);
    }
}
