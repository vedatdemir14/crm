package com.sirket.platform.hr.employee.controller;

import com.sirket.platform.hr.employee.domain.EmployeeStatus;
import com.sirket.platform.hr.employee.dto.EmployeeDtos;
import com.sirket.platform.hr.employee.service.EmployeeService;
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
@RequestMapping("/api/hr/employees")
@Tag(name = "İK — Çalışanlar")
@PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @Operation(summary = "Çalışan listesi; kimlik numarası bu yanıtta hiç yer almaz")
    public Page<EmployeeDtos.EmployeeResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID department,
            @RequestParam(required = false) UUID manager,
            @RequestParam(required = false) EmployeeStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return employeeService.search(name, department, manager, status, pageable);
    }

    @PostMapping
    @Operation(summary = "Yeni çalışan kaydı oluşturur")
    public ResponseEntity<EmployeeDtos.EmployeeResponse> create(
            @Valid @RequestBody EmployeeDtos.EmployeeRequest request) {
        EmployeeDtos.EmployeeResponse created = employeeService.create(request);
        return ResponseEntity.created(URI.create("/api/hr/employees/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Çalışan detayı; kimlik numarası varsayılan olarak maskelenir")
    public EmployeeDtos.EmployeeResponse get(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean includeNationalId) {
        return employeeService.get(id, includeNationalId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Çalışan bilgilerini günceller")
    public EmployeeDtos.EmployeeResponse update(
            @PathVariable UUID id, @Valid @RequestBody EmployeeDtos.EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Çalışanın durumunu değiştirir (aktif/izinde)")
    public EmployeeDtos.EmployeeResponse changeStatus(
            @PathVariable UUID id, @Valid @RequestBody EmployeeDtos.ChangeStatusRequest request) {
        return employeeService.changeStatus(id, request.status());
    }

    @PatchMapping("/{id}/terminate")
    @Operation(summary = "Çalışanı işten ayrılmış olarak işaretler (kayıt silinmez, pasife alınır)")
    public EmployeeDtos.EmployeeResponse terminate(
            @PathVariable UUID id, @Valid @RequestBody EmployeeDtos.TerminateRequest request) {
        return employeeService.terminate(id, request.terminationDate());
    }
}
