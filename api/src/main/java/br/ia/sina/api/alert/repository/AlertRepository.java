package br.ia.sina.api.alert.repository;

import br.ia.sina.api.alert.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
}
