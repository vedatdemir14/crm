package com.sirket.platform.hr.payroll.controller;

import com.sirket.platform.hr.payroll.dto.PayrollDtos;
import com.sirket.platform.hr.payroll.service.PayrollQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-HR-04. Every response carries {@code mock: true} while payroll is stubbed.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "İK — Bordro (mock)")
public class PayrollController {

    private final PayrollQueryService payrollQueryService;

    public PayrollController(PayrollQueryService payrollQueryService) {
        this.payrollQueryService = payrollQueryService;
    }

    @GetMapping("/hr/employees/{employeeId}/payroll-records")
    @Operation(summary = "Çalışanın bordro kayıtları — ilk sürümde mock veri döner")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public List<PayrollDtos.PayrollRecordResponse> ofEmployee(
            @PathVariable UUID employeeId,
            @RequestParam(defaultValue = "12") int months) {
        return payrollQueryService.recordsOfEmployee(employeeId, months);
    }

    @GetMapping("/hr/payroll-records/{id}")
    @Operation(summary = "Tekil bordro kaydı (mock)")
    @PreAuthorize("isAuthenticated()")
    public PayrollDtos.PayrollRecordResponse get(@PathVariable UUID id) {
        return payrollQueryService.get(id);
    }

    @GetMapping("/me/payroll-records")
    @Operation(summary = "Kendi bordro kayıtlarım (mock)")
    @PreAuthorize("isAuthenticated()")
    public List<PayrollDtos.PayrollRecordResponse> mine(@RequestParam(defaultValue = "12") int months) {
        return payrollQueryService.myRecords(months);
    }
}
