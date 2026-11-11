package br.ia.sina.api.analysis;

import br.ia.sina.api.alert.model.Alert;
import br.ia.sina.api.alert.repository.AlertRepository;
import br.ia.sina.api.analysis.dto.AnalysisDtos.AnalyzeRequest;
import br.ia.sina.api.analysis.dto.AnalysisDtos.AnalyzeResponse;
import br.ia.sina.api.analysis.model.AnalysisCache;
import br.ia.sina.api.analysis.model.Engine;
import br.ia.sina.api.analysis.model.LocalSignal;
import br.ia.sina.api.analysis.model.ScamType;
import br.ia.sina.api.analysis.model.Source;
import br.ia.sina.api.analysis.model.Verdict;
import br.ia.sina.api.analysis.openai.LlmVerdict;
import br.ia.sina.api.analysis.openai.OpenAiClient;
import br.ia.sina.api.analysis.openai.OpenAiException;
import br.ia.sina.api.analysis.repository.AnalysisCacheRepository;
import br.ia.sina.api.analysis.service.AnalysisRateLimiter;
import br.ia.sina.api.analysis.service.AnalysisService;
import br.ia.sina.api.config.SinaProperties;
import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.service.TokenService;
import br.ia.sina.api.notification.GuardianNotifier;
import br.ia.sina.api.notification.ScamNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private static final String TEXT = "Oi mãe, troquei de número. Faz um Pix pra mim?";

    private final OpenAiClient openAi = mock(OpenAiClient.class);
    private final AnalysisCacheRepository cacheRepository = mock(AnalysisCacheRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final GuardianNotifier notifier = mock(GuardianNotifier.class);
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final AnalysisService service = new AnalysisService(openAi, cacheRepository, alertRepository, notifier,
            new TokenService(new SinaProperties("p".repeat(32))), new AnalysisRateLimiter(), mapper,
            new TransactionTemplate(mock(PlatformTransactionManager.class)));

    private final Device device = new Device();
    private final AtomicInteger sends = new AtomicInteger();

    @BeforeEach
    void setUp() {
        device.setId(UUID.randomUUID());
        when(cacheRepository.findById(anyString())).thenReturn(Optional.empty());
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notifier.prepareScam(any())).thenReturn(Optional.of(sends::incrementAndGet));
    }

    @Test
    void golpeComConfiancaAltaGravaAlertaNotificaECacheia() {
        llmReturns(Verdict.SCAM, ScamType.FALSE_RELATIVE, 0.93);

        AnalyzeResponse response = service.analyze(device, request(List.of(LocalSignal.PIX_REQUEST)));

        assertThat(response.engine()).isEqualTo(Engine.LLM);
        assertThat(response.verdict()).isEqualTo(Verdict.SCAM);
        assertThat(response.scamType()).isEqualTo("FALSE_RELATIVE");
        assertThat(response.guardianNotified()).isTrue();
        assertThat(sends).hasValue(1);
        verify(alertRepository).lockAnalysis(anyLong());
        ArgumentCaptor<Alert> alert = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alert.capture());
        assertThat(alert.getValue().getProtectedId()).isEqualTo(device.getId());
        assertThat(alert.getValue().getFamilySummary()).doesNotContain(TEXT);
        ArgumentCaptor<AnalysisCache> cache = ArgumentCaptor.forClass(AnalysisCache.class);
        verify(cacheRepository).save(cache.capture());
        assertThat(cache.getValue().getTextHash()).hasSize(64);
        assertThat(cache.getValue().getResponseJson()).doesNotContain(TEXT);
        ArgumentCaptor<ScamNotification> notification = ArgumentCaptor.forClass(ScamNotification.class);
        verify(notifier).prepareScam(notification.capture());
        assertThat(notification.getValue().messageExcerpt()).isEqualTo(TEXT);
        assertThat(notification.getValue().scamType()).isEqualTo("FALSE_RELATIVE");
        assertThat(notification.getValue().source()).isEqualTo(Source.WHATSAPP);
    }

    @Test
    void semFamiliarComCanalNaoContaComoNotificado() {
        llmReturns(Verdict.SCAM, ScamType.FALSE_RELATIVE, 0.93);
        when(notifier.prepareScam(any())).thenReturn(Optional.empty());

        AnalyzeResponse response = service.analyze(device, request(List.of()));

        assertThat(response.guardianNotified()).isFalse();
        assertThat(sends).hasValue(0);
        verify(alertRepository).save(any());
    }

    @Test
    void trechoLongoEnviadoAoFamiliarEhCortado() {
        llmReturns(Verdict.SCAM, ScamType.FALSE_BANK, 0.95);

        service.analyze(device, new AnalyzeRequest(Source.SMS, "a".repeat(1000), List.of(), Instant.now()));

        ArgumentCaptor<ScamNotification> notification = ArgumentCaptor.forClass(ScamNotification.class);
        verify(notifier).prepareScam(notification.capture());
        assertThat(notification.getValue().messageExcerpt()).hasSize(ScamNotification.EXCERPT_MAX).endsWith("…");
    }

    @Test
    void golpeComConfiancaBaixaNaoNotifica() {
        llmReturns(Verdict.SCAM, ScamType.FALSE_RELATIVE, 0.7);

        AnalyzeResponse response = service.analyze(device, request(List.of()));

        assertThat(response.guardianNotified()).isFalse();
        verify(alertRepository).save(any());
        verify(notifier, never()).prepareScam(any());
    }

    @Test
    void seguroNaoGravaAlerta() {
        llmReturns(Verdict.SAFE, ScamType.NONE, 0.95);

        AnalyzeResponse response = service.analyze(device, request(List.of()));

        assertThat(response.scamType()).isNull();
        assertThat(response.alertId()).isNull();
        verify(alertRepository, never()).save(any());
        verify(alertRepository, never()).lockAnalysis(anyLong());
    }

    @Test
    void cacheValidoNaoChamaOLlm() {
        String json = mapper.writeValueAsString(new LlmVerdict(Verdict.SCAM, ScamType.FALSE_RELATIVE, 0.9, "Golpe", "Falso parente"));
        when(cacheRepository.findById(anyString())).thenReturn(Optional.of(new AnalysisCache("h", Verdict.SCAM, json)));

        AnalyzeResponse response = service.analyze(device, request(List.of()));

        assertThat(response.engine()).isEqualTo(Engine.CACHE);
        verify(openAi, never()).classify(any(), anyString(), anyList());
    }

    @Test
    void mesmoTextoComEspacosECaixaDiferentesGeraMesmoHash() {
        llmReturns(Verdict.SAFE, ScamType.NONE, 0.9);
        service.analyze(device, request(List.of()));
        service.analyze(device, new AnalyzeRequest(Source.SMS, "  " + TEXT.toUpperCase() + "\n", List.of(), Instant.now()));

        ArgumentCaptor<String> hashes = ArgumentCaptor.forClass(String.class);
        verify(cacheRepository, times(2)).findById(hashes.capture());
        assertThat(hashes.getAllValues().get(0)).isEqualTo(hashes.getAllValues().get(1));
    }

    @Test
    void falhaDoLlmUsaRegrasSemCacheNemNotificacao() {
        when(openAi.classify(any(), anyString(), anyList())).thenThrow(new OpenAiException("timeout"));

        AnalyzeResponse response = service.analyze(device, request(List.of(LocalSignal.URGENCY, LocalSignal.CODE_REQUEST)));

        assertThat(response.engine()).isEqualTo(Engine.RULES);
        assertThat(response.verdict()).isEqualTo(Verdict.SUSPICIOUS);
        assertThat(response.scamType()).isEqualTo("VERIFICATION_CODE");
        verify(alertRepository).save(any());
        verify(cacheRepository, never()).save(any());
        verify(notifier, never()).prepareScam(any());
    }

    @Test
    void falhaDoLlmSemSinaisNuncaDizQueEhSeguroENaoGravaAlerta() {
        when(openAi.classify(any(), anyString(), anyList())).thenThrow(new OpenAiException("timeout"));

        AnalyzeResponse response = service.analyze(device, request(null));

        assertThat(response.engine()).isEqualTo(Engine.RULES);
        assertThat(response.verdict()).isEqualTo(Verdict.SUSPICIOUS);
        assertThat(response.confidence()).isLessThan(0.5);
        assertThat(response.scamType()).isNull();
        assertThat(response.alertId()).isNull();
        assertThat(response.guardianNotified()).isFalse();
        verify(alertRepository, never()).save(any());
        verify(notifier, never()).prepareScam(any());
    }

    @Test
    void mesmaMensagemJaNotificadaReaproveitaOAlertaSemNotificar() {
        llmReturns(Verdict.SCAM, ScamType.FALSE_RELATIVE, 0.93);
        Alert existing = duplicateOf(Verdict.SCAM, 0.93);
        when(notifier.alreadyNotified(existing.getId())).thenReturn(true);

        AnalyzeResponse response = service.analyze(device, request(List.of()));

        assertThat(response.guardianNotified()).isFalse();
        assertThat(response.alertId()).isEqualTo(existing.getId());
        verify(alertRepository, never()).save(any());
        verify(notifier, never()).prepareScam(any());
    }

    @Test
    void mesmaMensagemNuncaNotificadaReaproveitaOAlertaENotifica() {
        llmReturns(Verdict.SCAM, ScamType.FALSE_RELATIVE, 0.93);
        Alert existing = duplicateOf(Verdict.SCAM, 0.93);
        when(notifier.alreadyNotified(existing.getId())).thenReturn(false);

        AnalyzeResponse response = service.analyze(device, request(List.of()));

        assertThat(response.guardianNotified()).isTrue();
        assertThat(response.alertId()).isEqualTo(existing.getId());
        assertThat(sends).hasValue(1);
        verify(alertRepository, never()).save(any());
        verify(notifier).prepareScam(any());
    }

    @Test
    void mesmaMensagemSuspeitaNaoConsultaEntregas() {
        llmReturns(Verdict.SUSPICIOUS, ScamType.OTHER, 0.6);
        duplicateOf(Verdict.SUSPICIOUS, 0.6);

        AnalyzeResponse response = service.analyze(device, request(List.of()));

        assertThat(response.guardianNotified()).isFalse();
        verify(alertRepository, never()).save(any());
        verify(notifier, never()).alreadyNotified(any());
        verify(notifier, never()).prepareScam(any());
    }

    @Test
    void vereditoQuePioraGeraNovoAlertaENotifica() {
        llmReturns(Verdict.SCAM, ScamType.FALSE_RELATIVE, 0.93);
        duplicateOf(Verdict.SUSPICIOUS, 0.6);

        AnalyzeResponse response = service.analyze(device, request(List.of()));

        assertThat(response.guardianNotified()).isTrue();
        verify(alertRepository).save(any());
        verify(notifier).prepareScam(any());
    }

    private Alert duplicateOf(Verdict verdict, double confidence) {
        Alert existing = new Alert(device.getId(), Source.WHATSAPP, verdict, "FALSE_RELATIVE", confidence, "Resumo", "h");
        when(alertRepository.findFirstByProtectedIdAndTextHashAndCreatedAtAfterOrderByCreatedAtDesc(any(), anyString(), any()))
                .thenReturn(Optional.of(existing));
        return existing;
    }

    private void llmReturns(Verdict verdict, ScamType type, double confidence) {
        when(openAi.classify(any(), anyString(), anyList()))
                .thenReturn(new LlmVerdict(verdict, type, confidence, "Mensagem ao idoso", "Resumo ao familiar"));
    }

    private AnalyzeRequest request(List<LocalSignal> signals) {
        return new AnalyzeRequest(Source.WHATSAPP, TEXT, signals, Instant.now());
    }
}
