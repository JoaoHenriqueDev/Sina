package br.ia.sina.api.analysis.controller;

import br.ia.sina.api.analysis.dto.AnalysisDtos.AnalyzeRequest;
import br.ia.sina.api.analysis.dto.AnalysisDtos.AnalyzeResponse;
import br.ia.sina.api.analysis.service.AnalysisService;
import br.ia.sina.api.device.CurrentDevice;
import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.role.DeviceRole;
import br.ia.sina.api.device.role.RequiresRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/analyze")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresRole(DeviceRole.PROTECTED)
    AnalyzeResponse analyze(@CurrentDevice Device device, @Valid @RequestBody AnalyzeRequest request) {
        return service.analyze(device, request);
    }
}
