package br.ia.sina.api.notification;

import br.ia.sina.api.analysis.model.ScamType;
import br.ia.sina.api.analysis.model.Verdict;
import br.ia.sina.api.analysis.openai.LlmVerdict;
import br.ia.sina.api.analysis.openai.OpenAiClient;
import br.ia.sina.api.notification.email.EmailContent;
import br.ia.sina.api.notification.email.EmailSender;
import br.ia.sina.api.notification.model.AlertDelivery;
import br.ia.sina.api.notification.model.DeliveryChannel;
import br.ia.sina.api.notification.model.DeliveryStatus;
import br.ia.sina.api.notification.push.PushResult;
import br.ia.sina.api.notification.push.PushSender;
import br.ia.sina.api.notification.repository.AlertDeliveryRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(NotificationFlowTest.SyncAsync.class)
class NotificationFlowTest {

    private static final String TEXT = "Oi mãe, troquei de número, faz um Pix de [VALOR] <b>urgente</b>";

    @TestConfiguration
    static class SyncAsync {
        @Bean("taskExecutor")
        TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    AlertDeliveryRepository deliveryRepository;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    OpenAiClient openAi;

    @MockitoBean
    PushSender push;

    @MockitoBean
    EmailSender email;

    @BeforeEach
    void setUp() {
        when(openAi.classify(any(), anyString(), anyList()))
                .thenReturn(new LlmVerdict(Verdict.SCAM, ScamType.FALSE_RELATIVE, 0.95,
                        "Isso é golpe. Não faça o Pix.", "Falso parente pedindo Pix urgente."));
        when(push.enabled()).thenReturn(true);
        when(email.enabled()).thenReturn(true);
        when(push.send(anyString(), anyMap())).thenReturn(PushResult.SENT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void golpeEnviaPushEEmailComCategoriaETrecho() throws Exception {
        Device idoso = register("{\"role\":\"PROTECTED\",\"displayName\":\"Dona Rosa\"}");
        Device familiar = register("{\"role\":\"GUARDIAN\",\"fcmToken\":\"tok-1\",\"email\":\"familia@example.com\"}");
        pair(idoso, familiar);

        String alertId = analyze(idoso);

        List<AlertDelivery> deliveries = deliveryRepository.findByAlertIdOrderByCreatedAt(UUID.fromString(alertId));
        assertThat(deliveries).extracting(AlertDelivery::getChannel)
                .containsExactlyInAnyOrder(DeliveryChannel.PUSH, DeliveryChannel.EMAIL);
        assertThat(deliveries).allMatch(d -> d.getStatus() == DeliveryStatus.SENT && d.getGuardianId().equals(familiar.id()));

        ArgumentCaptor<Map<String, String>> data = ArgumentCaptor.forClass(Map.class);
        verify(push).send(eq("tok-1"), data.capture());
        assertThat(data.getValue())
                .containsEntry("type", "SCAM_ALERT")
                .containsEntry("alertId", alertId)
                .containsEntry("protectedDeviceId", idoso.id().toString())
                .containsEntry("protectedName", "Dona Rosa")
                .containsEntry("source", "WHATSAPP")
                .containsEntry("scamType", "FALSE_RELATIVE")
                .containsEntry("scamLabel", "Falso parente")
                .containsEntry("familySummary", "Falso parente pedindo Pix urgente.")
                .containsEntry("messageExcerpt", TEXT)
                .containsKey("createdAt");

        ArgumentCaptor<EmailContent> mail = ArgumentCaptor.forClass(EmailContent.class);
        verify(email).send(anyString(), eq("familia@example.com"), mail.capture());
        assertThat(mail.getValue().subject()).isEqualTo("Alerta Sina: possível golpe contra Dona Rosa");
        assertThat(mail.getValue().html())
                .contains("Falso parente")
                .contains("&lt;b&gt;urgente&lt;/b&gt;")
                .doesNotContain("<b>urgente");
        assertThat(mail.getValue().text()).contains(TEXT);
    }

    @Test
    void tokenInvalidoEApagadoEEmailComFalhaFicaFailed() throws Exception {
        when(push.send(anyString(), anyMap())).thenReturn(PushResult.INVALID_TOKEN);
        doThrow(new RestClientException("Resend fora")).when(email).send(anyString(), anyString(), any());
        Device idoso = register("{\"role\":\"PROTECTED\"}");
        Device familiar = register("{\"role\":\"GUARDIAN\",\"fcmToken\":\"tok-velho\",\"email\":\"familia@example.com\"}");
        pair(idoso, familiar);

        String alertId = analyze(idoso);

        assertThat(deliveryRepository.findByAlertIdOrderByCreatedAt(UUID.fromString(alertId)))
                .hasSize(2)
                .allMatch(d -> d.getStatus() == DeliveryStatus.FAILED);
        String token = jdbc.queryForObject("select fcm_token from device where id = ?", String.class, familiar.id());
        assertThat(token).isNull();
    }

    @Test
    void familiarSemCanalNaoENotificado() throws Exception {
        Device idoso = register("{\"role\":\"PROTECTED\"}");
        Device familiar = register("{\"role\":\"GUARDIAN\"}");
        pair(idoso, familiar);

        String body = mvc.perform(post("/v1/analyze").header("Authorization", "Bearer " + idoso.token())
                        .contentType(MediaType.APPLICATION_JSON).content(analyzeBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardianNotified").value(false))
                .andReturn().getResponse().getContentAsString();

        String alertId = JsonPath.read(body, "$.alertId");
        assertThat(deliveryRepository.findByAlertIdOrderByCreatedAt(UUID.fromString(alertId))).isEmpty();
        verify(push, never()).send(anyString(), anyMap());
        verify(email, never()).send(anyString(), anyString(), any());
    }

    @Test
    void canaisDesligadosNaoGeramEntrega() throws Exception {
        when(push.enabled()).thenReturn(false);
        when(email.enabled()).thenReturn(false);
        Device idoso = register("{\"role\":\"PROTECTED\"}");
        Device familiar = register("{\"role\":\"GUARDIAN\",\"fcmToken\":\"tok-1\",\"email\":\"familia@example.com\"}");
        pair(idoso, familiar);

        mvc.perform(post("/v1/analyze").header("Authorization", "Bearer " + idoso.token())
                        .contentType(MediaType.APPLICATION_JSON).content(analyzeBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardianNotified").value(false));
    }

    private record Device(UUID id, String token) {
    }

    private Device register(String json) throws Exception {
        String body = mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON).content(json))
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

    private String analyze(Device idoso) throws Exception {
        String body = mvc.perform(post("/v1/analyze").header("Authorization", "Bearer " + idoso.token())
                        .contentType(MediaType.APPLICATION_JSON).content(analyzeBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SCAM"))
                .andExpect(jsonPath("$.guardianNotified").value(true))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.alertId");
    }

    private String analyzeBody() {
        return """
                {"source":"WHATSAPP","text":"%s","localSignals":["PIX_REQUEST","URGENCY"],"receivedAt":"2026-11-10T12:00:00Z"}
                """.formatted(TEXT);
    }
}
