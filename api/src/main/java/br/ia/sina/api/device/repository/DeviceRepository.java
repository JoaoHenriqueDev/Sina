package br.ia.sina.api.device.repository;

import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.role.DeviceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByTokenHash(String tokenHash);

    List<Device> findByRoleAndLastSeenAtBeforeAndInactiveNotifiedAtIsNull(DeviceRole role, Instant limit);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Device d set d.inactiveNotifiedAt = :now
            where d.id = :id and d.inactiveNotifiedAt is null and d.lastSeenAt < :limit""")
    int markInactiveNotified(@Param("id") UUID id, @Param("limit") Instant limit, @Param("now") Instant now);

    /** Desfaz a marca quando o aviso falhou, para o próximo ciclo tentar de novo. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Device d set d.inactiveNotifiedAt = null where d.id = :id and d.inactiveNotifiedAt = :at")
    int unmarkInactiveNotified(@Param("id") UUID id, @Param("at") Instant at);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Device d set d.fcmToken = null where d.id = :id and d.fcmToken = :token")
    int clearFcmToken(@Param("id") UUID id, @Param("token") String token);
}
