package com.sirket.platform.crm.dashboard.repository;

import com.sirket.platform.crm.opportunity.domain.Opportunity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only aggregates behind the CRM dashboard (FR-CRM-08, FR-CRM-09).
 * <p>
 * Date bounds are never null here: the service widens an omitted bound to a sentinel, which keeps
 * the queries free of null-handling and sidesteps PostgreSQL's inability to type a null parameter.
 * <p>
 * Open opportunities are filtered on {@code expectedCloseDate} — "what is forecast to land in this
 * window" — while closed ones are filtered on {@code closedAt}, the date they were actually won or
 * lost.
 */
public interface DashboardRepository extends Repository<Opportunity, UUID> {

    @Query("""
            SELECT new com.sirket.platform.crm.dashboard.repository.DashboardProjections$CountAndAmount(
                COUNT(o), SUM(o.amount))
            FROM Opportunity o
            WHERE o.status = com.sirket.platform.crm.opportunity.domain.OpportunityStatus.OPEN
              AND o.expectedCloseDate >= :from
              AND o.expectedCloseDate <= :to
              AND (:owner IS NULL OR o.ownerUserId = :owner)
              AND (:ownerFilter IS NULL OR o.ownerUserId = :ownerFilter)
            """)
    DashboardProjections.CountAndAmount openStats(@Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("owner") UUID owner,
            @Param("ownerFilter") UUID ownerFilter);

    @Query("""
            SELECT new com.sirket.platform.crm.dashboard.repository.DashboardProjections$StatusAggregate(
                o.status, COUNT(o), SUM(o.amount))
            FROM Opportunity o
            WHERE o.status <> com.sirket.platform.crm.opportunity.domain.OpportunityStatus.OPEN
              AND o.closedAt >= :from
              AND o.closedAt < :to
              AND (:owner IS NULL OR o.ownerUserId = :owner)
              AND (:ownerFilter IS NULL OR o.ownerUserId = :ownerFilter)
            GROUP BY o.status
            """)
    List<DashboardProjections.StatusAggregate> closedStatsByStatus(@Param("from") Instant from,
            @Param("to") Instant to,
            @Param("owner") UUID owner,
            @Param("ownerFilter") UUID ownerFilter);

    /**
     * Driven from PipelineStage with a left join so stages holding no opportunities still appear
     * with a zero count — an empty column is information on a pipeline board.
     */
    @Query("""
            SELECT new com.sirket.platform.crm.dashboard.repository.DashboardProjections$StageAggregate(
                st.id, st.name, st.displayOrder, COUNT(o.id), SUM(o.amount))
            FROM PipelineStage st
            LEFT JOIN Opportunity o
                   ON o.stage = st
                  AND o.status = com.sirket.platform.crm.opportunity.domain.OpportunityStatus.OPEN
                  AND o.expectedCloseDate >= :from
                  AND o.expectedCloseDate <= :to
                  AND (:owner IS NULL OR o.ownerUserId = :owner)
                  AND (:ownerFilter IS NULL OR o.ownerUserId = :ownerFilter)
            GROUP BY st.id, st.name, st.displayOrder
            ORDER BY st.displayOrder
            """)
    List<DashboardProjections.StageAggregate> stageDistribution(@Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("owner") UUID owner,
            @Param("ownerFilter") UUID ownerFilter);

    @Query("""
            SELECT new com.sirket.platform.crm.dashboard.repository.DashboardProjections$LostReasonAggregate(
                o.lostReason, COUNT(o), SUM(o.amount))
            FROM Opportunity o
            WHERE o.status = com.sirket.platform.crm.opportunity.domain.OpportunityStatus.LOST
              AND o.closedAt >= :from
              AND o.closedAt < :to
              AND (:owner IS NULL OR o.ownerUserId = :owner)
              AND (:ownerFilter IS NULL OR o.ownerUserId = :ownerFilter)
            GROUP BY o.lostReason
            ORDER BY COUNT(o) DESC
            """)
    List<DashboardProjections.LostReasonAggregate> lostReasons(@Param("from") Instant from,
            @Param("to") Instant to,
            @Param("owner") UUID owner,
            @Param("ownerFilter") UUID ownerFilter);
}
