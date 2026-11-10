package br.ia.sina.api.analysis.service;

import br.ia.sina.api.analysis.repository.AnalysisCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AnalysisCacheCleanup {

    private static final Logger log = LoggerFactory.getLogger(AnalysisCacheCleanup.class);

    private final AnalysisCacheRepository repository;

    public AnalysisCacheCleanup(AnalysisCacheRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 17 * * * *")
    public void run() {
        int removed = repository.deleteOlderThan(Instant.now().minus(AnalysisService.CACHE_TTL));
        if (removed > 0) {
            log.info("Cache de análise: {} entradas expiradas removidas", removed);
        }
    }
}
