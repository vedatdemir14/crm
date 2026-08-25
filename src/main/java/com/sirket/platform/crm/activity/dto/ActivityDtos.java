package com.sirket.platform.crm.activity.dto;

import com.sirket.platform.crm.activity.domain.Activity;
import com.sirket.platform.crm.activity.domain.ActivityType;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.opportunity.domain.Opportunity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ActivityDtos {

    private ActivityDtos() {
    }

    public record ActivityRequest(
            @NotNull(message = "Aktivite türü zorunludur") ActivityType type,
            @NotBlank(message = "Konu zorunludur") @Size(max = 255) String subject,
            String description,
            UUID contactId,
            UUID opportunityId,
            @NotNull(message = "Gerçekleşme zamanı zorunludur") Instant occurredAt) {
    }

    /**
     * Editing may not move an activity onto a different contact or opportunity; that would
     * silently rewrite another record's history.
     */
    public record UpdateActivityRequest(
            @NotNull(message = "Aktivite türü zorunludur") ActivityType type,
            @NotBlank(message = "Konu zorunludur") @Size(max = 255) String subject,
            String description,
            @NotNull(message = "Gerçekleşme zamanı zorunludur") Instant occurredAt) {
    }

    public record ActivityResponse(
            UUID id,
            ActivityType type,
            String subject,
            String description,
            UUID contactId,
            String contactName,
            UUID opportunityId,
            String opportunityName,
            Instant occurredAt,
            UUID createdBy,
            Instant createdAt) {

        public static ActivityResponse from(Activity activity) {
            Contact contact = activity.getContact();
            Opportunity opportunity = activity.getOpportunity();
            return new ActivityResponse(
                    activity.getId(),
                    activity.getType(),
                    activity.getSubject(),
                    activity.getDescription(),
                    contact != null ? contact.getId() : null,
                    contact != null ? contact.getFirstName() + " " + contact.getLastName() : null,
                    opportunity != null ? opportunity.getId() : null,
                    opportunity != null ? opportunity.getName() : null,
                    activity.getOccurredAt(),
                    activity.getCreatedBy(),
                    activity.getCreatedAt());
        }
    }
}
