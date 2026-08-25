package com.sirket.platform.crm.integration.service;

import com.sirket.platform.crm.integration.domain.ExternalMessage;
import com.sirket.platform.crm.integration.domain.IntegrationSyncState;
import com.sirket.platform.crm.integration.provider.MailCalendarProvider;
import com.sirket.platform.crm.integration.repository.IntegrationSyncStateRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-CRM-12: periodically pulls from the configured mail/calendar provider and files what it finds
 * onto the matching contacts' timelines.
 * <p>
 * With the stub provider in place this does nothing but record that it ran, so the scheduling and
 * bookkeeping are exercised well before a real provider is chosen.
 */
@Component
public class MailCalendarSyncJob {

    private static final Logger log = LoggerFactory.getLogger(MailCalendarSyncJob.class);

    private final MailCalendarProvider provider;
    private final ExternalMessageLinker linker;
    private final IntegrationSyncStateRepository syncStateRepository;
    private final Duration initialLookback;

    public MailCalendarSyncJob(MailCalendarProvider provider, ExternalMessageLinker linker,
            IntegrationSyncStateRepository syncStateRepository,
            @Value("${crm.integration.initial-lookback:P7D}") Duration initialLookback) {
        this.provider = provider;
        this.linker = linker;
        this.syncStateRepository = syncStateRepository;
        this.initialLookback = initialLookback;
    }

    @Scheduled(cron = "${crm.integration.sync-cron:0 */15 * * * *}")
    public void runScheduled() {
        ExternalMessageLinker.BatchResult result = run();
        if (result.messagesReceived() > 0) {
            log.info("E-posta/takvim senkronizasyonu: {} mesaj, {} aktivite oluşturuldu, {} zaten mevcut, "
                            + "{} eşleşmedi",
                    result.messagesReceived(), result.createdActivities(), result.alreadyImported(),
                    result.unmatchedMessages());
        }
    }

    /**
     * The cursor only advances after the batch has been filed, so a failure mid-run leaves the
     * window open and the next pass picks the same messages up again. Re-importing is harmless
     * because the linker de-duplicates on external id.
     */
    @Transactional
    public ExternalMessageLinker.BatchResult run() {
        Instant startedAt = Instant.now();
        Instant since = syncStateRepository.findById(provider.name())
                .map(IntegrationSyncState::getLastSyncedAt)
                .orElse(startedAt.minus(initialLookback));

        List<ExternalMessage> messages = provider.fetchSince(since);
        ExternalMessageLinker.BatchResult result = linker.linkAll(messages);

        syncStateRepository.findById(provider.name())
                .ifPresentOrElse(
                        state -> state.advanceTo(startedAt),
                        () -> syncStateRepository.save(new IntegrationSyncState(provider.name(), startedAt)));
        return result;
    }
}
