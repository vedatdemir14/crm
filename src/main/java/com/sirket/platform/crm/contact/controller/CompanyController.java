package com.sirket.platform.crm.contact.controller;

import com.sirket.platform.crm.contact.dto.CompanyDtos;
import com.sirket.platform.crm.contact.service.CompanyService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm/companies")
@Tag(name = "CRM — Firmalar")
@PreAuthorize("hasAnyRole('SALES_REP', 'SALES_MANAGER', 'ADMIN')")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(summary = "Firma listesi (satış temsilcisi yalnızca kendi kayıtlarını görür)")
    public Page<CompanyDtos.CompanyResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) UUID owner,
            @PageableDefault(size = 20) Pageable pageable) {
        return companyService.search(name, industry, owner, pageable);
    }

    @PostMapping
    @Operation(summary = "Yeni firma oluşturur")
    public ResponseEntity<CompanyDtos.CompanyResponse> create(@Valid @RequestBody CompanyDtos.CompanyRequest request) {
        CompanyDtos.CompanyResponse created = companyService.create(request);
        return ResponseEntity.created(URI.create("/api/crm/companies/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Firma detayı")
    public CompanyDtos.CompanyResponse get(@PathVariable UUID id) {
        return companyService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Firma bilgilerini günceller")
    public CompanyDtos.CompanyResponse update(
            @PathVariable UUID id, @Valid @RequestBody CompanyDtos.CompanyRequest request) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Firmayı siler (soft delete)")
    @PreAuthorize("hasAnyRole('SALES_MANAGER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
