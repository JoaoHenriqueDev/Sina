package br.ia.sina.api.analysis;

import br.ia.sina.api.alert.repository.AlertRepository;
import br.ia.sina.api.analysis.model.ScamType;
import br.ia.sina.api.analysis.model.Verdict;
import br.ia.sina.api.analysis.openai.LlmVerdict;
import br.ia.sina.api.analysis.openai.OpenAiClient;
import br.ia.sina.api.analysis.repository.AnalysisCacheRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalysisControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AlertRepository alertRepository;

    @Autowired
    AnalysisCacheRepository cacheRepository;

    @MockitoBean
    OpenAiClient openAi;

    @Test
    void analisaGravaAlertaECacheiaSemOTexto() throws Exception {
        when(openAi.classify(any(), anyString(), anyList()))
                .thenReturn(new LlmVerdict(Verdict.SCAM, ScamType.FALSE_RELATIVE, 0.93, "Não faça o Pix.", "Falso parente pedindo Pix."));
        String body = register("PROTECTED");
        String token = JsonPath.read(body, "$.deviceToken");
        UUID deviceId = UUID.fromString(JsonPath.read(body, "$.deviceId"));
        String text = "Oi mãe, troquei de número, faz um Pix urgente";

        analyze(token, text).andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SCAM"))
                .andExpect(jsonPath("$.scamType").value("FALSE_RELATIVE"))
                .andExpect(jsonPath("$.engine").value("LLM"))
                .andExpect(jsonPath("$.alertId").isNotEmpty())
                .andExpect(jsonPath("$.guardianNotified").value(false));
        String first = analyze(token, text).andReturn().getResponse().getContentAsString();
        String second = analyze(token, text).andExpect(status().isOk())
                .andExpect(jsonPath("$.engine").value("CACHE"))
                .andExpect(jsonPath("$.guardianNotified").value(false))
                .andReturn().getResponse().getContentAsString();

        verify(openAi, times(1)).classify(any(), anyString(), anyList());
        assertThat(alertRepository.findAll()).filteredOn(a -> a.getProtectedId().equals(deviceId)).hasSize(1);
        assertThat((String) JsonPath.read(second, "$.alertId")).isEqualTo(JsonPath.read(first, "$.alertId"));
        assertThat(cacheRepository.findAll()).allSatisfy(c -> assertThat(c.getResponseJson()).doesNotContain(text));
    }

    @Test
    void guardianNaoAnalisa() throws Exception {
        String token = JsonPath.read(register("GUARDIAN"), "$.deviceToken");
        analyze(token, "teste").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    }

    @Test
    void entradaInvalidaDa400() throws Exception {
        String token = JsonPath.read(register("PROTECTED"), "$.deviceToken");
        for (String json : new String[]{
                "{\"source\":\"SMS\",\"text\":\"" + "a".repeat(1001) + "\",\"receivedAt\":\"2026-11-10T12:00:00Z\"}",
                "{\"source\":\"SMS\",\"text\":\"oi\",\"localSignals\":[\"XYZ\"],\"receivedAt\":\"2026-11-10T12:00:00Z\"}",
                "{\"source\":\"EMAIL\",\"text\":\"oi\",\"receivedAt\":\"2026-11-10T12:00:00Z\"}",
                "{\"source\":\"SMS\",\"text\":\" \",\"receivedAt\":\"2026-11-10T12:00:00Z\"}",
                "{\"source\":\"SMS\",\"text\":\"oi\",\"receivedAt\":\"2026-11-10T12:00:00Z\",\"messageCount\":0}",
                "{\"source\":\"SMS\",\"text\":\"oi\",\"receivedAt\":\"2026-11-10T12:00:00Z\",\"messageCount\":51}",
                "{\"source\":\"SMS\",\"text\":\"oi\"}"}) {
            mvc.perform(post("/v1/analyze").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void aceitaVariasMensagensComMessageCount() throws Exception {
        when(openAi.classify(any(), anyString(), anyList()))
                .thenReturn(new LlmVerdict(Verdict.SAFE, ScamType.NONE, 0.9, "Mensagem sem perigo.", "Conversa comum."));
        String token = JsonPath.read(register("PROTECTED"), "$.deviceToken");
        mvc.perform(post("/v1/analyze").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"WHATSAPP\",\"text\":\"Oi\\nTudo bem?\\nMe liga depois\",\"messageCount\":3,\"receivedAt\":\"2026-11-10T12:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAFE"));
        verify(openAi).classify(any(), org.mockito.ArgumentMatchers.eq("Oi\nTudo bem?\nMe liga depois"), anyList());
    }

    @Test
    void semTokenDa401() throws Exception {
        analyze(null, "oi").andExpect(status().isUnauthorized());
    }

    private String register(String role) throws Exception {
        return mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions analyze(String token, String text) throws Exception {
        var req = post("/v1/analyze").contentType(MediaType.APPLICATION_JSON)
                .content("{\"source\":\"WHATSAPP\",\"text\":\"" + text + "\",\"localSignals\":[\"PIX_REQUEST\",\"URGENCY\"],\"receivedAt\":\"2026-11-10T12:00:00Z\"}");
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(req);
    }
}
