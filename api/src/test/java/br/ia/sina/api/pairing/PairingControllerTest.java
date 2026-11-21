package br.ia.sina.api.pairing;

import br.ia.sina.api.pairing.service.PairingAttemptLimiter;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PairingControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void fluxoCompletoDePareamento() throws Exception {
        String[] idoso = register("PROTECTED", "Dona Maria");
        String[] familiar = register("GUARDIAN", "João");
        String code = createCode(idoso[1]);
        assertThat(code).matches("\\d{8}");

        String body = redeem(familiar[1], code).andExpect(status().isCreated())
                .andExpect(jsonPath("$.protected.deviceId").value(idoso[0]))
                .andExpect(jsonPath("$.protected.displayName").value("Dona Maria"))
                .andExpect(jsonPath("$.protected.phone").value("11987654321"))
                .andExpect(jsonPath("$.protected.protectionStatus").value("INACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String pairingId = JsonPath.read(body, "$.pairingId");

        redeem(familiar[1], code).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PAIRING_CODE_NOT_FOUND"));
        redeem(familiar[1], createCode(idoso[1])).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_PAIRED"));

        heartbeat(idoso[1]);
        mvc.perform(get("/v1/pairings").header("Authorization", "Bearer " + familiar[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].pairingId").value(pairingId))
                .andExpect(jsonPath("$.items[0].createdAt").exists())
                .andExpect(jsonPath("$.items[0].protected.protectionStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].protected.lastSeenAt").exists())
                .andExpect(jsonPath("$.items[0].guardian").doesNotExist());
        mvc.perform(get("/v1/pairings").header("Authorization", "Bearer " + idoso[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].guardian.deviceId").value(familiar[0]))
                .andExpect(jsonPath("$.items[0].guardian.protectionStatus").doesNotExist())
                .andExpect(jsonPath("$.items[0].protected").doesNotExist());

        String[] estranho = register("GUARDIAN", "Outro");
        mvc.perform(delete("/v1/pairings/" + pairingId).header("Authorization", "Bearer " + estranho[1]))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PAIRING_NOT_FOUND"));
        mvc.perform(delete("/v1/pairings/" + pairingId).header("Authorization", "Bearer " + idoso[1]))
                .andExpect(status().isNoContent());
        mvc.perform(get("/v1/pairings").header("Authorization", "Bearer " + familiar[1]))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void codigoExpiradoRespondeIgualAInexistente() throws Exception {
        String[] idoso = register("PROTECTED", null);
        String[] familiar = register("GUARDIAN", null);
        String code = createCode(idoso[1]);
        jdbc.update("update pairing_code set expires_at = now() - interval '1 second' where code = ?", code);

        redeem(familiar[1], code).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PAIRING_CODE_NOT_FOUND"));
    }

    @Test
    void novoCodigoInvalidaOAnterior() throws Exception {
        String[] idoso = register("PROTECTED", null);
        String[] familiar = register("GUARDIAN", null);
        String antigo = createCode(idoso[1]);
        String novo = createCode(idoso[1]);
        Integer ativos = jdbc.queryForObject("select count(*) from pairing_code where protected_id = ?::uuid and used_at is null",
                Integer.class, idoso[0]);
        assertThat(ativos).isEqualTo(1);

        if (!antigo.equals(novo)) {
            redeem(familiar[1], antigo).andExpect(status().isNotFound());
        }
        redeem(familiar[1], novo).andExpect(status().isCreated());
    }

    @Test
    void papeisErradosDao403() throws Exception {
        String[] idoso = register("PROTECTED", null);
        String[] familiar = register("GUARDIAN", null);
        mvc.perform(post("/v1/pairing-codes").header("Authorization", "Bearer " + familiar[1]))
                .andExpect(status().isForbidden());
        redeem(idoso[1], "12345678").andExpect(status().isForbidden());
    }

    @Test
    void validacoes() throws Exception {
        String[] familiar = register("GUARDIAN", null);
        redeem(familiar[1], "12ab").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mvc.perform(delete("/v1/pairings/nao-e-uuid").header("Authorization", "Bearer " + familiar[1]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mvc.perform(delete("/v1/pairings/" + UUID.randomUUID()).header("Authorization", "Bearer " + familiar[1]))
                .andExpect(status().isNotFound());
    }

    @Test
    void cincoErrosBloqueiamComRateLimited() throws Exception {
        String[] familiar = register("GUARDIAN", null);
        for (int i = 0; i < PairingAttemptLimiter.MAX_ATTEMPTS; i++) {
            redeem(familiar[1], "00000000").andExpect(status().isNotFound());
        }
        redeem(familiar[1], "00000000").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void limiteDeFamiliaresPorPessoaProtegida() throws Exception {
        String[] idoso = register("PROTECTED", null);
        for (int i = 0; i < br.ia.sina.api.pairing.service.PairingService.MAX_GUARDIANS; i++) {
            redeem(register("GUARDIAN", null)[1], createCode(idoso[1])).andExpect(status().isCreated());
        }
        redeem(register("GUARDIAN", null)[1], createCode(idoso[1])).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_GUARDIANS"));
    }

    @Test
    void excluirContaApagaCadastroLigacoesEAlertas() throws Exception {
        String[] maria = register("PROTECTED", "Maria");
        String[] ana = register("GUARDIAN", "Ana");
        redeem(ana[1], createCode(maria[1])).andExpect(status().isCreated());
        jdbc.update("INSERT INTO alert (id, protected_id, source, verdict, scam_type, confidence, family_summary, created_at) VALUES (?, ?::uuid, 'SMS', 'SCAM', 'FALSE_BANK', 0.9, 'Resumo', now())",
                UUID.randomUUID(), maria[0]);

        mvc.perform(delete("/v1/devices/me").header("Authorization", "Bearer " + maria[1])).andExpect(status().isNoContent());

        mvc.perform(get("/v1/pairings").header("Authorization", "Bearer " + ana[1]))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get("/v1/pairings").header("Authorization", "Bearer " + maria[1])).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM device WHERE id = ?::uuid", Integer.class, maria[0])).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM alert WHERE protected_id = ?::uuid", Integer.class, maria[0])).isZero();
    }

    private String[] register(String role, String name) throws Exception {
        String json = "{\"role\":\"%s\",\"displayName\":\"%s\",%s}".formatted(role, name == null ? "Pessoa" : name, "\"email\":\"pessoa@teste.com.br\",\"phone\":\"11987654321\"");
        String body = mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new String[]{JsonPath.read(body, "$.deviceId"), JsonPath.read(body, "$.deviceToken")};
    }

    private String createCode(String token) throws Exception {
        String body = mvc.perform(post("/v1/pairing-codes").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.code");
    }

    private ResultActions redeem(String token, String code) throws Exception {
        return mvc.perform(post("/v1/pairings").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"" + code + "\"}"));
    }

    private void heartbeat(String token) throws Exception {
        mvc.perform(post("/v1/devices/me/heartbeat").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"listenerEnabled\":true}"))
                .andExpect(status().isNoContent());
    }
}
