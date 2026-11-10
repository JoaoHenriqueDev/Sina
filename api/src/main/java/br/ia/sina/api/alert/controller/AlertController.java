package br.ia.sina.api.alert.controller;

import br.ia.sina.api.alert.dto.AlertDtos.AlertPage;
import br.ia.sina.api.alert.service.AlertService;
import br.ia.sina.api.device.CurrentDevice;
import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.role.DeviceRole;
import br.ia.sina.api.device.role.RequiresRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/alerts")
public class AlertController {

    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresRole(DeviceRole.GUARDIAN)
    AlertPage list(@CurrentDevice Device device,
                   @RequestParam(required = false) UUID protectedId,
                   @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
                   @RequestParam(required = false) String cursor) {
        return service.list(device, protectedId, limit, cursor);
    }
}
