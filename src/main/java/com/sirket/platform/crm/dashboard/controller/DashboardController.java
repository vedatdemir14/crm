package com.sirket.platform.crm.dashboard.controller;

import com.sirket.platform.crm.dashboard.dto.DashboardDtos;
import com.sirket.platform.crm.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard and reporting endpoints. Restricted to sales managers and admins, following the roles
 * in the API design document; the underlying queries still apply the ownership restriction, so
 * opening these up to sales reps later would scope their figures automatically.
 */
@RestController
@RequestMapping("/api/crm")
@Tag(name = "CRM — Dashboard ve Raporlar")
@PreAuthorize("hasAnyRole('SALES_MANAGER', 'ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Açık fırsat sayısı ve toplam değeri, kazanma/kaybetme sayıları ve kazanma oranı")
    public DashboardDtos.SummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID owner) {
        return dashboardService.summary(from, to, owner);
    }

    @GetMapping("/dashboard/pipeline")
    @Operation(summary = "Açık fırsatların aşama bazlı dağılımı (boş aşamalar da sıfır ile listelenir)")
    public DashboardDtos.StageDistributionResponse pipeline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID owner) {
        return dashboardService.pipeline(from, to, owner);
    }

    @GetMapping("/reports/lost-reasons")
    @Operation(summary = "Kayıp nedenlerinin dağılımı (FR-CRM-09)")
    public DashboardDtos.LostReasonsResponse lostReasons(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID owner) {
        return dashboardService.lostReasons(from, to, owner);
    }
}
