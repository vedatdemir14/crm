package com.sirket.platform.crm.contact.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

/**
 * The owner is held as a bare user id rather than a JPA association: the database keeps the
 * foreign key for integrity, but the CRM module stays free of a compile-time dependency on the
 * identity module (Mimari Tasarım Dokümanı §5).
 */
@Entity
@Table(name = "companies", schema = "crm")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Company {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String industry;

    private String website;

    private String address;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {
    }

    public Company(String name, String industry, String website, String address, UUID ownerUserId) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.industry = industry;
        this.website = website;
        this.address = address;
        this.ownerUserId = ownerUserId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, String industry, String website, String address) {
        this.name = name;
        this.industry = industry;
        this.website = website;
        this.address = address;
        this.updatedAt = Instant.now();
    }

    public void reassignTo(UUID newOwnerUserId) {
        this.ownerUserId = newOwnerUserId;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIndustry() {
        return industry;
    }

    public String getWebsite() {
        return website;
    }

    public String getAddress() {
        return address;
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
