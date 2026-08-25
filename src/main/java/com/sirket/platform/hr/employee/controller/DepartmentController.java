package com.sirket.platform.hr.employee.controller;

import com.sirket.platform.hr.employee.dto.DepartmentDtos;
import com.sirket.platform.hr.employee.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/departments")
@Tag(name = "İK — Departmanlar")
@PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @Operation(summary = "Departman listesi (düz liste, isme göre sıralı)")
    public List<DepartmentDtos.DepartmentResponse> list() {
        return departmentService.list();
    }

    @GetMapping("/org-chart")
    @Operation(summary = "Departman hiyerarşisi, her düğümde doğrudan bağlı çalışan sayısı ile")
    public List<DepartmentDtos.DepartmentNode> orgChart() {
        return departmentService.orgChart();
    }

    @PostMapping
    @Operation(summary = "Yeni departman tanımlar")
    public ResponseEntity<DepartmentDtos.DepartmentResponse> create(
            @Valid @RequestBody DepartmentDtos.DepartmentRequest request) {
        DepartmentDtos.DepartmentResponse created = departmentService.create(request);
        return ResponseEntity.created(URI.create("/api/hr/departments/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Departmanı günceller")
    public DepartmentDtos.DepartmentResponse update(
            @PathVariable UUID id, @Valid @RequestBody DepartmentDtos.DepartmentRequest request) {
        return departmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Departmanı siler (soft delete); alt departman veya çalışan varsa reddedilir")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
