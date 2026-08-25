package com.sirket.platform.hr.onboarding.domain;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.hr.employee.domain.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

@Entity
@Table(name = "onboarding_tasks", schema = "hr")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class OnboardingTask {

    @Id
    private UUID id;

    // Employee is a soft-deleted entity, which Hibernate refuses to map lazily.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private OnboardingTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OnboardingTaskStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Who has to do it — IT for access, HR for paperwork. Optional while it is unassigned. */
    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OnboardingTask() {
    }

    public OnboardingTask(Employee employee, String taskName, OnboardingTaskType taskType, LocalDate dueDate,
            UUID assignedTo) {
        this.id = UUID.randomUUID();
        this.employee = employee;
        this.taskName = taskName;
        this.taskType = taskType;
        this.status = OnboardingTaskStatus.PENDING;
        this.dueDate = dueDate;
        this.assignedTo = assignedTo;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String taskName, LocalDate dueDate, UUID assignedTo) {
        requirePending();
        this.taskName = taskName;
        this.dueDate = dueDate;
        this.assignedTo = assignedTo;
        this.updatedAt = Instant.now();
    }

    /**
     * Records who closed the item as well as when. An offboarding checklist that cannot say who
     * confirmed the access was revoked is not evidence of anything.
     */
    public void complete(UUID completedBy) {
        requirePending();
        this.status = OnboardingTaskStatus.DONE;
        this.completedAt = Instant.now();
        this.completedBy = completedBy;
        this.updatedAt = this.completedAt;
    }

    public void reopen() {
        if (status == OnboardingTaskStatus.PENDING) {
            throw new ApiExceptions.Conflict("Görev zaten açık");
        }
        this.status = OnboardingTaskStatus.PENDING;
        this.completedAt = null;
        this.completedBy = null;
        this.updatedAt = Instant.now();
    }

    public boolean isPending() {
        return status == OnboardingTaskStatus.PENDING;
    }

    public boolean isOverdue(LocalDate today) {
        return isPending() && dueDate != null && dueDate.isBefore(today);
    }

    private void requirePending() {
        if (status != OnboardingTaskStatus.PENDING) {
            throw new ApiExceptions.Conflict("Görev zaten tamamlanmış");
        }
    }

    public UUID getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public String getTaskName() {
        return taskName;
    }

    public OnboardingTaskType getTaskType() {
        return taskType;
    }

    public OnboardingTaskStatus getStatus() {
        return status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public UUID getCompletedBy() {
        return completedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
