package com.sirket.platform.crm.opportunity.domain;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.crm.contact.domain.Company;
import com.sirket.platform.crm.contact.domain.Contact;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

@Entity
@Table(name = "opportunities", schema = "crm")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Opportunity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    // Contact and Company are soft-deleted entities, which Hibernate refuses to map lazily;
    // list queries use JOIN FETCH so this does not become an N+1.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private PipelineStage stage;

    private BigDecimal amount;

    private Integer probability;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpportunityStatus status;

    @Column(name = "lost_reason")
    private String lostReason;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Opportunity() {
    }

    public Opportunity(String name, Contact contact, Company company, PipelineStage stage, BigDecimal amount,
            Integer probability, LocalDate expectedCloseDate, UUID ownerUserId) {
        if (stage.isClosingStage()) {
            throw new ApiExceptions.BadRequest(
                    "Fırsat kazanıldı/kaybedildi aşamasında açılamaz, açık bir aşama seçiniz");
        }
        this.id = UUID.randomUUID();
        this.name = name;
        this.contact = contact;
        this.company = company;
        this.stage = stage;
        this.amount = amount;
        this.probability = probability;
        this.expectedCloseDate = expectedCloseDate;
        this.status = OpportunityStatus.OPEN;
        this.ownerUserId = ownerUserId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, Contact contact, Company company, BigDecimal amount, Integer probability,
            LocalDate expectedCloseDate) {
        this.name = name;
        this.contact = contact;
        this.company = company;
        this.amount = amount;
        this.probability = probability;
        this.expectedCloseDate = expectedCloseDate;
        this.updatedAt = Instant.now();
    }

    /**
     * FR-CRM-04. Moving straight onto a won/lost stage is rejected: closing has to go through
     * {@link #close}, which is where the mandatory lost reason of FR-CRM-09 is enforced.
     */
    public void moveToStage(PipelineStage newStage) {
        requireOpen();
        if (newStage.isClosingStage()) {
            throw new ApiExceptions.BadRequest(
                    "Kazanıldı/kaybedildi aşamasına geçiş için kapatma işlemini kullanın");
        }
        this.stage = newStage;
        this.updatedAt = Instant.now();
    }

    /**
     * FR-CRM-04 and FR-CRM-09: closes the opportunity and requires a reason when it is lost.
     */
    public void close(boolean won, String lostReason, PipelineStage closingStage) {
        requireOpen();
        if (!won && (lostReason == null || lostReason.isBlank())) {
            throw new ApiExceptions.BadRequest("Kaybedilen fırsat için kayıp nedeni zorunludur");
        }
        this.status = won ? OpportunityStatus.WON : OpportunityStatus.LOST;
        this.lostReason = won ? null : lostReason;
        this.stage = closingStage;
        this.closedAt = Instant.now();
        this.updatedAt = this.closedAt;
    }

    public void reassignTo(UUID newOwnerUserId) {
        this.ownerUserId = newOwnerUserId;
        this.updatedAt = Instant.now();
    }

    public boolean isOpen() {
        return status == OpportunityStatus.OPEN;
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new ApiExceptions.Conflict("Fırsat zaten kapatılmış: " + status);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Contact getContact() {
        return contact;
    }

    public Company getCompany() {
        return company;
    }

    public PipelineStage getStage() {
        return stage;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getProbability() {
        return probability;
    }

    public LocalDate getExpectedCloseDate() {
        return expectedCloseDate;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public String getLostReason() {
        return lostReason;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
