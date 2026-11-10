package br.ia.sina.api.analysis.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "analysis_cache")
@Getter
@NoArgsConstructor
public class AnalysisCache {

    @Id
    @Column(length = 64)
    private String textHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Verdict verdict;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String responseJson;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public AnalysisCache(String textHash, Verdict verdict, String responseJson) {
        this.textHash = textHash;
        this.verdict = verdict;
        this.responseJson = responseJson;
    }
}
