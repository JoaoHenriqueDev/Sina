package br.ia.sina.api.device;

import br.ia.sina.api.config.SinaProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private final TokenService service = new TokenService(new SinaProperties("key"));

    @Test
    void geraTokenDe32BytesEmBase64UrlSemPadding() {
        String token = service.generate();
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(service.generate()).isNotEqualTo(token);
    }

    @Test
    void hashEhHmacSha256EmHex() {
        assertThat(service.hash("The quick brown fox jumps over the lazy dog"))
                .isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }

    @Test
    void pepperDiferenteGeraHashDiferente() {
        TokenService outro = new TokenService(new SinaProperties("outra-chave"));
        assertThat(outro.hash("abc")).isNotEqualTo(service.hash("abc")).hasSize(64);
    }
}
