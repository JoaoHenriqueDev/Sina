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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void registraEUsaOToken() throws Exception {
        String token = register("{\"role\":\"GUARDIAN\",\"displayName\":\"Ana\",\"email\":\"ana@exemplo.com\"}");

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
    void emailEmProtectedDa400() throws Exception {
        mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"PROTECTED\",\"email\":\"a@b.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
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
        String token = register("{\"role\":\"GUARDIAN\"}");
        heartbeat(token, true).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    }

    @Test
    void heartbeatSoAtualizaQuandoListenerAtivo() throws Exception {
        String body = mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"PROTECTED\"}"))
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
