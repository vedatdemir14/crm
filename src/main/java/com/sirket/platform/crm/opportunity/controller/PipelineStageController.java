package com.sirket.platform.crm.opportunity.controller;

import com.sirket.platform.crm.opportunity.dto.PipelineStageDtos;
import com.sirket.platform.crm.opportunity.service.PipelineStageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm/pipeline-stages")
@Tag(name = "CRM — Pipeline Aşamaları")
public class PipelineStageController {

    private final PipelineStageService stageService;

    public PipelineStageController(PipelineStageService stageService) {
        this.stageService = stageService;
    }

    @GetMapping
    @Operation(summary = "Tanımlı pipeline aşamalarını sıra ile listeler")
    @PreAuthorize("hasAnyRole('SALES_REP', 'SALES_MANAGER', 'ADMIN')")
    public List<PipelineStageDtos.PipelineStageResponse> list() {
        return stageService.list();
    }

    @PostMapping
    @Operation(summary = "Yeni pipeline aşaması tanımlar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PipelineStageDtos.PipelineStageResponse> create(
            @Valid @RequestBody PipelineStageDtos.PipelineStageRequest request) {
        PipelineStageDtos.PipelineStageResponse created = stageService.create(request);
        return ResponseEntity.created(URI.create("/api/crm/pipeline-stages/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Pipeline aşamasını günceller")
    @PreAuthorize("hasRole('ADMIN')")
    public PipelineStageDtos.PipelineStageResponse update(
            @PathVariable UUID id, @Valid @RequestBody PipelineStageDtos.PipelineStageRequest request) {
        return stageService.update(id, request);
    }
}
