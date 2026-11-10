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
        assertThat(code).matches("\\d{6}");

        String body = redeem(familiar[1], code).andExpect(status().isCreated())
                .andExpect(jsonPath("$.protected.deviceId").value(idoso[0]))
                .andExpect(jsonPath("$.protected.displayName").value("Dona Maria"))
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
    void codigoExpiradoDa410() throws Exception {
        String[] idoso = register("PROTECTED", null);
        String[] familiar = register("GUARDIAN", null);
        String code = createCode(idoso[1]);
        jdbc.update("update pairing_code set expires_at = now() - interval '1 second' where code = ?", code);

        redeem(familiar[1], code).andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code").value("PAIRING_CODE_EXPIRED"));
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
        redeem(idoso[1], "123456").andExpect(status().isForbidden());
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
        for (int i = 0; i < PairingAttemptLimiter.MAX_FAILURES; i++) {
            redeem(familiar[1], "000000").andExpect(status().isNotFound());
        }
        redeem(familiar[1], "000000").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    private String[] register(String role, String name) throws Exception {
        String json = name == null ? "{\"role\":\"%s\"}".formatted(role) : "{\"role\":\"%s\",\"displayName\":\"%s\"}".formatted(role, name);
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
