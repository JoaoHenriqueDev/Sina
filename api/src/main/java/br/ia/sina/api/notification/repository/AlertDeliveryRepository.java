package br.ia.sina.api.notification.repository;

import br.ia.sina.api.notification.model.AlertDelivery;
import br.ia.sina.api.notification.model.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface AlertDeliveryRepository extends JpaRepository<AlertDelivery, UUID> {

    List<AlertDelivery> findByAlertIdOrderByCreatedAt(UUID alertId);

    boolean existsByAlertId(UUID alertId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AlertDelivery d set d.status = :status where d.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") DeliveryStatus status);
}
