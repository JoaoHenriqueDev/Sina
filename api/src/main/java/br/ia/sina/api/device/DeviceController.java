package br.ia.sina.api.device;

import br.ia.sina.api.device.DeviceDtos.FcmTokenRequest;
import br.ia.sina.api.device.DeviceDtos.HeartbeatRequest;
import br.ia.sina.api.device.DeviceDtos.RegisterRequest;
import br.ia.sina.api.device.DeviceDtos.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/devices")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PutMapping("/me/fcm-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateFcmToken(@CurrentDevice Device device, @Valid @RequestBody FcmTokenRequest request) {
        service.updateFcmToken(device, request.fcmToken());
    }

    @PostMapping("/me/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresRole(DeviceRole.PROTECTED)
    void heartbeat(@CurrentDevice Device device, @Valid @RequestBody HeartbeatRequest request) {
        service.heartbeat(device, request.listenerEnabled());
    }
}
