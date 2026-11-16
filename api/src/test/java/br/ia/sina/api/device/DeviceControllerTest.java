package br.ia.sina.api.device;

import br.ia.sina.api.device.repository.DeviceRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeviceControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DeviceRepository repository;

    @Test
    void health() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void corsLiberaSoAOrigemDaDemo() throws Exception {
        mvc.perform(options("/v1/alerts").header("Origin", "https://joaohenriquedev.github.io")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://joaohenriquedev.github.io"));
        mvc.perform(options("/v1/alerts").header("Origin", "https://site-qualquer.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registraEUsaOToken() throws Exception {
        String token = register("{\"role\":\"GUARDIAN\",\"displayName\":\"Ana\",\"email\":\"ana@exemplo.com\",\"phone\":\"(11) 98765-4321\"}");

        mvc.perform(put("/v1/devices/me/fcm-token").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fcmToken\":\"novo\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void semTokenOuTokenInvalidoDa401ComMesmaMensagem() throws Exception {
        for (String header : new String[]{null, "Bearer xyz", "Basic abc"}) {
            var req = put("/v1/devices/me/fcm-token").contentType(MediaType.APPLICATION_JSON).content("{\"fcmToken\":\"x\"}");
            if (header != null) {
                req.header("Authorization", header);
            }
            mvc.perform(req).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.error.message").value(DeviceAuthInterceptor.UNAUTHORIZED_MESSAGE));
        }
    }

    @Test
    void cadastroIncompletoOuTelefoneInvalidoDa400() throws Exception {
        for (String json : new String[]{
                "{\"role\":\"PROTECTED\"}",
                "{\"role\":\"PROTECTED\",\"displayName\":\"Maria\",\"email\":\"maria@teste.com.br\"}",
                "{\"role\":\"GUARDIAN\",\"displayName\":\"Ana\",\"phone\":\"11987654321\"}",
                "{\"role\":\"GUARDIAN\",\"displayName\":\"Ana\",\"email\":\"ana@teste.com.br\",\"phone\":\"abc\"}",
                "{\"role\":\"GUARDIAN\",\"displayName\":\"Ana\",\"email\":\"ana@teste.com.br\",\"phone\":\"(11) 9876-54\"}"}) {
            mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void cadastroGuardaTelefoneSoComDigitos() throws Exception {
        String body = mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"PROTECTED\",\"displayName\":\" Maria \",\"email\":\"maria@teste.com.br\",\"phone\":\"+55 (11) 98765-4321\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var device = repository.findById(UUID.fromString(JsonPath.read(body, "$.deviceId"))).orElseThrow();
        assertThat(device.getPhone()).isEqualTo("5511987654321");
        assertThat(device.getDisplayName()).isEqualTo("Maria");
    }

    @Test
    void corpoInvalidoDa400() throws Exception {
        mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void heartbeatDeGuardianDa403() throws Exception {
        String token = register("{\"role\":\"GUARDIAN\",\"displayName\":\"Ana\",\"email\":\"pessoa@teste.com.br\",\"phone\":\"11987654321\"}");
        heartbeat(token, true).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    }

    @Test
    void heartbeatSoAtualizaQuandoListenerAtivo() throws Exception {
        String body = mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"PROTECTED\",\"displayName\":\"Maria\",\"email\":\"pessoa@teste.com.br\",\"phone\":\"11987654321\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.deviceToken");
        UUID id = UUID.fromString(JsonPath.read(body, "$.deviceId"));

        heartbeat(token, false).andExpect(status().isNoContent());
        assertThat(repository.findById(id).orElseThrow().getLastSeenAt()).isNull();

        heartbeat(token, true).andExpect(status().isNoContent());
        assertThat(repository.findById(id).orElseThrow().getLastSeenAt()).isNotNull();
    }

    private String register(String json) throws Exception {
        String body = mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.deviceToken");
    }

    private org.springframework.test.web.servlet.ResultActions heartbeat(String token, boolean enabled) throws Exception {
        return mvc.perform(post("/v1/devices/me/heartbeat").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"listenerEnabled\":" + enabled + "}"));
    }
}
