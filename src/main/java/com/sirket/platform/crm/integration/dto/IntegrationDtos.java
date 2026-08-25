package com.sirket.platform.crm.integration.dto;

import com.sirket.platform.crm.integration.domain.ExternalMessage;
import com.sirket.platform.crm.integration.domain.ExternalMessageType;
import com.sirket.platform.crm.integration.domain.MessageDirection;
import com.sirket.platform.crm.integration.service.ExternalMessageLinker;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class IntegrationDtos {

    private IntegrationDtos() {
    }

    public record IncomingMessage(
            @NotBlank(message = "Kaynak mesaj kimliği zorunludur") @Size(max = 255) String externalId,
            @NotBlank(message = "Kaynak sistem adı zorunludur") @Size(max = 60) String source,
            @NotNull(message = "Mesaj türü zorunludur") ExternalMessageType type,
            @NotNull(message = "Yön bilgisi zorunludur") MessageDirection direction,
            @Size(max = 255) String subject,
            String body,
            @NotEmpty(message = "En az bir katılımcı e-postası gereklidir") List<String> participants,
            @NotNull(message = "Gerçekleşme zamanı zorunludur") Instant occurredAt) {

        public ExternalMessage toDomain() {
            return new ExternalMessage(externalId, source, type, direction, subject, body,
                    participants, occurredAt);
        }
    }

    /**
     * {@code @Valid} on the element type is what makes the constraints inside IncomingMessage run;
     * without it only the list itself is checked and malformed messages pass straight through.
     */
    public record ImportRequest(
            @NotEmpty(message = "En az bir mesaj gönderilmelidir")
            List<@NotNull @Valid IncomingMessage> messages) {
    }

    /**
     * {@code unmatchedMessages} is reported rather than hidden: a message whose participants match
     * no contact is silently dropped, and whoever runs the import needs to see that happening.
     */
    public record ImportResponse(
            int messagesReceived,
            int createdActivities,
            int alreadyImported,
            int unmatchedMessages) {

        public static ImportResponse from(ExternalMessageLinker.BatchResult result) {
            return new ImportResponse(result.messagesReceived(), result.createdActivities(),
                    result.alreadyImported(), result.unmatchedMessages());
        }
    }
}
