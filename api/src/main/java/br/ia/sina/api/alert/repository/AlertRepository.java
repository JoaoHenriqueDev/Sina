package br.ia.sina.api.alert.repository;

import br.ia.sina.api.alert.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

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
