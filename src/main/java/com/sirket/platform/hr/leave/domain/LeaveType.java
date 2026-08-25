package com.sirket.platform.hr.leave.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

/** FR-HR-03: a configurable leave category (annual, sick, unpaid, ...). */
@Entity
@Table(name = "leave_types", schema = "hr")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class LeaveType {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_paid", nullable = false)
    private boolean paid;

    /** Entitlement a new yearly balance starts from. */
    @Column(name = "default_annual_days", nullable = false)
    private int defaultAnnualDays;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LeaveType() {
    }

    public LeaveType(String name, boolean paid, int defaultAnnualDays) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.paid = paid;
        this.defaultAnnualDays = defaultAnnualDays;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, boolean paid, int defaultAnnualDays) {
        this.name = name;
        this.paid = paid;
        this.defaultAnnualDays = defaultAnnualDays;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isPaid() {
        return paid;
    }

    public int getDefaultAnnualDays() {
        return defaultAnnualDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
