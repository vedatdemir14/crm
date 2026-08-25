package com.sirket.platform.crm.task.controller;

import com.sirket.platform.crm.task.domain.TaskStatus;
import com.sirket.platform.crm.task.dto.TaskDtos;
import com.sirket.platform.crm.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
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
@RequestMapping("/api/crm/tasks")
@Tag(name = "CRM — Görevler")
@PreAuthorize("hasAnyRole('SALES_REP', 'SALES_MANAGER', 'ADMIN')")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "Görev listesi (satış temsilcisi yalnızca kendine atananları görür), son tarihe göre sıralı")
    public Page<TaskDtos.TaskResponse> list(
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @PageableDefault(size = 20) Pageable pageable) {
        return taskService.search(assignedTo, status, dueBefore, pageable);
    }

    @PostMapping
    @Operation(summary = "Yeni görev/hatırlatma oluşturur")
    public ResponseEntity<TaskDtos.TaskResponse> create(@Valid @RequestBody TaskDtos.TaskRequest request) {
        TaskDtos.TaskResponse created = taskService.create(request);
        return ResponseEntity.created(URI.create("/api/crm/tasks/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Görev detayı")
    public TaskDtos.TaskResponse get(@PathVariable UUID id) {
        return taskService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Görevi günceller")
    public TaskDtos.TaskResponse update(
            @PathVariable UUID id, @Valid @RequestBody TaskDtos.TaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Görevi tamamlandı olarak işaretler")
    public TaskDtos.TaskResponse complete(@PathVariable UUID id) {
        return taskService.complete(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Görevi siler (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
