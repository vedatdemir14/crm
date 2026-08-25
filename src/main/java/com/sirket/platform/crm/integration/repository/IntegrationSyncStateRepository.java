package com.sirket.platform.crm.integration.repository;

import com.sirket.platform.crm.integration.domain.IntegrationSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationSyncStateRepository extends JpaRepository<IntegrationSyncState, String> {
}
