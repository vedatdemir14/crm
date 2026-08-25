package com.sirket.platform.hr.onboarding.dto;

import com.sirket.platform.hr.onboarding.domain.OnboardingTask;
import com.sirket.platform.hr.onboarding.domain.OnboardingTaskStatus;
import com.sirket.platform.hr.onboarding.domain.OnboardingTaskTemplate;
import com.sirket.platform.hr.onboarding.domain.OnboardingTaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class OnboardingDtos {

    private OnboardingDtos() {
    }

    // --- templates ---

    public record TemplateRequest(
            @NotBlank(message = "Görev adı zorunludur") @Size(max = 255) String name,
            @NotNull(message = "Görev türü zorunludur") OnboardingTaskType taskType,
            @PositiveOrZero(message = "Sıra değeri negatif olamaz") int displayOrder,
            int offsetDays) {
    }

    public record TemplateResponse(
            UUID id, String name, OnboardingTaskType taskType, int displayOrder, int offsetDays) {

        public static TemplateResponse from(OnboardingTaskTemplate template) {
            return new TemplateResponse(template.getId(), template.getName(), template.getTaskType(),
                    template.getDisplayOrder(), template.getOffsetDays());
        }
    }

    // --- tasks ---

    public record TaskRequest(
            @NotNull(message = "Çalışan zorunludur") UUID employeeId,
            @NotBlank(message = "Görev adı zorunludur") @Size(max = 255) String taskName,
            @NotNull(message = "Görev türü zorunludur") OnboardingTaskType taskType,
            LocalDate dueDate,
            UUID assignedTo) {
    }

    public record UpdateTaskRequest(
            @NotBlank(message = "Görev adı zorunludur") @Size(max = 255) String taskName,
            LocalDate dueDate,
            UUID assignedTo) {
    }

    public record TaskResponse(
            UUID id,
            UUID employeeId,
            String employeeName,
            String taskName,
            OnboardingTaskType taskType,
            OnboardingTaskStatus status,
            LocalDate dueDate,
            boolean overdue,
            UUID assignedTo,
            Instant completedAt,
            UUID completedBy,
            Instant createdAt) {

        public static TaskResponse from(OnboardingTask task) {
            return new TaskResponse(
                    task.getId(),
                    task.getEmployee().getId(),
                    task.getEmployee().getFullName(),
                    task.getTaskName(),
                    task.getTaskType(),
                    task.getStatus(),
                    task.getDueDate(),
                    task.isOverdue(LocalDate.now()),
                    task.getAssignedTo(),
                    task.getCompletedAt(),
                    task.getCompletedBy(),
                    task.getCreatedAt());
        }
    }

    /**
     * Reports what applying a template actually did. {@code skipped} matters: re-running a template
     * is allowed, and the caller needs to see which items already existed rather than being told
     * everything was created.
     */
    public record ApplyTemplateResponse(
            OnboardingTaskType taskType,
            LocalDate anchorDate,
            int created,
            int skipped,
            List<TaskResponse> tasks) {
    }

    /**
     * A checklist at a glance — how far along a joiner or leaver is without counting rows by hand.
     */
    public record ChecklistSummary(
            UUID employeeId,
            String employeeName,
            OnboardingTaskType taskType,
            int total,
            int completed,
            int overdue,
            List<TaskResponse> tasks) {
    }
}
