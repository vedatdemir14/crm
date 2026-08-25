package com.sirket.platform.crm.opportunity.repository;

import com.sirket.platform.crm.opportunity.domain.PipelineStage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {

    List<PipelineStage> findAllByOrderByDisplayOrderAsc();

    Optional<PipelineStage> findFirstByWonStageIsTrue();

    Optional<PipelineStage> findFirstByLostStageIsTrue();

    Optional<PipelineStage> findByNameIgnoreCase(String name);
}
