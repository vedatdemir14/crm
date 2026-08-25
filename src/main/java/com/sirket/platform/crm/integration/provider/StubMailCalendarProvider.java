package com.sirket.platform.crm.integration.provider;

import com.sirket.platform.crm.integration.domain.ExternalMessage;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Placeholder provider used until the company picks a mail and calendar system (SRS Bölüm 8).
 * <p>
 * It returns nothing, so the scheduled sync is a no-op rather than a failure. It steps aside
 * automatically once a real {@link MailCalendarProvider} bean exists, which means adopting a
 * provider needs no change here.
 * <p>
 * The import path is still usable in the meantime: messages can be posted to the integration
 * endpoint, which runs the same matching logic a provider would feed.
 */
@Component
@ConditionalOnMissingBean(ignored = StubMailCalendarProvider.class, value = MailCalendarProvider.class)
public class StubMailCalendarProvider implements MailCalendarProvider {

    private static final Logger log = LoggerFactory.getLogger(StubMailCalendarProvider.class);

    @Override
    public String name() {
        return "stub";
    }

    @Override
    public List<ExternalMessage> fetchSince(Instant since) {
        log.debug("E-posta/takvim sağlayıcısı henüz seçilmedi; senkronizasyon boş geçiliyor (since={})", since);
        return List.of();
    }
}
