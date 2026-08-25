package com.sirket.platform.crm.integration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Where the last successful sync for a provider left off. Without this the job would either
 * re-fetch everything each run or, worse, silently skip messages that arrived while the
 * application was down.
 */
@Entity
@Table(name = "integration_sync_state", schema = "common")
public class IntegrationSyncState {

    @Id
    private String provider;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IntegrationSyncState() {
    }

    public IntegrationSyncState(String provider, Instant lastSyncedAt) {
        this.provider = provider;
        this.lastSyncedAt = lastSyncedAt;
        this.updatedAt = Instant.now();
    }

    public void advanceTo(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
        this.updatedAt = Instant.now();
    }

    public String getProvider() {
        return provider;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
