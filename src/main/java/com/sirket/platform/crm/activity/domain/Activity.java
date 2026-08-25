package com.sirket.platform.crm.activity.domain;

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
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

@Entity
@Table(name = "activities", schema = "crm")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Activity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Column(nullable = false)
    private String subject;

    private String description;

    // Contact and Opportunity are soft-deleted entities, which Hibernate refuses to map lazily.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "opportunity_id")
    private Opportunity opportunity;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Activity() {
    }

    public Activity(ActivityType type, String subject, String description, Contact contact,
            Opportunity opportunity, Instant occurredAt, UUID createdBy) {
        if (contact == null && opportunity == null) {
            throw new ApiExceptions.BadRequest("Aktivite en az bir kişi veya fırsata bağlı olmalıdır");
        }
        this.id = UUID.randomUUID();
        this.type = type;
        this.subject = subject;
        this.description = description;
        this.contact = contact;
        this.opportunity = opportunity;
        this.occurredAt = occurredAt;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(ActivityType type, String subject, String description, Instant occurredAt) {
        this.type = type;
        this.subject = subject;
        this.description = description;
        this.occurredAt = occurredAt;
        this.updatedAt = Instant.now();
    }

    /**
     * The API design document allows an activity to be corrected by its author "shortly after"
     * it was logged. A timeline that stays editable forever is not a reliable history, so edits
     * close once the window has passed.
     */
    public void requireEditableBy(UUID userId, Duration editWindow) {
        if (!createdBy.equals(userId)) {
            throw new ApiExceptions.Forbidden("Aktiviteyi yalnızca oluşturan kullanıcı düzenleyebilir");
        }
        if (Instant.now().isAfter(createdAt.plus(editWindow))) {
            throw new ApiExceptions.Conflict(
                    "Aktivite düzenleme süresi doldu (%d saat)".formatted(editWindow.toHours()));
        }
    }

    public UUID getId() {
        return id;
    }

    public ActivityType getType() {
        return type;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public Contact getContact() {
        return contact;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public Instant getOccurredAt() {
        return occurredAt;
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
