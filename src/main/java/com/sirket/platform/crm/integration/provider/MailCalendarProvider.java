package com.sirket.platform.crm.integration.provider;

import com.sirket.platform.crm.integration.domain.ExternalMessage;
import java.time.Instant;
import java.util.List;

/**
 * The one piece of FR-CRM-12 that depends on which mail and calendar system the company uses.
 * <p>
 * This mirrors how the SRS handles payroll: the provider is undecided, so the platform is built
 * against an interface and ships with a stub. Adding Gmail, Microsoft Graph or CalDAV later means
 * writing a class here and nothing else — the matching and import logic already works.
 */
public interface MailCalendarProvider {

    /** Identifier stored on imported activities and used as the sync-state key. */
    String name();

    /**
     * Messages and calendar items that appeared at or after {@code since}. Implementations may
     * return items already imported; the linker de-duplicates by external id.
     */
    List<ExternalMessage> fetchSince(Instant since);
}
