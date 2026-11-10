package br.ia.sina.api.pairing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pairing")
@Getter
@NoArgsConstructor
public class Pairing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID protectedId;

    @Column(nullable = false, updatable = false)
    private UUID guardianId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Pairing(UUID protectedId, UUID guardianId) {
        this.protectedId = protectedId;
        this.guardianId = guardianId;
    }
}
