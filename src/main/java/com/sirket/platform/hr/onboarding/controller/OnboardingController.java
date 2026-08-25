package com.sirket.platform.hr.onboarding.controller;

import com.sirket.platform.hr.onboarding.domain.OnboardingTaskStatus;
import com.sirket.platform.hr.onboarding.domain.OnboardingTaskType;
import com.sirket.platform.hr.onboarding.dto.OnboardingDtos;
import com.sirket.platform.hr.onboarding.service.OnboardingService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-HR-06 and FR-HR-07. Managing the checklists is HR's job; the one exception is the personal
 * view and completion of an item that was assigned to you, since the people doing the work — IT
 * revoking access, a manager taking handover — are not HR.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "İK — Oryantasyon ve Çıkış")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    // --- templates ---

    @GetMapping("/hr/onboarding-task-templates")
    @Operation(summary = "Görev şablonlarını listeler")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public List<OnboardingDtos.TemplateResponse> listTemplates(
            @RequestParam(required = false) OnboardingTaskType type) {
        return onboardingService.listTemplates(type);
    }

    @PostMapping("/hr/onboarding-task-templates")
    @Operation(summary = "Yeni görev şablonu tanımlar")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public ResponseEntity<OnboardingDtos.TemplateResponse> createTemplate(
            @Valid @RequestBody OnboardingDtos.TemplateRequest request) {
        OnboardingDtos.TemplateResponse created = onboardingService.createTemplate(request);
        return ResponseEntity.created(URI.create("/api/hr/onboarding-task-templates/" + created.id()))
                .body(created);
    }

    @PutMapping("/hr/onboarding-task-templates/{id}")
    @Operation(summary = "Görev şablonunu günceller")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public OnboardingDtos.TemplateResponse updateTemplate(
            @PathVariable UUID id, @Valid @RequestBody OnboardingDtos.TemplateRequest request) {
        return onboardingService.updateTemplate(id, request);
    }

    @DeleteMapping("/hr/onboarding-task-templates/{id}")
    @Operation(summary = "Görev şablonunu siler; hâlihazırda oluşmuş görevler etkilenmez")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        onboardingService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    // --- tasks ---

    @GetMapping("/hr/onboarding-tasks")
    @Operation(summary = "Oryantasyon/çıkış görevlerini listeler")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public List<OnboardingDtos.TaskResponse> list(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) OnboardingTaskType type,
            @RequestParam(required = false) OnboardingTaskStatus status,
            @RequestParam(required = false) UUID assignedTo) {
        return onboardingService.search(employeeId, type, status, assignedTo);
    }

    @GetMapping("/hr/employees/{employeeId}/checklist")
    @Operation(summary = "Çalışanın kontrol listesi ve ilerleme özeti")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public OnboardingDtos.ChecklistSummary checklist(
            @PathVariable UUID employeeId, @RequestParam OnboardingTaskType type) {
        return onboardingService.checklist(employeeId, type);
    }

    @PostMapping("/hr/onboarding-tasks")
    @Operation(summary = "Tek bir görev ekler")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public ResponseEntity<OnboardingDtos.TaskResponse> create(
            @Valid @RequestBody OnboardingDtos.TaskRequest request) {
        OnboardingDtos.TaskResponse created = onboardingService.create(request);
        return ResponseEntity.created(URI.create("/api/hr/onboarding-tasks/" + created.id())).body(created);
    }

    @PostMapping("/hr/employees/{employeeId}/checklist")
    @Operation(summary = "Şablondan toplu görev oluşturur; mevcut olanlar atlanır")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public OnboardingDtos.ApplyTemplateResponse applyTemplate(
            @PathVariable UUID employeeId,
            @RequestParam OnboardingTaskType type,
            @RequestParam(required = false) UUID assignTo) {
        return onboardingService.applyTemplate(employeeId, type, assignTo);
    }

    @PutMapping("/hr/onboarding-tasks/{id}")
    @Operation(summary = "Görevi günceller")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public OnboardingDtos.TaskResponse update(
            @PathVariable UUID id, @Valid @RequestBody OnboardingDtos.UpdateTaskRequest request) {
        return onboardingService.update(id, request);
    }

    @PatchMapping("/hr/onboarding-tasks/{id}/complete")
    @Operation(summary = "Görevi tamamlandı işaretler; atanan kişi veya İK yapabilir")
    @PreAuthorize("isAuthenticated()")
    public OnboardingDtos.TaskResponse complete(@PathVariable UUID id) {
        return onboardingService.complete(id);
    }

    @PatchMapping("/hr/onboarding-tasks/{id}/reopen")
    @Operation(summary = "Yanlışlıkla kapatılmış görevi yeniden açar")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public OnboardingDtos.TaskResponse reopen(@PathVariable UUID id) {
        return onboardingService.reopen(id);
    }

    @DeleteMapping("/hr/onboarding-tasks/{id}")
    @Operation(summary = "Görevi siler (soft delete)")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        onboardingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * The people who actually carry out these steps are IT and managers, not HR, so they need to
     * see their own items without access to the rest of the checklist.
     */
    @GetMapping("/me/onboarding-tasks")
    @Operation(summary = "Bana atanan oryantasyon/çıkış görevleri")
    @PreAuthorize("isAuthenticated()")
    public List<OnboardingDtos.TaskResponse> myTasks(
            @RequestParam(required = false) OnboardingTaskStatus status) {
        return onboardingService.myTasks(status);
    }
}
