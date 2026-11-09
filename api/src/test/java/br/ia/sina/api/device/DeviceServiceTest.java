package br.ia.sina.api.device;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import br.ia.sina.api.config.SinaProperties;
import br.ia.sina.api.device.DeviceDtos.RegisterRequest;
import br.ia.sina.api.device.DeviceDtos.RegisterResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceServiceTest {

    private final DeviceRepository repository = mock(DeviceRepository.class);
    private final TokenService tokenService = new TokenService(new SinaProperties("p".repeat(32)));
    private final DeviceService service = new DeviceService(repository, tokenService);

    @Test
    void registroGravaSoOHashDoToken() {
        UUID id = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(inv -> {
            Device d = inv.getArgument(0);
            d.setId(id);
            return d;
        });

        RegisterResponse response = service.register(new RegisterRequest(DeviceRole.GUARDIAN, "fcm", "Ana", "ana@exemplo.com"));

        ArgumentCaptor<Device> saved = ArgumentCaptor.forClass(Device.class);
        verify(repository).save(saved.capture());
        assertThat(response.deviceId()).isEqualTo(id);
        assertThat(saved.getValue().getTokenHash()).isEqualTo(tokenService.hash(response.deviceToken()));
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(response.deviceToken());
    }

    @Test
    void emailEmProtectedEhRecusado() {
        assertThatThrownBy(() -> service.register(new RegisterRequest(DeviceRole.PROTECTED, null, null, "a@b.com")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(repository, never()).save(any());
    }

    @Test
    void heartbeatTrueAtualizaLastSeen() {
        Device device = new Device();
        service.heartbeat(device, true);
        assertThat(device.getLastSeenAt()).isNotNull();
        verify(repository).save(device);
    }

    @Test
    void heartbeatFalseNaoAtualiza() {
        Device device = new Device();
        service.heartbeat(device, false);
        assertThat(device.getLastSeenAt()).isNull();
        verify(repository, never()).save(any());
    }
}
