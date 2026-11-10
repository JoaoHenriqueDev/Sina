package br.ia.sina.api.alert.model;

import br.ia.sina.api.analysis.model.Source;
import br.ia.sina.api.analysis.model.Verdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert")
@Getter
@NoArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID protectedId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private Source source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private Verdict verdict;

    @Column(updatable = false, length = 40)
    private String scamType;

    @Column(nullable = false, updatable = false)
    private Double confidence;

    @Column(nullable = false, updatable = false, length = 500)
    private String familySummary;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Alert(UUID protectedId, Source source, Verdict verdict, String scamType, double confidence, String familySummary) {
        this.protectedId = protectedId;
        this.source = source;
        this.verdict = verdict;
        this.scamType = scamType;
        this.confidence = confidence;
        this.familySummary = familySummary;
    }
}
