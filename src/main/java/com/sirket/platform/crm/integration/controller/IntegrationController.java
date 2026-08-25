package com.sirket.platform.crm.integration.controller;

import com.sirket.platform.crm.integration.dto.IntegrationDtos;
import com.sirket.platform.crm.integration.service.ExternalMessageLinker;
import com.sirket.platform.crm.integration.service.MailCalendarSyncJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-CRM-12 entry points. Admin-only: this writes onto other people's timelines, and the payload is
 * trusted to describe real correspondence.
 */
@RestController
@RequestMapping("/api/crm/integrations/mail-calendar")
@Tag(name = "CRM — E-posta/Takvim Entegrasyonu")
@PreAuthorize("hasRole('ADMIN')")
public class IntegrationController {

    private final ExternalMessageLinker linker;
    private final MailCalendarSyncJob syncJob;

    public IntegrationController(ExternalMessageLinker linker, MailCalendarSyncJob syncJob) {
        this.linker = linker;
        this.syncJob = syncJob;
    }

    /**
     * Feeds messages in directly. This is what makes the matching logic usable before a provider is
     * chosen — a script, an export or a webhook can post here and the entries land on the right
     * timelines, exactly as a provider's messages would.
     */
    @PostMapping("/messages")
    @Operation(summary = "Dış kaynaktan gelen yazışma/toplantı kayıtlarını ilgili kişilere ilişkilendirir")
    public IntegrationDtos.ImportResponse importMessages(
            @Valid @RequestBody IntegrationDtos.ImportRequest request) {
        return IntegrationDtos.ImportResponse.from(linker.linkAll(
                request.messages().stream().map(IntegrationDtos.IncomingMessage::toDomain).toList()));
    }

    @PostMapping("/sync")
    @Operation(summary = "Yapılandırılmış sağlayıcıdan senkronizasyonu hemen çalıştırır")
    public IntegrationDtos.ImportResponse syncNow() {
        ExternalMessageLinker.BatchResult result = syncJob.run();
        return IntegrationDtos.ImportResponse.from(result);
    }
}
