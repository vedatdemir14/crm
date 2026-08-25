package com.sirket.platform.crm.integration.service;

import com.sirket.platform.crm.activity.domain.Activity;
import com.sirket.platform.crm.activity.repository.ActivityRepository;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.contact.repository.ContactRepository;
import com.sirket.platform.crm.integration.domain.ExternalMessage;
import com.sirket.platform.crm.integration.domain.MessageDirection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-CRM-12: turns an external mail or calendar item into timeline entries on the contacts it
 * involves. This is the part that carries the actual value and is deliberately independent of any
 * provider.
 */
@Service
public class ExternalMessageLinker {

    private static final Logger log = LoggerFactory.getLogger(ExternalMessageLinker.class);

    private final ContactRepository contactRepository;
    private final ActivityRepository activityRepository;

    public ExternalMessageLinker(ContactRepository contactRepository, ActivityRepository activityRepository) {
        this.contactRepository = contactRepository;
        this.activityRepository = activityRepository;
    }

    /**
     * Rules, all of which are choices worth knowing about:
     * <ul>
     *   <li>Participants are matched to contacts by e-mail, case-insensitively. Any address that
     *       matches no contact is ignored — colleagues and unknown senders are on most threads.</li>
     *   <li>A message involving several known contacts produces one activity per contact, because a
     *       meeting with two people belongs on both timelines.</li>
     *   <li>Nothing is imported when no participant matches. An activity has to belong to a contact
     *       or an opportunity, and inventing a contact from an unknown address would fill the CRM
     *       with junk records.</li>
     *   <li>No opportunity is guessed at. Picking one because a contact happens to have a single
     *       open deal would attach correspondence to the wrong deal as soon as they have two.</li>
     *   <li>The imported activity is attributed to the contact's owner, and marked with its source
     *       so it is distinguishable from something a person typed in.</li>
     * </ul>
     */
    @Transactional
    public LinkResult link(ExternalMessage message) {
        List<Contact> matches = matchingContacts(message.participants());
        if (matches.isEmpty()) {
            log.debug("Eşleşen kişi bulunamadı, mesaj atlandı: {}", message.externalId());
            return new LinkResult(message.externalId(), 0, 0, false);
        }

        int created = 0;
        int alreadyPresent = 0;
        for (Contact contact : matches) {
            if (activityRepository.existsByExternalIdAndContactId(message.externalId(), contact.getId())) {
                alreadyPresent++;
                continue;
            }
            activityRepository.save(Activity.imported(
                    message.type().toActivityType(),
                    subjectFor(message),
                    message.body(),
                    contact,
                    message.occurredAt(),
                    contact.getOwnerUserId(),
                    message.externalId(),
                    message.source()));
            created++;
        }
        return new LinkResult(message.externalId(), created, alreadyPresent, true);
    }

    @Transactional
    public BatchResult linkAll(List<ExternalMessage> messages) {
        List<LinkResult> results = new ArrayList<>();
        for (ExternalMessage message : messages) {
            results.add(link(message));
        }
        int created = results.stream().mapToInt(LinkResult::createdActivities).sum();
        int skipped = results.stream().mapToInt(LinkResult::alreadyImported).sum();
        long unmatched = results.stream().filter(result -> !result.matched()).count();
        return new BatchResult(messages.size(), created, skipped, (int) unmatched, results);
    }

    /**
     * Looks the participants up in one query and keeps the result de-duplicated: the same contact
     * can appear on a message twice (say as both a To and a Cc address).
     */
    private List<Contact> matchingContacts(List<String> participants) {
        if (participants == null || participants.isEmpty()) {
            return List.of();
        }
        List<String> normalised = participants.stream()
                .filter(address -> address != null && !address.isBlank())
                .map(address -> address.trim().toLowerCase())
                .distinct()
                .toList();
        if (normalised.isEmpty()) {
            return List.of();
        }

        Map<UUID, Contact> byId = new LinkedHashMap<>();
        for (Contact contact : contactRepository.findByEmailInIgnoreCase(normalised)) {
            byId.putIfAbsent(contact.getId(), contact);
        }
        return List.copyOf(byId.values());
    }

    private String subjectFor(ExternalMessage message) {
        String prefix = message.direction() == MessageDirection.INBOUND ? "Gelen" : "Giden";
        String subject = (message.subject() == null || message.subject().isBlank())
                ? "(konusuz)"
                : message.subject();
        return "%s: %s".formatted(prefix, subject);
    }

    public record LinkResult(String externalId, int createdActivities, int alreadyImported, boolean matched) {
    }

    public record BatchResult(int messagesReceived, int createdActivities, int alreadyImported,
            int unmatchedMessages, List<LinkResult> results) {
    }
}
