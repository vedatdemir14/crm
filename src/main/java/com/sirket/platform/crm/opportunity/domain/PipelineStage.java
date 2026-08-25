package com.sirket.platform.crm.opportunity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A configurable pipeline stage (FR-CRM-04). Deliberately not soft-deleted: the API offers no
 * delete for stages, and keeping it a plain entity lets opportunities reference it lazily.
 */
@Entity
@Table(name = "pipeline_stages", schema = "crm")
public class PipelineStage {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_won_stage", nullable = false)
    private boolean wonStage;

    @Column(name = "is_lost_stage", nullable = false)
    private boolean lostStage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PipelineStage() {
    }

    public PipelineStage(String name, int displayOrder, boolean wonStage, boolean lostStage) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.displayOrder = displayOrder;
        this.wonStage = wonStage;
        this.lostStage = lostStage;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, int displayOrder, boolean wonStage, boolean lostStage) {
        this.name = name;
        this.displayOrder = displayOrder;
        this.wonStage = wonStage;
        this.lostStage = lostStage;
        this.updatedAt = Instant.now();
    }

    public boolean isClosingStage() {
        return wonStage || lostStage;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isWonStage() {
        return wonStage;
    }

    public boolean isLostStage() {
        return lostStage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
