package com.sirket.platform.crm.opportunity.controller;

import com.sirket.platform.crm.opportunity.domain.OpportunityStatus;
import com.sirket.platform.crm.opportunity.dto.OpportunityDtos;
import com.sirket.platform.crm.opportunity.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm")
@Tag(name = "CRM — Fırsatlar")
@PreAuthorize("hasAnyRole('SALES_REP', 'SALES_MANAGER', 'ADMIN')")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @GetMapping("/opportunities")
    @Operation(summary = "Fırsat listesi (satış temsilcisi yalnızca kendi kayıtlarını görür)")
    public Page<OpportunityDtos.OpportunityResponse> list(
            @RequestParam(required = false) UUID stage,
            @RequestParam(required = false) OpportunityStatus status,
            @RequestParam(required = false) UUID contactId,
            @RequestParam(required = false) UUID owner,
            @PageableDefault(size = 20) Pageable pageable) {
        return opportunityService.search(stage, status, contactId, owner, pageable);
    }

    /**
     * Declared here rather than on the contact controller so the contact package keeps no
     * dependency on the opportunity package; the path still follows the API design document.
     */
    @GetMapping("/contacts/{contactId}/opportunities")
    @Operation(summary = "Kişiye bağlı fırsatlar")
    public Page<OpportunityDtos.OpportunityResponse> listByContact(
            @PathVariable UUID contactId,
            @PageableDefault(size = 20) Pageable pageable) {
        return opportunityService.search(null, null, contactId, null, pageable);
    }

    @PostMapping("/opportunities")
    @Operation(summary = "Yeni fırsat oluşturur (tutar, kapanış tarihi, olasılık)")
    public ResponseEntity<OpportunityDtos.OpportunityResponse> create(
            @Valid @RequestBody OpportunityDtos.CreateOpportunityRequest request) {
        OpportunityDtos.OpportunityResponse created = opportunityService.create(request);
        return ResponseEntity.created(URI.create("/api/crm/opportunities/" + created.id())).body(created);
    }

    @GetMapping("/opportunities/{id}")
    @Operation(summary = "Fırsat detayı")
    public OpportunityDtos.OpportunityResponse get(@PathVariable UUID id) {
        return opportunityService.get(id);
    }

    @PutMapping("/opportunities/{id}")
    @Operation(summary = "Fırsat bilgilerini günceller")
    public OpportunityDtos.OpportunityResponse update(
            @PathVariable UUID id, @Valid @RequestBody OpportunityDtos.UpdateOpportunityRequest request) {
        return opportunityService.update(id, request);
    }

    @PatchMapping("/opportunities/{id}/stage")
    @Operation(summary = "Fırsatı başka bir pipeline aşamasına taşır (kapanış aşamaları hariç)")
    public OpportunityDtos.OpportunityResponse changeStage(
            @PathVariable UUID id, @Valid @RequestBody OpportunityDtos.ChangeStageRequest request) {
        return opportunityService.changeStage(id, request.stageId());
    }

    @PatchMapping("/opportunities/{id}/close")
    @Operation(summary = "Fırsatı kazanıldı/kaybedildi olarak kapatır; kaybedildiyse kayıp nedeni zorunlu")
    public OpportunityDtos.OpportunityResponse close(
            @PathVariable UUID id, @Valid @RequestBody OpportunityDtos.CloseOpportunityRequest request) {
        return opportunityService.close(id, request.won(), request.lostReason());
    }

    @DeleteMapping("/opportunities/{id}")
    @Operation(summary = "Fırsatı siler (soft delete)")
    @PreAuthorize("hasAnyRole('SALES_MANAGER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        opportunityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
