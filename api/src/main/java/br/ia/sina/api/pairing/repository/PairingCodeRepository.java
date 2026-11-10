package br.ia.sina.api.pairing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ia.sina.api.pairing.model.PairingCode;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PairingCodeRepository extends JpaRepository<PairingCode, UUID> {

    Optional<PairingCode> findByCodeAndUsedAtIsNull(String code);

    @Modifying
    @Query("delete from PairingCode c where c.usedAt is null and (c.expiresAt < :now or c.protectedId = :protectedId)")
    void deleteExpiredOrActiveOf(@Param("protectedId") UUID protectedId, @Param("now") Instant now);

    @Modifying
    @Query(nativeQuery = true, value = """
            insert into pairing_code (code, protected_id, expires_at)
            values (:code, :protectedId, :expiresAt)
            on conflict (code) where used_at is null do nothing""")
    int insertIfFree(@Param("code") String code, @Param("protectedId") UUID protectedId, @Param("expiresAt") Instant expiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PairingCode c set c.usedAt = :now where c.id = :id and c.usedAt is null")
    int markUsed(@Param("id") UUID id, @Param("now") Instant now);
}
