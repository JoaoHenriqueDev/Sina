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
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AnalysisService {

    public static final double NOTIFY_THRESHOLD = 0.8;
    public static final Duration CACHE_TTL = Duration.ofDays(7);
    public static final Duration DUPLICATE_WINDOW = Duration.ofHours(6);

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    /** persist = false quando nada foi analisado de fato (falha do LLM sem sinais locais): não vira alerta. */
    private record Result(LlmVerdict verdict, Engine engine, boolean persist) {
    }

    private record Outcome(UUID alertId, Optional<Runnable> send) {
    }

    private final OpenAiClient openAi;
    private final AnalysisCacheRepository cacheRepository;
    private final AlertRepository alertRepository;
    private final GuardianNotifier notifier;
    private final TokenService tokenService;
    private final AnalysisRateLimiter limiter;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;

    public AnalysisService(OpenAiClient openAi, AnalysisCacheRepository cacheRepository, AlertRepository alertRepository,
                           GuardianNotifier notifier, TokenService tokenService, AnalysisRateLimiter limiter,
                           ObjectMapper mapper, TransactionTemplate tx) {
        this.openAi = openAi;
        this.cacheRepository = cacheRepository;
        this.alertRepository = alertRepository;
        this.notifier = notifier;
        this.tokenService = tokenService;
        this.limiter = limiter;
        this.mapper = mapper;
        this.tx = tx;
    }

    public AnalyzeResponse analyze(Device device, AnalyzeRequest request) {
        limiter.acquire(device.getId());
        List<LocalSignal> signals = request.localSignals() == null ? List.of() : request.localSignals();
        String hash = textHash(request.text());

        Result result = cached(hash)
                .map(v -> new Result(v, Engine.CACHE, true))
                .orElseGet(() -> fresh(hash, request, signals));
        LlmVerdict v = result.verdict();
        String scamType = v.scamType() == ScamType.NONE ? null : v.scamType().name();
        log.info("Análise concluída: source={} messageCount={} engine={} verdict={}",
                request.source(), request.messageCount(), result.engine(), v.verdict());

        if (v.verdict() == Verdict.SAFE || !result.persist()) {
            return new AnalyzeResponse(v.verdict(), scamType, v.confidence(), v.elderlyMessage(), result.engine(), null, false);
        }
        boolean shouldNotify = v.verdict() == Verdict.SCAM && v.confidence() >= NOTIFY_THRESHOLD;
        // Gravação do alerta e das entregas sob trava; o envio em si só sai depois do commit,
        // para o disparo assíncrono enxergar as entregas já confirmadas.
        Outcome outcome = tx.execute(status -> persist(device.getId(), hash, request, v, scamType, shouldNotify));
        outcome.send().ifPresent(Runnable::run);
        return new AnalyzeResponse(v.verdict(), scamType, v.confidence(), v.elderlyMessage(), result.engine(),
                outcome.alertId(), outcome.send().isPresent());
    }

    private Outcome persist(UUID protectedId, String hash, AnalyzeRequest request, LlmVerdict v, String scamType,
                            boolean shouldNotify) {
        alertRepository.lockAnalysis(lockKey(protectedId, hash));
        Optional<Alert> duplicate = alertRepository
                .findFirstByProtectedIdAndTextHashAndCreatedAtAfterOrderByCreatedAtDesc(
                        protectedId, hash, Instant.now().minus(DUPLICATE_WINDOW))
                .filter(a -> a.getVerdict() == v.verdict() && (!shouldNotify || a.getConfidence() >= NOTIFY_THRESHOLD));
        if (duplicate.isPresent()) {
            Alert alert = duplicate.get();
            // Reaproveita o alerta; só aciona a família se ela ainda não foi acionada para ele
            // (ex.: o familiar pareou depois do primeiro alerta).
            boolean pending = shouldNotify && !notifier.alreadyNotified(alert.getId());
            return new Outcome(alert.getId(),
                    pending ? notifier.prepareScam(ScamNotification.of(alert, request.text())) : Optional.empty());
        }
        Alert alert = alertRepository.save(new Alert(protectedId, request.source(), v.verdict(), scamType,
                v.confidence(), v.familySummary(), hash));
        return new Outcome(alert.getId(),
                shouldNotify ? notifier.prepareScam(ScamNotification.of(alert, request.text())) : Optional.empty());
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
            return new Result(LocalRules.evaluate(signals), Engine.RULES, !signals.isEmpty());
        }
        try {
            cacheRepository.save(new AnalysisCache(hash, verdict.verdict(), mapper.writeValueAsString(verdict)));
        } catch (DataIntegrityViolationException e) {
            log.debug("Cache já gravado por requisição concorrente");
        }
        return new Result(verdict, Engine.LLM, true);
    }

    private String textHash(String text) {
        String normalized = text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return tokenService.hash("analysis:" + normalized);
    }

    /** Chave de 64 bits para o advisory lock; colisões só serializam mais, nunca liberam a trava. */
    static long lockKey(UUID protectedId, String textHash) {
        long hashBits = HexFormat.fromHexDigitsToLong(textHash, 0, 16);
        return hashBits ^ protectedId.getMostSignificantBits() ^ protectedId.getLeastSignificantBits();
    }
}
