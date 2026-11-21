package br.ia.sina.api.pairing.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.repository.DeviceRepository;
import br.ia.sina.api.pairing.repository.PairingCodeRepository;
import br.ia.sina.api.pairing.repository.PairingRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PairingServiceTest {

    @Test
    void espacoDeCodigosEsgotadoDa503EmVezDe500() {
        PairingCodeRepository codes = mock(PairingCodeRepository.class);
        when(codes.insertIfFree(anyString(), any(), any())).thenReturn(0);
        PairingService service = new PairingService(codes, mock(PairingRepository.class), mock(DeviceRepository.class),
                new PairingAttemptLimiter(100, 100));
        Device idoso = new Device();
        idoso.setId(UUID.randomUUID());

        assertThatThrownBy(() -> service.createCode(idoso))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.PAIRING_UNAVAILABLE);
                    assertThat(e.code().status().value()).isEqualTo(503);
                });
    }
}
