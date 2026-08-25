package com.sirket.platform.crm.opportunity.dto;

import com.sirket.platform.crm.contact.domain.Company;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.opportunity.domain.Opportunity;
import com.sirket.platform.crm.opportunity.domain.OpportunityStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class OpportunityDtos {

    private OpportunityDtos() {
    }

    public record CreateOpportunityRequest(
            @NotBlank(message = "Fırsat adı zorunludur") @Size(max = 255) String name,
            UUID contactId,
            UUID companyId,
            @NotNull(message = "Aşama zorunludur") UUID stageId,
            @PositiveOrZero(message = "Tutar negatif olamaz") BigDecimal amount,
            @Min(value = 0, message = "Olasılık 0-100 aralığında olmalıdır")
            @Max(value = 100, message = "Olasılık 0-100 aralığında olmalıdır") Integer probability,
            LocalDate expectedCloseDate) {
    }

    public record UpdateOpportunityRequest(
            @NotBlank(message = "Fırsat adı zorunludur") @Size(max = 255) String name,
            UUID contactId,
            UUID companyId,
            @PositiveOrZero(message = "Tutar negatif olamaz") BigDecimal amount,
            @Min(value = 0, message = "Olasılık 0-100 aralığında olmalıdır")
            @Max(value = 100, message = "Olasılık 0-100 aralığında olmalıdır") Integer probability,
            LocalDate expectedCloseDate) {
    }

    public record ChangeStageRequest(@NotNull(message = "Aşama zorunludur") UUID stageId) {
    }

    /**
     * FR-CRM-09: {@code lostReason} is required whenever {@code won} is false.
     */
    public record CloseOpportunityRequest(
            @NotNull(message = "Kazanıldı/kaybedildi bilgisi zorunludur") Boolean won,
            @Size(max = 255) String lostReason) {
    }

    public record OpportunityResponse(
            UUID id,
            String name,
            UUID contactId,
            String contactName,
            UUID companyId,
            String companyName,
            UUID stageId,
            String stageName,
            BigDecimal amount,
            Integer probability,
            LocalDate expectedCloseDate,
            OpportunityStatus status,
            String lostReason,
            Instant closedAt,
            UUID ownerUserId,
            Instant createdAt,
            Instant updatedAt) {

        public static OpportunityResponse from(Opportunity opportunity) {
            Contact contact = opportunity.getContact();
            Company company = opportunity.getCompany();
            return new OpportunityResponse(
                    opportunity.getId(),
                    opportunity.getName(),
                    contact != null ? contact.getId() : null,
                    contact != null ? contact.getFirstName() + " " + contact.getLastName() : null,
                    company != null ? company.getId() : null,
                    company != null ? company.getName() : null,
                    opportunity.getStage().getId(),
                    opportunity.getStage().getName(),
                    opportunity.getAmount(),
                    opportunity.getProbability(),
                    opportunity.getExpectedCloseDate(),
                    opportunity.getStatus(),
                    opportunity.getLostReason(),
                    opportunity.getClosedAt(),
                    opportunity.getOwnerUserId(),
                    opportunity.getCreatedAt(),
                    opportunity.getUpdatedAt());
        }
    }
}
