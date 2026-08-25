package com.sirket.platform.crm.opportunity.dto;

import com.sirket.platform.crm.opportunity.domain.PipelineStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class PipelineStageDtos {

    private PipelineStageDtos() {
    }

    public record PipelineStageRequest(
            @NotBlank(message = "Aşama adı zorunludur") @Size(max = 100) String name,
            @PositiveOrZero(message = "Sıra değeri negatif olamaz") int displayOrder,
            boolean wonStage,
            boolean lostStage) {
    }

    public record PipelineStageResponse(
            UUID id,
            String name,
            int displayOrder,
            boolean wonStage,
            boolean lostStage) {

        public static PipelineStageResponse from(PipelineStage stage) {
            return new PipelineStageResponse(
                    stage.getId(),
                    stage.getName(),
                    stage.getDisplayOrder(),
                    stage.isWonStage(),
                    stage.isLostStage());
        }
    }
}
