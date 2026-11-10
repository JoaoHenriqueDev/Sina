package br.ia.sina.api.analysis.repository;

import br.ia.sina.api.analysis.model.AnalysisCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface AnalysisCacheRepository extends JpaRepository<AnalysisCache, String> {

    @Transactional
    @Modifying
    @Query("delete from AnalysisCache c where c.createdAt < :limit")
    int deleteOlderThan(@Param("limit") Instant limit);
}
