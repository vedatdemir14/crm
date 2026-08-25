package com.sirket.platform.crm.task.dto;

import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.opportunity.domain.Opportunity;
import com.sirket.platform.crm.task.domain.Task;
import com.sirket.platform.crm.task.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class TaskDtos {

    private TaskDtos() {
    }

    public record TaskRequest(
            @NotBlank(message = "Görev başlığı zorunludur") @Size(max = 255) String title,
            String description,
            @NotNull(message = "Son tarih zorunludur") LocalDate dueDate,
            @NotNull(message = "Sorumlu zorunludur") UUID assignedTo,
            UUID relatedContactId,
            UUID relatedOpportunityId) {
    }

    public record TaskResponse(
            UUID id,
            String title,
            String description,
            LocalDate dueDate,
            TaskStatus status,
            UUID assignedTo,
            UUID relatedContactId,
            String relatedContactName,
            UUID relatedOpportunityId,
            String relatedOpportunityName,
            Instant completedAt,
            UUID createdBy,
            Instant createdAt) {

        public static TaskResponse from(Task task) {
            Contact contact = task.getRelatedContact();
            Opportunity opportunity = task.getRelatedOpportunity();
            return new TaskResponse(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getDueDate(),
                    task.getStatus(),
                    task.getAssignedTo(),
                    contact != null ? contact.getId() : null,
                    contact != null ? contact.getFirstName() + " " + contact.getLastName() : null,
                    opportunity != null ? opportunity.getId() : null,
                    opportunity != null ? opportunity.getName() : null,
                    task.getCompletedAt(),
                    task.getCreatedBy(),
                    task.getCreatedAt());
        }
    }
}
