package br.ia.sina.api.device;

import br.ia.sina.api.config.SinaProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenService {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public TokenService(SinaProperties properties) {
        this.key = new SecretKeySpec(properties.deviceTokenPepper().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String token) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
