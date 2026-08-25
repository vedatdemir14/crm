package com.sirket.platform.crm.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    /**
     * FR-CRM-08. {@code winRate} is the percentage of closed opportunities that were won, and is
     * null when nothing closed in the window — "no data" is not the same statement as "0%".
     */
    public record SummaryResponse(
            LocalDate from,
            LocalDate to,
            long openCount,
            BigDecimal openAmount,
            long wonCount,
            BigDecimal wonAmount,
            long lostCount,
            BigDecimal lostAmount,
            BigDecimal winRate) {
    }

    public record StageDistributionEntry(
            UUID stageId,
            String stageName,
            int displayOrder,
            long count,
            BigDecimal totalAmount) {
    }

    public record StageDistributionResponse(
            LocalDate from,
            LocalDate to,
            long totalCount,
            BigDecimal totalAmount,
            List<StageDistributionEntry> stages) {
    }

    /**
     * FR-CRM-09: the distribution of reasons behind lost opportunities.
     */
    public record LostReasonEntry(
            String reason,
            long count,
            BigDecimal totalAmount,
            BigDecimal share) {
    }

    public record LostReasonsResponse(
            LocalDate from,
            LocalDate to,
            long totalLostCount,
            List<LostReasonEntry> reasons) {
    }
}
