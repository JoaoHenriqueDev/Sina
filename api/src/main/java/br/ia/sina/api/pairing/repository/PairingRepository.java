package br.ia.sina.api.pairing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ia.sina.api.pairing.model.Pairing;

import java.util.List;
import java.util.UUID;

public interface PairingRepository extends JpaRepository<Pairing, UUID> {

    boolean existsByProtectedIdAndGuardianId(UUID protectedId, UUID guardianId);

    List<Pairing> findByGuardianIdOrderByCreatedAt(UUID guardianId);

    List<Pairing> findByProtectedIdOrderByCreatedAt(UUID protectedId);
}
