package com.sirket.platform.crm.opportunity.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.crm.opportunity.domain.PipelineStage;
import com.sirket.platform.crm.opportunity.dto.PipelineStageDtos;
import com.sirket.platform.crm.opportunity.repository.PipelineStageRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineStageService {

    private final PipelineStageRepository stageRepository;

    public PipelineStageService(PipelineStageRepository stageRepository) {
        this.stageRepository = stageRepository;
    }

    @Transactional(readOnly = true)
    public List<PipelineStageDtos.PipelineStageResponse> list() {
        return stageRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(PipelineStageDtos.PipelineStageResponse::from)
                .toList();
    }

    @Transactional
    public PipelineStageDtos.PipelineStageResponse create(PipelineStageDtos.PipelineStageRequest request) {
        requireNotBothWonAndLost(request);
        stageRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            throw new ApiExceptions.Conflict("Bu isimde bir aşama zaten var: " + request.name());
        });
        PipelineStage stage = new PipelineStage(
                request.name(), request.displayOrder(), request.wonStage(), request.lostStage());
        return PipelineStageDtos.PipelineStageResponse.from(stageRepository.save(stage));
    }

    @Transactional
    public PipelineStageDtos.PipelineStageResponse update(UUID id, PipelineStageDtos.PipelineStageRequest request) {
        requireNotBothWonAndLost(request);
        PipelineStage stage = requireExisting(id);
        stageRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ApiExceptions.Conflict("Bu isimde bir aşama zaten var: " + request.name());
            }
        });
        stage.update(request.name(), request.displayOrder(), request.wonStage(), request.lostStage());
        return PipelineStageDtos.PipelineStageResponse.from(stageRepository.save(stage));
    }

    public PipelineStage requireExisting(UUID id) {
        return stageRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Pipeline aşaması bulunamadı: " + id));
    }

    /**
     * The stage an opportunity lands on when it is closed. Configuration owns which stage that is,
     * so a missing won/lost stage is a setup error rather than a user error.
     */
    public PipelineStage requireClosingStage(boolean won) {
        return (won ? stageRepository.findFirstByWonStageIsTrue() : stageRepository.findFirstByLostStageIsTrue())
                .orElseThrow(() -> new ApiExceptions.Conflict(won
                        ? "Kazanıldı olarak işaretlenmiş bir pipeline aşaması tanımlı değil"
                        : "Kaybedildi olarak işaretlenmiş bir pipeline aşaması tanımlı değil"));
    }

    private void requireNotBothWonAndLost(PipelineStageDtos.PipelineStageRequest request) {
        if (request.wonStage() && request.lostStage()) {
            throw new ApiExceptions.BadRequest("Bir aşama hem kazanıldı hem kaybedildi olarak işaretlenemez");
        }
    }
}
