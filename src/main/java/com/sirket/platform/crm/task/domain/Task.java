package com.sirket.platform.crm.task.domain;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.opportunity.domain.Opportunity;
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
@Table(name = "tasks", schema = "crm")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Task {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(name = "assigned_to", nullable = false)
    private UUID assignedTo;

    // Contact and Opportunity are soft-deleted entities, which Hibernate refuses to map lazily.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "related_contact_id")
    private Contact relatedContact;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "related_opportunity_id")
    private Opportunity relatedOpportunity;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {
    }

    public Task(String title, String description, LocalDate dueDate, UUID assignedTo, Contact relatedContact,
            Opportunity relatedOpportunity, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = TaskStatus.OPEN;
        this.assignedTo = assignedTo;
        this.relatedContact = relatedContact;
        this.relatedOpportunity = relatedOpportunity;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String title, String description, LocalDate dueDate, UUID assignedTo,
            Contact relatedContact, Opportunity relatedOpportunity) {
        requireOpen();
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.assignedTo = assignedTo;
        this.relatedContact = relatedContact;
        this.relatedOpportunity = relatedOpportunity;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        requireOpen();
        this.status = TaskStatus.DONE;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public boolean isOpen() {
        return status == TaskStatus.OPEN;
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new ApiExceptions.Conflict("Görev zaten tamamlanmış");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public Contact getRelatedContact() {
        return relatedContact;
    }

    public Opportunity getRelatedOpportunity() {
        return relatedOpportunity;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
