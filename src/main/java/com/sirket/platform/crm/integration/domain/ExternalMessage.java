package com.sirket.platform.crm.integration.domain;

import java.time.Instant;
import java.util.List;

/**
 * A mail or calendar item in a shape no provider owns.
 * <p>
 * This is the seam described in FR-CRM-12: everything downstream — matching participants to
 * contacts, building the timeline entry, avoiding duplicate imports — works against this record, so
 * choosing Gmail, Microsoft Graph or an IMAP/CalDAV source later only means writing something that
 * produces these.
 *
 * @param externalId  the id in the source system; identity for de-duplication
 * @param source      which system it came from, kept on the imported activity
 * @param type        whether it was correspondence or a meeting
 * @param direction   inbound or outbound, from the company's point of view
 * @param participants e-mail addresses on the message, used to find the matching contacts
 */
public record ExternalMessage(
        String externalId,
        String source,
        ExternalMessageType type,
        MessageDirection direction,
        String subject,
        String body,
        List<String> participants,
        Instant occurredAt) {
}
