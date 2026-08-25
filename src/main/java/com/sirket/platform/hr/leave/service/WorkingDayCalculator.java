package com.sirket.platform.hr.leave.service;

import com.sirket.platform.hr.leave.repository.PublicHolidayRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Counts the working days in a date range, which is what a leave request actually costs the
 * employee (FR-HR-02, FR-HR-03).
 * <p>
 * Weekends and the public holidays from {@code hr.public_holidays} are excluded. Counting calendar
 * days instead would charge someone for a week off that happens to contain a national holiday.
 */
@Service
public class WorkingDayCalculator {

    private static final Set<DayOfWeek> WEEKEND = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private final PublicHolidayRepository holidayRepository;

    public WorkingDayCalculator(PublicHolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    /**
     * Both bounds are inclusive: a request for a single day counts that day.
     */
    @Transactional(readOnly = true)
    public int countWorkingDays(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            return 0;
        }
        Set<LocalDate> holidays = Set.copyOf(
                holidayRepository.findDatesBetween(startDate, endDate));

        int workingDays = 0;
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            if (!WEEKEND.contains(day.getDayOfWeek()) && !holidays.contains(day)) {
                workingDays++;
            }
        }
        return workingDays;
    }
}
