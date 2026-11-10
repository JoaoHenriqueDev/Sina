package br.ia.sina.api.alert;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AlertControllerTest {

    private static final Instant BASE = Instant.parse("2026-11-10T12:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void paginaDoMaisRecenteAoMaisAntigoSemRepetir() throws Exception {
        Device idoso = register("PROTECTED");
        Device familiar = register("GUARDIAN");
        pair(idoso, familiar);
        List<UUID> expected = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            expected.addFirst(insertAlert(idoso.id(), BASE.plusSeconds(i)));
        }
        UUID tieA = insertAlert(idoso.id(), BASE.minusSeconds(10));
        UUID tieB = insertAlert(idoso.id(), BASE.minusSeconds(10));
        boolean aFirst = tieA.toString().compareTo(tieB.toString()) > 0;
        expected.add(aFirst ? tieA : tieB);
        expected.add(aFirst ? tieB : tieA);

        List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            var req = get("/v1/alerts").param("limit", "3").header("Authorization", "Bearer " + familiar.token());
            if (cursor != null) {
                req.param("cursor", cursor);
            }
            String body = mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            seen.addAll(JsonPath.read(body, "$.items[*].id"));
            cursor = JsonPath.read(body, "$.nextCursor");
            pages++;
        } while (cursor != null);

        assertThat(pages).isEqualTo(3);
        assertThat(seen).containsExactlyElementsOf(expected.stream().map(UUID::toString).toList());
    }

    @Test
    void retornaOsCamposDoContrato() throws Exception {
        Device idoso = register("PROTECTED");
        Device familiar = register("GUARDIAN");
        pair(idoso, familiar);
        UUID id = insertAlert(idoso.id(), BASE);

        mvc.perform(get("/v1/alerts").param("protectedId", idoso.id().toString())
                        .header("Authorization", "Bearer " + familiar.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].protectedDeviceId").value(idoso.id().toString()))
                .andExpect(jsonPath("$.items[0].source").value("WHATSAPP"))
                .andExpect(jsonPath("$.items[0].verdict").value("SCAM"))
                .andExpect(jsonPath("$.items[0].scamType").value("FALSE_RELATIVE"))
                .andExpect(jsonPath("$.items[0].confidence").value(0.95))
                .andExpect(jsonPath("$.items[0].familySummary").value("Falso parente pedindo Pix."))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-11-10T12:00:00Z"))
                .andExpect(jsonPath("$.nextCursor").isEmpty());
    }

    @Test
    void familiarSoVeAlertasDeQuemEstaPareado() throws Exception {
        Device idoso = register("PROTECTED");
        Device outroIdoso = register("PROTECTED");
        Device familiar = register("GUARDIAN");
        pair(idoso, familiar);
        UUID meu = insertAlert(idoso.id(), BASE);
        insertAlert(outroIdoso.id(), BASE.plusSeconds(1));

        mvc.perform(get("/v1/alerts").header("Authorization", "Bearer " + familiar.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(meu.toString()));

        mvc.perform(get("/v1/alerts").param("protectedId", outroIdoso.id().toString())
                        .header("Authorization", "Bearer " + familiar.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PAIRING_NOT_FOUND"));
    }

    @Test
    void semPareamentoListaVazia() throws Exception {
        Device familiar = register("GUARDIAN");
        mvc.perform(get("/v1/alerts").header("Authorization", "Bearer " + familiar.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.nextCursor").isEmpty());
    }

    @Test
    void protegidoNaoListaAlertas() throws Exception {
        Device idoso = register("PROTECTED");
        mvc.perform(get("/v1/alerts").header("Authorization", "Bearer " + idoso.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    }

    @Test
    void parametrosInvalidosDao400() throws Exception {
        Device familiar = register("GUARDIAN");
        String[][] cases = {{"limit", "51"}, {"limit", "0"}, {"cursor", "nao-e-cursor"}, {"protectedId", "abc"}};
        for (String[] c : cases) {
            mvc.perform(get("/v1/alerts").param(c[0], c[1]).header("Authorization", "Bearer " + familiar.token()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    private record Device(UUID id, String token) {
    }

    private Device register(String role) throws Exception {
        String body = mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Device(UUID.fromString(JsonPath.read(body, "$.deviceId")), JsonPath.read(body, "$.deviceToken"));
    }

    private void pair(Device idoso, Device familiar) throws Exception {
        String body = mvc.perform(post("/v1/pairing-codes").header("Authorization", "Bearer " + idoso.token()))
                .andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(body, "$.code");
        mvc.perform(post("/v1/pairings").header("Authorization", "Bearer " + familiar.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().is2xxSuccessful());
    }

    private UUID insertAlert(UUID protectedId, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO alert (id, protected_id, source, verdict, scam_type, confidence, family_summary, created_at)
                VALUES (?, ?, 'WHATSAPP', 'SCAM', 'FALSE_RELATIVE', 0.95, 'Falso parente pedindo Pix.', ?)
                """, id, protectedId, Timestamp.from(createdAt));
        return id;
    }
}
