package com.sirket.platform.hr.leave.controller;

import com.sirket.platform.hr.leave.dto.LeaveDtos;
import com.sirket.platform.hr.leave.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The self-service half of FR-HR-02. The rest of the ESS portal (FR-HR-09) is separate work; without
 * these endpoints an employee could not raise a leave request at all.
 */
@RestController
@RequestMapping("/api/me")
@Tag(name = "Çalışan Self-Servis — İzin")
@PreAuthorize("isAuthenticated()")
public class MyLeaveController {

    private final LeaveService leaveService;

    public MyLeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/leave-balance")
    @Operation(summary = "Kendi izin bakiyem")
    public List<LeaveDtos.LeaveBalanceResponse> myBalance(@RequestParam(required = false) Integer year) {
        return leaveService.myBalances(year);
    }

    @GetMapping("/leave-requests")
    @Operation(summary = "Kendi izin taleplerimin geçmişi")
    public Page<LeaveDtos.LeaveRequestResponse> myRequests(@PageableDefault(size = 20) Pageable pageable) {
        return leaveService.myRequests(pageable);
    }

    @PostMapping("/leave-requests")
    @Operation(summary = "Yeni izin talebi oluşturur; yöneticinin onayına düşer")
    public ResponseEntity<LeaveDtos.LeaveRequestResponse> request(
            @Valid @RequestBody LeaveDtos.LeaveRequestInput input) {
        LeaveDtos.LeaveRequestResponse created = leaveService.requestLeaveForCurrentUser(input);
        return ResponseEntity.created(URI.create("/api/me/leave-requests/" + created.id())).body(created);
    }

    @DeleteMapping("/leave-requests/{id}")
    @Operation(summary = "Henüz sonuçlanmamış kendi talebimi iptal eder")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        leaveService.cancelOwnRequest(id);
        return ResponseEntity.noContent().build();
    }
}
