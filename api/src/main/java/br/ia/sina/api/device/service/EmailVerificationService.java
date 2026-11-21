package br.ia.sina.api.device.service;

import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.repository.DeviceRepository;
import br.ia.sina.api.notification.email.EmailContent;
import br.ia.sina.api.notification.email.EmailSender;
import br.ia.sina.api.notification.service.FcmResendGuardianNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

/** O e-mail do familiar só vira canal de aviso depois que o dono do endereço confirma pelo link. */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final DeviceRepository repository;
    private final TokenService tokenService;
    private final EmailSender email;
    private final String publicBaseUrl;

    public EmailVerificationService(DeviceRepository repository, TokenService tokenService, EmailSender email,
                                    @Value("${sina.public-base-url}") String publicBaseUrl) {
        this.repository = repository;
        this.tokenService = tokenService;
        this.email = email;
        this.publicBaseUrl = publicBaseUrl;
    }

    /** Marca o dispositivo como pendente de confirmação e devolve o token que vai no link. */
    public String prepare(Device device) {
        String token = tokenService.generate();
        device.setEmailVerifiedAt(null);
        device.setEmailTokenHash(hash(token));
        return token;
    }

    @Async
    public void sendLink(String deviceId, String to, String token) {
        if (!email.enabled() || FcmResendGuardianNotifier.isReservedDomain(to)) {
            return;
        }
        String link = UriComponentsBuilder.fromUriString(publicBaseUrl).path("/email/confirmar")
                .queryParam("token", token).build().toUriString();
        String text = """
                Alguém cadastrou este e-mail no Sina para receber avisos de golpe de uma pessoa da família.

                Se foi você, confirme em: %s

                Se não foi você, ignore esta mensagem. Nenhum aviso será enviado para este endereço sem a confirmação.
                """.formatted(link);
        String html = """
                <div style="font-family:Arial,sans-serif;font-size:16px;line-height:1.5;color:#1a1a1a;max-width:560px">
                <h2 style="margin:0 0 12px">Confirme seu e-mail</h2>
                <p>Alguém cadastrou este e-mail no Sina para receber avisos de golpe de uma pessoa da família.</p>
                <p><a href="%s" style="display:inline-block;padding:12px 20px;background:#1a1a1a;color:#fff;border-radius:10px;text-decoration:none">Confirmar e-mail</a></p>
                <p>Se não foi você, ignore esta mensagem. Nenhum aviso será enviado para este endereço sem a confirmação.</p>
                </div>
                """.formatted(link);
        try {
            email.send("verify-" + deviceId, to, new EmailContent("Sina: confirme seu e-mail", html, text));
        } catch (RuntimeException e) {
            log.warn("Falha ao enviar confirmação de e-mail: {}", e.getClass().getSimpleName());
        }
    }

    @Transactional
    public boolean confirm(String token) {
        if (token == null || token.isBlank() || token.length() > 64) {
            return false;
        }
        return repository.findByEmailTokenHash(hash(token)).map(device -> {
            device.setEmailVerifiedAt(Instant.now());
            device.setEmailTokenHash(null);
            repository.save(device);
            return true;
        }).orElse(false);
    }

    private String hash(String token) {
        return tokenService.hash("email:" + token);
    }
}
