package com.sirket.platform.crm.contact.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

@Entity
@Table(name = "contacts", schema = "crm")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Contact {

    @Id
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String email;

    private String phone;

    private String title;

    // Hibernate rejects a LAZY to-one pointing at a @SoftDelete entity, since the proxy cannot
    // tell whether the target is deleted without loading it. List queries use JOIN FETCH so the
    // eager association does not turn into an N+1.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

    private String source;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Contact() {
    }

    public Contact(String firstName, String lastName, String email, String phone, String title,
            Company company, String source, UUID ownerUserId) {
        this.id = UUID.randomUUID();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.title = title;
        this.company = company;
        this.source = source;
        this.ownerUserId = ownerUserId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String firstName, String lastName, String email, String phone, String title,
            Company company, String source) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.title = title;
        this.company = company;
        this.source = source;
        this.updatedAt = Instant.now();
    }

    public void reassignTo(UUID newOwnerUserId) {
        this.ownerUserId = newOwnerUserId;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getTitle() {
        return title;
    }

    public Company getCompany() {
        return company;
    }

    public String getSource() {
        return source;
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
