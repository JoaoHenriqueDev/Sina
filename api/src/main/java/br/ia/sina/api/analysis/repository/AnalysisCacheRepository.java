package br.ia.sina.api.analysis.repository;

import br.ia.sina.api.analysis.model.AnalysisCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisCacheRepository extends JpaRepository<AnalysisCache, String> {
}
