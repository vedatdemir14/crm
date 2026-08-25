package com.sirket.platform.hr.leave.controller;

import com.sirket.platform.hr.leave.domain.LeaveStatus;
import com.sirket.platform.hr.leave.dto.LeaveDtos;
import com.sirket.platform.hr.leave.service.LeaveService;
import com.sirket.platform.hr.leave.service.LeaveTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/hr")
@Tag(name = "İK — İzin Yönetimi")
public class LeaveAdminController {

    private final LeaveService leaveService;
    private final LeaveTypeService leaveTypeService;

    public LeaveAdminController(LeaveService leaveService, LeaveTypeService leaveTypeService) {
        this.leaveService = leaveService;
        this.leaveTypeService = leaveTypeService;
    }

    // --- leave types (FR-HR-03) ---

    @GetMapping("/leave-types")
    @Operation(summary = "Tanımlı izin türlerini listeler")
    @PreAuthorize("isAuthenticated()")
    public List<LeaveDtos.LeaveTypeResponse> listTypes() {
        return leaveTypeService.listTypes();
    }

    @PostMapping("/leave-types")
    @Operation(summary = "Yeni izin türü tanımlar")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public ResponseEntity<LeaveDtos.LeaveTypeResponse> createType(
            @Valid @RequestBody LeaveDtos.LeaveTypeRequest request) {
        LeaveDtos.LeaveTypeResponse created = leaveTypeService.createType(request);
        return ResponseEntity.created(URI.create("/api/hr/leave-types/" + created.id())).body(created);
    }

    @PutMapping("/leave-types/{id}")
    @Operation(summary = "İzin türünü günceller")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public LeaveDtos.LeaveTypeResponse updateType(
            @PathVariable UUID id, @Valid @RequestBody LeaveDtos.LeaveTypeRequest request) {
        return leaveTypeService.updateType(id, request);
    }

    // --- public holidays (FR-HR-03) ---

    @GetMapping("/public-holidays")
    @Operation(summary = "Resmi tatil takvimi")
    @PreAuthorize("isAuthenticated()")
    public List<LeaveDtos.PublicHolidayResponse> listHolidays(@RequestParam(required = false) Integer year) {
        return leaveTypeService.listHolidays(year);
    }

    @PostMapping("/public-holidays")
    @Operation(summary = "Resmi tatil ekler; iş günü hesabından düşülür")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public ResponseEntity<LeaveDtos.PublicHolidayResponse> addHoliday(
            @Valid @RequestBody LeaveDtos.PublicHolidayRequest request) {
        LeaveDtos.PublicHolidayResponse created = leaveTypeService.addHoliday(request);
        return ResponseEntity.created(URI.create("/api/hr/public-holidays/" + created.id())).body(created);
    }

    @DeleteMapping("/public-holidays/{id}")
    @Operation(summary = "Resmi tatili siler")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteHoliday(@PathVariable UUID id) {
        leaveTypeService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/working-days")
    @Operation(summary = "Bir tarih aralığındaki iş günü sayısı (hafta sonu ve resmi tatiller hariç)")
    @PreAuthorize("isAuthenticated()")
    public LeaveDtos.WorkingDaysResponse workingDays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return leaveService.workingDays(from, to);
    }

    // --- leave requests (FR-HR-02) ---

    @GetMapping("/leave-requests")
    @Operation(summary = "İzin taleplerini listeler")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public Page<LeaveDtos.LeaveRequestResponse> list(
            @RequestParam(required = false) UUID employee,
            @RequestParam(required = false) UUID department,
            @RequestParam(required = false) LeaveStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return leaveService.search(employee, department, status, pageable);
    }

    @PostMapping("/employees/{employeeId}/leave-requests")
    @Operation(summary = "Çalışan adına izin talebi oluşturur")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public ResponseEntity<LeaveDtos.LeaveRequestResponse> createFor(
            @PathVariable UUID employeeId, @Valid @RequestBody LeaveDtos.LeaveRequestInput input) {
        LeaveDtos.LeaveRequestResponse created = leaveService.createFor(employeeId, input);
        return ResponseEntity.created(URI.create("/api/hr/leave-requests/" + created.id())).body(created);
    }

    /**
     * Open to any authenticated user because the decision belongs to the employee's own manager as
     * well as to HR; the service decides which of the two the caller actually is.
     */
    @PatchMapping("/leave-requests/{id}/approve")
    @Operation(summary = "İzin talebini onaylar ve bakiyeden düşer")
    @PreAuthorize("isAuthenticated()")
    public LeaveDtos.LeaveRequestResponse approve(
            @PathVariable UUID id, @RequestBody(required = false) LeaveDtos.DecisionRequest request) {
        return leaveService.approve(id, request != null ? request.note() : null);
    }

    @PatchMapping("/leave-requests/{id}/reject")
    @Operation(summary = "İzin talebini reddeder; bakiye etkilenmez")
    @PreAuthorize("isAuthenticated()")
    public LeaveDtos.LeaveRequestResponse reject(
            @PathVariable UUID id, @RequestBody(required = false) LeaveDtos.DecisionRequest request) {
        return leaveService.reject(id, request != null ? request.note() : null);
    }

    @GetMapping("/employees/{employeeId}/leave-balance")
    @Operation(summary = "Çalışanın izin bakiyesi")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public List<LeaveDtos.LeaveBalanceResponse> balance(
            @PathVariable UUID employeeId, @RequestParam(required = false) Integer year) {
        return leaveService.balancesOfEmployee(employeeId, year);
    }

    @PutMapping("/employees/{employeeId}/leave-balance")
    @Operation(summary = "Çalışanın izin bakiyesindeki toplam gün sayısını düzeltir")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public LeaveDtos.LeaveBalanceResponse adjustBalance(
            @PathVariable UUID employeeId, @Valid @RequestBody LeaveDtos.AdjustBalanceRequest request) {
        return leaveService.adjustBalance(employeeId, request);
    }
}
