package br.ia.sina.api.alert.repository;

import br.ia.sina.api.alert.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findFirstByProtectedIdAndTextHashAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID protectedId, String textHash, Instant after);

    /**
     * Serializa, até o fim da transação atual, as análises com a mesma chave (idoso + texto),
     * para que requisições simultâneas não gravem nem notifiquem o mesmo alerta duas vezes.
     */
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:key)", nativeQuery = true)
    Integer lockAnalysis(@Param("key") long key);

    @Query(value = """
            SELECT * FROM alert
            WHERE protected_id IN (:protectedIds)
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Alert> findFirstPage(@Param("protectedIds") Collection<UUID> protectedIds, @Param("limit") int limit);

    @Query(value = """
            SELECT * FROM alert
            WHERE protected_id IN (:protectedIds)
              AND (created_at, id) < (:createdAt, :id)
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Alert> findPageAfter(@Param("protectedIds") Collection<UUID> protectedIds, @Param("createdAt") Instant createdAt,
                              @Param("id") UUID id, @Param("limit") int limit);
}
