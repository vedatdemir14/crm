package com.sirket.platform.hr.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

/**
 * A checklist item HR maintains once and applies to every joiner or leaver, rather than retyping
 * the same eight steps for each one. Templates are data, so the list can change without a release.
 */
@Entity
@Table(name = "onboarding_task_templates", schema = "hr")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class OnboardingTaskTemplate {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private OnboardingTaskType taskType;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /**
     * Days from the anchor date. Negative values matter for offboarding: handover has to be done
     * before the last day, not on it.
     */
    @Column(name = "offset_days", nullable = false)
    private int offsetDays;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OnboardingTaskTemplate() {
    }

    public OnboardingTaskTemplate(String name, OnboardingTaskType taskType, int displayOrder, int offsetDays) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.taskType = taskType;
        this.displayOrder = displayOrder;
        this.offsetDays = offsetDays;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, int displayOrder, int offsetDays) {
        this.name = name;
        this.displayOrder = displayOrder;
        this.offsetDays = offsetDays;
        this.updatedAt = Instant.now();
    }

    public LocalDate dueDateFrom(LocalDate anchorDate) {
        return anchorDate.plusDays(offsetDays);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public OnboardingTaskType getTaskType() {
        return taskType;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public int getOffsetDays() {
        return offsetDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
