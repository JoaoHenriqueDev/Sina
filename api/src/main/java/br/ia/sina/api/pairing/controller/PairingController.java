package br.ia.sina.api.pairing.controller;

import br.ia.sina.api.device.CurrentDevice;
import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.role.DeviceRole;
import br.ia.sina.api.device.role.RequiresRole;
import br.ia.sina.api.pairing.dto.PairingDtos.CodeResponse;
import br.ia.sina.api.pairing.service.PairingService;
import br.ia.sina.api.pairing.dto.PairingDtos.PairingItem;
import br.ia.sina.api.pairing.dto.PairingDtos.PairingList;
import br.ia.sina.api.pairing.dto.PairingDtos.RedeemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class PairingController {

    private final PairingService service;

    public PairingController(PairingService service) {
        this.service = service;
    }

    @PostMapping("/pairing-codes")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresRole(DeviceRole.PROTECTED)
    CodeResponse createCode(@CurrentDevice Device device) {
        return service.createCode(device);
    }

    @PostMapping("/pairings")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresRole(DeviceRole.GUARDIAN)
    PairingItem redeem(@CurrentDevice Device device, @Valid @RequestBody RedeemRequest request) {
        return service.redeem(device, request.code());
    }

    @GetMapping("/pairings")
    PairingList list(@CurrentDevice Device device) {
        return service.list(device);
    }

    @DeleteMapping("/pairings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentDevice Device device, @PathVariable UUID id) {
        service.delete(device, id);
    }
}
