package com.sirket.platform.crm.opportunity.repository;

import com.sirket.platform.crm.opportunity.domain.Opportunity;
import com.sirket.platform.crm.opportunity.domain.OpportunityStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    /**
     * {@code ownerFilter} is null for roles allowed to see every record (FR-CRM-10). The status
     * parameter is an enum, so it needs no explicit cast the way nullable strings do.
     */
    @Query(value = """
            SELECT o FROM Opportunity o
            LEFT JOIN FETCH o.contact
            LEFT JOIN FETCH o.company
            JOIN FETCH o.stage st
            WHERE (:stageId IS NULL OR st.id = :stageId)
              AND (:status IS NULL OR o.status = :status)
              AND (:contactId IS NULL OR o.contact.id = :contactId)
              AND (:owner IS NULL OR o.ownerUserId = :owner)
              AND (:ownerFilter IS NULL OR o.ownerUserId = :ownerFilter)
            """,
            countQuery = """
            SELECT COUNT(o) FROM Opportunity o
            WHERE (:stageId IS NULL OR o.stage.id = :stageId)
              AND (:status IS NULL OR o.status = :status)
              AND (:contactId IS NULL OR o.contact.id = :contactId)
              AND (:owner IS NULL OR o.ownerUserId = :owner)
              AND (:ownerFilter IS NULL OR o.ownerUserId = :ownerFilter)
            """)
    Page<Opportunity> search(@Param("stageId") UUID stageId,
            @Param("status") OpportunityStatus status,
            @Param("contactId") UUID contactId,
            @Param("owner") UUID owner,
            @Param("ownerFilter") UUID ownerFilter,
            Pageable pageable);

    /** Chunked read for the CSV export (FR-CRM-11); ordering keeps successive pages stable. */
    Page<Opportunity> findByCreatedAtBetweenOrderByCreatedAtAsc(Instant from, Instant to, Pageable pageable);

    boolean existsByStageId(UUID stageId);

    boolean existsByContactId(UUID contactId);
}
