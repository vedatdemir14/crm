package com.sirket.platform.crm.dashboard.service;

import com.sirket.platform.crm.access.CrmAccessPolicy;
import com.sirket.platform.crm.dashboard.dto.DashboardDtos;
import com.sirket.platform.crm.dashboard.repository.DashboardProjections;
import com.sirket.platform.crm.dashboard.repository.DashboardRepository;
import com.sirket.platform.crm.opportunity.domain.OpportunityStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    /**
     * Sentinels for an omitted date bound. Widening the range in Java keeps the queries free of
     * null date parameters, which PostgreSQL cannot type on its own.
     */
    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final DashboardRepository dashboardRepository;
    private final CrmAccessPolicy accessPolicy;

    public DashboardService(DashboardRepository dashboardRepository, CrmAccessPolicy accessPolicy) {
        this.dashboardRepository = dashboardRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.SummaryResponse summary(LocalDate from, LocalDate to, UUID owner) {
        LocalDate fromDate = from != null ? from : MIN_DATE;
        LocalDate toDate = to != null ? to : MAX_DATE;
        UUID restriction = accessPolicy.ownerRestriction();

        DashboardProjections.CountAndAmount open =
                dashboardRepository.openStats(fromDate, toDate, owner, restriction);
        List<DashboardProjections.StatusAggregate> closed = dashboardRepository.closedStatsByStatus(
                startOfDay(fromDate), startOfDayAfter(toDate), owner, restriction);

        DashboardProjections.StatusAggregate won = pick(closed, OpportunityStatus.WON);
        DashboardProjections.StatusAggregate lost = pick(closed, OpportunityStatus.LOST);
        long wonCount = won != null ? won.count() : 0;
        long lostCount = lost != null ? lost.count() : 0;

        return new DashboardDtos.SummaryResponse(
                from,
                to,
                open != null ? open.count() : 0,
                zeroIfNull(open != null ? open.totalAmount() : null),
                wonCount,
                zeroIfNull(won != null ? won.totalAmount() : null),
                lostCount,
                zeroIfNull(lost != null ? lost.totalAmount() : null),
                winRate(wonCount, lostCount));
    }

    @Transactional(readOnly = true)
    public DashboardDtos.StageDistributionResponse pipeline(LocalDate from, LocalDate to, UUID owner) {
        LocalDate fromDate = from != null ? from : MIN_DATE;
        LocalDate toDate = to != null ? to : MAX_DATE;

        List<DashboardDtos.StageDistributionEntry> stages =
                dashboardRepository.stageDistribution(fromDate, toDate, owner, accessPolicy.ownerRestriction())
                        .stream()
                        .map(row -> new DashboardDtos.StageDistributionEntry(
                                row.stageId(), row.stageName(), row.displayOrder(),
                                row.count(), zeroIfNull(row.totalAmount())))
                        .toList();

        long totalCount = stages.stream().mapToLong(DashboardDtos.StageDistributionEntry::count).sum();
        BigDecimal totalAmount = stages.stream()
                .map(DashboardDtos.StageDistributionEntry::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardDtos.StageDistributionResponse(from, to, totalCount, totalAmount, stages);
    }

    @Transactional(readOnly = true)
    public DashboardDtos.LostReasonsResponse lostReasons(LocalDate from, LocalDate to, UUID owner) {
        LocalDate fromDate = from != null ? from : MIN_DATE;
        LocalDate toDate = to != null ? to : MAX_DATE;

        List<DashboardProjections.LostReasonAggregate> rows = dashboardRepository.lostReasons(
                startOfDay(fromDate), startOfDayAfter(toDate), owner, accessPolicy.ownerRestriction());
        long total = rows.stream().mapToLong(DashboardProjections.LostReasonAggregate::count).sum();

        List<DashboardDtos.LostReasonEntry> reasons = rows.stream()
                .map(row -> new DashboardDtos.LostReasonEntry(
                        row.reason(),
                        row.count(),
                        zeroIfNull(row.totalAmount()),
                        percentage(row.count(), total)))
                .toList();

        return new DashboardDtos.LostReasonsResponse(from, to, total, reasons);
    }

    private DashboardProjections.StatusAggregate pick(List<DashboardProjections.StatusAggregate> rows,
            OpportunityStatus status) {
        return rows.stream().filter(row -> row.status() == status).findFirst().orElse(null);
    }

    /**
     * Null rather than zero when nothing has closed yet, so a dashboard does not show a 0% win rate
     * for a team that simply has no closed deals.
     */
    private BigDecimal winRate(long wonCount, long lostCount) {
        long closed = wonCount + lostCount;
        return closed == 0 ? null : percentage(wonCount, closed);
    }

    private BigDecimal percentage(long part, long total) {
        return total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(part)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Upper bound is exclusive at the start of the following day, so an opportunity closed at any
     * time on the {@code to} date is still counted.
     */
    private Instant startOfDayAfter(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
