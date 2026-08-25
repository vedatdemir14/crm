package com.sirket.platform.crm.dashboard.repository;

import com.sirket.platform.crm.opportunity.domain.OpportunityStatus;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Raw aggregate rows returned by the dashboard queries. {@code totalAmount} is null when a group
 * has no rows with an amount, which the service turns into zero.
 */
public final class DashboardProjections {

    private DashboardProjections() {
    }

    public record CountAndAmount(long count, BigDecimal totalAmount) {
    }

    public record StatusAggregate(OpportunityStatus status, long count, BigDecimal totalAmount) {
    }

    public record StageAggregate(UUID stageId, String stageName, int displayOrder, long count,
            BigDecimal totalAmount) {
    }

    public record LostReasonAggregate(String reason, long count, BigDecimal totalAmount) {
    }
}
