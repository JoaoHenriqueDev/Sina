package br.ia.sina.api.device;

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
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device")
@Getter
@Setter
@NoArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeviceRole role;

    @Column(length = 80)
    private String displayName;

    @Column(length = 4096)
    private String fcmToken;

    @Column(length = 254)
    private String email;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    private Instant lastSeenAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
