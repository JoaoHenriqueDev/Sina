package br.ia.sina.api.analysis.service;

import br.ia.sina.api.alert.model.Alert;
import br.ia.sina.api.alert.repository.AlertRepository;
import br.ia.sina.api.analysis.dto.AnalysisDtos.AnalyzeRequest;
import br.ia.sina.api.analysis.dto.AnalysisDtos.AnalyzeResponse;
import br.ia.sina.api.analysis.model.AnalysisCache;
import br.ia.sina.api.analysis.model.Engine;
import br.ia.sina.api.analysis.model.LocalSignal;
import br.ia.sina.api.analysis.model.ScamType;
import br.ia.sina.api.analysis.model.Verdict;
import br.ia.sina.api.analysis.openai.LlmVerdict;
import br.ia.sina.api.analysis.openai.OpenAiClient;
import br.ia.sina.api.analysis.repository.AnalysisCacheRepository;
import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.service.TokenService;
import br.ia.sina.api.notification.GuardianNotifier;
import br.ia.sina.api.notification.ScamNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AnalysisService {

    public static final double NOTIFY_THRESHOLD = 0.8;
    public static final Duration CACHE_TTL = Duration.ofDays(7);

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private record Result(LlmVerdict verdict, Engine engine) {
    }

    private final OpenAiClient openAi;
    private final AnalysisCacheRepository cacheRepository;
    private final AlertRepository alertRepository;
    private final GuardianNotifier notifier;
    private final TokenService tokenService;
    private final AnalysisRateLimiter limiter;
    private final ObjectMapper mapper;

    public AnalysisService(OpenAiClient openAi, AnalysisCacheRepository cacheRepository, AlertRepository alertRepository,
                           GuardianNotifier notifier, TokenService tokenService, AnalysisRateLimiter limiter,
                           ObjectMapper mapper) {
        this.openAi = openAi;
        this.cacheRepository = cacheRepository;
        this.alertRepository = alertRepository;
        this.notifier = notifier;
        this.tokenService = tokenService;
        this.limiter = limiter;
        this.mapper = mapper;
    }

    public AnalyzeResponse analyze(Device device, AnalyzeRequest request) {
        limiter.acquire(device.getId());
        List<LocalSignal> signals = request.localSignals() == null ? List.of() : request.localSignals();
        String hash = textHash(request.text());

        Result result = cached(hash)
                .map(v -> new Result(v, Engine.CACHE))
                .orElseGet(() -> fresh(hash, request, signals));
        LlmVerdict v = result.verdict();
        String scamType = v.scamType() == ScamType.NONE ? null : v.scamType().name();

        if (v.verdict() == Verdict.SAFE) {
            return new AnalyzeResponse(v.verdict(), scamType, v.confidence(), v.elderlyMessage(), result.engine(), null, false);
        }
        Alert alert = alertRepository.save(new Alert(device.getId(), request.source(), v.verdict(), scamType,
                v.confidence(), v.familySummary()));
        boolean notified = v.verdict() == Verdict.SCAM && v.confidence() >= NOTIFY_THRESHOLD
                && notifier.notifyScam(ScamNotification.of(alert, request.text()));
        return new AnalyzeResponse(v.verdict(), scamType, v.confidence(), v.elderlyMessage(), result.engine(), alert.getId(), notified);
    }

    private Optional<LlmVerdict> cached(String hash) {
        Instant limit = Instant.now().minus(CACHE_TTL);
        return cacheRepository.findById(hash)
                .filter(c -> c.getCreatedAt().isAfter(limit))
                .flatMap(c -> {
                    try {
                        return Optional.of(mapper.readValue(c.getResponseJson(), LlmVerdict.class));
                    } catch (JacksonException e) {
                        return Optional.empty();
                    }
                });
    }

    private Result fresh(String hash, AnalyzeRequest request, List<LocalSignal> signals) {
        LlmVerdict verdict;
        try {
            verdict = openAi.classify(request.source(), request.text(), signals);
        } catch (RuntimeException e) {
            log.warn("Falha na análise pelo LLM, usando regras locais: {}", e.getClass().getSimpleName());
            return new Result(LocalRules.evaluate(signals), Engine.RULES);
        }
        try {
            cacheRepository.save(new AnalysisCache(hash, verdict.verdict(), mapper.writeValueAsString(verdict)));
        } catch (DataIntegrityViolationException e) {
            log.debug("Cache já gravado por requisição concorrente");
        }
        return new Result(verdict, Engine.LLM);
    }

    private String textHash(String text) {
        String normalized = text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return tokenService.hash("analysis:" + normalized);
    }
}
