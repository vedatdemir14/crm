package com.sirket.platform.crm.activity.repository;

import com.sirket.platform.crm.activity.domain.Activity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    /**
     * FR-CRM-05: the timeline of a contact, newest first. Activities logged against an
     * opportunity that belongs to the contact are deliberately not folded in here — the contact
     * endpoint answers "what did we do with this person".
     */
    @Query(value = """
            SELECT a FROM Activity a
            LEFT JOIN FETCH a.contact
            LEFT JOIN FETCH a.opportunity
            WHERE a.contact.id = :contactId
            ORDER BY a.occurredAt DESC
            """,
            countQuery = "SELECT COUNT(a) FROM Activity a WHERE a.contact.id = :contactId")
    Page<Activity> timelineOfContact(@Param("contactId") UUID contactId, Pageable pageable);

    @Query(value = """
            SELECT a FROM Activity a
            LEFT JOIN FETCH a.contact
            LEFT JOIN FETCH a.opportunity
            WHERE a.opportunity.id = :opportunityId
            ORDER BY a.occurredAt DESC
            """,
            countQuery = "SELECT COUNT(a) FROM Activity a WHERE a.opportunity.id = :opportunityId")
    Page<Activity> timelineOfOpportunity(@Param("opportunityId") UUID opportunityId, Pageable pageable);

    /** FR-CRM-12: identity of an imported activity, so a repeated sync does not import it twice. */
    boolean existsByExternalIdAndContactId(String externalId, UUID contactId);
}
