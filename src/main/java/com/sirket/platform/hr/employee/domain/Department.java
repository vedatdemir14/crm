package com.sirket.platform.hr.employee.domain;

import com.sirket.platform.common.error.ApiExceptions;
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
@Table(name = "departments", schema = "hr")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Department {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    // Soft-deleted entities cannot be mapped lazily in Hibernate; the hierarchy is shallow, so
    // eager loading of the immediate parent is cheap.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_department_id")
    private Department parent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Department() {
    }

    public Department(String name, Department parent) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.parent = parent;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, Department parent) {
        requireNotOwnAncestor(parent);
        this.name = name;
        this.parent = parent;
        this.updatedAt = Instant.now();
    }

    /**
     * A department that ends up inside its own subtree makes the org chart infinite; every walk up
     * the tree, including the one below, would never terminate.
     */
    private void requireNotOwnAncestor(Department candidateParent) {
        for (Department node = candidateParent; node != null; node = node.getParent()) {
            if (node.getId().equals(this.id)) {
                throw new ApiExceptions.BadRequest(
                        "Departman kendi alt ağacına bağlanamaz, döngüsel hiyerarşi oluşur");
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Department getParent() {
        return parent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
